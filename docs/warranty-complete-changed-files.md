# Warranty / RMA Changed Files

## Added

- `database/patch-warranty-rma-workflow.sql`
- `src/main/resources/templates/warranty/rma-details.html`
- `docs/warranty-complete-verification.md`
- `docs/warranty-complete-routes.md`
- `docs/warranty-complete-security.md`
- `docs/warranty-schema-changes.md`
- `docs/warranty-workflow.md`

## Updated Function 4 files

The project already had an unusual physical folder nesting under `warranty/controller/...`; those paths were intentionally preserved so a files-only overlay does not leave duplicate Java classes behind.

- `src/main/java/com/sliit/sparepartshub/warranty/controller/controller/WarrantyController.java`
- `src/main/java/com/sliit/sparepartshub/warranty/controller/dto/WarrantyLookup.java`
- `src/main/java/com/sliit/sparepartshub/warranty/controller/repository/WarrantyRmaRepository.java`
- `src/main/java/com/sliit/sparepartshub/warranty/controller/repository/WarrantySerialRepository.java`
- `src/main/java/com/sliit/sparepartshub/warranty/controller/service/WarrantyService.java`
- `src/main/resources/templates/warranty/index.html`

The existing function-local `WarrantyAuditLogRepository` was left physically unchanged for overlay compatibility, but the new `WarrantyService` uses the shared `com.sliit.sparepartshub.repository.AuditLogRepository` for actual audit writes.

## Updated shared model/schema required by Function 4

- `src/main/java/com/sliit/sparepartshub/entity/RmaClaim.java`
- `src/main/java/com/sliit/sparepartshub/entity/SerialNumber.java`
- `database/schema.sql`
- `database/schema-simulation.sql`

## Protected modules intentionally unchanged

- `supplier/`
- `inventory/`
- `sales/`
- `stockmonitoring/`
- `reporting/`
- `security/`
