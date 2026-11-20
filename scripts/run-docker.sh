#!/usr/bin/env bash
# Run the built jar inside a container, mounting ./output for the results.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p output
docker run --rm -v "$PWD/output":/app/output -w /app \
  eclipse-temurin:21-jre java -jar /app/crawlforge.jar "$@"
