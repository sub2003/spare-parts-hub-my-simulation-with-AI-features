# Functional Motion Expansion

## Scope

Frontend-only enhancement. No Java, security, database, validation, supplier-comparison, urgency, inventory, sales, warranty, purchase-order or reporting business logic was changed.

## Shared motion architecture

The upgrade extends the existing `app-shell.css` / `app.js` system rather than adding a heavy animation dependency.

- module-aware accent colors for Inventory, Sales, Urgency, Warranty, Supplier and Reporting
- functional-page ambient glows kept in page margins/background layers
- page/header/surface entrance choreography
- short-table row entrance and large-table container reveal
- form focus/validation micro-interactions
- POST action loading feedback and duplicate-submit protection
- animated backend-derived urgency/progress widths
- lifecycle/status motion
- scan-line treatment on the QR scan card
- POS cart entrance/total feedback
- receiving quantity progress and serial-count feedback
- display-only RMA workflow visualization derived from the statuses already rendered by Thymeleaf
- empty-state orbit decoration
- modal/dropdown motion
- mobile horizontal summary/module scrollers
- complete reduced-motion and print fallbacks

## Files changed

- `src/main/resources/static/css/app-shell.css`
- `src/main/resources/static/css/supplier-management.css`
- `src/main/resources/static/js/app.js`
- `src/main/resources/static/js/supplier-management.js`
- `docs/functional-motion-expansion.md`

## Verification performed

- `node --check` passed for `app.js`
- `node --check` passed for `supplier-management.js`
- `tinycss2` reported 0 parse errors in all three CSS files
- 46 Thymeleaf templates inspected for nested forms: 0 found
- 46 Thymeleaf templates inspected for duplicate literal IDs: 0 found
- Supplier Portal templates confirmed to load `app.js`
- module coverage confirmed for Inventory, Sales, Urgency, Warranty, Supplier and Reporting templates

## Build limitation

`./mvnw clean compile -DskipTests` was attempted. The Maven wrapper could not download Apache Maven 3.9.16 from Maven Central in the execution environment, so a Java compile/runtime claim is intentionally not made here.
