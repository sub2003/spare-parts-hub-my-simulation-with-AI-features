# Add Supplier Subset — Routes

All `/supplier/**` routes remain protected by the existing staff security chain and require `ROLE_ADMIN`.

| Method | Route | Purpose | Access |
|---|---|---|---|
| GET | `/supplier/records` | List supplier accounts | Admin |
| GET | `/supplier/records/new` | Open Create Supplier form | Admin |
| POST | `/supplier/records` | Validate and create supplier account | Admin |
| GET | `/supplier/records/{id}/edit` | Existing supplier edit form | Admin |
| POST | `/supplier/records/{id}` | Existing supplier update | Admin |
| POST | `/supplier/records/{id}/active` | Activate/deactivate supplier | Admin |
| GET | `/supplier-portal/login` | Separate Supplier Portal login | Public login page |
| GET | `/supplier-portal/dashboard` | Supplier-specific portal | Active Supplier |

## Create flow

1. Admin opens `/supplier/records`.
2. Admin selects **Add Supplier**.
3. Browser opens `/supplier/records/new`.
4. Admin enters supplier code, name, email, contact, temporary password, confirmation and account status.
5. POST `/supplier/records` validates input, duplicate code/email and password confirmation.
6. Password is BCrypt encoded before persistence.
7. Supplier and `SUPPLIER_CREATED` audit event are stored in one transaction.
8. Admin is redirected to `/supplier/records`.
9. If the supplier is active, the new email/password can be used at `/supplier-portal/login`.
