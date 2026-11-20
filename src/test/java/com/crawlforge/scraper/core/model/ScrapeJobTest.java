package com.crawlforge.scraper.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crawlforge.scraper.core.config.SiteConfig;
import org.junit.jupiter.api.Test;

class ScrapeJobTest {

    private SiteConfig site() {
        return SiteConfig.builder()
                .name("limits-test")
                .seedUrl("http://example.test/start")
                .maxPages(7)
                .maxDepth(2)
                .build();
    }

    @Test
    void jobInheritsLimitsFromSiteRecipe() {
        ScrapeJob job = ScrapeJob.builder().site(site()).build();
        assertEquals(7, job.maxPages());
        assertEquals(2, job.maxDepth());
    }

    @Test
    void explicitLimitsOverrideSiteRecipe() {
        ScrapeJob job = ScrapeJob.builder().site(site()).maxPages(1).build();
        assertEquals(1, job.maxPages());
        assertEquals(2, job.maxDepth());
    }
}
