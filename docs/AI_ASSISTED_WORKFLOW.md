# AI-assisted development workflow (Claude Code)

AI-driven coding tools are part of modern software delivery. This repository was developed
with Claude Code, and this document records **how**, with the actual prompts and the review
process used to keep AI-generated code correct and maintainable.

## Project instructions

- `CLAUDE.md` — instructions Claude Code reads automatically (architecture rules, Docker-only
  commands, security rules, mandatory review checklist).
- `AGENTS.md` — the same guidance in an agent-neutral form.

## Prompt library (used while building this project)

### 1. Scaffold the architecture

> "Design a Java 21 + Groovy 4 web scraping engine with separate packages for models, config,
> HTTP transport, anti-bot, parsers, crawlers, exporters, persistence, orchestration and payment
> integration. Use template-method for the crawler pipeline, strategy interfaces for parsers and
> writers, and builders for immutable models. Do not hard-code selectors — put them in JSON recipes.
> Explain each design decision before writing code."

### 2. Implement the transport layer

> "Implement ResilientHttpClient with Apache HttpClient 5: connection pooling, timeouts, cookie
> store, proxy and User-Agent rotation, a thread-safe token-bucket rate limiter, typed exceptions
> with error codes, and retries with exponential backoff. Wrap HTTP 401/403/429 and CAPTCHA
> marker text in specific exception types. Never log credentials or cookies. Add JUnit tests using
> a local HTTP server that fails twice then succeeds to prove the retry path."

### 3. Implement multi-format parsing

> "Implement parsers for HTML (Jsoup CSS selectors), JSON (Jackson JsonPointer), XML (secure DOM
> + XPath), CSV (Commons CSV) and PDF (PDFBox). Add a DataNormalizer and a DataValidator that
> scores every record from 0 to 1 and filters low-quality rows. Write one test per format using
> inline fixtures, including a PDF generated at test time."

### 4. Add browser automation

> "Add three dynamic crawlers that share the AbstractCrawler template: Selenium Chrome with
> explicit waits and failure screenshots, HtmlUnit for lightweight JS rendering, and Playwright
> behind a Maven profile so the default jar stays small. Keep browser fingerprint settings
> consistent within BrowserFingerprint."

### 5. Add the client-domain layer

> "Implement a Paymentwall-style webhook parser: verify HMAC-SHA256 signatures, map transactions
> to a PCI-aware record, mask card numbers, and reject invalid PANs with a Luhn check. Add tests
> for a valid signature, an invalid signature and a masked card number."

### 6. Review pass (the important one)

> "Review every class you generated. List: (a) places that swallow exceptions, (b) unclosed
> resources, (c) secrets or PANs that could reach logs, (d) coupling between layers, (e) missing
> tests. Fix each issue and explain the fix."

## Mandatory human review checklist

AI output is never accepted unchecked:

1. Does it compile and do **all** tests pass inside Docker?
2. Are exceptions typed, propagated and logged with context?
3. Is there a test that would fail if the logic were wrong?
4. Are secrets, cookies, tokens and card numbers kept out of logs and source?
5. Does the change stay inside its documented layer?
6. Are external calls bounded by timeouts, retries and a rate limiter?
7. Does the documentation reflect the actual behaviour?

## Evidence in the repository

- `CLAUDE.md` / `AGENTS.md` — persistent project instructions.
- This file — reproducible prompts and review process.
- `.github/PULL_REQUEST_TEMPLATE.md` — asks reviewers to identify AI-assisted parts.
- Consistent style, Javadoc and tests across all generated classes.

## Rules we followed

- No scraping of sites without authorisation.
- No CAPTCHA-solving service credentials or real proxies committed.
- Rate limiting is enabled by default; robots.txt rules should be reviewed per client engagement.
