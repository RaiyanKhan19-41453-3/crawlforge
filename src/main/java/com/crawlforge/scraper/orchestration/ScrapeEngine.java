package com.crawlforge.scraper.orchestration;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.crawler.CrawlContext;
import com.crawlforge.scraper.crawler.Crawler;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.export.WriterFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs a job's seed URLs in parallel worker threads, then exports and reports. */
public final class ScrapeEngine {

    private static final Logger log = LoggerFactory.getLogger(ScrapeEngine.class);
    private final ScraperConfig config;
    private final CrawlerFactory crawlerFactory;
    private final MetricsCollector metrics = new MetricsCollector();

    public ScrapeEngine(ScraperConfig config, CrawlerFactory crawlerFactory) {
        this.config = config;
        this.crawlerFactory = crawlerFactory;
    }

    public EngineResult execute(ScrapeJob job) { return execute(job, null); }

    public EngineResult execute(ScrapeJob job, String outputTarget) {
        long started = System.currentTimeMillis();
        job.markRunning();
        int seedCount = Math.max(1, job.seedUrls().size());
        ExecutorService pool = WorkerPoolExecutor.newPool(Math.min(config.maxThreads(), seedCount));
        List<Future<List<ScrapedRecord>>> futures = new ArrayList<>();

        for (int i = 0; i < job.seedUrls().size(); i++) {
            final String seed = job.seedUrls().get(i);
            ScrapeJob subJob = ScrapeJob.builder()
                    .id(job.id() + "-" + i)
                    .name(job.name() + " [" + seed + "]")
                    .site(job.site())
                    .seedUrl(seed)
                    .maxPages(Math.max(1, job.maxPages() / seedCount))
                    .maxDepth(job.maxDepth())
                    .build();
            futures.add(pool.submit(() -> {
                Crawler crawler = crawlerFactory.create(job.site().sourceType());
                CrawlContext context = new CrawlContext();
                List<ScrapedRecord> records = crawler.crawl(subJob, context);
                job.aggregate(context.pages(), records.size(), context.failures());
                metrics.addPages(context.pages());
                metrics.addFailures(context.failures());
                return records;
            }));
        }

        List<ScrapedRecord> all = new ArrayList<>();
        for (Future<List<ScrapedRecord>> future : futures) {
            try {
                List<ScrapedRecord> records = future.get(5, TimeUnit.MINUTES);
                all.addAll(records);
                metrics.addRecords(records.size());
            } catch (Exception e) {
                log.error("Worker failed: {}", e.getMessage());
                metrics.addFailures(1);
            }
        }
        WorkerPoolExecutor.shutdown(pool, 30);

        if (outputTarget != null && !outputTarget.isBlank()) {
            WriterFactory.forTarget(outputTarget).write(all, outputTarget);
            log.info("Wrote {} records to {}", all.size(), outputTarget);
        }
        job.finish();
        return new EngineResult(all, metrics.snapshot(), System.currentTimeMillis() - started);
    }

    public MetricsCollector metrics() { return metrics; }
}
