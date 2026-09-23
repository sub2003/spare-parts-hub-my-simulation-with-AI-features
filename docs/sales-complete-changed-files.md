# Function 2 — Sales / POS Changed Files

Current Sales/POS implementation plus the v8 fixed-discount upgrade.

## Java

- `src/main/java/com/sliit/sparepartshub/sales/controller/SalesController.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/CartLine.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/SalesCart.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/SerialOption.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesSerialNumberRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/service/SalesService.java`
- `src/main/java/com/sliit/sparepartshub/entity/SaleItem.java`
- `src/main/java/com/sliit/sparepartshub/reporting/service/ReportingService.java`

## Templates / frontend

- `src/main/resources/templates/sales/index.html`
- `src/main/resources/templates/sales/receipt.html`
- `src/main/resources/static/js/app.js`
- `src/main/resources/static/css/app-shell.css`

## Documentation

- `docs/sales-complete-changed-files.md`
- `docs/sales-complete-routes.md`
- `docs/sales-complete-security.md`
- `docs/sales-complete-verification.md`
- `docs/sales-stock-ownership.md`
- `docs/sales-discount-fix-v8.md`
- `docs/database-patch-order.md`

## Database

The v8 discount fix adds one historical transaction column:

- `sale_item.discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00`

Fresh databases include it in `schema.sql` / `schema-simulation.sql`. Existing databases must run `database/patch-sales-discount.sql` before startup because Hibernate is configured with `ddl-auto=validate`.
