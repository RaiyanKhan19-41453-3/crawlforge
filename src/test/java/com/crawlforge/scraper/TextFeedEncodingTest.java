package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.CsvParser;
import com.crawlforge.scraper.parsers.JsonParser;
import com.crawlforge.scraper.parsers.XmlParser;
import org.junit.jupiter.api.Test;

class TextFeedEncodingTest {

    @Test
    void csvBomDoesNotBreakFieldMapping() {
        SiteConfig site = SiteConfig.builder().name("csv").sourceType(SourceType.CSV)
                .field("id", "id").field("title", "title").build();
        var records = new CsvParser().parse("\uFEFFid,title\nSKU-1,Widget\n", site);
        assertEquals(1, records.size());
        assertEquals("SKU-1", records.get(0).attributes().get("id"));
    }

    @Test
    void jsonBomIsAccepted() {
        SiteConfig site = SiteConfig.builder().name("json").sourceType(SourceType.JSON).build();
        assertEquals(1, new JsonParser().parse("\uFEFF{\"title\":\"Widget\"}", site).size());
    }

    @Test
    void xmlBomIsAccepted() {
        SiteConfig site = SiteConfig.builder().name("xml").sourceType(SourceType.XML)
                .itemSelector("//item").field("title", "title/text()").build();
        String xml = "\uFEFF<catalog><item><title>Widget</title></item></catalog>";
        assertEquals(1, new XmlParser().parse(xml, site).size());
    }

    @Test
    void cdataTextIsNotMangledByPrefixStripping() {
        SiteConfig site = SiteConfig.builder().name("xml").sourceType(SourceType.XML)
                .itemSelector("//item").field("note", "note/text()").build();
        String xml = "<catalog xmlns:x='urn:x'><x:item><note><![CDATA[Use <x:b> tag]]>"
                + "</note></x:item></catalog>";
        assertEquals("Use <x:b> tag", new XmlParser().parse(xml, site).get(0).attributes().get("note"));
    }

    @Test
    void jsonPriceKeepsDeclaredDecimals() {
        SiteConfig site = SiteConfig.builder().name("json").sourceType(SourceType.JSON)
                .field("price", "price").build();
        String payload = "{\"price\":100.00}";
        assertEquals("100.00", new JsonParser().parse(payload, site).get(0).attributes().get("price"));
    }
}
