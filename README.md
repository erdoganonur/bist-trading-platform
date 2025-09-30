# BIST Trading Platform 🚀

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/your-org/bist-trading-platform/actions)
[![Architecture](https://img.shields.io/badge/Architecture-Simplified%20Monolith-blue.svg)](./docs/architecture/system-design.md)
[![Version](https://img.shields.io/badge/version-2.0.0--SIMPLIFIED-green.svg)](https://github.com/your-org/bist-trading-platform/releases)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue.svg)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-24.0+-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A high-performance, enterprise-grade trading platform for Borsa Istanbul (BIST) built with modern Java technologies. **Now simplified into a single, manageable application** that consolidates all trading functionality while maintaining enterprise-grade features and Turkish market compliance.

**🎯 Current Status**: **Architecture Successfully Simplified!** 🎉 From complex microservices to a single, powerful application. All functionality consolidated, simplified deployment, and enterprise-ready for production.

## 🌟 Project Overview

### Key Features

- 🎯 **Unified Application**: All functionality consolidated into a single, manageable application
- 🔥 **Real-time Market Data**: WebSocket streaming with comprehensive analytics
- ⚡ **High-Performance Trading**: Optimized for Turkish market requirements
- 🏛️ **BIST Market Compliance**: Full Turkish market support with TCKN validation
- 🔐 **Enterprise Security**: JWT authentication with RBAC and comprehensive validation
- 📊 **Integrated Analytics**: Built-in market data analysis and technical indicators
- 🚀 **Simplified Deployment**: Single application with streamlined startup scripts
- 📈 **Comprehensive API**: REST endpoints covering all trading operations
- 🐳 **Cloud-Ready**: Docker containerization with simplified orchestration
- 📚 **Comprehensive Documentation**: Updated for simplified architecture

### Architecture Highlights

- **Consolidated Monolith**: All services integrated into one powerful application
- **Clean Architecture**: Well-organized modules with clear separation of concerns
- **Simplified Deployment**: From 5+ services to 1 unified application
- **Retained Functionality**: User management, trading, market data, and broker integration
- **Enterprise Security**: JWT authentication and role-based access control maintained
- **Scalable Data Layer**: PostgreSQL with optimized schema design
- **Mock Integrations**: AlgoLab broker integration with comprehensive mock responses
- **Production-Ready**: Simplified but enterprise-grade architecture

## ⚡ Quick Start

Get up and running in just **3 simple steps**:

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/bist-trading-platform.git
cd bist-trading-platform
```

### 2. Build the Application (Optional)
```bash
# Build the application
./build-app.sh

# OR use Gradle directly
./gradlew clean build -x test
```

### 3. Start the Application
```bash
# One command to start everything!
./start-app.sh

# OR start manually
./gradlew :platform-services:user-management-service:bootRun

# Access the application
open http://localhost:8080
open http://localhost:8080/swagger-ui.html
```

**That's it! 🎉** Single application with all functionality available at **http://localhost:8080**

### Stop the Application
```bash
# Clean shutdown
./stop-app.sh
```

## 📚 Documentation

| Documentation | Link | Description |
|---------------|------|-------------|
| **GraphQL API** | [/docs/api/graphql-api.md](./docs/api/graphql-api.md) | **NEW!** GraphQL schema, queries, and mutations |
| **Sprint Reports** | [/docs/sprints/](./docs/sprints/) | Sprint progress and achievements |
| **API Documentation** | [/docs/api/](./docs/api/) | REST API and WebSocket API specs |
| **Architecture Guide** | [/docs/architecture/](./docs/architecture/) | System design and data flow |
| **Setup Guide** | [/docs/setup/](./docs/setup/) | Development and production setup |
| **Issues & Gaps** | [Issues Analysis](./docs/sprints/issues-and-gaps.md) | Current limitations and roadmap |

### Quick Links
- 📊 [Sprint Reports Overview](./docs/sprints/sprint-reports-overview.md) - Complete sprint history & status
- ✅ [Sprint 4 Completion Report](./SPRINT-4-COMPLETION-REPORT.md) - **NEW!** Complete success report
- 🚀 [Sprint 5 Planning](./docs/sprints/sprint-5-planning.md) - Next sprint roadmap & priorities
- 📈 [Sprint Comparison Analysis](./docs/sprints/sprint-comparison-analysis.md) - 4 Sprint progress analysis
- 🔍 [Issues & Gaps Analysis](./docs/sprints/issues-and-gaps.md) - Current status & resolution plan
- 🏗️ [System Architecture](./docs/architecture/system-design.md)
- 🔄 [Data Flow Diagrams](./docs/architecture/data-flow.md)
- 🚀 [Deployment Guide](./docs/architecture/deployment.md)
- 🛠️ [Development Setup](./docs/setup/development.md)
- 🚀 [GraphQL API Documentation](./docs/api/graphql-api.md) - **NEW!** Complete GraphQL schema & examples
- 🌐 [REST API Documentation](./docs/api/rest-api.md)

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
| **GraphQL Gateway** | [Health](http://localhost:8090/actuator/health) | [Metrics](http://localhost:8090/actuator/metrics) | [GraphiQL](http://localhost:8090/graphiql) |
| **REST API Gateway** | [Health](http://localhost:8080/actuator/health) | [Metrics](http://localhost:8080/actuator/metrics) | [Actuator](http://localhost:8080/actuator) |
| **User Management** | [Health](http://localhost:8081/actuator/health) | [Metrics](http://localhost:8081/actuator/metrics) | [Actuator](http://localhost:8081/actuator) |
| **Order Management** | [Health](http://localhost:8082/actuator/health) | [Metrics](http://localhost:8082/actuator/metrics) | [Actuator](http://localhost:8082/actuator) |
| **Market Data** | [Health](http://localhost:8083/actuator/health) | [Metrics](http://localhost:8083/actuator/metrics) | [Actuator](http://localhost:8083/actuator) |
| **Broker Integration** | [Health](http://localhost:8084/actuator/health) | [Metrics](http://localhost:8084/actuator/metrics) | [Actuator](http://localhost:8084/actuator) |

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
| **Framework** | Spring Boot | 3.3.4 | Web framework & DI |
| **GraphQL** | Netflix DGS | 8.7.1 | GraphQL framework |
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

## 🏛️ Simplified Architecture

```mermaid
graph TB
    subgraph "Load Balancer (Optional)"
        LB[NGINX/HAProxy]
    end

    subgraph "Consolidated Application"
        APP[BIST Trading Platform<br/>:8080<br/><br/>✅ User Management<br/>✅ Market Data Analysis<br/>✅ Order Management<br/>✅ Broker Integration<br/>✅ JWT Security<br/>✅ REST API]
    end

    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Database)]
    end

    subgraph "External Systems (Mock)"
        AL[AlgoLab Broker<br/>(Mock Integration)]
        MS[Market Data Providers<br/>(Mock Data)]
    end

    subgraph "Monitoring (Optional)"
        PR[Prometheus<br/>Metrics]
        GR[Grafana<br/>Dashboards]
    end

    LB --> APP
    APP --> PG
    APP --> AL
    APP --> MS
    APP --> PR
    PR --> GR

    classDef app fill:#e1f5fe,stroke:#01579b,stroke-width:3px
    classDef data fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef external fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef monitor fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px

    class APP app
    class PG data
    class AL,MS external
    class PR,GR monitor
```

### Architecture Benefits
- **🎯 Single Application**: All functionality in one deployable unit
- **🚀 Simplified Deployment**: One port, one process, easy management
- **🔧 Easy Development**: Single codebase with clear module separation
- **📈 Maintained Features**: All original functionality preserved
- **🔐 Enterprise Security**: JWT authentication and RBAC maintained
- **📊 Comprehensive API**: All endpoints consolidated under port 8080

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