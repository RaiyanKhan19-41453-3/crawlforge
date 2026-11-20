package com.crawlforge.scraper.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.model.SourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SiteConfigLoaderTest {

    @Test
    void loadsDeclarativeSiteRecipe() throws IOException {
        Path file = Files.createTempFile("site", ".json");
        Files.writeString(file, """
                {
                  "name": "demo",
                  "sourceType": "HTML",
                  "baseUrl": "http://demo.test",
                  "seedUrls": ["http://demo.test/page/1"],
                  "itemSelector": ".product",
                  "nextPageSelector": "a.next",
                  "fields": {"title": "h2", "price": ".price"},
                  "xpathFields": {"sku": "//span[@id='sku']"},
                  "maxPages": 3,
                  "maxDepth": 2,
                  "followLinks": true
                }
                """);
        SiteConfig config = new SiteConfigLoader().load(file);
        assertEquals("demo", config.name());
        assertEquals(SourceType.HTML, config.sourceType());
        assertEquals(List.of("http://demo.test/page/1"), config.seedUrls());
        assertEquals("h2", config.fields().get("title"));
        assertTrue(config.xpathFields().containsKey("sku"));
        assertEquals(3, config.maxPages());
    }
}
