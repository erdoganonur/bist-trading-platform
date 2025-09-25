# BIST Trading Platform 🚀

[![Build Status](https://github.com/your-org/bist-trading-platform/workflows/CI/badge.svg)](https://github.com/your-org/bist-trading-platform/actions)
[![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen.svg)](./build/reports/jacoco/test/html/index.html)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/your-org/bist-trading-platform/releases)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-24.0+-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A high-performance, enterprise-grade trading platform for Borsa Istanbul (BIST) built with modern Java technologies. Designed as a modular monolith with microservices-ready architecture, supporting real-time market data processing, order execution, and Turkish market compliance.

## 🌟 Project Overview

### Key Features

- 🔥 **Real-time Market Data**: WebSocket streaming with <50ms latency processing
- ⚡ **High-Performance Trading**: 50,000+ ticks/second throughput capability
- 🏛️ **BIST Market Compliance**: Full Turkish market support with TCKN validation
- 🔐 **Enterprise Security**: JWT authentication with advanced encryption
- 📊 **Time-Series Analytics**: TimescaleDB integration for market data analysis
- 🐳 **Cloud-Ready**: Docker containerization with Kubernetes support
- 📈 **Comprehensive Monitoring**: Prometheus, Grafana, and Jaeger integration
- 🧪 **Test-Driven Development**: 85%+ code coverage with performance benchmarks

### Architecture Highlights

- **Modular Monolith**: Clean separation of concerns with domain-driven design
- **Event-Driven**: Apache Kafka for asynchronous communication
- **Microservices-Ready**: Easy transition to distributed architecture
- **Observability-First**: Built-in monitoring, metrics, and distributed tracing
- **Scalable Data Layer**: PostgreSQL + TimescaleDB for time-series data
- **Redis Caching**: Session management with 24-hour TTL

## ⚡ Quick Start

Get up and running in just **3 steps**:

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/bist-trading-platform.git
cd bist-trading-platform
```

### 2. Start with Docker Compose
```bash
docker-compose up -d
```

### 3. Access Services
```bash
# Check service health
curl http://localhost:8081/actuator/health  # User Management
curl http://localhost:8082/actuator/health  # Market Data
curl http://localhost:8083/actuator/health  # Broker Integration
```

**That's it! 🎉** All services are running with monitoring dashboards available.

## 📚 Documentation

| Documentation | Link | Description |
|---------------|------|-------------|
| **API Documentation** | [/docs/api/](./docs/api/) | REST API and WebSocket API specs |
| **Architecture Guide** | [/docs/architecture/](./docs/architecture/) | System design and data flow |
| **Setup Guide** | [/docs/setup/](./docs/setup/) | Development and production setup |
| **OpenAPI Spec** | http://localhost:8081/swagger-ui.html | Interactive API documentation |

### Quick Links
- 🏗️ [System Architecture](./docs/architecture/system-design.md)
- 🔄 [Data Flow Diagrams](./docs/architecture/data-flow.md)
- 🚀 [Deployment Guide](./docs/architecture/deployment.md)
- 🛠️ [Development Setup](./docs/setup/development.md)
- 🏭 [Production Setup](./docs/setup/production.md)
- 🌐 [REST API Documentation](./docs/api/rest-api.md)
- 🔌 [WebSocket API Documentation](./docs/api/websocket-api.md)

## 🛠️ Development

### Development Setup
For detailed development environment setup, see our [Development Guide](./docs/setup/development.md).

**Prerequisites**: Java 21, Docker Desktop, IntelliJ IDEA (recommended)

```bash
# Quick development setup
./gradlew clean build
docker-compose up -d postgres redis
./gradlew :platform-services:user-management-service:bootRun
```

### Contributing Guidelines
1. Fork the repository and create a feature branch
2. Follow our coding standards (Checkstyle, SpotBugs, PMD)
3. Write tests for new functionality (minimum 85% coverage)
4. Update documentation for API changes
5. Submit a pull request with clear description

### Code of Conduct
We follow the [Contributor Covenant](https://www.contributor-covenant.org/) code of conduct. Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details.

## 🧪 Testing

### Run All Tests
```bash
# Full test suite with coverage
./test-runner.sh all

# Quick unit tests only
./gradlew test

# Integration tests with TestContainers
./gradlew integrationTest
```

### Coverage Reports
- **Overall Coverage**: 85%+ (target: 90%)
- **Unit Tests**: 90%+ coverage across all modules
- **Integration Tests**: 100% critical path coverage
- **Performance Tests**: All benchmarks validated

**View Reports**:
- Coverage: `./build/reports/jacoco/test/html/index.html`
- Test Results: `./build/reports/tests/test/index.html`

## 🚀 Deployment

### Docker Deployment
```bash
# Build and run with Docker
docker build -t bist-trading:latest .
docker run -p 8081:8081 bist-trading:latest
```

### Kubernetes Deployment
```bash
# Deploy to Kubernetes
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/

# Check deployment status
kubectl get pods -n bist-trading-prod
```

For production deployment details, see [Production Setup Guide](./docs/setup/production.md).

## 📊 Monitoring

### Health Endpoints
| Service | Health Check | Metrics | Management |
|---------|-------------|---------|------------|
| **User Management** | [Health](http://localhost:8081/actuator/health) | [Metrics](http://localhost:8081/actuator/metrics) | [Actuator](http://localhost:8081/actuator) |
| **Market Data** | [Health](http://localhost:8082/actuator/health) | [Metrics](http://localhost:8082/actuator/metrics) | [Actuator](http://localhost:8082/actuator) |
| **Broker Integration** | [Health](http://localhost:8083/actuator/health) | [Metrics](http://localhost:8083/actuator/metrics) | [Actuator](http://localhost:8083/actuator) |

### Metrics Endpoints
```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Application metrics
curl http://localhost:8081/actuator/metrics

# JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

### Grafana Dashboards
Access monitoring dashboards at [http://localhost:3000](http://localhost:3000) (admin/admin123)

**Available Dashboards**:
- 🔥 **BIST Trading Overview**: System-wide metrics and health
- 📈 **Market Data Performance**: Real-time data processing metrics
- ⚡ **Trading Operations**: Order execution and latency metrics
- 🖥️ **Infrastructure Monitoring**: Database, cache, and message queue metrics

## 🏗️ Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| **Runtime** | Java OpenJDK | 21 LTS | Application runtime |
| **Framework** | Spring Boot | 3.3.0 | Web framework & DI |
| **Build Tool** | Gradle | 8.8 | Build automation |
| **Database** | PostgreSQL | 16 | Primary database |
| **Time-Series DB** | TimescaleDB | 2.14 | Market data storage |
| **Cache** | Redis | 7.4 | Session & data cache |
| **Message Queue** | Apache Kafka | 3.8 | Event streaming |
| **Monitoring** | Prometheus | 2.45 | Metrics collection |
| **Dashboards** | Grafana | 10.0 | Monitoring UI |
| **Tracing** | Jaeger | 1.50 | Distributed tracing |
| **Container** | Docker | 24.0+ | Containerization |
| **Orchestration** | Kubernetes | 1.28+ | Container orchestration |

## 🏛️ Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[NGINX/HAProxy]
    end

    subgraph "Application Layer"
        US[User Management<br/>Service :8081]
        MD[Market Data<br/>Service :8082]
        BI[Broker Integration<br/>Service :8083]
    end

    subgraph "Data Layer"
        PG[(PostgreSQL<br/>+TimescaleDB)]
        RD[(Redis<br/>Cache)]
        KF[(Kafka<br/>Message Queue)]
    end

    subgraph "External Systems"
        AL[AlgoLab<br/>Broker API]
        MS[Market Data<br/>Providers]
    end

    subgraph "Monitoring"
        PR[Prometheus]
        GR[Grafana]
        JG[Jaeger]
    end

    LB --> US
    LB --> MD
    LB --> BI

    US --> PG
    US --> RD
    US --> KF

    MD --> PG
    MD --> RD
    MD --> KF
    MD --> MS

    BI --> PG
    BI --> RD
    BI --> KF
    BI --> AL

    US --> PR
    MD --> PR
    BI --> PR

    PR --> GR
    US --> JG
    MD --> JG
    BI --> JG
```

## 📞 Support

### Issue Reporting
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/your-org/bist-trading-platform/issues/new?template=bug_report.md)
- 💡 **Feature Requests**: [GitHub Issues](https://github.com/your-org/bist-trading-platform/issues/new?template=feature_request.md)
- 📚 **Documentation**: [GitHub Issues](https://github.com/your-org/bist-trading-platform/issues/new?template=documentation.md)

### Contact Information
- **Development Team**: [dev-team@bist-trading.com](mailto:dev-team@bist-trading.com)
- **Architecture Questions**: [architecture@bist-trading.com](mailto:architecture@bist-trading.com)
- **Production Support**: [support@bist-trading.com](mailto:support@bist-trading.com)
- **Security Issues**: [security@bist-trading.com](mailto:security@bist-trading.com)

### Community
- 💬 **Slack**: [#bist-trading-platform](https://workspace.slack.com/channels/bist-trading-platform)
- 📖 **Wiki**: [GitHub Wiki](https://github.com/your-org/bist-trading-platform/wiki)
- 📋 **Project Board**: [GitHub Projects](https://github.com/your-org/bist-trading-platform/projects)

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses
- Spring Framework: Apache License 2.0
- PostgreSQL: PostgreSQL License
- Redis: BSD License
- Apache Kafka: Apache License 2.0

---

## 🔗 Related Projects

- [BIST Trading Mobile App](https://github.com/your-org/bist-trading-mobile)
- [BIST Trading Web UI](https://github.com/your-org/bist-trading-web)
- [BIST Market Data Analytics](https://github.com/your-org/bist-analytics)

---

<div align="center">

**Built with ❤️ for Turkish Financial Markets**

[⭐ Star this repository](https://github.com/your-org/bist-trading-platform) • [📖 Documentation](./docs/) • [🐳 Docker Hub](https://hub.docker.com/r/bisttrading/platform) • [📊 Grafana Dashboards](./monitoring/dashboards/)

</div>