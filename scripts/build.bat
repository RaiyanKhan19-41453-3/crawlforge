@echo off
REM Build and test entirely inside Docker (Windows). No local Java/Maven required.
if not exist .mvn-cache mkdir .mvn-cache
docker run --rm -v "%cd%":/workspace -v "%cd%\.mvn-cache":/tmp/.m2 -w /workspace -e MAVEN_OPTS="-Dmaven.repo.local=/tmp/.m2/repository" maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
