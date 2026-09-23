# Warranty / RMA Workflow

## Eligibility

A serial can open an RMA only when:

1. the serial exists,
2. it is linked to an original Sale,
3. it is still in the `sold` state,
4. the Product has a positive warranty period,
5. today is not after `sold_at + warranty_period_months`, and
6. no `pending` or `approved` claim already exists for that serial.

## Claim lifecycle

```text
sold serial
  -> create claim
  -> pending / resolution pending
  -> approve OR reject

reject
  -> claim status rejected
  -> original serial returns to sold state
  -> no Product stock change

approve
  -> claim status approved
  -> choose one final resolution
```

## Replacement

```text
approved RMA
  -> lock Product row
  -> lock original serial
  -> lock chosen replacement serial
  -> verify same Product
  -> verify replacement is in_stock + unsold + unused
  -> Product stock - 1 exactly once
  -> replacement serial = replacement
  -> original serial = returned
  -> claim resolution = replaced
  -> claim status = closed
  -> audit
  -> urgency recalculation
```

The lock order intentionally matches Sales/POS (Product before the physical sellable serial) to reduce product/serial lock inversion when a sale and replacement race for the last unit.

## Refund

Records `refunded`, closes the claim and sets the original serial to `returned`. It does not implement a fake payment gateway and does not silently increase Product stock.

## Send to manufacturer

Records `sent_to_manufacturer`, closes the claim and uses the existing non-sellable `returned` state for the original serial. No Product stock is automatically added.
