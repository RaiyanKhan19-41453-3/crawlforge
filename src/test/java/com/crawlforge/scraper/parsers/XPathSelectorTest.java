package com.crawlforge.scraper.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class XPathSelectorTest {

    @Test
    void evaluatesXpathAgainstJsoupDom() {
        var element = Jsoup.parse("<div><span id='sku'>SKU-42</span><span class='p'>10</span></div>")
                .selectFirst("div");
        List<String> values = XPathSelector.select(element, "//span[@id='sku']/text()");
        assertEquals(List.of("SKU-42"), values);
    }

    @Test
    void evaluatesRelativeXpathAgainstElement() {
        var element = Jsoup.parse("<div><span id='sku'>SKU-42</span></div>").selectFirst("div");
        assertEquals(java.util.List.of("SKU-42"),
                XPathSelector.select(element, "span[@id='sku']/text()"));
    }

    @Test
    void evaluatesXpathAgainstXmlString() {
        List<String> values = XPathSelector.selectXml(
                "<root><item><title>A</title></item></root>", "//item/title/text()");
        assertTrue(values.contains("A"));
    }
}
