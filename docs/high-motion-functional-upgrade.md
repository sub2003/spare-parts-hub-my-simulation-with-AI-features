# High-Motion Functional UI Upgrade

This pass increases visible motion across all functional pages without changing Java, security, database, validation, or business logic.

## Added globally

- Peripheral moving light rails and orbit node on functional pages
- Continuous but calm edge runners on metric/action/workflow surfaces
- Reactive edge runners on forms, tables, detail cards, PO documents and panels
- Animated section-title light accents
- Richer card hover lift/glow and detail-item interactions
- Table-card top light, row hover sweep and action-row feedback
- Search/filter result refresh motion and count feedback
- Field focus trace and change-confirm animation
- Button pointer ripple plus existing hover sheen/loading states
- Active navigation icon halo and moving active underline
- Status/chip hover motion and low-frequency status sheen
- Supplier workflow sequential pulsing/floating motion
- Continuous urgency/progress sheen on live bars
- Receive-shipment progress sheen
- Dynamic value change pop for totals/status feedback
- Stronger nested section choreography on all functional modules

## Module behavior

- Inventory: cards/tables/forms/pick/scan inherit high-motion system; scan line remains active.
- Sales/POS: product rows, stock values, cart lines, totals and action buttons react quickly.
- Urgency: progress bars retain a live sheen after backend-value fill; status motion remains local.
- Warranty/RMA: workflow steps, details, forms and statuses inherit richer choreography.
- Supplier Management: subnav, procurement workflow, comparison, totals, tables and PO surfaces receive stronger motion.
- Supplier Portal: operational cards/nav/surfaces receive the same high-motion language.
- Reporting/Audit: entrances, filters and hover interactions are animated; dense data remains calm.
- Profile/Password: form and action motion applies without continuous movement under input text.

## Safety / concentration

Continuous motion remains on page edges, glows, small status indicators and card edges. Dense table text and form values do not continuously move. `prefers-reduced-motion` and print modes disable the decorative motion.
