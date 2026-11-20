package com.crawlforge.scraper.observability;

import com.crawlforge.scraper.orchestration.MetricsCollector;
import java.util.Map;

/** Derives a health verdict from live crawl metrics. */
public final class HealthIndicator {

    private HealthIndicator() {}

    public static HealthReport check(MetricsCollector metrics) {
        return check(metrics.snapshot());
    }

    /** Derives a verdict directly from a run's metric map. */
    public static HealthReport check(Map<String, Long> snapshot) {
        long pages = snapshot.getOrDefault("pages", 0L);
        long failures = snapshot.getOrDefault("failures", 0L);
        boolean healthy = pages > 0 && failures * 2 < pages;
        return new HealthReport(healthy, healthy ? "OK" : "DEGRADED", snapshot);
    }
}
