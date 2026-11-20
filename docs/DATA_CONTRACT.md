# Data contract

Every output row is a `ScrapedRecord` with:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `source` | string | yes | Name of the site recipe (`config/sites/*.json`) |
| `url` | string | yes | Absolute source URL (or `csv://row/n`, `pdf://page/n`) |
| `title` | string | no | Short label extracted from the item |
| `content` | string | no | Main extracted text, truncated to 10,000 chars |
| `attributes` | map | yes | Field-name → value, driven by the recipe's `fields` / `xpathFields` |
| `qualityScore` | 0.0–1.0 | yes | Computed by `DataValidator.score()` |
| `scrapedAt` | ISO-8601 instant | yes | Time of extraction |

## Quality rules

- `url` must start with `http` (non-HTTP sources use explicit pseudo-schemes).
- `isValid()` requires a payload (content or attributes).
- `DataValidator.filter()` keeps records with score ≥ `0.4`.
- Missing fields reduce the score by `0.15` each; an empty payload by `0.3`.

## Output formats

- CSV — flat map, one row per record (`CsvWriter`).
- JSON — `{ generatedBy, count, records: [...] }` envelope (`JsonWriter`).
- XLSX — one sheet, auto-sized columns (`ExcelWriter`).
- SQL — `scraped_records` table, batched insert in one transaction (`RecordRepository`).

## Changing the contract

A contract change requires: a JIRA ticket, a version bump in the client's feed, an update to
this file, and new tests asserting the shape.
