# AI Retrain 422 Integration Fix (v11)

## Problem observed

The Admin **Retrain Model** request reached FastAPI but returned:

`POST /model/retrain -> 422 Unprocessable Content`

Because FastAPI rejected the request during schema validation, `persist_runtime_payload(...)` never ran and `ai/data/real_sales_history.csv` stayed empty even though real POS sales existed in MySQL.

## Fix

1. Spring Boot now builds the retraining request as an explicit JSON-shaped `Map` with the exact contract expected by FastAPI:
   - `products`
   - `realSales`
   - `force`
   - product fields: `productId`, `productCode`, `name`, `category`, `brand`
   - real-sale fields: `date`, `productId`, `unitsSold`
2. FastAPI accepts both camelCase and snake_case aliases at the integration boundary, while continuing to use the same internal field names.
3. Mutable Pydantic list defaults were replaced with `default_factory=list`.
4. `scikit-learn` is pinned to `1.8.0`, matching the bundled v1 model artifacts and preventing the 1.8.0 -> 1.9.1 model-persistence warning on a fresh setup.

## Expected behavior after the fix

After one or more POS sales exist in MySQL and Admin clicks **Retrain Model**:

- FastAPI should log `POST /model/retrain ... 202 Accepted`.
- `ai/data/real_sales_history.csv` should be rewritten from the current MySQL daily aggregate snapshot.
- Multiple receipts for the same product on the same date remain one `DATE + PRODUCT` row with summed `units_sold`.
- Candidate training continues in the background when forced by the Admin button.

## Local environment note

If the existing `.venv` already has scikit-learn 1.9.1 installed, run:

```powershell
.\.venv\Scripts\python.exe -m pip install --force-reinstall "scikit-learn==1.8.0"
```

Then restart the FastAPI service.
