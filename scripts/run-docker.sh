#!/usr/bin/env bash
# Run the built jar inside a container, mounting ./output for the results.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f target/crawlforge.jar ]; then
  echo "target/crawlforge.jar not found - run ./scripts/build.sh first." >&2
  exit 1
fi
mkdir -p output
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$PWD/output":/app/output \
  -v "$PWD/config":/app/config:ro \
  -v "$PWD/target/crawlforge.jar":/app/crawlforge.jar:ro \
  -w /app \
  eclipse-temurin:21-jre java -jar /app/crawlforge.jar "$@"
