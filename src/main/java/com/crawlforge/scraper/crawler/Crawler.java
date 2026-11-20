package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.util.List;

/** Traversal strategy implemented for each browser/HTTP engine. */
public interface Crawler {

    boolean supports(SourceType sourceType);

    List<ScrapedRecord> crawl(ScrapeJob job, CrawlContext context);
}
