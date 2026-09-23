from pathlib import Path
import sys

import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from forecasting import FEATURE_COLUMNS, add_features, load_history, predict_all  # noqa: E402


def test_history_non_negative():
    history = load_history()
    assert not history.empty
    assert (history["units_sold"] >= 0).all()


def test_features_do_not_contain_target_columns():
    history = load_history()
    featured = add_features(history, include_targets=True)
    assert set(FEATURE_COLUMNS).isdisjoint({"future_7d_demand", "future_30d_demand"})
    assert not featured.empty


def test_lag_one_is_previous_day():
    history = load_history()
    product_id = int(history.iloc[0]["product_id"])
    product = history[history["product_id"] == product_id].sort_values("date")
    featured = add_features(product, include_targets=False)
    row = featured.iloc[0]
    previous_date = row["date"] - pd.Timedelta(days=1)
    previous = product[product["date"] == previous_date]
    assert not previous.empty
    assert float(row["lag_1"]) == float(previous.iloc[0]["units_sold"])


def test_active_model_returns_forecasts():
    forecasts = predict_all()
    assert forecasts
    assert all(f["forecast7Days"] >= 0 and f["forecast30Days"] >= 0 for f in forecasts)
    assert all(f["trend"] in {"RISING", "STABLE", "FALLING"} for f in forecasts)
