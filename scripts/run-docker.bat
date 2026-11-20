@echo off
REM Run the built jar inside a container (Windows).
if not exist target\crawlforge.jar (
  echo target\crawlforge.jar not found - run scripts\build.bat first.
  exit /b 1
)
if not exist output mkdir output
docker run --rm -v "%cd%\output":/app/output -v "%cd%\config":/app/config:ro -v "%cd%\target\crawlforge.jar":/app/crawlforge.jar:ro -w /app eclipse-temurin:21-jre java -jar /app/crawlforge.jar %*
