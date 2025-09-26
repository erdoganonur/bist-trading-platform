# Deployment Architecture - BIST Trading Platform

## Overview

This document outlines deployment strategies, infrastructure requirements, and operational procedures for the BIST Trading Platform across different environments (development, staging, production).

## Deployment Environments

### 1. Development Environment

**Purpose**: Local development and testing
**Infrastructure**: Docker Compose on developer machines

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: bist_trading_dev
      POSTGRES_USER: dev_user
      POSTGRES_PASSWORD: dev_password

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    ports: ["9092:9092"]
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
```

**Service Configuration**:
- GraphQL Gateway: `localhost:8090` (NEW!)
- REST API Gateway: `localhost:8080`
- User Management Service: `localhost:8081`
- Order Management Service: `localhost:8082`
- Market Data Service: `localhost:8083`
- Broker Integration Service: `localhost:8084`

### 2. Staging Environment

**Purpose**: Pre-production testing and integration validation
**Infrastructure**: Docker containers on cloud VMs

**Architecture**:
```
┌─────────────────────────────────────────────────────────────┐
│                    Staging Environment                      │
├─────────────────────────────────────────────────────────────┤
│  Load Balancer (nginx)                                     │
│  ├─ graphql-gateway (2 instances)                         │
│  ├─ rest-api-gateway (2 instances)                        │
│  ├─ user-management-service (2 instances)                  │
│  ├─ order-management-service (2 instances)                 │
│  ├─ market-data-service (2 instances)                      │
│  └─ broker-integration-service (2 instances)               │
├─────────────────────────────────────────────────────────────┤
│  Database Layer                                            │
│  ├─ PostgreSQL Primary (with TimescaleDB)                  │
│  ├─ PostgreSQL Replica                                     │
│  ├─ Redis Cluster (3 nodes)                               │
│  └─ Kafka Cluster (3 brokers)                             │
├─────────────────────────────────────────────────────────────┤
│  Monitoring Stack                                          │
│  ├─ Prometheus                                             │
│  ├─ Grafana                                                │
│  └─ Jaeger                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3. Production Environment

**Purpose**: Live trading platform serving real users
**Infrastructure**: Kubernetes cluster with high availability

**Architecture**:
```
┌─────────────────────────────────────────────────────────────┐
│                  Production Environment                     │
├─────────────────────────────────────────────────────────────┤
│  Ingress Controller (AWS ALB / Azure Load Balancer)        │
│  └─ SSL Termination                                        │
├─────────────────────────────────────────────────────────────┤
│  Kubernetes Cluster (3+ nodes)                             │
│  ├─ graphql-gateway (3 replicas)                          │
│  ├─ rest-api-gateway (3 replicas)                         │
│  ├─ user-management-service (3 replicas)                   │
│  ├─ order-management-service (3 replicas)                  │
│  ├─ market-data-service (5 replicas)                       │
│  ├─ broker-integration-service (3 replicas)                │
│  └─ Auto-scaling (HPA)                                     │
├─────────────────────────────────────────────────────────────┤
│  Managed Database Services                                 │
│  ├─ AWS RDS PostgreSQL / Azure Database                    │
│  ├─ TimescaleDB Extension                                  │
│  ├─ Redis ElastiCache / Azure Cache                       │
│  └─ Kafka MSK / Azure Event Hubs                          │
├─────────────────────────────────────────────────────────────┤
│  Monitoring & Security                                     │
│  ├─ CloudWatch / Azure Monitor                            │
│  ├─ Prometheus + Grafana                                   │
│  ├─ Jaeger Tracing                                        │
│  └─ Security Groups / Network Policies                     │
└─────────────────────────────────────────────────────────────┘
```

## Container Strategy

### 1. Docker Images

**Base Image Strategy**:
```dockerfile
# Multi-stage build for production
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /app/platform-services/*/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Non-root user
RUN addgroup --system appgroup && adduser --system --group appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Image Optimization**:
- Multi-stage builds to reduce image size
- Non-root user for security
- Health checks for container orchestration
- Optimized JVM settings for containers

### 2. Service-Specific Configurations

#### User Management Service
```dockerfile
FROM openjdk:21-jre-slim
COPY user-management-service.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8081
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

#### Market Data Service
```dockerfile
FROM openjdk:21-jre-slim
COPY market-data-service.jar app.jar
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8082
# Higher memory for market data processing
ENTRYPOINT ["java", "-Xms1g", "-Xmx2g", "-jar", "app.jar"]
```

#### Broker Integration Service
```dockerfile
FROM openjdk:21-jre-slim
COPY broker-integration-service.jar app.jar
EXPOSE 8083
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8083
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

## Kubernetes Deployment

### 1. Service Manifests

#### User Management Service Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-management-service
  namespace: bist-trading
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-management-service
  template:
    metadata:
      labels:
        app: user-management-service
    spec:
      containers:
      - name: user-management
        image: bist-trading/user-management-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: database-secrets
              key: host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secrets
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1024Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: user-management-service
  namespace: bist-trading
spec:
  selector:
    app: user-management-service
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
  type: ClusterIP
```

#### Market Data Service with Auto-scaling
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: market-data-service
spec:
  replicas: 5
  selector:
    matchLabels:
      app: market-data-service
  template:
    spec:
      containers:
      - name: market-data
        image: bist-trading/market-data-service:1.0.0
        ports:
        - containerPort: 8082
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: market-data-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: market-data-service
  minReplicas: 5
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 2. Configuration Management

#### ConfigMaps
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: bist-trading
data:
  application.yml: |
    spring:
      profiles:
        active: production
      datasource:
        url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
      redis:
        host: ${REDIS_HOST}
        port: 6379
        password: ${REDIS_PASSWORD}

    server:
      port: 8080

    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          probes:
            enabled: true
```

#### Secrets Management
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: database-secrets
  namespace: bist-trading
type: Opaque
data:
  host: cG9zdGdyZXNxbC5leGFtcGxlLmNvbQ==  # base64 encoded
  username: YmlzdF91c2Vy  # base64 encoded
  password: c2VjdXJlX3Bhc3N3b3Jk  # base64 encoded
---
apiVersion: v1
kind: Secret
metadata:
  name: algolab-secrets
  namespace: bist-trading
type: Opaque
data:
  api-key: YWxnb2xhYl9hcGlfa2V5  # base64 encoded
  api-code: YWxnb2xhYl9hcGlfY29kZQ==  # base64 encoded
```

## Infrastructure as Code

### 1. Terraform Configuration

#### AWS Infrastructure
```hcl
# terraform/aws/main.tf
provider "aws" {
  region = var.aws_region
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"

  cluster_name    = "bist-trading-cluster"
  cluster_version = "1.27"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  node_groups = {
    trading_nodes = {
      desired_capacity = 3
      max_capacity     = 10
      min_capacity     = 3

      instance_types = ["m5.large"]

      labels = {
        Environment = var.environment
        Application = "bist-trading"
      }
    }
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "bist-trading-postgres"

  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.r5.xlarge"

  allocated_storage     = 100
  max_allocated_storage = 1000
  storage_encrypted     = true

  db_name  = "bist_trading"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.postgres.name

  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "Sun:04:00-Sun:05:00"

  tags = {
    Name        = "BIST Trading Database"
    Environment = var.environment
  }
}

# ElastiCache Redis
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "bist-trading-redis"
  description                = "Redis cluster for BIST Trading Platform"

  node_type                  = "cache.r6g.large"
  port                      = 6379
  parameter_group_name      = "default.redis7"

  num_cache_clusters        = 3
  automatic_failover_enabled = true
  multi_az_enabled          = true

  subnet_group_name = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  tags = {
    Name        = "BIST Trading Cache"
    Environment = var.environment
  }
}
```

### 2. Monitoring Infrastructure

#### Prometheus Configuration
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'user-management-service'
    static_configs:
      - targets: ['user-management-service:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'market-data-service'
    static_configs:
      - targets: ['market-data-service:8082']
    metrics_path: '/actuator/prometheus'

  - job_name: 'broker-integration-service'
    static_configs:
      - targets: ['broker-integration-service:8083']
    metrics_path: '/actuator/prometheus'
```

#### Grafana Dashboards
```json
{
  "dashboard": {
    "title": "BIST Trading Platform",
    "panels": [
      {
        "title": "Service Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\".*service.*\"}",
            "legendFormat": "{{job}}"
          }
        ]
      },
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{service}} - {{method}}"
          }
        ]
      },
      {
        "title": "Market Data Throughput",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(market_ticks_processed_total[1m])",
            "legendFormat": "Ticks/second"
          }
        ]
      }
    ]
  }
}
```

## Deployment Procedures

### 1. CI/CD Pipeline

#### GitHub Actions Workflow
```yaml
name: Build and Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}

    - name: Run tests
      run: ./gradlew test

    - name: Generate test report
      run: ./test-runner.sh all

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - uses: actions/checkout@v4
    - name: Build Docker images
      run: |
        ./build.sh
        docker build -t bist-trading/user-management-service:${{ github.sha }} \
          -f docker/user-management-service.Dockerfile .
        docker build -t bist-trading/market-data-service:${{ github.sha }} \
          -f docker/market-data-service.Dockerfile .
        docker build -t bist-trading/broker-integration-service:${{ github.sha }} \
          -f docker/broker-integration-service.Dockerfile .

    - name: Push to registry
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker push bist-trading/user-management-service:${{ github.sha }}
        docker push bist-trading/market-data-service:${{ github.sha }}
        docker push bist-trading/broker-integration-service:${{ github.sha }}

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - name: Deploy to staging
      run: |
        kubectl set image deployment/user-management-service \
          user-management=bist-trading/user-management-service:${{ github.sha }} \
          --namespace=bist-trading-staging

        kubectl set image deployment/market-data-service \
          market-data=bist-trading/market-data-service:${{ github.sha }} \
          --namespace=bist-trading-staging

        kubectl set image deployment/broker-integration-service \
          broker-integration=bist-trading/broker-integration-service:${{ github.sha }} \
          --namespace=bist-trading-staging
```

### 2. Blue-Green Deployment

#### Deployment Strategy
```bash
#!/bin/bash
# deploy-blue-green.sh

NEW_VERSION=$1
CURRENT_COLOR=$(kubectl get service production-service -o jsonpath='{.spec.selector.color}')

if [ "$CURRENT_COLOR" = "blue" ]; then
    NEW_COLOR="green"
else
    NEW_COLOR="blue"
fi

echo "Deploying version $NEW_VERSION to $NEW_COLOR environment"

# Update the deployment
kubectl set image deployment/bist-trading-$NEW_COLOR \
    app=bist-trading:$NEW_VERSION

# Wait for deployment to be ready
kubectl rollout status deployment/bist-trading-$NEW_COLOR

# Run health checks
if ./health-check.sh $NEW_COLOR; then
    echo "Health checks passed, switching traffic"

    # Switch service selector
    kubectl patch service production-service -p '{"spec":{"selector":{"color":"'$NEW_COLOR'"}}}'

    echo "Traffic switched to $NEW_COLOR"

    # Scale down old deployment
    kubectl scale deployment/bist-trading-$CURRENT_COLOR --replicas=0
else
    echo "Health checks failed, rolling back"
    kubectl scale deployment/bist-trading-$NEW_COLOR --replicas=0
    exit 1
fi
```

### 3. Database Migrations

#### Flyway Migration Strategy
```bash
#!/bin/bash
# migrate-database.sh

ENVIRONMENT=$1
DB_HOST=$2

echo "Running database migrations for $ENVIRONMENT"

# Backup database before migration
pg_dump -h $DB_HOST -U postgres bist_trading > backup_$(date +%Y%m%d_%H%M%S).sql

# Run Flyway migrations
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://$DB_HOST:5432/bist_trading

if [ $? -eq 0 ]; then
    echo "Database migration completed successfully"
else
    echo "Database migration failed, consider rollback"
    exit 1
fi
```

## Security Considerations

### 1. Network Security

**Security Groups (AWS) / Network Policies (Kubernetes)**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: bist-trading-network-policy
spec:
  podSelector:
    matchLabels:
      app: bist-trading
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx-ingress
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
```

### 2. Secrets Management

**Kubernetes Secrets with External Secrets Operator**:
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-secret
spec:
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: database-secret
    creationPolicy: Owner
  data:
  - secretKey: password
    remoteRef:
      key: bist-trading/database
      property: password
```

### 3. SSL/TLS Configuration

**Ingress with SSL Termination**:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bist-trading-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.bist-trading.com
    secretName: bist-trading-tls
  rules:
  - host: api.bist-trading.com
    http:
      paths:
      - path: /user-management
        pathType: Prefix
        backend:
          service:
            name: user-management-service
            port:
              number: 8081
      - path: /market-data
        pathType: Prefix
        backend:
          service:
            name: market-data-service
            port:
              number: 8082
```

## Disaster Recovery

### 1. Backup Strategy

**Database Backups**:
```bash
#!/bin/bash
# backup-database.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="bist_trading_backup_$TIMESTAMP.sql"

# Full database backup
pg_dump -h $DB_HOST -U postgres bist_trading > $BACKUP_FILE

# Upload to S3
aws s3 cp $BACKUP_FILE s3://bist-trading-backups/database/

# Retain backups for 30 days
aws s3api put-object-tagging \
  --bucket bist-trading-backups \
  --key database/$BACKUP_FILE \
  --tagging 'TagSet=[{Key=RetentionDays,Value=30}]'
```

### 2. Recovery Procedures

**Service Recovery**:
```bash
#!/bin/bash
# disaster-recovery.sh

echo "Starting disaster recovery procedure"

# 1. Restore database from latest backup
LATEST_BACKUP=$(aws s3 ls s3://bist-trading-backups/database/ | tail -n 1 | awk '{print $4}')
aws s3 cp s3://bist-trading-backups/database/$LATEST_BACKUP ./
psql -h $DB_HOST -U postgres -d bist_trading < $LATEST_BACKUP

# 2. Deploy services to new cluster
kubectl apply -f k8s/

# 3. Verify service health
./health-check.sh production

# 4. Update DNS to point to new cluster
# (Manual step or automated with external-dns)

echo "Disaster recovery completed"
```

---

**Last Updated:** September 2024
**Version:** 1.0
**Maintainer:** BIST Trading Platform Team