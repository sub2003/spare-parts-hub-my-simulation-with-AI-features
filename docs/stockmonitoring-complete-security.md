# Function 3 — Security Notes

The existing security configuration is preserved; this subset does not replace either security chain.

## Staff routes

- `/stockmonitoring/stock-requests/**` — `SALES_EXEC`, `INVENTORY_SUPERVISOR`, `ADMIN`.
- `/stockmonitoring/**` — `INVENTORY_SUPERVISOR`, `ADMIN`.

The stock-request matcher remains **before** the broader `/stockmonitoring/**` matcher because Spring Security evaluates them in order.

## Supplier Portal

- Supplier authentication remains separate under `/supplier-portal/**`.
- Supplier accounts are not staff `User` accounts.
- Suppliers do not receive access to staff Stock Monitoring routes.

## Procurement hand-off

Approved/modified restock suggestions are visible through the existing Admin-only Supplier Management procurement flow. Function 3 does not weaken `/supplier/**` authorization.
