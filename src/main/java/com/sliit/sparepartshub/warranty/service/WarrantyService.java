package com.sliit.sparepartshub.warranty.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.RmaClaim;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.repository.AuditLogRepository;
import com.sliit.sparepartshub.sales.repository.SalesProductRepository;
import com.sliit.sparepartshub.stockmonitoring.service.StockMonitoringService;
import com.sliit.sparepartshub.warranty.dto.WarrantyLookup;
import com.sliit.sparepartshub.warranty.repository.WarrantyRmaRepository;
import com.sliit.sparepartshub.warranty.repository.WarrantySerialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class WarrantyService {

    private static final Set<RmaClaim.ClaimStatus> OPEN_STATUSES =
            EnumSet.of(RmaClaim.ClaimStatus.pending, RmaClaim.ClaimStatus.approved);

    private final WarrantySerialRepository serials;
    private final WarrantyRmaRepository claims;
    private final AuditLogRepository audits;
    private final SalesProductRepository products;
    private final StockMonitoringService stockMonitoring;

    public WarrantyService(WarrantySerialRepository serials,
                           WarrantyRmaRepository claims,
                           AuditLogRepository audits,
                           SalesProductRepository products,
                           StockMonitoringService stockMonitoring) {
        this.serials = serials;
        this.claims = claims;
        this.audits = audits;
        this.products = products;
        this.stockMonitoring = stockMonitoring;
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return claims.countByClaimStatus(RmaClaim.ClaimStatus.pending);
    }

    @Transactional(readOnly = true)
    public long approvedCount() {
        return claims.countByClaimStatus(RmaClaim.ClaimStatus.approved);
    }

    @Transactional(readOnly = true)
    public long todayCount() {
        return claims.countByClaimDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<RmaClaim> claims(RmaClaim.ClaimStatus status) {
        if (status == null) {
            return claims.findAllByOrderByClaimDateDescClaimIdDesc();
        }
        return claims.findByClaimStatusOrderByClaimDateDescClaimIdDesc(status);
    }

    @Transactional(readOnly = true)
    public RmaClaim getClaim(Integer claimId) {
        return claims.findDetailedById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("RMA claim not found."));
    }

    @Transactional(readOnly = true)
    public List<RmaClaim> history(Integer serialId) {
        return claims.findBySerial_SerialIdOrderByClaimDateDescClaimIdDesc(serialId);
    }

    @Transactional(readOnly = true)
    public List<SerialNumber> replacementOptions(Integer claimId) {
        RmaClaim claim = getClaim(claimId);
        if (claim.getClaimStatus() != RmaClaim.ClaimStatus.approved
                || claim.getResolution() != RmaClaim.Resolution.pending) {
            return List.of();
        }
        return serials.findAvailableReplacementSerials(
                claim.getSerial().getProduct().getProductId(),
                SerialNumber.CurrentStatus.in_stock
        );
    }

    @Transactional(readOnly = true)
    public WarrantyLookup lookup(String value) {
        String serialValue = cleanRequired(value, "Serial number", 50);
        SerialNumber serial = serials.findBySerialValueIgnoreCase(serialValue)
                .orElseThrow(() -> new IllegalArgumentException("Serial number was not found."));

        RmaClaim openClaim = claims
                .findFirstBySerial_SerialIdAndClaimStatusInOrderByClaimIdDesc(serial.getSerialId(), OPEN_STATUSES)
                .orElse(null);

        return evaluate(serial, openClaim);
    }

    @Transactional
    public RmaClaim create(String serialValue,
                           String faultDescription,
                           String conditionNotes,
                           User actor) {
        String cleanSerial = cleanRequired(serialValue, "Serial number", 50);
        String cleanFault = cleanRequired(faultDescription, "Fault description", 255);
        String cleanNotes = cleanOptional(conditionNotes, 255);

        SerialNumber found = serials.findBySerialValueIgnoreCase(cleanSerial)
                .orElseThrow(() -> new IllegalArgumentException("Serial number was not found."));
        SerialNumber serial = serials.findByIdForUpdate(found.getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Serial number was not found."));

        RmaClaim openClaim = claims
                .findFirstBySerial_SerialIdAndClaimStatusInOrderByClaimIdDesc(serial.getSerialId(), OPEN_STATUSES)
                .orElse(null);
        WarrantyLookup eligibility = evaluate(serial, openClaim);
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException(eligibility.getEligibilityMessage());
        }

        RmaClaim claim = new RmaClaim();
        claim.setSerial(serial);
        claim.setProcessedBy(actor);
        claim.setFaultDescription(cleanFault);
        claim.setConditionNotes(cleanNotes);
        claim.setClaimDate(LocalDate.now());
        claim.setClaimStatus(RmaClaim.ClaimStatus.pending);
        claim.setResolution(RmaClaim.Resolution.pending);

        claim = claims.saveAndFlush(claim);
        claim.setClaimCode("RMA-" + String.format("%06d", claim.getClaimId()));
        claim = claims.save(claim);

        // Opening an RMA records that the sold unit is currently reported as
        // defective. This does not affect sellable stock because the unit was
        // already removed from inventory by the original Sale transaction.
        serial.setCurrentStatus(SerialNumber.CurrentStatus.defective);
        serials.save(serial);

        audit(actor,
                "RMA_CREATED",
                claim.getClaimId(),
                null,
                "{\"claimCode\":" + jsonString(claim.getClaimCode())
                        + ",\"serial\":" + jsonString(serial.getSerialValue())
                        + ",\"status\":\"pending\"}");

        return claim;
    }

    public boolean canDeletePendingClaim(RmaClaim claim) {
        return claim.getClaimStatus() == RmaClaim.ClaimStatus.pending
                && claim.getResolution() == RmaClaim.Resolution.pending
                && claim.getReviewedBy() == null && claim.getResolvedBy() == null
                && claim.getReplacementSerial() == null
                && claim.getReviewedAt() == null && claim.getResolvedAt() == null
                && claim.getClosedAt() == null
                && claim.getRejectionReason() == null && claim.getResolutionNotes() == null;
    }

    @Transactional
    public String deletePendingClaim(Integer claimId, User actor) {
        if (claimId == null || claimId <= 0) {
            throw new IllegalArgumentException("RMA claim not found.");
        }
        RmaClaim claim = lockedClaim(claimId);
        if (!canDeletePendingClaim(claim)) {
            throw new IllegalArgumentException("Only an unreviewed pending RMA created by mistake can be deleted.");
        }
        if (claim.getSerial() == null) {
            throw new IllegalArgumentException("The original serial number no longer exists.");
        }
        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("The original serial number no longer exists."));
        if (original.getSale() == null || original.getCurrentStatus() != SerialNumber.CurrentStatus.defective) {
            throw new IllegalArgumentException("RMA could not be deleted safely. No changes were made.");
        }
        if (actor == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }
        String code = claim.getClaimCode();
        String oldValue = "{\"claimCode\":" + jsonString(code)
                + ",\"serial\":" + jsonString(original.getSerialValue())
                + ",\"status\":\"pending\",\"resolution\":\"pending\",\"faultDescription\":"
                + jsonString(claim.getFaultDescription()) + "}";
        original.setCurrentStatus(SerialNumber.CurrentStatus.sold);
        serials.save(original);
        claims.delete(claim);
        claims.flush();
        audit(actor, "RMA_DELETED", claimId, oldValue,
                "{\"deleted\":true,\"reason\":\"accidental_pending_claim\"}");
        return code;
    }

    @Transactional
    public RmaClaim approve(Integer claimId, User actor) {
        RmaClaim claim = lockedClaim(claimId);
        requireStatus(claim, RmaClaim.ClaimStatus.pending,
                "Only a pending RMA can be approved.");

        RmaClaim.ClaimStatus oldStatus = claim.getClaimStatus();
        claim.setClaimStatus(RmaClaim.ClaimStatus.approved);
        claim.setReviewedBy(actor);
        claim.setReviewedAt(LocalDateTime.now());
        claim.setRejectionReason(null);

        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Original serial number no longer exists."));
        original.setCurrentStatus(SerialNumber.CurrentStatus.defective);
        serials.save(original);
        claim = claims.save(claim);

        audit(actor,
                "RMA_APPROVED",
                claim.getClaimId(),
                "{\"status\":" + jsonString(oldStatus.name()) + "}",
                "{\"status\":\"approved\",\"claimCode\":" + jsonString(claim.getClaimCode()) + "}");
        return claim;
    }

    @Transactional
    public RmaClaim reject(Integer claimId, String reason, User actor) {
        String cleanReason = cleanRequired(reason, "Rejection reason", 255);
        RmaClaim claim = lockedClaim(claimId);
        requireStatus(claim, RmaClaim.ClaimStatus.pending,
                "Only a pending RMA can be rejected.");

        LocalDateTime now = LocalDateTime.now();
        claim.setClaimStatus(RmaClaim.ClaimStatus.rejected);
        claim.setRejectionReason(cleanReason);
        claim.setReviewedBy(actor);
        claim.setReviewedAt(now);
        claim.setClosedAt(now);

        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Original serial number no longer exists."));
        // Rejection means the warranty workflow did not accept the item. It
        // remains the customer's already-sold unit, so restore logical state.
        original.setCurrentStatus(SerialNumber.CurrentStatus.sold);
        serials.save(original);
        claim = claims.save(claim);

        audit(actor,
                "RMA_REJECTED",
                claim.getClaimId(),
                "{\"status\":\"pending\"}",
                "{\"status\":\"rejected\",\"reason\":" + jsonString(cleanReason) + "}");
        return claim;
    }

    @Transactional
    public RmaClaim replace(Integer claimId,
                            Integer replacementSerialId,
                            String notes,
                            boolean confirmed,
                            User actor) {
        if (!confirmed) {
            throw new IllegalArgumentException("Confirm the replacement before completing this action.");
        }
        if (replacementSerialId == null) {
            throw new IllegalArgumentException("Select a replacement serial number.");
        }

        RmaClaim claim = lockedClaim(claimId);
        requireApprovedUnresolved(claim);

        Integer productId = claim.getSerial().getProduct().getProductId();

        // Match Sales/POS lock ordering: product first, then physical serial.
        // This avoids a product<->serial lock inversion when a sale and a
        // warranty replacement race for the final unit.
        Product product = products.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product no longer exists."));
        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Original serial number no longer exists."));
        SerialNumber replacement = serials.findByIdForUpdate(replacementSerialId)
                .orElseThrow(() -> new IllegalArgumentException("Replacement serial number was not found."));

        if (original.getSerialId().equals(replacement.getSerialId())) {
            throw new IllegalArgumentException("The original faulty serial cannot be used as its own replacement.");
        }
        if (!productId.equals(replacement.getProduct().getProductId())) {
            throw new IllegalArgumentException("Replacement serial must belong to the same product.");
        }
        if (replacement.getCurrentStatus() != SerialNumber.CurrentStatus.in_stock) {
            throw new IllegalArgumentException("Replacement serial is no longer in stock.");
        }
        if (replacement.getSale() != null) {
            throw new IllegalArgumentException("Replacement serial has already been sold.");
        }
        if (claims.existsByReplacementSerial_SerialId(replacement.getSerialId())) {
            throw new IllegalArgumentException("Replacement serial has already been used for another RMA.");
        }

        int currentStock = product.getStockCount() == null ? 0 : product.getStockCount();
        if (currentStock <= 0) {
            throw new IllegalArgumentException("No sellable stock is available for this replacement.");
        }

        product.setStockCount(currentStock - 1);
        products.save(product);

        replacement.setCurrentStatus(SerialNumber.CurrentStatus.replacement);
        serials.save(replacement);

        original.setCurrentStatus(SerialNumber.CurrentStatus.returned);
        serials.save(original);

        LocalDateTime now = LocalDateTime.now();
        claim.setReplacementSerial(replacement);
        claim.setResolution(RmaClaim.Resolution.replaced);
        claim.setResolutionNotes(cleanOptional(notes, 500));
        claim.setResolvedBy(actor);
        claim.setResolvedAt(now);
        claim.setClosedAt(now);
        claim.setClaimStatus(RmaClaim.ClaimStatus.closed);
        claim = claims.save(claim);

        audit(actor,
                "RMA_REPLACED",
                claim.getClaimId(),
                "{\"stock\":" + currentStock + ",\"resolution\":\"pending\"}",
                "{\"stock\":" + (currentStock - 1)
                        + ",\"resolution\":\"replaced\",\"replacementSerial\":"
                        + jsonString(replacement.getSerialValue()) + "}");

        stockMonitoring.recalculateProduct(product.getProductId());
        return claim;
    }

    @Transactional
    public RmaClaim refund(Integer claimId,
                           String notes,
                           boolean confirmed,
                           User actor) {
        if (!confirmed) {
            throw new IllegalArgumentException("Confirm the refund resolution before completing this action.");
        }
        RmaClaim claim = lockedClaim(claimId);
        requireApprovedUnresolved(claim);

        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Original serial number no longer exists."));
        original.setCurrentStatus(SerialNumber.CurrentStatus.returned);
        serials.save(original);

        finalizeWithoutStockChange(claim, RmaClaim.Resolution.refunded, notes, actor);
        audit(actor,
                "RMA_REFUNDED",
                claim.getClaimId(),
                "{\"resolution\":\"pending\"}",
                "{\"resolution\":\"refunded\"}");
        return claim;
    }

    @Transactional
    public RmaClaim sendToManufacturer(Integer claimId,
                                       String notes,
                                       boolean confirmed,
                                       User actor) {
        if (!confirmed) {
            throw new IllegalArgumentException("Confirm the manufacturer-return resolution before completing this action.");
        }
        RmaClaim claim = lockedClaim(claimId);
        requireApprovedUnresolved(claim);

        SerialNumber original = serials.findByIdForUpdate(claim.getSerial().getSerialId())
                .orElseThrow(() -> new IllegalArgumentException("Original serial number no longer exists."));
        // 'returned' is an existing non-sellable state and safely represents
        // the unit leaving the customer/store workflow for the manufacturer.
        original.setCurrentStatus(SerialNumber.CurrentStatus.returned);
        serials.save(original);

        finalizeWithoutStockChange(claim, RmaClaim.Resolution.sent_to_manufacturer, notes, actor);
        audit(actor,
                "RMA_SENT_TO_MANUFACTURER",
                claim.getClaimId(),
                "{\"resolution\":\"pending\"}",
                "{\"resolution\":\"sent_to_manufacturer\"}");
        return claim;
    }

    private void finalizeWithoutStockChange(RmaClaim claim,
                                            RmaClaim.Resolution resolution,
                                            String notes,
                                            User actor) {
        LocalDateTime now = LocalDateTime.now();
        claim.setResolution(resolution);
        claim.setResolutionNotes(cleanOptional(notes, 500));
        claim.setResolvedBy(actor);
        claim.setResolvedAt(now);
        claim.setClosedAt(now);
        claim.setClaimStatus(RmaClaim.ClaimStatus.closed);
        claims.save(claim);
    }

    private RmaClaim lockedClaim(Integer claimId) {
        if (claimId == null) {
            throw new IllegalArgumentException("RMA claim is required.");
        }
        return claims.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalArgumentException("RMA claim not found."));
    }

    private void requireStatus(RmaClaim claim,
                               RmaClaim.ClaimStatus expected,
                               String message) {
        if (claim.getClaimStatus() != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireApprovedUnresolved(RmaClaim claim) {
        if (claim.getClaimStatus() != RmaClaim.ClaimStatus.approved) {
            throw new IllegalArgumentException("Only an approved RMA can be resolved.");
        }
        if (claim.getResolution() != RmaClaim.Resolution.pending) {
            throw new IllegalArgumentException("This RMA already has a final resolution.");
        }
    }

    private WarrantyLookup evaluate(SerialNumber serial, RmaClaim openClaim) {
        boolean soldByStore = serial.getSale() != null;
        LocalDate expiry = null;

        if (!soldByStore) {
            return new WarrantyLookup(
                    serial, false, null, "Not Sold", false,
                    "This serial has not been sold by this store.", openClaim);
        }

        if (openClaim != null) {
            return new WarrantyLookup(
                    serial, true, warrantyExpiry(serial), "RMA Open", false,
                    "An open RMA already exists for this serial number.", openClaim);
        }

        Integer warrantyMonths = serial.getProduct().getWarrantyPeriodMonths();
        if (warrantyMonths == null || warrantyMonths <= 0) {
            return new WarrantyLookup(
                    serial, true, null, "Warranty Not Configured", false,
                    "This product does not have a valid warranty period configured.", null);
        }
        if (serial.getSale().getSoldAt() == null) {
            return new WarrantyLookup(
                    serial, true, null, "Sale Date Missing", false,
                    "The original sale date is unavailable, so warranty eligibility cannot be calculated.", null);
        }

        expiry = warrantyExpiry(serial);
        if (LocalDate.now().isAfter(expiry)) {
            return new WarrantyLookup(
                    serial, true, expiry, "Out of Warranty", false,
                    "This item is out of warranty.", null);
        }

        if (serial.getCurrentStatus() != SerialNumber.CurrentStatus.sold) {
            return new WarrantyLookup(
                    serial, true, expiry, "Not Eligible", false,
                    "This serial is not in a sold state and cannot open a new RMA.", null);
        }

        return new WarrantyLookup(
                serial, true, expiry, "In Warranty", true,
                "This serial is eligible for an RMA claim.", null);
    }

    private LocalDate warrantyExpiry(SerialNumber serial) {
        if (serial.getSale() == null || serial.getSale().getSoldAt() == null) {
            return null;
        }
        Integer months = serial.getProduct().getWarrantyPeriodMonths();
        if (months == null || months <= 0) {
            return null;
        }
        return serial.getSale().getSoldAt().toLocalDate().plusMonths(months);
    }

    private String cleanRequired(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters.");
        }
        return clean;
    }

    private String cleanOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new IllegalArgumentException("Value must not exceed " + maxLength + " characters.");
        }
        return clean;
    }

    private void audit(User actor,
                       String action,
                       Integer recordId,
                       String oldValue,
                       String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setActionType(action);
        log.setTableName("rma_claim");
        log.setRecordId(recordId == null ? 0 : recordId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        audits.save(log);
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
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
        return escaped.append('"').toString();
    }
}
