# Integration Requirement Matrix

| Requirement | Implementation point | Verification status in generated artifact |
|---|---|---|
| Supplier receipt adds stock once | `SupplierManagementService.receiveSingleItemShipment` | Source inspected; local runtime test still required |
| Serial-tracked receipt requires exact serial count | Supplier receiving service | Source inspected |
| Sale checkout is atomic | `SalesService.checkout @Transactional` | Source inspected |
| Sale locks final stock and selected serials | Product/serial `FOR UPDATE` repository calls | Source inspected |
| Picking does not double-deduct | `InventoryService.completeTicket` | Source inspected |
| Partial/exception pick returns shortfall | Inventory service | Source inspected |
| Urgency reacts to sale/receipt/pick-return/warranty | StockMonitoring integration hooks | Source inspected |
| Warranty uses real sold serial | Warranty lookup/service | Source inspected |
| Replacement serial explicit + locked | `WarrantyService.replace` | Source inspected |
| Warranty replacement decrements once | Warranty service | Source inspected |
| Reporting uses real Sale/SaleItem data | Reporting repositories/service | Implemented + statically checked |
| CSV/PDF respect date filters | Reporting service/controller | Implemented + statically checked |
| Exports are audited | `REPORT_EXPORTED` | Implemented |
| Audit review persists separately | `AuditReview` / `audit_review` | Implemented; DB/runtime test required |
| Anomaly detection is explainable | deterministic ReportingService rules | Implemented |
| Supplier tenant isolation | supplier-scoped repository queries | Source inspected |
| Staff/Supplier auth remain separate | two security chains, ordered 1/2 | Source inspected |
| Hibernate schema is validation-only | properties example / project rule | Preserved |
