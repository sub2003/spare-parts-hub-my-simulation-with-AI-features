# Manage Supplier Products — Changed Files

This subset adds the missing Admin workflow for linking existing inventory products to a supplier and maintaining supplier-specific commercial terms.

## Java

- `src/main/java/com/sliit/sparepartshub/supplier/controller/SupplierController.java`
  - GET `/supplier/records/{id}/products`
  - POST `/supplier/records/{id}/products`
- `src/main/java/com/sliit/sparepartshub/supplier/dto/SupplierProductAssignmentForm.java`
  - form wrapper for product rows
- `src/main/java/com/sliit/sparepartshub/supplier/dto/SupplierProductRowForm.java`
  - product selection + price/MOQ/lead-time row
- `src/main/java/com/sliit/sparepartshub/supplier/repository/ProductRepository.java`
  - ordered inventory product lookup
- `src/main/java/com/sliit/sparepartshub/supplier/repository/SupplierProductRepository.java`
  - supplier-specific terms lookup
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierManagementService.java`
  - load/manage supplier product assignments
  - server-side validation
  - transactional create/update/remove
  - audit logging
- `src/main/java/com/sliit/sparepartshub/supplier/service/SupplierProductValidationException.java`
  - row-specific validation errors

## Thymeleaf / CSS

- `src/main/resources/templates/supplier/suppliers.html`
  - added `Manage Products` action
- `src/main/resources/templates/supplier/supplier-products.html`
  - new Admin management screen
- `src/main/resources/templates/supplier/partnership-requests.html`
  - approved requests linked to `Manage Products` when a supplier account exists
- `src/main/resources/static/css/supplier-management.css`
  - matching premium responsive styles

## Database

No database schema patch is required. The existing `product` and `supplier_product` tables already support this workflow.
