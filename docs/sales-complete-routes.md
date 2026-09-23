# Function 2 — Sales / POS Routes

| Method | Route | Purpose |
|---|---|---|
| GET | `/sales` | POS product search, filters and current session cart |
| POST | `/sales/cart/add` | Add product/quantity to cart |
| POST | `/sales/cart/update` | Update quantity, fixed per-unit discount amount and discount reason |
| POST | `/sales/cart/serials` | Save explicit serial-number selection for a serial-tracked cart line |
| POST | `/sales/cart/remove` | Remove one cart line |
| POST | `/sales/cart/clear` | Clear the whole session cart |
| POST | `/sales/checkout` | Payment-confirmed transactional checkout |
| GET | `/sales/receipt/{id}` | Printable receipt with serials and pick-ticket QR |

All `/sales/**` routes remain protected by the existing staff security chain for `SALES_EXEC` and `ADMIN`.
