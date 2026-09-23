# Function 3 — Urgency Formula

## Inputs

The Stock Monitoring module calculates urgency from live operational data for each product:

- **Current stock** — `product.stock_count`.
- **Reorder level** — `product.reorder_level`.
- **30-day sales** — quantity sold from `sale_item` during the last 30 days.
- **Daily sales velocity** — `30-day sold quantity / 30`, rounded to two decimals for display.
- **Open customer demand** — total `stock_request.requested_quantity` for every request that is not `fulfilled`.
- **Incoming PO quantity** — remaining quantity on purchase-order items whose PO is `pending`, `shipped`, or `partially_received`:
  `max(0, quantity_ordered - received_quantity)`.

## Target stock

The target used by the formula is:

```text
targetStock = reorderLevel
            + ceil(dailySalesVelocity × 14)
            + openCustomerDemand
```

The 14-day component gives the score a short replenishment-coverage horizon while still using the actual 30-day sales history.

## Urgency score

The score is deterministic and clamped to `0–100`:

```text
shortage        = max(0, targetStock - currentStock)

shortagePressure = clamp((shortage / targetStock) × 60, 0, 60)
salesPressure    = clamp((sold30 / max(1,reorderLevel)) × 20, 0, 20)
demandPressure   = clamp((openDemand / max(1,reorderLevel)) × 20, 0, 20)
stockOutBoost    = 10 when currentStock == 0, otherwise 0
incomingRelief   = clamp((incomingPO / targetStock) × 30, 0, 30)

urgencyScore = clamp(
    shortagePressure
  + salesPressure
  + demandPressure
  + stockOutBoost
  - incomingRelief,
  0,
  100
)
```

The score is stored in `product.urgency_score` with two decimal places.

## Classification

- **Safe:** score below `40`.
- **Warning:** score `40` through `69.99`.
- **Critical:** score `70` or above.

## Suggested reorder quantity

```text
suggestedQuantity = max(
    0,
    reorderLevel
  + ceil(dailySalesVelocity × 14)
  + openCustomerDemand
  - currentStock
  - incomingPOQuantity
)
```

A new suggestion is generated only when:

- urgency score is at least `40`, and
- the suggested quantity is greater than zero, and
- that product does not already have an unresolved `pending`, `approved`, or `modified` suggestion.

## Recalculation triggers

Urgency is recalculated by:

1. new product creation after the product has been persisted and has an ID,
2. product edits that can change the reorder level,
3. inline reorder-level updates from Product Inventory,
4. one scheduled Function 3 job (default every hour),
5. the Inventory Supervisor/Admin manual **Recalculate Now** action,
6. successful Sales/POS checkout,
7. accepted Supplier PO stock receipt,
8. inventory pick reconciliation when stock is returned after a short/exception pick,
9. other existing focused stock/demand events that already call `recalculateProduct(...)` (for example warranty replacement and stock-request lifecycle changes).

The scheduler defaults are configurable in `application.properties`:

```properties
app.stockmonitoring.recalc-fixed-delay-ms=3600000
app.stockmonitoring.recalc-initial-delay-ms=10000
```

## Customer request rule

Receiving stock does **not** claim that the customer was contacted. Eligible requests move:

```text
pending → ready_to_notify → notified → fulfilled
```

`notified_at` is populated only when a staff member actually marks the request **Notified**.
