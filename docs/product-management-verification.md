# Product Management Verification

## Implemented

- Add Product page and POST flow.
- Edit Product page and POST flow.
- Product code uniqueness validation (case-insensitive at service level; DB unique index when possible).
- Required name/category/brand validation.
- `BigDecimal` selling-price validation.
- Non-negative initial stock/reorder level/warranty validation.
- Optional real storage-location assignment.
- Explicit `product.serial_tracked` configuration.
- Serial-tracked new products must start at stock `0`; stock must arrive with serials through receiving.
- Serial-tracking mode conversion safeguards during edit.
- Normal edit never overwrites `stock_count`.
- `PRODUCT_CREATED` and `PRODUCT_UPDATED` audit entries.
- Product list includes Add/Edit actions, price, warranty and explicit serial status.
- Newly-created products automatically appear in existing Supplier → Manage Products because that page reads the canonical `product` table.
- Supplier receiving now requires exactly one serial per accepted unit for serial-tracked products, and rejects serial entry for standard products.
- Existing Sales/POS now checks serial availability for serial-tracked products before allowing quantity and only assigns serials for explicitly serial-tracked products.
- Supplier Portal/staff authentication separation is unchanged.

## Static verification performed in this environment

- 0 unresolved internal `com.sliit.sparepartshub...` imports.
- 0 duplicate Repository simple names.
- 41 Thymeleaf/HTML templates parsed without parser exceptions.
- `pom.xml` parsed as valid XML.
- `javac` syntax scan of changed Java files reported no syntax-style diagnostics; type resolution cannot finish here because Spring/JPA dependencies are unavailable to the raw compiler.

## Maven verification

`./mvnw clean compile` was attempted. The wrapper could not download Maven 3.9.16 from Maven Central in this environment, so a real Spring/Maven compile and application startup could not be completed here.

Run locally:

```powershell
.\mvnw.cmd clean compile
```

Then start `SparePartsHubApplication` and confirm:

- Hibernate `ddl-auto=validate` passes after the product-management SQL patch.
- `GET /inventory/products/new` opens for ADMIN/WAREHOUSE_CLERK.
- create/edit succeeds.
- duplicate code is rejected.
- serial-tracked product with positive initial stock is rejected.
- new product appears in Supplier → Manage Products.
- Supplier receiving still works for standard products and requires serials for tracked products.
- Supplier Portal still works.

## Known legacy-data limitation

Existing simulation databases may already contain products whose `stock_count` is higher than the number of recorded available serials. The migration does not invent missing serial numbers. Such products are safely limited by actual available serial rows in Sales/POS until the legacy data is reconciled or new serial-tracked stock is received.
