# Client domain notes: BPO, IT-enabled services and merchant data

**BPO / data-entry firms, IT-enabled services and software companies** all depend on
accurate, high-volume data feeds. This document records what that means in practice and how
CrawlForge addresses it.

## What BPO / data-entry clients actually need

| Challenge | Consequence for a scraper | How CrawlForge handles it |
| --- | --- | --- |
| Accuracy SLAs (often 99%+) | A single bad row can fail an audit | `DataValidator` scores every record; low-score rows never reach the client |
| High-volume, repetitive feeds | Manual checking does not scale | JUnit tests + `DataQualityReport` Groovy tool for sampled QA |
| Many client-specific formats | One parser per client is expensive | Declarative `SiteConfig` recipes and a `ParserRegistry` |
| Frequent target-site changes | Breakages must be fixed quickly | Selectors are config-only; failure screenshots and typed error codes speed root-cause analysis |
| Strict turnaround times | Crawls must finish predictably | Fixed thread pool, timeouts, retries with bounded backoff, health metrics |
| Sensitive customer data | PII / PCI exposure risk | `CardDataMasker`, no secrets in logs, `.gitignore` for `.env` |
| Auditability | Clients ask "where did this row come from?" | Every record carries `source`, `url` and `scrapedAt` |

## IT-enabled services / software-company context

- Multiple clients share the same platform → tenant isolation is achieved with one JSON recipe
  per client and separate output files / DB rows.
- Delivery happens on VDI or a client-provided platform → the fat jar and Docker image are the
  deployment unit; configuration comes from environment variables.
- Clients want integration, not raw files → `JsonWriter` and `IntegrationPayloadBuilder` (Groovy)
  produce ready-to-consume payloads; `RecordRepository` supports direct SQL delivery.

## Merchant / payments domain (Paymentwall-style integration)

`payment/PaymentwallPayloadParser.java` demonstrates the kind of integration a BPO client with
e-commerce data often needs:

1. Verify the `HmacSHA256` signature before trusting the payload.
2. Parse transaction fields (`id`, `amount`, `currency`, `status`, `created_at`).
3. Keep only the last four digits of a card number; brand detected from the PAN prefix.
4. Validate PANs with the Luhn checksum.
5. Raise a typed `ScraperException` with `SIGNATURE_ERROR` / `PAYLOAD_ERROR` codes.

No real card data, API keys or webhook secrets are present in the repository.

## Communication with clients

- Define a data contract before writing selectors: `docs/DATA_CONTRACT.md`.
- Report metrics per run (pages, records, failures, retries, captchas, blocked) so the client
  can see reliability, not just a file.
- Document root causes in `docs/TROUBLESHOOTING.md` for knowledge sharing.
