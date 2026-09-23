# Spare Parts Hub — AI Demand Forecasting demo service

This service forecasts product demand for the next **7 days** and **30 days**. The initial historical dataset is **synthetically generated for demonstration** because the academic prototype does not yet have months of real POS history.

## Windows quick start

```powershell
cd ai
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python generate_synthetic_data.py
python train.py --force-promote
python -m uvicorn app:app --host 127.0.0.1 --port 8000
```

Then start the Spring Boot app normally and open `/reporting`.

The repository already includes a generated demo dataset and trained `v1` model, so after installing requirements you normally only need the final `uvicorn` command.

## Internal endpoints

- `GET /health`
- `GET /model/status`
- `GET /forecast/products`
- `GET /forecast/products/{productId}`
- `POST /model/retrain`

`POST /model/retrain` is intended to be called by the authenticated Spring Boot Admin workflow, not exposed publicly. For the demo, bind FastAPI to `127.0.0.1` only.

## Data lifecycle

Initial synthetic history + new real daily POS aggregates -> candidate retraining -> evaluation against the active model -> promote only if the candidate is at least as good on the new chronological test slice.
