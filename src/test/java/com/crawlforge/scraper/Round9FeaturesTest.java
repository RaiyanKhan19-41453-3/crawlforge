package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.automation.CsvCleanupTool;
import com.crawlforge.scraper.core.config.SiteConfigLoader;
import com.crawlforge.scraper.core.exception.ConfigurationException;
import com.crawlforge.scraper.core.exception.ScraperException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.payment.CardDataMasker;
import com.crawlforge.scraper.payment.PaymentwallPayloadParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class Round9FeaturesTest {

    @Test
    void detectsModernMastercardRange() {
        assertEquals("MASTERCARD", CardDataMasker.brand("2223000048400011"));
    }

    @Test
    void scrapedFieldsCollidingWithMetadataArePreserved() {
        ScrapedRecord record = ScrapedRecord.builder().source("demo").url("http://demo/1")
                .attribute("source", "CLIENT-SOURCE")
                .attribute("quality_score", "client value").build();
        Map<String, String> map = record.toMap();
        assertEquals("demo", map.get("source"));
        assertEquals("CLIENT-SOURCE", map.get("attr_source"));
        assertEquals("client value", map.get("attr_quality_score"));
    }

    @Test
    void groovyCsvCleanupKeepsDuplicateHeaderColumns() {
        String cleaned = CsvCleanupTool.clean("id,name,name\n1,  A  ,  B  \n");
        assertTrue(cleaned.contains("1,A,B"), cleaned);
    }

    @Test
    void merchantPayloadRequiresPositiveAmountAndTimestamp() throws Exception {
        String secret = "secret";
        String payload = "{\"transactions\":[{\"id\":\"tx\",\"card_number\":\"4111111111111111\"}]}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        ScraperException error = assertThrows(ScraperException.class,
                () -> new PaymentwallPayloadParser().parse(payload, signature, secret));
        assertEquals("PAYLOAD_ERROR", error.errorCode());
    }

    @Test
    void duplicateRecipeNamesAreRejected() throws Exception {
        Path dir = Files.createTempDirectory("recipes");
        String recipe = "{\"name\":\"same\",\"sourceType\":\"HTML\",\"seedUrls\":[\"http://x/1\"]}";
        Files.writeString(dir.resolve("a.json"), recipe);
        Files.writeString(dir.resolve("b.json"), recipe);
        assertThrows(ConfigurationException.class, () -> new SiteConfigLoader().loadAll(dir));
    }
}
