package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.parsers.ParserRegistry;

/** Chooses the right crawler for a source type and runtime configuration. */
public final class CrawlerFactory {

    private final ScraperConfig config;
    private final ResilientHttpClient http;
    private final ParserRegistry registry;
    private final PaginationHandler pagination;

    public CrawlerFactory(ScraperConfig config, ResilientHttpClient http, ParserRegistry registry) {
        this.config = config;
        this.http = http;
        this.registry = registry;
        this.pagination = new PaginationHandler(registry);
    }

    public Crawler create(SourceType type) {
        return create(type, null);
    }

    /** Site-aware selection: a recipe requesting JavaScript never uses the static crawler. */
    public Crawler create(SourceType type, com.crawlforge.scraper.core.config.SiteConfig site) {
        String engine = System.getProperty("crawler.engine", "auto");
        boolean needsJs = site != null && site.javascriptRequired();
        if (type == SourceType.HTML && !needsJs) return new StaticCrawler(http, registry, pagination);
        if (needsJs && type != SourceType.DYNAMIC_HTML) {
            type = SourceType.DYNAMIC_HTML;
        }
        if (type == SourceType.DYNAMIC_HTML) {
            if ("playwright".equalsIgnoreCase(engine)) return new PlaywrightCrawler(config, registry, pagination);
            if ("selenium".equalsIgnoreCase(engine) || config.javascriptEnabled()) {
                return new DynamicCrawler(config, registry, pagination);
            }
            return new HtmlUnitCrawler(config, registry, pagination);
        }
        // JSON / XML / CSV / PDF are plain HTTP fetches parsed by the matching parser.
        return new StaticCrawler(http, registry, pagination);
    }

    public PaginationHandler pagination() { return pagination; }
}
