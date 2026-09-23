# Warranty / RMA Routes

All routes are under the staff Security filter chain and require `OPERATIONS_COORDINATOR` or `ADMIN` through the existing `/warranty/**` matcher.

| Method | Route | Purpose |
|---|---|---|
| GET | `/warranty` | Warranty dashboard, optional serial lookup and RMA status filter |
| GET | `/warranty/lookup?serial=...` | Lookup convenience route; redirects to dashboard lookup |
| GET | `/warranty/rmas` | RMA list alias, optional status filter |
| GET | `/warranty/rmas/{id}` | RMA details, history and allowed actions |
| POST | `/warranty/claims` | Create a new pending RMA after server-side eligibility checks |
| POST | `/warranty/rmas/{id}/approve` | Approve a pending claim |
| POST | `/warranty/rmas/{id}/reject` | Reject a pending claim with required reason |
| POST | `/warranty/rmas/{id}/replace` | Finalize explicit serial replacement transaction |
| POST | `/warranty/rmas/{id}/refund` | Finalize refund outcome |
| POST | `/warranty/rmas/{id}/manufacturer` | Finalize send-to-manufacturer outcome |
