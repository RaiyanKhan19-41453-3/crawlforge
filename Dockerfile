# Build stage: Maven + JDK 21, nothing installed on the host.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY config ./config
RUN mvn -B -q -DskipTests package

# Runtime stage: small JRE image, non-root user, same jar runs on VDI or client platform.
FROM eclipse-temurin:21-jre
# UID/GID 1000 matches the first regular user on most Linux hosts, so bind-mounted
# output folders stay writable. Override with `docker run --user $(id -u):$(id -g)` if needed.
RUN mkdir -p /app/output && chown -R 1000:1000 /app
WORKDIR /app
COPY --from=build --chown=1000:1000 /workspace/target/crawlforge.jar /app/crawlforge.jar
COPY --chown=1000:1000 config /app/config
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/crawlforge.jar"]
CMD ["--demo", "--output", "/app/output/demo-records.csv"]
