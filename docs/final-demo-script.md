# Final End-to-End Demo Script

Use separate browser sessions/incognito windows where helpful so staff and Supplier sessions do not collide.

1. Login as Admin.
2. Open Inventory → Products and confirm Add/Edit Product works.
3. Open Suppliers → Manage Products and confirm a Product is linked to a Supplier with price/MOQ/lead time.
4. Login to the corresponding Supplier Portal and submit/inspect an offer if required.
5. Login as Inventory Supervisor and open Stock Monitoring.
6. Confirm a low/critical Product has a real urgency score and restock suggestion.
7. Approve or modify a pending suggestion.
8. Login as Admin and open Supplier Restock Requirements / Compare Suppliers.
9. Choose a linked Supplier and create a Purchase Order.
10. Login as that Supplier and confirm only its own PO is visible.
11. Submit shipment/tracking information.
12. Login as Admin and mark the PO shipped if still pending.
13. Receive part/all of the PO. For serial-tracked Product, enter exactly one real serial per accepted unit.
14. Confirm Product stock increased only by the newly accepted quantity.
15. Confirm new accepted serials are `in_stock`.
16. Confirm matching pending customer stock requests move to `ready_to_notify`, not `notified`.
17. Confirm urgency recalculates.
18. Mark the customer request Notified manually.
19. Login as Sales Executive and open POS.
20. Search the Product and add it to cart.
21. For serial-tracked Product, explicitly select a physical `in_stock` serial.
22. Trigger/inspect compatibility rules when applicable; enter an override reason only when needed.
23. Check Payment Received and confirm the Sale.
24. Confirm Sale/SaleItems/PickTicket exist and stock fell exactly once.
25. Confirm the selected serial is `sold` and linked to the Sale.
26. Login as Warehouse Clerk and open the PickTicket by QR or manual code.
27. Complete the physical pick and confirm stock is not deducted a second time.
28. Login as Operations Coordinator and enter the sold serial in Warranty/RMA.
29. Confirm Sale date, warranty months, expiry and eligibility are shown.
30. Create an RMA and approve it.
31. For replacement, explicitly choose a valid same-product `in_stock` replacement serial; alternatively demonstrate Refund or Manufacturer Return.
32. Confirm replacement stock decreases once and the replacement serial becomes non-sellable.
33. Confirm urgency recalculates.
34. Login as Admin and open `/reporting`.
35. Open Sales Report and filter the date range that includes the Sale.
36. Confirm Sale count, revenue, quantity and top products use real data.
37. Download CSV and PDF.
38. Open Audit Log and confirm `REPORT_EXPORTED` exists.
39. Open an Audit detail and verify sensitive values are redacted.
40. Save an AuditReview as Reviewed, Dismissed, or Action Required; Action Required must have a note.
41. Restart the application and confirm the review still exists.
42. Open Anomalies and verify triggered rows show rule, actor, time window and evidence; otherwise verify the clean no-anomaly state.
43. Verify Supplier A cannot open Supplier B PO URLs.
44. Run `database/final-integrity-checks.sql` and investigate any returned violation rows.
45. Confirm no negative stock and Hibernate startup succeeds under `ddl-auto=validate`.
