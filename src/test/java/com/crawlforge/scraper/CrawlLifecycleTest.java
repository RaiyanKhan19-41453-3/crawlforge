package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class CrawlLifecycleTest {

    @Test
    void recordsWithoutOwnUrlInheritPageUrl() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/page/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Widget</h2></div>"));
            ScraperConfig config = config();
            // No baseUrl on purpose: a seed URL is enough to run.
            SiteConfig site = SiteConfig.builder().name("no-base").sourceType(SourceType.HTML)
                    .seedUrl(server.url("/page/1"))
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();
            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("no-base-test").site(site).build());
                assertEquals(1, result.records().size());
                assertEquals(server.url("/page/1"), result.records().get(0).url());
            }
        }
    }

    @Test
    void authenticationStateIsResetBetweenJobs() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/login", exchange -> {
                exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
                LocalServer.respond(exchange, 200, "ok");
            });
            server.route("/protected", exchange -> {
                if (exchange.getRequestHeaders().getFirst("Authorization") == null) {
                    exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                } else {
                    LocalServer.respond(exchange, 200,
                            "<div class='product'><h2 class='title'>Secret</h2></div>");
                }
            });
            ScraperConfig config = config();
            SiteConfig formSite = SiteConfig.builder().name("form").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/protected"))
                    .loginUrl(server.url("/login")).username("u").password("p")
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();
            SiteConfig basicSite = SiteConfig.builder().name("basic").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/protected"))
                    .username("u").password("p")
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = engine(config, http);
                assertEquals(1, engine.execute(ScrapeJob.builder().name("a").site(formSite).build())
                        .records().size());
                assertEquals(1, engine.execute(ScrapeJob.builder().name("b").site(basicSite).build())
                        .records().size());
            }
        }
    }

    private ScraperConfig config() throws Exception {
        Path file = Files.createTempFile("r8-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }

    private ScrapeEngine engine(ScraperConfig config, ResilientHttpClient http) {
        return new ScrapeEngine(config, new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
    }
}
