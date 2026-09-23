from __future__ import annotations

import json
import math
import shutil
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "models"
CONFIG_DIR = BASE_DIR / "config"
SYNTHETIC_FILE = DATA_DIR / "synthetic_sales_history.csv"
REAL_FILE = DATA_DIR / "real_sales_history.csv"
BASE_PRODUCTS_FILE = CONFIG_DIR / "products.csv"
RUNTIME_PRODUCTS_FILE = DATA_DIR / "runtime_products.csv"
ACTIVE_FILE = MODELS_DIR / "active.json"

FEATURE_NUMERIC = [
    "day_of_week", "month", "week_of_year", "day_of_month",
    "lag_1", "lag_7", "lag_14", "lag_30",
    "rolling_mean_7", "rolling_mean_14", "rolling_mean_30",
    "rolling_std_7", "rolling_std_30",
]
FEATURE_CATEGORICAL = ["product_code", "category", "brand"]
FEATURE_COLUMNS = FEATURE_CATEGORICAL + FEATURE_NUMERIC
MIN_PRODUCT_DAYS = 60


@dataclass
class Evaluation:
    mae7: float
    rmse7: float
    mae30: float
    rmse30: float
    baseline_mae7: float
    baseline_rmse7: float
    baseline_mae30: float
    baseline_rmse30: float

    @property
    def score(self) -> float:
        return self.mae7 + self.mae30


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def _read_csv_if_present(path: Path) -> pd.DataFrame:
    if not path.exists() or path.stat().st_size == 0:
        return pd.DataFrame()
    try:
        return pd.read_csv(path)
    except pd.errors.EmptyDataError:
        return pd.DataFrame()


def load_products() -> pd.DataFrame:
    base = pd.read_csv(BASE_PRODUCTS_FILE)
    runtime = _read_csv_if_present(RUNTIME_PRODUCTS_FILE)
    if runtime.empty:
        return base
    expected = ["product_id", "product_code", "name", "category", "brand"]
    runtime = runtime[[c for c in expected if c in runtime.columns]].copy()
    for c in expected:
        if c not in runtime.columns:
            runtime[c] = "" if c != "product_id" else np.nan
    combined = pd.concat([base, runtime[expected]], ignore_index=True)
    combined = combined.dropna(subset=["product_id"]).copy()
    combined["product_id"] = combined["product_id"].astype(int)
    # Runtime/current DB catalogue wins for matching ids.
    return combined.drop_duplicates(subset=["product_id"], keep="last").sort_values("product_id")


def load_history() -> pd.DataFrame:
    synthetic = pd.read_csv(SYNTHETIC_FILE)
    real = _read_csv_if_present(REAL_FILE)
    frames = [synthetic]
    if not real.empty:
        frames.append(real)
    df = pd.concat(frames, ignore_index=True)
    df["date"] = pd.to_datetime(df["date"])
    df["product_id"] = pd.to_numeric(df["product_id"], errors="coerce").astype("Int64")
    df["units_sold"] = pd.to_numeric(df["units_sold"], errors="coerce").fillna(0).clip(lower=0).astype(int)
    if "is_synthetic" not in df.columns:
        df["is_synthetic"] = True
    df["is_synthetic"] = df["is_synthetic"].astype(str).str.lower().isin({"true", "1", "yes"})
    df = df.dropna(subset=["product_id", "date"]).copy()
    df["product_id"] = df["product_id"].astype(int)

    # Real rows override a synthetic row for the exact same product/day.
    df["_source_order"] = np.where(df["is_synthetic"], 0, 1)
    df = df.sort_values(["product_id", "date", "_source_order"])
    df = df.drop_duplicates(subset=["product_id", "date"], keep="last").drop(columns=["_source_order"])

    # Fill missing days for each product only within the span it actually has.
    products = load_products().set_index("product_id")
    completed: list[pd.DataFrame] = []
    for product_id, group in df.groupby("product_id", sort=False):
        group = group.sort_values("date").set_index("date")
        dates = pd.date_range(group.index.min(), group.index.max(), freq="D")
        expanded = group.reindex(dates)
        expanded.index.name = "date"
        expanded["product_id"] = product_id
        expanded["units_sold"] = expanded["units_sold"].fillna(0).astype(int)
        # Missing rows in a real-only period are real zero-demand observations.
        if "is_synthetic" in expanded:
            expanded["is_synthetic"] = expanded["is_synthetic"].fillna(False).astype(bool)
        if product_id in products.index:
            p = products.loc[product_id]
            for col in ["product_code", "category", "brand"]:
                expanded[col] = expanded[col].fillna(str(p[col]))
        else:
            for col in ["product_code", "category", "brand"]:
                expanded[col] = expanded[col].ffill().bfill().fillna("UNKNOWN")
        completed.append(expanded.reset_index())

    if not completed:
        return df.sort_values(["product_id", "date"]).reset_index(drop=True)
    return pd.concat(completed, ignore_index=True).sort_values(["product_id", "date"]).reset_index(drop=True)


def _future_sum(series: pd.Series, horizon: int) -> pd.Series:
    result = pd.Series(0.0, index=series.index)
    for step in range(1, horizon + 1):
        result = result + series.shift(-step)
    # Rows without a complete future horizon must not be used as targets.
    valid_until = len(series) - horizon
    if valid_until < len(series):
        result.iloc[max(0, valid_until):] = np.nan
    return result


def add_features(history: pd.DataFrame, include_targets: bool) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for _, group in history.groupby("product_id", sort=False):
        g = group.sort_values("date").copy()
        sales = g["units_sold"].astype(float)
        g["day_of_week"] = g["date"].dt.dayofweek
        g["month"] = g["date"].dt.month
        g["week_of_year"] = g["date"].dt.isocalendar().week.astype(int)
        g["day_of_month"] = g["date"].dt.day
        for lag in (1, 7, 14, 30):
            g[f"lag_{lag}"] = sales.shift(lag)
        shifted = sales.shift(1)
        for window in (7, 14, 30):
            g[f"rolling_mean_{window}"] = shifted.rolling(window=window, min_periods=window).mean()
        for window in (7, 30):
            g[f"rolling_std_{window}"] = shifted.rolling(window=window, min_periods=window).std().fillna(0)
        if include_targets:
            g["future_7d_demand"] = _future_sum(sales, 7)
            g["future_30d_demand"] = _future_sum(sales, 30)
        frames.append(g)
    result = pd.concat(frames, ignore_index=True)
    needed = FEATURE_COLUMNS.copy()
    if include_targets:
        needed += ["future_7d_demand", "future_30d_demand"]
    return result.dropna(subset=needed).reset_index(drop=True)


def make_model(seed: int = 42) -> Pipeline:
    preprocessor = ColumnTransformer([
        ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), FEATURE_CATEGORICAL),
        ("num", "passthrough", FEATURE_NUMERIC),
    ])
    model = RandomForestRegressor(
        n_estimators=140,
        max_depth=16,
        min_samples_leaf=2,
        random_state=seed,
        n_jobs=-1,
    )
    return Pipeline([("prep", preprocessor), ("model", model)])


def chronological_split(data: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    unique_dates = np.array(sorted(data["date"].dt.normalize().unique()))
    if len(unique_dates) < 30:
        raise ValueError("Not enough chronological observations to train the forecasting model.")
    train_cut = unique_dates[max(1, int(len(unique_dates) * 0.70)) - 1]
    valid_cut = unique_dates[max(2, int(len(unique_dates) * 0.85)) - 1]
    train = data[data["date"].dt.normalize() <= train_cut].copy()
    valid = data[(data["date"].dt.normalize() > train_cut) & (data["date"].dt.normalize() <= valid_cut)].copy()
    test = data[data["date"].dt.normalize() > valid_cut].copy()
    if train.empty or valid.empty or test.empty:
        raise ValueError("Chronological split produced an empty partition.")
    return train, valid, test


def _metrics(y_true: pd.Series, y_pred: np.ndarray) -> tuple[float, float]:
    mae = float(mean_absolute_error(y_true, y_pred))
    rmse = float(math.sqrt(mean_squared_error(y_true, y_pred)))
    return mae, rmse


def evaluate_models(model7: Pipeline, model30: Pipeline, test: pd.DataFrame) -> Evaluation:
    x = test[FEATURE_COLUMNS]
    pred7 = np.maximum(0, model7.predict(x))
    pred30 = np.maximum(0, model30.predict(x))
    mae7, rmse7 = _metrics(test["future_7d_demand"], pred7)
    mae30, rmse30 = _metrics(test["future_30d_demand"], pred30)
    baseline7 = np.maximum(0, test["rolling_mean_7"].to_numpy() * 7)
    baseline30 = np.maximum(0, test["rolling_mean_30"].to_numpy() * 30)
    b_mae7, b_rmse7 = _metrics(test["future_7d_demand"], baseline7)
    b_mae30, b_rmse30 = _metrics(test["future_30d_demand"], baseline30)
    return Evaluation(mae7, rmse7, mae30, rmse30, b_mae7, b_rmse7, b_mae30, b_rmse30)


def active_metadata() -> dict[str, Any] | None:
    if not ACTIVE_FILE.exists():
        return None
    pointer = json.loads(ACTIVE_FILE.read_text(encoding="utf-8"))
    version = pointer.get("version")
    if not version:
        return None
    metadata_path = MODELS_DIR / version / "metadata.json"
    if not metadata_path.exists():
        return None
    return json.loads(metadata_path.read_text(encoding="utf-8"))


def _next_version() -> str:
    versions: list[int] = []
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    for path in MODELS_DIR.iterdir():
        if path.is_dir() and path.name.startswith("v") and path.name[1:].isdigit():
            versions.append(int(path.name[1:]))
    return f"v{max(versions, default=0) + 1}"


def _load_active_models() -> tuple[Pipeline, Pipeline, dict[str, Any]] | None:
    meta = active_metadata()
    if not meta:
        return None
    version_dir = MODELS_DIR / meta["version"]
    return (
        joblib.load(version_dir / "forecast_7d.joblib"),
        joblib.load(version_dir / "forecast_30d.joblib"),
        meta,
    )


def _score_existing_on_test(active: tuple[Pipeline, Pipeline, dict[str, Any]] | None, test: pd.DataFrame) -> float | None:
    if active is None:
        return None
    model7, model30, _ = active
    try:
        p7 = np.maximum(0, model7.predict(test[FEATURE_COLUMNS]))
        p30 = np.maximum(0, model30.predict(test[FEATURE_COLUMNS]))
        return float(mean_absolute_error(test["future_7d_demand"], p7) + mean_absolute_error(test["future_30d_demand"], p30))
    except Exception:
        return None


def train_candidate(seed: int = 42, force_promote: bool = False) -> dict[str, Any]:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    history = load_history()
    training = add_features(history, include_targets=True)
    train, valid, test = chronological_split(training)
    fit = pd.concat([train, valid], ignore_index=True)

    model7 = make_model(seed)
    model30 = make_model(seed + 1)
    model7.fit(fit[FEATURE_COLUMNS], fit["future_7d_demand"])
    model30.fit(fit[FEATURE_COLUMNS], fit["future_30d_demand"])
    evaluation = evaluate_models(model7, model30, test)

    version = _next_version()
    version_dir = MODELS_DIR / version
    version_dir.mkdir(parents=True, exist_ok=False)
    joblib.dump(model7, version_dir / "forecast_7d.joblib", compress=3)
    joblib.dump(model30, version_dir / "forecast_30d.joblib", compress=3)

    active = _load_active_models()
    active_score = _score_existing_on_test(active, test)
    candidate_score = evaluation.score
    promoted = force_promote or active is None or active_score is None or candidate_score <= active_score

    source_counts = history["is_synthetic"].value_counts().to_dict()
    metadata: dict[str, Any] = {
        "version": version,
        "status": "ACTIVE" if promoted else "CANDIDATE_REJECTED",
        "modelType": "RandomForestRegressor",
        "trainedAt": utc_now_iso(),
        "trainingRows": int(len(fit)),
        "testRows": int(len(test)),
        "historyRows": int(len(history)),
        "syntheticRows": int(source_counts.get(True, 0)),
        "realRows": int(source_counts.get(False, 0)),
        "dateFrom": history["date"].min().date().isoformat(),
        "dateTo": history["date"].max().date().isoformat(),
        "mae7": round(evaluation.mae7, 4),
        "rmse7": round(evaluation.rmse7, 4),
        "mae30": round(evaluation.mae30, 4),
        "rmse30": round(evaluation.rmse30, 4),
        "baselineMae7": round(evaluation.baseline_mae7, 4),
        "baselineRmse7": round(evaluation.baseline_rmse7, 4),
        "baselineMae30": round(evaluation.baseline_mae30, 4),
        "baselineRmse30": round(evaluation.baseline_rmse30, 4),
        "beatsBaseline7": evaluation.mae7 <= evaluation.baseline_mae7,
        "beatsBaseline30": evaluation.mae30 <= evaluation.baseline_mae30,
        "candidateScore": round(candidate_score, 4),
        "previousActiveScoreOnCandidateTest": None if active_score is None else round(active_score, 4),
        "promoted": promoted,
        "featureList": FEATURE_COLUMNS,
        "target7": "future_7d_demand",
        "target30": "future_30d_demand",
        "dataSource": "SYNTHETIC_PLUS_REAL" if source_counts.get(False, 0) else "SYNTHETIC_DEMO",
        "minimumProductDays": MIN_PRODUCT_DAYS,
        "syntheticDisclosure": "Initial historical training data is synthetically generated for demonstration.",
    }
    (version_dir / "metadata.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    if promoted:
        ACTIVE_FILE.write_text(json.dumps({"version": version}, indent=2), encoding="utf-8")
    return metadata


def _latest_feature_rows(history: pd.DataFrame) -> pd.DataFrame:
    featured = add_features(history, include_targets=False)
    if featured.empty:
        return featured
    return featured.sort_values("date").groupby("product_id", as_index=False).tail(1)


def _trend(forecast30: float, recent30_mean: float) -> str:
    if recent30_mean <= 0.05:
        return "RISING" if forecast30 > 1 else "STABLE"
    forecast_daily = forecast30 / 30.0
    change = (forecast_daily - recent30_mean) / recent30_mean
    if change > 0.10:
        return "RISING"
    if change < -0.10:
        return "FALLING"
    return "STABLE"


def predict_all() -> list[dict[str, Any]]:
    active = _load_active_models()
    if active is None:
        return []
    model7, model30, meta = active
    history = load_history()
    latest = _latest_feature_rows(history)
    products = load_products()
    history_counts = history.groupby("product_id")["date"].nunique().to_dict()
    known_predictions: dict[int, dict[str, Any]] = {}

    for row in latest.itertuples(index=False):
        product_id = int(row.product_id)
        if history_counts.get(product_id, 0) < MIN_PRODUCT_DAYS:
            continue
        row_df = pd.DataFrame([{col: getattr(row, col) for col in FEATURE_COLUMNS}])
        p7 = float(max(0, model7.predict(row_df)[0]))
        p30 = float(max(0, model30.predict(row_df)[0]))
        product_history = history[history["product_id"] == product_id].sort_values("date")
        history30 = product_history.tail(30)["units_sold"].astype(int).tolist()
        recent_mean = float(product_history.tail(30)["units_sold"].mean()) if not product_history.empty else 0.0
        known_predictions[product_id] = {
            "productId": product_id,
            "productCode": str(row.product_code),
            "productName": "",
            "category": str(row.category),
            "forecast7Days": int(round(p7)),
            "forecast30Days": int(round(p30)),
            "forecast7Raw": round(p7, 3),
            "forecast30Raw": round(p30, 3),
            "trend": _trend(p30, recent_mean),
            "scope": "PRODUCT",
            "history30": history30,
            "recent30DailyAverage": round(recent_mean, 3),
            "forecast30DailyAverage": round(p30 / 30.0, 3),
            "modelVersion": meta["version"],
            "trainedAt": meta["trainedAt"],
            "dataSource": meta["dataSource"],
        }

    # Fill names from the latest/current product catalogue.
    product_map = {int(p.product_id): p for p in products.itertuples(index=False)}
    for pid, forecast in known_predictions.items():
        if pid in product_map:
            forecast["productName"] = str(product_map[pid].name)
            forecast["productCode"] = str(product_map[pid].product_code)
            forecast["category"] = str(product_map[pid].category)

    # New products with too little history get a clearly-labelled category fallback.
    results = list(known_predictions.values())
    by_category: dict[str, list[dict[str, Any]]] = {}
    for f in known_predictions.values():
        by_category.setdefault(f["category"], []).append(f)

    for product in products.itertuples(index=False):
        pid = int(product.product_id)
        if pid in known_predictions:
            continue
        peers = by_category.get(str(product.category), [])
        if not peers:
            continue
        p7 = int(round(float(np.mean([p["forecast7Days"] for p in peers]))))
        p30 = int(round(float(np.mean([p["forecast30Days"] for p in peers]))))
        peer_histories = [p["history30"] for p in peers if len(p["history30"]) == 30]
        if peer_histories:
            history30 = np.mean(np.array(peer_histories), axis=0).round().astype(int).tolist()
            recent_mean = float(np.mean(history30))
        else:
            history30 = []
            recent_mean = 0.0
        results.append({
            "productId": pid,
            "productCode": str(product.product_code),
            "productName": str(product.name),
            "category": str(product.category),
            "forecast7Days": p7,
            "forecast30Days": p30,
            "forecast7Raw": float(p7),
            "forecast30Raw": float(p30),
            "trend": _trend(float(p30), recent_mean),
            "scope": "CATEGORY_FALLBACK",
            "history30": history30,
            "recent30DailyAverage": round(recent_mean, 3),
            "forecast30DailyAverage": round(p30 / 30.0, 3),
            "modelVersion": meta["version"],
            "trainedAt": meta["trainedAt"],
            "dataSource": meta["dataSource"],
        })

    return sorted(results, key=lambda x: (-x["forecast30Days"], x["productCode"]))


def persist_runtime_payload(products: list[dict[str, Any]], real_sales: list[dict[str, Any]]) -> int:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if products:
        pd.DataFrame(products).rename(columns={
            "productId": "product_id",
            "productCode": "product_code",
        })[["product_id", "product_code", "name", "category", "brand"]].to_csv(RUNTIME_PRODUCTS_FILE, index=False)

    old_real = _read_csv_if_present(REAL_FILE)
    if old_real.empty:
        old_canonical: list[tuple[str, int, int]] = []
    else:
        old_real["date"] = old_real["date"].astype(str)
        old_canonical = sorted(
            (str(r.date), int(r.product_id), int(r.units_sold))
            for r in old_real.itertuples(index=False)
        )

    product_lookup = load_products().set_index("product_id")
    rows = []
    for item in real_sales:
        pid = int(item["productId"])
        if pid not in product_lookup.index:
            continue
        p = product_lookup.loc[pid]
        rows.append({
            "date": item["date"],
            "product_id": pid,
            "product_code": p["product_code"],
            "category": p["category"],
            "brand": p["brand"],
            "units_sold": max(0, int(item["unitsSold"])),
            "is_synthetic": False,
        })
    # The payload is a full daily aggregate snapshot from the current Spring DB.
    # Overwrite, rather than append, so a reset demo database cannot retain stale real rows.
    new_real_df = pd.DataFrame(rows, columns=["date", "product_id", "product_code", "category", "brand", "units_sold", "is_synthetic"])
    new_real_df.to_csv(REAL_FILE, index=False)
    new_canonical = sorted((str(r["date"]), int(r["product_id"]), int(r["units_sold"])) for r in rows)
    old_map = {(d, pid): units for d, pid, units in old_canonical}
    new_map = {(d, pid): units for d, pid, units in new_canonical}
    changed_keys = {key for key in set(old_map) | set(new_map) if old_map.get(key) != new_map.get(key)}
    return len(changed_keys)


def reset_models() -> None:
    if MODELS_DIR.exists():
        shutil.rmtree(MODELS_DIR)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
