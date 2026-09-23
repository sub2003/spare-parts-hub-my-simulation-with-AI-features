# Final Integration Routes

This is the high-level route map for the final build. Individual module route documents remain under `docs/`.

## Shared
- `GET /` → dashboard redirect
- `GET /login`
- `GET /dashboard`
- `GET /profile`
- `POST /profile`
- `GET /profile/password`
- `POST /profile/password`
- `POST /logout`

## Inventory / Product / Picking
- `GET /inventory`
- `GET /inventory/products`
- `GET /inventory/products/new`
- `POST /inventory/products`
- `GET /inventory/products/{id}/edit`
- `POST /inventory/products/{id}`
- storage-location routes under `/inventory/locations/**`
- PickTicket/QR/scan routes under `/inventory/pick-tickets/**`

## Sales/POS
- `GET /sales`
- cart actions under `/sales/cart/**`
- `POST /sales/checkout`
- `GET /sales/receipt/{id}`

## Stock Monitoring
- `GET /stockmonitoring`
- `POST /stockmonitoring/recalculate`
- suggestion review under `/stockmonitoring/suggestions/**`
- customer requests under `/stockmonitoring/stock-requests/**`

## Warranty/RMA
- `GET /warranty`
- `GET /warranty/lookup`
- `GET /warranty/rmas/{id}`
- `POST /warranty/claims`
- approve/reject/replacement/refund/manufacturer actions under `/warranty/rmas/{id}/**`

## Supplier Management
Admin procurement and supplier-record routes are under `/supplier/**`, including supplier products, partnerships, restock requirements, comparison, Purchase Orders, receiving, and PO CSV export.

## Supplier Portal
Supplier-only routes remain under `/supplier-portal/**`: login, dashboard, profile/password, owned orders, PO CSV, shipment info, offers, and partnership catalog submission.

## Reporting / Audit
- `GET /reporting`
- `GET /reporting/sales`
- `GET /reporting/sales/export.csv`
- `GET /reporting/sales/export.pdf`
- `GET /reporting/audit`
- `GET /reporting/audit/{id}`
- `POST /reporting/audit/{id}/review`
- `GET /reporting/anomalies`
- Staff Account Management under `/reporting/staff/**`
