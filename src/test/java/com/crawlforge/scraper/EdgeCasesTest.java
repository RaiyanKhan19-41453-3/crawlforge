package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.config.SiteConfigLoader;
import com.crawlforge.scraper.core.exception.ConfigurationException;
import com.crawlforge.scraper.core.exception.ScraperException;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.CsvParser;
import com.crawlforge.scraper.parsers.DataNormalizer;
import com.crawlforge.scraper.parsers.HtmlParser;
import com.crawlforge.scraper.parsers.JsonParser;
import com.crawlforge.scraper.parsers.XmlParser;
import com.crawlforge.scraper.payment.PaymentwallPayloadParser;
import com.crawlforge.scraper.util.RegexUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Regression tests for unusual but realistic client data. */
class EdgeCasesTest {

    @Test
    void rejectsNonArrayTransactionPayload() throws Exception {
        String secret = "secret";
        String payload = "{\"transactions\":{\"id\":\"1\",\"amount\":10}}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        ScraperException error = assertThrows(ScraperException.class,
                () -> new PaymentwallPayloadParser().parse(payload, signature, secret));
        assertEquals("PAYLOAD_ERROR", error.errorCode());
    }

    @Test
    void priceRegexHandlesLakhGrouping() {
        assertEquals("1,20,000", RegexUtils.extractPrice("Tk 1,20,000 only"));
        assertEquals("12,34,567.89", RegexUtils.extractPrice("BDT 12,34,567.89"));
        assertEquals("1,250.00", RegexUtils.extractPrice("BDT 1,250.00"));
    }

    @Test
    void phoneRegexHandlesFormattedBangladeshiNumbers() {
        assertEquals("01712345678", RegexUtils.normalizePhone("01712-345678"));
        assertEquals("01712345678", RegexUtils.normalizePhone("01712 345678"));
        assertEquals("01712345678", RegexUtils.normalizePhone("+88 01712 345678"));
    }

    @Test
    void xmlParserHandlesPrefixedNamespaces() {
        SiteConfig site = SiteConfig.builder().name("ns").sourceType(SourceType.XML)
                .itemSelector("//item").field("title", "title/text()").build();
        String xml = "<catalog xmlns:x='urn:x'><x:item><x:title>Widget A</x:title></x:item></catalog>";
        var records = new XmlParser().parse(xml, site);
        assertEquals(1, records.size());
        assertEquals("Widget A", records.get(0).attributes().get("title"));
    }

    @Test
    void jsonPathSupportsArrayIndex() {
        SiteConfig site = SiteConfig.builder().name("json").sourceType(SourceType.JSON)
                .jsonResultPath("$.items[0]").field("sku", "id").build();
        var records = new JsonParser().parse("{\"items\":[{\"id\":\"A\"},{\"id\":\"B\"}]}", site);
        assertEquals(1, records.size());
        assertEquals("A", records.get(0).attributes().get("sku"));
    }

    @Test
    void truncationDoesNotSplitSurrogatePairs() {
        String result = DataNormalizer.truncate("AB\uD83D\uDE00CD", 4);
        assertEquals(4, result.codePointCount(0, result.length()));
    }

    @Test
    void configLoaderStripsUtf8Bom() throws Exception {
        Path file = Files.createTempFile("bom-config", ".properties");
        Files.writeString(file, "\uFEFFscraper.maxThreads=9\n", StandardCharsets.UTF_8);
        assertEquals(9, ScraperConfig.load(file).maxThreads());
    }

    @Test
    void nextPageFragmentsAreNormalisedAway() {
        SiteConfig site = SiteConfig.builder().name("frag").sourceType(SourceType.HTML)
                .baseUrl("http://shop.test").nextPageSelector("a.next").build();
        List<String> links = new HtmlParser().links("<a class='next' href='/page/2#results'>Next</a>", site);
        assertTrue(links.contains("http://shop.test/page/2"));
    }

    @Test
    void headerlessCsvUsesColumnIndexes() {
        SiteConfig site = SiteConfig.builder().name("plain").sourceType(SourceType.CSV)
                .csvHasHeader(false).field("id", "0").field("title", "1").build();
        var records = new CsvParser().parse("1,Alpha\n2,Beta\n", site);
        assertEquals(2, records.size());
        assertEquals("Beta", records.get(1).attributes().get("title"));
    }

    @Test
    void nonObjectSiteRecipeIsRejected() throws Exception {
        Path file = Files.createTempFile("bad-site", ".json");
        Files.writeString(file, "[1,2,3]");
        assertThrows(ConfigurationException.class, () -> new SiteConfigLoader().load(file));
    }
}
