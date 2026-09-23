# Sales / POS Discount Fix v8

## Root cause

The previous POS did not have a discount-amount input. The editable numeric field was `price` (the final negotiated unit price), while the field labelled **Discount / negotiated-price reason** was only the text `discountReason`. Entering `10000` into that reason field therefore stored the text `10000` but left the unit price at `105000`, so the cart total and receipt remained `105000` under the old implementation.

## New transaction model

- `catalogPrice`: current unit price snapshot reviewed in the cart.
- `discountAmount`: fixed LKR discount per unit.
- `unitPrice`: server-computed final unit price = catalog price - discount amount.
- `discountReason`: required when discount amount > 0.
- `sale_item.price_at_sale`: final historical unit price actually charged.
- `sale_item.discount_amount`: historical per-unit discount.
- Historical original unit price = `price_at_sale + discount_amount`.

## Formula

```text
finalUnitPrice = catalogPrice - discountAmount
lineSubtotal   = catalogPrice * quantity
lineDiscount   = discountAmount * quantity
lineTotal      = finalUnitPrice * quantity
cartTotal      = sum(lineTotal)
```

For LKR 105,000.00, quantity 1, discount LKR 10,000.00:

```text
Subtotal = 105,000.00
Discount = 10,000.00
Total    = 95,000.00
```

## Existing database

Run `database/patch-sales-discount.sql` before application startup because Hibernate schema validation expects the new `sale_item.discount_amount` column.
