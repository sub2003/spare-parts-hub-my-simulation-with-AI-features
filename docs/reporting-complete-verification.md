# Function 6 — Reporting / Audit Verification

## Scope
Function 6 was implemented on top of the Warranty-complete working baseline. Supplier Management, Supplier Portal, Inventory/Picking, Product Management, Sales/POS, Stock Monitoring/Urgency, Warranty/RMA, Staff Account Management, and both security chains were treated as protected modules.

## Implemented behavior
- Admin-only reporting dashboard backed by real Sale, SaleItem, Product urgency, PurchaseOrder, RmaClaim, and AuditLog data.
- Sales report with no date filter, from-only, to-only, and from+to date handling.
- Date range validation (`from <= to`) with inclusive end-date behavior.
- Top products calculated from SaleItem quantity and sale-time revenue.
- CSV export with active date filters and spreadsheet-formula injection protection for textual cells.
- Dependency-free generated PDF report using the built-in `SimplePdfWriter`.
- `REPORT_EXPORTED` audit events for CSV/PDF exports.
- Audit list, filters, detail view, JSON-like pretty formatting, and sensitive-value redaction.
- Persistent, separate `AuditReview` entity with reviewed/dismissed/action_required states.
- `action_required` review requires a note.
- Deterministic anomaly rules based only on reliable audit evidence:
  - 3+ compatibility overrides by the same actor within 30 minutes.
  - 5+ product updates to the same product within 60 minutes.
  - 3+ sensitive staff-account actions by the same admin within 60 minutes.
- Staff Account Management remains under `/reporting/staff/**`.

## Database
A single new table is required on an existing database: `audit_review`.

Run: `database/patch-reporting-audit-review.sql`

Do not rerun earlier patches that are already applied.

## Static verification performed
- Java source files: 131
- Thymeleaf/HTML templates: 45
- Unresolved internal project imports: 0
- Duplicate Repository simple names: 0
- Duplicate Spring component simple names: 0
- Template parse errors: 0
- Missing local static references detected from templates: 0
- Rough duplicate controller route signatures: 0
- `SimplePdfWriter` compiled standalone with the JDK and produced a valid PDF 1.4 file.

## Maven / startup status
A real Maven compile was attempted with:

`./mvnw -q -DskipTests clean compile`

The build could not start dependency resolution because the execution environment cannot resolve/download Maven 3.9.16 from Maven Central:

`wget: Failed to fetch https://repo.maven.apache.org/.../apache-maven-3.9.16-bin.zip`

Therefore this artifact does **not** claim a successful Spring Boot compile or startup inside the generation environment. Final verification must be run locally:

1. `.\mvnw.cmd clean compile`
2. start `SparePartsHubApplication`
3. confirm Hibernate `ddl-auto=validate` passes
4. manually verify Reporting routes as Admin.

## Manual checks to run locally
- `/reporting`
- `/reporting/sales`
- `/reporting/sales/export.csv`
- `/reporting/sales/export.pdf`
- `/reporting/audit`
- `/reporting/audit/{id}`
- `/reporting/anomalies`
- `/reporting/staff`

Verify the exports create `REPORT_EXPORTED` entries and that an AuditReview persists after an application restart.

## Known evidence limitation
Large-discount and abnormal-stock-adjustment anomaly rules are deliberately not enabled unless a trustworthy historical audit value exists for the exact value being judged. The build does not infer missing evidence from current catalog values.
