package com.crawlforge.scraper.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScrapeEngineTest {

    @Test
    void crawlsTwoPagesAndWritesOutput() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/page/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String html = path.endsWith("1")
                        ? "<div class='product'><h2 class='title'>Widget A</h2>"
                          + "<span class='price'>100</span></div><a class='next' href='/page/2'>Next</a>"
                        : "<div class='product'><h2 class='title'>Widget B</h2>"
                          + "<span class='price'>200</span></div>";
                LocalServer.respond(exchange, 200, html);
            });

            Path properties = Files.createTempFile("crawlforge-engine", ".properties");
            Files.writeString(properties, """
                    scraper.retry.attempts=1
                    scraper.rate.requestsPerSecond=1000
                    scraper.maxThreads=2
                    """);
            ScraperConfig config = ScraperConfig.load(properties);

            SiteConfig site = SiteConfig.builder()
                    .name("local-shop")
                    .sourceType(SourceType.HTML)
                    .baseUrl(server.url(""))
                    .seedUrl(server.url("/page/1"))
                    .itemSelector(".product")
                    .nextPageSelector("a.next")
                    .field("title", "h2.title")
                    .field("price", ".price")
                    .maxPages(4)
                    .maxDepth(2)
                    .build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry()));
                Path output = Files.createTempFile("crawlforge-output", ".csv");
                EngineResult result = engine.execute(
                        ScrapeJob.builder().name("engine-test").site(site).build(), output.toString());

                assertEquals(2, result.records().size());
                assertEquals("OK", com.crawlforge.scraper.observability.HealthIndicator
                        .check(engine.metrics()).status());
                assertTrue(Files.readString(output).contains("Widget B"));
            }
        }
    }

    @Test
    void enforcesTotalPageBudgetAcrossMultipleSeeds() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/page/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Item</h2></div>"));

            Path properties = Files.createTempFile("crawlforge-budget", ".properties");
            Files.writeString(properties, """
                    scraper.retry.attempts=1
                    scraper.rate.requestsPerSecond=1000
                    scraper.maxThreads=3
                    """);
            ScraperConfig config = ScraperConfig.load(properties);

            SiteConfig site = SiteConfig.builder()
                    .name("budget-test")
                    .sourceType(SourceType.HTML)
                    .baseUrl(server.url(""))
                    .seedUrl(server.url("/page/1"))
                    .seedUrl(server.url("/page/2"))
                    .seedUrl(server.url("/page/3"))
                    .itemSelector(".product")
                    .field("title", "h2.title")
                    .followLinks(false)
                    .maxPages(2)
                    .maxDepth(1)
                    .build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry()));
                EngineResult result = engine.execute(
                        ScrapeJob.builder().name("budget-test").site(site).build());
                assertEquals(2, result.metrics().get("pages"));
                assertEquals(2, result.records().size());
            }
        }
    }

    @Test
    void duplicateSeedsDoNotDuplicateRecords() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/page/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Only once</h2></div>"));

            Path properties = Files.createTempFile("crawlforge-dupes", ".properties");
            Files.writeString(properties, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=1000\n");
            ScraperConfig config = ScraperConfig.load(properties);

            String page = server.url("/page/1");
            SiteConfig site = SiteConfig.builder().name("dupes")
                    .sourceType(SourceType.HTML).baseUrl(server.url(""))
                    .seedUrl(page).seedUrl(page)
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(4).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry()));
                EngineResult result = engine.execute(
                        ScrapeJob.builder().name("dupe-test").site(site).build());
                assertEquals(1, result.records().size());
            }
        }
    }
}
