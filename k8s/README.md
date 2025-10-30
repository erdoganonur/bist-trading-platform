# BIST Trading Platform - Kubernetes Deployment Guide

Complete guide for deploying BIST Trading Platform on Kubernetes, optimized for Mac Mini.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Kubernetes Options for Mac Mini](#kubernetes-options-for-mac-mini)
- [Quick Start](#quick-start)
- [Deployment Methods](#deployment-methods)
  - [Method 1: Raw Kubernetes Manifests](#method-1-raw-kubernetes-manifests)
  - [Method 2: Kustomize](#method-2-kustomize)
  - [Method 3: Helm Chart (Recommended)](#method-3-helm-chart-recommended)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## 📦 Prerequisites

### Required Software

```bash
# 1. Kubernetes Cluster (choose one):
# - Docker Desktop Kubernetes (easiest)
# - K3s (recommended for production)
# - Minikube (for testing)

# 2. kubectl
brew install kubectl

# 3. Helm (for Helm deployment)
brew install helm

# 4. Docker (for building images)
brew install --cask docker
```

### Required Resources (Mac Mini)

- **Minimum:** 8GB RAM, 50GB disk space
- **Recommended:** 16GB RAM, 100GB disk space

---

## 🎯 Kubernetes Options for Mac Mini

### Option 1: Docker Desktop Kubernetes (Easiest)

**Best for:** Development, quick testing

**Pros:**
- Built into Docker Desktop
- Easy GUI management
- No additional setup

**Cons:**
- Resource intensive
- Not for production

**Setup:**

```bash
# 1. Install Docker Desktop
brew install --cask docker

# 2. Enable Kubernetes in Docker Desktop
# Settings → Kubernetes → Enable Kubernetes → Apply

# 3. Verify
kubectl cluster-info
kubectl get nodes
```

---

### Option 2: K3s (Recommended for Production)

**Best for:** Production-like deployments on Mac Mini

**Pros:**
- Lightweight (uses ~512MB RAM)
- Production-ready
- ARM64 optimized (M1/M2/M3)
- Includes Traefik ingress controller

**Cons:**
- Requires terminal setup
- Less GUI tooling

**Setup:**

```bash
# 1. Install K3s
curl -sfL https://get.k3s.io | sh -

# 2. Configure kubectl
mkdir -p ~/.kube
sudo k3s kubectl config view --raw > ~/.kube/config
chmod 600 ~/.kube/config

# 3. Verify
kubectl get nodes

# 4. Optional: Install K9s for cluster management
brew install k9s
k9s
```

**K3s Configuration for Mac Mini:**

```bash
# Optimize for Mac Mini (8GB RAM)
curl -sfL https://get.k3s.io | sh -s - server \
  --write-kubeconfig-mode 644 \
  --disable traefik \
  --kube-apiserver-arg="max-requests-inflight=200" \
  --kubelet-arg="max-pods=50"

# For 16GB RAM Mac Mini (more resources)
curl -sfL https://get.k3s.io | sh -s - server \
  --write-kubeconfig-mode 644 \
  --disable traefik \
  --kube-apiserver-arg="max-requests-inflight=400" \
  --kubelet-arg="max-pods=110"
```

---

### Option 3: Minikube (Testing)

**Best for:** Learning Kubernetes, testing

**Pros:**
- Full Kubernetes features
- Good addon ecosystem
- Easy to reset

**Cons:**
- Overkill for single-node
- Resource intensive

**Setup:**

```bash
# 1. Install Minikube
brew install minikube

# 2. Start cluster
minikube start --cpus=4 --memory=8192 --driver=docker

# 3. Enable addons
minikube addons enable ingress
minikube addons enable metrics-server

# 4. Verify
kubectl get nodes
```

---

## 🚀 Quick Start

### 1. Build Docker Image

```bash
# Build the application image
docker build -t bist-trading-backend:2.0.0 .

# For K3s, import image
sudo k3s ctr images import bist-trading-backend:2.0.0

# For Docker Desktop, no import needed
```

### 2. Deploy with Helm (Recommended)

```bash
# Navigate to project root
cd /path/to/bist-trading-platform

# Install with Helm
helm install bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --create-namespace

# Verify deployment
kubectl get all -n bist-trading

# Watch pods starting
kubectl get pods -n bist-trading -w
```

### 3. Access the Application

```bash
# Get service URL
kubectl get svc -n bist-trading

# For LoadBalancer (K3s or Docker Desktop)
export SERVICE_IP=$(kubectl get svc bist-trading-backend-lb -n bist-trading -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "Access at: http://$SERVICE_IP"

# For Ingress (add to /etc/hosts)
echo "127.0.0.1 bist-trading.local" | sudo tee -a /etc/hosts
open http://bist-trading.local

# Check health
curl http://bist-trading.local/actuator/health
```

---

## 📚 Deployment Methods

### Method 1: Raw Kubernetes Manifests

**Use case:** Maximum control, understanding Kubernetes

```bash
# Apply all manifests in order
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/secret.yaml
kubectl apply -f k8s/base/postgres-statefulset.yaml
kubectl apply -f k8s/base/redis-statefulset.yaml

# Wait for databases
kubectl wait --for=condition=ready pod -l app=postgres -n bist-trading --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n bist-trading --timeout=300s

# Deploy backend
kubectl apply -f k8s/base/backend-deployment.yaml
kubectl apply -f k8s/base/ingress.yaml

# Check status
kubectl get all -n bist-trading
```

---

### Method 2: Kustomize

**Use case:** Multi-environment deployments (dev/staging/prod)

```bash
# Deploy base configuration
kubectl apply -k k8s/base/

# Deploy dev overlay (future)
kubectl apply -k k8s/overlays/dev/

# Deploy prod overlay (future)
kubectl apply -k k8s/overlays/prod/

# Verify
kubectl get all -n bist-trading
```

**Create overlays for different environments:**

```bash
# Development overlay
cat > k8s/overlays/dev/kustomization.yaml <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: bist-trading-dev

bases:
  - ../../base

patches:
  - path: replica-patch.yaml
  - path: resources-patch.yaml

configMapGenerator:
  - name: bist-trading-config
    behavior: merge
    literals:
      - SPRING_PROFILES_ACTIVE=dev
      - LOGGING_LEVEL_ROOT=DEBUG
EOF

# Reduce replicas for dev
cat > k8s/overlays/dev/replica-patch.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bist-trading-backend
spec:
  replicas: 1
EOF
```

---

### Method 3: Helm Chart (Recommended)

**Use case:** Production deployments, easy configuration, versioning

#### Basic Installation

```bash
# Install with default values
helm install bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --create-namespace

# Install with custom values
helm install bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --create-namespace \
  --set backend.replicaCount=3 \
  --set postgresql.persistence.size=50Gi

# Install with custom values file
helm install bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --create-namespace \
  --values my-values.yaml
```

#### Custom Values Example

Create `my-values.yaml`:

```yaml
# Mac Mini optimized configuration
backend:
  replicaCount: 2
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1500m"

postgresql:
  persistence:
    size: 30Gi
    storageClass: "local-path"  # For K3s

redis:
  persistence:
    size: 10Gi

ingress:
  enabled: true
  className: "traefik"  # For K3s
  hosts:
    - host: bist.local
      paths:
        - path: /
          pathType: Prefix

config:
  security:
    corsAllowedOrigins: "http://localhost:3000,http://bist.local"

secrets:
  algolab:
    apiKey: "YOUR_ACTUAL_API_KEY"
    apiSecret: "YOUR_ACTUAL_SECRET"
```

#### Helm Operations

```bash
# List releases
helm list -n bist-trading

# Upgrade release
helm upgrade bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --values my-values.yaml

# Rollback to previous version
helm rollback bist-trading -n bist-trading

# Uninstall
helm uninstall bist-trading -n bist-trading

# Get status
helm status bist-trading -n bist-trading

# Get values
helm get values bist-trading -n bist-trading
```

---

## ⚙️ Configuration

### Storage Classes

**For K3s:**
```yaml
postgresql:
  persistence:
    storageClass: "local-path"

redis:
  persistence:
    storageClass: "local-path"
```

**For Docker Desktop:**
```yaml
postgresql:
  persistence:
    storageClass: "hostpath"

redis:
  persistence:
    storageClass: "hostpath"
```

### Ingress Controllers

**K3s (Traefik - Built-in):**
```yaml
ingress:
  className: "traefik"
```

**Install Nginx Ingress:**
```bash
# For K3s or any Kubernetes
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# For Minikube
minikube addons enable ingress
```

```yaml
ingress:
  className: "nginx"
```

### Secrets Management

**For Production, use external secrets:**

```bash
# Install sealed-secrets
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create sealed secret
echo -n "my-secret-password" | kubectl create secret generic my-secret \
  --dry-run=client --from-file=password=/dev/stdin -o yaml | \
  kubeseal -o yaml > sealed-secret.yaml

# Apply sealed secret
kubectl apply -f sealed-secret.yaml
```

**Or use External Secrets Operator with Vault/AWS Secrets Manager.**

---

## 📊 Monitoring

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/bist-trading-backend -n bist-trading

# PostgreSQL logs
kubectl logs -f statefulset/postgres -n bist-trading

# Redis logs
kubectl logs -f statefulset/redis -n bist-trading

# All pods
kubectl logs -f -l app.kubernetes.io/name=bist-trading-platform -n bist-trading
```

### Check Status

```bash
# All resources
kubectl get all -n bist-trading

# Pods with more details
kubectl get pods -n bist-trading -o wide

# Describe pod
kubectl describe pod <pod-name> -n bist-trading

# Get events
kubectl get events -n bist-trading --sort-by='.lastTimestamp'
```

### Port Forwarding (for testing)

```bash
# Forward backend port
kubectl port-forward svc/bist-trading-backend 8080:8080 -n bist-trading

# Forward PostgreSQL port
kubectl port-forward svc/postgres-service 5432:5432 -n bist-trading

# Forward Redis port
kubectl port-forward svc/redis-service 6379:6379 -n bist-trading
```

### Install Prometheus + Grafana (Optional)

```bash
# Add Helm repos
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace

# Access Grafana
kubectl port-forward svc/prometheus-grafana 3000:80 -n monitoring
# Username: admin
# Password: prom-operator
```

---

## 🔧 Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n bist-trading

# Describe pod to see events
kubectl describe pod <pod-name> -n bist-trading

# Check logs
kubectl logs <pod-name> -n bist-trading

# Common issues:
# 1. Image pull errors - check image name and tag
# 2. Resource limits - reduce memory/cpu requests
# 3. Database not ready - check init containers
```

### Database Connection Issues

```bash
# Test PostgreSQL connectivity
kubectl run -it --rm debug --image=postgres:16 --restart=Never -n bist-trading -- \
  psql -h postgres-service -U bist_user -d bist_trading

# Test Redis connectivity
kubectl run -it --rm debug --image=redis:7 --restart=Never -n bist-trading -- \
  redis-cli -h redis-service -a redis_password ping
```

### Ingress Not Working

```bash
# Check ingress
kubectl get ingress -n bist-trading
kubectl describe ingress bist-trading-ingress -n bist-trading

# Check ingress controller
kubectl get pods -n kube-system | grep ingress

# For K3s with Traefik
kubectl get pods -n kube-system | grep traefik

# Test service directly
kubectl port-forward svc/bist-trading-backend 8080:8080 -n bist-trading
curl http://localhost:8080/actuator/health
```

### Resource Issues (Mac Mini)

```bash
# Check node resources
kubectl top nodes
kubectl top pods -n bist-trading

# Reduce replicas
kubectl scale deployment bist-trading-backend --replicas=1 -n bist-trading

# Or with Helm
helm upgrade bist-trading k8s/helm/bist-trading-platform \
  --namespace bist-trading \
  --reuse-values \
  --set backend.replicaCount=1
```

---

## 🧹 Cleanup

### Delete Deployment

```bash
# With Helm
helm uninstall bist-trading -n bist-trading

# With kubectl
kubectl delete namespace bist-trading

# With Kustomize
kubectl delete -k k8s/base/
```

### Stop Kubernetes

```bash
# K3s
sudo systemctl stop k3s

# Minikube
minikube stop

# Docker Desktop
# Docker Desktop → Settings → Kubernetes → Uncheck "Enable Kubernetes"
```

---

## 📈 Production Recommendations

### Mac Mini Production Setup

1. **Use K3s** - Lightweight and production-ready
2. **External Database** - Use managed PostgreSQL (AWS RDS, Azure Database)
3. **External Redis** - Use managed Redis (AWS ElastiCache, Redis Cloud)
4. **Backup Strategy** - Regular PVC snapshots
5. **Monitoring** - Install Prometheus + Grafana
6. **Logging** - Install EFK stack (Elasticsearch, Fluentd, Kibana)
7. **TLS** - Use cert-manager for automatic HTTPS
8. **Resource Limits** - Set appropriate limits for your Mac Mini specs

### Security Checklist

- [ ] Use Sealed Secrets or External Secrets Operator
- [ ] Enable Network Policies
- [ ] Use RBAC for service accounts
- [ ] Enable Pod Security Standards
- [ ] Use private container registry
- [ ] Regular security updates
- [ ] Enable audit logging

---

## 📞 Support

For issues or questions:
- Check logs: `kubectl logs -n bist-trading`
- GitHub Issues: https://github.com/bisttrading/bist-trading-platform/issues
- Documentation: See `docs/` directory

---

**Last Updated:** 2025-10-30
