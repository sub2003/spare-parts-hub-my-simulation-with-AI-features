# Contributing to spare-parts-hub

This doc exists so the "how we work" decisions we made don't only live in a chat log.
Read this before you branch off for your module.

## Stack

Spring Boot (Maven, Java 17) · Spring Data JPA · Spring Security · MySQL · Thymeleaf/Bootstrap

## Project structure

We use **package-by-feature**, not package-by-layer. Each of the 6 core functions gets its
own package, and each package owns its own `controller` / `service` / `repository`
subfolders and its own Thymeleaf templates folder. This means two people almost never edit
the same file, which is what actually prevents merge conflicts — branch naming alone
doesn't.

```
src/main/java/com/sliit/sparepartshub/
├── entity/            (shared — all 18 JPA entities + User, already committed)
├── repository/         (shared — UserRepository lives here; function-specific
│                        repositories live inside each function's own package)
├── security/           (shared — SecurityConfig, CustomUserDetailsService, etc.)
├── web/                 (shared — PageController: /, /login, /dashboard)
├── inventory/           (Function 1 — Inventory Storage Location Tracking)
├── sales/                (Function 2 — Product Search & Automated Checkout)
├── stockmonitoring/      (Function 3 — Dynamic Urgency Score Tracking)
├── warranty/             (Function 4 — Warranty and Returns Management)
├── supplier/             (Function 5 — Supplier Management)
└── reporting/            (Function 6 — Reporting, Audit Log & Supplier Portal)

src/main/resources/templates/
├── fragments/layout.html   (shared base layout — everyone's pages extend this)
├── login.html, dashboard.html   (shared)
└── inventory/, sales/, stockmonitoring/, warranty/, supplier/, reporting/
    (one folder per function, matching the packages above)
```

## Module ownership

| Function | Package | Actor(s) | Backlog items | Sprint |
|---|---|---|---|---|
| 1. Inventory Storage Location Tracking | `inventory` | Warehouse Clerk, Senior Sales Executive | PBI-01 to PBI-04 | Sprint 2 |
| 2. Product Search & Automated Checkout | `sales` | Senior Sales Executive | PBI-05 to PBI-08 | Sprint 1 |
| 3. Dynamic Urgency Score Tracking | `stockmonitoring` | Inventory Supervisor (+ Senior Sales Executive for stock request logging only) | PBI-09 to PBI-12 | Sprint 2/3 |
| 4. Warranty and Returns Management | `warranty` | Operations Coordinator | PBI-13 to PBI-16 | Sprint 3 |
| 5. Supplier Management | `supplier` | Shop Owner / Admin | PBI-17, PBI-18 | Sprint 3 |
| 6. Reporting, Audit Log & Supplier Portal | `reporting` | Admin (internal) + Supplier (external) | PBI-19, PBI-21 to PBI-24 | Sprint 4 |

`stockmonitoring` also owns `RestockSuggestion` (approve/modify/reject decisions on restock recommendations) — this was missing from the original schema and got added after cross-checking against `Use_Case_Scenarios.docx` (UC-03 postcondition 3).

`stockmonitoring` is functionally complete against its backlog (PBI-09 to PBI-12): repositories, scheduled urgency recalculation, the dashboard with critical/warning alerts, the full customer stock request lifecycle (log → notify → fulfill), and the restock suggestion generate → approve/modify/reject flow. If you're starting your own module and want a working example of the full repository → service → controller → template split for one function, this is the one to look at.

_Fill in each teammate's name against their function below once assigned:_

- Inventory Storage Location Tracking — _name_
- Product Search & Automated Checkout — _name_
- Dynamic Urgency Score Tracking — _name_
- Warranty and Returns Management — _name_
- Supplier Management — _name_
- Reporting, Audit Log & Supplier Portal — _name_

**Shared tables, one owner:** `Product` is used by both Function 2 (search/checkout) and
Function 3 (urgency scoring). Whoever builds Function 2 first owns `ProductRepository` —
everyone else autowires it rather than creating a second one. If you need a new query
method on a repository you don't own, ask the owner to add it or open a small PR against
just that file.

## Branching

- One branch per member, named after their function:
  `feature/inventory-tracking`, `feature/pos-checkout`, `feature/urgency-tracking`,
  `feature/warranty-returns`, `feature/supplier-management`, `feature/reporting-portal`
- Branch off `main`, not off someone else's feature branch.
- Only touch your own package (`com.sliit.sparepartshub.<your-package>`) and your own
  templates folder. If a change outside your package is genuinely needed (e.g. adding a
  navbar link, extending a shared repository), say so in the PR description and tag the
  owner of that file.

## Local setup

1. Copy `application.properties.example` to `application.properties` (gitignored — never
   commit the real one) and fill in your local MySQL credentials.
2. Run the schema at `/database/schema.sql` against a local `spare_parts_db`.
3. Seed at least one test user per role so you can actually log in — see
   `docs/test-credentials.md` (or ask in the team chat) for ready-made BCrypt hashes.
4. `mvn clean compile` should succeed with zero errors before you write any new code.
5. `mvn spring-boot:run` should boot with no Hibernate schema-validation errors. This is
   the same bar we already hit with the `User` entity and the full 18-entity set — if it
   doesn't boot clean, fix that before adding your module.

## Building your module

1. Add any repositories your function needs under your own package
   (`com.sliit.sparepartshub.<yourpackage>.repository`), following the pattern in
   `UserRepository`.
2. Add a service layer if your logic is non-trivial (e.g. urgency score calculation,
   compatibility checking) — don't put business logic directly in the controller.
3. Replace the placeholder controller in your package (e.g. `InventoryController`) with
   real endpoints. The route prefix (`/inventory`, `/sales`, etc.) is already wired into
   `SecurityConfig` with the correct role restrictions — you shouldn't need to touch that
   file.
4. Replace the placeholder `index.html` in your templates folder. Extend the shared layout
   the same way it already does:
   ```html
   <html xmlns:th="http://www.thymeleaf.org"
         th:replace="~{fragments/layout :: page(title='Your Page Title', content=~{::main})}">
   <body>
   <main>
     <!-- your content -->
   </main>
   </body>
   </html>
   ```

## Merge criteria

Before opening a PR into `main`, your module must:

- [ ] Compile clean (`mvn clean compile`, zero errors)
- [ ] Boot clean (`mvn spring-boot:run`, no Hibernate validation errors)
- [ ] Be reachable end-to-end: log in as your role's test account, click your navbar link,
  confirm the page renders
- [ ] Not modify files outside your own package/templates folder, unless flagged and agreed
  with the owner of that file

Keeping `main` always in a demoable state matters — we have weekly checkpoints and a
Week 13 live demo, so nobody should be stuck untangling a broken `main` right before either.

## Two things that already caused real bugs — read before touching shared security/pages

**Spring Security checks `requestMatchers` top-to-bottom and stops at the
first match — not the most specific one.** If you need a narrower rule inside
a broader URL prefix (e.g. one specific page under `/yourmodule/**` needing a
different role set than the rest of that module), the narrower rule must be
listed *above* the broader one in `SecurityConfig`, or it will silently never
apply. This happened for real when Sales Executive needed access to
`/stockmonitoring/stock-requests/**` while the rest of `/stockmonitoring/**`
stayed Supervisor/Admin-only — look at that block in `SecurityConfig` for the
pattern to copy.

**A shared file becomes branch-only the moment it depends on your module's
code.** `PageController.java` and `dashboard.html` are shared, but the
version that includes Function 3's landing-page widgets imports
`UrgencyScoreService` and friends — classes that only exist on
`feature/urgency-tracking` until that branch merges to `main`. Pushing that
version of a shared file to `main` early breaks the build for everyone else.
Rule of thumb: before pushing a shared-file change to `main`, check whether
you just added an import from your own function package — if so, it stays on
your branch until your module is merged, same as any other function-specific
code.

## Shared landing page widgets (`/dashboard`)

Every logged-in staff member sees the same `/dashboard` page. Each function
contributes its own widget as a `sec:authorize`-gated block inside the shared
`<div class="row">` in `dashboard.html` — see the Urgency/Warning/Restock
Suggestions/Stock Requests cards for the pattern. When adding yours:

- Add a new gated block, don't edit an existing one.
- Inject your own service into `PageController`'s constructor, same as the
  existing ones — but see the shared-file-branch-only rule above.
- Flag it in the team chat before you start editing this file. Two people
  editing it at the same time on different branches is a guaranteed merge
  conflict on the same lines — usually resolved by keeping both sides' widget
  blocks, since they're additive, not competing.

## Supplier authentication

`Supplier` is a primary actor in UC-06 (Centralized Reporting, Audit Logging & Supplier
Portal) with its own login — but it's not in the `users` table and never will be (the
`role` ENUM only covers the 5 internal staff roles). Staff and suppliers are authenticated
through **two completely separate systems**:

| | Staff | Supplier |
|---|---|---|
| Entity | `User` | `Supplier` |
| Repository | `UserRepository` | `SupplierRepository` (in `reporting`) |
| `UserDetailsService` | `CustomUserDetailsService` | `SupplierUserDetailsService` (in `reporting.security`) |
| `UserDetails` wrapper | `CustomUserPrincipal` | `CustomSupplierPrincipal` (in `reporting.security`) |
| Security config | `SecurityConfig` (`@Order(2)`) | `SupplierSecurityConfig` (`@Order(1)`, in `reporting.security`) |
| Login route | `/login` | `/supplier-portal/login` |
| Layout | `fragments/layout.html` | its own standalone layout — do not try to reuse the staff navbar/layout for supplier pages |

A supplier session cannot reach `/inventory`, `/sales`, etc., and a staff session cannot
reach `/supplier-portal/**` — they're enforced by entirely separate `SecurityFilterChain`
beans, not by role checks within one chain.

**Shared-file dependency:** adding the second filter chain required one change to the
shared `SecurityConfig.java` — an `@Order(2)` annotation on its `filterChain` bean, since
Spring Security requires an explicit order once more than one `SecurityFilterChain` bean
exists. This is the one exception to "don't touch shared files without flagging it" — it's
already done, just be aware of it if you're debugging an auth issue and wondering why
`SecurityConfig` looks different from before.

Whoever builds out the rest of `reporting` owns the actual supplier-portal pages (sales
velocity reports, restock offer submission, catalog upload, assigned PO status —
UC-06 steps B2 through B5). The login/logout/dashboard shell is already there to build
behind.

## Schema and entities were cross-checked against Use_Case_Scenarios.docx

The original `schema.sql` and entity set were checked against the team's use case
scenarios document and had several gaps closed as a result — this was a full rewrite of
`schema.sql`, not a migration, since it happened before anyone had pulled the original.
If your local `schema.sql` predates this, drop and recreate `spare_parts_db` from the
current file rather than trying to patch it by hand. Changes made:

- `supplier` — added `password_hash` (see Supplier authentication above)
- `pick_ticket` — status widened to `pending / completed / partially_completed / exception`
  (was just `pending / fulfilled`); added `ticket_code`
- `pick_ticket_item` — added `picked_quantity` (actual quantity picked, separate from
  requested, for partial picks)
- `purchase_order` — status widened to include `partially_received`
- `serial_number` — added `po_item_id`, linking a received serial number back to the
  purchase order it arrived on
- `rma_claim` — added `fault_description` and `condition_notes`
- `sale_item` — added `compatibility_override_reason`
- **New table** `restock_suggestion` — stores the Inventory Supervisor's approve / modify /
  reject decision on a restock recommendation, with an optional reason (owned by
  `stockmonitoring`, see table above)

If you generated entity classes or wrote queries against the old schema before this
update, regenerate against the current `schema.sql` — column names for the tables above
changed shape, not just gained columns.
