# ── Stage 1: Build ───────────────────────────────────────────
FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

# Copy gradle files first for layer caching
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source and build fat JAR
COPY src/ src/
RUN gradle buildFatJar --no-daemon -x test

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user untuk keamanan
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy fat JAR dari builder
COPY --from=builder /app/build/libs/course-app-be.jar app.jar

# Buat folder uploads dengan permissions yang benar
RUN mkdir -p uploads/users uploads/courses uploads/lessons logs \
    && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
