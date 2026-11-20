package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.export.CsvWriter;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.payment.PaymentwallPayloadParser;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class Round10FeaturesTest {

    @Test
    void metricsArePerRunNotCumulative() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/page/", exchange -> LocalServer.respond(exchange, 200,
                    "<div class='product'><h2 class='title'>Item</h2></div>"));
            ScraperConfig config = config();
            SiteConfig site = SiteConfig.builder().name("metrics").sourceType(SourceType.HTML)
                    .baseUrl(server.url("")).seedUrl(server.url("/page/1"))
                    .itemSelector(".product").field("title", "h2.title")
                    .followLinks(false).maxPages(1).build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
                EngineResult first = engine.execute(ScrapeJob.builder().name("run1").site(site).build());
                EngineResult second = engine.execute(ScrapeJob.builder().name("run2").site(site).build());
                assertEquals(first.metrics().get("pages"), second.metrics().get("pages"));
                assertEquals(1L, second.metrics().get("pages"));
                assertEquals(1L, second.metrics().get("records"));
            }
        }
    }

    @Test
    void signatureWithTrailingNewlineIsAccepted() throws Exception {
        String secret = "webhook-secret";
        String payload = "{\"transactions\":[{\"id\":\"tx-1\",\"amount\":10.5,"
                + "\"created_at\":1700000000,\"status\":\"approved\"}]}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, new PaymentwallPayloadParser()
                .parse(payload, signature + "\n", secret).size());
    }

    @Test
    void emptyCsvWriterProducesEmptyFile() throws Exception {
        Path target = Files.createTempFile("empty", ".csv");
        new CsvWriter().write(List.of(), target.toString());
        assertEquals(0, Files.size(target));
        assertTrue(Files.exists(target));
    }

    private ScraperConfig config() throws Exception {
        Path file = Files.createTempFile("r10-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }
}
