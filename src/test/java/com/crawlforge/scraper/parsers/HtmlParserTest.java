package com.crawlforge.scraper.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlParserTest {

    private static final String HTML = """
            <html><body>
              <a class="next" href="/page/2">Next</a>
              <div class="product"><h2 class="title">Widget A</h2>
                <span class="price">BDT 1,250.00</span><span id="sku">SKU-1001</span></div>
              <div class="product"><h2 class="title">Widget B</h2>
                <span class="price">BDT 899.00</span><span id="sku">SKU-1002</span></div>
            </body></html>
            """;

    private SiteConfig site() {
        return SiteConfig.builder()
                .name("test-shop")
                .sourceType(SourceType.HTML)
                .baseUrl("http://shop.test")
                .itemSelector(".product")
                .nextPageSelector("a.next")
                .field("title", "h2.title")
                .field("price", ".price")
                .xpathField("sku", "//span[@id='sku']")
                .build();
    }

    @Test
    void extractsCssFieldsAndXpathFields() {
        List<ScrapedRecord> records = new HtmlParser().parse(HTML, site());
        assertEquals(2, records.size());
        assertEquals("Widget A", records.get(0).attributes().get("title"));
        assertEquals("SKU-1001", records.get(0).attributes().get("sku"));
        assertTrue(records.get(0).isValid());
    }

    @Test
    void discoversNextPageLink() {
        List<String> links = new HtmlParser().links(HTML, site());
        assertTrue(links.contains("http://shop.test/page/2"));
    }
}
