package com.sliit.sparepartshub.reporting.ai;

/** View model combining AI output with live inventory values from Spring/MySQL. */
public record AiForecastRow(
        Integer productId,
        String productCode,
        String productName,
        String category,
        int forecast7Days,
        int forecast30Days,
        String trend,
        String scope,
        int currentStock,
        int potentialShortage30Days,
        String historyCsv,
        double forecast30DailyAverage
) {
    public boolean isRising() {
        return "RISING".equalsIgnoreCase(trend);
    }

    public boolean isFalling() {
        return "FALLING".equalsIgnoreCase(trend);
    }

    public boolean hasShortageRisk() {
        return potentialShortage30Days > 0;
    }
}
