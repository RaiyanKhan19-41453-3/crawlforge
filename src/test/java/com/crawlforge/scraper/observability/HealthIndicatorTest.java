package com.crawlforge.scraper.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.orchestration.MetricsCollector;
import org.junit.jupiter.api.Test;

class HealthIndicatorTest {

    @Test
    void reportsHealthyWhenNothingRan() {
        HealthReport report = HealthIndicator.check(new MetricsCollector());
        assertTrue(report.healthy());
    }

    @Test
    void reportsDegradedWhenAllPagesFail() {
        MetricsCollector metrics = new MetricsCollector();
        metrics.addPages(2);
        metrics.addFailures(2);
        HealthReport report = HealthIndicator.check(metrics);
        assertFalse(report.healthy());
        assertEquals("DEGRADED", report.status());
    }
}
