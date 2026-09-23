# Function 2 — Sales / POS Verification

## Implemented

- Search by product code/SKU, name, category or brand.
- Cart add/update/remove/clear.
- Current stock and physical serial availability are checked before cart operations.
- Explicit serial selection for serial-tracked products.
- Exact serial count must equal the cart quantity.
- Duplicate, wrong-product, sold or otherwise unavailable serials are rejected.
- Compatibility conflicts continue to use existing ProductSpec + CompatibilityRule data.
- Compatibility override reason is mandatory when a conflict exists and is persisted on SaleItem.
- Fixed per-unit LKR discounts are server-calculated from catalog price and require a reason when greater than zero.
- Any stale cart price snapshot is rejected if the Product catalog price changed while the cart was open.
- Payment confirmation is required before checkout.
- Checkout is `@Transactional`.
- Product rows are write-locked before final stock validation.
- Selected serial rows are write-locked before assignment.
- Sale, SaleItems, stock deduction, serial assignment, PickTicket, PickTicketItems and audit writes are part of the same transaction.
- Stock decreases exactly once at Sale checkout.
- Session cart is cleared only after the transactional checkout returns successfully.
- Successful Sale triggers the existing stock-monitoring recalculation service.
- Receipt includes selected serial numbers and PickTicket QR.
- Sales actions use `SALE_COMPLETED`, `SALE_STOCK`, and `SALE_COMPATIBILITY_OVERRIDE` audit events.

## Regression/static checks performed in this environment

- Protected Supplier Java/templates: no differences from the working baseline.
- Protected Inventory/Product Management Java/templates: no differences from the working baseline.
- 122 project Java classes discovered from their declared packages.
- 0 unresolved internal `com.sliit.sparepartshub...` imports.
- 0 duplicate repository simple names.
- 41 HTML/Thymeleaf templates parsed as HTML without structural parser errors.
- A raw `javac` syntax pass over the Sales package showed dependency-resolution errors only (expected without the Maven classpath) and no Java syntax diagnostics such as `';' expected`, `illegal start`, or unclosed source constructs.

## Maven result

Attempted:

```powershell
.\mvnw.cmd clean compile
```

The sandbox cannot fetch Maven 3.9.16 from Maven Central, so a real Maven/Spring compile and application startup could not be completed here. Do **not** treat this as a local runtime pass.

Run the command on the user's development machine, then start `SparePartsHubApplication` and verify the workflow below.

## Local functional checklist

- [ ] Sales Executive can open `/sales`.
- [ ] SKU/name/category/brand search works.
- [ ] Add, update, remove and clear cart work.
- [ ] Invalid quantity is blocked.
- [ ] Serial-tracked quantity cannot exceed real in-stock serial count.
- [ ] Exact serial selection is required.
- [ ] Wrong-product serial is blocked.
- [ ] Duplicate serial is blocked.
- [ ] Previously sold serial is blocked.
- [ ] Compatibility conflict is shown.
- [ ] Compatibility override reason is required.
- [ ] Non-zero discount requires a reason and cannot exceed the current catalog unit price.
- [ ] Payment checkbox is required.
- [ ] Successful checkout creates Sale/SaleItems/PickTicket.
- [ ] Stock drops exactly once.
- [ ] Chosen serial becomes `sold` and links to the Sale.
- [ ] Receipt shows serial(s) and pick QR.
- [ ] Warehouse pick does not deduct stock a second time.
- [ ] Supplier Management still works.
- [ ] Supplier Portal still works.
- [ ] Inventory/Picking still works.
- [ ] Add/Edit Product still works.

## Database

Run `database/patch-sales-discount.sql` on an existing `spareparts_mysimulation` database before starting this version. It adds `sale_item.discount_amount` while preserving `price_at_sale` as the final historical unit price.
