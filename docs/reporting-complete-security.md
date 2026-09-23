# Function 6 — Reporting / Audit Security

## Reporting access
`SecurityConfig` protects `/reporting/**` with `hasRole("ADMIN")`.

This means:
- ADMIN: Reporting/Audit and Staff Account Management.
- SALES_EXEC: denied.
- WAREHOUSE_CLERK: denied.
- INVENTORY_SUPERVISOR: denied.
- OPERATIONS_COORDINATOR: denied.
- SUPPLIER: handled by a different filter chain and denied from staff Reporting routes.

## Separate authentication chains
- Supplier Portal: `SupplierSecurityConfig`, `@Order(1)`, scoped to `/supplier-portal/**`.
- Staff: `SecurityConfig`, `@Order(2)`.

Supplier is not converted into a staff `User`.

## Sensitive audit protection
Audit detail redacts:
- password/password_hash values,
- temporary password fields,
- token/secret/credential-style keys,
- bcrypt hash-looking values,
- database-password-style keys.

## Audit review authorization
The controller is ADMIN-only through `/reporting/**`. The service also reloads the actor and independently requires an active `User.Role.admin` before saving a review or report-export audit.

## CSRF
Reporting write actions use regular Spring MVC POST forms, so they remain under the existing Spring Security CSRF protection.
