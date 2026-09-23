package com.sliit.sparepartshub.reporting.dto;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.AuditReview;

public class AuditDetailView {
    private final AuditLog audit;
    private final AuditReview review;
    private final String oldValue;
    private final String newValue;

    public AuditDetailView(AuditLog audit, AuditReview review, String oldValue, String newValue) {
        this.audit = audit;
        this.review = review;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public AuditLog getAudit() { return audit; }
    public AuditReview getReview() { return review; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}
