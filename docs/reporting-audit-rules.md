# Reporting / Audit Rules

## Persistent review
AuditLog remains the immutable event. AuditReview stores the Admin's decision:
- `reviewed`
- `dismissed`
- `action_required`

`action_required` requires a non-blank note.

Review decisions generate:
- `AUDIT_REVIEWED`
- `AUDIT_DISMISSED`
- `AUDIT_ACTION_REQUIRED`

To avoid a recursive review-of-review audit chain, reviewing an `AUDIT_*` event does not generate another review audit event.

## Enabled deterministic anomaly rules

### REPEATED_COMPATIBILITY_OVERRIDES
Trigger: 3 or more `SALE_COMPATIBILITY_OVERRIDE` audit events by the same staff user inside a rolling 30-minute window.

### REPEATED_PRODUCT_EDITS
Trigger: 5 or more `PRODUCT_UPDATED` events for the same product inside a rolling 60-minute window.

### SENSITIVE_ACCOUNT_ACTIVITY
Trigger: 3 or more of the following by the same Admin inside a rolling 60-minute window:
- `ACCOUNT_CREATED`
- `ACCOUNT_UPDATED`
- `ROLE_CHANGED`
- `ACCOUNT_ACTIVATED`
- `ACCOUNT_DEACTIVATED`
- `PASSWORD_RESET`

## Deliberately not enabled
- Large-discount anomaly: the existing audit trail does not reliably preserve an immutable original list price for every sale-line comparison.
- Abnormal-stock-adjustment anomaly: only enable when the system has a dedicated, reliable stock-adjustment audit event/value.

This avoids fabricating evidence from current mutable catalog state.

## Export audit
CSV and PDF exports generate `REPORT_EXPORTED` with report type, format, from/to dates, and generated timestamp.
