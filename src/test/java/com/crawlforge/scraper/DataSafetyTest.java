package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.NonRetryableFetchException;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.export.CsvWriter;
import com.crawlforge.scraper.export.ExcelWriter;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.stealth.RobotsTxtChecker;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DataSafetyTest {

    @Test
    void relativeCredentialUrlNeverRegistersCredentials() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/basic", exchange -> {
                if (exchange.getRequestHeaders().getFirst("Authorization") == null) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } else {
                    LocalServer.respond(exchange, 200, "leaked");
                }
            });
            try (ResilientHttpClient http = new ResilientHttpClient(config(1))) {
                http.withBasicAuth("/login", "user", "pass");
                assertThrows(NonRetryableFetchException.class, () -> http.fetch(server.url("/basic")));
            }
        }
    }

    @Test
    void binaryFetchesValidateStatusAndRetryTransientErrors() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            AtomicInteger calls = new AtomicInteger();
            server.route("/doc.pdf", exchange -> {
                if (calls.incrementAndGet() == 1) {
                    LocalServer.respond(exchange, 503, "temporarily unavailable");
                } else {
                    LocalServer.respond(exchange, 200, "PDF-BYTES");
                }
            });
            try (ResilientHttpClient http = new ResilientHttpClient(config(3))) {
                assertEquals("PDF-BYTES", new String(http.fetchBytes(server.url("/doc.pdf")).body()));
                assertEquals(1, http.totalRetries());
            }
        }
    }

    @Test
    void parallelSeedsLogInOnce() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            AtomicInteger logins = new AtomicInteger();
            server.route("/login", exchange -> {
                logins.incrementAndGet();
                exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
                LocalServer.respond(exchange, 200, "ok");
            });
            server.route("/page/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Item</h2></div>"));

            ScraperConfig config = config(1);
            SiteConfig site = SiteConfig.builder().name("login-once").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/page/1"))
                    .seedUrl(server.url("/page/2")).seedUrl(server.url("/page/3"))
                    .loginUrl(server.url("/login")).username("u").password("p")
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(3).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry(), "auto"))
                        .execute(ScrapeJob.builder().name("login-test").site(site).build());
                assertEquals(3, result.records().size());
                assertEquals(1, logins.get());
            }
        }
    }

    @Test
    void robotsTxtServerErrorDisallowsSite() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/robots.txt", exchange -> LocalServer.respond(exchange, 503, "down"));
            assertFalse(RobotsTxtChecker.INSTANCE.allowed(server.url("/page")));
        }
    }

    @Test
    void csvWriterNeutralisesFormulaInjection() throws Exception {
        Path target = Files.createTempFile("formula", ".csv");
        new CsvWriter().write(List.of(ScrapedRecord.builder().source("x").url("http://x/1")
                .title("=HYPERLINK(\"http://evil\",\"click\")").content("c").build()), target.toString());
        assertTrue(Files.readString(target).contains("'=HYPERLINK"));
    }

    @Test
    void excelWriterAcceptsVeryLongCellValues() throws Exception {
        Path target = Files.createTempFile("long", ".xlsx");
        String longText = "x".repeat(40_000);
        new ExcelWriter().write(List.of(ScrapedRecord.builder().source("x").url("http://x/1")
                .content(longText).build()), target.toString());
        assertTrue(Files.size(target) > 0);
    }

    private ScraperConfig config(int attempts) throws Exception {
        Path file = Files.createTempFile("r7-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=" + attempts
                + "\nscraper.retry.baseDelayMs=1\nscraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }
}
