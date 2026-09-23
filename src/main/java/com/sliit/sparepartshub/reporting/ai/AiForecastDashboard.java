package com.sliit.sparepartshub.reporting.ai;

import java.util.List;

/** Safe Reporting-page representation; unavailable AI never breaks the dashboard. */
public record AiForecastDashboard(
        boolean available,
        String message,
        AiModelStatus model,
        List<AiForecastRow> forecasts,
        AiForecastRow featured
) {
    public static AiForecastDashboard unavailable(String message) {
        return new AiForecastDashboard(false, message, null, List.of(), null);
    }

    public static AiForecastDashboard available(AiModelStatus model, List<AiForecastRow> forecasts) {
        return new AiForecastDashboard(true, null, model, forecasts,
                forecasts == null || forecasts.isEmpty() ? null : forecasts.get(0));
    }
}
