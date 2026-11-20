# CrawlForge

### Enterprise Java / Groovy Web Scraping & Data Extraction Engine

> A portfolio project demonstrating an end-to-end web scraping and data extraction
> engine: Java, Groovy, browser automation, anti-bot handling, data quality, SQL,
> Docker deployment and AI-assisted development.
> See [`REQUIREMENTS_MATRIX.md`](REQUIREMENTS_MATRIX.md) for a capability-to-code mapping.

[![CI](https://github.com/example/crawlforge/actions/workflows/ci.yml/badge.svg)](.github/workflows/ci.yml)
Java 21 · Groovy 4 · Maven · Docker · Selenium · HtmlUnit · Playwright · Jsoup · Apache HttpClient 5 · PDFBox · Apache POI · PostgreSQL / MySQL / H2

---

## Quick start (Docker only — nothing installed locally)

```bash
# 1. Build + run every test inside a container
./scripts/build.sh

# 2. Build the runtime image
docker build -t crawlforge .

# 3. Run the offline end-to-end demo (built-in fixture website)
docker compose up --build
# results land in ./output/demo-records.csv
```

Or run the reusable commands directly:

```bash
# Build & test
docker run --rm -v "$PWD":/workspace -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -B clean verify

# Run the demo against the fat jar
docker run --rm -v "$PWD/output":/app/output -w /app \
  eclipse-temurin:21-jre java -jar target/crawlforge.jar \
  --demo --output /app/output/demo-records.csv
```

Windows users: `scripts\build.bat` and `scripts\run-docker.bat` do the same thing.

---

## What this project proves

| Area | Evidence |
| --- | --- |
| **Java + OOP + collections + multithreading** | Builder/Strategy/Template-method patterns, immutable models, `WorkerPoolExecutor`, `RateLimiter` |
| **Groovy automation** | `src/main/groovy/com/crawlforge/scraper/automation/` — QA report, CSV cleanup, client payload builder |
| **Exceptions + advanced regex** | Typed `ScraperException` hierarchy with error codes; `RegexPatterns`, `RegexUtils`, Luhn check |
| **Static + dynamic scraping** | `StaticCrawler` (HttpClient + Jsoup), `DynamicCrawler` (Selenium), `HtmlUnitCrawler`, `PlaywrightCrawler` |
| **All data formats** | HTML, JSON, XML (XPath), CSV and PDF parsers behind one `ParserRegistry` |
| **Anti-bot / nice-to-haves** | Proxy + User-Agent rotation, CAPTCHA strategy, browser fingerprinting, rate limiting, retries |
| **SQL + system integration** | JDBC repository, PostgreSQL/MySQL/H2 schemas, HMAC-signed merchant webhook parser |
| **AI-driven development** | `CLAUDE.md`, `AGENTS.md`, real prompt library in `docs/AI_ASSISTED_WORKFLOW.md` |
| **DevOps** | Dockerfile, docker-compose, GitHub Actions CI, JIRA/PR/issue templates |
| **Cross-platform / IDEs** | `.vscode/`, `.idea/`, `scripts/*.sh` and `scripts/*.bat` |
| **Data quality** | Per-record quality score, validator gate, 20+ automated tests |

---

## Architecture at a glance

```
                    ┌──────────────────────┐
                    │  CrawlForgeApplication│  CLI (Picocli) - Docker entrypoint
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │     ScrapeEngine     │  parallel workers + metrics
                    └──────────┬───────────┘
             ┌─────────────────┼──────────────────┐
             ▼                 ▼                  ▼
      ┌─────────────┐   ┌──────────────┐   ┌──────────────┐
      │  Crawlers   │   │   Parsers    │   │   Exporters  │
      │ static/Sel. │──▶│ HTML/JSON/   │──▶│ CSV/JSON/    │
      │ HtmlUnit/   │   │ XML/CSV/PDF  │   │ Excel/SQL    │
      │ Playwright  │   └──────────────┘   └──────────────┘
      └──────┬──────┘
             ▼
   ┌──────────────────────────────────────┐
   │ HTTP + stealth layer                 │
   │ pooling · cookies · login · proxy    │
   │ rotation · UA rotation · rate limit  │
   │ retries · CAPTCHA · fingerprint      │
   └──────────────────────────────────────┘
```

Full details: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Example output

```
Records: 5
Elapsed: 412 ms
Metrics: {pages=2, records=5, failures=0, retries=0, captchas=0, blocked=0}
Output:  output/demo-records.csv
Health:  OK
```

The demo crawls two pages of `DemoServer`, follows the `a.next` link, extracts three fields
per product, scores data quality and writes CSV — all offline.

---

## Repository layout

```
config/            runtime properties + declarative site recipes (JSON)
sql/               PostgreSQL / H2 and MySQL schemas
src/main/java/     core, http, stealth, parsers, crawler, export,
                   persistence, orchestration, observability, payment, demo
src/main/groovy/   automation scripts (QA, cleanup, integration)
src/test/          JUnit 5 tests (parsers, HTTP, anti-bot, SQL, Groovy, end-to-end)
docs/              architecture, Agile, AI workflow, domain notes, runbooks
.github/           CI workflow, issue templates, PR checklist
scripts/           Docker-only build/run wrappers for Linux and Windows
```

---

## Notes for the reviewer

- No secrets or client credentials are committed. `.env.example` shows the pattern.
- Anti-bot code is intended for reliability on **authorised** client sites; it respects
  rate limits and never bypasses access controls or CAPTCHAs fraudulently.
- The built-in demo server exists so the full pipeline can be verified without a live site.
- Every claim in this README is backed by code and an automated test.

**Your name / degree details:** update `README.md` and `docs/` with your own education and
contact information before submitting.
