package com.sliit.sparepartshub.reporting.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiForecastScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiForecastScheduler.class);
    private final DemandForecastService forecasts;
    private final boolean scheduledRetraining;

    public AiForecastScheduler(DemandForecastService forecasts,
                               @Value("${ai.forecast.scheduled-retraining:true}") boolean scheduledRetraining) {
        this.forecasts = forecasts;
        this.scheduledRetraining = scheduledRetraining;
    }

    @Scheduled(cron = "${ai.forecast.retrain-cron:0 0 2 * * SUN}")
    public void weeklyRetraining() {
        if (!scheduledRetraining) {
            return;
        }
        try {
            log.info("Starting scheduled AI demand-forecast retraining request.");
            log.info(forecasts.requestRetraining(false));
        } catch (RuntimeException ex) {
            // AI is optional: an offline forecasting service must never break the core application.
            log.warn("Scheduled AI retraining was skipped: {}", ex.getMessage());
        }
    }
}
