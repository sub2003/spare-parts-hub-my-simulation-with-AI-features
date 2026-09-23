# Supplier Portal subset routes

## Public supplier auth
- `GET /supplier-portal/login`
- `POST /supplier-portal/login` (Spring Security)

## Authenticated supplier
- `GET /supplier-portal/dashboard`
- `GET /supplier-portal/profile`
- `POST /supplier-portal/profile`
- `GET /supplier-portal/profile/password`
- `POST /supplier-portal/profile/password`
- `GET /supplier-portal/orders/{id}` — server-side supplier ownership required
- `GET /supplier-portal/orders/{id}/export.csv` — same ownership check
- `POST /supplier-portal/orders/{id}/shipment` — same ownership check
- `POST /supplier-portal/offers`
- `POST /supplier-portal/partnership` — multipart PDF/CSV/XLSX catalog
- `POST /supplier-portal/logout` (Spring Security)

## Admin cross-function route
- `GET /supplier/partnerships/{id}/catalog` — Admin-only through existing `/supplier/**` security rule
