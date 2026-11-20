#!/usr/bin/env bash
# Build and test entirely inside Docker. No local Java/Maven required.
# Runs as the host user so target/ stays editable, and keeps the Maven cache on the host.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .mvn-cache
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$PWD":/workspace \
  -v "$PWD/.mvn-cache":/tmp/.m2 \
  -w /workspace \
  -e MAVEN_OPTS="-Dmaven.repo.local=/tmp/.m2/repository" \
  maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
