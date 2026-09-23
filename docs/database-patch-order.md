# Database Patch List and Order

## Fresh final simulation database — recommended
The final schema already contains every structural feature. For a new database:

1. `database/schema-simulation.sql`
2. `database/simulation-seed.sql`

Do not run the historical ALTER patches after creating the DB from this final schema.

## Existing older database — historical migration order
1. `patch-account-management.sql`
2. `patch-inventory-reorder-level.sql`
3. `patch-product-management.sql`
4. `patch-supplier-portal-integration.sql`
5. `patch-stockmonitoring-ready-to-notify.sql`
6. `patch-warranty-rma-workflow.sql`
7. `patch-reporting-audit-review.sql`
8. `patch-sales-discount.sql`

Optional demo data only:
- `patch-supplier-portal-demo-data.sql`

## Current working user database
For the Sales/POS discount fix in this version, run:

`database/patch-sales-discount.sql`

The patch is written to safely skip the column-add step if `sale_item.discount_amount` already exists. Do not rerun unrelated historical patches just for this discount fix.
