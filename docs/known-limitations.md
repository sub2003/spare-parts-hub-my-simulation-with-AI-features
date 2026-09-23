# Known Limitations

1. **Environment build limitation** — Maven Central is unreachable from the generation environment, so the full Spring Boot compile/startup must be verified locally.
2. **No fake payment/refund gateway** — Sales records payment confirmation internally; Warranty refund records a business resolution only.
3. **Anomaly rules are evidence-driven** — large-discount and abnormal-stock-adjustment rules are not enabled unless immutable audit evidence supports them.
4. **Audit list cap** — the UI loads the latest 500 audit events before applying in-memory filters. This is appropriate for the course/demo dataset; a production-scale system should add database pagination/specification queries.
5. **Legacy/demo serial reconciliation** — historical seed/data can have `product.stock_count` larger than the number of loaded `in_stock` serial rows for a serial-tracked product. POS is safe because it validates real available serials. `database/final-integrity-checks.sql` exposes this mismatch instead of silently fabricating missing serials.
6. **PDF design** — the built-in PDF writer intentionally produces a reliable text-oriented report without adding an external PDF dependency; it is not a complex chart/report designer.
