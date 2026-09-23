package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rma_claim")
public class RmaClaim {

    /**
     * Workflow state of the claim itself. This is intentionally separate from
     * the resolution so an approved claim can wait for a replacement/refund/
     * manufacturer decision without pretending that it is already complete.
     */
    public enum ClaimStatus {
        pending, approved, rejected, closed
    }

    public enum Resolution {
        sent_to_manufacturer, refunded, replaced, pending
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Integer claimId;

    @Column(name = "claim_code", unique = true, length = 20)
    private String claimCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id", nullable = false)
    private SerialNumber serial;

    /** User who originally opened the claim. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by", nullable = false)
    private User processedBy;

    /** User who approved/rejected the claim. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** User who finalized the resolution. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    /** Physical unit issued as a replacement, when resolution == replaced. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_serial_id")
    private SerialNumber replacementSerial;

    @Column(name = "fault_description", length = 255)
    private String faultDescription;

    @Column(name = "condition_notes", length = 255)
    private String conditionNotes;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false)
    private ClaimStatus claimStatus = ClaimStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false)
    private Resolution resolution = Resolution.pending;

    public RmaClaim() {
    }

    public Integer getClaimId() {
        return claimId;
    }

    public void setClaimId(Integer claimId) {
        this.claimId = claimId;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public void setClaimCode(String claimCode) {
        this.claimCode = claimCode;
    }

    public SerialNumber getSerial() {
        return serial;
    }

    public void setSerial(SerialNumber serial) {
        this.serial = serial;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public SerialNumber getReplacementSerial() {
        return replacementSerial;
    }

    public void setReplacementSerial(SerialNumber replacementSerial) {
        this.replacementSerial = replacementSerial;
    }

    public String getFaultDescription() {
        return faultDescription;
    }

    public void setFaultDescription(String faultDescription) {
        this.faultDescription = faultDescription;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public ClaimStatus getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(ClaimStatus claimStatus) {
        this.claimStatus = claimStatus;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }
}
