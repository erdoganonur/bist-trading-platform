#!/bin/bash

# BIST Trading Platform Build Script
# Sets correct Java version and runs Gradle build

export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home

echo "🚀 Building BIST Trading Platform..."
echo "Java Version: $(java -version 2>&1 | head -1)"
echo "Gradle Version: $(./gradlew --version | grep Gradle)"

# Build without tests (for faster builds)
if [ "$1" = "--with-tests" ]; then
    echo "🧪 Running build with tests..."
    ./gradlew build
else
    echo "⚡ Running build without tests (faster)..."
    ./gradlew build -x test
fi

echo "✅ Build completed!"
