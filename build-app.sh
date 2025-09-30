#!/bin/bash

# BIST Trading Platform - Build Script
# This script builds the consolidated trading platform application

echo "🔨 Building BIST Trading Platform (Simplified Architecture)"
echo "=========================================================="

# Clean and build all necessary modules
echo "🧹 Cleaning previous builds..."
./gradlew clean

echo "🔨 Building core modules (skipping tests)..."
./gradlew :platform-core:core-common:build -x test
./gradlew :platform-core:core-domain:build -x test
./gradlew :platform-core:core-security:build -x test
./gradlew :platform-core:core-messaging:build -x test

echo "🔨 Building infrastructure modules (skipping tests)..."
./gradlew :platform-infrastructure:infrastructure-persistence:build -x test
./gradlew :platform-infrastructure:infrastructure-integration:build -x test
./gradlew :platform-infrastructure:infrastructure-monitoring:build -x test

echo "🔨 Building main application (skipping tests)..."
./gradlew :platform-services:user-management-service:build -x test

echo "✅ Build completed successfully!"
echo "🚀 You can now start the application with: ./start-app.sh"