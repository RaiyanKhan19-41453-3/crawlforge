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
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.persistence.DatabaseManager;
import com.crawlforge.scraper.persistence.RecordRepository;
import java.nio.file.Path;
import java.util.Map;
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

    @Option(names = {"--engine"}, description = "auto, selenium, playwright or htmlunit", defaultValue = "auto")
    private String engine = "auto";

    @Option(names = {"--demo"}, description = "Run against the built-in offline demo website")
    private boolean demo;

    @Option(names = {"--persist"}, description = "Also write records to the configured SQL database")
    private boolean persist;

    @Override
    public void run() {
        ScraperConfig config = ScraperConfig.load(configPath);
        System.setProperty("crawler.engine", engine);
        ParserRegistry registry = new ParserRegistry();

        try (ResilientHttpClient http = new ResilientHttpClient(config)) {
            CrawlerFactory factory = new CrawlerFactory(config, http, registry);
            ScrapeEngine scrapeEngine = new ScrapeEngine(config, factory);

            if (demo) {
                runDemo(scrapeEngine, config);
                return;
            }
            runConfiguredSite(scrapeEngine, config);
        } catch (Exception e) {
            System.err.println("Crawl failed: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new CommandLine.ExecutionException(new CommandLine(this), e.getMessage(), e);
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
        database.initializeSchema(Path.of("sql", "schema.sql"));
        RecordRepository repository = new RecordRepository(database);
        int saved = repository.saveAll(result.records());
        System.out.println("Persisted: " + saved + " rows (" + repository.qualitySummary() + ")");
    }

    private void runConfiguredSite(ScrapeEngine engine, ScraperConfig config) {
        Map<String, SiteConfig> sites = new SiteConfigLoader().loadAll(sitesDir);
        if (siteName == null || !sites.containsKey(siteName)) {
            System.out.println("Available site recipes: " + sites.keySet());
            System.out.println("Use --site <name> to run one, or --demo for the offline example.");
            return;
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
        long pages = result.metrics().getOrDefault("pages", 0L);
        long failures = result.metrics().getOrDefault("failures", 0L);
        System.out.println("Health:  " + (pages == 0 || failures * 2 < pages ? "OK" : "DEGRADED"));
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CrawlForgeApplication()).execute(args);
        System.exit(exitCode);
    }
}
