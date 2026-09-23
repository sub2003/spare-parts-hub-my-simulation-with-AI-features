from pathlib import Path
import sys

import pandas as pd
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import app as app_module  # noqa: E402
import forecasting  # noqa: E402


def _payload(real_sales_key: str = "realSales", product_id_key: str = "productId", units_key: str = "unitsSold"):
    return {
        "products": [
            {
                product_id_key: 1,
                "productCode" if product_id_key == "productId" else "product_code": "CPU001",
                "name": "AMD Ryzen 7 7700",
                "category": "CPU",
                "brand": "AMD",
            }
        ],
        real_sales_key: [
            {
                "date": "2026-09-23",
                product_id_key: 1,
                units_key: 2,
            }
        ],
        "force": False,
    }


def _prepare(monkeypatch, tmp_path):
    real_file = tmp_path / "real_sales_history.csv"
    runtime_products = tmp_path / "runtime_products.csv"
    monkeypatch.setattr(forecasting, "REAL_FILE", real_file)
    monkeypatch.setattr(forecasting, "RUNTIME_PRODUCTS_FILE", runtime_products)
    monkeypatch.setattr(app_module, "active_metadata", lambda: {"version": "v1"})
    monkeypatch.setattr(app_module, "_training", False)
    return real_file


def test_retrain_accepts_camel_case_and_persists_real_sales(tmp_path, monkeypatch):
    real_file = _prepare(monkeypatch, tmp_path)
    response = TestClient(app_module.app).post("/model/retrain", json=_payload())
    assert response.status_code == 202
    saved = pd.read_csv(real_file)
    assert len(saved) == 1
    assert int(saved.iloc[0]["product_id"]) == 1
    assert int(saved.iloc[0]["units_sold"]) == 2


def test_retrain_accepts_snake_case_aliases(tmp_path, monkeypatch):
    real_file = _prepare(monkeypatch, tmp_path)
    response = TestClient(app_module.app).post(
        "/model/retrain",
        json=_payload(real_sales_key="real_sales", product_id_key="product_id", units_key="units_sold"),
    )
    assert response.status_code == 202
    saved = pd.read_csv(real_file)
    assert len(saved) == 1
    assert int(saved.iloc[0]["units_sold"]) == 2


def test_retrain_does_not_422_for_java_style_payload_with_extra_fields(tmp_path, monkeypatch):
    real_file = _prepare(monkeypatch, tmp_path)
    payload = _payload()
    payload["products"][0]["unusedField"] = "ignored"
    payload["realSales"][0]["unusedField"] = "ignored"
    response = TestClient(app_module.app).post("/model/retrain", json=payload)
    assert response.status_code == 202
    assert real_file.exists()


def test_retrain_skips_one_malformed_row_instead_of_rejecting_entire_request(tmp_path, monkeypatch):
    real_file = _prepare(monkeypatch, tmp_path)
    payload = _payload()
    payload["realSales"].append({"date": "2026-09-24", "productId": None, "unitsSold": "bad"})
    response = TestClient(app_module.app).post("/model/retrain", json=payload)
    assert response.status_code == 202
    saved = pd.read_csv(real_file)
    assert len(saved) == 1
