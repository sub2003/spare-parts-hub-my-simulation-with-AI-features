# Add Supplier Subset — Verification Notes

## Implemented

- Admin-only Add Supplier page and POST action.
- Safe DTO instead of binding the `Supplier` entity directly to the create form.
- Required supplier code, name, email, contact and temporary password.
- Supplier code normalization to uppercase.
- Case-insensitive duplicate supplier-code validation.
- Case-insensitive duplicate email validation.
- Email normalization to lowercase before save.
- Contact format and maximum-length validation.
- Minimum 8-character temporary password.
- Password confirmation validation.
- Existing shared `PasswordEncoder` reused; no second encoder bean was added.
- BCrypt hash stored in `password_hash`; password/hash are not included in audit data.
- Active/inactive can be chosen at creation; Active is the default.
- Existing `CustomSupplierPrincipal.isEnabled()` and Supplier Portal active-account protection remain intact.
- New active suppliers can authenticate through the existing separate Supplier Portal security chain.
- `SUPPLIER_CREATED` audit event records only safe supplier fields.
- Supplier save and audit save are in the same `@Transactional` service method.
- Existing Supplier edit now also checks case-insensitive duplicate email/code.
- Existing Supplier Management UC-05 routes and logic remain present.

## Database

No schema patch is required. The current simulation schema already contains every field needed by this feature.

## Static verification performed in this environment

- 36 Thymeleaf/HTML templates parsed without HTML parser errors.
- 24 local `/css`, `/js`, `/images` template references checked; 0 missing assets.
- Duplicate `*Repository.java` simple-name scan: 0 duplicates.
- Java bracket/parenthesis balance scan: 0 issues.
- `javac` syntax-oriented scan found 0 parse-error patterns. Full compilation cannot use that scan because Spring/JPA dependencies are not on the raw `javac` classpath.

## Maven result

Attempted:

```bash
./mvnw -DskipTests compile
```

The environment could not download Maven 3.9.16 from Maven Central, so a real Maven compile and Spring Boot startup could not be completed here. Do not interpret this as an application compile failure.

Run on the user's Windows PC:

```powershell
.\mvnw.cmd clean compile
```

Then start `SparePartsHubApplication` and confirm the console reaches both:

- `Tomcat started on port 8080`
- `Started SparePartsHubApplication`

## Manual verification checklist on the running app

- [ ] Login as Admin.
- [ ] Open `http://localhost:8080/supplier/records`.
- [ ] Confirm **Add Supplier** is visible.
- [ ] Open `http://localhost:8080/supplier/records/new`.
- [ ] Create an active supplier using a new code/email and an 8+ character temporary password.
- [ ] Confirm the supplier appears in Supplier Records.
- [ ] Confirm the DB `password_hash` is a BCrypt hash, not the temporary password.
- [ ] Logout Admin and login through `/supplier-portal/login` using the new supplier email/password.
- [ ] Confirm the active supplier reaches its own dashboard.
- [ ] Create a second supplier as Inactive and confirm Supplier Portal login is denied.
- [ ] Try a duplicate supplier code and confirm creation is rejected.
- [ ] Try a duplicate email and confirm creation is rejected.
- [ ] Try mismatched passwords and confirm creation is rejected.
- [ ] Confirm an audit row with action `SUPPLIER_CREATED` exists and contains no password/hash.
- [ ] Confirm existing supplier edit and activate/deactivate still work.

## Demo Admin

The existing simulation seed uses:

- Email: `admin@sparepartshub.lk`
- Password: `Demo123!`

These are local demo credentials only.
