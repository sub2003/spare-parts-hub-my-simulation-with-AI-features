# Spare Parts Hub — AI Retrain 422 Fix v11 Verification

## Confirmed runtime symptom

The supplied FastAPI log showed the Admin retraining request reaching the AI service but failing validation with:

`POST /model/retrain -> 422 Unprocessable Content`

This explains why real POS rows present in MySQL were not written into `ai/data/real_sales_history.csv`: FastAPI rejected the request before `persist_runtime_payload(...)` could run.

## Integration fix

### Spring Boot

`DemandForecastService.requestRetraining(...)` now builds an explicit JSON-shaped payload using `LinkedHashMap` rather than relying on serialization of private nested request records.

The exact keys sent are:

- `products`
- `realSales`
- `force`
- product: `productId`, `productCode`, `name`, `category`, `brand`
- real sale: `date`, `productId`, `unitsSold`

This matches the FastAPI contract directly.

### FastAPI

The Pydantic request models now accept both camelCase and snake_case integration aliases:

- `productId` / `product_id`
- `productCode` / `product_code`
- `unitsSold` / `units_sold`
- `realSales` / `real_sales`

List defaults now use `default_factory=list`.

### Model compatibility

`ai/requirements.txt` now pins:

`scikit-learn==1.8.0`

The bundled v1 model artifacts were created with scikit-learn 1.8.0, so a fresh environment will no longer install 1.9.1 and produce the observed model-persistence version warnings.

## Targeted verification

Executed against the corrected Python service code:

- Python compilation: PASS
- Existing forecasting tests: PASS
- Camel-case retrain request: PASS
- Camel-case real sale persisted to temporary `real_sales_history.csv`: PASS
- Snake-case alias retrain request: PASS
- Full pytest result: **6 passed**

The retrain API test verifies that a valid request returns HTTP 202 and persists the supplied real daily sale row.

## Java/Maven verification limitation

A full Maven compile could not be run in this environment because the Maven wrapper attempted to download Maven 3.9.16 from Maven Central and network access was unavailable. A raw `javac` parse attempt reached normal missing-dependency/classpath errors and showed no Java syntax-parser error in the changed source.

## Expected local behavior

After starting MySQL, FastAPI and Spring Boot, clicking **Reports -> Retrain Model** should now produce a FastAPI line similar to:

`POST /model/retrain HTTP/1.1" 202 Accepted`

The file:

`ai/data/real_sales_history.csv`

should then contain the current MySQL daily POS aggregate snapshot, grouped by `DATE(sold_at) + product_id`.
