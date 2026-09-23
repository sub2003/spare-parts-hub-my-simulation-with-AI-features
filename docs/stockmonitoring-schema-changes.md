# Function 3 — Schema Changes

One schema change is required for an existing `spareparts_mysimulation` database.

## `stock_request.status`

Old lifecycle:

```text
pending | notified | fulfilled
```

New lifecycle:

```text
pending | ready_to_notify | notified | fulfilled
```

This is necessary because receiving stock means the shop **can contact** the customer; it does not mean that the customer has already been contacted.

Run once before starting the updated application:

`database/patch-stockmonitoring-ready-to-notify.sql`

The full `schema.sql` and `schema-simulation.sql` definitions have also been updated for fresh databases.

Existing `notified` rows are intentionally left unchanged because they may represent customers who were genuinely contacted before this upgrade.
