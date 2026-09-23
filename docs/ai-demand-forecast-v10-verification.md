# Spare Parts Hub — AI Demand Forecasting v10 Verification

## What was implemented

A complete demo AI Demand Forecasting feature was added to Function 6 Reporting / Insights Workspace.

The implementation provides:

- realistic synthetic historical daily demand for the project's seeded products
- 7-day demand forecast
- 30-day demand forecast
- RISING / STABLE / FALLING trend
- current-stock comparison and simple potential-shortage indicator
- FastAPI forecasting service
- model status/version metadata
- chronological train/validation/test workflow
- moving-average baselines
- model promotion safety
- real SaleItem daily aggregation from Spring/MySQL
- scheduled weekly retraining
- Admin-triggered retraining
- category fallback for new products with insufficient individual history
- graceful Reporting-page fallback when the AI service is offline
- explicit synthetic/demo-data disclosure

The existing deterministic urgency formula was not changed.

## Actual project structures inspected

Relevant existing source inspected before implementation included:

- `entity/Product.java`
- `entity/Sale.java`
- `entity/SaleItem.java`
- `reporting/controller/ReportingController.java`
- `reporting/service/ReportingService.java`
- `reporting/repository/ReportingProductRepository.java`
- `reporting/repository/ReportingSaleItemRepository.java`
- `templates/reporting/index.html`
- `static/css/app-shell.css`
- `static/js/app.js`
- `database/simulation-seed.sql`
- `database/schema-simulation.sql`
- `application.properties`
- `security/SecurityConfig.java`

The source bootstrap catalogue contains 10 products across CPU, Motherboard, RAM, GPU, SSD, PSU, Case and Cooler categories.

## Synthetic dataset

- Path: `ai/data/synthetic_sales_history.csv`
- Date range: `2025-03-01` to `2026-08-31`
- Products: 10
- Rows: 5,490
- Granularity: one row per product per day
- Seed: 42
- `is_synthetic=true` for generated history

The generator includes stable product/category baselines, per-product trend, day-of-week influence, annual seasonality, end-of-month / holiday-style effects, Poisson noise and infrequent spikes. The generated dataset is reproducible with the same seed.

## Features

Categorical:

- product_code
- category
- brand

Calendar:

- day_of_week
- month
- week_of_year
- day_of_month

Historical:

- lag_1
- lag_7
- lag_14
- lag_30
- rolling_mean_7
- rolling_mean_14
- rolling_mean_30
- rolling_std_7
- rolling_std_30

Future target columns are never part of the feature list.

## Targets

- `future_7d_demand`
- `future_30d_demand`

Each target is the sum of the following 7 or 30 daily quantities respectively.

## Data split

The split is chronological, not randomly shuffled:

- oldest ~70% of dates: train
- next ~15%: validation
- newest ~15%: test

The final training fit uses train + validation and is evaluated on the newest test period.

## Baselines

- 7-day baseline = previous 7-day mean × 7
- 30-day baseline = previous 30-day mean × 30

## Model

Two `RandomForestRegressor` pipelines are used, one per horizon, with one-hot encoding for product/category/brand and numeric lag/calendar features.

The initial active model is `v1`.

## Initial v1 evaluation

| Horizon | ML MAE | ML RMSE | Baseline MAE | Baseline RMSE | Result |
|---|---:|---:|---:|---:|---|
| 7 days | 2.8497 | 3.5686 | 3.7135 | 4.6476 | ML beats baseline |
| 30 days | 7.0566 | 9.4958 | 7.9784 | 9.8965 | ML beats baseline |

These are synthetic-demo holdout results and are not claimed as real-world validated business accuracy.

## Initial example forecasts

- AMD Ryzen 7 7700 (`CPU001`): 14 units / 7 days, 63 units / 30 days, RISING
- Intel Core i5-14400F (`CPU002`): 14 / 53, FALLING
- Kingston Fury 16GB DDR5 (`RAM001`): 9 / 51, STABLE
- Samsung 990 EVO 1TB (`SSD001`): 10 / 41, RISING

## Trend rule

The forecasted 30-day daily average is compared with the recent 30-day average:

- > +10% => RISING
- < -10% => FALLING
- otherwise => STABLE

## FastAPI endpoints

- `GET /health`
- `GET /model/status`
- `GET /forecast/products`
- `GET /forecast/products/{productId}`
- `POST /model/retrain`

The service is intended to bind to `127.0.0.1` only for the demo.

## Real sales / retraining flow

Spring aggregates real POS demand using a native query grouped by:

`DATE(sold_at) + product_id`

and sends it with the current product catalogue to FastAPI.

The Python service stores real rows with `is_synthetic=false`. Real data replaces a synthetic row when the exact product/date overlaps.

Scheduled retraining:

- default: Sunday 02:00
- waits until at least 3 real daily observations have changed
- keeps the current model if there is no meaningful new data

Admin `Retrain Model`:

- explicit force-retrain request
- starts candidate training in the FastAPI background
- the active model continues serving forecasts during training

Candidate promotion:

- candidate and current model are evaluated on the same newest chronological test slice
- candidate becomes active only if combined 7-day + 30-day MAE is at least as good
- rejected candidates do not replace the active model

## New-product / insufficient-history behavior

Product-level forecasting requires at least 60 daily observations.

A current catalogue product without enough individual history can receive a clearly labelled `CATEGORY_FALLBACK` forecast from established products in the same category.

A retraining smoke test used an additional product `GPU0011` and confirmed:

- scope: `CATEGORY_FALLBACK`
- 7-day forecast: 11
- 30-day forecast: 32
- trend: RISING

## Reporting UI

The Reporting dashboard now contains an `AI Demand Forecast` section with:

- AI Active / Training state
- model version/type
- last training time
- training row counts
- synthetic / real observation counts
- MAE / RMSE
- explicit synthetic-demo disclosure
- highest 30-day forecast card
- historical solid-line + future dashed-average chart
- product forecast table
- current stock
- simple potential shortage before incoming POs
- Product Model / Category Fallback scope label
- Refresh Forecast control
- Admin Retrain Model control

When FastAPI is unavailable, the Reporting page remains usable and shows a non-blocking unavailable card.

## Security / business-logic safety

- `/reporting/**` remains Admin-only through existing Spring Security.
- FastAPI retraining is documented as an internal localhost service.
- Existing urgency calculations are unchanged.
- No database schema changes were introduced for AI.
- Existing Sales, Inventory, Supplier, RMA and Reporting behavior remains separate from the forecasting service.

## Files added

- `ai/.gitignore`
- `ai/README.md`
- `ai/app.py`
- `ai/config/products.csv`
- `ai/data/real_sales_history.csv`
- `ai/data/synthetic_sales_history.csv`
- `ai/forecasting.py`
- `ai/generate_synthetic_data.py`
- `ai/models/active.json`
- `ai/models/v1/forecast_7d.joblib`
- `ai/models/v1/forecast_30d.joblib`
- `ai/models/v1/metadata.json`
- `ai/requirements.txt`
- `ai/run-ai-service.bat`
- `ai/run-ai-service.ps1`
- `ai/tests/test_forecasting.py`
- `ai/train.py`
- `docs/ai-demand-forecasting.md`
- `docs/ai-demand-forecasting-run-guide.md`
- `docs/ai-demand-forecasting-viva.md`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/AiForecastDashboard.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/AiForecastRow.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/AiForecastScheduler.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/AiForecastSchedulingConfig.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/AiModelStatus.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/DailyProductSalesProjection.java`
- `src/main/java/com/sliit/sparepartshub/reporting/ai/DemandForecastService.java`

## Existing files changed

- `README.md`
- `src/main/java/com/sliit/sparepartshub/reporting/controller/ReportingController.java`
- `src/main/java/com/sliit/sparepartshub/reporting/repository/ReportingSaleItemRepository.java`
- `src/main/resources/application.properties`
- `src/main/resources/templates/reporting/index.html`
- `src/main/resources/static/css/app-shell.css`
- `src/main/resources/static/js/app.js`

## Verification performed

### Python / model

- synthetic generation: PASS
- reproducibility with seed 42: PASS
- no-negative-demand test: PASS
- lag feature test: PASS
- active forecast test: PASS
- `pytest`: **4 passed**
- `py_compile`: PASS

### FastAPI

- `/health`: PASS (`UP`)
- `/model/status`: PASS (`ACTIVE`, `v1`)
- `/forecast/products`: PASS (10 bootstrap forecasts)
- retraining request: PASS
- active model remains available while training: PASS
- category fallback for a newly supplied GPU product: PASS
- no-new-data scheduled retrain skip: PASS
- force retrain acceptance: PASS

### Frontend static checks

- JavaScript `node --check`: PASS
- CSS parse: 0 errors
- Thymeleaf/HTML templates scanned: 46
- duplicate literal IDs: 0
- nested forms: 0

### Java / Spring

A raw `javac` syntax scan of changed Java source showed no syntax-style parser diagnostics; expected missing Spring/project-class symbols occur without a Maven classpath.

Attempted:

```bash
./mvnw -q -DskipTests compile
```

The environment could not download Maven 3.9.16 from Maven Central:

```text
wget: Failed to fetch https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

Therefore a full Spring Boot compile/runtime integration against MySQL was **not** claimed in this environment.

## Exact local run steps

1. Open the updated project in IntelliJ.
2. Ensure the existing MySQL database is running as usual.
3. Open PowerShell in the project root.
4. Run:

```powershell
cd ai
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app:app --host 127.0.0.1 --port 8000
```

5. Keep that terminal open.
6. Start Spring Boot from IntelliJ.
7. Open `http://localhost:8080/reporting`.
8. The AI section should show v1 immediately.
9. Click `Retrain Model` as Admin to send the current DB catalogue + real SaleItem history to the Python service.
10. Refresh Reporting after training completes to see updated model metadata and real-observation counts.

## Main limitation

The initial training history is deliberately synthetic, so the demo demonstrates a technically meaningful forecasting pipeline but does not prove production forecasting accuracy. As real data accumulates, real holdout evaluation should become the basis for model selection.
