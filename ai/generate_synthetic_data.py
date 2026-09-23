from __future__ import annotations

import argparse
import hashlib
import math
from pathlib import Path

import numpy as np
import pandas as pd

BASE_DIR = Path(__file__).resolve().parent
DEFAULT_PRODUCTS = BASE_DIR / "config" / "products.csv"
DEFAULT_OUTPUT = BASE_DIR / "data" / "synthetic_sales_history.csv"

# Fixed bootstrap window for this academic/demo build. It intentionally ends
# before the current demo period so real POS sales can take over the newest dates.
DEFAULT_START = "2025-03-01"
DEFAULT_END = "2026-08-31"
DEFAULT_SEED = 42

CATEGORY_BASE = {
    "CPU": 1.65,
    "Motherboard": 1.05,
    "RAM": 1.75,
    "GPU": 0.85,
    "SSD": 1.55,
    "PSU": 1.15,
    "Case": 0.70,
    "Cooler": 1.00,
}

CATEGORY_WEEKEND = {
    "CPU": 1.12,
    "Motherboard": 1.08,
    "RAM": 1.10,
    "GPU": 1.18,
    "SSD": 1.12,
    "PSU": 1.08,
    "Case": 1.13,
    "Cooler": 1.09,
}


def stable_fraction(value: str) -> float:
    digest = hashlib.sha256(value.encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") / float(2**64 - 1)


def demand_personality(product_code: str, category: str) -> tuple[float, float, float, float]:
    """Return baseline multiplier, monthly trend, season phase, spike probability."""
    u = stable_fraction(product_code + "|baseline")
    t = stable_fraction(product_code + "|trend")
    p = stable_fraction(product_code + "|phase")
    s = stable_fraction(product_code + "|spike")

    base = CATEGORY_BASE.get(category, 1.0) * (0.75 + 0.65 * u)
    # Around -1.2% .. +1.8% change per month, product-specific.
    monthly_trend = -0.012 + 0.030 * t
    phase = 2 * math.pi * p
    spike_probability = 0.008 + 0.018 * s
    return base, monthly_trend, phase, spike_probability


def generate(products: pd.DataFrame, start: str, end: str, seed: int) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    dates = pd.date_range(start=start, end=end, freq="D")
    rows: list[dict] = []

    for product in products.itertuples(index=False):
        base, monthly_trend, phase, spike_probability = demand_personality(product.product_code, product.category)
        weekend_multiplier = CATEGORY_WEEKEND.get(product.category, 1.10)

        for day_index, date in enumerate(dates):
            months = day_index / 30.4375
            trend_factor = max(0.65, 1.0 + monthly_trend * months)

            # Saturday/Sunday are a little busier for retail PC-part shopping.
            weekly_factor = weekend_multiplier if date.dayofweek >= 5 else (1.04 if date.dayofweek == 4 else 1.0)

            # Smooth yearly seasonality with product-specific phase.
            seasonal_factor = 1.0 + 0.10 * math.sin((2 * math.pi * date.dayofyear / 365.25) + phase)

            # Mild end-of-month effect and stronger November/December shopping period.
            month_factor = 1.08 if date.day >= 25 else 1.0
            if date.month in (11, 12):
                month_factor *= 1.16
            elif date.month in (1, 2):
                month_factor *= 0.94

            expected = max(0.03, base * trend_factor * weekly_factor * seasonal_factor * month_factor)
            units = int(rng.poisson(expected))

            # Infrequent promotion/popularity bursts, larger for GPU/CPU categories.
            if rng.random() < spike_probability:
                spike_scale = 2.3 if product.category in {"GPU", "CPU"} else 1.7
                units += int(max(1, rng.poisson(expected * spike_scale)))

            rows.append({
                "date": date.date().isoformat(),
                "product_id": int(product.product_id),
                "product_code": product.product_code,
                "category": product.category,
                "brand": product.brand,
                "units_sold": max(0, units),
                "is_synthetic": True,
            })

    return pd.DataFrame(rows)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate reproducible synthetic daily product demand for the Spare Parts Hub demo.")
    parser.add_argument("--products", default=str(DEFAULT_PRODUCTS))
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--start", default=DEFAULT_START)
    parser.add_argument("--end", default=DEFAULT_END)
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED)
    args = parser.parse_args()

    products = pd.read_csv(args.products)
    history = generate(products, args.start, args.end, args.seed)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    history.to_csv(output, index=False)

    print(f"Generated {len(history):,} daily rows for {products.shape[0]} products.")
    print(f"Date range: {history['date'].min()} -> {history['date'].max()}")
    print(f"Seed: {args.seed}")
    print(f"Output: {output}")


if __name__ == "__main__":
    main()
