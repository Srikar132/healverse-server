# syntax=docker/dockerfile:1

# ---- Build: compile, then split the Spring Boot jar into cache-friendly layers ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Dependencies first so they stay cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI / locally (./mvnw test); the image build only packages.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && cp target/healverse-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Run: JRE only, non-root ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /application

RUN addgroup -S app && adduser -S app -G app

# Least-changing layers first
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Liveness needs no database, so a database blip does not get the container restarted.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
