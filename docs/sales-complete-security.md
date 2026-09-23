# Function 2 — Security Notes

The existing security configuration was not changed.

- `SALES_EXEC`: allowed under `/sales/**`.
- `ADMIN`: allowed under `/sales/**`.
- `WAREHOUSE_CLERK`: not allowed to perform POS sales; continues to use Inventory/Picking.
- Supplier accounts: remain isolated under the separate `/supplier-portal/**` security chain and cannot use staff Sales routes.

Final checkout revalidates all submitted state server-side. Hidden/disabled UI controls are not treated as authorization or validation.

Explicit serial selection is validated again at checkout and the selected serial rows are pessimistically locked before being marked sold.
