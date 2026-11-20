package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TransportFeaturesTest {

    @Test
    void charsetFromContentTypeIsHonoured() {
        byte[] bytes = "Caf\u00e9".getBytes(java.nio.charset.Charset.forName("windows-1252"));
        assertEquals("Café", ResilientHttpClient.decode(bytes, "text/html; charset=windows-1252"));
    }

    @Test
    void charsetFromMetaTagIsHonoured() {
        byte[] bytes = "<html><meta charset='windows-1252'><h2>Caf\u00e9</h2></html>"
                .getBytes(java.nio.charset.Charset.forName("windows-1252"));
        assertTrue(ResilientHttpClient.decode(bytes, "text/html").contains("Café"));
    }

    @Test
    void responseReportsUrlAfterRedirect() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/start", exchange -> {
                exchange.getResponseHeaders().set("Location", server.url("/target"));
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            server.route("/target", exchange -> LocalServer.respond(exchange, 200, "done"));

            ScraperConfig config = fastConfig();
            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                assertEquals(server.url("/target"), http.fetch(server.url("/start")).effectiveUrl());
            }
        }
    }

    @Test
    void transportCountersReachTheRunReport() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            AtomicInteger calls = new AtomicInteger();
            server.route("/flaky", exchange -> {
                if (calls.incrementAndGet() < 3) LocalServer.respond(exchange, 500, "oops");
                else LocalServer.respond(exchange, 200,
                        "<div class='product'><h2 class='title'>Recovered</h2></div>");
            });
            ScraperConfig config = fastConfig();
            SiteConfig site = SiteConfig.builder().name("flaky").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/flaky"))
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("retry-test").site(site).build());
                assertEquals(2L, result.metrics().get("retries"));
                assertEquals(1, result.records().size());
            }
        }
    }

    @Test
    void robotsTxtDisallowedPathsAreSkipped() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/robots.txt", exchange ->
                    LocalServer.respond(exchange, 200, "User-agent: *\nDisallow: /private\n"));
            server.route("/private/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Secret</h2></div>"));

            ScraperConfig config = fastConfig();
            SiteConfig site = SiteConfig.builder().name("robots").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/private/x"))
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(2).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("robots-test").site(site).build());
                assertEquals(0, result.records().size());
            }
        }
    }

    @Test
    void httpBasicAuthChallengeIsAnswered() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/basic", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.startsWith("Basic ")) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } else {
                    LocalServer.respond(exchange, 200, "authenticated");
                }
            });
            try (ResilientHttpClient http = new ResilientHttpClient(fastConfig())) {
                http.withBasicAuth(server.url("/basic"), "user", "pass");
                assertEquals("authenticated", http.fetch(server.url("/basic")).body());
            }
        }
    }

    private ScraperConfig fastConfig() throws Exception {
        Path file = Files.createTempFile("r5-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=3\nscraper.retry.baseDelayMs=1\n"
                + "scraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }

    private ScrapeEngine engine(ScraperConfig config, ResilientHttpClient http) {
        return new ScrapeEngine(config, new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
    }
}
