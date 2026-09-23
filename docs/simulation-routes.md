# Integrated Simulation Routes

## `inventory/controller/InventoryController.java`

- `GET /inventory`
- `GET /inventory/pick-tickets`
- `GET /inventory/pick-tickets/scan`
- `GET /inventory/pick-tickets/{id}`
- `POST /inventory/pick-tickets/{id}/complete`
- `GET /inventory/locations`
- `POST /inventory/locations/assign`

## `reporting/controller/ReportingController.java`

- `GET /reporting`
- `GET /reporting/sales`
- `GET /reporting/sales.csv`

## `reporting/controller/SupplierPortalController.java`

- `GET /supplier-portal/login`
- `GET /supplier-portal/dashboard`
- `GET /supplier-portal/orders/{id}`
- `POST /supplier-portal/offers`
- `POST /supplier-portal/partnership`

## `sales/controller/SalesController.java`

- `GET /sales`
- `POST /sales/cart/add`
- `POST /sales/cart/update`
- `POST /sales/cart/remove`
- `POST /sales/checkout`
- `GET /sales/receipt/{id}`

## `stockmonitoring/controller/StockMonitoringController.java`

- `GET /stockmonitoring`
- `POST /stockmonitoring/suggestions/{id}`
- `GET /stockmonitoring/stock-requests`
- `POST /stockmonitoring/stock-requests`
- `POST /stockmonitoring/stock-requests/{id}/status`

## `supplier/controller/SupplierController.java`

- `GET /supplier`
- `GET /supplier/records`
- `GET /supplier/records/{id}/edit`
- `POST /supplier/records/{id}`
- `POST /supplier/records/{id}/active`
- `GET /supplier/partnerships`
- `POST /supplier/partnerships/{id}/decision`
- `GET /supplier/restock-requirements`
- `GET /supplier/compare`
- `GET /supplier/purchase-orders/new`
- `POST /supplier/purchase-orders`
- `GET /supplier/purchase-orders`
- `GET /supplier/purchase-orders/{id}`
- `POST /supplier/purchase-orders/{id}/ship`
- `GET /supplier/purchase-orders/{id}/receive`
- `POST /supplier/purchase-orders/{id}/receive`
- `GET /supplier/purchase-orders/{id}/export.csv`

## `warranty/controller/WarrantyController.java`

- `GET /warranty`
- `POST /warranty/claims`
- `POST /warranty/claims/{id}/resolve`

## `web/PageController.java`

- `GET /`
- `GET /login`
- `GET /dashboard`

## Security-only routes

- `POST /logout` — staff logout
- `POST /supplier-portal/login` — Supplier Portal login processing
- `POST /supplier-portal/logout` — Supplier Portal logout
