# AI Demand Forecasting — Viva Quick Answers

**What AI feature did you add?**  
Demand forecasting. It estimates how many units of each product may be needed in the next 7 and 30 days and labels the trend as Rising, Stable or Falling.

**Why did you use synthetic data?**  
The project is a prototype and does not yet have months of real sales. We generated reproducible synthetic history for the real seed-product catalogue and clearly label it as demo data.

**Is the synthetic data just random?**  
No. It includes product/category baselines, trend, weekly patterns, seasonality, noise and occasional spikes. Seed 42 makes it reproducible.

**What are lag features?**  
They are previous sales values, for example `lag_7` means the units sold seven days earlier.

**What is a rolling mean?**  
The average demand during a previous time window. `rolling_mean_7` is the previous seven-day average.

**How did you avoid data leakage?**  
Lag and rolling features only use earlier dates, future targets are not model inputs, and the train/test split is chronological rather than shuffled.

**What model did you use?**  
Random Forest regression with separate models for 7-day and 30-day demand. The data is small/tabular, so a tree model is more appropriate and easier to explain than an LSTM or Transformer.

**How did you evaluate it?**  
MAE and RMSE on the newest chronological test period, and compared it with moving-average baselines. The initial demo model beat both baselines on the synthetic holdout.

**Does AI replace the urgency score?**  
No. Urgency measures current operational pressure using deterministic rules. AI forecasting estimates future demand. They are shown separately.

**How does it learn from new sales?**  
New real SaleItem data is aggregated by date and product. Weekly or Admin-triggered retraining combines the synthetic bootstrap with the real observations and trains a candidate model.

**Does every retrained model automatically replace the old model?**  
No. The candidate is evaluated against the active model on the same new test slice. It replaces the active model only if its error is at least as good.

**What happens if the AI service is down?**  
Reporting still works. The AI panel simply shows that forecasting is temporarily unavailable.
