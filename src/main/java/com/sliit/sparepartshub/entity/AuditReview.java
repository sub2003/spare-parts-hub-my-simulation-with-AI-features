package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Persistent administrative review of one immutable AuditLog event.
 * The original audit entry remains unchanged; this entity stores the
 * administrator's review decision separately.
 */
@Entity
@Table(name = "audit_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_audit_review_log", columnNames = "audit_log_id")
})
public class AuditReview {

    public enum ReviewStatus {
        reviewed,
        dismissed,
        action_required
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_log_id", nullable = false)
    private AuditLog auditLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_by", nullable = false)
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public AuditReview() {
    }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }
    public AuditLog getAuditLog() { return auditLog; }
    public void setAuditLog(AuditLog auditLog) { this.auditLog = auditLog; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
