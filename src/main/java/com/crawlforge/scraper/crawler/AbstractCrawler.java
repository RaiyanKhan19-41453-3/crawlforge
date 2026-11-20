package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ScraperException;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.parsers.DataValidator;
import com.crawlforge.scraper.parsers.ParserRegistry;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template method shared by all crawlers: seed the frontier, fetch, parse, validate, discover
 * links and respect page/depth limits. Subclasses only implement fetching and link discovery.
 */
public abstract class AbstractCrawler implements Crawler {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ParserRegistry parserRegistry;
    protected final PaginationHandler pagination;

    protected AbstractCrawler(ParserRegistry parserRegistry, PaginationHandler pagination) {
        this.parserRegistry = parserRegistry;
        this.pagination = pagination;
    }

    @Override
    public final List<ScrapedRecord> crawl(ScrapeJob job, CrawlContext context) {
        List<ScrapedRecord> results = new ArrayList<>();
        SiteConfig site = job.site();
        try {
            beforeCrawl(job, context);
            job.markRunning();
            job.seedUrls().forEach(url -> context.enqueue(url, 0));

            while (!context.isEmpty() && context.pages() < job.maxPages()) {
                String url = context.poll();
                if (url == null) break;
                int depth = context.depthOf(url);
                if (depth > job.maxDepth()) continue;
                if (!com.crawlforge.scraper.stealth.RobotsTxtChecker.INSTANCE.allowed(url)) {
                    log.debug("Skipping {} - disallowed by robots.txt", url);
                    continue;
                }
                if (!context.tryReservePage()) break;

                try {
                    String raw = fetchRaw(url, job);
                    String pageUrl = resolvedPageUrl(url, job);
                    if (raw == null || raw.isBlank()) {
                        context.recordFailure();
                        context.recordPage(0);
                        job.recordPage(false);
                        continue;
                    }
                    // Parsers work from a page body and may not know the exact URL. Restore it
                    // here so every exported row has auditable provenance.
                    List<ScrapedRecord> records = DataValidator.filter(parserRegistry.parse(raw, site))
                            .stream()
                            .map(record -> record.url() == null || record.url().isBlank()
                                    || record.url().equals(site.baseUrl()) ? record.withUrl(pageUrl) : record)
                            .toList();
                    results.addAll(records);
                    context.recordPage(records.size());
                    job.recordPage(true);
                    job.recordRecords(records.size());
                    log.debug("Crawled {} -> {} records", url, records.size());

                    if (site.followLinks() && depth < job.maxDepth()) {
                        discoverLinks(raw, job, pageUrl).forEach(link -> context.enqueue(link, depth + 1));
                    }
                } catch (ScraperException e) {
                    log.error("[{}] {} at {}: {}", e.errorCode(), e.getMessage(), url, e.toString());
                    context.recordFailure();
                    context.recordPage(0);
                    job.recordPage(false);
                    onPageFailure(url, e);
                } catch (RuntimeException e) {
                    log.error("Unexpected crawler error at {}: {}", url, e.toString());
                    context.recordFailure();
                    context.recordPage(0);
                    job.recordPage(false);
                    onPageFailure(url, e);
                }
            }
        } finally {
            // Always release browser/web-client resources, even when setup failed.
            try {
                afterCrawl(job, context);
            } finally {
                job.finish();
            }
        }
        return List.copyOf(results);
    }

    /** Raw source fetched from one URL. */
    protected abstract String fetchPage(String url, ScrapeJob job);

    /** Binary-safe fetch hook; overrides handle formats such as PDF. */
    protected String fetchRaw(String url, ScrapeJob job) { return fetchPage(url, job); }

    /** URL after redirects, used as the base for relative links and record provenance. */
    protected String resolvedPageUrl(String requestedUrl, ScrapeJob job) { return requestedUrl; }

    /** Hook for engines that discover links differently (for example by clicking Next). */
    protected List<String> discoverLinks(String raw, ScrapeJob job, String pageUrl) {
        return pagination.discover(raw, job.site(), pageUrl);
    }

    protected void beforeCrawl(ScrapeJob job, CrawlContext context) { }
    protected void afterCrawl(ScrapeJob job, CrawlContext context) { }
    protected void onPageFailure(String url, Throwable cause) { }
}
