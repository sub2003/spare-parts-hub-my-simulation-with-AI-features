# Function 6 — Reporting / Audit Routes

All routes below are inside the staff security chain. `/reporting/**` is ADMIN-only.

| Method | Route | Purpose |
|---|---|---|
| GET | `/reporting` | Reporting dashboard |
| GET | `/reporting/sales` | Filtered sales report |
| GET | `/reporting/sales/export.csv` | CSV sales export |
| GET | `/reporting/sales.csv` | Legacy CSV alias |
| GET | `/reporting/sales/export.pdf` | PDF sales export |
| GET | `/reporting/audit` | Audit list/filter page |
| GET | `/reporting/audit/{id}` | Audit detail and current review |
| POST | `/reporting/audit/{id}/review` | Save reviewed/dismissed/action_required state |
| GET | `/reporting/anomalies` | Deterministic anomaly evidence |
| GET | `/reporting/staff` | Existing Staff Account Management |
| GET | `/reporting/staff/new` | Existing Add Staff form |
| POST | `/reporting/staff` | Existing Add Staff action |
| GET | `/reporting/staff/{id}/edit` | Existing Edit Staff form |
| POST | `/reporting/staff/{id}` | Existing Edit Staff action |
| POST | `/reporting/staff/{id}/active` | Existing active/inactive action |
| POST | `/reporting/staff/{id}/reset-password` | Existing admin password reset |

Supplier Portal remains on the separate `/supplier-portal/**` security chain and is not part of `/reporting/**`.
