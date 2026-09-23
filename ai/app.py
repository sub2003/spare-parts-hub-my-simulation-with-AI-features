from __future__ import annotations

import logging
import threading
from datetime import datetime, timedelta
from typing import Any

from fastapi import BackgroundTasks, FastAPI, HTTPException
from pydantic import AliasChoices, BaseModel, ConfigDict, Field

from forecasting import active_metadata, persist_runtime_payload, predict_all, train_candidate

app = FastAPI(
    title="Spare Parts Hub AI Demand Forecast Service",
    version="1.0.0",
    description="Internal demo forecasting service. Initial historical data is synthetic and clearly labelled.",
)

logger = logging.getLogger("spare-parts-hub-ai")

_training_lock = threading.Lock()
_training = False
_last_training_error: str | None = None


class ProductCatalogItem(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    productId: int = Field(validation_alias=AliasChoices("productId", "product_id"))
    productCode: str = Field(validation_alias=AliasChoices("productCode", "product_code"))
    name: str
    category: str
    brand: str


class RealSalesRow(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    date: str
    productId: int = Field(validation_alias=AliasChoices("productId", "product_id"))
    unitsSold: int = Field(ge=0, validation_alias=AliasChoices("unitsSold", "units_sold"))


class RetrainRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    products: list[ProductCatalogItem] = Field(default_factory=list)
    realSales: list[RealSalesRow] = Field(
        default_factory=list,
        validation_alias=AliasChoices("realSales", "real_sales"),
    )
    force: bool = False


def _next_sunday_2am() -> str:
    now = datetime.now().astimezone()
    days = (6 - now.weekday()) % 7
    candidate = (now + timedelta(days=days)).replace(hour=2, minute=0, second=0, microsecond=0)
    if candidate <= now:
        candidate += timedelta(days=7)
    return candidate.isoformat()


def _background_retrain() -> None:
    global _training, _last_training_error
    try:
        train_candidate()
        _last_training_error = None
    except Exception as exc:  # kept internal; Spring receives safe status text
        _last_training_error = f"{type(exc).__name__}: {exc}"
    finally:
        with _training_lock:
            _training = False


@app.get("/health")
def health() -> dict[str, Any]:
    return {"status": "UP", "service": "demand-forecast"}


@app.get("/model/status")
def model_status() -> dict[str, Any]:
    meta = active_metadata()
    if meta is None:
        return {
            "status": "TRAINING" if _training else "UNAVAILABLE",
            "version": None,
            "message": _last_training_error or "No active model has been trained yet.",
            "nextRetraining": _next_sunday_2am(),
        }
    result = dict(meta)
    result["status"] = "TRAINING" if _training else "ACTIVE"
    result["nextRetraining"] = _next_sunday_2am()
    result["message"] = _last_training_error
    return result


@app.get("/forecast/products")
def forecasts() -> list[dict[str, Any]]:
    if active_metadata() is None:
        raise HTTPException(status_code=503, detail="No active forecasting model is available.")
    return predict_all()


@app.get("/forecast/products/{product_id}")
def forecast_product(product_id: int) -> dict[str, Any]:
    for forecast in predict_all():
        if forecast["productId"] == product_id:
            return forecast
    raise HTTPException(status_code=404, detail="No product-level or category fallback forecast is available.")


def _normalize_retrain_payload(payload: dict[str, Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]], bool]:
    """Normalize Spring/JSON naming differences at the service boundary.

    We intentionally validate row-by-row instead of letting FastAPI reject the
    entire request with HTTP 422 because one integration field uses a different
    JSON naming convention. The core training code still receives the canonical
    camelCase contract used by persist_runtime_payload().
    """
    raw_products = payload.get("products") or []
    raw_real_sales = payload.get("realSales")
    if raw_real_sales is None:
        raw_real_sales = payload.get("real_sales") or []

    products: list[dict[str, Any]] = []
    if isinstance(raw_products, list):
        for item in raw_products:
            if not isinstance(item, dict):
                continue
            pid = item.get("productId", item.get("product_id"))
            code = item.get("productCode", item.get("product_code"))
            name = item.get("name")
            category = item.get("category")
            brand = item.get("brand")
            try:
                pid = int(pid)
            except (TypeError, ValueError):
                continue
            if any(value is None for value in (code, name, category, brand)):
                continue
            products.append({
                "productId": pid,
                "productCode": str(code),
                "name": str(name),
                "category": str(category),
                "brand": str(brand),
            })

    real_sales: list[dict[str, Any]] = []
    if isinstance(raw_real_sales, list):
        for item in raw_real_sales:
            if not isinstance(item, dict):
                continue
            date = item.get("date")
            pid = item.get("productId", item.get("product_id"))
            units = item.get("unitsSold", item.get("units_sold"))
            try:
                pid = int(pid)
                units = max(0, int(units))
            except (TypeError, ValueError):
                continue
            if date is None:
                continue
            real_sales.append({
                "date": str(date),
                "productId": pid,
                "unitsSold": units,
            })

    raw_force = payload.get("force", False)
    if isinstance(raw_force, str):
        force = raw_force.strip().lower() in {"1", "true", "yes", "on"}
    else:
        force = bool(raw_force)
    return products, real_sales, force


@app.post("/model/retrain", status_code=202)
def retrain(payload: dict[str, Any], background_tasks: BackgroundTasks) -> dict[str, Any]:
    global _training
    products, real_sales, force = _normalize_retrain_payload(payload)
    logger.info(
        "Retrain request received: products=%d realSales=%d force=%s",
        len(products),
        len(real_sales),
        force,
    )
    with _training_lock:
        if _training:
            return {"accepted": False, "status": "TRAINING", "message": "Retraining is already in progress."}
        changed_observations = persist_runtime_payload(products, real_sales)
        if active_metadata() is not None and not force:
            if changed_observations == 0:
                return {
                    "accepted": False,
                    "status": "ACTIVE",
                    "message": "No new real daily sales observations were found. The current model was kept.",
                }
            if changed_observations < 3:
                return {
                    "accepted": False,
                    "status": "ACTIVE",
                    "message": f"Only {changed_observations} real daily observation(s) changed; scheduled retraining waits for at least 3.",
                }
        _training = True
    background_tasks.add_task(_background_retrain)
    return {
        "accepted": True,
        "status": "TRAINING",
        "message": "Candidate model training started. The current active model remains available until evaluation finishes.",
    }
