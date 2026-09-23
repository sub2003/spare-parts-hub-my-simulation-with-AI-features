# Product Management Routes

## Product inventory

| Method | Route | Purpose | Roles |
|---|---|---|---|
| GET | `/inventory/products` | Product inventory list | ADMIN, WAREHOUSE_CLERK, SALES_EXEC |
| GET | `/inventory/products/new` | Add Product form | ADMIN, WAREHOUSE_CLERK |
| POST | `/inventory/products` | Create product | ADMIN, WAREHOUSE_CLERK |
| GET | `/inventory/products/{id}/edit` | Edit Product form | ADMIN, WAREHOUSE_CLERK |
| POST | `/inventory/products/{id}` | Update safe product fields | ADMIN, WAREHOUSE_CLERK |
| POST | `/inventory/products/{id}/reorder-level` | Update reorder level | ADMIN, WAREHOUSE_CLERK |

## Existing integration routes used by this feature

- `/supplier/records/{supplierId}/products` — Admin explicitly links a newly-created inventory product to a supplier and enters supplier-specific price/MOQ/lead time.
- `/sales/**` — Sales/POS reads the same `product` rows. Serial-tracked products are now capped by valid `in_stock` serial availability as well as stock count.
- `/inventory/locations` — Storage locations used by Add/Edit Product.

Supplier Portal authentication remains separate under `/supplier-portal/**`.
