# KPI Orbit Redundancy Cleanup

## Goal

Keep each animated KPI Orbit as the primary summary visual and remove repeated KPI cards/chips around it. The cleanup follows the rule: one metric should normally have one primary visual home.

## Page audit

| Page | Orbit metrics kept | Redundant content removed | Unique supporting content kept |
| --- | --- | --- | --- |
| Dashboard | Pending Picks, Open POs, Pending RMA, Stock Requests | Operational marquee and four repeated summary cards | Workspace navigation cards |
| Inventory | Total Products, Low Stock, Pending Picks, Locations | Matching four summary cards | Out of Stock, Serial Tracked, Partial Picks, Processed Today |
| Urgency | Critical, Warning, Pending Restock, Products Scored | Critical/Warning/Pending Suggestion cards | Last Recalculation moved into the Product Urgency header |
| Stock Requests | Waiting for Stock, Ready to Notify, Visible Requests, Requestable Products | Waiting for Stock / Ready to Notify cards | New-request form and request queue |
| Warranty / RMA | Pending Review, Approved Open, Opened Today, Visible Claims | Entire repeated four-card KPI row | Serial lookup and RMA claims workflow/content |
| Supplier Management | Active Suppliers, Partnership Review, Ready to Order, Open POs | Entire repeated four-card KPI row | Procurement Workflow |
| Sales / POS | Visible Products, Cart Units, Cart Lines, Cart Total | No duplicate KPI row existed | Product search, product list, cart and checkout |
| Reports | All Sales, Units Sold, Critical Products, Open POs | Four matching report cards | Total Revenue and Open RMAs |
| Supplier Portal | Open POs, Products Supplied, Active Offers, Partnership | Entire repeated portal-summary row | Assigned POs, sales velocity, offer history and partnership actions |

## Header chips

Pages containing a primary `[data-kpi-orbit]` now show only contextual floating header badges (for example `Live workspace` and the module name). The automatically generated numeric third badge is suppressed so it does not repeat an orbit KPI.

## Dashboard

The Dashboard orbit previously had three KPI badges while Stock Requests existed only in the repeated marquee/card area. Stock Requests was moved into the orbit as the fourth floating KPI. The duplicate operational marquee and repeated KPI-card row were then removed.

## Functional safety

No Java controller, service, repository, entity, security configuration, database schema, or business rule was changed. Existing Thymeleaf values were reused; no fake metrics were introduced.
