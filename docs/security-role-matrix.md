# Final Security Role Matrix

| Area | ADMIN | SALES_EXEC | WAREHOUSE_CLERK | INVENTORY_SUPERVISOR | OPERATIONS_COORDINATOR | SUPPLIER |
|---|---|---|---|---|---|---|
| Dashboard | Yes | Yes | Yes | Yes | Yes | Separate portal |
| Inventory general | Yes | Yes | Yes | No by current matcher | No | No |
| Product create/edit | Yes | No | Yes | No | No | No |
| Sales/POS | Yes | Yes | No | No | No | No |
| Stock requests | Yes | Yes | No | Yes | No | No |
| Stock Monitoring admin | Yes | No | No | Yes | No | No |
| Warranty/RMA | Yes | No | No | No | Yes | No |
| Supplier Management | Yes | No | No | No | No | No |
| Reporting/Audit/Staff Management | Yes | No | No | No | No | No |
| Supplier Portal | No staff principal | No | No | No | No | Yes, own data only |

## Filter chains
- `SupplierSecurityConfig @Order(1)` applies only to `/supplier-portal/**`.
- `SecurityConfig @Order(2)` handles internal staff.

## Supplier tenant isolation
Supplier Portal queries derive ownership from the authenticated supplier ID. Purchase-order detail uses `findByPoIdAndSupplier_SupplierId`; shipment updates call the same ownership check. Product terms, offers, and partnership history are supplier-scoped.

## Matcher ordering
`/stockmonitoring/stock-requests/**` remains before `/stockmonitoring/**`, preserving the narrower SALES_EXEC access to customer stock requests without granting the full urgency dashboard.
