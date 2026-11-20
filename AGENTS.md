# AGENTS.md

Agent-neutral instructions (works with Claude Code, Codex, Cursor and other coding agents).

- Project: CrawlForge, Java 21 + Groovy 4 + Maven, Docker-only toolchain.
- Build/test: `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B clean verify`
- Source layout: `core` (models/config/errors), `http` (transport), `stealth` (anti-bot),
  `parsers` (data extraction), `crawler` (traversal), `export` (output),
  `persistence` (SQL), `orchestration` (concurrency), `payment` (client domain),
  `observability` (metrics), `util` (helpers). Groovy automation in `src/main/groovy`.
- Always add tests with changes. Keep public APIs documented.
- Never commit secrets. `.env.example` is the only env file committed.
- More detail: `CLAUDE.md`, `docs/ARCHITECTURE.md`, `docs/AI_ASSISTED_WORKFLOW.md`.
