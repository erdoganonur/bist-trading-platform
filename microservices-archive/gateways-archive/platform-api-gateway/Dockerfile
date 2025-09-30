# Multi-stage build for BIST Trading Platform API Gateway
FROM openjdk:21-jdk-slim as builder

# Set working directory
WORKDIR /workspace/app

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy source code
COPY src src

# Build the application
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon

# Extract layers for better caching
RUN mkdir -p build/extracted
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

# Production stage
FROM openjdk:21-jre-slim

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r gateway && \
    useradd --no-log-init -r -g gateway gateway

# Set working directory
WORKDIR /app

# Copy application layers from builder stage
COPY --from=builder workspace/app/build/extracted/dependencies/ ./
COPY --from=builder workspace/app/build/extracted/spring-boot-loader/ ./
COPY --from=builder workspace/app/build/extracted/snapshot-dependencies/ ./
COPY --from=builder workspace/app/build/extracted/application/ ./

# Change ownership to gateway user
RUN chown -R gateway:gateway /app
USER gateway

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Application properties
ENV SPRING_PROFILES_ACTIVE=docker
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# Labels for image metadata
LABEL org.opencontainers.image.title="BIST Trading Platform API Gateway"
LABEL org.opencontainers.image.description="Spring Cloud Gateway for BIST Trading Platform microservices"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="BIST Trading Platform"
LABEL org.opencontainers.image.source="https://github.com/bisttrading/platform"