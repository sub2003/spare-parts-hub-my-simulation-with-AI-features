# Stock and Serial Ownership

The final system uses one owner for each physical stock mutation.

| Event | Stock mutation | Serial mutation |
|---|---|---|
| Supplier receipt | Adds only the newly accepted quantity | Creates accepted serial-tracked units as `in_stock` |
| Sales checkout | Subtracts the sold cart quantity exactly once | Explicitly selected serials become `sold` and are linked to the Sale |
| Warehouse picking | No second sale stock deduction | Unpicked serials are released only when a partial/exception pick returns the shortfall to stock |
| Warranty replacement | Subtracts one replacement unit exactly once | Replacement serial becomes `replacement`; original becomes `returned` |
| Stock Monitoring | Recalculates urgency | Does not independently consume physical serials |

## Supplier receipt
`SupplierManagementService.receiveSingleItemShipment` calculates `remaining = ordered - received`, validates `accepted` against that remaining quantity, increments stock by `accepted`, increments `received_quantity` by `accepted`, and creates exactly one serial per accepted unit for serial-tracked products. The whole method is transactional.

## Sales checkout
`SalesService.checkout` is the sale stock owner. It write-locks products and selected serials, revalidates them, creates Sale/SaleItem/PickTicket data, decrements stock, links serials, writes audit events, and recalculates urgency inside one transaction.

## Picking
Inventory explicitly documents that Sales checkout already deducted stock. Picking therefore never subtracts it again. A partial/exception pick returns only the unpicked shortfall to stock and releases the corresponding unpicked serials.

## Warranty replacement
Warranty locks the product, original serial, and selected replacement serial. It decrements sellable stock once, sets the replacement serial to `replacement`, sets the original to `returned`, closes the RMA, audits the decision, and recalculates urgency.
