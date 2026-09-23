# KPI Orbit Family v5

## Goal

Adds a reusable family of animated light-theme KPI orbit summaries to major Spare Parts Hub functional pages while preserving all existing backend/business behavior.

## Shared frontend implementation

- `static/css/app-shell.css`
  - base `.kpi-orbit-panel` presentation surface
  - reusable `.kpi-orbit` stage, rings, core, metrics and live status components
  - light blue/cyan/lavender ambient field
  - rotating/pulsing orbit rings
  - floating KPI badges
  - count-ready numeric styling
  - module-specific color and motion modifiers
  - responsive tablet/mobile layouts
  - `prefers-reduced-motion` and print fallbacks
- `static/js/app.js`
  - `initKpiOrbits()`
  - stagger delays for orbit badges
  - reuses the existing numeric counter helper for integer KPI values
  - progressive entrance activation with `kpi-orbit-ready`

## Orbit coverage and real data

| Page | Orbit | Server-rendered values used |
| --- | --- | --- |
| Dashboard | Operations / workspace orbit | `pendingPicks`, `openPos`, `pendingRmas` |
| Inventory | Inventory Core | `totalProducts`, `lowStockCount`, `pendingCount`, `locationCount` |
| Sales / POS | Checkout Core | `products.size`, `cart.itemCount`, `cart.lines.size`, `cart.total` |
| Urgency | Stock Health Orbit | `criticalCount`, `warningCount`, `pendingSuggestionCount`, `rows.size` |
| Stock Requests | Request Flow | `pendingCount`, `readyCount`, `requests.size`, `products.size` |
| Warranty / RMA | RMA Workspace | `pendingCount`, `approvedCount`, `todayCount`, `claims.size` |
| Supplier Management | Supplier Network | `supplierCount`, `partnershipCount`, `restockCount`, `poCount` |
| Reports | Insights Workspace | `summary.totalSales`, `summary.totalQuantitySold`, `summary.criticalProducts`, `summary.openPos` |
| Supplier Portal | Supplier Workspace | `openOrderCount`, `terms.size`, `activeOfferCount`, `latestPartnership.status` |

No fake KPI counts were introduced and no controller/service/repository/database logic was changed.

## Module personalities

- Inventory: blue/cyan, stock cube core
- Sales/POS: teal/green/cyan, cart core and faster ring motion
- Urgency: amber/orange/red, stronger risk ring pulse
- Stock Requests: blue/teal, queue-oriented compressed ring
- Warranty/RMA: purple/indigo/blue, shield core
- Supplier Management: teal/blue/cyan, wider network orbit
- Reports: slate/blue/indigo, calmer slower orbit
- Supplier Portal: blue/teal/lavender, softer secure portal motion

## Concentration and accessibility

The orbit is placed as a summary/hero element above work areas. Forms, table text and detailed transactional content are left stable. Under `prefers-reduced-motion: reduce`, drifting, spinning, pulsing and count-up motion is disabled or reduced while the KPI information remains visible.
