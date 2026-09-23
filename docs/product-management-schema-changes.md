# Product Management Schema Changes

A schema change is required for explicit serial tracking.

## New column

```sql
product.serial_tracked BOOLEAN NOT NULL DEFAULT FALSE
```

Why: the previous Inventory implementation inferred serial tracking from historical `serial_number` rows. That cannot represent a newly-created serial-tracked product before its first shipment is received.

## Product code uniqueness

Fresh schemas now define `product.product_code` as `NOT NULL UNIQUE`.

For an existing `spareparts_mysimulation` database, `database/patch-product-management.sql`:

1. adds `serial_tracked`;
2. marks products that already have serial records as serial-tracked;
3. adds a unique index on `product_code` only when legacy duplicate codes do not exist.

Run the patch once after the earlier Inventory reorder-level patch.
