# Serial Lifecycle

The final schema supports these serial states:

`in_stock`, `sold`, `returned`, `defective`, `replacement`

## Normal sale lifecycle
Supplier receipt → `in_stock` → explicit Sales/POS selection → `sold`

A sold serial is linked to exactly one Sale row and cannot be offered by POS again because POS requires both `current_status = in_stock` and `sale_id IS NULL`.

## Warranty lifecycle
When a valid sold serial enters an RMA, it is treated as non-sellable. On approval it is `defective`; on rejection it is restored to the logical `sold` state. Final refund/manufacturer handling uses the supported `returned` state.

## Replacement lifecycle
A replacement unit must start as `in_stock`, belong to the same Product, be unsold, and never have been used by another RMA. Final replacement changes it to `replacement`, which is non-sellable. The RMA stores `replacement_serial_id`.

## Invariants
- A physical serial value is unique.
- One serial cannot be selected in two concurrent Sales; selected rows are locked and revalidated.
- One replacement serial cannot be reused across RMAs; the service checks existing replacement use and write-locks the row.
- `sold` serials must not be offered to POS.
- `replacement`, `returned`, and `defective` serials are non-sellable.
