# Function 3 — Stock Monitoring / Urgency Verification

## Scope completed

This subset completes Function 3 only. Warranty/RMA and Reporting/Audit were intentionally not started because the master implementation instructions require one module to be completed and verified before proceeding.

Implemented:

- deterministic urgency score from current stock, reorder level, 30-day sales, unresolved customer demand, and remaining incoming PO quantity;
- Safe / Warning / Critical classification;
- persisted `product.urgency_score`;
- hourly scheduled recalculation with configurable delay;
- manual **Recalculate Now** action with `URGENCY_RECALCULATED` audit entry;
- existing Sales checkout recalculation hook preserved;
- existing Supplier receiving hook preserved and upgraded;
- inventory short/exception-pick reconciliation now triggers a focused product recalculation;
- restock suggestion generation with duplicate-unresolved-suggestion protection;
- approve / modify / reject workflow and review audit logging;
- existing Supplier Management hand-off reused for approved/modified restock suggestions;
- customer stock-request lifecycle: `pending → ready_to_notify → notified → fulfilled`;
- Supplier stock receipt moves only eligible FIFO pending requests to **Ready to Notify** and never falsely marks them **Notified**;
- strict server-side request lifecycle transitions;
- premium Stock Monitoring and Stock Request pages using the existing app shell.

## Database migration

Required for an existing simulation database:

`database/patch-stockmonitoring-ready-to-notify.sql`

This widens the existing `stock_request.status` enum with `ready_to_notify`.

No other Function 3 schema change is required.

## Static verification performed in the implementation environment

- Java source files discovered: **124**.
- Unresolved internal `com.sliit.sparepartshub...` imports: **0**.
- Duplicate `*Repository` simple names: **0**.
- Duplicate detected Spring component simple names: **0**.
- Thymeleaf/HTML templates parsed by an HTML parser: **41**, parse failures: **0**.
- Missing direct local `/css`, `/js`, `/images` references found by static scan: **0**.
- `javac` parser-oriented scan of the changed Function 3 Java sources showed no syntax/parser diagnostics; dependency/classpath errors are expected without Maven's Spring/JPA libraries.
- Staff `SecurityConfig` is byte-for-byte unchanged from the Sales/POS baseline.
- `SupplierSecurityConfig` is byte-for-byte unchanged from the Sales/POS baseline.
- Supplier Java package: **0 differences** from protected baseline.
- Supplier templates: **0 differences** from protected baseline.
- Supplier Portal templates: **0 differences** from protected baseline.
- Sales Java package: **0 differences** from protected baseline.
- Sales templates: **0 differences** from protected baseline.
- Inventory templates: **0 differences** from protected baseline.
- Product entity: **unchanged**.
- Only one protected Inventory Java file was intentionally changed: `InventoryService.java`, solely to recalculate urgency after stock reconciliation.

## Integration points confirmed statically

- Sales/POS still calls `StockMonitoringService.recalculateAll()` after successful checkout.
- Supplier receiving still calls `StockReceiptIntegrationService.onStockReceived(product)` after stock has already been added.
- `StockReceiptIntegrationService` does **not** modify stock, preventing a second stock addition.
- Existing Supplier Management already reads `approved` and `modified` Restock Suggestions for the restock-requirements / supplier-comparison flow.

## Maven compile / startup result

A real Maven compile was attempted with:

```text
./mvnw -q -DskipTests clean compile
```

The wrapper could not download Maven 3.9.16 from Maven Central in this execution environment:

```text
wget: Failed to fetch https://repo.maven.apache.org/.../apache-maven-3.9.16-bin.zip
```

Therefore **a successful Maven compile and Spring Boot startup are not claimed here**.

The required final verification on the user's Windows machine is:

```powershell
.\mvnw.cmd clean compile
```

then run `SparePartsHubApplication` and confirm:

```text
Tomcat started on port 8080
Started SparePartsHubApplication
```

## Manual functional checks to run locally

1. Run the DB patch before starting the updated app.
2. Login as `INVENTORY_SUPERVISOR` or `ADMIN` and open `/stockmonitoring`.
3. Press **Recalculate Now** and confirm scores/classes change from real data.
4. Confirm a high-urgency product receives at most one unresolved suggestion.
5. Approve/modify/reject a pending suggestion.
6. As Admin, open Supplier Management restock requirements and confirm approved/modified suggestions appear.
7. Create a customer stock request for a low/out-of-stock product.
8. Receive that product through the existing Supplier PO receiving flow.
9. Confirm the request becomes **Ready to Notify**, not **Notified**.
10. Mark it **Notified**, then **Fulfilled**.
11. Complete a Sales/POS checkout and confirm urgency recalculates while stock decreases only once.
12. Regression-test Supplier, Inventory/Picking, Product Management and Sales/POS.

## Remaining limitation within the existing schema

`restock_suggestion` has no separate `consumed_by_po`/closed procurement state. Approved/modified suggestions therefore remain the procurement queue items defined by the current four-status model (`pending`, `approved`, `modified`, `rejected`). This subset does not invent a fifth status that was not requested. The existing Supplier Management flow is reused unchanged.
