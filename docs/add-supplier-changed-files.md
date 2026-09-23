# Add Supplier Subset — Changed Files

## New files

- `src/main/java/com/sliit/sparepartshub/supplier/dto/CreateSupplierRequest.java`
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierFieldValidationException.java`
- `src/main/resources/templates/supplier/supplier-create.html`
- `docs/add-supplier-changed-files.md`
- `docs/add-supplier-routes.md`
- `docs/add-supplier-verification.md`

## Modified files

- `src/main/java/com/sliit/sparepartshub/reporting/repository/SupplierRepository.java`
  - Added case-insensitive email lookup and duplicate checks for supplier email/code.
- `src/main/java/com/sliit/sparepartshub/reporting/security/SupplierUserDetailsService.java`
  - Supplier login lookup is now case-insensitive while preserving the separate supplier authentication chain.
- `src/main/java/com/sliit/sparepartshub/supplier/controller/SupplierController.java`
  - Added Admin GET/POST create-supplier routes and field-level validation handling.
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierManagementService.java`
  - Added transactional supplier creation, validation, BCrypt password hashing, uniqueness checks, safe auditing, and stronger edit validation.
  - Existing UC-05 purchase-order, receiving, comparison, partnership and supplier-status behavior was preserved.
- `src/main/resources/templates/supplier/suppliers.html`
  - Added the primary **Add Supplier** action.

## Database files

No database schema change was required. The existing `supplier` table already contains:

- `supplier_id`
- `supplier_code`
- `name`
- `contact`
- `email`
- `password_hash`
- `active`

Therefore there is no SQL patch for this subset.
