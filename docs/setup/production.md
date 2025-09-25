# Production Setup Guide - BIST Trading Platform

## Overview

This guide provides comprehensive instructions for deploying the BIST Trading Platform in production environments. It covers infrastructure setup, security configurations, monitoring, and operational procedures for a high-availability, scalable trading system.

## Production Requirements

### Infrastructure Requirements

#### Minimum Specifications
| Component | Specification | Purpose |
|-----------|---------------|---------|
| **Application Servers** | 4 CPU cores, 8 GB RAM | Service hosting |
| **Database Server** | 8 CPU cores, 32 GB RAM, 500 GB SSD | PostgreSQL + TimescaleDB |
| **Cache Server** | 4 CPU cores, 16 GB RAM | Redis cluster |
| **Load Balancer** | 2 CPU cores, 4 GB RAM | Traffic distribution |
| **Monitoring Stack** | 4 CPU cores, 8 GB RAM | Prometheus, Grafana, Jaeger |

#### Recommended Specifications (High Load)
| Component | Specification | Purpose |
|-----------|---------------|---------|
| **Application Servers** | 8 CPU cores, 16 GB RAM | Enhanced performance |
| **Database Server** | 16 CPU cores, 64 GB RAM, 1 TB NVMe SSD | High-performance storage |
| **Cache Cluster** | 3 nodes: 8 cores, 32 GB RAM each | Redis HA cluster |
| **Load Balancer** | HA pair: 4 cores, 8 GB RAM each | High availability |

### Network Requirements

#### Firewall Rules
| Service | Port | Source | Purpose |
|---------|------|--------|---------|
| **Load Balancer** | 80, 443 | Internet | HTTP/HTTPS traffic |
| **User Management** | 8081 | Load Balancer | Internal API |
| **Market Data** | 8082 | Load Balancer | Internal API |
| **Broker Integration** | 8083 | Load Balancer | Internal API |
| **PostgreSQL** | 5432 | App Servers | Database access |
| **Redis** | 6379 | App Servers | Cache access |
| **Prometheus** | 9090 | Monitoring | Metrics collection |
| **Grafana** | 3000 | Admins | Monitoring dashboard |
| **SSH** | 22 | Admin IPs | Server management |

#### DNS Configuration
```
# Production domains
api.bist-trading.com        → Load Balancer
user-api.bist-trading.com   → User Management Service
market.bist-trading.com     → Market Data Service
trading.bist-trading.com    → Broker Integration Service
monitor.bist-trading.com    → Grafana Dashboard
```

## Cloud Infrastructure Setup

### AWS Deployment

#### VPC and Network Setup
```hcl
# terraform/aws/vpc.tf
resource "aws_vpc" "bist_trading_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "BIST Trading VPC"
    Environment = "production"
  }
}

resource "aws_subnet" "public_subnets" {
  count             = 2
  vpc_id            = aws_vpc.bist_trading_vpc.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  map_public_ip_on_launch = true

  tags = {
    Name = "Public Subnet ${count.index + 1}"
  }
}

resource "aws_subnet" "private_subnets" {
  count             = 2
  vpc_id            = aws_vpc.bist_trading_vpc.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "Private Subnet ${count.index + 1}"
  }
}
```

#### EKS Cluster Configuration
```hcl
# terraform/aws/eks.tf
module "eks" {
  source = "terraform-aws-modules/eks/aws"

  cluster_name    = "bist-trading-prod"
  cluster_version = "1.28"

  vpc_id     = aws_vpc.bist_trading_vpc.id
  subnet_ids = aws_subnet.private_subnets[*].id

  # OIDC Identity provider
  cluster_identity_providers = {
    sts = {
      client_id = "sts.amazonaws.com"
    }
  }

  node_groups = {
    application_nodes = {
      name           = "application"
      instance_types = ["m5.xlarge"]

      scaling_config = {
        desired_size = 3
        max_size     = 10
        min_size     = 3
      }

      labels = {
        NodeType = "application"
      }

      taints = {
        application = {
          key    = "application"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      }
    }

    database_nodes = {
      name           = "database"
      instance_types = ["r5.2xlarge"]

      scaling_config = {
        desired_size = 2
        max_size     = 4
        min_size     = 2
      }

      labels = {
        NodeType = "database"
      }
    }
  }

  tags = {
    Environment = "production"
    Project     = "bist-trading"
  }
}
```

#### RDS PostgreSQL Setup
```hcl
# terraform/aws/rds.tf
resource "aws_db_instance" "postgres" {
  identifier = "bist-trading-postgres-prod"

  # Database configuration
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.r5.2xlarge"

  # Storage configuration
  allocated_storage     = 500
  max_allocated_storage = 2000
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id           = aws_kms_key.database.arn

  # Database settings
  db_name  = "bist_trading"
  username = var.db_username
  password = var.db_password
  port     = 5432

  # Network configuration
  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  publicly_accessible    = false

  # Backup configuration
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "Sun:04:00-Sun:05:00"

  # High availability
  multi_az = true

  # Monitoring
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_enhanced_monitoring.arn

  # Performance insights
  performance_insights_enabled = true
  performance_insights_retention_period = 7

  tags = {
    Name        = "BIST Trading Database"
    Environment = "production"
  }
}

# TimescaleDB extension (manual setup required)
resource "null_resource" "timescaledb_setup" {
  depends_on = [aws_db_instance.postgres]

  provisioner "local-exec" {
    command = <<-EOT
      psql -h ${aws_db_instance.postgres.endpoint} \
           -U ${var.db_username} \
           -d ${aws_db_instance.postgres.db_name} \
           -c "CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;"
    EOT
  }
}
```

#### ElastiCache Redis Configuration
```hcl
# terraform/aws/elasticache.tf
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "bist-trading-redis-prod"
  description                = "Redis cluster for BIST Trading Platform"

  # Node configuration
  node_type                  = "cache.r6g.xlarge"
  port                      = 6379
  parameter_group_name      = "default.redis7"

  # Cluster configuration
  num_cache_clusters        = 3
  automatic_failover_enabled = true
  multi_az_enabled          = true

  # Network configuration
  subnet_group_name = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  # Security
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = var.redis_auth_token

  # Backup configuration
  snapshot_retention_limit = 5
  snapshot_window         = "03:00-05:00"

  # Maintenance
  maintenance_window = "sun:05:00-sun:07:00"

  tags = {
    Name        = "BIST Trading Cache"
    Environment = "production"
  }
}
```

### Kubernetes Deployment

#### Namespace and RBAC
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: bist-trading-prod
  labels:
    name: bist-trading-prod
    environment: production
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: bist-trading-sa
  namespace: bist-trading-prod
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: bist-trading-prod
  name: bist-trading-role
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "secrets"]
  verbs: ["get", "watch", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: bist-trading-binding
  namespace: bist-trading-prod
subjects:
- kind: ServiceAccount
  name: bist-trading-sa
  namespace: bist-trading-prod
roleRef:
  kind: Role
  name: bist-trading-role
  apiGroup: rbac.authorization.k8s.io
```

#### ConfigMaps and Secrets
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: bist-trading-prod
data:
  application.yml: |
    spring:
      profiles:
        active: production

      datasource:
        url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000

      redis:
        host: ${REDIS_HOST}
        port: 6379
        password: ${REDIS_PASSWORD}
        timeout: 2000ms
        lettuce:
          pool:
            max-active: 20
            max-idle: 8
            min-idle: 2

    # Security configuration
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration: 900000
      refresh-token-expiration: 604800000

    # AlgoLab configuration
    algolab:
      api:
        url: ${ALGOLAB_API_URL}
        key: ${ALGOLAB_API_KEY}
        code: ${ALGOLAB_API_CODE}
      timeout:
        connect: PT30S
        read: PT60S

    # Logging configuration
    logging:
      level:
        root: INFO
        com.bisttrading: INFO
      pattern:
        console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: bist-trading-prod
type: Opaque
data:
  DB_HOST: # base64 encoded database host
  DB_NAME: # base64 encoded database name
  DB_USERNAME: # base64 encoded database username
  DB_PASSWORD: # base64 encoded database password
  REDIS_HOST: # base64 encoded redis host
  REDIS_PASSWORD: # base64 encoded redis password
  JWT_SECRET: # base64 encoded JWT secret
  ALGOLAB_API_URL: # base64 encoded AlgoLab API URL
  ALGOLAB_API_KEY: # base64 encoded AlgoLab API key
  ALGOLAB_API_CODE: # base64 encoded AlgoLab API code
```

#### Service Deployments

**User Management Service**:
```yaml
# k8s/user-management-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-management-service
  namespace: bist-trading-prod
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
      serviceAccountName: bist-trading-sa
      containers:
      - name: user-management
        image: bist-trading/user-management-service:1.0.0
        ports:
        - containerPort: 8081
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: logs
        emptyDir: {}
      nodeSelector:
        NodeType: application
      tolerations:
      - key: application
        operator: Equal
        value: "true"
        effect: NoSchedule
---
apiVersion: v1
kind: Service
metadata:
  name: user-management-service
  namespace: bist-trading-prod
spec:
  selector:
    app: user-management-service
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
  type: ClusterIP
```

**Market Data Service (High Performance)**:
```yaml
# k8s/market-data-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: market-data-service
  namespace: bist-trading-prod
spec:
  replicas: 5
  selector:
    matchLabels:
      app: market-data-service
  template:
    metadata:
      labels:
        app: market-data-service
    spec:
      serviceAccountName: bist-trading-sa
      containers:
      - name: market-data
        image: bist-trading/market-data-service:1.0.0
        ports:
        - containerPort: 8082
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 90
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 45
          periodSeconds: 10
      nodeSelector:
        NodeType: application
      tolerations:
      - key: application
        operator: Equal
        value: "true"
        effect: NoSchedule
---
apiVersion: v1
kind: Service
metadata:
  name: market-data-service
  namespace: bist-trading-prod
spec:
  selector:
    app: market-data-service
  ports:
  - port: 8082
    targetPort: 8082
    protocol: TCP
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: market-data-hpa
  namespace: bist-trading-prod
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

## Load Balancer and Ingress

### Ingress Configuration
```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bist-trading-ingress
  namespace: bist-trading-prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.bist-trading.com
    - user-api.bist-trading.com
    - market.bist-trading.com
    - trading.bist-trading.com
    secretName: bist-trading-tls
  rules:
  - host: user-api.bist-trading.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: user-management-service
            port:
              number: 8081
  - host: market.bist-trading.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: market-data-service
            port:
              number: 8082
  - host: trading.bist-trading.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: broker-integration-service
            port:
              number: 8083
  - host: api.bist-trading.com
    http:
      paths:
      - path: /user
        pathType: Prefix
        backend:
          service:
            name: user-management-service
            port:
              number: 8081
      - path: /market
        pathType: Prefix
        backend:
          service:
            name: market-data-service
            port:
              number: 8082
      - path: /trading
        pathType: Prefix
        backend:
          service:
            name: broker-integration-service
            port:
              number: 8083
```

### SSL/TLS Certificate Management
```yaml
# k8s/certificate.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@bist-trading.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: bist-trading-cert
  namespace: bist-trading-prod
spec:
  secretName: bist-trading-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - api.bist-trading.com
  - user-api.bist-trading.com
  - market.bist-trading.com
  - trading.bist-trading.com
```

## Security Configuration

### Network Security Policies
```yaml
# k8s/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: bist-trading-network-policy
  namespace: bist-trading-prod
spec:
  podSelector:
    matchLabels:
      app: user-management-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    - podSelector:
        matchLabels:
          app: market-data-service
    ports:
    - protocol: TCP
      port: 8081
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

### Pod Security Standards
```yaml
# k8s/pod-security.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: bist-trading-prod
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### Secret Management with External Secrets
```yaml
# k8s/external-secrets.yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: bist-trading-prod
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
  namespace: bist-trading-prod
spec:
  refreshInterval: 15s
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: database-secret
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: bist-trading/database
      property: username
  - secretKey: password
    remoteRef:
      key: bist-trading/database
      property: password
```

## Monitoring and Observability

### Prometheus Configuration
```yaml
# monitoring/prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: bist-trading-prod
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    rule_files:
      - "/etc/prometheus/rules/*.yml"

    alerting:
      alertmanagers:
        - static_configs:
            - targets:
              - alertmanager:9093

    scrape_configs:
      # Kubernetes API server
      - job_name: 'kubernetes-apiservers'
        kubernetes_sd_configs:
        - role: endpoints
        scheme: https
        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
        relabel_configs:
        - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
          action: keep
          regex: default;kubernetes;https

      # Application services
      - job_name: 'bist-trading-services'
        kubernetes_sd_configs:
        - role: endpoints
          namespaces:
            names:
            - bist-trading-prod
        relabel_configs:
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__
        - action: labelmap
          regex: __meta_kubernetes_service_label_(.+)
        - source_labels: [__meta_kubernetes_namespace]
          action: replace
          target_label: kubernetes_namespace
        - source_labels: [__meta_kubernetes_service_name]
          action: replace
          target_label: kubernetes_name

      # Node exporter
      - job_name: 'kubernetes-nodes'
        kubernetes_sd_configs:
        - role: node
        relabel_configs:
        - action: labelmap
          regex: __meta_kubernetes_node_label_(.+)
```

### Alert Rules
```yaml
# monitoring/alert-rules.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-rules
  namespace: bist-trading-prod
data:
  trading-alerts.yml: |
    groups:
    - name: bist-trading.rules
      rules:
      # High CPU usage
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on {{ $labels.instance }}"
          description: "CPU usage is above 80% for more than 5 minutes"

      # High memory usage
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"
          description: "Memory usage is above 85% for more than 5 minutes"

      # Service down
      - alert: ServiceDown
        expr: up{job=~"bist-trading-.*"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service {{ $labels.job }} has been down for more than 2 minutes"

      # High error rate
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100 > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.service }}"
          description: "Error rate is above 5% for more than 5 minutes"

      # Market data processing lag
      - alert: MarketDataLag
        expr: increase(market_data_processing_lag_seconds[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Market data processing lag detected"
          description: "Market data processing is lagging behind by more than 10 seconds"

      # Database connection pool exhaustion
      - alert: DatabaseConnectionPoolExhausted
        expr: hikari_connections_active / hikari_connections_max * 100 > 90
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Database connection pool usage is above 90%"
```

### Grafana Dashboards
```json
{
  "dashboard": {
    "id": null,
    "title": "BIST Trading Platform - Production",
    "tags": ["bist-trading", "production"],
    "timezone": "Europe/Istanbul",
    "panels": [
      {
        "id": 1,
        "title": "Service Health Overview",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"bist-trading-.*\"}",
            "legendFormat": "{{job}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Request Rate (req/s)",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "id": 4,
        "title": "Market Data Throughput",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(market_ticks_processed_total[1m])",
            "legendFormat": "Ticks/second"
          }
        ]
      },
      {
        "id": 5,
        "title": "Database Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "hikari_connections_active",
            "legendFormat": "Active Connections"
          },
          {
            "expr": "hikari_connections_idle",
            "legendFormat": "Idle Connections"
          }
        ]
      },
      {
        "id": 6,
        "title": "Error Rate (%)",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service) * 100",
            "legendFormat": "{{service}}"
          }
        ]
      }
    ]
  }
}
```

## Database Setup and Optimization

### Production Database Configuration
```sql
-- PostgreSQL production configuration
-- /etc/postgresql/15/main/postgresql.conf

# Memory settings
shared_buffers = 8GB                    # 25% of total RAM
effective_cache_size = 24GB             # 75% of total RAM
work_mem = 256MB                        # For complex queries
maintenance_work_mem = 2GB              # For maintenance operations

# Checkpoint settings
checkpoint_completion_target = 0.9
wal_buffers = 64MB
checkpoint_timeout = 10min

# Connection settings
max_connections = 200
superuser_reserved_connections = 3

# Logging settings
log_destination = 'stderr,csvlog'
log_directory = '/var/log/postgresql'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_statement = 'ddl'
log_min_duration_statement = 1000       # Log queries > 1 second

# Replication settings (for read replicas)
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB

# TimescaleDB settings
shared_preload_libraries = 'timescaledb'
timescaledb.max_background_workers = 8
```

### Database Initialization Script
```sql
-- production-init.sql
-- Create extensions
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create hypertables for market data
SELECT create_hypertable('market_ticks', 'timestamp', chunk_time_interval => INTERVAL '1 hour');

-- Create indexes for performance
CREATE INDEX CONCURRENTLY idx_market_ticks_symbol_time ON market_ticks (symbol, timestamp DESC);
CREATE INDEX CONCURRENTLY idx_orders_user_status ON orders (user_id, status, created_at DESC);
CREATE INDEX CONCURRENTLY idx_transactions_user_date ON transactions (user_id, transaction_date DESC);

-- Set up compression policy (compress data older than 1 day)
SELECT add_compression_policy('market_ticks', INTERVAL '1 day');

-- Set up retention policy (keep data for 1 year)
SELECT add_retention_policy('market_ticks', INTERVAL '1 year');

-- Create read-only user for reporting
CREATE USER readonly_user WITH PASSWORD 'secure_readonly_password';
GRANT CONNECT ON DATABASE bist_trading TO readonly_user;
GRANT USAGE ON SCHEMA public TO readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_user;
```

### Database Backup Strategy
```bash
#!/bin/bash
# /opt/scripts/backup-database.sh

# Configuration
DB_HOST="your-rds-endpoint.amazonaws.com"
DB_NAME="bist_trading"
DB_USER="postgres"
BACKUP_DIR="/backups/postgresql"
S3_BUCKET="bist-trading-backups"
RETENTION_DAYS=30

# Create backup directory
mkdir -p $BACKUP_DIR

# Generate backup filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/bist_trading_backup_$TIMESTAMP.sql"

# Create full backup
echo "Starting database backup..."
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --verbose --no-owner --no-acl --compress=9 \
  --file=$BACKUP_FILE.gz

# Verify backup
if [ $? -eq 0 ]; then
    echo "Database backup completed successfully"

    # Upload to S3
    aws s3 cp $BACKUP_FILE.gz s3://$S3_BUCKET/database/

    # Set lifecycle policy
    aws s3api put-object-tagging \
      --bucket $S3_BUCKET \
      --key database/$(basename $BACKUP_FILE.gz) \
      --tagging "TagSet=[{Key=RetentionDays,Value=$RETENTION_DAYS}]"

    # Clean up local backup older than 7 days
    find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

    echo "Backup uploaded to S3 and local cleanup completed"
else
    echo "Database backup failed!"
    exit 1
fi
```

## Security Hardening

### Application Security Configuration
```yaml
# k8s/security-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: security-config
  namespace: bist-trading-prod
data:
  application-security.yml: |
    # Security configuration
    spring:
      security:
        oauth2:
          resourceserver:
            jwt:
              issuer-uri: https://your-auth-provider.com

    # HTTPS enforcement
    server:
      ssl:
        enabled: true
        key-store-type: PKCS12
        key-store: classpath:keystore.p12
        key-store-password: ${KEYSTORE_PASSWORD}
        key-alias: bist-trading

    # CORS configuration
    cors:
      allowed-origins:
        - "https://bist-trading.com"
        - "https://app.bist-trading.com"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600

    # Rate limiting
    rate-limiting:
      enabled: true
      default-rate: 100
      burst-capacity: 200
      rules:
        - path: "/api/auth/**"
          rate: 10
          window: "1m"
        - path: "/api/orders/**"
          rate: 50
          window: "1m"
```

### WAF Rules (AWS WAF)
```json
{
  "Name": "BISTTradingWAF",
  "Scope": "CLOUDFRONT",
  "DefaultAction": {
    "Allow": {}
  },
  "Rules": [
    {
      "Name": "RateLimitRule",
      "Priority": 1,
      "Statement": {
        "RateBasedStatement": {
          "Limit": 2000,
          "AggregateKeyType": "IP"
        }
      },
      "Action": {
        "Block": {}
      },
      "VisibilityConfig": {
        "SampledRequestsEnabled": true,
        "CloudWatchMetricsEnabled": true,
        "MetricName": "RateLimitRule"
      }
    },
    {
      "Name": "SQLInjectionRule",
      "Priority": 2,
      "Statement": {
        "ManagedRuleGroupStatement": {
          "VendorName": "AWS",
          "Name": "AWSManagedRulesSQLiRuleSet"
        }
      },
      "Action": {
        "Block": {}
      },
      "VisibilityConfig": {
        "SampledRequestsEnabled": true,
        "CloudWatchMetricsEnabled": true,
        "MetricName": "SQLInjectionRule"
      }
    }
  ]
}
```

## Deployment Procedures

### CI/CD Pipeline for Production
```yaml
# .github/workflows/production-deploy.yml
name: Production Deployment

on:
  push:
    tags:
      - 'v*'

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Run security scan
      run: |
        ./gradlew dependencyCheckAnalyze
        docker run --rm -v $(pwd):/code sonarqube:latest sonar-scanner

  build-and-test:
    needs: security-scan
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Run comprehensive tests
      run: |
        ./test-runner.sh all
        ./gradlew jacocoTestReport

    - name: Build production images
      run: |
        ./build.sh
        docker build -t ${{ secrets.ECR_REGISTRY }}/user-management-service:${{ github.ref_name }} \
          -f docker/user-management.Dockerfile .
        docker build -t ${{ secrets.ECR_REGISTRY }}/market-data-service:${{ github.ref_name }} \
          -f docker/market-data.Dockerfile .
        docker build -t ${{ secrets.ECR_REGISTRY }}/broker-integration-service:${{ github.ref_name }} \
          -f docker/broker-integration.Dockerfile .

    - name: Push to ECR
      run: |
        aws ecr get-login-password | docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
        docker push ${{ secrets.ECR_REGISTRY }}/user-management-service:${{ github.ref_name }}
        docker push ${{ secrets.ECR_REGISTRY }}/market-data-service:${{ github.ref_name }}
        docker push ${{ secrets.ECR_REGISTRY }}/broker-integration-service:${{ github.ref_name }}

  deploy-staging:
    needs: build-and-test
    runs-on: ubuntu-latest
    environment: staging
    steps:
    - name: Deploy to staging
      run: |
        kubectl set image deployment/user-management-service \
          user-management=${{ secrets.ECR_REGISTRY }}/user-management-service:${{ github.ref_name }} \
          --namespace=bist-trading-staging
        kubectl rollout status deployment/user-management-service --namespace=bist-trading-staging

    - name: Run integration tests
      run: |
        ./scripts/integration-tests.sh staging

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    steps:
    - name: Blue-Green Deployment
      run: |
        ./scripts/blue-green-deploy.sh ${{ github.ref_name }}

    - name: Health Check
      run: |
        ./scripts/health-check.sh production

    - name: Smoke Tests
      run: |
        ./scripts/smoke-tests.sh production
```

### Blue-Green Deployment Script
```bash
#!/bin/bash
# scripts/blue-green-deploy.sh

VERSION=$1
NAMESPACE="bist-trading-prod"

# Determine current and new environments
CURRENT_COLOR=$(kubectl get service production-service -n $NAMESPACE -o jsonpath='{.spec.selector.color}' 2>/dev/null || echo "blue")

if [ "$CURRENT_COLOR" = "blue" ]; then
    NEW_COLOR="green"
else
    NEW_COLOR="blue"
fi

echo "Deploying version $VERSION to $NEW_COLOR environment"

# Update deployments with new color and version
for service in user-management-service market-data-service broker-integration-service; do
    # Update deployment
    kubectl patch deployment $service-$NEW_COLOR -n $NAMESPACE -p '{
      "spec": {
        "template": {
          "metadata": {
            "labels": {
              "color": "'$NEW_COLOR'",
              "version": "'$VERSION'"
            }
          },
          "spec": {
            "containers": [{
              "name": "'${service%-service}'",
              "image": "'$ECR_REGISTRY'/'$service':'$VERSION'"
            }]
          }
        }
      }
    }'

    # Wait for rollout
    kubectl rollout status deployment/$service-$NEW_COLOR -n $NAMESPACE --timeout=600s
done

# Run health checks
echo "Running health checks on $NEW_COLOR environment"
if ./scripts/health-check.sh $NEW_COLOR; then
    echo "Health checks passed. Switching traffic to $NEW_COLOR"

    # Update service selectors to point to new environment
    for service in user-management-service market-data-service broker-integration-service; do
        kubectl patch service $service -n $NAMESPACE -p '{
          "spec": {
            "selector": {
              "color": "'$NEW_COLOR'"
            }
          }
        }'
    done

    echo "Traffic switched to $NEW_COLOR environment"

    # Scale down old environment after 5 minutes
    sleep 300
    for service in user-management-service market-data-service broker-integration-service; do
        kubectl scale deployment $service-$CURRENT_COLOR -n $NAMESPACE --replicas=0
    done

    echo "Blue-Green deployment completed successfully"
else
    echo "Health checks failed. Rolling back."
    for service in user-management-service market-data-service broker-integration-service; do
        kubectl scale deployment $service-$NEW_COLOR -n $NAMESPACE --replicas=0
    done
    exit 1
fi
```

## Disaster Recovery

### Backup and Recovery Procedures
```bash
#!/bin/bash
# scripts/disaster-recovery.sh

RECOVERY_TYPE=$1  # full, database, application
BACKUP_TIMESTAMP=$2

case $RECOVERY_TYPE in
  "full")
    echo "Starting full system recovery..."

    # 1. Restore database
    ./scripts/restore-database.sh $BACKUP_TIMESTAMP

    # 2. Restore application configuration
    kubectl apply -f k8s/

    # 3. Deploy latest stable version
    ./scripts/deploy-stable.sh

    # 4. Verify system health
    ./scripts/health-check.sh production
    ;;

  "database")
    echo "Starting database recovery..."
    ./scripts/restore-database.sh $BACKUP_TIMESTAMP
    ;;

  "application")
    echo "Starting application recovery..."
    kubectl apply -f k8s/
    ./scripts/deploy-stable.sh
    ;;

  *)
    echo "Usage: $0 {full|database|application} [backup_timestamp]"
    exit 1
    ;;
esac
```

### RTO and RPO Targets
| Component | RTO (Recovery Time Objective) | RPO (Recovery Point Objective) |
|-----------|--------------------------------|----------------------------------|
| **Database** | 15 minutes | 1 minute (with streaming replication) |
| **Application Services** | 5 minutes | Real-time (stateless) |
| **Market Data** | 2 minutes | 30 seconds |
| **User Sessions** | Immediate | 5 minutes (Redis persistence) |

## Maintenance Procedures

### Scheduled Maintenance Window
```bash
#!/bin/bash
# scripts/maintenance-window.sh

MAINTENANCE_TYPE=$1  # update, patch, scale

echo "Starting maintenance window: $MAINTENANCE_TYPE"

# 1. Enable maintenance mode
kubectl patch ingress bist-trading-ingress -n bist-trading-prod -p '{
  "metadata": {
    "annotations": {
      "nginx.ingress.kubernetes.io/default-backend": "maintenance-page"
    }
  }
}'

# 2. Wait for connections to drain
sleep 30

# 3. Perform maintenance
case $MAINTENANCE_TYPE in
  "update")
    echo "Performing application update..."
    # Rolling update with zero downtime
    kubectl set image deployment/user-management-service \
      user-management=$ECR_REGISTRY/user-management-service:$NEW_VERSION \
      -n bist-trading-prod
    kubectl rollout status deployment/user-management-service -n bist-trading-prod
    ;;

  "patch")
    echo "Performing security patches..."
    # Update base images and redeploy
    ./scripts/security-patch.sh
    ;;

  "scale")
    echo "Scaling services..."
    # Scale based on expected load
    kubectl scale deployment/market-data-service --replicas=$NEW_REPLICAS -n bist-trading-prod
    ;;
esac

# 4. Health checks
./scripts/health-check.sh production

# 5. Disable maintenance mode
kubectl patch ingress bist-trading-ingress -n bist-trading-prod -p '{
  "metadata": {
    "annotations": {
      "nginx.ingress.kubernetes.io/default-backend": null
    }
  }
}'

echo "Maintenance window completed"
```

---

**Last Updated:** September 2024
**Version:** 1.0
**Maintainer:** BIST Trading Platform Operations Team