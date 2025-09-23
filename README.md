# BIST Trading Platform

A professional trading platform for Borsa Istanbul (BIST) built with modern Java technologies and designed as a modular monolith ready for microservices transition.

## 🚀 Technology Stack

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.0
- **Build Tool**: Gradle 8.8
- **Database**: PostgreSQL 16 with TimescaleDB extension
- **Cache**: Redis 7.4
- **Messaging**: Apache Kafka 3.8
- **Monitoring**: Prometheus + Grafana
- **Tracing**: Jaeger
- **Container**: Docker & Docker Compose
- **CI/CD**: GitHub Actions

## 🏗️ Architecture

The project follows a **modular monolith** architecture organized into the following modules:

### Platform Core
- `core-common`: Shared utilities, exceptions, DTOs, and common functionality
- `core-domain`: Domain models, entities, and business logic
- `core-security`: Security configurations, JWT handling, and authentication
- `core-messaging`: Kafka integration and event handling

### Platform Infrastructure
- `infrastructure-persistence`: Database access, repositories, and data persistence
- `infrastructure-integration`: External API integrations and HTTP clients
- `infrastructure-monitoring`: Metrics, health checks, and observability

### Platform Services
- `user-management-service`: User authentication, authorization, and profile management

## 📁 Project Structure

```
bist-trading-platform/
├── platform-core/
│   ├── core-common/
│   ├── core-domain/
│   ├── core-security/
│   └── core-messaging/
├── platform-infrastructure/
│   ├── infrastructure-persistence/
│   ├── infrastructure-integration/
│   └── infrastructure-monitoring/
├── platform-services/
│   └── user-management-service/
├── docker/
├── .github/workflows/
├── docker-compose.yml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 🛠️ Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** (OpenJDK or Oracle JDK)
- **Docker** (20.10.0 or later)
- **Docker Compose** (2.0.0 or later)
- **Git**

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/bist-trading-platform.git
cd bist-trading-platform
```

### 2. Environment Setup

Copy the example environment file and configure it:

```bash
cp .env.example .env
```

Edit the `.env` file with your specific configuration:

```bash
# Database Configuration
POSTGRES_DB=bist_trading
POSTGRES_USER=bist_user
POSTGRES_PASSWORD=your_secure_password

# Redis Configuration
REDIS_PASSWORD=your_redis_password

# Security Configuration
JWT_SECRET=your-256-bit-secret-key-here
```

### 3. Start Infrastructure Services

Start all required infrastructure services using Docker Compose:

```bash
# Start core services (PostgreSQL, Redis, Kafka)
docker-compose up -d postgres redis kafka zookeeper

# Start with monitoring (optional)
docker-compose --profile monitoring up -d

# Start with development tools (optional)
docker-compose --profile dev up -d
```

### 4. Build the Application

```bash
# Grant execute permissions to gradlew (Unix/Linux/macOS)
chmod +x gradlew

# Build all modules
./gradlew build

# Or build without tests for faster startup
./gradlew build -x test
```

### 5. Run Database Migrations

```bash
./gradlew flywayMigrate
```

### 6. Start the Application

```bash
# Run the user management service
./gradlew :platform-services:user-management-service:bootRun
```

The application will start on `http://localhost:8080`

## 🔧 Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :platform-core:core-common:test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Code Quality

```bash
# Run code quality checks
./gradlew check

# Generate code coverage report
./gradlew aggregateJacocoReport

# Run SonarQube analysis (requires SonarQube server)
./gradlew sonar
```

### Working with Docker

```bash
# Start only database and cache
docker-compose up -d postgres redis

# View logs
docker-compose logs -f postgres

# Stop all services
docker-compose down

# Clean up volumes (⚠️ This will delete all data)
docker-compose down -v
```

## 📊 Monitoring & Management

### Health Checks

- Application Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus Metrics: `http://localhost:8080/actuator/prometheus`

### Management UIs

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Grafana**: `http://localhost:3000` (admin/admin)
- **Prometheus**: `http://localhost:9090`
- **Kafka UI**: `http://localhost:8080` (development profile)
- **pgAdmin**: `http://localhost:5050` (development profile)
- **Jaeger**: `http://localhost:16686` (monitoring profile)

## 🏃‍♂️ Running Different Profiles

### Development Profile

```bash
# Start with development tools
docker-compose --profile dev up -d

# Run application with dev profile
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### Production Profile

```bash
# Build production-ready JAR
./gradlew bootJar

# Run with production profile
SPRING_PROFILES_ACTIVE=prod java -jar platform-services/user-management-service/build/libs/user-management-service-1.0.0-SNAPSHOT.jar
```

## 🧪 Testing

### Integration Tests

```bash
# Run integration tests (uses TestContainers)
./gradlew integrationTest
```

### Performance Tests

```bash
# Run performance tests
./gradlew performanceTest
```

## 📦 Deployment

### Building Docker Image

```bash
# Build application Docker image
docker build -t bist-trading-platform:latest .
```

### Environment-Specific Deployments

The application supports multiple environments through Spring profiles:

- **dev**: Development environment with debug logging and relaxed security
- **test**: Testing environment with in-memory database
- **prod**: Production environment with optimized settings

## 🔒 Security

### JWT Configuration

The application uses JWT tokens for authentication. Configure the JWT secret in your environment:

```bash
JWT_SECRET=your-very-secure-256-bit-secret-key-here
JWT_EXPIRATION=86400000  # 24 hours
JWT_REFRESH_EXPIRATION=604800000  # 7 days
```

### Database Security

- Use strong passwords for database connections
- Configure PostgreSQL to use SSL in production
- Regularly update database credentials

## 🐛 Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check if ports are already in use
   lsof -i :8080  # Application port
   lsof -i :5432  # PostgreSQL port
   lsof -i :6379  # Redis port
   ```

2. **Database Connection Issues**
   ```bash
   # Check if PostgreSQL is running
   docker-compose logs postgres

   # Test connection
   docker-compose exec postgres psql -U bist_user -d bist_trading
   ```

3. **Memory Issues**
   ```bash
   # Increase JVM memory
   export JAVA_OPTS="-Xmx2g -Xms1g"
   ./gradlew bootRun
   ```

### Logs

Application logs are available at:
- Development: Console output
- Production: `/var/log/bist-trading/user-management-service.log`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

The project uses:
- **Checkstyle** for code formatting
- **SpotBugs** for bug detection
- **PMD** for code analysis

Run code quality checks before committing:

```bash
./gradlew check
```

## 📋 TODO

- [ ] Implement order management service
- [ ] Add real-time market data integration
- [ ] Implement portfolio management
- [ ] Add advanced analytics and reporting
- [ ] Implement notification service
- [ ] Add mobile API endpoints
- [ ] Implement audit trail
- [ ] Add comprehensive documentation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:

- Create an issue in the repository
- Contact the development team
- Check the documentation in the `/docs` folder

## 🔗 Useful Links

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)