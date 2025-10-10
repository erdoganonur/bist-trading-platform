# IntelliJ IDEA Setup Guide for BIST Trading Platform

## Overview

This guide will help you set up IntelliJ IDEA to run the **REAL Monolith** BIST Trading Platform with Docker infrastructure services.

**Architecture**: All functionality is now consolidated into a single Spring Boot application with standard `src/` directory structure and unified deployment.

## Prerequisites

- **Java 21 LTS** (OpenJDK or Oracle JDK)
- **IntelliJ IDEA** (Community or Ultimate Edition)
- **Docker Desktop** (for infrastructure services)
- **Git** (for version control)

## Quick Setup

### 1. Start Infrastructure Services

First, start the required infrastructure services using Docker:

```bash
# Navigate to project directory
cd /path/to/bist-trading-platform

# Start infrastructure services (PostgreSQL, Redis, Kafka)
docker-compose up -d postgres redis zookeeper kafka

# Verify services are running and healthy
docker-compose ps
```

Expected output:
```
NAME             STATUS                    PORTS
bist-kafka       Up (healthy)             0.0.0.0:29092->29092/tcp
bist-postgres    Up (healthy)             0.0.0.0:5432->5432/tcp
bist-redis       Up (healthy)             0.0.0.0:6379->6379/tcp
bist-zookeeper   Up                       2181/tcp, 2888/tcp, 3888/tcp
```

### 2. IntelliJ IDEA Configuration

#### Import Project
1. Open IntelliJ IDEA
2. Choose **"Open or Import"**
3. Navigate to the project directory: `/path/to/bist-trading-platform`
4. Select the project folder and click **"Open"**
5. IntelliJ will detect the Gradle project and import it automatically

#### Configure Java SDK
1. Go to **File → Project Structure → Project**
2. Set **Project SDK** to **Java 21**
3. Set **Project language level** to **21 - Records, patterns, local enums and interfaces**

#### Configure Run Configuration

1. Go to **Run → Edit Configurations**
2. Click **"+"** and select **"Application"**
3. Configure as follows:

**Main Configuration:**
- **Name**: `BIST Trading Platform (REAL Monolith)`
- **Main class**: `com.bisttrading.BistTradingPlatformApplication`
- **Working directory**: `/path/to/bist-trading-platform`

**Environment Variables:**
Add these environment variables (copy/paste all at once):
```
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_USERNAME=bist_user
SPRING_DATASOURCE_PASSWORD=bist_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
SPRING_FLYWAY_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=update
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long
SERVER_PORT=8080
```

**Optional - If using Redis (uncomment in application.yml):**
```
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=redis_password
```

**Program arguments**: (leave empty)

**VM options** (optional for debugging):
```
-Xmx2g -Xms1g -XX:+UseG1GC
```

4. Click **"Apply"** and **"OK"**

### 3. Run the Application

1. Select the **"BIST Trading Platform (REAL Monolith)"** run configuration
2. Click the **Run** button (▶️) or press **Shift+F10**
3. Wait for the application to start (usually 15-30 seconds)

**Success indicators:**
- Console shows: `Started BistTradingPlatformApplication in X.XXX seconds`
- No error messages about database connectivity
- Application is accessible at `http://localhost:8080`

### 4. Verify Setup

Test the application endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation
open http://localhost:8080/swagger-ui.html

# Application info
curl http://localhost:8080/actuator/info
```

## Development Tips

### Database Management

**View database contents:**
```bash
# Connect to PostgreSQL
docker exec -it bist-postgres psql -U bist_user -d bist_trading

# List tables
\dt

# Exit
\q
```

### Redis Management

**View Redis contents:**
```bash
# Connect to Redis
docker exec -it bist-redis redis-cli -a redis_password

# List all keys
KEYS *

# Exit
quit
```

### Hot Reload

IntelliJ IDEA supports automatic compilation:
1. Go to **File → Settings → Build, Execution, Deployment → Compiler**
2. Check **"Build project automatically"**
3. Enable **"Allow auto-make to start even if developed application is currently running"**

### Debug Mode

1. Click the **Debug** button (🐛) instead of Run
2. Set breakpoints by clicking in the gutter next to line numbers
3. Use standard IntelliJ debugging features

### Environment Profiles

Switch between profiles by changing `SPRING_PROFILES_ACTIVE`:
- `dev` - Development mode (default)
- `docker` - For Docker deployment
- `prod` - Production mode (if configured)

## Troubleshooting

### Common Issues

**1. "Connection refused" to PostgreSQL**
```bash
# Check if containers are running
docker-compose ps

# Restart PostgreSQL if needed
docker-compose restart postgres
```

**2. "Port 8080 already in use"**
- Change `SERVER_PORT` environment variable to `8081` or another free port
- Or stop any existing services on port 8080

**3. OutOfMemoryError**
- Increase VM heap size: `-Xmx4g -Xms2g`
- Close other memory-intensive applications

**4. Build failures**
```bash
# Clean and rebuild
./gradlew clean build

# Or from IntelliJ: Build → Clean, then Build → Rebuild Project
```

**5. Database schema issues**
- Set `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop` to recreate schema
- Or manually drop tables and restart

### Infrastructure Commands

**Stop all services:**
```bash
docker-compose down
```

**Start only infrastructure (no app):**
```bash
docker-compose up -d postgres redis zookeeper kafka
```

**View logs:**
```bash
# PostgreSQL logs
docker logs bist-postgres

# Redis logs
docker logs bist-redis

# All services
docker-compose logs -f
```

**Clean restart:**
```bash
# Stop and remove everything including volumes
docker-compose down -v

# Start fresh
docker-compose up -d postgres redis zookeeper kafka
```

## Architecture Notes

### REAL Monolith Benefits

- **🎯 Single Application**: One JAR, one process, one port (8080)
- **📁 Standard Structure**: Maven-like src/ directory layout
- **🔧 Simple Build**: One build.gradle, no complex modules
- **🚀 Easy Development**: Standard IntelliJ project setup
- **⚡ Fast Startup**: No inter-service communication overhead
- **🔐 Integrated Security**: JWT authentication built-in

### Key Components

All functionality is integrated into a single application:
- **User Management**: Authentication, authorization, profiles
- **Market Data**: Real-time data processing and analysis
- **Trading Engine**: Order management and execution
- **Broker Integration**: AlgoLab integration (mock implementation)
- **Security**: JWT-based authentication and RBAC

### External Dependencies

- **PostgreSQL**: Primary database with TimescaleDB for time-series data
- **Redis**: Caching and session storage (optional)
- **Kafka**: Event streaming and messaging (optional)

## Next Steps

1. **Explore the API**: Use Swagger UI at `http://localhost:8080/swagger-ui.html`
2. **Run Tests**: Use IntelliJ's test runner or `./gradlew test`
3. **Monitor**: Access health endpoints at `http://localhost:8080/actuator/*`
4. **Develop**: Start implementing new features in the standard src/ structure

For more detailed documentation, see:
- [System Architecture](./docs/architecture/system-design.md)
- [API Documentation](./docs/api/rest-api.md)
- [Development Guide](./docs/setup/development.md)