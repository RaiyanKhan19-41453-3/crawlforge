package com.crawlforge.scraper.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.parsers.ParserRegistry;
import org.junit.jupiter.api.Test;

class PaginationHandlerTest {

    private final PaginationHandler handler = new PaginationHandler(new ParserRegistry());

    @Test
    void resolvesRelativeLinks() {
        assertEquals("http://shop.test/page/2",
                handler.absolutize("http://shop.test/page/1", "/page/2"));
    }

    @Test
    void keepsCrawlOnTheConfiguredHost() {
        SiteConfig site = SiteConfig.builder().name("shop").baseUrl("http://shop.test").build();
        assertTrue(handler.allowed("http://shop.test/page/2", site));
        assertFalse(handler.allowed("http://other.test/page/2", site));
    }

    @Test
    void discoversNextPageFromHtml() {
        SiteConfig site = SiteConfig.builder().name("shop").baseUrl("http://shop.test")
                .nextPageSelector("a.next").followLinks(false).build();
        String html = "<a class='next' href='/page/2'>Next</a>";
        assertTrue(handler.discover(html, site, "http://shop.test/page/1")
                .contains("http://shop.test/page/2"));
    }

    @Test
    void resolvesRelativeLinksAgainstCurrentPage() {
        SiteConfig site = SiteConfig.builder().name("shop").baseUrl("http://shop.test")
                .nextPageSelector("a.next").followLinks(false).build();
        String html = "<a class='next' href='product/2'>Next</a>";
        assertTrue(handler.discover(html, site, "http://shop.test/page/1")
                .contains("http://shop.test/page/product/2"));
    }
}
