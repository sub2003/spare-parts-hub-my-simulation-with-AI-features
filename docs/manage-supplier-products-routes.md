# Manage Supplier Products — Routes

All `/supplier/**` routes remain protected by the existing staff security chain and require ADMIN.

| Method | Route | Purpose |
|---|---|---|
| GET | `/supplier/records` | Supplier directory |
| GET | `/supplier/records/{supplierId}/products` | Open Admin Manage Products page |
| POST | `/supplier/records/{supplierId}/products` | Save product links and commercial terms |
| GET | `/supplier/partnerships` | Review partnership requests; approved linked requests expose Manage Products |
| GET | `/supplier-portal/dashboard` | Supplier sees linked products after Admin saves them |

Supplier Portal behavior does not need a new route. It already reads `supplier_product` rows for the authenticated supplier.
