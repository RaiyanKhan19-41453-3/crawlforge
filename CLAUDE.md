# CLAUDE.md — Project instructions for Claude Code

This file is read automatically by Claude Code. It documents how this repository was
built with AI assistance and how to work in it safely. See `docs/AI_ASSISTED_WORKFLOW.md`
for the full prompt library.

## What this project is

CrawlForge is a Java 21 + Groovy 4 web scraping engine. It separates fetching
(`http`), parsing (`parsers`), crawling (`crawler`), persistence (`persistence`),
orchestration (`orchestration`) and export (`export`). Never put HTTP calls in parsers
or parsing logic in crawlers.

## Ground rules

- All builds/tests run inside Docker. Never assume Maven is installed on the host.
  Use `./scripts/build.sh` (runs as the host user and keeps the Maven cache in `.mvn-cache/`).
- Java 21, Maven, package root `com.crawlforge.scraper`.
- Every new class needs unit tests in `src/test/java` mirroring its package.
- Prefer composition + interfaces over inheritance. Use abstract classes only for
  template methods (see `crawler/AbstractCrawler.java`).
- Always handle failure explicitly: never swallow exceptions, wrap them in the
  `ScraperException` hierarchy with an error code and context.
- Never log secrets, cookies, card numbers or proxy credentials.
- Selectors live in `config/sites/*.json`, never hard-coded in Java.
- Respect `robots.txt` and rate limits. Anti-bot code is for reliability on sites the
  client has authorised us to access, never for evading access controls.

## Commands

```bash
# Build + all tests (Docker only)
./scripts/build.sh

# Run the demo crawler against the built-in local fixture server
./scripts/run-docker.sh --demo --output /app/output/demo-records.csv
```

## Reviewing AI-generated code (mandatory checklist)

1. Does it compile and do all tests pass in Docker?
2. Are exceptions typed and propagated with context?
3. Is there a test that would fail if the logic were wrong?
4. Are credentials/secrets kept out of logs and source?
5. Does the change belong in the documented package, or is it coupling modules?
6. Are external calls bounded by timeouts, retries and a rate limiter?
