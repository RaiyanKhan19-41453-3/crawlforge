package com.crawlforge.scraper;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.config.SiteConfigLoader;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.demo.DemoServer;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.orchestration.JobScheduler;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.persistence.DatabaseManager;
import com.crawlforge.scraper.persistence.RecordRepository;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** CLI entry point for the fat jar used in Docker, on a VDI or on any client platform. */
@Command(name = "crawlforge", version = "CrawlForge 1.0.0", mixinStandardHelpOptions = true,
        description = "Enterprise Java/Groovy web scraping engine with data-quality and anti-bot tooling")
public final class CrawlForgeApplication implements Runnable {

    @Option(names = {"--config"}, description = "Path to application.properties")
    private Path configPath = Path.of("config/application.properties");

    @Option(names = {"--sites"}, description = "Directory containing site JSON recipes")
    private Path sitesDir = Path.of("config/sites");

    @Option(names = {"--site"}, description = "Name of a site recipe to run")
    private String siteName;

    @Option(names = {"--output"}, description = "Output file (.csv, .json or .xlsx)")
    private String output;

    @Option(names = {"--engine"}, description = "auto, selenium, htmlunit or playwright (browser binaries required)", defaultValue = "auto")
    private String engine = "auto";

    @Option(names = {"--demo"}, description = "Run against the built-in offline demo website")
    private boolean demo;

    @Option(names = {"--persist"}, description = "Also write records to the configured SQL database")
    private boolean persist;

    @Option(names = {"--interval"}, description = "Repeat the crawl every N seconds until interrupted (0 = once)")
    private int intervalSeconds;

    @Override
    public void run() {
        ScraperConfig config = ScraperConfig.load(configPath);
        ParserRegistry registry = new ParserRegistry();

        try (ResilientHttpClient http = new ResilientHttpClient(config)) {
            CrawlerFactory factory = new CrawlerFactory(config, http, registry, engine);
            ScrapeEngine scrapeEngine = new ScrapeEngine(config, factory);

            if (intervalSeconds > 0) {
                try (JobScheduler scheduler = new JobScheduler()) {
                    scheduler.schedule(() -> {
                        try {
                            executeOnce(scrapeEngine, config);
                        } catch (Exception e) {
                            throw new IllegalStateException("Scheduled crawl failed", e);
                        }
                    }, 0, intervalSeconds, TimeUnit.SECONDS);
                    System.out.println("Scheduled every " + intervalSeconds + " seconds. Press Ctrl+C to stop.");
                    new CountDownLatch(1).await();
                }
            } else {
                executeOnce(scrapeEngine, config);
            }
        } catch (Exception e) {
            System.err.println("Crawl failed: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new CommandLine.ExecutionException(new CommandLine(this), e.getMessage(), e);
        }
    }

    private void executeOnce(ScrapeEngine engine, ScraperConfig config) throws Exception {
        if (demo) {
            runDemo(engine, config);
        } else {
            runConfiguredSite(engine, config);
        }
    }

    private void runDemo(ScrapeEngine engine, ScraperConfig config) throws Exception {
        try (DemoServer server = DemoServer.start()) {
            String base = server.baseUrl();
            SiteConfig site = SiteConfig.builder()
                    .name("demo-shop")
                    .sourceType(SourceType.HTML)
                    .baseUrl(base)
                    .seedUrl(base + "/page/1")
                    .itemSelector(".product")
                    .nextPageSelector("a.next")
                    .field("title", "h2.title")
                    .field("price", ".price")
                    .xpathField("sku", "//span[@id='sku']")
                    .maxPages(2)
                    .maxDepth(2)
                    .build();
            ScrapeJob job = ScrapeJob.builder().name("Offline demo crawl").site(site).build();
            String target = output == null ? Path.of(config.outputDir(), "demo-records.csv").toString() : output;
            EngineResult result = engine.execute(job, target);
            printSummary(result, target);
            persistIfRequested(result, config);
        }
    }

    private void persistIfRequested(EngineResult result, ScraperConfig config) {
        if (!persist) return;
        if (!config.databaseEnabled()) {
            System.out.println("Persistence skipped: no SCRAPER_DB_URL configured.");
            return;
        }
        DatabaseManager database = new DatabaseManager(config);
        String schema = config.dbUrl().contains(":mysql:") ? "schema-mysql.sql" : "schema.sql";
        database.initializeSchema(Path.of("sql", schema));
        RecordRepository repository = new RecordRepository(database);
        int saved = repository.saveAll(result.records());
        System.out.println("Persisted: " + saved + " rows (" + repository.qualitySummary() + ")");
    }

    private void runConfiguredSite(ScrapeEngine engine, ScraperConfig config) {
        Map<String, SiteConfig> sites = new SiteConfigLoader().loadAll(sitesDir);
        if (siteName == null) {
            System.out.println("Available site recipes: " + sites.keySet());
            System.out.println("Use --site <name> to run one, or --demo for the offline example.");
            return;
        }
        if (!sites.containsKey(siteName)) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "Unknown site '" + siteName + "'. Available: " + sites.keySet());
        }
        SiteConfig site = sites.get(siteName);
        ScrapeJob job = ScrapeJob.builder().name(site.name()).site(site).build();
        String target = output == null
                ? Path.of(config.outputDir(), site.name() + ".json").toString() : output;
        EngineResult result = engine.execute(job, target);
        printSummary(result, target);
        persistIfRequested(result, config);
    }

    private static void printSummary(EngineResult result, String target) {
        System.out.println("Records: " + result.records().size());
        System.out.println("Elapsed: " + result.elapsedMs() + " ms");
        System.out.println("Metrics: " + result.metrics());
        System.out.println("Output:  " + target);
        com.crawlforge.scraper.observability.HealthReport health =
                com.crawlforge.scraper.observability.HealthIndicator.check(result.metrics());
        System.out.println("Health:  " + health.status());
        if (!health.healthy()) {
            throw new CommandLine.ExecutionException(new CommandLine(new CrawlForgeApplication()),
                    "Crawl health is " + health.status() + " - check the metrics above");
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CrawlForgeApplication()).execute(args);
        System.exit(exitCode);
    }
}
