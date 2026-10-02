package com.sliit.sparepartshub.inventory.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.PickTicket;
import com.sliit.sparepartshub.entity.PickTicketItem;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.entity.StorageLocation;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.inventory.dto.CreateProductForm;
import com.sliit.sparepartshub.inventory.dto.EditProductForm;
import com.sliit.sparepartshub.inventory.dto.InventoryProductRow;
import com.sliit.sparepartshub.inventory.dto.LocationForm;
import com.sliit.sparepartshub.inventory.dto.PickTicketLineView;
import com.sliit.sparepartshub.inventory.repository.InventoryAuditLogRepository;
import com.sliit.sparepartshub.inventory.repository.InventoryPickTicketItemRepository;
import com.sliit.sparepartshub.inventory.repository.InventoryPickTicketRepository;
import com.sliit.sparepartshub.inventory.repository.InventoryProductRepository;
import com.sliit.sparepartshub.inventory.repository.InventorySerialNumberRepository;
import com.sliit.sparepartshub.inventory.repository.InventoryStorageLocationRepository;
import com.sliit.sparepartshub.stockmonitoring.service.StockMonitoringService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class InventoryService {
    private final InventoryPickTicketRepository tickets;
    private final InventoryPickTicketItemRepository items;
    private final InventoryProductRepository products;
    private final InventoryStorageLocationRepository locations;
    private final InventorySerialNumberRepository serials;
    private final InventoryAuditLogRepository auditLogs;
    private final StockMonitoringService stockMonitoring;

    public InventoryService(InventoryPickTicketRepository tickets,
                            InventoryPickTicketItemRepository items,
                            InventoryProductRepository products,
                            InventoryStorageLocationRepository locations,
                            InventorySerialNumberRepository serials,
                            InventoryAuditLogRepository auditLogs,
                            StockMonitoringService stockMonitoring) {
        this.tickets = tickets;
        this.items = items;
        this.products = products;
        this.locations = locations;
        this.serials = serials;
        this.auditLogs = auditLogs;
        this.stockMonitoring = stockMonitoring;
    }

    public long totalProductCount() {
        return products.count();
    }

    public long pendingCount() {
        return tickets.countByStatus(PickTicket.Status.pending);
    }

    public long partialCount() {
        return tickets.countByStatus(PickTicket.Status.partially_completed);
    }

    public long completedToday() {
        LocalDate today = LocalDate.now();
        return tickets.countByFulfilledAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    public long locationCount() {
        return locations.count();
    }

    public long lowStockCount() {
        return products.countLowStockProducts();
    }

    public long outOfStockCount() {
        return products.countByStockCount(0);
    }

    public long serialTrackedProductCount() {
        return products.countBySerialTrackedTrue();
    }

    public List<Product> lowStock() {
        List<Product> all = products.findLowStockProducts();
        return all.size() <= 8 ? all : all.subList(0, 8);
    }

    public List<PickTicket> recentCompletedTickets() {
        return tickets.findTop5ByStatusInOrderByFulfilledAtDesc(
                EnumSet.of(PickTicket.Status.completed,
                        PickTicket.Status.partially_completed,
                        PickTicket.Status.exception)
        );
    }

    public List<PickTicket> allTickets() {
        return tickets.findAllByOrderByCreatedAtDesc();
    }

    public PickTicket findByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Enter a pick ticket code.");
        }
        return tickets.findByTicketCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Pick ticket not found."));
    }

    public PickTicket findProcessableByCode(String code) {
        PickTicket ticket = findByCode(code);
        if (ticket.getStatus() != PickTicket.Status.pending) {
            throw new IllegalArgumentException(
                    "Pick ticket " + ticket.getTicketCode() + " has already been processed (" +
                            ticket.getStatus().name().replace('_', ' ') + ")."
            );
        }
        return ticket;
    }

    public PickTicket getTicket(Integer id) {
        return tickets.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pick ticket not found."));
    }

    public List<PickTicketItem> ticketItems(Integer id) {
        return items.findByTicket_TicketIdOrderByPickTicketItemId(id);
    }

    public List<PickTicketLineView> ticketLines(Integer id) {
        PickTicket ticket = getTicket(id);
        List<PickTicketLineView> rows = new ArrayList<>();
        for (PickTicketItem line : ticketItems(id)) {
            Integer productId = line.getProduct().getProductId();
            List<SerialNumber> assigned = serials
                    .findBySale_SaleIdAndProduct_ProductIdOrderBySerialIdAsc(
                            ticket.getSale().getSaleId(), productId
                    );
            rows.add(new PickTicketLineView(line, line.getProduct().isSerialTracked(), assigned));
        }
        return rows;
    }

    public List<Product> products() {
        return products.findAllByOrderByNameAsc();
    }

    public List<InventoryProductRow> productRows() {
        List<InventoryProductRow> rows = new ArrayList<>();
        for (Product product : products.findAllByOrderByNameAsc()) {
            long total = serials.countByProduct_ProductId(product.getProductId());
            long available = serials.countByProduct_ProductIdAndCurrentStatus(
                    product.getProductId(), SerialNumber.CurrentStatus.in_stock
            );
            rows.add(new InventoryProductRow(product, total, available));
        }
        return rows;
    }

    public List<StorageLocation> locations() {
        return locations.findAllByOrderByLocationCodeAsc();
    }

    public List<String> productCategories() {
        return products.findDistinctCategories();
    }

    public List<String> productBrands() {
        return products.findDistinctBrands();
    }

    public Product getProduct(Integer id) {
        return products.findByProductId(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    public EditProductForm editProductForm(Integer id) {
        Product product = getProduct(id);
        EditProductForm form = new EditProductForm();
        form.setProductCode(product.getProductCode());
        form.setName(product.getName());
        form.setCategory(product.getCategory());
        form.setBrand(product.getBrand());
        form.setPrice(product.getPrice());
        form.setReorderLevel(product.getReorderLevel());
        form.setWarrantyPeriodMonths(product.getWarrantyPeriodMonths());
        form.setSerialTracked(product.isSerialTracked());
        form.setLocationId(product.getLocation() == null ? null : product.getLocation().getLocationId());
        return form;
    }

    @Transactional
    public Product createProduct(CreateProductForm form, User actor) {
        validateCreateProduct(form);

        Product product = new Product();
        product.setProductCode(clean(form.getProductCode()).toUpperCase(Locale.ROOT));
        product.setName(clean(form.getName()));
        product.setCategory(clean(form.getCategory()));
        product.setBrand(clean(form.getBrand()));
        product.setPrice(form.getPrice());
        product.setStockCount(form.isSerialTracked() ? 0 : form.getInitialStock());
        product.setReorderLevel(form.getReorderLevel());
        product.setUrgencyScore(BigDecimal.ZERO);
        product.setWarrantyPeriodMonths(form.getWarrantyPeriodMonths());
        product.setSerialTracked(form.isSerialTracked());
        product.setLocation(resolveLocation(form.getLocationId()));

        try {
            product = products.saveAndFlush(product);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductValidationException(
                    "productCode",
                    "Product could not be created because the product code is already in use."
            );
        }

        audit(actor, "PRODUCT_CREATED", "product", product.getProductId(), null, productSnapshot(product));
        stockMonitoring.recalculateProduct(product.getProductId());
        return product;
    }

    @Transactional
    public Product updateProduct(Integer id, EditProductForm form, User actor) {
        Product product = getProduct(id);
        validateEditProduct(form, product);
        String oldValue = productSnapshot(product);

        product.setProductCode(clean(form.getProductCode()).toUpperCase(Locale.ROOT));
        product.setName(clean(form.getName()));
        product.setCategory(clean(form.getCategory()));
        product.setBrand(clean(form.getBrand()));
        product.setPrice(form.getPrice());
        product.setReorderLevel(form.getReorderLevel());
        product.setWarrantyPeriodMonths(form.getWarrantyPeriodMonths());
        product.setSerialTracked(form.isSerialTracked());
        product.setLocation(resolveLocation(form.getLocationId()));

        try {
            product = products.saveAndFlush(product);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductValidationException(
                    "productCode",
                    "Product could not be updated because the product code is already in use."
            );
        }

        audit(actor, "PRODUCT_UPDATED", "product", product.getProductId(), oldValue, productSnapshot(product));
        stockMonitoring.recalculateProduct(product.getProductId());
        return product;
    }

    public StorageLocation getLocation(Integer id) {
        return locations.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage location not found."));
    }

    public Set<Integer> usedLocationIds() {
        return new LinkedHashSet<>(products.findUsedLocationIds());
    }

    @Transactional
    public String deleteLocation(Integer locationId, User actor) {
        if (locationId == null || locationId <= 0) {
            throw new IllegalArgumentException("Storage location not found.");
        }
        StorageLocation location = getLocation(locationId);
        String code = location.getLocationCode();
        if (products.existsByLocation_LocationId(locationId)) {
            throw new IllegalArgumentException("Storage location " + code
                    + " cannot be deleted because one or more products are assigned to it. Move or unassign those products first.");
        }
        String oldValue = locationJson(location);
        try {
            locations.delete(location);
            locations.flush();
        } catch (DataIntegrityViolationException ex) {
            // Throw out of the transactional boundary: never commit after a failed flush.
            throw new IllegalArgumentException("Storage location cannot be deleted because it is currently assigned to a product.", ex);
        }
        audit(actor, "LOCATION_DELETED", "storage_location", locationId, oldValue, null);
        return code;
    }

    public LocationForm locationForm(Integer id) {
        StorageLocation location = getLocation(id);
        LocationForm form = new LocationForm();
        form.setLocationCode(location.getLocationCode());
        form.setAisle(location.getAisle());
        form.setShelf(location.getShelf());
        form.setBin(location.getBin());
        return form;
    }

    @Transactional
    public StorageLocation createLocation(LocationForm form, User actor) {
        validateLocationForm(form, null);
        StorageLocation location = new StorageLocation();
        applyLocationForm(location, form);
        location = locations.save(location);
        audit(actor, "LOCATION_CREATED", "storage_location", location.getLocationId(), null,
                locationJson(location));
        return location;
    }

    @Transactional
    public StorageLocation updateLocation(Integer id, LocationForm form, User actor) {
        StorageLocation location = getLocation(id);
        validateLocationForm(form, id);
        String oldValue = locationJson(location);
        applyLocationForm(location, form);
        location = locations.save(location);
        audit(actor, "LOCATION_UPDATED", "storage_location", location.getLocationId(), oldValue,
                locationJson(location));
        return location;
    }

    @Transactional
    public void updateReorderLevel(Integer productId, Integer reorderLevel, User actor) {
        if (reorderLevel == null || reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level must be zero or greater.");
        }
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        int old = product.getReorderLevel() == null ? 0 : product.getReorderLevel();
        product.setReorderLevel(reorderLevel);
        products.save(product);
        audit(actor, "REORDER_LEVEL_UPDATED", "product", product.getProductId(),
                "{\"reorderLevel\":" + old + "}",
                "{\"reorderLevel\":" + reorderLevel + "}");
        stockMonitoring.recalculateProduct(product.getProductId());
    }

    /**
     * Sales checkout is the source of truth for the stock deduction: it deducts
     * the requested quantity when the Sale/PickTicket transaction commits.
     * Warehouse picking MUST NOT deduct it again. If the warehouse can only
     * fulfill part of a ticket, the unpicked shortfall is returned to stock so
     * the net reduction equals the quantity physically fulfilled.
     */
    @Transactional
    public void completeTicket(Integer ticketId,
                               Map<Integer, Integer> pickedByItem,
                               Map<Integer, String> pickedSerialTextByItem,
                               User actor) {
        PickTicket ticket = getTicket(ticketId);
        if (ticket.getStatus() != PickTicket.Status.pending) {
            throw new IllegalArgumentException("Only pending tickets can be processed.");
        }

        List<PickTicketItem> ticketItems = ticketItems(ticketId);
        if (ticketItems.isEmpty()) {
            throw new IllegalArgumentException("This pick ticket has no items.");
        }

        boolean anyPicked = false;
        boolean allPicked = true;

        for (PickTicketItem line : ticketItems) {
            Integer lineId = line.getPickTicketItemId();
            if (!pickedByItem.containsKey(lineId)) {
                throw new IllegalArgumentException("Enter a picked quantity for every line.");
            }

            int requested = line.getQuantity();
            Integer pickedValue = pickedByItem.get(lineId);
            int picked = pickedValue == null ? -1 : pickedValue;
            if (picked < 0 || picked > requested) {
                throw new IllegalArgumentException(
                        "Picked quantity for " + line.getProduct().getName() +
                                " must be between 0 and " + requested + "."
                );
            }

            Product product = products.findById(line.getProduct().getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found."));

            boolean serialTracked = product.isSerialTracked();
            List<SerialNumber> assignedSerials = serials
                    .findBySale_SaleIdAndProduct_ProductIdOrderBySerialIdAsc(
                            ticket.getSale().getSaleId(), product.getProductId()
                    );

            Set<String> pickedSerialValues = Set.of();
            if (serialTracked) {
                if (assignedSerials.size() != requested) {
                    throw new IllegalStateException(
                            product.getName() + " is serial-tracked, but the sale has " + assignedSerials.size() +
                                    " assigned serial(s) for " + requested + " requested unit(s). " +
                                    "Correct the sale serial assignment before confirming this pick."
                    );
                }
                pickedSerialValues = validatePickedSerials(
                        product,
                        assignedSerials,
                        picked,
                        pickedSerialTextByItem.get(lineId)
                );
            }

            line.setPickedQuantity(picked);
            items.save(line);

            anyPicked |= picked > 0;
            allPicked &= picked == requested;

            int shortfall = requested - picked;
            if (shortfall > 0) {
                int oldStock = product.getStockCount();
                product.setStockCount(oldStock + shortfall);
                products.save(product);
                stockMonitoring.recalculateProduct(product.getProductId());

                audit(actor, "PICK_STOCK_RECONCILE", "product", product.getProductId(),
                        "{\"stock\":" + oldStock + "}",
                        "{\"stock\":" + product.getStockCount() + ",\"returned\":" + shortfall + "}");

                if (serialTracked) {
                    releaseUnpickedSerials(assignedSerials, pickedSerialValues, actor);
                }
            }
        }

        ticket.setFulfilledBy(actor);
        ticket.setFulfilledAt(LocalDateTime.now());
        if (allPicked) {
            ticket.setStatus(PickTicket.Status.completed);
        } else if (anyPicked) {
            ticket.setStatus(PickTicket.Status.partially_completed);
        } else {
            ticket.setStatus(PickTicket.Status.exception);
        }
        tickets.save(ticket);

        String action = switch (ticket.getStatus()) {
            case completed -> "PICK_COMPLETED";
            case partially_completed -> "PICK_PARTIAL";
            case exception -> "PICK_EXCEPTION";
            default -> "PICK_PROCESSED";
        };
        audit(actor, action, "pick_ticket", ticket.getTicketId(),
                "{\"status\":\"pending\"}",
                "{\"status\":\"" + ticket.getStatus().name() + "\"}");
    }

    @Transactional
    public void assignLocation(Integer productId, Integer locationId, User actor) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        StorageLocation location = locations.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found."));
        String old = product.getLocation() == null ? null : product.getLocation().getLocationCode();
        product.setLocation(location);
        products.save(product);
        audit(actor, "LOCATION_ASSIGN", "product", product.getProductId(),
                "{\"location\":" + jsonNullable(old) + "}",
                "{\"location\":\"" + jsonEscape(location.getLocationCode()) + "\"}");
    }

    private Set<String> validatePickedSerials(Product product,
                                              List<SerialNumber> assignedSerials,
                                              int pickedQuantity,
                                              String rawSerials) {
        Set<String> entered = parseSerialValues(rawSerials);
        if (entered.size() != pickedQuantity) {
            throw new IllegalArgumentException(
                    "Enter exactly " + pickedQuantity + " serial number(s) physically picked for " +
                            product.getName() + "."
            );
        }

        Set<String> assigned = new LinkedHashSet<>();
        for (SerialNumber serial : assignedSerials) {
            assigned.add(normalizeSerial(serial.getSerialValue()));
        }
        for (String enteredSerial : entered) {
            if (!assigned.contains(enteredSerial)) {
                throw new IllegalArgumentException(
                        "Serial " + enteredSerial + " is not assigned to this sale for " + product.getName() + "."
                );
            }
        }
        return entered;
    }

    private Set<String> parseSerialValues(String raw) {
        Set<String> values = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return values;
        for (String value : raw.split("[\\r\\n,;]+")) {
            if (!value.isBlank()) {
                String normalized = normalizeSerial(value);
                if (!values.add(normalized)) {
                    throw new IllegalArgumentException("Duplicate serial number entered: " + value.trim());
                }
            }
        }
        return values;
    }

    private String normalizeSerial(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void releaseUnpickedSerials(List<SerialNumber> assignedSerials,
                                        Set<String> pickedSerialValues,
                                        User actor) {
        for (SerialNumber serial : assignedSerials) {
            if (pickedSerialValues.contains(normalizeSerial(serial.getSerialValue()))) {
                continue;
            }
            String saleCode = serial.getSale() == null ? null : serial.getSale().getSaleCode();
            serial.setSale(null);
            serial.setCurrentStatus(SerialNumber.CurrentStatus.in_stock);
            serials.save(serial);
            audit(actor, "PICK_SERIAL_RELEASE", "serial_number", serial.getSerialId(),
                    "{\"status\":\"sold\",\"sale\":" + jsonNullable(saleCode) + "}",
                    "{\"status\":\"in_stock\",\"sale\":null}");
        }
    }

    private void validateCreateProduct(CreateProductForm form) {
        if (form == null) {
            throw new ProductValidationException("form", "Product details are required.");
        }
        validateProductFields(
                form.getProductCode(), form.getName(), form.getCategory(), form.getBrand(),
                form.getPrice(), form.getReorderLevel(), form.getWarrantyPeriodMonths()
        );
        String code = clean(form.getProductCode()).toUpperCase(Locale.ROOT);
        if (products.existsByProductCodeIgnoreCase(code)) {
            throw new ProductValidationException("productCode", "Product code " + code + " already exists.");
        }
        if (form.getInitialStock() == null || form.getInitialStock() < 0) {
            throw new ProductValidationException("initialStock", "Initial stock cannot be negative.");
        }
        if (form.isSerialTracked() && form.getInitialStock() > 0) {
            throw new ProductValidationException(
                    "initialStock",
                    "Serial-tracked products must start at zero stock and receive units with serial numbers."
            );
        }
        resolveLocation(form.getLocationId());
    }

    private void validateEditProduct(EditProductForm form, Product existing) {
        if (form == null) {
            throw new ProductValidationException("form", "Product details are required.");
        }
        validateProductFields(
                form.getProductCode(), form.getName(), form.getCategory(), form.getBrand(),
                form.getPrice(), form.getReorderLevel(), form.getWarrantyPeriodMonths()
        );
        String code = clean(form.getProductCode()).toUpperCase(Locale.ROOT);
        if (products.existsByProductCodeIgnoreCaseAndProductIdNot(code, existing.getProductId())) {
            throw new ProductValidationException("productCode", "Product code " + code + " already exists.");
        }

        if (!existing.isSerialTracked() && form.isSerialTracked() && existing.getStockCount() != null
                && existing.getStockCount() > 0) {
            throw new ProductValidationException(
                    "serialTracked",
                    "Cannot enable serial tracking while this product has stock. Reduce/reconcile stock first."
            );
        }
        if (existing.isSerialTracked() && !form.isSerialTracked()
                && serials.countByProduct_ProductId(existing.getProductId()) > 0) {
            throw new ProductValidationException(
                    "serialTracked",
                    "Cannot disable serial tracking while serial-number records exist for this product."
            );
        }
        resolveLocation(form.getLocationId());
    }

    private void validateProductFields(String codeValue,
                                       String nameValue,
                                       String categoryValue,
                                       String brandValue,
                                       BigDecimal price,
                                       Integer reorderLevel,
                                       Integer warrantyMonths) {
        String code = clean(codeValue);
        String name = clean(nameValue);
        String category = clean(categoryValue);
        String brand = clean(brandValue);

        if (code == null) throw new ProductValidationException("productCode", "Product code is required.");
        if (!code.matches("[A-Za-z0-9_-]{2,20}")) {
            throw new ProductValidationException(
                    "productCode",
                    "Product code must be 2-20 letters, numbers, dashes or underscores."
            );
        }
        if (name == null) throw new ProductValidationException("name", "Product name is required.");
        if (name.length() > 50) throw new ProductValidationException("name", "Product name must be 50 characters or fewer.");
        if (category == null) throw new ProductValidationException("category", "Category is required.");
        if (category.length() > 30) throw new ProductValidationException("category", "Category must be 30 characters or fewer.");
        if (brand == null) throw new ProductValidationException("brand", "Brand is required.");
        if (brand.length() > 30) throw new ProductValidationException("brand", "Brand must be 30 characters or fewer.");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductValidationException("price", "Selling price must be greater than zero.");
        }
        if (price.scale() > 2 || price.precision() - price.scale() > 8) {
            throw new ProductValidationException("price", "Selling price must fit the database amount format with at most 2 decimal places.");
        }
        if (reorderLevel == null || reorderLevel < 0) {
            throw new ProductValidationException("reorderLevel", "Reorder level cannot be negative.");
        }
        if (warrantyMonths != null && warrantyMonths < 0) {
            throw new ProductValidationException("warrantyPeriodMonths", "Warranty period cannot be negative.");
        }
    }

    private StorageLocation resolveLocation(Integer locationId) {
        if (locationId == null) return null;
        return locations.findById(locationId)
                .orElseThrow(() -> new ProductValidationException("locationId", "Storage location could not be found."));
    }

    private String productSnapshot(Product product) {
        String locationCode = product.getLocation() == null ? null : product.getLocation().getLocationCode();
        return "{\"productCode\":\"" + jsonEscape(product.getProductCode())
                + "\",\"name\":\"" + jsonEscape(product.getName())
                + "\",\"category\":\"" + jsonEscape(product.getCategory())
                + "\",\"brand\":\"" + jsonEscape(product.getBrand())
                + "\",\"price\":" + product.getPrice()
                + ",\"stockCount\":" + product.getStockCount()
                + ",\"reorderLevel\":" + product.getReorderLevel()
                + ",\"warrantyPeriodMonths\":" + (product.getWarrantyPeriodMonths() == null ? "null" : product.getWarrantyPeriodMonths())
                + ",\"serialTracked\":" + product.isSerialTracked()
                + ",\"location\":" + jsonNullable(locationCode)
                + "}";
    }

    private void validateLocationForm(LocationForm form, Integer existingId) {
        if (form == null) {
            throw new IllegalArgumentException("Storage location details are required.");
        }
        String code = clean(form.getLocationCode());
        String aisle = clean(form.getAisle());
        String shelf = clean(form.getShelf());
        String bin = clean(form.getBin());

        if (code == null) throw new IllegalArgumentException("Location code is required.");
        if (aisle == null) throw new IllegalArgumentException("Aisle is required.");
        if (shelf == null) throw new IllegalArgumentException("Shelf is required.");
        if (bin == null) throw new IllegalArgumentException("Bin is required.");
        if (code.length() > 20) throw new IllegalArgumentException("Location code must be 20 characters or fewer.");
        if (aisle.length() > 30 || shelf.length() > 30 || bin.length() > 30) {
            throw new IllegalArgumentException("Aisle, shelf and bin must be 30 characters or fewer.");
        }

        boolean duplicate = existingId == null
                ? locations.existsByLocationCodeIgnoreCase(code)
                : locations.existsByLocationCodeIgnoreCaseAndLocationIdNot(code, existingId);
        if (duplicate) {
            throw new IllegalArgumentException("Storage location code already exists.");
        }
    }

    private void applyLocationForm(StorageLocation location, LocationForm form) {
        location.setLocationCode(clean(form.getLocationCode()).toUpperCase());
        location.setAisle(clean(form.getAisle()));
        location.setShelf(clean(form.getShelf()));
        location.setBin(clean(form.getBin()));
    }

    private String locationJson(StorageLocation location) {
        return "{\"locationCode\":\"" + jsonEscape(location.getLocationCode()) +
                "\",\"aisle\":\"" + jsonEscape(location.getAisle()) +
                "\",\"shelf\":\"" + jsonEscape(location.getShelf()) +
                "\",\"bin\":\"" + jsonEscape(location.getBin()) + "\"}";
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String jsonNullable(String value) {
        return value == null ? "null" : "\"" + jsonEscape(value) + "\"";
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private void audit(User user,
                       String action,
                       String table,
                       Integer id,
                       String oldValue,
                       String newValue) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setActionType(action);
        log.setTableName(table);
        log.setRecordId(id);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        auditLogs.save(log);
    }
}
