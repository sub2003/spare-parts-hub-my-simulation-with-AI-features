# Warranty / RMA Security

The existing staff SecurityConfig was intentionally left unchanged.

- `OPERATIONS_COORDINATOR`: allowed `/warranty/**`
- `ADMIN`: allowed `/warranty/**`
- `SALES_EXEC`: denied Warranty/RMA routes
- `WAREHOUSE_CLERK`: denied Warranty/RMA routes
- `INVENTORY_SUPERVISOR`: denied Warranty/RMA routes
- Supplier authentication: separate `SupplierSecurityConfig @Order(1)`; Supplier sessions do not gain staff Warranty access
- Staff filter chain remains `@Order(2)`

All state-changing eligibility rules are revalidated server-side. The replacement endpoint does not trust a replacement serial ID supplied by the browser: product ownership, status, Sale linkage, previous RMA use and stock are checked again under transaction locks.
