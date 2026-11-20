package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.ParserRegistry;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;

/** Lightweight JS-capable crawler using HtmlUnit (no browser binary required). */
public final class HtmlUnitCrawler extends AbstractCrawler {

    private final ScraperConfig config;
    private WebClient webClient;

    public HtmlUnitCrawler(ScraperConfig config, ParserRegistry registry, PaginationHandler pagination) {
        super(registry, pagination);
        this.config = config;
    }

    @Override
    public boolean supports(SourceType type) { return type == SourceType.DYNAMIC_HTML; }

    @Override
    protected void beforeCrawl(ScrapeJob job, CrawlContext context) {
        webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setTimeout(config.timeoutMs());
    }

    @Override
    protected String fetchPage(String url, ScrapeJob job) {
        try {
            HtmlPage page = webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(2_000);
            return page.asXml();
        } catch (java.io.IOException e) {
            throw new com.crawlforge.scraper.core.exception.FetchException("HtmlUnit fetch failed: " + url, e);
        }
    }

    @Override
    protected void afterCrawl(ScrapeJob job, CrawlContext context) {
        if (webClient != null) webClient.close();
    }
}
