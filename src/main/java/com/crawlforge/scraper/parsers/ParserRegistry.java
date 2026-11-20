package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Factory/registry: the crawler never needs to know which parser handles a format. */
public final class ParserRegistry {

    private final Map<SourceType, Parser> parsers = new LinkedHashMap<>();

    public ParserRegistry() {
        register(new HtmlParser());
        register(new JsonParser());
        register(new XmlParser());
        register(new CsvParser());
        register(new PdfParser());
    }

    public void register(Parser parser) {
        for (SourceType type : SourceType.values()) {
            if (parser.supports(type)) parsers.put(type, parser);
        }
    }

    public Parser parserFor(SourceType type) {
        Parser parser = parsers.get(type);
        if (parser == null) throw new ParseException("No parser registered for " + type);
        return parser;
    }

    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        SourceType type = site.sourceType();
        SourceType effective = type == SourceType.DYNAMIC_HTML ? SourceType.HTML : type;
        return parserFor(effective).parse(raw, site);
    }

    public List<String> links(String raw, SiteConfig site) {
        return links(raw, site, site.baseUrl());
    }

    public List<String> links(String raw, SiteConfig site, String pageUrl) {
        SourceType type = site.sourceType();
        SourceType effective = type == SourceType.DYNAMIC_HTML ? SourceType.HTML : type;
        return parserFor(effective).links(raw, site, pageUrl);
    }
}
