package com.sliit.sparepartshub.reporting.ai;

/** Read-only metadata returned by the internal Python forecasting service. */
public record AiModelStatus(
        String status,
        String version,
        String modelType,
        String trainedAt,
        Integer trainingRows,
        Integer historyRows,
        Integer syntheticRows,
        Integer realRows,
        Double mae7,
        Double rmse7,
        Double mae30,
        Double rmse30,
        Double baselineMae7,
        Double baselineRmse7,
        Double baselineMae30,
        Double baselineRmse30,
        Boolean beatsBaseline7,
        Boolean beatsBaseline30,
        String dataSource,
        String nextRetraining,
        String message
) {
}
