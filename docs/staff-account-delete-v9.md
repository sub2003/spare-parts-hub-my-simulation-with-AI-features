# Staff Account Delete v9

## Rule
An Admin can permanently delete a staff account only when it is not the signed-in account, it is not the last active Admin, and it has no historical/business records. Otherwise the account must be deactivated.

## Route
`POST /reporting/staff/{id}/delete`

## Historical dependencies checked
- `sale.sold_by`
- `pick_ticket.fulfilled_by`
- `stock_request.logged_by`
- `purchase_order.created_by`
- `partnership_request.reviewed_by`
- `restock_suggestion.reviewed_by`
- `rma_claim.processed_by`
- `rma_claim.reviewed_by`
- `rma_claim.resolved_by`
- `audit_log.user_id`
- `audit_review.reviewed_by`

No business/history row is deleted or nullified to make account deletion possible.

## Audit
Successful deletion writes `STAFF_ACCOUNT_DELETED` using the acting Admin as the audit user and the deleted account ID as `record_id`. The target account snapshot is captured before deletion; passwords and hashes are not included.

## Schema
No schema change is required.
