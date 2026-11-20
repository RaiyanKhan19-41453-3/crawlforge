# Troubleshooting / production runbook

Each error code is emitted by the `ScraperException` hierarchy and logged with the URL.

| Error code | Symptom | Likely root cause | Fix |
| --- | --- | --- | --- |
| `FETCH_ERROR` | HTTP 4xx/5xx after retries | Site down, changed URL, auth required | Check URL in recipe, credentials, then `LoginManager` |
| `RETRY_EXHAUSTED` | Page consistently fails | Permanent block or wrong selector URL | Inspect logs for the underlying exception; verify with curl inside a container |
| `ANTI_BOT` | Bot-challenge text in body | Rate too high or fingerprint mismatch | Reduce `scraper.rate.requestsPerSecond`, enable proxy rotation |
| `CAPTCHA_ERROR` (subclass of `ANTI_BOT`) | CAPTCHA marker text | Challenge page served | `CaptchaSolver.QUEUE` records it for review; do not attempt fraudulent bypass |
| `RATE_LIMIT` | HTTP 429 | Too many requests | Lower rate, spread across proxies, back off |
| `PARSE_ERROR` | Zero records | Selector or JSON path changed | Update `config/sites/<name>.json`; add a fixture test |
| `STORAGE_ERROR` | Write/DB failure | Bad path or DB unreachable | Check mounted volume and `SCRAPER_DB_URL` env var |
| `CONFIG_ERROR` | Startup failure | Invalid properties or JSON recipe | Validate files; check environment overrides |
| `SIGNATURE_ERROR` | Merchant webhook rejected | Wrong secret or tampered payload | Verify secret configuration, never log it |

## Debugging recipe

1. Reproduce in Docker with the offline demo: `./scripts/run-docker.sh --demo`.
2. For a live site, raise log level: add `-Dlogging.level.com.crawlforge=DEBUG` to the java command.
3. For dynamic crawls, check `output/screenshots/failure-*.png`.
4. Inspect `Metrics.snapshot()` — a spike in `retries` or `blocked` points at transport issues;
   a drop in `records` with healthy `pages` points at selectors (parsing).
5. Add a JUnit fixture that reproduces the bad HTML/JSON before changing code.

## Known operational limits

- Selenium / Playwright browser binaries are not baked into the runtime image; install Chromium
  in the VDI image or use `HtmlUnitCrawler`, which needs no browser.
- The Playwright driver is enabled with `-Pplaywright-runtime` for image builds that need it.
- Proxy credentials are stored in configuration only; never committed.
