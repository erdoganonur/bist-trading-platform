# BIST Trading Platform Documentation

Welcome to the BIST Trading Platform documentation! This index will help you navigate all available documentation.

---

## 📋 Quick Navigation

- [🚀 Getting Started](#getting-started)
- [🏗️ Architecture](#architecture)
- [🔌 API Documentation](#api-documentation)
- [💼 AlgoLab Integration](#algolab-integration)
- [🔐 Security](#security)
- [💾 Database](#database)
- [🛠️ Setup Guides](#setup-guides)
- [📊 Sprint Reports](#sprint-reports)

---

## 🚀 Getting Started

**New to the project?** Start here:

1. **[Quick Start Guide](./setup/startup-guide.md)**
   - Get the platform running in minutes
   - Prerequisites and initial setup
   - Running all components

2. **[Development Environment Setup](./setup/development.md)**
   - Detailed development environment configuration
   - IDE setup and recommendations
   - Development tools and workflows

3. **[IntelliJ IDEA Configuration](./setup/intellij-run-config.md)**
   - Run configurations for IntelliJ IDEA
   - Debugging setup
   - Recommended plugins

---

## 🏗️ Architecture

Understand the system design and architecture:

### Core Architecture Documents

- **[System Design](./architecture/system-design.md)**
  - Overall system architecture
  - Component diagram
  - Technology stack overview

- **[Data Flow](./architecture/data-flow.md)**
  - How data flows through the system
  - Request/response lifecycle
  - WebSocket data streaming

- **[GraphQL Implementation](./architecture/graphql-implementation.md)**
  - GraphQL schema design
  - Query and mutation patterns
  - Best practices

---

## 🔌 API Documentation

### REST API

- **[REST API Reference](./api/rest-api.md)**
  - All REST endpoints
  - Request/response formats
  - Authentication requirements

### GraphQL API

- **[GraphQL API Reference](./api/graphql-api.md)**
  - GraphQL schema
  - Available queries and mutations
  - Example requests

### WebSocket API

- **[WebSocket API Reference](./api/websocket-api.md)**
  - WebSocket connection setup
  - Message formats
  - Real-time data streaming

---

## 💼 AlgoLab Integration

Complete guides for AlgoLab broker integration:

### Essential Reading

1. **[WebSocket Complete Guide](./algolab/WEBSOCKET_COMPLETE_GUIDE.md)** ⭐ **START HERE**
   - Comprehensive WebSocket integration guide
   - Real-time data streaming
   - CLI integration
   - Troubleshooting
   - **Status:** ✅ Production Ready

### API Integration

2. **[AlgoLab API Endpoints](./algolab/ALGOLAB_API_ENDPOINTS.md)**
   - Complete list of AlgoLab REST API endpoints
   - Request/response examples
   - Error handling

3. **[Authentication Flow](./algolab/ALGOLAB_AUTHENTICATION_FLOW.md)**
   - How to authenticate with AlgoLab
   - Session management
   - OTP/2FA flow

### Development Reference

4. **[Python to Java Mapping](./algolab/PYTHON_TO_JAVA_MAPPING.md)**
   - Mapping Python AlgoLab client to Java implementation
   - Code equivalents
   - Migration guide

---

## 🔐 Security

Security configuration and best practices:

- **[Authority and Role Matrix](./security/authority-role-matrix.md)**
  - Complete list of authorities and roles
  - Permission matrix
  - Access control patterns

- **[Test Authorities](./security/test-authorities.md)**
  - Test users and their authorities
  - Development credentials
  - Testing security scenarios

- **[Security Quick Reference](./security/QUICK-REFERENCE.md)**
  - Common security tasks
  - JWT configuration
  - Quick troubleshooting

---

## 💾 Database

Database schema and setup:

- **[Database Setup Guide](./database/setup-guide.md)**
  - PostgreSQL installation and configuration
  - Database creation
  - Flyway migrations

- **[User Schema](./database/user-schema.md)**
  - User table structure
  - Relationships
  - Authentication data

---

## 🛠️ Setup Guides

Detailed setup instructions for various environments:

### Development

- **[Development Environment](./setup/development.md)**
  - Local development setup
  - Required tools and versions
  - Configuration files

- **[IntelliJ IDEA Setup](./setup/intellij-setup.md)**
  - IDE configuration
  - Code style settings
  - Run configurations

- **[Startup Guide](./setup/startup-guide.md)**
  - Quick start instructions
  - Starting all services
  - Verification steps

### Production

- **[Production Deployment](./setup/production.md)**
  - Production environment setup
  - Docker deployment
  - Environment variables
  - Monitoring and logging

---

## 📊 Sprint Reports

Project progress and sprint documentation:

### Sprint Overview

- **[Sprint Reports Overview](./sprints/sprint-reports-overview.md)**
  - All sprint summaries
  - Overall progress
  - Milestone achievements

### Recent Sprints

- **[Sprint 5 Planning](./sprints/sprint-5-planning.md)**
  - Current sprint objectives
  - Planned features
  - Timeline

- **[Sprint 4 Completion Report](./sprints/sprint-4-completion-report.md)**
  - Sprint 4 achievements
  - Completed features
  - Lessons learned

- **[Sprint Comparison Analysis](./sprints/sprint-comparison-analysis.md)**
  - Progress across sprints
  - Velocity trends
  - Team performance

### Project Status

- **[Issues and Gaps Analysis](./sprints/issues-and-gaps.md)**
  - Current limitations
  - Known issues
  - Resolution roadmap

---

## 📚 Additional Resources

### Main Documentation

- **[Main README (English)](../README.md)**
  - Project overview
  - Quick start
  - Technology stack

- **[Turkish README](../README-TR.md)**
  - Türkçe proje dokümantasyonu
  - Kurulum ve kullanım
  - Özellikler

### External Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [AlgoLab API Documentation](https://www.algolab.com.tr/api-docs)

---

## 🔍 Quick Find

### By Topic

| Topic | Document |
|-------|----------|
| **Getting Started** | [Startup Guide](./setup/startup-guide.md) |
| **WebSocket** | [WebSocket Complete Guide](./algolab/WEBSOCKET_COMPLETE_GUIDE.md) |
| **API Reference** | [REST API](./api/rest-api.md), [GraphQL API](./api/graphql-api.md) |
| **Authentication** | [Authentication Flow](./algolab/ALGOLAB_AUTHENTICATION_FLOW.md) |
| **Database Setup** | [Database Setup Guide](./database/setup-guide.md) |
| **Deployment** | [Production Setup](./setup/production.md) |
| **Security** | [Authority Matrix](./security/authority-role-matrix.md) |

### By User Type

| I am a... | Start here |
|-----------|------------|
| **New Developer** | [Development Setup](./setup/development.md) → [Startup Guide](./setup/startup-guide.md) |
| **Frontend Developer** | [REST API](./api/rest-api.md) → [GraphQL API](./api/graphql-api.md) |
| **Backend Developer** | [System Design](./architecture/system-design.md) → [Data Flow](./architecture/data-flow.md) |
| **DevOps Engineer** | [Production Setup](./setup/production.md) → [Database Setup](./database/setup-guide.md) |
| **Integrator** | [AlgoLab Integration](./algolab/WEBSOCKET_COMPLETE_GUIDE.md) → [API Endpoints](./algolab/ALGOLAB_API_ENDPOINTS.md) |

---

## 📝 Documentation Standards

### Contributing to Documentation

When updating or creating documentation:

1. **Use Markdown** - All documentation should be in Markdown format
2. **Include Examples** - Provide code examples and screenshots where applicable
3. **Keep Updated** - Update last modified date at the bottom of each document
4. **Link Related Docs** - Cross-reference related documentation
5. **Follow Structure** - Use consistent heading structure and formatting

### Template Structure

```markdown
# Document Title

Brief description of what this document covers.

## Table of Contents
- [Section 1](#section-1)
- [Section 2](#section-2)

---

## Section 1
Content...

## Section 2
Content...

---

**Last Updated:** YYYY-MM-DD
**Status:** ✅ Current | 🔄 In Progress | ⚠️ Outdated
```

---

## 🆘 Need Help?

Can't find what you're looking for?

1. **Search the documentation** - Use your editor's search or GitHub search
2. **Check the main README** - [English](../README.md) | [Turkish](../README-TR.md)
3. **Review sprint reports** - Recent changes documented in [sprints/](./sprints/)
4. **Ask the team** - Open an issue on GitHub or contact the dev team

---

**Last Updated:** 2025-10-17
**Maintained by:** BIST Trading Platform Team
