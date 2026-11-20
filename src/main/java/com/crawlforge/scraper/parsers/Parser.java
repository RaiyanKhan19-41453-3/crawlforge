package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.util.List;

/** Strategy interface implemented by one parser per source format. */
public interface Parser {

    boolean supports(SourceType sourceType);

    /** @param raw source text (PDF bytes are transported as ISO-8859-1 text) */
    List<ScrapedRecord> parse(String raw, SiteConfig site) throws ParseException;

    /** Default no-op hook for parsers that also discover links. */
    default List<String> links(String raw, SiteConfig site) { return List.of(); }
}
