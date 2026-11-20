package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.http.HttpResponse;
import com.crawlforge.scraper.http.LoginManager;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.parsers.ParserRegistry;
import java.util.List;

/** Fast crawler for server-rendered pages: Apache HttpClient + Jsoup. */
public final class StaticCrawler extends AbstractCrawler {

    private final ResilientHttpClient http;
    private final LoginManager loginManager;

    public StaticCrawler(ResilientHttpClient http, ParserRegistry registry,
                         PaginationHandler pagination) {
        super(registry, pagination);
        this.http = http;
        this.loginManager = new LoginManager(http);
    }

    @Override
    public boolean supports(SourceType type) { return type == SourceType.HTML; }

    @Override
    protected void beforeCrawl(ScrapeJob job, CrawlContext context) {
        loginManager.loginIfNeeded(job.site());
    }

    @Override
    protected String fetchPage(String url, ScrapeJob job) {
        HttpResponse response = http.fetch(url);
        // Parse links against the URL actually requested (may differ after redirects).
        return response.body();
    }

    @Override
    protected String fetchRaw(String url, ScrapeJob job) {
        if (job.site().sourceType() == com.crawlforge.scraper.core.model.SourceType.PDF) {
            // ISO-8859-1 is a lossless byte-to-char transport for the PDFBox parser.
            return new String(http.fetchBytes(url).body(), java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        return super.fetchRaw(url, job);
    }

    @Override
    protected List<String> discoverLinks(String raw, ScrapeJob job, String pageUrl) {
        return pagination.discover(raw, job.site(), pageUrl);
    }
}
