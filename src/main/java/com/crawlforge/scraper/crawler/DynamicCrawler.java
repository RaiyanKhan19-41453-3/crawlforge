package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.parsers.ParserRegistry;
import com.crawlforge.scraper.stealth.BrowserFingerprint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Selenium crawler for JavaScript-rendered pages, with login, waits and failure screenshots. */
public final class DynamicCrawler extends AbstractCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicCrawler.class);
    private final ScraperConfig config;
    private final BrowserFingerprint fingerprint;
    private WebDriver driver;
    private boolean loggedIn;

    public DynamicCrawler(ScraperConfig config, ParserRegistry registry, PaginationHandler pagination) {
        this(config, registry, pagination, BrowserFingerprint.consistent());
    }

    public DynamicCrawler(ScraperConfig config, ParserRegistry registry, PaginationHandler pagination,
                          BrowserFingerprint fingerprint) {
        super(registry, pagination);
        this.config = config;
        this.fingerprint = fingerprint;
    }

    @Override
    public boolean supports(SourceType type) { return type == SourceType.DYNAMIC_HTML; }

    @Override
    protected void beforeCrawl(ScrapeJob job, CrawlContext context) {
        ChromeOptions options = new ChromeOptions();
        if (config.headless()) {
            options.addArguments(fingerprint.chromeArguments());
        } else {
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        }
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(config.timeoutMs()));
    }

    @Override
    protected String fetchPage(String url, ScrapeJob job) {
        SiteConfig site = job.site();
        if (site.requiresLogin() && !loggedIn) {
            performLogin(site);
            loggedIn = true;
        }
        driver.get(url);
        if (site.waitForSelector() != null && !site.waitForSelector().isBlank()) {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(site.waitForSelector())));
        }
        return driver.getPageSource();
    }

    private void performLogin(SiteConfig site) {
        driver.get(site.loginUrl());
        WebElement username = driver.findElement(By.cssSelector("input[name='username'], input[name='email'], #username"));
        WebElement password = driver.findElement(By.cssSelector("input[type='password']"));
        WebElement submit = driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']"));
        username.sendKeys(site.username());
        password.sendKeys(site.password());
        submit.click();
        LOG.info("Selenium login submitted for site '{}'", site.name());
    }

    @Override
    protected void onPageFailure(String url, Throwable cause) {
        if (!(driver instanceof TakesScreenshot shooter)) return;
        try {
            Path dir = Path.of(config.outputDir(), "screenshots");
            Files.createDirectories(dir);
            Path target = dir.resolve("failure-" + Instant.now().toEpochMilli() + ".png");
            Files.write(target, shooter.getScreenshotAs(OutputType.BYTES));
            LOG.warn("Saved failure screenshot for {} to {}", url, target);
        } catch (Exception e) {
            LOG.debug("Could not save screenshot for {}: {}", url, e.getMessage());
        }
    }

    @Override
    protected void afterCrawl(ScrapeJob job, CrawlContext context) {
        if (driver != null) driver.quit();
    }
}
