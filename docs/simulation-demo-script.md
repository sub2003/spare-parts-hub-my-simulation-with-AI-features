# Wednesday Progress Evaluation Demo Script

1. Log in as `sales@sparepartshub.lk` / `Demo123!` and open **Sales / POS**.
2. Search for a product, add it to the cart, and complete checkout. Note the SALE and PICK codes.
3. Log out; log in as `warehouse@sparepartshub.lk`. Open **Inventory > Pick Tickets**, open the new ticket, and complete it.
4. Log in as `inventory@sparepartshub.lk`. Open **Urgency Dashboard**, recalculate/view scores, then approve or modify a restock suggestion. Optionally log a customer stock request.
5. Log in as `admin@sparepartshub.lk`. Open **Suppliers > Restock Requirements**, compare suppliers, create a PO, then mark it shipped.
6. Receive part or all of the PO. Show that inventory increases and partial receipts use `received_quantity` so stock is not double-counted.
7. Log in as `operations@sparepartshub.lk`. Open **Warranty / RMA**, search `SSD-DEMO-001`, and show its store sale/warranty/RMA history.
8. Log in as Admin and open **Reports** to show sales totals, audit events and simple deterministic anomaly flags.
9. Open `/supplier-portal/login`, sign in as `supplier1@demo.lk`, show assigned POs and submit a restock offer.

All modules use the same `spareparts_mysimulation` database.
