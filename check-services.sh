#!/bin/bash

# BIST Trading Platform - Servis Durumu Kontrol Script'i

echo "🔍 BIST Trading Platform - Servis Durumu"
echo "========================================"
echo ""

# Docker Servisleri
echo "📦 Docker Servisleri:"
echo "--------------------"
if docker ps &> /dev/null; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|bist" || echo "❌ BIST servisleri bulunamadı"
else
    echo "❌ Docker çalışmıyor veya erişilemiyor"
fi

echo ""

# PostgreSQL
echo "🐘 PostgreSQL (Port 5432):"
echo "--------------------------"
if docker ps | grep -q postgres; then
    echo "✅ Container çalışıyor"
    # Test connection
    if docker exec bist-trading-platform-postgres-1 pg_isready -U bist_user &> /dev/null; then
        echo "✅ Veritabanı bağlantısı OK"
    else
        echo "⚠️  Veritabanı henüz hazır değil"
    fi
else
    echo "❌ PostgreSQL container çalışmıyor"
fi

echo ""

# Redis
echo "🔴 Redis (Port 6379):"
echo "--------------------"
if docker ps | grep -q redis; then
    echo "✅ Container çalışıyor"
    # Test connection
    if docker exec bist-trading-platform-redis-1 redis-cli ping &> /dev/null; then
        echo "✅ Redis bağlantısı OK"
    else
        echo "⚠️  Redis henüz hazır değil"
    fi
else
    echo "❌ Redis container çalışmıyor"
fi

echo ""

# Spring Boot
echo "🍃 Spring Boot (Port 8080):"
echo "---------------------------"
if lsof -i :8080 &> /dev/null; then
    echo "✅ Port 8080 kullanımda"
    # Health check
    HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$HEALTH_STATUS" == "UP" ]; then
        echo "✅ Health check: UP"
        echo "📊 Swagger UI: http://localhost:8080/swagger-ui.html"
    else
        echo "⚠️  Health check yanıt vermiyor"
    fi
else
    echo "❌ Spring Boot çalışmıyor (Port 8080 boş)"
fi

echo ""

# Frontend
echo "⚛️  Frontend (Port 3000):"
echo "------------------------"
if lsof -i :3000 &> /dev/null; then
    echo "✅ Port 3000 kullanımda"
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null)
    if [ "$HTTP_STATUS" == "200" ]; then
        echo "✅ Frontend erişilebilir"
        echo "🌐 URL: http://localhost:3000"
    else
        echo "⚠️  Frontend yanıt vermiyor (HTTP $HTTP_STATUS)"
    fi
else
    echo "❌ Frontend çalışmıyor (Port 3000 boş)"
fi

echo ""
echo "======================================"
echo "✅ Kontrol tamamlandı!"
echo ""

# Özet
DOCKER_OK=$(docker ps | grep -q postgres && docker ps | grep -q redis && echo "1" || echo "0")
BACKEND_OK=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP" && echo "1" || echo "0")
FRONTEND_OK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null | grep -q "200" && echo "1" || echo "0")

echo "📊 Sistem Durumu:"
echo "  Docker Servisleri: $([ "$DOCKER_OK" == "1" ] && echo "✅" || echo "❌")"
echo "  Spring Boot:       $([ "$BACKEND_OK" == "1" ] && echo "✅" || echo "❌")"
echo "  Frontend:          $([ "$FRONTEND_OK" == "1" ] && echo "✅" || echo "❌")"

if [ "$DOCKER_OK" == "1" ] && [ "$BACKEND_OK" == "1" ] && [ "$FRONTEND_OK" == "1" ]; then
    echo ""
    echo "🎉 Tüm servisler çalışıyor! Ready to code!"
else
    echo ""
    echo "⚠️  Bazı servisler çalışmıyor. Yukarıdaki detayları kontrol edin."
fi
