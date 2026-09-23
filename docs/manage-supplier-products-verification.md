# Manage Supplier Products — Verification Notes

## Intended workflow

1. Admin opens `/supplier/records`.
2. Clicks **Manage Products** for the target supplier.
3. Selects only products that supplier actually provides.
4. Enters supplier-specific **Supply Price**, **MOQ**, and **Lead Time**.
5. Saves.
6. `supplier_product` rows are created/updated/deleted transactionally.
7. Admin logs out and the supplier logs into `/supplier-portal/login`.
8. Supplier Portal `Products Supplied` count and Restock/Bulk Offer dropdown now use the new links automatically.

## Server-side safeguards

- Supplier must exist.
- Supplier must be active before assignments can be saved.
- Product IDs are resolved from the database; posted labels are not trusted.
- Duplicate product rows in one request are rejected.
- Selected products require price > 0, MOQ >= 1, and lead time >= 0.
- Unchecking an existing product removes only that supplier-product relationship.
- Updates are transactional.
- Change is audited with action `SUPPLIER_PRODUCTS_UPDATED`.

## Static checks completed in this environment

- 113 Java files scanned.
- 0 unresolved internal `com.sliit.sparepartshub...` imports.
- 0 duplicate `*Repository` simple names.
- 0 rough Java delimiter/balance errors.
- 37 HTML templates parsed without parser exceptions.
- 0 missing local CSS/JS/image references detected.
- New DTO/form/validation classes compile successfully with `javac`.

## Real Maven/runtime verification

A full Spring/Maven compile and application startup could not be performed in this environment because Maven is not installed and the wrapper distribution/dependencies are not available locally. Run on the user's machine:

```powershell
.\mvnw.cmd clean compile
```

Then start `SparePartsHubApplication` and verify Hibernate `ddl-auto=validate` succeeds.

## Database patch

None required for this feature.
