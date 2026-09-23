package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.CompatibilityRule;
import com.sliit.sparepartshub.entity.PickTicket;
import com.sliit.sparepartshub.entity.PickTicketItem;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.ProductSpec;
import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.sales.dto.CartLine;
import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;
import com.sliit.sparepartshub.sales.dto.SalesCart;
import com.sliit.sparepartshub.sales.dto.SerialOption;
import com.sliit.sparepartshub.sales.repository.SalesAuditLogRepository;
import com.sliit.sparepartshub.sales.repository.SalesCompatibilityRuleRepository;
import com.sliit.sparepartshub.sales.repository.SalesPickTicketItemRepository;
import com.sliit.sparepartshub.sales.repository.SalesPickTicketRepository;
import com.sliit.sparepartshub.sales.repository.SalesProductRepository;
import com.sliit.sparepartshub.sales.repository.SalesProductSpecRepository;
import com.sliit.sparepartshub.sales.repository.SalesSaleItemRepository;
import com.sliit.sparepartshub.sales.repository.SalesSaleRepository;
import com.sliit.sparepartshub.sales.repository.SalesSerialNumberRepository;
import com.sliit.sparepartshub.stockmonitoring.service.StockMonitoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class SalesService {
    private final SalesProductRepository products;
    private final SalesProductSpecRepository specs;
    private final SalesCompatibilityRuleRepository rules;
    private final SalesSaleRepository sales;
    private final SalesSaleItemRepository saleItems;
    private final SalesPickTicketRepository tickets;
    private final SalesPickTicketItemRepository ticketItems;
    private final SalesSerialNumberRepository serials;
    private final SalesAuditLogRepository audit;
    private final StockMonitoringService stockMonitoring;

    public SalesService(SalesProductRepository products,
                        SalesProductSpecRepository specs,
                        SalesCompatibilityRuleRepository rules,
                        SalesSaleRepository sales,
                        SalesSaleItemRepository saleItems,
                        SalesPickTicketRepository tickets,
                        SalesPickTicketItemRepository ticketItems,
                        SalesSerialNumberRepository serials,
                        SalesAuditLogRepository audit,
                        StockMonitoringService stockMonitoring) {
        this.products = products;
        this.specs = specs;
        this.rules = rules;
        this.sales = sales;
        this.saleItems = saleItems;
        this.tickets = tickets;
        this.ticketItems = ticketItems;
        this.serials = serials;
        this.audit = audit;
        this.stockMonitoring = stockMonitoring;
    }

    public List<Product> search(String q, String category, String brand) {
        return products.search(q, category, brand);
    }

    public List<String> categories() {
        return products.categories();
    }

    public List<String> brands() {
        return products.brands();
    }

    public Product getProduct(Integer id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    public Map<Integer, Integer> serialAvailability(Collection<Product> visibleProducts) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Product product : visibleProducts) {
            if (product.isSerialTracked()) {
                long count = serials.countByProduct_ProductIdAndCurrentStatusAndSaleIsNull(
                        product.getProductId(),
                        SerialNumber.CurrentStatus.in_stock
                );
                result.put(product.getProductId(), (int) Math.min(Integer.MAX_VALUE, count));
            }
        }
        return result;
    }

    public Map<Integer, List<SerialOption>> availableSerialsForCart(SalesCart cart) {
        Map<Integer, List<SerialOption>> result = new HashMap<>();
        for (CartLine line : cart.getLines()) {
            if (!line.isSerialTracked()) {
                continue;
            }
            List<SerialOption> options = serials
                    .findByProduct_ProductIdAndCurrentStatusAndSaleIsNullOrderBySerialValueAsc(
                            line.getProductId(),
                            SerialNumber.CurrentStatus.in_stock
                    )
                    .stream()
                    .map(s -> new SerialOption(s.getSerialId(), s.getSerialValue()))
                    .toList();
            result.put(line.getProductId(), options);
        }
        return result;
    }

    public void addToCart(SalesCart cart, Integer productId, Integer qty) {
        Product product = getProduct(productId);
        if (qty == null || qty < 1) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        Optional<CartLine> existing = cart.getLines().stream()
                .filter(line -> line.getProductId().equals(productId))
                .findFirst();

        int requested = existing.map(line -> line.getQuantity() + qty).orElse(qty);
        validateSellableQuantity(product, requested);

        if (existing.isPresent()) {
            existing.get().setQuantity(requested);
        } else {
            CartLine line = new CartLine();
            line.setProductId(product.getProductId());
            line.setCode(product.getProductCode());
            line.setName(product.getName());
            line.setQuantity(qty);
            line.setCatalogPrice(money(product.getPrice()));
            line.setDiscountAmount(BigDecimal.ZERO);
            line.setUnitPrice(money(product.getPrice()));
            line.setSerialTracked(product.isSerialTracked());
            cart.getLines().add(line);
        }
    }

    public void updateLine(SalesCart cart,
                           int index,
                           int qty,
                           BigDecimal discountAmount,
                           String reason) {
        CartLine line = requireLine(cart, index);
        Product product = getProduct(line.getProductId());

        if (qty < 1) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        validateSellableQuantity(product, qty);

        BigDecimal catalogPrice = money(product.getPrice());
        BigDecimal discount = normalizeDiscount(discountAmount);
        validateDiscount(catalogPrice, discount, reason, product.getName());
        BigDecimal finalUnitPrice = money(catalogPrice.subtract(discount));

        boolean quantityChanged = !Integer.valueOf(qty).equals(line.getQuantity());
        line.setQuantity(qty);
        line.setCatalogPrice(catalogPrice);
        line.setDiscountAmount(discount);
        line.setUnitPrice(finalUnitPrice);
        line.setDiscountReason(discount.signum() == 0 ? null : blank(reason));
        line.setSerialTracked(product.isSerialTracked());

        if (quantityChanged) {
            // The Sales Executive must explicitly confirm the exact physical
            // units again after a quantity change.
            line.getSelectedSerialIds().clear();
        }
    }

    public void selectSerials(SalesCart cart, int index, List<Integer> requestedSerialIds) {
        CartLine line = requireLine(cart, index);
        Product product = getProduct(line.getProductId());

        if (!product.isSerialTracked()) {
            throw new IllegalArgumentException("This product does not require serial-number selection.");
        }

        List<Integer> ids = requestedSerialIds == null ? List.of() : requestedSerialIds;
        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>(ids);

        if (uniqueIds.size() != ids.size()) {
            throw new IllegalArgumentException("The same serial number cannot be selected more than once.");
        }
        if (uniqueIds.size() != line.getQuantity()) {
            throw new IllegalArgumentException(
                    "Select exactly " + line.getQuantity() + " serial number(s) for " + product.getName() + "."
            );
        }

        Map<Integer, SerialNumber> availableById = new HashMap<>();
        for (SerialNumber serial : serials
                .findByProduct_ProductIdAndCurrentStatusAndSaleIsNullOrderBySerialValueAsc(
                        product.getProductId(),
                        SerialNumber.CurrentStatus.in_stock
                )) {
            availableById.put(serial.getSerialId(), serial);
        }

        for (Integer id : uniqueIds) {
            if (!availableById.containsKey(id)) {
                throw new IllegalArgumentException(
                        "One of the selected serial numbers is no longer available for " + product.getName() + "."
                );
            }
        }

        line.getSelectedSerialIds().clear();
        line.getSelectedSerialIds().addAll(uniqueIds);
    }

    public List<CompatibilityConflict> compatibility(SalesCart cart) {
        if (cart.getLines().size() < 2) {
            return List.of();
        }

        List<Integer> ids = cart.getLines().stream()
                .map(CartLine::getProductId)
                .toList();

        List<ProductSpec> productSpecs = specs.findByProduct_ProductIdIn(ids);
        Map<Integer, List<ProductSpec>> byProduct = new HashMap<>();
        for (ProductSpec spec : productSpecs) {
            byProduct.computeIfAbsent(
                    spec.getProduct().getProductId(),
                    ignored -> new ArrayList<>()
            ).add(spec);
        }

        List<CompatibilityRule> allRules = rules.findAll();
        List<CompatibilityConflict> conflicts = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                for (ProductSpec a : byProduct.getOrDefault(ids.get(i), List.of())) {
                    for (ProductSpec b : byProduct.getOrDefault(ids.get(j), List.of())) {
                        if (!a.getSpecType().equalsIgnoreCase(b.getSpecType())) {
                            continue;
                        }

                        for (CompatibilityRule rule : allRules) {
                            boolean match = rule.getSpecType().equalsIgnoreCase(a.getSpecType())
                                    && (
                                    (rule.getValueA().equalsIgnoreCase(a.getSpecValue())
                                            && rule.getValueB().equalsIgnoreCase(b.getSpecValue()))
                                    ||
                                    (rule.getValueA().equalsIgnoreCase(b.getSpecValue())
                                            && rule.getValueB().equalsIgnoreCase(a.getSpecValue()))
                            );

                            if (match) {
                                String key = ids.get(i) + ":" + ids.get(j) + ":" + rule.getReason();
                                if (seen.add(key)) {
                                    conflicts.add(new CompatibilityConflict(
                                            getProduct(ids.get(i)).getName(),
                                            getProduct(ids.get(j)).getName(),
                                            rule.getReason()
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    /**
     * Final checkout is deliberately one transaction. Product rows and the
     * exact selected serial rows are write-locked before any stock mutation.
     * If any validation/save/audit/integration step fails, Spring rolls the
     * database work back.
     */
    @Transactional
    public Sale checkout(SalesCart cart,
                         User actor,
                         String override,
                         boolean paymentConfirmed) {
        if (!paymentConfirmed) {
            throw new IllegalArgumentException("Confirm that payment has been received before completing the sale.");
        }
        if (cart.getLines().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        List<CompatibilityConflict> conflicts = compatibility(cart);
        String normalizedOverride = blank(override);
        if (!conflicts.isEmpty() && normalizedOverride == null) {
            throw new IllegalArgumentException(
                    "Compatibility conflict detected. Enter an authorized override reason to continue."
            );
        }

        // Lock products in a stable order to reduce the chance of deadlocks.
        Map<Integer, Product> lockedProducts = new LinkedHashMap<>();
        for (Integer productId : new TreeSet<>(
                cart.getLines().stream().map(CartLine::getProductId).toList()
        )) {
            Product product = products.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalArgumentException("A product in the cart no longer exists."));
            lockedProducts.put(productId, product);
        }

        Set<Integer> allSelectedSerialIds = new LinkedHashSet<>();
        Map<Integer, BigDecimal> authoritativeUnitPrices = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (CartLine line : cart.getLines()) {
            Product product = lockedProducts.get(line.getProductId());
            BigDecimal authoritativeUnitPrice = validateCheckoutLine(line, product);
            authoritativeUnitPrices.put(line.getProductId(), authoritativeUnitPrice);

            if (product.isSerialTracked()) {
                if (line.getSelectedSerialIds().size() != line.getQuantity()) {
                    throw new IllegalArgumentException(
                            "Select exactly " + line.getQuantity() + " serial number(s) for " + product.getName() + "."
                    );
                }
                for (Integer serialId : line.getSelectedSerialIds()) {
                    if (!allSelectedSerialIds.add(serialId)) {
                        throw new IllegalArgumentException(
                                "A serial number was selected more than once in the cart."
                        );
                    }
                }
            } else if (!line.getSelectedSerialIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "Serial numbers were submitted for a product that is not serial-tracked."
                );
            }

            total = money(total.add(
                    authoritativeUnitPrice.multiply(BigDecimal.valueOf(line.getQuantity()))
            ));
        }

        Map<Integer, SerialNumber> lockedSerials = new HashMap<>();
        if (!allSelectedSerialIds.isEmpty()) {
            List<SerialNumber> locked = serials.findAllForUpdate(allSelectedSerialIds);
            if (locked.size() != allSelectedSerialIds.size()) {
                throw new IllegalArgumentException("One or more selected serial numbers no longer exist.");
            }
            for (SerialNumber serial : locked) {
                lockedSerials.put(serial.getSerialId(), serial);
            }
        }

        // Revalidate each exact physical serial after locking it.
        for (CartLine line : cart.getLines()) {
            Product product = lockedProducts.get(line.getProductId());
            if (!product.isSerialTracked()) {
                continue;
            }

            for (Integer serialId : line.getSelectedSerialIds()) {
                SerialNumber serial = lockedSerials.get(serialId);
                if (serial == null
                        || serial.getProduct() == null
                        || !serial.getProduct().getProductId().equals(product.getProductId())
                        || serial.getCurrentStatus() != SerialNumber.CurrentStatus.in_stock
                        || serial.getSale() != null) {
                    throw new IllegalArgumentException(
                            "Serial selection changed while checking out. Re-select the serial numbers and try again."
                    );
                }
            }
        }

        Sale sale = new Sale();
        sale.setSoldBy(actor);
        sale.setAmount(total);
        sale = sales.save(sale);
        sale.setSaleCode("SALE-" + String.format("%06d", sale.getSaleId()));
        sale = sales.save(sale);

        PickTicket ticket = new PickTicket();
        ticket.setSale(sale);
        ticket.setStatus(PickTicket.Status.pending);
        ticket = tickets.save(ticket);
        ticket.setTicketCode("PICK-" + String.format("%06d", ticket.getTicketId()));
        ticket = tickets.save(ticket);

        for (CartLine line : cart.getLines()) {
            Product product = lockedProducts.get(line.getProductId());

            int oldStock = product.getStockCount();
            product.setStockCount(oldStock - line.getQuantity());
            products.save(product);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(line.getQuantity());
            saleItem.setPriceAtSale(authoritativeUnitPrices.get(line.getProductId()));
            saleItem.setDiscountAmount(line.getDiscountAmount());
            saleItem.setDiscountReason(blank(line.getDiscountReason()));
            saleItem.setCompatibilityOverrideReason(conflicts.isEmpty() ? null : normalizedOverride);
            saleItems.save(saleItem);

            PickTicketItem pickItem = new PickTicketItem();
            pickItem.setTicket(ticket);
            pickItem.setProduct(product);
            pickItem.setQuantity(line.getQuantity());
            ticketItems.save(pickItem);

            if (product.isSerialTracked()) {
                for (Integer serialId : line.getSelectedSerialIds()) {
                    SerialNumber serial = lockedSerials.get(serialId);
                    serial.setSale(sale);
                    serial.setCurrentStatus(SerialNumber.CurrentStatus.sold);
                    serials.save(serial);
                }
            }

            audit(
                    actor,
                    "SALE_STOCK",
                    "product",
                    product.getProductId(),
                    "{\"stock\":" + oldStock + "}",
                    "{\"stock\":" + product.getStockCount() + "}"
            );
        }

        if (!conflicts.isEmpty()) {
            audit(
                    actor,
                    "SALE_COMPATIBILITY_OVERRIDE",
                    "sale",
                    sale.getSaleId(),
                    null,
                    "{\"reason\":" + jsonString(normalizedOverride) + "}"
            );
        }

        audit(
                actor,
                "SALE_COMPLETED",
                "sale",
                sale.getSaleId(),
                null,
                "{\"saleCode\":" + jsonString(sale.getSaleCode())
                        + ",\"subtotal\":" + cart.getSubtotal()
                        + ",\"discount\":" + cart.getDiscountTotal()
                        + ",\"amount\":" + total + "}"
        );

        // Function 3 already has a working recalculation service. Trigger it
        // after the Sale/SaleItems/stock changes are in the same transaction.
        stockMonitoring.recalculateAll();

        return sale;
    }

    public Sale getSale(Integer id) {
        return sales.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found."));
    }

    public List<SaleItem> items(Integer id) {
        return saleItems.findBySale_SaleId(id);
    }

    public PickTicket ticketForSale(Integer id) {
        return tickets.findFirstBySale_SaleId(id);
    }

    public Map<Integer, List<String>> serialValuesForSale(Integer saleId) {
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        for (SerialNumber serial : serials.findBySaleIdWithProduct(saleId)) {
            result.computeIfAbsent(
                    serial.getProduct().getProductId(),
                    ignored -> new ArrayList<>()
            ).add(serial.getSerialValue());
        }
        return result;
    }

    public BigDecimal receiptSubtotal(List<SaleItem> items) {
        return money(items.stream()
                .map(SaleItem::getOriginalLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal receiptDiscount(List<SaleItem> items) {
        return money(items.stream()
                .map(SaleItem::getLineDiscountTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal validateCheckoutLine(CartLine line, Product product) {
        if (line.getQuantity() == null || line.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        if (product.getStockCount() == null || line.getQuantity() > product.getStockCount()) {
            int stock = product.getStockCount() == null ? 0 : product.getStockCount();
            throw new IllegalArgumentException(
                    product.getName() + " now has only " + stock + " unit(s) available."
            );
        }

        BigDecimal currentCatalogPrice = money(product.getPrice());
        BigDecimal reviewedCatalogPrice = line.getCatalogPrice();
        if (reviewedCatalogPrice == null || reviewedCatalogPrice.compareTo(currentCatalogPrice) != 0) {
            throw new IllegalArgumentException(
                    "The price of " + product.getName()
                            + " changed while the cart was open. Update the cart line before checkout."
            );
        }

        BigDecimal discount = normalizeDiscount(line.getDiscountAmount());
        validateDiscount(currentCatalogPrice, discount, line.getDiscountReason(), product.getName());

        BigDecimal authoritativeUnitPrice = money(currentCatalogPrice.subtract(discount));
        if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(authoritativeUnitPrice) != 0) {
            throw new IllegalArgumentException(
                    "The calculated price for " + product.getName()
                            + " changed. Update the cart line before checkout."
            );
        }

        if (product.isSerialTracked()) {
            long availableSerials = serials
                    .countByProduct_ProductIdAndCurrentStatusAndSaleIsNull(
                            product.getProductId(),
                            SerialNumber.CurrentStatus.in_stock
                    );
            if (line.getQuantity() > availableSerials) {
                throw new IllegalArgumentException(
                        "Only " + availableSerials + " serial-numbered unit(s) are currently available for "
                                + product.getName() + "."
                );
            }
        }

        return authoritativeUnitPrice;
    }

    private void validateSellableQuantity(Product product, int quantity) {
        if (quantity < 1 || product.getStockCount() == null || quantity > product.getStockCount()) {
            int stock = product.getStockCount() == null ? 0 : product.getStockCount();
            throw new IllegalArgumentException("Only " + stock + " units are available.");
        }

        if (product.isSerialTracked()) {
            long availableSerials = serials
                    .countByProduct_ProductIdAndCurrentStatusAndSaleIsNull(
                            product.getProductId(),
                            SerialNumber.CurrentStatus.in_stock
                    );
            if (quantity > availableSerials) {
                throw new IllegalArgumentException(
                        "Only " + availableSerials + " serial-numbered unit(s) are available for "
                                + product.getName() + "."
                );
            }
        }
    }

    private BigDecimal normalizeDiscount(BigDecimal discountAmount) {
        return money(discountAmount == null ? BigDecimal.ZERO : discountAmount);
    }

    private void validateDiscount(BigDecimal catalogPrice,
                                  BigDecimal discount,
                                  String reason,
                                  String productName) {
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }
        if (discount.compareTo(catalogPrice) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the unit price.");
        }
        if (discount.signum() > 0 && blank(reason) == null) {
            throw new IllegalArgumentException("Enter a discount reason for " + productName + ".");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private CartLine requireLine(SalesCart cart, int index) {
        if (index < 0 || index >= cart.getLines().size()) {
            throw new IllegalArgumentException("Cart line not found.");
        }
        return cart.getLines().get(index);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private void audit(User user,
                       String action,
                       String table,
                       Integer id,
                       String oldValue,
                       String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setActionType(action);
        log.setTableName(table);
        log.setRecordId(id);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        audit.save(log);
    }
}
