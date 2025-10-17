# BIST Trading Platform

**Borsa İstanbul için Modern, Enterprise-Grade Ticaret Platformu**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)]()
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)]()

> 🚀 **Gerçek zamanlı piyasa verileri, güvenli işlem sistemi ve AlgoLab broker entegrasyonu**

---

## 📋 İçindekiler

- [Genel Bakış](#genel-bakış)
- [Özellikler](#özellikler)
- [Hızlı Başlangıç](#hızlı-başlangıç)
- [Mimari](#mimari)
- [Teknolojiler](#teknolojiler)
- [Dokümantasyon](#dokümantasyon)
- [Geliştirme](#geliştirme)
- [Üretim Dağıtımı](#üretim-dağıtımı)

---

## 🎯 Genel Bakış

BIST Trading Platform, Borsa İstanbul üzerinden hisse senedi ticareti yapmak isteyen yatırımcılar ve kurumlar için geliştirilmiş, kurumsal seviye bir alım-satım platformudur.

### 🎨 Ana Bileşenler

- **Backend API** - Spring Boot ile geliştirilmiş RESTful ve GraphQL API
- **CLI Client** - Python tabanlı interaktif komut satırı arayüzü
- **Web Frontend** - React + TypeScript ile modern web uygulaması (Geliştirme aşamasında)
- **AlgoLab Integration** - Gerçek zamanlı broker entegrasyonu ve WebSocket veri akışı

---

## ✨ Özellikler

### 🔐 Kimlik Doğrulama ve Güvenlik

- JWT tabanlı güvenli authentication
- Role-based access control (RBAC)
- TC Kimlik doğrulama
- 2FA desteği (İsteğe bağlı)
- Rate limiting ve brute-force koruması

### 💼 Broker Entegrasyonu

- ✅ **AlgoLab API Entegrasyonu**
  - REST API bağlantısı
  - WebSocket real-time veri akışı
  - Otomatik session yönetimi
  - Circuit breaker pattern ile hata yönetimi

- **Desteklenen İşlemler**
  - Emir verme (Limit, Market, Stop)
  - Emir iptal/düzenleme
  - Portföy görüntüleme
  - Gerçekleşen işlemler
  - Gerçek zamanlı fiyat verileri

### 📊 Piyasa Verileri

- ✅ **Gerçek Zamanlı Veri Akışı**
  - WebSocket tabanlı tick data
  - Trade stream (işlem akışı)
  - Order book (emir defteri)
  - HTTP polling API (CLI için)

- **Sembol Bilgileri**
  - 500+ BIST hisse senedi
  - FOREX pariteler (USDTRY, EURTRY vb.)
  - Gerçek zamanlı fiyatlar
  - Günlük değişim ve hacim

### 🖥️ CLI Client

- ✅ **Gerçek Zamanlı Görüntüleme**
  - Live tick data stream
  - Trade flow görüntüleme
  - Portföy takibi
  - Emir defteri

- **İnteraktif Menü**
  - Kullanıcı girişi
  - AlgoLab bağlantı yönetimi
  - Piyasa verilerine erişim
  - Broker işlemleri

### 🔧 Teknik Özellikler

- **Resilience Patterns**
  - Circuit Breaker (Resilience4j)
  - Automatic retry logic
  - Fallback mechanisms
  - Rate limiting

- **Caching**
  - Redis distributed cache
  - Multi-level caching strategy
  - TTL based expiration

- **Database**
  - PostgreSQL (Primary database)
  - Flyway migrations
  - JPA/Hibernate ORM

---

## 🚀 Hızlı Başlangıç

### Gereksinimler

- Java 21+
- PostgreSQL 14+
- Redis 7+
- Python 3.10+ (CLI için)
- Node.js 18+ (Frontend için)

### 1. Depoyu Klonlayın

```bash
git clone <repository-url>
cd bist-trading-platform
```

### 2. Veritabanını Hazırlayın

```bash
# PostgreSQL başlat
brew services start postgresql@14

# Database oluştur
psql postgres
CREATE DATABASE bist_trading;
CREATE USER bist_user WITH PASSWORD 'bist_password';
GRANT ALL PRIVILEGES ON DATABASE bist_trading TO bist_user;
\q
```

### 3. Redis'i Başlatın

```bash
brew services start redis
```

### 4. Backend'i Başlatın

```bash
# Ortam değişkenlerini ayarlayın
export SPRING_DATASOURCE_USERNAME=bist_user
export SPRING_DATASOURCE_PASSWORD=bist_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
export BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long

# Uygulamayı başlat
./gradlew bootRun
```

Backend çalıştığında:
- REST API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- GraphQL: http://localhost:8080/graphql
- Actuator: http://localhost:8080/actuator

### 5. CLI Client'ı Başlatın

```bash
cd cli-client

# Virtual environment oluştur ve bağımlılıkları yükle
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# CLI'ı başlat
./start.sh
```

### 6. Frontend'i Başlatın (Opsiyonel)

```bash
cd frontend
npm install
npm run dev
```

---

## 🏗️ Mimari

### Sistem Mimarisi

```
┌─────────────────┐
│   Web Frontend  │
│  (React + TS)   │
└────────┬────────┘
         │
┌────────┴────────┐      ┌──────────────┐
│   API Gateway   │─────▶│ AlgoLab API  │
│  (Spring Boot)  │      │  (External)  │
└────────┬────────┘      └──────────────┘
         │
┌────────┴────────────────────────┐
│       Core Services             │
├─────────────────────────────────┤
│ • User Management               │
│ • Order Management              │
│ • Market Data Service           │
│ • Broker Integration            │
└────────┬────────────────────────┘
         │
┌────────┴────────┐
│   Data Layer    │
├─────────────────┤
│  • PostgreSQL   │
│  • Redis Cache  │
└─────────────────┘
```

### WebSocket Akışı

```
AlgoLab Server → Backend WebSocket → Message Buffer → HTTP API → CLI
     (WSS)           (Spring)          (In-Memory)      (REST)    (Python)
```

Detaylar için: [docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md](docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md)

---

## 🛠️ Teknolojiler

### Backend

- **Framework:** Spring Boot 3.3.4
- **Language:** Java 21
- **Security:** Spring Security 6 + JWT
- **Database:** PostgreSQL 14, Flyway Migrations
- **Caching:** Redis 7
- **API:** REST (OpenAPI 3) + GraphQL
- **Resilience:** Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- **Build:** Gradle 8
- **Testing:** JUnit 5, Mockito, TestContainers

### CLI Client

- **Language:** Python 3.10+
- **Framework:** Typer
- **UI:** Rich (Terminal UI)
- **HTTP:** httpx
- **Config:** python-dotenv, pydantic

### Frontend (In Development)

- **Framework:** React 18
- **Language:** TypeScript 5
- **Build:** Vite
- **Styling:** Tailwind CSS
- **State:** React Query

---

## 📚 Dokümantasyon

### Kurulum ve Geliştirme

- [Geliştirme Ortamı Kurulumu](docs/setup/development.md)
- [IntelliJ IDEA Kurulumu](docs/setup/intellij-setup.md)
- [Hızlı Başlangıç Kılavuzu](docs/setup/startup-guide.md)
- [IntelliJ Run Configurations](docs/setup/intellij-run-config.md)

### API Dokümantasyonu

- [REST API](docs/api/rest-api.md)
- [GraphQL API](docs/api/graphql-api.md)
- [WebSocket API](docs/api/websocket-api.md)

### AlgoLab Entegrasyonu

- [WebSocket Kapsamlı Kılavuz](docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md)
- [AlgoLab API Endpoints](docs/algolab/ALGOLAB_API_ENDPOINTS.md)
- [Kimlik Doğrulama Akışı](docs/algolab/ALGOLAB_AUTHENTICATION_FLOW.md)
- [Python-Java Mapping](docs/algolab/PYTHON_TO_JAVA_MAPPING.md)

### Güvenlik

- [Authority ve Role Matrix](docs/security/authority-role-matrix.md)
- [Test Authorities](docs/security/test-authorities.md)
- [Hızlı Referans](docs/security/QUICK-REFERENCE.md)

### Veritabanı

- [Kurulum Kılavuzu](docs/database/setup-guide.md)
- [User Schema](docs/database/user-schema.md)

### Mimari ve Tasarım

- [Sistem Tasarımı](docs/architecture/system-design.md)
- [Veri Akışı](docs/architecture/data-flow.md)
- [GraphQL İmplementasyonu](docs/architecture/graphql-implementation.md)

---

## 👨‍💻 Geliştirme

### Kod Standartları

- Java: Google Java Style Guide
- Spring Boot Best Practices
- Clean Code prensipler
- SOLID principles

### Test Stratejisi

```bash
# Tüm testleri çalıştır
./gradlew test

# Integration testler
./gradlew integrationTest

# Coverage raporu
./gradlew jacocoTestReport
```

### Branch Stratejisi

- `main` - Production-ready kod
- `develop` - Development branch
- `feature/*` - Yeni özellikler
- `bugfix/*` - Hata düzeltmeleri
- `hotfix/*` - Acil production fixes

---

## 🚢 Üretim Dağıtımı

### Docker ile Dağıtım

```bash
# Docker image oluştur
./gradlew bootBuildImage

# Container başlat
docker-compose up -d
```

### Ortam Değişkenleri

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
SPRING_DATASOURCE_USERNAME=bist_user
SPRING_DATASOURCE_PASSWORD=your-secure-password

# Security
BIST_SECURITY_JWT_SECRET=your-256-bit-secret-key
BIST_SECURITY_JWT_ACCESS_TOKEN_EXPIRATION=3600

# AlgoLab
ALGOLAB_API_KEY=your-algolab-api-key
ALGOLAB_USERNAME=your-username
ALGOLAB_PASSWORD=your-password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your-redis-password
```

Detaylar için: [docs/setup/production.md](docs/setup/production.md)

---

## 📊 Proje Durumu

### ✅ Tamamlanan Özellikler

- [x] User authentication ve authorization
- [x] AlgoLab REST API entegrasyonu
- [x] AlgoLab WebSocket real-time veri akışı
- [x] CLI client ile real-time görüntüleme
- [x] Message buffering ve HTTP polling API
- [x] Circuit breaker ve resilience patterns
- [x] Redis caching
- [x] Database migrations
- [x] GraphQL API
- [x] Swagger/OpenAPI documentation

### 🔄 Devam Eden

- [ ] Web frontend development
- [ ] Additional broker integrations
- [ ] Advanced charting
- [ ] Mobile app

### 📅 Planlanan

- [ ] Algorithmic trading support
- [ ] Backtesting framework
- [ ] Portfolio analytics
- [ ] Risk management tools

---

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen CONTRIBUTING.md dosyasını okuyun.

---

## 📝 Lisans

Bu proje özel (proprietary) lisans altındadır. Tüm hakları saklıdır.

---

## 📞 İletişim

- Proje Yöneticisi: [İletişim Bilgileri]
- Issue Tracker: GitHub Issues
- Email: support@bisttrading.com

---

## 🙏 Teşekkürler

- Spring Boot Team
- AlgoLab API ekibi
- Tüm katkıda bulunanlara

---

**Son Güncelleme:** 2025-10-17
