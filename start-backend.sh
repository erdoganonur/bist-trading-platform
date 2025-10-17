#!/bin/bash
# Start BIST Trading Platform Backend
# This script starts the backend and monitors WebSocket connection

echo "🚀 Starting BIST Trading Platform Backend..."
echo "Port: 8081"
echo "Watching for: Session validation & WebSocket connection"
echo ""

# Kill any existing processes on ports
lsof -ti :8080 | xargs kill -9 2>/dev/null
lsof -ti :8081 | xargs kill -9 2>/dev/null
sleep 1

# Start backend
SPRING_DATASOURCE_USERNAME=bist_user \
SPRING_DATASOURCE_PASSWORD=bist_password \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading \
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration \
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long \
SERVER_PORT=8081 \
./gradlew bootRun 2>&1 | tee /tmp/backend-full.log | grep --line-buffered -i -E "(started|session|websocket|algolab|✅|❌|error|port)"
