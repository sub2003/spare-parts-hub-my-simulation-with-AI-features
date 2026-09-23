package com.sliit.sparepartshub.reporting.dto;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.AuditReview;

public class AuditRow {
    private final AuditLog audit;
    private final AuditReview review;
    private final boolean anomaly;

    public AuditRow(AuditLog audit, AuditReview review, boolean anomaly) {
        this.audit = audit;
        this.review = review;
        this.anomaly = anomaly;
    }

    public AuditLog getAudit() { return audit; }
    public AuditReview getReview() { return review; }
    public boolean isAnomaly() { return anomaly; }
    public String getReviewStatus() {
        return review == null ? "unreviewed" : review.getReviewStatus().name();
    }
}
