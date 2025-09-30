#!/bin/bash

# BIST Trading Platform - Stop Script
# This script stops the consolidated trading platform application

echo "🛑 Stopping BIST Trading Platform"
echo "================================="

# Kill processes running on application ports
echo "🔄 Stopping application processes..."
lsof -ti:8080,8081,8082,8083,8084,8085,8086,8091 | xargs -r kill -9 2>/dev/null || true

# Kill any remaining gradle processes
echo "🔄 Cleaning up gradle processes..."
pkill -f "gradlew.*bootRun" 2>/dev/null || true

echo "✅ Application stopped successfully"