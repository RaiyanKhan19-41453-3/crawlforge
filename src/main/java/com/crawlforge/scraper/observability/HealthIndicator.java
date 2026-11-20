package com.crawlforge.scraper.observability;

import com.crawlforge.scraper.orchestration.MetricsCollector;
import java.util.Map;

/** Derives a health verdict from live crawl metrics. */
public final class HealthIndicator {

    private HealthIndicator() {}

    public static HealthReport check(MetricsCollector metrics) {
        Map<String, Long> snapshot = metrics.snapshot();
        long pages = snapshot.getOrDefault("pages", 0L);
        long failures = snapshot.getOrDefault("failures", 0L);
        boolean healthy = pages == 0 || failures * 2 < pages;
        return new HealthReport(healthy, healthy ? "OK" : "DEGRADED", snapshot);
    }
}
