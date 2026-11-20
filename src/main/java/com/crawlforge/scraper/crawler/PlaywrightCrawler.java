package com.crawlforge.scraper.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.stealth.BrowserFingerprint;

/** Emerging headless-browser automation with Playwright (optional runtime dependency). */
public final class PlaywrightCrawler extends AbstractCrawler {

    private final ScraperConfig config;
    private final BrowserFingerprint fingerprint;
    private Playwright playwright;
    private Browser browser;

    public PlaywrightCrawler(ScraperConfig config, ParserRegistry registry, PaginationHandler pagination) {
        this(config, registry, pagination, BrowserFingerprint.consistent());
    }

    public PlaywrightCrawler(ScraperConfig config, ParserRegistry registry, PaginationHandler pagination,
                             BrowserFingerprint fingerprint) {
        super(registry, pagination);
        this.config = config;
        this.fingerprint = fingerprint;
    }

    @Override
    public boolean supports(SourceType type) { return type == SourceType.DYNAMIC_HTML; }

    @Override
    protected void beforeCrawl(ScrapeJob job, CrawlContext context) {
        playwright = Playwright.create();
        java.util.List<String> args = config.headless()
                ? fingerprint.chromeArguments()
                : java.util.List.of("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(config.headless())
                .setArgs(args));
    }

    @Override
    protected String fetchPage(String url, ScrapeJob job) {
        try (Page page = browser.newPage()) {
            page.setDefaultTimeout(config.timeoutMs());
            page.navigate(url);
            String selector = job.site().waitForSelector();
            if (selector != null && !selector.isBlank()) page.waitForSelector(selector);
            return page.content();
        }
    }

    @Override
    protected void afterCrawl(ScrapeJob job, CrawlContext context) {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
