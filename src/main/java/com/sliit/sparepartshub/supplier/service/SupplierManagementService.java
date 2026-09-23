package com.sliit.sparepartshub.supplier.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.PartnershipRequest;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import com.sliit.sparepartshub.entity.RestockOffer;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.entity.Supplier;
import com.sliit.sparepartshub.entity.SupplierProduct;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.repository.AuditLogRepository;
import com.sliit.sparepartshub.reporting.repository.SupplierRepository;
import com.sliit.sparepartshub.reporting.service.CatalogStorageService;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import com.sliit.sparepartshub.stockmonitoring.service.StockReceiptIntegrationService;
import com.sliit.sparepartshub.supplier.dto.CreatePurchaseOrderForm;
import com.sliit.sparepartshub.supplier.dto.CreateSupplierRequest;
import com.sliit.sparepartshub.supplier.dto.ReceiveShipmentForm;
import com.sliit.sparepartshub.supplier.dto.SupplierComparisonRow;
import com.sliit.sparepartshub.supplier.dto.SupplierProductAssignmentForm;
import com.sliit.sparepartshub.supplier.dto.SupplierProductRowForm;
import com.sliit.sparepartshub.supplier.repository.PartnershipRequestRepository;
import com.sliit.sparepartshub.supplier.repository.ProductRepository;
import com.sliit.sparepartshub.supplier.repository.PurchaseOrderItemRepository;
import com.sliit.sparepartshub.supplier.repository.PurchaseOrderRepository;
import com.sliit.sparepartshub.supplier.repository.RestockOfferRepository;
import com.sliit.sparepartshub.supplier.repository.SerialNumberRepository;
import com.sliit.sparepartshub.supplier.repository.SupplierProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SupplierManagementService {

    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String SUPPLIER_CODE_PATTERN = "^[A-Za-z0-9_-]{2,20}$";
    private static final String CONTACT_PATTERN = "^[0-9+()\\-\\s]{5,20}$";

    private final SupplierRepository suppliers;
    private final PartnershipRequestRepository partnerships;
    private final ProductRepository products;
    private final SupplierProductRepository terms;
    private final RestockOfferRepository offers;
    private final PurchaseOrderRepository pos;
    private final PurchaseOrderItemRepository poItems;
    private final SerialNumberRepository serials;
    private final AuditLogRepository audits;
    private final RestockSuggestionRepository suggestions;
    private final StockReceiptIntegrationService receiptIntegration;
    private final CatalogStorageService catalogStorage;
    private final PasswordEncoder passwordEncoder;

    public SupplierManagementService(
            SupplierRepository suppliers,
            PartnershipRequestRepository partnerships,
            ProductRepository products,
            SupplierProductRepository terms,
            RestockOfferRepository offers,
            PurchaseOrderRepository pos,
            PurchaseOrderItemRepository poItems,
            SerialNumberRepository serials,
            AuditLogRepository audits,
            RestockSuggestionRepository suggestions,
            StockReceiptIntegrationService receiptIntegration,
            CatalogStorageService catalogStorage,
            PasswordEncoder passwordEncoder) {
        this.suppliers = suppliers;
        this.partnerships = partnerships;
        this.products = products;
        this.terms = terms;
        this.offers = offers;
        this.pos = pos;
        this.poItems = poItems;
        this.serials = serials;
        this.audits = audits;
        this.suggestions = suggestions;
        this.receiptIntegration = receiptIntegration;
        this.catalogStorage = catalogStorage;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Supplier> getSuppliers() {
        return suppliers.findAllByOrderByNameAsc();
    }

    public Supplier getSupplier(Integer id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
    }

    /**
     * Creates a real Supplier Portal account from the Admin Supplier Management
     * area. Supplier persistence and audit persistence participate in the same
     * transaction so an unaudited supplier is not left behind if the audit write
     * fails.
     */
    @Transactional
    public Supplier createSupplier(CreateSupplierRequest form, User actor) {
        if (form == null) {
            throw new IllegalArgumentException("Supplier form is required.");
        }

        String supplierCode = validateSupplierCode(form.getSupplierCode(), true, null);
        String name = validateName(form.getName());
        String email = validateEmail(form.getEmail(), null);
        String contact = validateContact(form.getContact());
        validatePasswords(form.getTemporaryPassword(), form.getConfirmPassword());

        Supplier supplier = new Supplier();
        supplier.setSupplierCode(supplierCode);
        supplier.setName(name);
        supplier.setEmail(email);
        supplier.setContact(contact);
        supplier.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
        supplier.setActive(form.isActive());

        try {
            supplier = suppliers.saveAndFlush(supplier);
        } catch (DataIntegrityViolationException ex) {
            // Email is also protected by a DB UNIQUE constraint. Application-level
            // checks above give better messages, while this handles a concurrent race.
            throw new IllegalArgumentException(
                    "Supplier could not be created because the email or another unique value is already in use.");
        }

        log(
                actor,
                "SUPPLIER_CREATED",
                "supplier",
                supplier.getSupplierId(),
                null,
                supplierSnapshot(supplier)
        );

        return supplier;
    }

    @Transactional
    public void updateSupplier(
            Integer id,
            String code,
            String name,
            String contact,
            String email,
            User actor) {

        Supplier supplier = getSupplier(id);
        String oldValue = supplierSnapshot(supplier);

        String cleanCode = validateSupplierCode(code, false, id);
        String cleanName = validateName(name);
        String cleanContact = validateContact(contact);
        String cleanEmail = validateEmail(email, id);

        supplier.setSupplierCode(cleanCode);
        supplier.setName(cleanName);
        supplier.setContact(cleanContact);
        supplier.setEmail(cleanEmail);

        try {
            suppliers.saveAndFlush(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Supplier could not be updated because the email or another unique value is already in use.");
        }

        log(
                actor,
                "SUPPLIER_UPDATE",
                "supplier",
                id,
                oldValue,
                supplierSnapshot(supplier)
        );
    }

    @Transactional
    public void setSupplierActive(Integer id, boolean active, User actor) {
        Supplier supplier = getSupplier(id);
        boolean old = supplier.isActive();

        supplier.setActive(active);
        suppliers.save(supplier);

        log(
                actor,
                "SUPPLIER_STATUS",
                "supplier",
                id,
                "{\"active\":" + old + "}",
                "{\"active\":" + active + "}"
        );
    }

    /**
     * Permanently removes only suppliers that have never produced historical
     * procurement activity. Product assignments are current configuration and
     * can be discarded; purchase orders, offers and partnership requests are
     * retained as business history and therefore block a hard delete.
     */
    @Transactional
    public void deleteSupplier(Integer id, User actor) {
        Supplier supplier = getSupplier(id);
        validateSupplierCanBeDeleted(id);

        String oldValue = supplierSnapshot(supplier);

        List<SupplierProduct> supplierTerms =
                terms.findBySupplier_SupplierIdOrderByProduct_Name(id);
        if (!supplierTerms.isEmpty()) {
            terms.deleteAll(supplierTerms);
            terms.flush();
        }

        try {
            suppliers.delete(supplier);
            suppliers.flush();
        } catch (DataIntegrityViolationException ex) {
            // The explicit history checks above provide the normal business
            // decision. The database foreign keys remain the final guard for
            // a concurrent history record created between validation/delete.
            throw new IllegalArgumentException(
                    "Supplier cannot be deleted because procurement history exists. "
                            + "Deactivate the supplier instead."
            );
        }

        log(
                actor,
                "SUPPLIER_DELETED",
                "supplier",
                id,
                oldValue,
                null
        );
    }

    private void validateSupplierCanBeDeleted(Integer supplierId) {
        if (pos.existsBySupplier_SupplierId(supplierId)
                || offers.existsBySupplier_SupplierId(supplierId)
                || partnerships.existsBySupplier_SupplierId(supplierId)) {
            throw new IllegalArgumentException(
                    "Supplier cannot be deleted because procurement history exists. "
                            + "Deactivate the supplier instead."
            );
        }
    }

    /**
     * Builds the Admin Manage Products form from the canonical Product rows and
     * this supplier's existing supplier_product terms. Nothing is copied into
     * the Supplier entity; the relationship remains supplier-specific.
     */
    @Transactional(readOnly = true)
    public SupplierProductAssignmentForm getSupplierProductAssignmentForm(Integer supplierId) {
        getSupplier(supplierId);

        Map<Integer, SupplierProduct> existing = new HashMap<>();
        for (SupplierProduct term : terms.findBySupplier_SupplierIdOrderByProduct_Name(supplierId)) {
            existing.put(term.getProduct().getProductId(), term);
        }

        SupplierProductAssignmentForm form = new SupplierProductAssignmentForm();
        List<SupplierProductRowForm> rows = new ArrayList<>();

        for (Product product : products.findAllByOrderByCategoryAscNameAsc()) {
            SupplierProduct term = existing.get(product.getProductId());
            SupplierProductRowForm row = new SupplierProductRowForm();
            populateProductLabels(row, product);

            if (term != null) {
                row.setSelected(true);
                row.setPrice(term.getPrice());
                row.setMoq(term.getMoq());
                row.setLeadTimeDays(term.getLeadTimeDays());
            }

            rows.add(row);
        }

        form.setRows(rows);
        return form;
    }

    /**
     * Rehydrates product labels after a validation error. Display values posted
     * by a browser are deliberately ignored so names/codes always come from DB.
     */
    @Transactional(readOnly = true)
    public void refreshSupplierProductFormLabels(SupplierProductAssignmentForm form) {
        if (form == null || form.getRows() == null) {
            return;
        }

        Map<Integer, Product> byId = new HashMap<>();
        for (Product product : products.findAllByOrderByCategoryAscNameAsc()) {
            byId.put(product.getProductId(), product);
        }

        for (SupplierProductRowForm row : form.getRows()) {
            if (row.getProductId() == null) {
                continue;
            }
            Product product = byId.get(row.getProductId());
            if (product != null) {
                populateProductLabels(row, product);
            }
        }
    }

    public long getSupplierProductCount(Integer supplierId) {
        return terms.findBySupplier_SupplierIdOrderByProduct_Name(supplierId).size();
    }

    /**
     * Removes only the supplier-specific commercial relationship. Product and
     * Supplier remain intact, and PurchaseOrderItem already stores the agreed
     * product/price independently so historical orders are not broken.
     */
    @Transactional
    public void removeSupplierProduct(Integer supplierId, Integer productId, User actor) {
        getSupplier(supplierId);
        SupplierProduct term = terms.findBySupplier_SupplierIdAndProduct_ProductId(supplierId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier product relationship not found."));

        Integer recordId = term.getSupplierProductId();
        String oldValue = supplierProductSnapshot(term);

        terms.delete(term);
        terms.flush();

        log(
                actor,
                "SUPPLIER_PRODUCT_REMOVED",
                "supplier_product",
                recordId,
                oldValue,
                null
        );
    }

    /**
     * Creates, updates or removes supplier_product rows. Each selected product
     * must have a supplier-specific price, MOQ and lead time. Existing terms for
     * rows omitted from a request are left untouched; only explicit rows are
     * changed, which avoids accidental mass deletion from a malformed request.
     */
    @Transactional
    public void updateSupplierProducts(
            Integer supplierId,
            SupplierProductAssignmentForm form,
            User actor) {

        Supplier supplier = getSupplier(supplierId);
        if (!supplier.isActive()) {
            throw new IllegalArgumentException(
                    "Activate this supplier before assigning products and supply terms.");
        }
        if (form == null || form.getRows() == null) {
            throw new IllegalArgumentException("Supplier product form is required.");
        }

        List<SupplierProduct> before = terms.findBySupplier_SupplierIdOrderByProduct_Name(supplierId);
        String oldValue = supplierProductsSnapshot(before);

        Map<Integer, Product> availableProducts = new HashMap<>();
        for (Product product : products.findAllByOrderByCategoryAscNameAsc()) {
            availableProducts.put(product.getProductId(), product);
        }

        Map<Integer, SupplierProduct> existing = new HashMap<>();
        for (SupplierProduct term : before) {
            existing.put(term.getProduct().getProductId(), term);
        }

        Set<Integer> seen = new HashSet<>();

        for (int i = 0; i < form.getRows().size(); i++) {
            SupplierProductRowForm row = form.getRows().get(i);
            if (row.getProductId() == null) {
                throw new SupplierProductValidationException(
                        i, "productId", "A product row is missing its product identifier.");
            }
            if (!seen.add(row.getProductId())) {
                throw new SupplierProductValidationException(
                        i, "productId", "Duplicate product row submitted.");
            }

            Product product = availableProducts.get(row.getProductId());
            if (product == null) {
                throw new SupplierProductValidationException(
                        i, "productId", "Product no longer exists.");
            }

            SupplierProduct current = existing.get(product.getProductId());

            if (!row.isSelected()) {
                if (current != null) {
                    terms.delete(current);
                }
                continue;
            }

            validateSupplierProductRow(row, i);

            SupplierProduct term = current == null ? new SupplierProduct() : current;
            term.setSupplier(supplier);
            term.setProduct(product);
            term.setPrice(row.getPrice());
            term.setMoq(row.getMoq());
            term.setLeadTimeDays(row.getLeadTimeDays());
            terms.save(term);
        }

        terms.flush();
        List<SupplierProduct> after = terms.findBySupplier_SupplierIdOrderByProduct_Name(supplierId);

        log(
                actor,
                "SUPPLIER_PRODUCTS_UPDATED",
                "supplier",
                supplierId,
                oldValue,
                supplierProductsSnapshot(after)
        );
    }

    public List<PartnershipRequest> getPartnershipRequests() {
        return partnerships.findAllByOrderBySubmittedAtDesc();
    }

    public PartnershipRequest getPartnershipRequest(Integer id) {
        return partnerships.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
    }

    @Transactional
    public void decidePartnership(Integer id, PartnershipRequest.Status decision, User actor) {
        if (decision == PartnershipRequest.Status.pending) {
            throw new IllegalArgumentException("Choose approve or reject.");
        }

        PartnershipRequest request = partnerships.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));

        if (decision == PartnershipRequest.Status.approved
                && (request.getCatalogFilePath() == null || request.getCatalogFilePath().isBlank())) {
            throw new IllegalArgumentException(
                    "A product catalogue is required before approving this partnership request.");
        }

        PartnershipRequest.Status old = request.getStatus();
        request.setStatus(decision);
        request.setReviewedBy(actor);
        request.setReviewedAt(LocalDateTime.now());
        partnerships.save(request);

        log(
                actor,
                "PARTNERSHIP_REVIEW",
                "partnership_request",
                id,
                "{\"status\":\"" + old + "\"}",
                "{\"status\":\"" + decision + "\"}"
        );
    }

    @Transactional
    public void withdrawPartnershipRequest(Integer id, User actor) {
        PartnershipRequest request = getPartnershipRequest(id);
        if (request.getStatus() != PartnershipRequest.Status.pending) {
            throw new IllegalArgumentException("Only pending partnership requests can be withdrawn.");
        }

        String oldValue = partnershipSnapshot(request);
        CatalogStorageService.StagedDeletion staged =
                catalogStorage.stageForDeletion(request.getCatalogFilePath());
        scheduleCatalogDeletion(staged);

        partnerships.delete(request);
        partnerships.flush();

        log(
                actor,
                "PARTNERSHIP_REQUEST_WITHDRAWN",
                "partnership_request",
                id,
                oldValue,
                null
        );
    }

    @Transactional
    public void removePartnershipCatalog(Integer id, User actor) {
        PartnershipRequest request = getPartnershipRequest(id);
        if (request.getStatus() != PartnershipRequest.Status.pending) {
            throw new IllegalArgumentException(
                    "Catalogue cannot be removed after the partnership request has been reviewed.");
        }
        if (request.getCatalogFilePath() == null || request.getCatalogFilePath().isBlank()) {
            throw new IllegalArgumentException("No catalogue is attached to this partnership request.");
        }

        String oldPath = request.getCatalogFilePath();
        CatalogStorageService.StagedDeletion staged = catalogStorage.stageForDeletion(oldPath);
        scheduleCatalogDeletion(staged);

        request.setCatalogFilePath(null);
        partnerships.saveAndFlush(request);

        log(
                actor,
                "SUPPLIER_CATALOGUE_DELETED",
                "partnership_request",
                id,
                "{\"catalog\":\"" + escape(oldPath) + "\"}",
                "{\"catalog\":null}"
        );
    }

    public List<RestockOffer> getRestockOffers() {
        return offers.findAllByOrderBySubmittedAtDesc();
    }

    @Transactional(readOnly = true)
    public Set<Integer> getDeletableOfferIds() {
        Set<Integer> result = new HashSet<>();
        for (RestockOffer offer : getRestockOffers()) {
            if (isOfferDeletable(offer)) {
                result.add(offer.getOfferId());
            }
        }
        return result;
    }

    @Transactional
    public void deleteOffer(Integer offerId, User actor) {
        RestockOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier offer not found."));

        if (offer.getValidUntil() != null && offer.getValidUntil().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Expired offers are procurement history and cannot be deleted.");
        }
        if (isOfferUsed(offer)) {
            throw new IllegalArgumentException(
                    "Offer cannot be deleted because it has already been used. Keep it as procurement history.");
        }

        String oldValue = offerSnapshot(offer);
        offers.delete(offer);
        offers.flush();

        log(
                actor,
                "SUPPLIER_OFFER_DELETED",
                "restock_offer",
                offerId,
                oldValue,
                null
        );
    }

    public List<RestockSuggestion> getApprovedRestockSuggestions() {
        return suggestions.findAll().stream()
                .filter(suggestion -> suggestion.getStatus() == RestockSuggestion.Status.approved
                        || suggestion.getStatus() == RestockSuggestion.Status.modified)
                .sorted(Comparator.comparing(
                        RestockSuggestion::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public RestockSuggestion getRestockSuggestion(Integer id) {
        return suggestions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restock requirement not found."));
    }

    public List<SupplierComparisonRow> compareSuppliers(Integer productId, int quantity) {
        LocalDate today = LocalDate.now();
        List<RestockOffer> allOffers = offers.findByProduct_ProductIdOrderBySubmittedAtDesc(productId);
        Map<Integer, RestockOffer> bestOfferBySupplier = new HashMap<>();

        for (RestockOffer offer : allOffers) {
            if (offer.getSupplier().isActive()
                    && (offer.getValidUntil() == null || !offer.getValidUntil().isBefore(today))) {
                bestOfferBySupplier.putIfAbsent(offer.getSupplier().getSupplierId(), offer);
            }
        }

        List<SupplierComparisonRow> rows = new ArrayList<>();
        for (SupplierProduct term : terms.findByProduct_ProductId(productId)) {
            if (!term.getSupplier().isActive()) {
                continue;
            }

            SupplierComparisonRow row = new SupplierComparisonRow();
            row.setSupplierId(term.getSupplier().getSupplierId());
            row.setSupplierName(term.getSupplier().getName());
            row.setMoq(term.getMoq());
            row.setLeadTimeDays(term.getLeadTimeDays());
            row.setEligible(quantity >= term.getMoq());

            BigDecimal effectivePrice = term.getPrice();
            RestockOffer offer = bestOfferBySupplier.get(term.getSupplier().getSupplierId());
            if (offer != null
                    && offer.getQuantity() >= quantity
                    && offer.getPrice().compareTo(effectivePrice) < 0) {
                effectivePrice = offer.getPrice();
                row.setOfferUsed(true);
                row.setOfferQuantity(offer.getQuantity());
            }

            row.setUnitPrice(effectivePrice);
            row.setTotalCost(effectivePrice.multiply(BigDecimal.valueOf(quantity)));
            rows.add(row);
        }

        rows.sort(
                Comparator.comparing(SupplierComparisonRow::isEligible).reversed()
                        .thenComparing(SupplierComparisonRow::getTotalCost)
                        .thenComparing(SupplierComparisonRow::getLeadTimeDays)
        );

        rows.stream()
                .filter(SupplierComparisonRow::isEligible)
                .findFirst()
                .ifPresent(row -> row.setRecommended(true));

        return rows;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePurchaseOrderForm form, User actor) {
        if (form.getSupplierId() == null
                || form.getProductId() == null
                || form.getQuantity() == null
                || form.getAgreedPrice() == null) {
            throw new IllegalArgumentException(
                    "Supplier, product, quantity and agreed price are required.");
        }

        Supplier supplier = getSupplier(form.getSupplierId());
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Selected supplier is inactive.");
        }

        Product product = products.findById(form.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        SupplierProduct term = terms.findBySupplier_SupplierIdAndProduct_ProductId(
                        supplier.getSupplierId(),
                        product.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier terms not found."));

        if (form.getQuantity() < term.getMoq()) {
            throw new IllegalArgumentException("Quantity is below MOQ of " + term.getMoq() + ".");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setCreatedBy(actor);
        purchaseOrder = pos.save(purchaseOrder);
        purchaseOrder.setPoCode("PO-" + String.format("%06d", purchaseOrder.getPoId()));
        pos.save(purchaseOrder);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(purchaseOrder);
        item.setProduct(product);
        item.setQuantityOrdered(form.getQuantity());
        item.setReceivedQuantity(0);
        item.setPriceAgreed(form.getAgreedPrice());
        poItems.save(item);

        log(
                actor,
                "CREATE_PO",
                "purchase_order",
                purchaseOrder.getPoId(),
                null,
                "{\"poCode\":\"" + escape(purchaseOrder.getPoCode()) + "\"}"
        );

        return purchaseOrder;
    }

    public List<PurchaseOrder> getPurchaseOrders() {
        return pos.findAllByOrderByCreatedAtDesc();
    }

    public PurchaseOrder getPurchaseOrder(Integer id) {
        return pos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found."));
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(Integer id) {
        return poItems.findByPurchaseOrder_PoId(id);
    }

    @Transactional
    public void markShipped(Integer id, LocalDate expected, User actor) {
        PurchaseOrder purchaseOrder = getPurchaseOrder(id);
        if (purchaseOrder.getStatus() != PurchaseOrder.Status.pending) {
            throw new IllegalArgumentException("Only a pending PO can be marked shipped.");
        }

        purchaseOrder.setStatus(PurchaseOrder.Status.shipped);
        purchaseOrder.setShippedAt(LocalDateTime.now());
        purchaseOrder.setExpectedDeliveryDate(expected);
        pos.save(purchaseOrder);

        log(
                actor,
                "PO_STATUS",
                "purchase_order",
                id,
                "{\"status\":\"pending\"}",
                "{\"status\":\"shipped\"}"
        );
    }

    @Transactional
    public void receiveSingleItemShipment(
            Integer poId,
            Integer itemId,
            ReceiveShipmentForm form,
            User actor) {

        PurchaseOrder purchaseOrder = getPurchaseOrder(poId);
        if (purchaseOrder.getStatus() != PurchaseOrder.Status.shipped
                && purchaseOrder.getStatus() != PurchaseOrder.Status.partially_received) {
            throw new IllegalArgumentException("PO must be shipped before receiving.");
        }

        PurchaseOrderItem item = poItems.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("PO item not found."));

        if (!item.getPurchaseOrder().getPoId().equals(poId)) {
            throw new IllegalArgumentException("PO item mismatch.");
        }

        int current = item.getReceivedQuantity();
        int remaining = item.getQuantityOrdered() - current;
        int accepted = form.getAcceptedQuantity() == null ? 0 : form.getAcceptedQuantity();

        if (accepted < 0 || accepted > remaining) {
            throw new IllegalArgumentException(
                    "Accepted quantity must be between 0 and remaining quantity " + remaining + ".");
        }

        List<String> serialValues = parseSerials(form.getSerialNumbers());

        Product product = products.findById(item.getProduct().getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (product.isSerialTracked()) {
            if (serialValues.size() != accepted) {
                throw new IllegalArgumentException(
                        "Serial-tracked product " + product.getName() + " requires exactly "
                                + accepted + " serial number(s) for the accepted quantity."
                );
            }
        } else if (!serialValues.isEmpty()) {
            throw new IllegalArgumentException(
                    product.getName() + " is not configured for serial tracking. Do not enter serial numbers."
            );
        }

        for (String serialValue : serialValues) {
            if (serials.existsBySerialValue(serialValue)) {
                throw new IllegalArgumentException("Duplicate serial number: " + serialValue);
            }
        }

        int oldStock = product.getStockCount();
        product.setStockCount(oldStock + accepted);
        products.save(product);

        item.setReceivedQuantity(current + accepted);
        poItems.save(item);

        for (String serialValue : serialValues) {
            SerialNumber serial = new SerialNumber();
            serial.setProduct(product);
            serial.setPurchaseOrderItem(item);
            serial.setSerialValue(serialValue);
            serial.setCurrentStatus(SerialNumber.CurrentStatus.in_stock);
            serial.setReceivedDate(LocalDate.now());
            serials.save(serial);
        }

        boolean fullyReceived = getPurchaseOrderItems(poId).stream()
                .allMatch(poItem -> poItem.getReceivedQuantity() >= poItem.getQuantityOrdered());

        purchaseOrder.setStatus(
                fullyReceived
                        ? PurchaseOrder.Status.received
                        : PurchaseOrder.Status.partially_received
        );

        if (fullyReceived) {
            purchaseOrder.setReceivedAt(LocalDateTime.now());
        }
        pos.save(purchaseOrder);

        receiptIntegration.onStockReceived(product);

        log(
                actor,
                "RECEIVE_SHIPMENT",
                "purchase_order",
                poId,
                "{\"stock\":" + oldStock + "}",
                "{\"stock\":" + product.getStockCount()
                        + ",\"accepted\":" + accepted
                        + ",\"note\":\"" + escape(form.getDiscrepancyNote()) + "\"}"
        );
    }

    private boolean isOfferDeletable(RestockOffer offer) {
        return offer.getSubmittedAt() != null
                && (offer.getValidUntil() == null || !offer.getValidUntil().isBefore(LocalDate.now()))
                && !isOfferUsed(offer);
    }

    private boolean isOfferUsed(RestockOffer offer) {
        if (offer.getSubmittedAt() == null) {
            return true;
        }
        if (offer.getValidUntil() == null) {
            return poItems.countMatchingOfferUsageNoExpiry(
                    offer.getSupplier().getSupplierId(),
                    offer.getProduct().getProductId(),
                    offer.getPrice(),
                    offer.getQuantity(),
                    offer.getSubmittedAt()
            ) > 0;
        }

        return poItems.countMatchingOfferUsageWithExpiry(
                offer.getSupplier().getSupplierId(),
                offer.getProduct().getProductId(),
                offer.getPrice(),
                offer.getQuantity(),
                offer.getSubmittedAt(),
                offer.getValidUntil().atTime(23, 59, 59)
        ) > 0;
    }

    private void scheduleCatalogDeletion(CatalogStorageService.StagedDeletion staged) {
        if (staged == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            catalogStorage.finalizeDeletion(staged);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    catalogStorage.finalizeDeletion(staged);
                } else {
                    catalogStorage.restore(staged);
                }
            }
        });
    }

    private void validateSupplierProductRow(SupplierProductRowForm row, int rowIndex) {
        if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SupplierProductValidationException(
                    rowIndex, "price", "Supply price must be greater than 0.");
        }
        if (row.getPrice().scale() > 2) {
            throw new SupplierProductValidationException(
                    rowIndex, "price", "Supply price can have at most 2 decimal places.");
        }
        if (row.getMoq() == null || row.getMoq() <= 0) {
            throw new SupplierProductValidationException(
                    rowIndex, "moq", "MOQ must be at least 1.");
        }
        if (row.getLeadTimeDays() == null || row.getLeadTimeDays() < 0) {
            throw new SupplierProductValidationException(
                    rowIndex, "leadTimeDays", "Lead time cannot be negative.");
        }
    }

    private void populateProductLabels(SupplierProductRowForm row, Product product) {
        row.setProductId(product.getProductId());
        row.setProductCode(product.getProductCode());
        row.setProductName(product.getName());
        row.setCategory(product.getCategory());
        row.setBrand(product.getBrand());
    }

    private String supplierProductSnapshot(SupplierProduct term) {
        return "{"
                + "\"supplierId\":" + term.getSupplier().getSupplierId() + ","
                + "\"productId\":" + term.getProduct().getProductId() + ","
                + "\"price\":" + term.getPrice() + ","
                + "\"moq\":" + term.getMoq() + ","
                + "\"leadTimeDays\":" + term.getLeadTimeDays()
                + "}";
    }

    private String offerSnapshot(RestockOffer offer) {
        return "{"
                + "\"supplierId\":" + offer.getSupplier().getSupplierId() + ","
                + "\"productId\":" + offer.getProduct().getProductId() + ","
                + "\"price\":" + offer.getPrice() + ","
                + "\"quantity\":" + offer.getQuantity() + ","
                + "\"validUntil\":"
                + (offer.getValidUntil() == null
                    ? "null"
                    : "\"" + escape(offer.getValidUntil().toString()) + "\"")
                + "}";
    }

    private String partnershipSnapshot(PartnershipRequest request) {
        return "{"
                + "\"supplierId\":"
                + (request.getSupplier() == null ? "null" : request.getSupplier().getSupplierId()) + ","
                + "\"companyName\":\"" + escape(request.getCompanyName()) + "\","
                + "\"email\":\"" + escape(request.getEmail()) + "\","
                + "\"status\":\"" + request.getStatus() + "\","
                + "\"catalog\":"
                + (request.getCatalogFilePath() == null
                    ? "null"
                    : "\"" + escape(request.getCatalogFilePath()) + "\"")
                + "}";
    }

    private String supplierProductsSnapshot(List<SupplierProduct> values) {
        StringBuilder json = new StringBuilder("{\"products\":[");
        List<SupplierProduct> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(term -> term.getProduct().getProductId()));

        for (int i = 0; i < sorted.size(); i++) {
            SupplierProduct term = sorted.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"productId\":")
                    .append(term.getProduct().getProductId())
                    .append(",\"price\":")
                    .append(term.getPrice())
                    .append(",\"moq\":")
                    .append(term.getMoq())
                    .append(",\"leadTimeDays\":")
                    .append(term.getLeadTimeDays())
                    .append('}');
        }

        return json.append("]}").toString();
    }

    private String validateSupplierCode(String value, boolean required, Integer excludeSupplierId) {
        String clean = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);

        if (clean.isEmpty()) {
            if (required) {
                throw new SupplierFieldValidationException(
                        "supplierCode", "Supplier code is required.");
            }
            return null;
        }

        if (!clean.matches(SUPPLIER_CODE_PATTERN)) {
            throw new SupplierFieldValidationException(
                    "supplierCode",
                    "Supplier code must be 2-20 letters, numbers, dashes or underscores.");
        }

        boolean duplicate = excludeSupplierId == null
                ? suppliers.existsBySupplierCodeIgnoreCase(clean)
                : suppliers.existsBySupplierCodeIgnoreCaseAndSupplierIdNot(clean, excludeSupplierId);

        if (duplicate) {
            throw new SupplierFieldValidationException(
                    "supplierCode", "Supplier code " + clean + " is already in use.");
        }

        return clean;
    }

    private String validateName(String value) {
        String clean = required(value, "Supplier name");
        if (clean.length() > 50) {
            throw new SupplierFieldValidationException(
                    "name", "Supplier name must not exceed 50 characters.");
        }
        return clean;
    }

    private String validateEmail(String value, Integer excludeSupplierId) {
        String clean = required(value, "Email").toLowerCase(Locale.ROOT);

        if (clean.length() > 254 || !clean.matches(EMAIL_PATTERN)) {
            throw new SupplierFieldValidationException(
                    "email", "Enter a valid supplier email address.");
        }

        boolean duplicate = excludeSupplierId == null
                ? suppliers.existsByEmailIgnoreCase(clean)
                : suppliers.existsByEmailIgnoreCaseAndSupplierIdNot(clean, excludeSupplierId);

        if (duplicate) {
            throw new SupplierFieldValidationException(
                    "email", "A supplier account already uses this email address.");
        }

        return clean;
    }

    private String validateContact(String value) {
        String clean = required(value, "Contact number");
        if (!clean.matches(CONTACT_PATTERN)) {
            throw new SupplierFieldValidationException(
                    "contact",
                    "Enter a valid contact number using digits, spaces, +, -, or parentheses.");
        }
        return clean;
    }

    private void validatePasswords(String password, String confirmation) {
        if (password == null || password.length() < 8) {
            throw new SupplierFieldValidationException(
                    "temporaryPassword", "Password must contain at least 8 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new SupplierFieldValidationException(
                    "confirmPassword", "Passwords do not match.");
        }
    }

    private List<String> parseSerials(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(serial -> !serial.isEmpty())
                .distinct()
                .toList();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            String formField = switch (field) {
                case "Supplier name" -> "name";
                case "Email" -> "email";
                case "Contact number" -> "contact";
                default -> null;
            };

            if (formField != null) {
                throw new SupplierFieldValidationException(formField, field + " is required.");
            }
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private String supplierSnapshot(Supplier supplier) {
        return "{"
                + "\"supplierCode\":\"" + escape(supplier.getSupplierCode()) + "\"," 
                + "\"name\":\"" + escape(supplier.getName()) + "\"," 
                + "\"email\":\"" + escape(supplier.getEmail()) + "\"," 
                + "\"contact\":\"" + escape(supplier.getContact()) + "\"," 
                + "\"active\":" + supplier.isActive()
                + "}";
    }

    /**
     * Escapes arbitrary user-entered text before embedding it in the JSON
     * snapshots stored in audit_log.old_value / audit_log.new_value.
     *
     * MySQL validates JSON columns strictly, so raw control characters such
     * as a newline from a textarea make an otherwise normal audit insert fail.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch <= 0x1F) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private void log(
            User user,
            String action,
            String table,
            Integer recordId,
            String oldValue,
            String newValue) {

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setActionType(action);
        auditLog.setTableName(table);
        auditLog.setRecordId(recordId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        audits.save(auditLog);
    }
}
