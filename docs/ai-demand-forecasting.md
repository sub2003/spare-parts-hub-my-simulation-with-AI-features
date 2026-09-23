# AI Demand Forecasting — Demo Feature

## Purpose

The Reporting / Insights Workspace now includes an optional AI demand forecast for the next **7 days** and **30 days**. It is intentionally separate from Function 3's deterministic urgency score: urgency describes current operational pressure, while AI estimates future demand.

## Why synthetic history is used

This is an academic/demo application and does not yet contain months of real POS history. The initial model is therefore trained from a reproducible **synthetic historical dataset**. The UI explicitly labels this as demo data. New real sales from the application are sent to the forecasting service during retraining and are marked as real observations.

## Actual project data inspected

The existing project stores:

- `Product`: product id/code, name, category, brand, selling price, stock count, reorder level, urgency score, warranty months and serial-tracking flag.
- `Sale`: sale code, staff member, sale timestamp and total amount.
- `SaleItem`: product, quantity, final price at sale, per-unit discount and discount reason.

The bootstrap catalogue in `database/simulation-seed.sql` contains 10 products across CPU, Motherboard, RAM, GPU, SSD, PSU, Case and Cooler categories. Runtime retraining sends the **current MySQL Product catalogue**, so products added after the original seed can appear via a clearly labelled category fallback until enough product history exists.

## Synthetic dataset

- File: `ai/data/synthetic_sales_history.csv`
- Date range: **2025-03-01 to 2026-08-31**
- Products: **10 bootstrap products**
- Rows: **5,490 daily product observations**
- Random seed: **42**
- One row: one product on one date
- `is_synthetic=true` is stored on every generated row

Demand is not pure random noise. Each product has a stable personality based on its product code plus category baseline, slow trend, weekly/weekend effect, annual seasonality, mild end-of-month/holiday effects, Poisson demand noise and infrequent spikes.

## Features

Categorical:

- product code
- category
- brand

Calendar:

- day of week
- month
- ISO week of year
- day of month

Historical lag/rolling features:

- lag 1, 7, 14, 30
- rolling mean 7, 14, 30
- rolling standard deviation 7, 30

All lag/rolling features are built from past observations. Future target values are never used as model inputs.

## Targets

- `future_7d_demand`: sum of the next 7 daily units
- `future_30d_demand`: sum of the next 30 daily units

## Split and baseline

The data is sorted chronologically and split by date: about 70% oldest dates for training, 15% for validation and the newest 15% for test. It is **not randomly shuffled**.

Baselines:

- 7-day baseline = recent 7-day average × 7
- 30-day baseline = recent 30-day average × 30

## Model

Two `RandomForestRegressor` pipelines are trained, one for each horizon. Categorical columns use one-hot encoding and the numeric lag/calendar features pass through unchanged. This was chosen instead of an LSTM/Transformer because the demo dataset is small, tabular and easier to explain and evaluate with a tree ensemble.

Initial v1 test results:

| Horizon | ML MAE | ML RMSE | Baseline MAE | Baseline RMSE | Result |
|---|---:|---:|---:|---:|---|
| 7 days | 2.8497 | 3.5686 | 3.7135 | 4.6476 | ML beats baseline |
| 30 days | 7.0566 | 9.4958 | 7.9784 | 9.8965 | ML beats baseline |

These metrics measure performance on **synthetic demo history**, not validated real-world business performance.

## Trend rule

The predicted 30-day daily average is compared with the recent 30-day daily average:

- more than +10% -> `RISING`
- less than -10% -> `FALLING`
- otherwise -> `STABLE`

## Architecture

```text
Spare Parts Hub / Spring Boot
        |
        | REST (localhost)
        v
Python FastAPI forecasting service
        |
        +-- synthetic + real daily history
        +-- feature engineering
        +-- v1/v2/... model registry
        +-- prediction endpoints
```

Spring Boot remains the authenticated user-facing system. FastAPI is intended to bind only to `127.0.0.1` for this demo.

## API

- `GET /health`
- `GET /model/status`
- `GET /forecast/products`
- `GET /forecast/products/{productId}`
- `POST /model/retrain`

## Real sales and retraining

Spring aggregates real `SaleItem` quantities by **date + product** and sends a complete snapshot to FastAPI. Real rows are saved with `is_synthetic=false`. If a real row overlaps a synthetic product/date, the real row wins.

Retraining is requested weekly (default Sunday 02:00) and can also be started by an Admin from Reporting. Scheduled retraining waits until at least **3 real daily observations** have changed; the Admin button is an explicit force-retrain control. Training happens in the FastAPI background so the HTTP request returns quickly. The currently active model keeps serving predictions while a candidate trains.

The candidate is evaluated on a chronological test slice. It becomes active only when its combined 7-day + 30-day MAE is at least as good as the current active model on that same candidate test slice. Rejected candidates remain versioned but do not replace the active model.

## Data sufficiency and new products

Product-level prediction requires at least 60 daily observations. A current database product without enough individual history may receive a `CATEGORY_FALLBACK` forecast using peers in the same category. The UI labels the scope so a fallback is not presented as a product-specific trained estimate.

## Reporting UI

The Reporting page shows:

- AI status / model version
- last training timestamp
- synthetic vs real counts
- MAE/RMSE
- 7-day and 30-day demand
- Rising / Stable / Falling trend
- current stock
- simple potential shortage (`forecast30 - current stock`, floored at 0)
- historical solid-line chart and dashed forecast-average segment
- explicit synthetic-demo disclosure
- Admin retraining control

Potential shortage deliberately does **not** subtract incoming POs yet; the UI labels it as before incoming purchase orders to avoid inventing stock relief.

## Failure behavior

The AI service is optional. If FastAPI is offline or times out, `/reporting` still loads and displays an "AI forecasting temporarily unavailable" card. Existing reports, audit, urgency, sales and other business features continue normally.

## Limitations

1. Initial model quality is measured on synthetic rather than real operational history.
2. Forecasts are aggregate 7/30-day quantities, not a full probabilistic forecast interval.
3. Category fallback is a demo cold-start strategy, not a separately trained hierarchical model.
4. Potential shortage currently ignores incoming purchase orders.
5. No external market, promotion or economic variables are included.

## Future improvements

As enough real sales accumulate, reduce or retire synthetic rows, evaluate real holdout periods, add promotion/price/incoming-stock features where valid, add uncertainty intervals, and consider a dedicated hierarchical time-series model if data volume justifies it.
