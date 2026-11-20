# Capability Coverage Matrix

> Each capability below points to the exact place in this repository where it is
> implemented and proven by an automated test. Nothing is left as a claim on a CV.

## Core engineering

| Capability | Where it is demonstrated |
| --- | --- |
| Java and Groovy with real OOP applied to a business problem | `src/main/java/**` (builder, strategy and template-method patterns) and `src/main/groovy/com/crawlforge/scraper/automation/**` (QA, CSV cleanup, integration payload tools) |
| Collections, exception handling and multithreading | Immutable models in `core/model`, typed `ScraperException` hierarchy in `core/exception`, `WorkerPoolExecutor`, `RateLimiter` |
| Advanced regular expressions | `util/RegexPatterns.java`, `util/RegexUtils.java` (email, phone, price, SKU, Luhn check) - see `RegexUtilsTest` |
| Data extraction from HTML, JSON, XML, CSV and PDF | One parser per format in `parsers/`, selected by `ParserRegistry` - see `ParsersTest` |
| HTTP, CSS/DOM parsing, XPath and data structures | `http/ResilientHttpClient`, `parsers/HtmlParser`, `parsers/XPathSelector` (Jsoup DOM to W3C DOM to XPath), `crawler/PaginationHandler` |
| Data-quality assurance | `parsers/DataValidator.java`, per-record quality score, `docs/DATA_CONTRACT.md` |

## Browser automation

| Capability | Where it is demonstrated |
| --- | --- |
| Static and JavaScript-rendered pages | `crawler/StaticCrawler.java` (HttpClient + Jsoup), `crawler/DynamicCrawler.java` (Selenium), `crawler/HtmlUnitCrawler.java` |
| Playwright and emerging headless-browser tooling | `crawler/PlaywrightCrawler.java` plus the `-Pplaywright-runtime` Maven profile |
| Sessions, cookies, logins, pagination and rate limits | `http/CookieJar`, `http/LoginManager`, `crawler/PaginationHandler`, `http/RateLimiter` |
| Proxy rotation, CAPTCHA handling and browser fingerprinting | `http/ProxyRotator`, `stealth/CaptchaSolver`, `stealth/BrowserFingerprint`, `stealth/AntiBotDetector`, `core/config/RetryPolicy` |
| Failure diagnosis | Typed error codes, structured logs, `output/screenshots/` from Selenium, `docs/TROUBLESHOOTING.md` |

## DevOps and tooling

| Capability | Where it is demonstrated |
| --- | --- |
| Docker deployment on a VM, VDI or client platform | `Dockerfile`, `docker-compose.yml`, `scripts/run-docker.*`, fat jar via Maven Shade |
| Git and GitHub workflow | `.github/workflows/ci.yml`, issue templates, pull-request checklist, clean commit history |
| Agile task tracking | `docs/AGILE_WORKFLOW.md`, ticket-prefixed commits, acceptance criteria in issue templates |
| AI-assisted development | `CLAUDE.md`, `AGENTS.md`, `docs/AI_ASSISTED_WORKFLOW.md` with real prompts and a mandatory review checklist |
| IntelliJ IDEA and Visual Studio Code | `.idea/`, `.vscode/` (tasks, launch config, recommended extensions) |
| Ubuntu/Linux and Windows | Docker-only build scripts: `scripts/*.sh` and `scripts/*.bat` |

## System integration and client domain

| Capability | Where it is demonstrated |
| --- | --- |
| SQL with PostgreSQL, MySQL and H2 | `sql/schema.sql`, `sql/schema-mysql.sql`, `persistence/RecordRepository.java`, H2-backed integration test |
| Merchant/payment API integration | `payment/PaymentwallPayloadParser.java` (HMAC-SHA256 verification), `payment/CardDataMasker.java`, `payment/MerchantTransaction.java` |
| BPO / data-entry industry data challenges | `docs/CLIENT_DOMAIN_NOTES.md` (accuracy SLAs, auditability, high-volume feeds, PII/PCI handling) |
| Client-ready output formats | `export/CsvWriter`, `JsonWriter`, `ExcelWriter` and the Groovy `IntegrationPayloadBuilder` |
| Maintainability and knowledge sharing | `docs/ARCHITECTURE.md`, Javadoc, declarative site recipes in `config/sites`, 113 automated tests |

## Verification

```bash
./scripts/build.sh       # 113 tests, all inside Docker
docker compose up --build # offline end-to-end demo, writes output/demo-records.csv
```
