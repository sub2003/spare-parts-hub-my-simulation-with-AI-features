# AI Retrain 422 Fix — v12

This patch hardens the Spring Boot -> FastAPI retraining boundary after a real Windows run still returned HTTP 422.

Changes:

- FastAPI `/model/retrain` now accepts a generic JSON object and normalizes camelCase/snake_case keys row-by-row instead of rejecting the full request during strict Pydantic request validation.
- Valid product and real-sale rows are canonicalized before `persist_runtime_payload(...)`.
- One malformed row is skipped instead of causing the complete real-sales snapshot to fail with 422.
- Spring explicitly sends `Content-Type: application/json`.
- Spring's JDK HTTP client is forced to HTTP/1.1 to avoid unnecessary h2c upgrade attempts against Uvicorn.
- Spring now includes the FastAPI error response body in the surfaced retraining error if a non-2xx response occurs.
- scikit-learn remains pinned to 1.8.0 to match the bundled v1 joblib models.

Expected successful runtime flow:

1. Admin clicks Retrain Model.
2. Spring queries daily POS sales grouped by `DATE(sold_at) + product_id`.
3. Spring POSTs product catalogue + real daily sales JSON to `/model/retrain`.
4. FastAPI logs the received product/real-sales counts.
5. `ai/data/real_sales_history.csv` is overwritten with the current full real daily aggregate snapshot.
6. FastAPI responds `202 Accepted` and candidate training starts in the background.

Targeted Python validation: 8 tests passed.
