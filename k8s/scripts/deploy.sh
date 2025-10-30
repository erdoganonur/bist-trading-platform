#!/bin/bash

# BIST Trading Platform - Kubernetes Deployment Script
# Usage: ./deploy.sh [k3s|docker-desktop|minikube] [dev|prod]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
K8S_PLATFORM="${1:-k3s}"
ENV="${2:-dev}"
NAMESPACE="bist-trading"
HELM_RELEASE="bist-trading"
HELM_CHART_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")/../helm/bist-trading-platform" && pwd)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}BIST Trading Platform - K8s Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Platform: ${YELLOW}${K8S_PLATFORM}${NC}"
echo -e "Environment: ${YELLOW}${ENV}${NC}"
echo -e "Namespace: ${YELLOW}${NAMESPACE}${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}kubectl not found. Please install kubectl.${NC}"
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        echo -e "${RED}helm not found. Please install helm.${NC}"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        echo -e "${RED}docker not found. Please install docker.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ All prerequisites met${NC}"
}

# Function to check Kubernetes cluster
check_cluster() {
    echo -e "${YELLOW}Checking Kubernetes cluster...${NC}"

    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Cannot connect to Kubernetes cluster. Please start your cluster.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Kubernetes cluster is running${NC}"
    kubectl cluster-info
}

# Function to build Docker image
build_image() {
    echo -e "${YELLOW}Building Docker image...${NC}"

    cd "$(dirname "${BASH_SOURCE[0]}")/../.."

    IMAGE_TAG="bist-trading-backend:2.0.0"

    docker build -t "$IMAGE_TAG" .

    # Import image to K3s if using K3s
    if [ "$K8S_PLATFORM" = "k3s" ]; then
        echo -e "${YELLOW}Importing image to K3s...${NC}"
        docker save "$IMAGE_TAG" | sudo k3s ctr images import -
    fi

    echo -e "${GREEN}✓ Docker image built: ${IMAGE_TAG}${NC}"
}

# Function to create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"

    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        echo -e "${GREEN}✓ Namespace ${NAMESPACE} already exists${NC}"
    else
        kubectl create namespace "$NAMESPACE"
        echo -e "${GREEN}✓ Namespace ${NAMESPACE} created${NC}"
    fi
}

# Function to set storage class based on platform
get_storage_class() {
    case "$K8S_PLATFORM" in
        k3s)
            echo "local-path"
            ;;
        docker-desktop)
            echo "hostpath"
            ;;
        minikube)
            echo "standard"
            ;;
        *)
            echo "local-path"
            ;;
    esac
}

# Function to deploy with Helm
deploy_helm() {
    echo -e "${YELLOW}Deploying with Helm...${NC}"

    STORAGE_CLASS=$(get_storage_class)

    # Create values override file
    cat > /tmp/helm-values-override.yaml <<EOF
backend:
  replicaCount: 2

postgresql:
  persistence:
    storageClass: "${STORAGE_CLASS}"

redis:
  persistence:
    storageClass: "${STORAGE_CLASS}"

ingress:
  className: "$([ "$K8S_PLATFORM" = "k3s" ] && echo "traefik" || echo "nginx")"
EOF

    # Check if release exists
    if helm list -n "$NAMESPACE" | grep -q "$HELM_RELEASE"; then
        echo -e "${YELLOW}Upgrading existing release...${NC}"
        helm upgrade "$HELM_RELEASE" "$HELM_CHART_PATH" \
            --namespace "$NAMESPACE" \
            --values /tmp/helm-values-override.yaml \
            --wait \
            --timeout 10m
    else
        echo -e "${YELLOW}Installing new release...${NC}"
        helm install "$HELM_RELEASE" "$HELM_CHART_PATH" \
            --namespace "$NAMESPACE" \
            --create-namespace \
            --values /tmp/helm-values-override.yaml \
            --wait \
            --timeout 10m
    fi

    rm -f /tmp/helm-values-override.yaml

    echo -e "${GREEN}✓ Helm deployment complete${NC}"
}

# Function to wait for pods
wait_for_pods() {
    echo -e "${YELLOW}Waiting for pods to be ready...${NC}"

    kubectl wait --for=condition=ready pod \
        -l app=postgres \
        -n "$NAMESPACE" \
        --timeout=300s

    kubectl wait --for=condition=ready pod \
        -l app=redis \
        -n "$NAMESPACE" \
        --timeout=300s

    kubectl wait --for=condition=ready pod \
        -l app=backend \
        -n "$NAMESPACE" \
        --timeout=300s

    echo -e "${GREEN}✓ All pods are ready${NC}"
}

# Function to display deployment info
display_info() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Deployment Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""

    echo -e "${YELLOW}Resources:${NC}"
    kubectl get all -n "$NAMESPACE"

    echo ""
    echo -e "${YELLOW}Access Information:${NC}"

    # Get service information
    if [ "$K8S_PLATFORM" = "minikube" ]; then
        SERVICE_URL=$(minikube service "$HELM_RELEASE-backend-lb" -n "$NAMESPACE" --url 2>/dev/null || echo "N/A")
        echo -e "Service URL: ${GREEN}${SERVICE_URL}${NC}"
    else
        EXTERNAL_IP=$(kubectl get svc "$HELM_RELEASE-backend-lb" -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")
        echo -e "External IP: ${GREEN}${EXTERNAL_IP}${NC}"
        echo -e "Service URL: ${GREEN}http://${EXTERNAL_IP}${NC}"
    fi

    # Ingress information
    INGRESS_HOST=$(kubectl get ingress -n "$NAMESPACE" -o jsonpath='{.items[0].spec.rules[0].host}' 2>/dev/null || echo "N/A")
    if [ "$INGRESS_HOST" != "N/A" ]; then
        echo -e "Ingress Host: ${GREEN}${INGRESS_HOST}${NC}"
        echo -e "Add to /etc/hosts: ${YELLOW}127.0.0.1 ${INGRESS_HOST}${NC}"
    fi

    echo ""
    echo -e "${YELLOW}Health Check:${NC}"
    echo -e "kubectl port-forward svc/$HELM_RELEASE-backend 8080:8080 -n $NAMESPACE"
    echo -e "curl http://localhost:8080/actuator/health"

    echo ""
    echo -e "${YELLOW}Useful Commands:${NC}"
    echo -e "View logs:    ${GREEN}kubectl logs -f deployment/$HELM_RELEASE-backend -n $NAMESPACE${NC}"
    echo -e "View pods:    ${GREEN}kubectl get pods -n $NAMESPACE${NC}"
    echo -e "View events:  ${GREEN}kubectl get events -n $NAMESPACE${NC}"
    echo -e "Delete:       ${GREEN}helm uninstall $HELM_RELEASE -n $NAMESPACE${NC}"
}

# Main execution
main() {
    check_prerequisites
    check_cluster
    build_image
    create_namespace
    deploy_helm
    wait_for_pods
    display_info
}

# Run main function
main
