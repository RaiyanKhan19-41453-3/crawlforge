# Contributing

## Getting started

Docker and Git are the only host requirements.

```bash
git clone https://github.com/RaiyanKhan19-41453-3/crawlforge.git
cd crawlforge
./scripts/build.sh       # Windows: scripts\build.bat
```

## Workflow

1. Create a branch: `feature/short-description` or `fix/short-description`.
2. Make the change and add or update tests in `src/test/java`.
3. Run `./scripts/build.sh` — CI runs the same command.
4. Open a pull request using the template and reference the relevant issue.
5. Keep selectors in `config/sites/*.json`, never in Java.

## Code conventions

- Java 21, Groovy 4, four-space indent, UTF-8, LF line endings.
- Public classes and methods have Javadoc.
- Failures use the typed `ScraperException` hierarchy with a meaningful error code.
- Never log secrets, cookies, proxy credentials or card numbers.
- AI-assisted code must pass the review checklist in `docs/AI_ASSISTED_WORKFLOW.md`.

## Reporting issues

Use the issue templates in `.github/ISSUE_TEMPLATE`. For a scraper failure, include the error
code, URL, site recipe name and the relevant log lines.
