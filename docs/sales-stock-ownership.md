# Function 2 — Stock and Serial Ownership

## Stock deduction

Sales checkout is the source of truth for a normal sale stock deduction.

`Sale checkout -> stock_count decreases exactly once -> PickTicket is created`

Inventory/Picking does **not** deduct the requested quantity again. If the warehouse later performs a partial/exception pick, the existing Inventory logic returns the unpicked shortfall to stock so the net reduction equals the physically fulfilled quantity.

## Serial lifecycle at sale

For a serial-tracked product:

1. Sales Executive explicitly chooses the physical serial number(s).
2. Checkout validates that each serial belongs to the product, is `in_stock`, and has no sale.
3. Checkout locks those exact serial rows.
4. On successful checkout each chosen serial gets `sale_id = current sale` and status `sold`.
5. Warehouse picking checks the serials already assigned to that sale; it does not silently choose different serials.

## Concurrency protection

Product rows are pessimistically locked before final stock validation/deduction, and explicitly selected serial rows are pessimistically locked before assignment. This prevents two concurrent checkouts from consuming the same last product unit or selling the same serial number.
