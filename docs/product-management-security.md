# Product Management Security Notes

Product creation and editing are restricted server-side in the staff `SecurityConfig`.

- ADMIN: create, edit, view products.
- WAREHOUSE_CLERK: create, edit, view products.
- SALES_EXEC: may still view product inventory because POS requires product/stock visibility, but cannot access the Add/Edit routes or POST product administration changes.
- SUPPLIER: cannot access `/inventory/**`; Supplier Portal remains on its separate `@Order(1)` security chain.

The narrow Add/Edit matchers appear before the broader `/inventory/**` matcher because Spring Security evaluates matcher rules top-to-bottom.
