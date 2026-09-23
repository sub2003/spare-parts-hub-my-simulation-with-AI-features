# UI/UX + Animation Upgrade Verification

## Scope

This pass upgrades the existing Thymeleaf/Bootstrap frontend without changing business logic, routes, database schema, security rules, or backend calculations.

## Design strategy

The existing `app-shell.css` design system remains the source of truth. The upgrade adds a shared motion/interaction layer and targeted Supplier Management / Supplier Portal refinements instead of redesigning templates independently.

### Global improvements

- Shared motion tokens for fast, base, slow, and ambient animation timings.
- Slow ambient gradient/orb movement only on staff login, supplier login, and the dashboard hero.
- Short page entrance animation for server-rendered pages.
- Staggered metric-card and dashboard workspace-card entrance.
- Automatic numeric counter animation for plain numeric `.metric-value` elements.
- Lightweight IntersectionObserver reveal for major content surfaces.
- Consistent card hover lift, icon micro-motion, focus rings, and button pressed states.
- Subtle critical/pending/system-status pulse.
- Table row hover/focus improvements and tabular numeric alignment.
- Improved empty-state and alert presentation.
- Reusable Bootstrap confirmation modal for `form[data-confirm]`.
- Reduced-motion support disables decorative motion and counter tweening.
- Print mode disables animation/decorative motion.

### Supplier Management

- Supplier sub-navigation gets smoother active/hover behavior.
- Search boxes now have a clear button and result count through `supplier-management.js`.
- Existing supplier confirmation forms now use the shared confirmation modal rather than a second confirmation implementation.
- Recommended supplier rows get a restrained entrance/accent treatment without changing comparison logic.
- Purchase-order lifecycle current step gets a gentle animated progress treatment.
- Supplier product toolbar remains easy to access on larger screens.

### Supplier Portal

- All Supplier Portal pages now load Bootstrap JS and the shared `app.js`, enabling the same counters, accessibility helpers, reveals, and confirmation experience as staff pages.
- Inline `confirm(...)` handlers on offer/catalogue/request actions were replaced with `data-confirm` so they use the shared modal.
- Supplier login gets the same low-cost ambient visual language as staff login.
- Portal nav/cards/history interactions were refined while retaining the separate supplier authentication/UI identity.

## Intentionally restrained pages

- Sales/POS: no continuous animation on cart/search/data entry areas.
- Reporting/Audit: no pulsing table rows or decorative motion over evidence/history.
- Forms: inputs do not move; only borders/focus rings transition.
- Large tables: the container may reveal, but rows are not individually animated.

## Accessibility and performance

- `prefers-reduced-motion: reduce` disables continuous/decorative motion and transitions.
- Status information still uses text and color; animation is never the only signal.
- Confirmation UI uses Bootstrap modal focus handling where Bootstrap is available and falls back to `window.confirm` if it is not.
- Ambient movement uses CSS transforms/background-position rather than JavaScript animation loops.
- No GSAP, Three.js, Lottie, particles.js, or other frontend dependencies were added.

## Static verification performed

- `node --check src/main/resources/static/js/app.js` — PASS
- `node --check src/main/resources/static/js/supplier-management.js` — PASS
- CSS parse using `tinycss2` — 0 parse errors
- All 46 Thymeleaf templates scanned for nested forms — 0 nested-form issues
- All 46 Thymeleaf templates scanned for duplicate static IDs — 0 duplicates
- Remaining inline `window.confirm`/`onsubmit confirm(...)` in templates — 0

## Maven/build verification

`./mvnw -DskipTests clean compile` was attempted. The wrapper could not download Maven 3.9.16 from Maven Central in this execution environment, and no system `mvn` command is installed. Therefore a full Spring compile/startup/browser runtime verification could not be completed here. No backend Java source was changed by this UI/UX pass.

Run locally:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Then verify staff login, dashboard, all module routes, supplier management actions, and supplier portal routes in a browser.
