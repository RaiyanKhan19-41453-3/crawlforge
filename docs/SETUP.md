# Setup (Docker-only)

Nothing needs to be installed on the host except **Docker** and **Git**.

## Linux / Ubuntu

```bash
git clone https://github.com/RaiyanKhan19-41453-3/crawlforge.git
cd crawlforge
chmod +x scripts/*.sh
./scripts/build.sh                 # builds and runs all tests in a container
docker build -t crawlforge .
docker compose up --build          # demo + PostgreSQL
```

## Windows (PowerShell)

```powershell
git clone https://github.com/RaiyanKhan19-41453-3/crawlforge.git
cd crawlforge
.\scripts\build.bat
docker build -t crawlforge .
docker compose up --build
```

Note: Docker Desktop on Windows must use Linux containers.

The runtime image runs as UID/GID 1000 so a bind-mounted `./output` folder is writable on a
typical Linux host. If your host user has a different UID/GID, pass it explicitly:

```bash
docker run --rm --user "$(id -u):$(id -g)" -v "$PWD/output":/app/output crawlforge \
  --demo --output /app/output/demo-records.csv
```

`docker compose` reads `UID` and `GID` from the environment (defaulting to 1000).

## IDEs

- **VS Code**: open the folder, install recommended extensions (`.vscode/extensions.json`).
  Tasks: "Docker: build + test" and "Docker: run demo". Debug configuration runs the CLI demo.
- **IntelliJ IDEA**: open `pom.xml` as a Maven project, JDK 21. The `CrawlForge Demo` run
  configuration is in `.idea/runConfigurations/`. Build/test via the terminal command above so
  the toolchain stays identical to CI.

## Environment variables

Copy `.env.example` to `.env` (never commit the real values) and pass them with
`docker compose --env-file .env up`.

| Variable | Purpose |
| --- | --- |
| `SCRAPER_MAX_THREADS` | Parallel worker count |
| `SCRAPER_DB_URL` | JDBC URL, e.g. `jdbc:postgresql://postgres:5432/crawlforge` |
| `SCRAPER_DB_USER` / `SCRAPER_DB_PASSWORD` | Database credentials |

## Verifying the build

A green build prints test results from Surefire inside the Maven container. The runtime image
starts the offline demo and writes `./output/demo-records.csv`.
