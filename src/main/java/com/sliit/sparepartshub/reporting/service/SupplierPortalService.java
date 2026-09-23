package com.sliit.sparepartshub.reporting.service;

import com.sliit.sparepartshub.entity.*;
import com.sliit.sparepartshub.reporting.dto.SupplierVelocityRow;
import com.sliit.sparepartshub.reporting.repository.*;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupplierPortalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierPortalService.class);
    private final SupplierRepository suppliers;
    private final PortalSupplierProductRepository terms;
    private final PortalRestockOfferRepository offers;
    private final ReportingPurchaseOrderRepository purchaseOrders;
    private final ReportingPurchaseOrderItemRepository purchaseOrderItems;
    private final PortalPartnershipRepository partnerships;
    private final ReportingSaleItemRepository saleItems;
    private final PasswordEncoder passwordEncoder;
    private final CatalogStorageService catalogStorage;

    public SupplierPortalService(SupplierRepository suppliers,
                                 PortalSupplierProductRepository terms,
                                 PortalRestockOfferRepository offers,
                                 ReportingPurchaseOrderRepository purchaseOrders,
                                 ReportingPurchaseOrderItemRepository purchaseOrderItems,
                                 PortalPartnershipRepository partnerships,
                                 ReportingSaleItemRepository saleItems,
                                 PasswordEncoder passwordEncoder,
                                 CatalogStorageService catalogStorage) {
        this.suppliers = suppliers;
        this.terms = terms;
        this.offers = offers;
        this.purchaseOrders = purchaseOrders;
        this.purchaseOrderItems = purchaseOrderItems;
        this.partnerships = partnerships;
        this.saleItems = saleItems;
        this.passwordEncoder = passwordEncoder;
        this.catalogStorage = catalogStorage;
    }

    public Supplier supplier(Integer supplierId) {
        return suppliers.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier account not found."));
    }

    public List<SupplierProduct> terms(Integer supplierId) {
        return terms.findBySupplier_SupplierIdOrderByProduct_Name(supplierId);
    }

    public List<RestockOffer> offers(Integer supplierId) {
        return offers.findBySupplier_SupplierIdOrderBySubmittedAtDesc(supplierId);
    }

    public long activeOfferCount(Integer supplierId) {
        LocalDate today = LocalDate.now();
        return offers(supplierId).stream()
                .filter(o -> o.getValidUntil() == null || !o.getValidUntil().isBefore(today))
                .count();
    }

    @Transactional(readOnly = true)
    public Set<Integer> deletableOfferIds(Integer supplierId) {
        Set<Integer> result = new HashSet<>();
        for (RestockOffer offer : offers(supplierId)) {
            if (isOfferDeletable(offer)) {
                result.add(offer.getOfferId());
            }
        }
        return result;
    }

    @Transactional
    public void deleteOffer(Integer supplierId, Integer offerId) {
        RestockOffer offer = offers.findByOfferIdAndSupplier_SupplierId(offerId, supplierId)
                .orElseThrow(() -> new AccessDeniedException(
                        "This offer does not belong to your supplier account."));

        if (offer.getValidUntil() != null && offer.getValidUntil().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Expired offers are procurement history and cannot be deleted.");
        }
        if (isOfferUsed(offer)) {
            throw new IllegalArgumentException(
                    "Offer cannot be deleted because it has already been used. Keep it as procurement history.");
        }

        offers.delete(offer);
        offers.flush();
        LOGGER.info("Supplier {} deleted unused restock offer {}", supplierId, offerId);
    }

    public List<PurchaseOrder> orders(Integer supplierId) {
        return purchaseOrders.findBySupplier_SupplierIdOrderByCreatedAtDesc(supplierId);
    }

    public long openOrderCount(Integer supplierId) {
        return orders(supplierId).stream()
                .filter(po -> po.getStatus() != PurchaseOrder.Status.received)
                .count();
    }

    public PurchaseOrder ownedOrder(Integer supplierId, Integer purchaseOrderId) {
        return purchaseOrders.findByPoIdAndSupplier_SupplierId(purchaseOrderId, supplierId)
                .orElseThrow(() -> new AccessDeniedException("Purchase order is not assigned to this supplier account."));
    }

    public List<PurchaseOrderItem> ownedOrderItems(Integer supplierId, Integer purchaseOrderId) {
        ownedOrder(supplierId, purchaseOrderId);
        return purchaseOrderItems.findByPurchaseOrder_PoId(purchaseOrderId);
    }

    public List<SupplierVelocityRow> salesVelocity(Integer supplierId) {
        List<SupplierProduct> linkedTerms = terms(supplierId);
        Set<Integer> allowedProductIds = linkedTerms.stream()
                .map(term -> term.getProduct().getProductId())
                .collect(Collectors.toSet());

        LocalDateTime from = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        Map<Integer, Long> quantityByProduct = new HashMap<>();

        for (SaleItem item : saleItems.range(from, to)) {
            Integer productId = item.getProduct().getProductId();
            if (allowedProductIds.contains(productId)) {
                quantityByProduct.merge(productId, item.getQuantity().longValue(), Long::sum);
            }
        }

        List<SupplierVelocityRow> result = new ArrayList<>();
        for (SupplierProduct term : linkedTerms) {
            Integer productId = term.getProduct().getProductId();
            result.add(new SupplierVelocityRow(
                    productId,
                    term.getProduct().getName(),
                    quantityByProduct.getOrDefault(productId, 0L)
            ));
        }
        return result;
    }

    @Transactional
    public Supplier updateProfile(Integer supplierId, String name, String email, String contact) {
        Supplier supplier = supplier(supplierId);
        if (!supplier.isActive()) throw new IllegalArgumentException("This supplier account is inactive.");
        String cleanName = required(name, "Supplier name");
        String cleanEmail = required(email, "Email").toLowerCase(Locale.ROOT);
        String cleanContact = required(contact, "Contact");
        if (cleanName.length() > 50) throw new IllegalArgumentException("Supplier name is too long.");
        if (cleanEmail.length() > 254 || !cleanEmail.contains("@")) throw new IllegalArgumentException("Enter a valid email address.");
        if (cleanContact.length() > 20) throw new IllegalArgumentException("Contact number is too long.");
        if (suppliers.existsByEmailIgnoreCaseAndSupplierIdNot(cleanEmail, supplierId)) {
            throw new IllegalArgumentException("Another supplier account already uses that email address.");
        }
        supplier.setName(cleanName);
        supplier.setEmail(cleanEmail);
        supplier.setContact(cleanContact);
        return suppliers.save(supplier);
    }

    @Transactional
    public void changePassword(Integer supplierId, String currentPassword, String newPassword, String confirmation) {
        Supplier supplier = supplier(supplierId);
        if (!supplier.isActive()) throw new IllegalArgumentException("This supplier account is inactive.");
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, supplier.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        supplier.setPasswordHash(passwordEncoder.encode(newPassword));
        suppliers.save(supplier);
    }

    @Transactional
    public RestockOffer submitOffer(Supplier authenticatedSupplier, Integer productId, BigDecimal price,
                                    Integer quantity, LocalDate validUntil) {
        Supplier supplier = supplier(authenticatedSupplier.getSupplierId());
        if (!supplier.isActive()) throw new IllegalArgumentException("This supplier account is inactive.");
        SupplierProduct linkedTerm = terms
                .findBySupplier_SupplierIdAndProduct_ProductId(supplier.getSupplierId(), productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You can only submit offers for products linked to your supplier account."));
        if (price == null || price.signum() <= 0 || quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Price and quantity must be positive.");
        }
        if (validUntil != null && validUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Offer validity date cannot be in the past.");
        }
        RestockOffer offer = new RestockOffer();
        offer.setSupplier(supplier);
        offer.setProduct(linkedTerm.getProduct());
        offer.setPrice(price);
        offer.setQuantity(quantity);
        offer.setValidUntil(validUntil);
        return offers.save(offer);
    }

    @Transactional
    public PartnershipRequest submitPartnership(Supplier authenticatedSupplier, String contactPerson, MultipartFile catalogFile) {
        Supplier supplier = supplier(authenticatedSupplier.getSupplierId());
        if (!supplier.isActive()) throw new IllegalArgumentException("This supplier account is inactive.");
        if (contactPerson == null || contactPerson.isBlank()) {
            throw new IllegalArgumentException("Contact person is required.");
        }
        if (partnerships.existsBySupplier_SupplierIdAndStatus(supplier.getSupplierId(), PartnershipRequest.Status.pending)) {
            throw new IllegalArgumentException("A partnership request is already awaiting review.");
        }

        String storedFile = catalogStorage.store(catalogFile);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        catalogStorage.deleteQuietly(storedFile);
                    }
                }
            });
        }
        try {
            return savePartnership(supplier, contactPerson.trim(), storedFile);
        } catch (RuntimeException ex) {
            catalogStorage.deleteQuietly(storedFile);
            throw ex;
        }
    }

    private PartnershipRequest savePartnership(Supplier supplier, String contactPerson, String storedFile) {
        PartnershipRequest request = new PartnershipRequest();
        request.setSupplier(supplier);
        request.setCompanyName(supplier.getName());
        request.setContactPerson(contactPerson);
        request.setEmail(supplier.getEmail());
        request.setCatalogFilePath(storedFile);
        return partnerships.saveAndFlush(request);
    }

    public List<PartnershipRequest> partnerships(Supplier authenticatedSupplier) {
        return partnerships.findBySupplier_SupplierIdOrderBySubmittedAtDesc(authenticatedSupplier.getSupplierId());
    }

    public PartnershipRequest ownedPartnership(Integer supplierId, Integer requestId) {
        return partnerships.findByRequestIdAndSupplier_SupplierId(requestId, supplierId)
                .orElseThrow(() -> new AccessDeniedException(
                        "This partnership request does not belong to your supplier account."));
    }

    @Transactional
    public void withdrawPartnership(Integer supplierId, Integer requestId) {
        PartnershipRequest request = ownedPartnership(supplierId, requestId);
        requirePendingPartnership(request);

        CatalogStorageService.StagedDeletion staged =
                catalogStorage.stageForDeletion(request.getCatalogFilePath());
        scheduleCatalogDeletion(staged);

        partnerships.delete(request);
        partnerships.flush();
        LOGGER.info("Supplier {} withdrew pending partnership request {}", supplierId, requestId);
    }

    @Transactional
    public void removePartnershipCatalog(Integer supplierId, Integer requestId) {
        PartnershipRequest request = ownedPartnership(supplierId, requestId);
        requirePendingPartnership(request);
        if (request.getCatalogFilePath() == null || request.getCatalogFilePath().isBlank()) {
            throw new IllegalArgumentException("No catalogue is attached to this partnership request.");
        }

        CatalogStorageService.StagedDeletion staged =
                catalogStorage.stageForDeletion(request.getCatalogFilePath());
        scheduleCatalogDeletion(staged);

        request.setCatalogFilePath(null);
        partnerships.saveAndFlush(request);
        LOGGER.info("Supplier {} removed catalogue from pending partnership request {}", supplierId, requestId);
    }

    @Transactional
    public void replacePartnershipCatalog(
            Integer supplierId,
            Integer requestId,
            MultipartFile catalogFile) {

        PartnershipRequest request = ownedPartnership(supplierId, requestId);
        requirePendingPartnership(request);

        String newStoredFile = catalogStorage.store(catalogFile);
        registerNewCatalogRollbackCleanup(newStoredFile);

        CatalogStorageService.StagedDeletion oldStaged =
                catalogStorage.stageForDeletion(request.getCatalogFilePath());
        scheduleCatalogDeletion(oldStaged);

        request.setCatalogFilePath(newStoredFile);
        partnerships.saveAndFlush(request);
        LOGGER.info("Supplier {} replaced catalogue for pending partnership request {}", supplierId, requestId);
    }

    @Transactional
    public void submitShipmentInfo(Integer supplierId, Integer purchaseOrderId, String trackingReference, String note) {
        PurchaseOrder po = ownedOrder(supplierId, purchaseOrderId);
        if (po.getStatus() == PurchaseOrder.Status.received) {
            throw new IllegalArgumentException("Shipment information cannot be changed after the order is fully received.");
        }
        String tracking = required(trackingReference, "Tracking reference");
        if (tracking.length() > 100) throw new IllegalArgumentException("Tracking reference is too long.");
        String cleanNote = note == null ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > 500) throw new IllegalArgumentException("Shipment note is too long.");
        po.setTrackingReference(tracking);
        po.setSupplierShipmentNote(cleanNote == null || cleanNote.isBlank() ? null : cleanNote);
        po.setSupplierDispatchedAt(LocalDateTime.now());
        purchaseOrders.save(po);
    }

    public Resource catalogResource(Integer supplierId, Integer requestId) {
        PartnershipRequest request = ownedPartnership(supplierId, requestId);
        return catalogStorage.load(request.getCatalogFilePath());
    }

    public String catalogExtension(Integer supplierId, Integer requestId) {
        PartnershipRequest request = ownedPartnership(supplierId, requestId);
        return catalogStorage.extensionOfStoredName(request.getCatalogFilePath());
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
            return purchaseOrderItems.countMatchingOfferUsageNoExpiry(
                    offer.getSupplier().getSupplierId(),
                    offer.getProduct().getProductId(),
                    offer.getPrice(),
                    offer.getQuantity(),
                    offer.getSubmittedAt()
            ) > 0;
        }

        return purchaseOrderItems.countMatchingOfferUsageWithExpiry(
                offer.getSupplier().getSupplierId(),
                offer.getProduct().getProductId(),
                offer.getPrice(),
                offer.getQuantity(),
                offer.getSubmittedAt(),
                offer.getValidUntil().atTime(23, 59, 59)
        ) > 0;
    }

    private void requirePendingPartnership(PartnershipRequest request) {
        if (request.getStatus() != PartnershipRequest.Status.pending) {
            throw new IllegalArgumentException("Only pending partnership requests can be changed or withdrawn.");
        }
    }

    private void registerNewCatalogRollbackCleanup(String storedFile) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    catalogStorage.deleteQuietly(storedFile);
                }
            }
        });
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

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }
}
