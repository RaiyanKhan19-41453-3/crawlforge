package com.crawlforge.scraper.crawler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.parsers.ParserRegistry;
import org.junit.jupiter.api.Test;

class CrawlerFactoryTest {

    @Test
    void selectsCrawlerBySourceTypeAndRecipe() throws Exception {
        ScraperConfig config = ScraperConfig.defaults();
        try (ResilientHttpClient http = new ResilientHttpClient(config)) {
            CrawlerFactory factory = new CrawlerFactory(config, http, new ParserRegistry(), "auto");

            assertInstanceOf(StaticCrawler.class, factory.create(SourceType.HTML));
            assertInstanceOf(StaticCrawler.class, factory.create(SourceType.JSON));

            SiteConfig jsSite = SiteConfig.builder().name("js").javascriptRequired(true).build();
            assertInstanceOf(HtmlUnitCrawler.class, factory.create(SourceType.HTML, jsSite));
            assertTrue(factory.pagination() != null);
        }
    }
}
