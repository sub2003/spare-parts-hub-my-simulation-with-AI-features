package com.sliit.sparepartshub.stockmonitoring.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The single scheduled recalculation entry point for Function 3.
 */
@Component
public class StockMonitoringScheduler {

    private final StockMonitoringService monitoring;

    public StockMonitoringScheduler(StockMonitoringService monitoring) {
        this.monitoring = monitoring;
    }

    @Scheduled(
            fixedDelayString = "${app.stockmonitoring.recalc-fixed-delay-ms:3600000}",
            initialDelayString = "${app.stockmonitoring.recalc-initial-delay-ms:10000}"
    )
    public void recalculateUrgency() {
        monitoring.recalculateAll();
    }
}
