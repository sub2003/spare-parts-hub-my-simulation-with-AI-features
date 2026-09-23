# Function 1 — Inventory & Picking Routes

All routes are under the existing staff security chain. `/inventory/**` remains available to `WAREHOUSE_CLERK`, `SALES_EXEC`, and `ADMIN` exactly as before.

| Method | Route | Purpose |
|---|---|---|
| GET | `/inventory` | Inventory dashboard |
| GET | `/inventory/products` | Full product inventory and reorder-level view |
| POST | `/inventory/products/{id}/reorder-level` | Update configurable reorder level |
| GET | `/inventory/pick-tickets` | Pick queue |
| GET | `/inventory/pick-tickets/scan` | Manual ticket-code lookup / QR scan page |
| POST | `/inventory/pick-tickets/scan` | Decode uploaded/captured QR image |
| GET | `/inventory/pick-tickets/{id}` | Pick-ticket detail |
| GET | `/inventory/pick-tickets/{id}/qr` | PNG QR generated from `ticketCode` |
| POST | `/inventory/pick-tickets/{id}/complete` | Confirm full / partial / exception pick result |
| GET | `/inventory/locations` | Storage location directory and product assignment |
| GET | `/inventory/locations/new` | Create-location form |
| POST | `/inventory/locations` | Create location |
| GET | `/inventory/locations/{id}/edit` | Edit-location form |
| POST | `/inventory/locations/{id}` | Save location changes |
| GET | `/inventory/locations/{id}/qr` | PNG QR generated from `locationCode` |
| POST | `/inventory/locations/assign` | Assign a product to a storage location |

## Stock ownership

Sales checkout is the source of truth for initial stock deduction in the current codebase. Inventory picking does **not** deduct the requested quantity a second time. If the warehouse confirms a shortfall, only the unpicked quantity is returned to stock so the net stock reduction equals the quantity physically fulfilled.
