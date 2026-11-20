package com.crawlforge.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.CsvParser;
import com.crawlforge.scraper.parsers.DataNormalizer;
import com.crawlforge.scraper.parsers.HtmlParser;
import com.crawlforge.scraper.parsers.XmlParser;
import org.junit.jupiter.api.Test;

class ParserFidelityTest {

    @Test
    void absoluteXpathsAreScopedToEachXmlItem() {
        SiteConfig site = SiteConfig.builder().name("xml").sourceType(SourceType.XML)
                .itemSelector("//item")
                .field("title", "//title/text()")
                .field("price", "//price/text()").build();
        String xml = "<catalog><item><title>Alpha</title><price>10</price></item>"
                + "<item><title>Beta</title><price>20</price></item></catalog>";
        var records = new XmlParser().parse(xml, site);
        assertEquals(2, records.size());
        assertEquals("Alpha", records.get(0).attributes().get("title"));
        assertEquals("Beta", records.get(1).attributes().get("title"));
        assertEquals("20", records.get(1).attributes().get("price"));
    }

    @Test
    void htmlUsesMappedUrlField() {
        SiteConfig site = SiteConfig.builder().name("html").sourceType(SourceType.HTML)
                .baseUrl("http://shop.test/list").itemSelector(".product")
                .xpathField("url", ".//a/@href").build();
        String html = "<div class='product'><h2>A</h2><a href='/items/1'>open</a></div>"
                + "<div class='product'><h2>B</h2><a href='/items/2'>open</a></div>";
        var records = new HtmlParser().parse(html, site);
        assertEquals(2, records.size());
        assertEquals("http://shop.test/items/1", records.get(0).url());
        assertEquals("http://shop.test/items/2", records.get(1).url());
    }

    @Test
    void csvDuplicateHeadersKeepEveryColumn() {
        SiteConfig site = SiteConfig.builder().name("csv").sourceType(SourceType.CSV).build();
        var records = new CsvParser().parse("id,title,title\n1,First,Second\n", site);
        assertEquals("First", records.get(0).attributes().get("title"));
        assertEquals("Second", records.get(0).attributes().get("title_2"));
    }

    @Test
    void unicodeSeparatorsAreNormalised() {
        assertEquals("A B", DataNormalizer.normalize("A\u2028B"));
        assertEquals("A B", DataNormalizer.normalize("A\u200BB"));
    }
}
