package com.crawlforge.scraper.core.model;

import com.crawlforge.scraper.core.config.SiteConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A unit of work: which site to crawl, where to start, and the limits to respect. */
public final class ScrapeJob {

    private final String id;
    private final String name;
    private final SiteConfig site;
    private final List<String> seedUrls;
    private final int maxPages;
    private final int maxDepth;
    private JobStatus status;
    private int pagesCrawled;
    private int recordsExtracted;
    private int failures;
    private final Instant createdAt;
    private Instant finishedAt;

    private ScrapeJob(Builder builder) {
        this.id = builder.id == null ? UUID.randomUUID().toString() : builder.id;
        this.name = Objects.requireNonNullElse(builder.name, this.id);
        this.site = Objects.requireNonNull(builder.site, "site config is required");
        this.seedUrls = List.copyOf(builder.seedUrls);
        this.maxPages = builder.maxPages;
        this.maxDepth = builder.maxDepth;
        this.status = JobStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public String id() { return id; }
    public String name() { return name; }
    public SiteConfig site() { return site; }
    public List<String> seedUrls() { return seedUrls; }
    public int maxPages() { return maxPages; }
    public int maxDepth() { return maxDepth; }
    public JobStatus status() { return status; }
    public int pagesCrawled() { return pagesCrawled; }
    public int recordsExtracted() { return recordsExtracted; }
    public int failures() { return failures; }
    public Instant createdAt() { return createdAt; }
    public Instant finishedAt() { return finishedAt; }
    public long elapsedMillis() {
        return (finishedAt == null ? Instant.now() : finishedAt).toEpochMilli() - createdAt.toEpochMilli();
    }

    public synchronized void markRunning() { status = JobStatus.RUNNING; }
    public synchronized void recordPage(boolean ok) {
        pagesCrawled++;
        if (!ok) failures++;
    }
    public synchronized void recordRecords(int count) { recordsExtracted += count; }

    /** Aggregates a worker's counters into the parent job so its status is truthful. */
    public synchronized void aggregate(int pages, int records, int failures) {
        this.pagesCrawled += pages;
        this.recordsExtracted += records;
        this.failures += failures;
    }
    public synchronized void finish() {
        if (status == JobStatus.FAILED) {
            if (finishedAt == null) finishedAt = Instant.now();
            return;
        }
        status = failures == 0 ? JobStatus.SUCCEEDED
                : (recordsExtracted > 0 ? JobStatus.PARTIAL : JobStatus.FAILED);
        finishedAt = Instant.now();
    }
    public synchronized void fail(Throwable cause) {
        status = JobStatus.FAILED;
        finishedAt = Instant.now();
    }

    public static final class Builder {
        private String id;
        private String name;
        private SiteConfig site;
        private final List<String> seedUrls = new ArrayList<>();
        // -1 means "not explicitly set" - fall back to the site recipe's limits.
        private int maxPages = -1;
        private int maxDepth = -1;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder site(SiteConfig site) { this.site = site; return this; }
        public Builder seedUrl(String url) {
            if (url != null && !url.isBlank()) seedUrls.add(url);
            return this;
        }
        public Builder seedUrls(List<String> urls) {
            if (urls != null) urls.forEach(this::seedUrl);
            return this;
        }
        public Builder maxPages(int maxPages) { this.maxPages = maxPages; return this; }
        public Builder maxDepth(int maxDepth) { this.maxDepth = maxDepth; return this; }
        public ScrapeJob build() {
            if (site == null) throw new IllegalArgumentException("site config is required");
            if (seedUrls.isEmpty()) seedUrls.addAll(site.seedUrls());
            if (seedUrls.isEmpty()) throw new IllegalArgumentException("at least one seed URL is required");
            if (maxPages <= 0) maxPages = site.maxPages();
            if (maxDepth <= 0) maxDepth = site.maxDepth();
            return new ScrapeJob(this);
        }
    }
}
