# Warranty / RMA Completion Verification

## Scope

Function 4 was upgraded on top of the protected Stock Monitoring baseline. Supplier Management, Supplier Portal, Inventory/Picking, Product Management, Sales/POS, Stock Monitoring and staff security were not rewritten.

## Implemented behavior

- Real serial lookup from `serial_number`.
- Original Sale linkage and sold timestamp display.
- Warranty expiry calculated as `sale.sold_at + product.warranty_period_months`.
- Claim eligibility requires a real sold serial, a configured positive warranty period, a non-expired warranty, sold serial state and no unresolved RMA.
- Claim workflow state is separate from resolution:
  - status: `pending`, `approved`, `rejected`, `closed`
  - resolution: `pending`, `replaced`, `refunded`, `sent_to_manufacturer`
- Unique `RMA-######` code generated from the database claim ID.
- Rejection reason persisted.
- Review actor/time and resolution actor/time persisted.
- Replacement serial persisted as a foreign key, not only free-text.
- Replacement candidates are restricted to the same product, `in_stock`, unsold serials not already used by another RMA.
- Replacement finalization uses one transaction with pessimistic locking.
- Warranty replacement reduces sellable Product stock exactly once.
- Replacement serial moves to the non-sellable `replacement` state.
- Original serial moves to `returned` after a completed replacement/refund/manufacturer-return resolution.
- Rejected claims restore the original serial to its already-sold state.
- Replacement triggers Function 3 urgency recalculation.
- Audit events implemented:
  - `RMA_CREATED`
  - `RMA_APPROVED`
  - `RMA_REJECTED`
  - `RMA_REPLACED`
  - `RMA_REFUNDED`
  - `RMA_SENT_TO_MANUFACTURER`
- Audit JSON escaping uses control-character-safe encoding.
- RMA list, lookup/history and dedicated detail/action UI added.

## Static verification completed here

- 124 Java files discovered.
- 0 unresolved internal `com.sliit.sparepartshub.*` imports.
- 0 duplicate Repository filenames.
- 0 duplicate Spring component simple names.
- 42 Thymeleaf/HTML templates parsed without HTML parser errors.
- 0 missing direct local static resource references.
- Java parser-stage check on all changed Java files produced no syntax-style diagnostics; expected missing third-party classpath diagnostics remain because Maven dependencies are not installed in this environment.

## Protected-module regression diff

Compared with `spare-parts-hub-stockmonitoring-complete.zip`:

- `supplier/`: 0 differences
- `inventory/`: 0 differences
- `sales/`: 0 differences
- `stockmonitoring/`: 0 differences
- `reporting/`: 0 differences
- `security/`: 0 differences

Shared changes are limited to the Warranty-owned RMA/serial lifecycle schema/entities required for Function 4.

## Maven / startup status

A real Maven build was attempted with the project Maven Wrapper. The wrapper could not download Maven 3.9.16 from Maven Central in this execution environment, so a real Spring compile/startup cannot honestly be claimed here.

Required local verification:

```powershell
.\mvnw.cmd clean compile
```

Then run `SparePartsHubApplication` after applying `database/patch-warranty-rma-workflow.sql` to the existing `spareparts_mysimulation` database.

## Manual Function 4 verification checklist

- [ ] `/warranty` loads as Operations Coordinator/Admin
- [ ] unknown serial gives clear error
- [ ] unsold serial cannot create RMA
- [ ] expired warranty cannot create RMA
- [ ] valid sold serial can create pending RMA
- [ ] second unresolved RMA for same serial is blocked
- [ ] pending claim can be approved
- [ ] pending claim can be rejected only with reason
- [ ] rejected claim changes no stock
- [ ] approved claim shows only valid same-product replacement serials
- [ ] tampered wrong-product serial is rejected server-side
- [ ] sold/defective/reused replacement serial is rejected server-side
- [ ] completed replacement reduces Product stock exactly once
- [ ] completed replacement marks replacement serial `replacement`
- [ ] original serial becomes `returned`
- [ ] urgency recalculates after replacement
- [ ] refund changes no inventory stock
- [ ] manufacturer-return changes no inventory stock
- [ ] serial RMA history renders
- [ ] all Warranty audit actions are written
- [ ] Sales/POS can no longer sell a serial used as a warranty replacement

## Important remaining gate

Per the master prompt, Reporting/Audit and final cross-module integration should not be started until this Warranty build starts successfully on the user's machine and the Function 4 workflow is manually verified.
