# Architecture

CrawlForge is deliberately layered. A change to a website should touch a JSON recipe; a change
to a browser engine should touch one crawler class; a change to output format should touch one
writer. This separation is what makes the code maintainable and easy for a team to share.

## Layers

| Package | Responsibility | Key types |
| --- | --- | --- |
| `core.model` | Immutable domain objects | `ScrapedRecord`, `ScrapeJob`, `SourceType`, `JobStatus` |
| `core.config` | Configuration and declarative site recipes | `ScraperConfig`, `SiteConfig`, `SiteConfigLoader`, `RetryPolicy` |
| `core.exception` | Typed failures with stable error codes | `ScraperException` + 9 subclasses |
| `http` | All network I/O, sessions, rate limiting, rotation | `ResilientHttpClient`, `CookieJar`, `LoginManager`, `RateLimiter`, `ProxyRotator` |
| `stealth` | Anti-bot classification and browser profile | `AntiBotDetector`, `CaptchaSolver`, `BrowserFingerprint` |
| `parsers` | Format-specific extraction and data cleaning | `HtmlParser`, `JsonParser`, `XmlParser`, `CsvParser`, `PdfParser`, `XPathSelector`, `DataValidator` |
| `crawler` | Traversal, pagination and engine selection | `AbstractCrawler`, `StaticCrawler`, `DynamicCrawler`, `HtmlUnitCrawler`, `PlaywrightCrawler`, `CrawlerFactory` |
| `export` | Output strategies | `CsvWriter`, `JsonWriter`, `ExcelWriter`, `WriterFactory` |
| `persistence` | SQL storage and quality reporting | `DatabaseManager`, `RecordRepository` |
| `orchestration` | Parallel execution and metrics | `ScrapeEngine`, `WorkerPoolExecutor`, `JobScheduler`, `MetricsCollector` |
| `observability` | Health reporting | `HealthIndicator`, `HealthReport` |
| `payment` | Client-domain integration (merchant/payments) | `PaymentwallPayloadParser`, `CardDataMasker` |
| `demo` | Offline fixture website | `DemoServer` |

## Patterns used (real OOP, not tokenism)

- **Template method** — `AbstractCrawler.crawl()` defines the pipeline; subclasses only fetch.
- **Strategy** — `Parser`, `Crawler`, `DataWriter` and `CaptchaSolver` are selected by a
  registry/factory; each crawler declares the source types it supports via `supports()`.
- **Factory / registry** — `CrawlerFactory`, `ParserRegistry`, `WriterFactory`.
- **Builder** — `ScrapedRecord`, `ScrapeJob`, `SiteConfig`.
- **Immutable value objects** — records and configs use defensive copies.

## Data flow

1. CLI builds `ScraperConfig` and a `SiteConfig` (JSON recipe).
2. `ScrapeEngine` splits seed URLs across a named daemon thread pool.
3. `CrawlerFactory` selects an engine; `AbstractCrawler` loops over the frontier.
4. `ResilientHttpClient` applies rate limit, proxy/UA rotation, cookies, stealth headers,
   retries with backoff and anti-bot detection.
5. `ParserRegistry` dispatches raw content to the right parser; `DataValidator` filters rows.
6. `PaginationHandler` resolves and de-duplicates next-page links.
7. Records are written to CSV / JSON / Excel and optionally batched into SQL.
8. `MetricsCollector` and `HealthIndicator` produce the run report.

## Extension points

- New site: add `config/sites/<name>.json` — no code change.
- New format: implement `Parser` and register it in `ParserRegistry`.
- New browser engine: extend `AbstractCrawler` and add a case to `CrawlerFactory`.
- New output: implement `DataWriter` and add an extension to `WriterFactory`.
- New CAPTCHA vendor: implement `CaptchaSolver` and pass it to `ResilientHttpClient`.

## Dependency choices

- **Jsoup** for fast CSS-selector parsing; **W3C DOM + XPath** for real XPath support on both
  HTML and XML (so XPath knowledge is demonstrated, not hidden behind a library).
- **HttpClient 5** with a pooled connection manager; automatic retries disabled because
  `ResilientHttpClient` owns the retry/backoff policy. It tracks final URLs after redirects,
  decodes text using `Content-Type` or an HTML `<meta charset>`, caps response bodies at
  50 MB, supports HTTP Basic credentials, and skips paths disallowed by `robots.txt` (the
  common `User-agent: *` / `Disallow` rules).
- **PDFBox 3**, **Apache POI**, **Commons CSV**, **Jackson** for the non-HTML formats.
- **H2** only in test scope so SQL code is verified in CI without a database service.
- **Playwright driver-bundle** excluded from the default jar; enable with `-Pplaywright-runtime`
  to keep the runtime image small while still showing the integration.
