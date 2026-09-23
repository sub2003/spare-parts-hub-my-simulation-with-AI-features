package com.sliit.sparepartshub.stockmonitoring.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Keeps scheduling configuration inside Function 3 so the shared application
 * bootstrap class does not need to be modified.
 */
@Configuration
@EnableScheduling
public class StockMonitoringSchedulingConfig {
}
