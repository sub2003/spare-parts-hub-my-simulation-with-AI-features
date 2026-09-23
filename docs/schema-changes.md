# Final Schema Changes and Migration Order

## Current final schema
`database/schema-simulation.sql` contains the final integrated simulation schema, including:
- active staff accounts
- Product reorder level and serial tracking
- Supplier Portal integration fields
- customer request `ready_to_notify`
- Warranty/RMA workflow fields and replacement serial state
- persistent `audit_review`

## Fresh final database
For a brand-new database using this final build:
1. run `database/schema-simulation.sql`
2. run `database/simulation-seed.sql`

Do **not** run historical ALTER patches after using the final schema; their changes are already incorporated.

## Existing database historical upgrade order
For an older existing simulation database, the project contains these migrations in dependency order:
1. `patch-account-management.sql`
2. `patch-inventory-reorder-level.sql`
3. `patch-product-management.sql`
4. `patch-supplier-portal-integration.sql`
5. `patch-stockmonitoring-ready-to-notify.sql`
6. `patch-warranty-rma-workflow.sql`
7. `patch-reporting-audit-review.sql`

`patch-supplier-portal-demo-data.sql` is optional demo data, not a required schema migration.

## User's current database
The previously completed patches through Warranty/RMA are already part of the user's working database. For this Reporting stage, the **only new required patch is**:
`database/patch-reporting-audit-review.sql`

Hibernate remains `spring.jpa.hibernate.ddl-auto=validate`.
