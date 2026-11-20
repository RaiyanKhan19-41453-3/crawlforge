@echo off
REM Run the built jar inside a container (Windows).
if not exist output mkdir output
docker run --rm -v "%cd%\output":/app/output -w /app eclipse-temurin:21-jre java -jar /app/crawlforge.jar %*
