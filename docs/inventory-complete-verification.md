# Function 1 — Inventory & Picking Completion Verification

## Implemented

- Real inventory dashboard counts: total products, low stock, out of stock, serial-tracked products, pending picks, partial picks, processed-today picks, and storage locations.
- Configurable `product.reorder_level`; low-stock logic no longer uses a hard-coded threshold.
- Full product inventory page with stock, reorder level, serial counts, exact location, and urgency score.
- Storage-location create/edit/list, duplicate code validation, product assignment, and location QR PNG.
- Pick-ticket QR PNG generation from `PickTicket.ticketCode`.
- Server-side QR decode from an uploaded/captured image using ZXing, plus manual ticket-code fallback.
- Completed/partial/exception tickets are rejected by the scan flow instead of being reopened for processing.
- Exact aisle/shelf/bin display on pick-ticket detail.
- Full/partial/exception pick outcomes using `picked_quantity`.
- Sales remains the stock-deduction owner; picking never deducts the requested stock a second time.
- Partial/exception picks return only the unpicked shortfall to inventory.
- For products with serial history, the clerk must enter the exact serial number(s) physically picked. The service verifies they belong to that sale/product and match the picked quantity. Serial units corresponding to an unpicked shortfall are released back to `in_stock` and detached from the sale.
- Inventory actions write audit records with valid JSON escaping.
- Existing `/inventory/**` security matcher was preserved.
- Supplier package/routes/security were not rewritten.

## Required database patch for an existing simulation DB

Run:

`database/patch-inventory-reorder-level.sql`

before starting the updated application. Hibernate remains `ddl-auto=validate`.

## Build verification in this environment

`./mvnw clean compile` was attempted. The Maven wrapper could not download Maven 3.9.16 from Maven Central in this execution environment, so a real Maven compile/startup could not be honestly completed here.

Run locally on Windows:

```powershell
.\mvnw.cmd clean compile
```

Then start `SparePartsHubApplication` and confirm the usual `Tomcat started` / `Started SparePartsHubApplication` log messages.

## Manual checks to run locally

1. Open `/inventory` and verify dashboard counts are database-backed.
2. Open `/inventory/products`; update one reorder level and verify low-stock classification changes.
3. Create a location, edit it, attempt a duplicate code, and open its QR PNG.
4. Assign a product to the new location.
5. Open a pending pick ticket and its QR PNG.
6. Save/screenshot that QR and upload it through `/inventory/pick-tickets/scan`.
7. Verify manual ticket-code fallback.
8. Verify a completed/partial/exception ticket is blocked by the scan flow.
9. Process one full pick.
10. Process a fresh ticket with a partial quantity; confirm only the shortfall returns to stock.
11. Process a fresh ticket with all zero quantities; confirm `exception`.
12. For a serial-tracked product, verify assigned serials are displayed and an unpicked serial is released on shortfall.
13. Regression-test Supplier Records, Supplier Portal, PO receiving, and Manage Supplier Products.
