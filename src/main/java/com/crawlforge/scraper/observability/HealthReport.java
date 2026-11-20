package com.crawlforge.scraper.observability;

import java.util.Map;

/** Small health report suitable for a /health endpoint or VDI monitoring script. */
public record HealthReport(boolean healthy, String status, Map<String, Long> metrics) { }
