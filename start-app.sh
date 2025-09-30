#!/bin/bash

# BIST Trading Platform - Simplified Startup Script
# This script starts the consolidated trading platform application

echo "🚀 Starting BIST Trading Platform (Simplified Architecture)"
echo "==========================================================="

# Set environment variables
export SPRING_DATASOURCE_USERNAME=bist_user
export SPRING_DATASOURCE_PASSWORD=bist_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
export SPRING_FLYWAY_ENABLED=false
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
export BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long
export SERVER_PORT=8080

echo "✅ Environment variables set"
echo "🔗 Application will be available at: http://localhost:8080"
echo "📚 API Documentation: http://localhost:8080/swagger-ui.html"
echo "💻 Health Check: http://localhost:8080/actuator/health"
echo ""

# Kill any existing processes on port 8080
echo "🔄 Cleaning up existing processes..."
lsof -ti:8080 | xargs -r kill -9 2>/dev/null || true

# Start the application
echo "🎯 Starting consolidated application..."
./gradlew :platform-services:user-management-service:bootRun