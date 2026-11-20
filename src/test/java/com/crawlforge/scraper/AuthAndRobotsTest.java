package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.NonRetryableFetchException;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.stealth.RobotsTxtChecker;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AuthAndRobotsTest {

    @Test
    void deniedRobotsTxtDisallowsEverything() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/robots.txt", exchange -> LocalServer.respond(exchange, 403, "Forbidden"));
            assertFalse(RobotsTxtChecker.INSTANCE.allowed(server.url("/anything")));
        }
    }

    @Test
    void inlineRobotsCommentsAreIgnored() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/robots.txt", exchange -> LocalServer.respond(exchange, 200,
                    "User-agent: *\nDisallow: /private # admins only\n"));
            assertFalse(RobotsTxtChecker.INSTANCE.allowed(server.url("/private/x")));
        }
    }

    @Test
    void wildcardGroupWithSeveralUserAgentsApplies() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/robots.txt", exchange -> LocalServer.respond(exchange, 200,
                    "User-agent: *\nUser-agent: OtherBot\nDisallow: /private\n"));
            assertFalse(RobotsTxtChecker.INSTANCE.allowed(server.url("/private/x")));
        }
    }

    @Test
    void metaCharsetSniffingDoesNotTouchJson() {
        String json = "{\"bio\":\"<meta charset='windows-1252'>\",\"name\":\"Café\"}";
        assertEquals(json, ResilientHttpClient.decode(json.getBytes(StandardCharsets.UTF_8),
                "application/json"));
    }

    @Test
    void permanentHttpErrorsAreNotRetried() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            AtomicInteger calls = new AtomicInteger();
            server.route("/missing", exchange -> {
                calls.incrementAndGet();
                LocalServer.respond(exchange, 404, "no");
            });
            try (ResilientHttpClient http = new ResilientHttpClient(config())) {
                assertThrows(NonRetryableFetchException.class, () -> http.fetch(server.url("/missing")));
                assertEquals(1, calls.get());
            }
        }
    }

    @Test
    void basicCredentialsFromRecipeAreUsedWithoutLoginUrl() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/protected", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } else {
                    LocalServer.respond(exchange, 200,
                            "<div class='product'><h2 class='title'>Secret</h2></div>");
                }
            });
            ScraperConfig config = config();
            SiteConfig site = SiteConfig.builder().name("basic-only").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/protected"))
                    .username("user").password("pass")
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("basic-recipe").site(site).build());
                assertEquals(1, result.records().size());
            }
        }
    }

    @Test
    void basicCredentialsAreScopedToTheirHost() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/basic", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } else {
                    LocalServer.respond(exchange, 200, "leaked");
                }
            });
            try (ResilientHttpClient http = new ResilientHttpClient(config())) {
                http.withBasicAuth("http://some-other-host.test", "user", "pass");
                assertThrows(NonRetryableFetchException.class, () -> http.fetch(server.url("/basic")));
            }
        }
    }

    private ScraperConfig config() throws Exception {
        Path file = Files.createTempFile("r6-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }

    private ScrapeEngine engine(ScraperConfig config, ResilientHttpClient http) {
        return new ScrapeEngine(config, new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
    }
}
