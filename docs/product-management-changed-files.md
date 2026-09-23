# Product Management Changed Files

## New files

- `database/patch-product-management.sql`
- `src/main/java/com/sliit/sparepartshub/inventory/dto/CreateProductForm.java`
- `src/main/java/com/sliit/sparepartshub/inventory/dto/EditProductForm.java`
- `src/main/java/com/sliit/sparepartshub/inventory/service/ProductValidationException.java`
- `src/main/resources/templates/inventory/product-create.html`
- `src/main/resources/templates/inventory/product-edit.html`
- `docs/product-management-routes.md`
- `docs/product-management-security.md`
- `docs/product-management-schema-changes.md`
- `docs/product-management-verification.md`
- `docs/product-management-changed-files.md`

## Modified files

- `database/schema.sql`
- `database/schema-simulation.sql`
- `database/simulation-seed.sql`
- `src/main/java/com/sliit/sparepartshub/entity/Product.java`
- `src/main/java/com/sliit/sparepartshub/inventory/controller/InventoryController.java`
- `src/main/java/com/sliit/sparepartshub/inventory/dto/InventoryProductRow.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/service/InventoryService.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesSerialNumberRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/service/SalesService.java`
- `src/main/java/com/sliit/sparepartshub/security/SecurityConfig.java`
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierManagementService.java`
- `src/main/resources/static/css/app-shell.css`
- `src/main/resources/templates/inventory/products.html`

The Supplier service change is intentionally narrow: it only enforces the new explicit serial-tracking rule during PO receiving. Existing Supplier routes/UI/authentication are otherwise preserved.
