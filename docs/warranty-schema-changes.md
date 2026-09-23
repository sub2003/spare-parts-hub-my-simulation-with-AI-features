# Warranty / RMA Schema Changes

Apply `database/patch-warranty-rma-workflow.sql` once to the existing `spareparts_mysimulation` database before starting this project version.

## `rma_claim`

Added:

- `claim_status ENUM('pending','approved','rejected','closed')`
- `reviewed_by` → `users.user_id`
- `resolved_by` → `users.user_id`
- `replacement_serial_id` → `serial_number.serial_id`
- `rejection_reason VARCHAR(255)`
- `resolution_notes VARCHAR(500)`
- `created_at DATETIME`
- `reviewed_at DATETIME`
- `resolved_at DATETIME`
- `closed_at DATETIME`
- unique index on non-null `claim_code` where legacy data permits it

Existing legacy rows are mapped so `resolution='pending'` remains claim status `pending`, while already-finalized legacy resolutions become `closed`.

## `serial_number.current_status`

Extended from:

`in_stock, sold, returned, defective`

to:

`in_stock, sold, returned, defective, replacement`

`replacement` is a deliberately non-sellable state for a physical unit consumed as an RMA replacement. Sales/POS already selects only `in_stock` + unsold serials, so replacement units cannot be sold again.

## Canonical schema files

Both `database/schema.sql` and `database/schema-simulation.sql` were updated for fresh database creation. Do not rerun either full schema file over an existing populated simulation database; use the patch instead.
