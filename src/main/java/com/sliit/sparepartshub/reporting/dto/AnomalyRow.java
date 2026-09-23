package com.sliit.sparepartshub.reporting.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AnomalyRow {
    private final String type;
    private final String severity;
    private final String explanation;
    private final String actor;
    private final String entity;
    private final String record;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final String evidence;
    private final List<Integer> auditLogIds;

    public AnomalyRow(String type,
                      String severity,
                      String explanation,
                      String actor,
                      String entity,
                      String record,
                      LocalDateTime startedAt,
                      LocalDateTime endedAt,
                      String evidence,
                      List<Integer> auditLogIds) {
        this.type = type;
        this.severity = severity;
        this.explanation = explanation;
        this.actor = actor;
        this.entity = entity;
        this.record = record;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.evidence = evidence;
        this.auditLogIds = List.copyOf(auditLogIds);
    }

    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getExplanation() { return explanation; }
    public String getActor() { return actor; }
    public String getEntity() { return entity; }
    public String getRecord() { return record; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public String getEvidence() { return evidence; }
    public List<Integer> getAuditLogIds() { return auditLogIds; }
}
