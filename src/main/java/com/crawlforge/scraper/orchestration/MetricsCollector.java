package com.crawlforge.scraper.orchestration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe counters for pages, records, failures, retries and anti-bot events. */
public final class MetricsCollector {

    private final AtomicLong pages = new AtomicLong();
    private final AtomicLong records = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong captchas = new AtomicLong();
    private final AtomicLong blocked = new AtomicLong();

    public void addPages(long n) { pages.addAndGet(n); }
    public void addRecords(long n) { records.addAndGet(n); }
    public void addFailures(long n) { failures.addAndGet(n); }
    public void addRetries(long n) { retries.addAndGet(n); }
    public void addCaptchas(long n) { captchas.addAndGet(n); }
    public void addBlocked(long n) { blocked.addAndGet(n); }

    public Map<String, Long> snapshot() {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("pages", pages.get());
        values.put("records", records.get());
        values.put("failures", failures.get());
        values.put("retries", retries.get());
        values.put("captchas", captchas.get());
        values.put("blocked", blocked.get());
        return Map.copyOf(values);
    }

    public String summary() { return snapshot().toString(); }
}
