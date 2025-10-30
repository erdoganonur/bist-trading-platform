# BIST Trading Platform

**Modern, Enterprise-Grade Trading Platform for Borsa Istanbul**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)]()
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)]()

> 🚀 **Real-time market data, secure trading system, and AlgoLab broker integration**

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Documentation](#documentation)
- [Development](#development)
- [Production Deployment](#production-deployment)

---

## 🎯 Overview

BIST Trading Platform is an enterprise-grade trading platform developed for investors and institutions who want to trade stocks on Borsa Istanbul.

### 🎨 Main Components

- **Backend API** - RESTful and GraphQL API built with Spring Boot
- **CLI Client** - Python-based interactive command-line interface
- **Web Frontend** - Modern web application with React + TypeScript (In Development)
- **AlgoLab Integration** - Real-time broker integration and WebSocket data streaming

---

## ✨ Features

### 🔐 Authentication and Security

- JWT-based secure authentication
- Role-based access control (RBAC)
- Turkish ID (TC Kimlik) authentication
- 2FA support (Optional)
- Rate limiting and brute-force protection

### 💼 Broker Integration

- ✅ **AlgoLab API Integration**
  - REST API connection
  - WebSocket real-time data streaming
  - Automatic session management
  - Circuit breaker pattern for error handling

- **Supported Operations**
  - Order placement (Limit, Market, Stop)
  - Order cancel/modify
  - Portfolio view
  - Executed trades
  - Real-time price data

### 📊 Market Data

- ✅ **Real-Time Data Streaming**
  - WebSocket-based tick data
  - Trade stream
  - Order book
  - HTTP polling API (for CLI)

- **Symbol Information**
  - 500+ BIST stocks
  - FOREX pairs (USDTRY, EURTRY, etc.)
  - Real-time prices
  - Daily change and volume

### 🖥️ CLI Client

- ✅ **Real-Time Display**
  - Live tick data stream
  - Trade flow visualization
  - Portfolio tracking
  - Order book

- **Interactive Menu**
  - User login
  - AlgoLab connection management
  - Market data access
  - Broker operations

### 🔧 Technical Features

- **Resilience Patterns**
  - Circuit Breaker (Resilience4j)
  - Automatic retry logic
  - Fallback mechanisms
  - Rate limiting

- **Caching**
  - Redis distributed cache
  - Multi-level caching strategy
  - TTL-based expiration

- **Database**
  - PostgreSQL (Primary database)
  - Flyway migrations
  - JPA/Hibernate ORM

---

## 🚀 Quick Start

### Requirements

- Java 21+
- PostgreSQL 14+
- Redis 7+
- Python 3.10+ (for CLI)
- Node.js 18+ (for Frontend)

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bist-trading-platform
```

### 2. Setup Database

```bash
# Start PostgreSQL
brew services start postgresql@14

# Create database
psql postgres
CREATE DATABASE bist_trading;
CREATE USER bist_user WITH PASSWORD 'bist_password';
GRANT ALL PRIVILEGES ON DATABASE bist_trading TO bist_user;
\q
```

### 3. Start Redis

```bash
brew services start redis
```

### 4. Start Backend

```bash
# Set environment variables
export SPRING_DATASOURCE_USERNAME=bist_user
export SPRING_DATASOURCE_PASSWORD=bist_password
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
export BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long

# Start the application
./gradlew bootRun
```

Backend will be available at:
- REST API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- GraphQL: http://localhost:8080/graphql
- Actuator: http://localhost:8080/actuator

### 5. Start CLI Client

```bash
cd cli-client

# Create virtual environment and install dependencies
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Start CLI
./start.sh
```

### 6. Start Frontend (Optional)

```bash
cd frontend
npm install
npm run dev
```

---

## 🏗️ Architecture

### System Architecture

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

### WebSocket Flow

```
AlgoLab Server → Backend WebSocket → Message Buffer → HTTP API → CLI
     (WSS)           (Spring)          (In-Memory)      (REST)    (Python)
```

For details, see: [docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md](docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md)

---

## 🛠️ Technologies

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

## 📚 Documentation

### Setup and Development

- [Development Environment Setup](docs/setup/development.md)
- [IntelliJ IDEA Setup](docs/setup/intellij-setup.md)
- [Quick Start Guide](docs/setup/startup-guide.md)
- [IntelliJ Run Configurations](docs/setup/intellij-run-config.md)

### API Documentation

- [REST API](docs/api/rest-api.md)
- [GraphQL API](docs/api/graphql-api.md)
- [WebSocket API](docs/api/websocket-api.md)

### AlgoLab Integration

- [WebSocket Complete Guide](docs/algolab/WEBSOCKET_COMPLETE_GUIDE.md)
- [AlgoLab API Endpoints](docs/algolab/ALGOLAB_API_ENDPOINTS.md)
- [Authentication Flow](docs/algolab/ALGOLAB_AUTHENTICATION_FLOW.md)
- [Python-Java Mapping](docs/algolab/PYTHON_TO_JAVA_MAPPING.md)

### Security

- [Authority and Role Matrix](docs/security/authority-role-matrix.md)
- [Test Authorities](docs/security/test-authorities.md)
- [Quick Reference](docs/security/QUICK-REFERENCE.md)

### Database

- [Setup Guide](docs/database/setup-guide.md)
- [User Schema](docs/database/user-schema.md)

### Architecture and Design

- [System Design](docs/architecture/system-design.md)
- [Data Flow](docs/architecture/data-flow.md)
- [GraphQL Implementation](docs/architecture/graphql-implementation.md)

---

## 👨‍💻 Development

### Code Standards

- Java: Google Java Style Guide
- Spring Boot Best Practices
- Clean Code principles
- SOLID principles

### Testing Strategy

```bash
# Run all tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Coverage report
./gradlew jacocoTestReport
```

### Branch Strategy

- `main` - Production-ready code
- `develop` - Development branch
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `hotfix/*` - Emergency production fixes

---

## 🚢 Production Deployment

### Docker Deployment

```bash
# Build Docker image
./gradlew bootBuildImage

# Start container
docker-compose up -d
```

### Kubernetes Deployment (Mac Mini Compatible)

The platform includes full Kubernetes support optimized for Mac Mini deployment:

**Quick Start with Helm:**

```bash
# Build and deploy to K3s
./k8s/scripts/deploy.sh k3s

# Or deploy to Docker Desktop Kubernetes
./k8s/scripts/deploy.sh docker-desktop

# Health check
./k8s/scripts/health-check.sh

# Cleanup
./k8s/scripts/cleanup.sh
```

**Manual Helm Installation:**

```bash
# Install with Mac Mini optimized values
helm install bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --create-namespace \
  --values k8s/helm/bist-trading-platform/values-mac-mini.yaml

# Access the application
kubectl port-forward svc/bist-trading-backend 8080:8080 -n bist-trading
```

**Kubernetes Options for Mac Mini:**
- **K3s** (Recommended) - Lightweight, production-ready, ARM64 optimized
- **Docker Desktop Kubernetes** - Easy setup, GUI management
- **Minikube** - Full-featured, great for testing

For detailed Kubernetes deployment instructions, see: [k8s/README.md](k8s/README.md)

### Environment Variables

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

For details, see: [docs/setup/production.md](docs/setup/production.md)

---

## 📊 Project Status

### ✅ Completed Features

- [x] User authentication and authorization
- [x] AlgoLab REST API integration
- [x] AlgoLab WebSocket real-time data streaming
- [x] CLI client with real-time display
- [x] Message buffering and HTTP polling API
- [x] Circuit breaker and resilience patterns
- [x] Redis caching
- [x] Database migrations
- [x] GraphQL API
- [x] Swagger/OpenAPI documentation

### 🔄 In Progress

- [ ] Web frontend development
- [ ] Additional broker integrations
- [ ] Advanced charting
- [ ] Mobile app

### 📅 Planned

- [ ] Algorithmic trading support
- [ ] Backtesting framework
- [ ] Portfolio analytics
- [ ] Risk management tools

---

## 🤝 Contributing

Contributions are welcome! Please read CONTRIBUTING.md for details.

---

## 📝 License

This project is proprietary. All rights reserved.

---

## 📞 Contact

- Project Manager: [Contact Information]
- Issue Tracker: GitHub Issues
- Email: support@bisttrading.com

---

## 🙏 Acknowledgments

- Spring Boot Team
- AlgoLab API team
- All contributors

---

**Last Updated:** 2025-10-17
