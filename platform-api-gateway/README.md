# BIST Trading Platform API Gateway

## Overview

The API Gateway serves as the single entry point for all client requests to the BIST Trading Platform microservices architecture. It provides routing, authentication, rate limiting, monitoring, and other cross-cutting concerns.

## Features

### Core Features
- **Dynamic Routing**: Route requests to appropriate backend services based on path patterns
- **Load Balancing**: Distribute requests across multiple service instances
- **Circuit Breaker**: Protect backend services from cascade failures
- **Rate Limiting**: Redis-based distributed rate limiting with multiple strategies
- **CORS Support**: Cross-origin resource sharing for web applications

### Security
- **JWT Authentication**: Validate JWT tokens from Authorization header or cookies
- **Route-based Authorization**: Apply different security policies per route
- **Request Validation**: Input sanitization and validation filters
- **Security Headers**: Add standard security headers to responses

### Monitoring & Observability
- **Request/Response Logging**: Comprehensive logging with correlation IDs
- **Metrics Collection**: Prometheus metrics for monitoring
- **Health Checks**: Multiple health check endpoints for load balancers
- **Distributed Tracing**: Zipkin integration for request tracing

## Quick Start

### Prerequisites
- Java 21 or higher
- Docker and Docker Compose
- Redis (for rate limiting)

### Running Locally

1. **Start Redis**:
   ```bash
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   ```

2. **Build and Run Gateway**:
   ```bash
   ./gradlew bootRun
   ```

3. **Or using Docker Compose**:
   ```bash
   docker-compose up -d
   ```

### Running with Docker

Build the Docker image:
```bash
# Using Gradle Jib plugin
./gradlew jibDockerBuild

# Or using Dockerfile
docker build -t bist-trading/api-gateway .
```

## Configuration

### Application Profiles

- **dev**: Development profile with detailed logging
- **staging**: Staging profile with external service discovery
- **prod**: Production profile with clustering and optimized settings

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SERVER_PORT` | Gateway port | `8080` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `LOGGING_LEVEL_COM_BISTTRADING` | Log level | `INFO` |

### Route Configuration

Routes are defined in two ways:

1. **Declarative (application.yml)**:
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: user-management
             uri: http://localhost:8081
             predicates:
               - Path=/api/v1/users/**,/api/v1/auth/**
   ```

2. **Programmatic (RouteConfiguration.java)**:
   ```java
   .route("auth-login", r -> r
       .path("/api/v1/auth/login")
       .and().method(HttpMethod.POST)
       .uri("http://localhost:8081"))
   ```

## API Endpoints

### Health Endpoints
- `GET /health` - Simple health check
- `GET /health/detailed` - Detailed health with dependencies
- `GET /health/ready` - Readiness probe
- `GET /health/live` - Liveness probe

### Actuator Endpoints
- `GET /actuator/health` - Spring Boot health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /actuator/gateway/routes` - Current routes

### Gateway Routes

| Path Pattern | Target Service | Description |
|--------------|----------------|-------------|
| `/api/v1/auth/**` | user-management-service | Authentication |
| `/api/v1/users/**` | user-management-service | User management |
| `/api/v1/orders/**` | order-management-service | Order operations |
| `/api/v1/positions/**` | order-management-service | Position tracking |
| `/api/v1/market-data/**` | market-data-service | Market data |
| `/api/v1/symbols/**` | market-data-service | Symbol information |

## Rate Limiting

Multiple rate limiting strategies:

### User-based Rate Limiting
- **Limit**: 100 requests/second per user
- **Burst**: 200 requests
- **Key**: JWT subject (user ID)

### IP-based Rate Limiting
- **Limit**: 50 requests/second per IP
- **Burst**: 100 requests
- **Key**: Client IP address

### Admin Rate Limiting
- **Limit**: 10 requests/second per admin
- **Burst**: 20 requests
- **Key**: IP + User-Agent hash

## Monitoring

### Metrics
Gateway exposes the following custom metrics:
- `gateway.requests.total` - Total requests by method, URI, status
- `gateway.request.duration` - Request processing time
- `gateway.responses.total` - Response count by status class

### Logging
Structured logging with:
- Correlation IDs for request tracing
- Request/response details (configurable level)
- Performance metrics (slow request detection)
- Security events (authentication failures, rate limiting)

## Security

### JWT Authentication
- **Algorithm**: RS256 (RSA with SHA-256)
- **Token Location**: Authorization header or cookies
- **Claims**: Standard JWT claims + custom user information
- **Validation**: Signature, expiration, issuer, audience

### CORS Configuration
Pre-configured CORS settings for:
- Development: `http://localhost:*`
- Production: `https://*.bisttrading.com.tr`

## Development

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Code Quality
```bash
./gradlew check
```

### Docker Image
```bash
./gradlew jibDockerBuild
```

## Deployment

### Kubernetes
Kubernetes manifests are available in the `k8s/` directory:
```bash
kubectl apply -f k8s/
```

### Docker Compose
```bash
docker-compose up -d
```

### Environment-specific Configurations

Production deployment requires:
- Redis cluster for rate limiting
- Service discovery (Eureka/Consul)
- Load balancer health checks
- SSL/TLS termination
- Monitoring stack (Prometheus, Grafana)

## Troubleshooting

### Common Issues

1. **503 Service Unavailable**
   - Check backend service health
   - Verify service discovery configuration
   - Check network connectivity

2. **429 Too Many Requests**
   - Review rate limiting configuration
   - Check Redis connectivity
   - Verify client request patterns

3. **401 Unauthorized**
   - Validate JWT token format
   - Check token expiration
   - Verify signing key configuration

### Debug Mode
Enable debug logging:
```yaml
logging:
  level:
    com.bisttrading.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │  Mobile Client  │    │ Third-party API │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │   API Gateway           │
                    │ ┌─────────────────────┐ │
                    │ │   Rate Limiting     │ │
                    │ │   Authentication    │ │
                    │ │   Logging          │ │
                    │ │   Circuit Breaker  │ │
                    │ └─────────────────────┘ │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
 ┌────────┴────────┐   ┌────────┴────────┐   ┌────────┴────────┐
 │ User Management │   │ Market Data     │   │ Order Management│
 │ Service         │   │ Service         │   │ Service         │
 └─────────────────┘   └─────────────────┘   └─────────────────┘
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Run quality checks
6. Submit a pull request

## License

Copyright (c) 2024 BIST Trading Platform. All rights reserved.