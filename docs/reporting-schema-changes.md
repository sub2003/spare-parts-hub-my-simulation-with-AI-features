# Function 6 — Reporting / Audit Schema Changes

## New table: `audit_review`
The original `audit_log` remains immutable. Administrative review state is persisted separately.

Columns:
- `review_id` — primary key
- `audit_log_id` — required, unique foreign key to `audit_log.log_id`
- `reviewed_by` — required foreign key to `users.user_id`
- `review_status` — `reviewed`, `dismissed`, or `action_required`
- `review_note` — up to 500 characters
- `reviewed_at` — timestamp

## Existing database
Run only: `database/patch-reporting-audit-review.sql`

after the previously applied Warranty/RMA migration.

## Fresh database
The current `schema-simulation.sql` already contains the final `audit_review` definition. For a completely fresh final simulation database, use the current final schema followed by `simulation-seed.sql`; historical ALTER patches are not required on a database created from that final schema.

Hibernate remains: `spring.jpa.hibernate.ddl-auto=validate`.
