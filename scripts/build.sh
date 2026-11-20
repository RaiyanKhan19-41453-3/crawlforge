#!/usr/bin/env bash
# Build and test entirely inside Docker. No local Java/Maven required.
set -euo pipefail
cd "$(dirname "$0")/.."
docker run --rm -v "$PWD":/workspace -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
