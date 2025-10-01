#!/bin/bash

# BIST Trading Platform - REAL Monolith Startup Script
# Simplified startup for the real monolith architecture

echo "🚀 Starting BIST Trading Platform - REAL Monolith..."
echo "📋 Architecture: Simplified Single Application"

# Set environment variables for the REAL monolith
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_USERNAME=bist_user
export SPRING_DATASOURCE_PASSWORD=bist_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
export SPRING_FLYWAY_ENABLED=false
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
export BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long
export SERVER_PORT=8080

echo "✅ Environment variables set"
echo "🏗️  Building application..."

# Build the application
./gradlew build -x test

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
    echo "🚀 Starting REAL Monolith on port 8080..."
    echo ""
    echo "📖 Application will be available at: http://localhost:8080"
    echo "🔧 API endpoints: http://localhost:8080/swagger-ui.html"
    echo ""

    # Start the REAL monolith
    ./gradlew bootRun
else
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi