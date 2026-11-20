# Agile workflow (JIRA / Git / code reviews)

JIRA task management within an Agile process, Git version control and code reviews keep
delivery predictable. This repository is structured to make that workflow visible.

## Ticket taxonomy

| Prefix | Meaning | Example |
| --- | --- | --- |
| `CRF-###` | Story / feature | `CRF-014 Add Playwright crawler` |
| `CRF-BUG-###` | Defect | `CRF-BUG-003 Data quality drop on demo-shop` |
| `CRF-QA-###` | Data-quality task | `CRF-QA-002 Add XPath assertions for SKU` |

A ticket is "Done" only when: code is merged, tests pass in CI, docs are updated and the client
data contract is unchanged or explicitly versioned.

## Sprint rhythm (two-week sprints)

1. **Planning** — pick tickets, write acceptance criteria in `.github/ISSUE_TEMPLATE/feature_request.md`.
2. **Development** — one branch per ticket: `feature/CRF-014-playwright`, `bugfix/CRF-BUG-003`.
3. **Review** — open a PR using `.github/PULL_REQUEST_TEMPLATE.md`; at least one reviewer.
4. **Verification** — CI runs `docker run ... mvn -B clean verify` and builds the image.
5. **Demo** — run `docker compose up` and show the output file to the client/stakeholder.
6. **Retro** — record root causes of failures in `TROUBLESHOOTING.md`.

## Branch protection

- `main` is protected; no direct pushes.
- CI must be green before merge.
- Commit messages reference the JIRA ticket: `CRF-014: add Playwright crawler (#21)`.

## Code-review focus

- Selectors and API paths belong in `config/sites/*.json`, not Java.
- Every new parser/crawler has tests.
- Error codes are added to `docs/TROUBLESHOOTING.md`.
- AI-assisted code is reviewed against the checklist in `docs/AI_ASSISTED_WORKFLOW.md`.
