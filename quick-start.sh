#!/bin/bash

# BIST Trading Platform - Quick Start Script
# Docker servislerini başlatır ve sistem durumunu kontrol eder

echo "🚀 BIST Trading Platform - Quick Start"
echo "======================================="
echo ""

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker çalışmıyor!"
    echo "   Lütfen Docker Desktop'ı başlatın ve tekrar deneyin."
    exit 1
fi

echo "✅ Docker çalışıyor"
echo ""

# Start Docker services
echo "📦 Docker servislerini başlatıyorum..."
echo "   - PostgreSQL (port 5432)"
echo "   - Redis (port 6379)"
echo ""

docker-compose up -d postgres redis

# Wait for services to be ready
echo ""
echo "⏳ Servislerin hazır olması bekleniyor..."

# Wait for PostgreSQL
for i in {1..30}; do
    if docker exec bist-trading-platform-postgres-1 pg_isready -U bist_user &> /dev/null; then
        echo "✅ PostgreSQL hazır!"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""

# Wait for Redis
for i in {1..30}; do
    if docker exec bist-trading-platform-redis-1 redis-cli ping &> /dev/null; then
        echo "✅ Redis hazır!"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "======================================"
echo ""
echo "✅ Docker servisleri başarıyla başlatıldı!"
echo ""
echo "📝 Sonraki adımlar:"
echo ""
echo "1️⃣  Spring Boot'u IntelliJ'den başlat:"
echo "    → BistTradingPlatformApplication sınıfını çalıştır"
echo "    → Veya: Run → Run 'BIST Trading Platform'"
echo ""
echo "2️⃣  Frontend'i başlat:"
echo "    → Terminal: cd frontend && npm run dev"
echo "    → Veya: VS Code'da package.json → dev script'i çalıştır"
echo ""
echo "3️⃣  Tarayıcıdan eriş:"
echo "    → Backend:  http://localhost:8080/swagger-ui.html"
echo "    → Frontend: http://localhost:3000"
echo ""
echo "📊 Sistem durumunu kontrol etmek için:"
echo "    ./check-services.sh"
echo ""
echo "🛑 Servisleri durdurmak için:"
echo "    docker-compose down"
echo ""
echo "======================================"
echo "🎉 Hazır! Keyifli kodlamalar!"
