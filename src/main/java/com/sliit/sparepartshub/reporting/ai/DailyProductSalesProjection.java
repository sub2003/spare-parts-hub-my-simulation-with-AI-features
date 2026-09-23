package com.sliit.sparepartshub.reporting.ai;

import java.time.LocalDate;

/** Aggregated real POS demand used only when retraining the optional AI model. */
public interface DailyProductSalesProjection {
    LocalDate getSaleDate();
    Integer getProductId();
    Long getUnitsSold();
}
