package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crawlforge.scraper.automation.CsvCleanupTool;
import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Round11FeaturesTest {

    @Test
    void sharedFrontierVisitsConvergingPagesOnce() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/a", exchange -> LocalServer.respond(exchange, 200,
                    "<a href='/common'>go</a>"));
            server.route("/b", exchange -> LocalServer.respond(exchange, 200,
                    "<a href='/common'>go</a>"));
            server.route("/common", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>One</h2></div>"));

            ScraperConfig config = config();
            SiteConfig site = SiteConfig.builder().name("converge").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/a")).seedUrl(server.url("/b"))
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(true).maxPages(10).maxDepth(2).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("converge-test").site(site).build());
                assertEquals(1, result.records().size());
            }
        }
    }

    @Test
    void explicitZeroLimitsAreRespected() {
        SiteConfig site = SiteConfig.builder().name("limits").seedUrl("http://x/1")
                .maxPages(7).maxDepth(2).build();
        ScrapeJob job = ScrapeJob.builder().site(site).maxPages(0).maxDepth(0).build();
        assertEquals(0, job.maxPages());
        assertEquals(0, job.maxDepth());
    }

    @Test
    void groovyCsvCleanupKeepsExtraColumnsAndEmptyInput() {
        String cleaned = CsvCleanupTool.clean("id,name\n1,Widget,EXTRA\n");
        assertEquals("1,Widget,EXTRA", cleaned.split("\\R")[1]);
        assertEquals("", CsvCleanupTool.clean(""));
    }

    private ScraperConfig config() throws Exception {
        Path file = Files.createTempFile("r11-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=1\nscraper.maxThreads=2\n"
                + "scraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }

    private ScrapeEngine engine(ScraperConfig config, ResilientHttpClient http) {
        return new ScrapeEngine(config, new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
    }
}
