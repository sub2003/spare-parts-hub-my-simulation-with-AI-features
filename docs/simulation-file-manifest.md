# Integrated Simulation File Manifest

Compared with the simulation ZIP supplied before the integration work.

- Added: **63** files
- Changed: **38** files
- Removed: **1** file

## Added files

- `SIMULATION_README.md`
- `database/schema-simulation.sql`
- `database/simulation-seed.sql`
- `docs/simulation-build-notes.md`
- `docs/simulation-credentials.md`
- `docs/simulation-demo-script.md`
- `src/main/java/com/sliit/sparepartshub/inventory/dto/PickLineForm.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryAuditLogRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryPickTicketItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryPickTicketRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/repository/InventoryStorageLocationRepository.java`
- `src/main/java/com/sliit/sparepartshub/inventory/service/InventoryService.java`
- `src/main/java/com/sliit/sparepartshub/reporting/dto/ReportSummary.java`
- `src/main/java/com/sliit/sparepartshub/reporting/dto/SupplierVelocityRow.java`
- `src/main/java/com/sliit/sparepartshub/reporting/dto/TopProductRow.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/PortalPartnershipRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/PortalRestockOfferRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/PortalSupplierProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingAuditLogRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingPurchaseOrderItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingPurchaseOrderRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingRmaRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingSaleItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingSaleRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/security/SupplierSecurityConfig.java`
- `src/main/java/com/sliit/sparepartshub/reporting/service/ReportingService.java`
- `src/main/java/com/sliit/sparepartshub/reporting/service/SupplierPortalService.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/CartLine.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/CompatibilityConflict.java`
- `src/main/java/com/sliit/sparepartshub/sales/dto/SalesCart.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesAuditLogRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesCompatibilityRuleRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesPickTicketItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesPickTicketRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesProductSpecRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesSaleItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesSaleRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/repository/SalesSerialNumberRepository.java`
- `src/main/java/com/sliit/sparepartshub/sales/service/SalesService.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/dto/UrgencyRow.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/repository/MonitoringProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/repository/MonitoringPurchaseOrderItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/repository/MonitoringSaleItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/repository/MonitoringStockRequestRepository.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/service/StockMonitoringService.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/service/StockReceiptIntegrationService.java`
- `src/main/java/com/sliit/sparepartshub/warranty/dto/WarrantyLookup.java`
- `src/main/java/com/sliit/sparepartshub/warranty/repository/WarrantyAuditLogRepository.java`
- `src/main/java/com/sliit/sparepartshub/warranty/repository/WarrantyRmaRepository.java`
- `src/main/java/com/sliit/sparepartshub/warranty/repository/WarrantySerialRepository.java`
- `src/main/java/com/sliit/sparepartshub/warranty/service/WarrantyService.java`
- `src/main/resources/application.properties.simulation.example`
- `src/main/resources/templates/inventory/locations.html`
- `src/main/resources/templates/inventory/pick-ticket-detail.html`
- `src/main/resources/templates/inventory/pick-tickets.html`
- `src/main/resources/templates/inventory/scan.html`
- `src/main/resources/templates/reporting/sales.html`
- `src/main/resources/templates/sales/receipt.html`
- `src/main/resources/templates/stockmonitoring/stock-requests.html`
- `src/main/resources/templates/supplier-portal/order.html`

## Changed files

- `src/main/java/com/sliit/sparepartshub/entity/PartnershipRequest.java`
- `src/main/java/com/sliit/sparepartshub/entity/PurchaseOrder.java`
- `src/main/java/com/sliit/sparepartshub/entity/PurchaseOrderItem.java`
- `src/main/java/com/sliit/sparepartshub/entity/RestockOffer.java`
- `src/main/java/com/sliit/sparepartshub/entity/SaleItem.java`
- `src/main/java/com/sliit/sparepartshub/entity/StockRequest.java`
- `src/main/java/com/sliit/sparepartshub/entity/Supplier.java`
- `src/main/java/com/sliit/sparepartshub/inventory/controller/InventoryController.java`
- `src/main/java/com/sliit/sparepartshub/reporting/controller/ReportingController.java`
- `src/main/java/com/sliit/sparepartshub/reporting/controller/SupplierPortalController.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/SupplierRepository.java`
- `src/main/java/com/sliit/sparepartshub/reporting/security/CustomSupplierPrincipal.java`
- `src/main/java/com/sliit/sparepartshub/sales/controller/SalesController.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/controller/StockMonitoringController.java`
- `src/main/java/com/sliit/sparepartshub/stockmonitoring/repository/RestockSuggestionRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/controller/SupplierController.java`
- `src/main/java/com/sliit/sparepartshub/supplier/repository/PartnershipRequestRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/repository/PurchaseOrderItemRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/repository/PurchaseOrderRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/repository/RestockOfferRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/repository/SupplierProductRepository.java`
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierManagementService.java`
- `src/main/java/com/sliit/sparepartshub/warranty/controller/WarrantyController.java`
- `src/main/java/com/sliit/sparepartshub/web/PageController.java`
- `src/main/resources/application.properties`
- `src/main/resources/application.properties.example`
- `src/main/resources/static/css/app-shell.css`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/templates/inventory/index.html`
- `src/main/resources/templates/reporting/index.html`
- `src/main/resources/templates/sales/index.html`
- `src/main/resources/templates/stockmonitoring/index.html`
- `src/main/resources/templates/supplier-portal/dashboard.html`
- `src/main/resources/templates/supplier-portal/login.html`
- `src/main/resources/templates/supplier/purchase-order-detail.html`
- `src/main/resources/templates/supplier/receive-shipment.html`
- `src/main/resources/templates/supplier/suppliers.html`
- `src/main/resources/templates/warranty/index.html`

## Removed files

- `src/main/resources/static/fgf`
