@echo off
REM Build and test entirely inside Docker (Windows).
docker run --rm -v "%cd%":/workspace -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
