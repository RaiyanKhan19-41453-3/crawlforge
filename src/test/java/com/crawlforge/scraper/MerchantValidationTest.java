package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ScraperException;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.parsers.DataValidator;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.payment.PaymentwallPayloadParser;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MerchantValidationTest {

    @Test
    void nonNumericTimestampIsRejected() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"amount\":10,"
                + "\"created_at\":\"not-a-date\",\"status\":\"ok\"}]}";
        assertEquals("PAYLOAD_ERROR", parseError(payload).errorCode());
    }

    @Test
    void nonIntegralTimestampIsRejectedWithTypedError() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"amount\":10,"
                + "\"created_at\":1e30,\"status\":\"ok\"}]}";
        assertEquals("PAYLOAD_ERROR", parseError(payload).errorCode());
    }

    @Test
    void infiniteAmountIsRejected() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"amount\":1e309,"
                + "\"created_at\":1700000000,\"status\":\"ok\"}]}";
        assertEquals("PAYLOAD_ERROR", parseError(payload).errorCode());
    }

    @Test
    void emptyCurrencyOrStatusIsRejected() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"amount\":10,"
                + "\"created_at\":1700000000,\"currency\":\"\",\"status\":\"\"}]}";
        assertEquals("PAYLOAD_ERROR", parseError(payload).errorCode());
    }

    @Test
    void amountKeepsFullPrecision() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"amount\":9999999999999999999,"
                + "\"created_at\":1700000000,\"status\":\"ok\"}]}";
        var parsed = new PaymentwallPayloadParser().parse(payload, sign(payload), "secret");
        assertEquals(new BigDecimal("9999999999999999999"), parsed.get(0).amount());
    }

    @Test
    void qualityIsRecomputedAfterUrlRestoration() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/item.json", exchange -> LocalServer.respond(exchange, 200,
                    "{\"content\":\"AB\",\"title\":\"\"}"));
            ScraperConfig config = config();
            SiteConfig site = SiteConfig.builder().name("json-no-base").sourceType(SourceType.JSON)
                    .seedUrl(server.url("/item.json"))
                    .field("content", "content").field("title", "title")
                    .followLinks(false).maxPages(1).build();
            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                EngineResult result = engine(config, http).execute(
                        ScrapeJob.builder().name("quality-test").site(site).build());
                assertEquals(1, result.records().size());
            }
        }
    }

    @Test
    void fullyEmptyRecordsAreRejected() {
        assertEquals(0.0, DataValidator.score("http://x/1", "", Map.of("a", "")));
    }

    private ScraperException parseError(String payload) throws Exception {
        return assertThrows(ScraperException.class,
                () -> new PaymentwallPayloadParser().parse(payload, sign(payload), "secret"));
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private ScraperConfig config() throws Exception {
        Path file = Files.createTempFile("r13-config", ".properties");
        Files.writeString(file, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=1000\n");
        return ScraperConfig.load(file);
    }

    private ScrapeEngine engine(ScraperConfig config, ResilientHttpClient http) {
        return new ScrapeEngine(config, new CrawlerFactory(config, http, new ParserRegistry(), "auto"));
    }
}
