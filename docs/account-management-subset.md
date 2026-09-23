# Account Management Subset — Implemented

This subset completes the staff account features from PBI-20 without changing the Supplier Portal authentication system.

## Added routes

| Route | Role | Purpose |
|---|---|---|
| `GET /profile` | Authenticated staff | View own profile |
| `POST /profile` | Authenticated staff | Update own name/email |
| `GET /profile/password` | Authenticated staff | Password form |
| `POST /profile/password` | Authenticated staff | Verify current password and change password |
| `GET /reporting/staff` | Admin | List staff accounts |
| `GET /reporting/staff/new` | Admin | Create-account form |
| `POST /reporting/staff` | Admin | Create account |
| `GET /reporting/staff/{id}/edit` | Admin | Edit-account form |
| `POST /reporting/staff/{id}` | Admin | Update account and role |
| `POST /reporting/staff/{id}/active` | Admin | Activate/deactivate account |
| `POST /reporting/staff/{id}/reset-password` | Admin | Issue a temporary password |

## Safeguards

- Staff passwords are always BCrypt encoded.
- Current password is verified for self-service password changes.
- Password hashes are never rendered in templates.
- Inactive users fail Spring Security `UserDetails.isEnabled()`.
- `ActiveStaffFilter` rechecks the database on authenticated staff requests, so a disabled account cannot keep using an old session.
- An Admin cannot deactivate their own signed-in account.
- The last active Admin cannot be deactivated.
- The last active Admin cannot be changed to a non-admin role.
- Duplicate staff codes and emails are rejected.
- Critical account/profile/password operations are written to `audit_log`.

## Schema change

`users.active BOOLEAN NOT NULL DEFAULT TRUE` was added to `database/schema-simulation.sql`.

`database/simulation-seed.sql` now seeds one intentionally inactive staff account for an access-control demonstration.

## Database note

Because Hibernate is configured with `ddl-auto=validate`, recreate/update the local `spareparts_mysimulation` schema using the updated simulation schema before starting this version.
