# AGENTS.md

Context for AI coding agents (Claude Code, Cursor, etc.) working in this repo.
Humans: see `CONTRIBUTING.md` instead — this file is agent-facing and assumes
the reader can't ask a teammate a clarifying question.

## Project

Web-based Computer Spare Parts Management System — SE2030 (SLIIT) team project.
Spring Boot 3.x (Java 17, Maven) + Spring Data JPA + Spring Security + MySQL +
Thymeleaf/Bootstrap. 6-person team, 6 core functions, one package per function.

## Build / run / verify

```
mvn clean compile          # must succeed with zero errors before any commit
mvn spring-boot:run         # must boot with no Hibernate schema-validation errors
```

There is no test suite yet. "Works" currently means: compiles clean, boots clean,
and the relevant page is reachable by logging in as the right role and clicking
the nav link. Do not claim something works without actually running it.

Database: MySQL, database name `spare_parts_db`, schema at `database/schema.sql`.
`spring.jpa.hibernate.ddl-auto=validate` in `application.properties` — Hibernate
checks entities against the real schema at boot but never auto-creates or
auto-alters tables. If schema.sql changes, the local database must be manually
recreated from it; there is no migration tool in use.

`application.properties` is gitignored (real credentials). Never commit it.
`application.properties.example` is the template — update it if new config
keys are introduced, but never put real credentials in it.

## Project structure

Package-by-feature, not package-by-layer. Root package: `com.sliit.sparepartshub`.

```
entity/        shared - all JPA entities (18 + User)
repository/    shared - only UserRepository lives here
security/      shared - SecurityConfig, CustomUserDetailsService, CustomUserPrincipal
web/           shared - PageController (/, /login, /dashboard)

inventory/     Function 1 - Inventory Storage Location Tracking
sales/         Function 2 - Product Search & Automated Checkout
stockmonitoring/  Function 3 - Dynamic Urgency Score Tracking
warranty/      Function 4 - Warranty and Returns Management
supplier/      Function 5 - Supplier Management
reporting/     Function 6 - Reporting, Audit Log & Supplier Portal
               (also owns the separate Supplier-portal auth - see below)
```

Each function package owns its own `controller` / `service` / `repository`
subpackages and its own `templates/<function>/` folder. Do not create a second
repository for an entity that already has one owned by another package unless
there's a documented reason (see "Shared tables" below) - check before adding.

Templates extend a shared layout via native Thymeleaf parameterized fragments
(not the thymeleaf-layout-dialect library):

```html
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: page(title='...', content=~{::main})}">
```

## Two separate authentication systems - do not merge them

Staff (5 internal roles, `users` table) and Supplier (external, `supplier`
table) are authenticated through **completely separate** Spring Security filter
chains. This is intentional, not an oversight - `Supplier` is not in `users`
and never will be.

| | Staff | Supplier |
|---|---|---|
| Entity | `User` | `Supplier` |
| `UserDetailsService` | `CustomUserDetailsService` | `SupplierUserDetailsService` (`reporting.security`) |
| Filter chain | `SecurityConfig`, `@Order(2)` | `SupplierSecurityConfig`, `@Order(1)`, `reporting.security` |
| Login route | `/login` | `/supplier-portal/login` |

**Known gotcha, already fixed once, do not reintroduce it:** each
`SecurityFilterChain`'s `DaoAuthenticationProvider` bean must be explicitly
wired via `.authenticationProvider(...)` inside its own `filterChain()` method.
Spring Boot's automatic single-`UserDetailsService` fallback only works when
there is exactly one `UserDetailsService` bean in the whole application
context. With two (`CustomUserDetailsService` and `SupplierUserDetailsService`),
that automatic wiring becomes ambiguous and silently breaks login with no
obvious error - it just rejects correct passwords. If you add a third auth
system, this applies again.

Multiple `SecurityFilterChain` beans require explicit `@Order` on every one of
them, most-specific-`securityMatcher` first.

**Second gotcha, same family: within a single filter chain, `requestMatchers`
rules are evaluated top-to-bottom and Spring Security stops at the FIRST
match - not the most specific one.** If you add a narrower exception inside a
broader URL prefix (e.g. `/stockmonitoring/stock-requests/**` needing a wider
role set than the rest of `/stockmonitoring/**`), the narrower rule MUST be
listed before the broader one, or it will silently never apply - the broader
rule matches first and the request is decided there. This already happened
once in `SecurityConfig` (see the comment above the
`/stockmonitoring/stock-requests/**` matcher).

**Third gotcha: a shared file becomes branch-only the moment it imports
something from an unmerged function package.** `PageController.java` and
`dashboard.html` are shared files in principle, but `PageController` now
depends on `UrgencyScoreService`, `RestockSuggestionService`, and
`StockRequestService` - all only present on `feature/urgency-tracking`, not
`main`. Pushing this version of `PageController.java` to `main` breaks the
build for everyone else, since those classes genuinely don't exist there.
Before suggesting ANY shared-file edit be pushed to `main`, check whether it
introduces a new import from a function-specific package - if so, it has to
wait on that branch until the function is merged. This already happened once
and had to be reverted.

## Shared tables

`Product` is read/written by multiple function packages (`sales`, and
`stockmonitoring` at minimum). Per `CONTRIBUTING.md`, whoever builds `sales`
owns the canonical `ProductRepository`; other modules that needed it before
`sales` existed created their own narrowly-scoped local copy (see
`stockmonitoring/repository/ProductRepository.java` for the pattern and the
comment explaining why). Don't silently duplicate a repository for a table
that already has an owner - check for one first, and if you must duplicate,
document why in a comment the way that file does.

## Schema/entity gaps closed against Use_Case_Scenarios.docx

The team's use case scenarios document required more than the original schema
supported. `schema.sql` and the entity set already reflect the fix (widened
status enums on `pick_ticket` and `purchase_order`, `picked_quantity` on
`pick_ticket_item`, `fault_description`/`condition_notes` on `rma_claim`,
`compatibility_override_reason` on `sale_item`, a new `restock_suggestion`
table, `serial_number.po_item_id` linking back to receiving). If a use case in
that doc seems to need data with nowhere to put it, check whether this was
already addressed before adding a new column/table - search `schema.sql` and
`CONTRIBUTING.md` first.

## Git conventions

Branch naming: `feature/<function-name>` (e.g. `feature/urgency-tracking`,
`feature/pos-checkout`). One branch per person, matching the function they own.

**`git pull origin <your-branch>` does NOT bring in changes from `main`** - it
only syncs your branch with its own remote copy. To get `main`'s changes into
a feature branch, use `git fetch origin` then `git merge origin/main`
explicitly. This has already caused a real bug (a shared login fix landed on
`main` and didn't reach a feature branch via `git pull` alone) - don't assume
`pull` is enough when the goal is "get main's changes."

Before suggesting a commit be pushed to `main`: confirm it's genuinely shared/
foundational work (entities, security, schema, shared templates, docs), not
one function's individual implementation. Function-specific code belongs on
that function's branch, merged to `main` only once it meets the criteria in
`CONTRIBUTING.md` (compiles clean, boots clean, reachable end-to-end via the
right role's login).

Commit messages: conventional-commit style (`feat(scope): ...`,
`fix(scope): ...`, `docs: ...`, `chore: ...`), short title + body explaining
*why*, not just what changed.

## Shared landing page widgets

`web/PageController.java` and `templates/dashboard.html` render one shared
page every logged-in staff member sees at `/dashboard`. Each function
contributes its own widget as a `sec:authorize`-gated block within the same
`<div class="row">`, following the pattern already in `dashboard.html`
(the Urgency/Warning/Restock Suggestions/Stock Requests cards, all gated to
`INVENTORY_SUPERVISOR`/`ADMIN`). Rules for adding a new widget here:

- Add your own gated `<div>` inside the existing row - don't edit or remove
  another function's block.
- If your widget needs data from your own service/repository, inject it into
  `PageController`'s constructor the same way the existing ones are - but see
  the shared-file-branch-only gotcha above: this file can only be pushed to
  `main` once your function's branch (and therefore your service classes)
  are merged there.
- Two people editing this file on different branches at the same time is a
  guaranteed merge conflict on the same lines. Flag it in the team channel
  before starting, and resolve conflicts by keeping both sides' widget blocks
  (they're additive, not competing) rather than picking one.

`stockmonitoring` (repository/service/controller/template split, DTOs for
query projections, scheduled recalculation, the approve/modify/reject
pattern in `RestockSuggestionService`) is a complete, working example of the
full stack for one function - use it as the reference implementation when
building out another function's module rather than starting from a blank
pattern each time.

- Plain getters/setters on entities, no Lombok (matches the existing `User`
  entity, which was hand-written first and set the convention).
- Enum constants on entities use lowercase, matching the underlying MySQL
  ENUM literal values exactly (e.g. `Role.warehouse_clerk`, not
  `Role.WAREHOUSE_CLERK`) - this lets `@Enumerated(EnumType.STRING)` read/write
  correctly with zero converter code. Not idiomatic Java naming; intentional.
- DTOs (plain classes, no JPA annotations) live in a `dto` subpackage within
  the function that owns them, used for query projections and view models -
  never persisted, never annotated with `@Entity`.
- Relationships are unidirectional `@ManyToOne` only, no `@OneToMany` back-
  references, to avoid lazy-loading collection complexity. Use repository
  query methods (`findBySale(...)`) instead of navigating from the "one" side.

## What not to do

- Don't add a test framework, build tool, or major dependency without it being
  an explicit ask - this is a student project on a fixed timeline and stack.
- Don't refactor working code across function-package boundaries without
  flagging it - each person owns their package.
- Don't assume `git pull` synced with `main` (see Git conventions above).
- Don't merge two `UserDetailsService`-backed auth flows into one filter chain
  (see Authentication above) - they're deliberately separate.
