# Run Guide — AI Demand Forecasting

## 1. Install Python dependencies (one time)

From the project root in PowerShell:

```powershell
cd ai
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 2. Start the AI service

The project already contains the generated 5,490-row dataset and trained v1 model.

```powershell
python -m uvicorn app:app --host 127.0.0.1 --port 8000
```

Or run:

```powershell
.\run-ai-service.ps1
```

Keep this terminal open.

Check:

- `http://127.0.0.1:8000/health`
- `http://127.0.0.1:8000/model/status`

## 3. Start Spring Boot

Start Spare Parts Hub from IntelliJ as usual, then open:

`http://localhost:8080/reporting`

The **AI Demand Forecast** panel appears in the Reporting / Insights Workspace.

## 4. Retrain with the current database

Click **Retrain Model** on the Reporting page as Admin. Spring sends the current product catalogue and real POS daily sales to FastAPI. This manual action intentionally forces a candidate retrain; the weekly scheduler skips retraining until at least 3 real daily observations have changed. The active model continues serving while a candidate trains in the background.

Refresh the Reporting page after training finishes. `Real Records` will increase when the database contains SaleItem history.

## Recreate the bootstrap data/model if needed

```powershell
cd ai
python generate_synthetic_data.py
python train.py --force-promote
```

## Important demo disclosure

The initial history is synthetic. Do not describe the initial predictions as validated real-world forecasts. New POS sales are real observations and are marked separately during retraining.
