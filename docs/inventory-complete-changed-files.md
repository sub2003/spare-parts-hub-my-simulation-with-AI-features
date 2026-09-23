# Function 1 — Changed Files

## Shared / build / database
- `pom.xml` — adds ZXing Core + JavaSE QR dependencies.
- `src/main/java/com/sliit/sparepartshub/entity/Product.java` — adds configurable `reorderLevel` mapping.
- `database/schema.sql` — adds `product.reorder_level`.
- `database/schema-simulation.sql` — adds `product.reorder_level`.
- `database/simulation-seed.sql` — includes demo reorder levels.
- `database/patch-inventory-reorder-level.sql` — patch for an existing `spareparts_mysimulation` DB.

## Inventory Java
- `inventory/controller/InventoryController.java`
- `inventory/service/InventoryService.java`
- `inventory/service/QrCodeService.java`
- `inventory/dto/InventoryProductRow.java`
- `inventory/dto/LocationForm.java`
- `inventory/dto/PickTicketLineView.java`
- `inventory/repository/InventoryPickTicketRepository.java`
- `inventory/repository/InventoryProductRepository.java`
- `inventory/repository/InventoryStorageLocationRepository.java`
- `inventory/repository/InventorySerialNumberRepository.java`

Existing `PickLineForm`, `InventoryAuditLogRepository`, and `InventoryPickTicketItemRepository` remain available.

## Inventory templates
- `templates/inventory/index.html`
- `templates/inventory/products.html`
- `templates/inventory/locations.html`
- `templates/inventory/location-form.html`
- `templates/inventory/scan.html`
- `templates/inventory/pick-ticket-detail.html`
- `templates/inventory/pick-tickets.html`

## Documentation
- `docs/inventory-complete-routes.md`
- `docs/inventory-complete-verification.md`
- `docs/inventory-complete-changed-files.md`
