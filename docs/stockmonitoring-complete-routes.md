# Function 3 — Routes

## Stock Monitoring

| Method | Route | Purpose | Main access |
|---|---|---|---|
| GET | `/stockmonitoring` | Urgency dashboard and restock suggestions | INVENTORY_SUPERVISOR, ADMIN |
| POST | `/stockmonitoring/recalculate` | Manual urgency recalculation | INVENTORY_SUPERVISOR, ADMIN |
| POST | `/stockmonitoring/suggestions/{id}` | Approve, modify or reject a pending suggestion | INVENTORY_SUPERVISOR, ADMIN |

## Customer Stock Requests

| Method | Route | Purpose | Main access |
|---|---|---|---|
| GET | `/stockmonitoring/stock-requests` | View request queue | SALES_EXEC, INVENTORY_SUPERVISOR, ADMIN |
| POST | `/stockmonitoring/stock-requests` | Log a customer stock request | SALES_EXEC, INVENTORY_SUPERVISOR, ADMIN |
| POST | `/stockmonitoring/stock-requests/{id}/status` | Move a request through its allowed lifecycle | SALES_EXEC, INVENTORY_SUPERVISOR, ADMIN |

Server-side transition rules prevent skipping states.

## Existing Supplier integration

| Method | Route | Purpose |
|---|---|---|
| GET | `/supplier/restock-requirements` | Existing Admin procurement queue for approved/modified suggestions |

Function 3 reuses the existing Supplier comparison and PO workflow rather than duplicating it.
