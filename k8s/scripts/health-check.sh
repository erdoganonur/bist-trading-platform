#!/bin/bash

# BIST Trading Platform - Health Check Script
# Usage: ./health-check.sh [namespace]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="${1:-bist-trading}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}BIST Trading Platform - Health Check${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Namespace: ${YELLOW}${NAMESPACE}${NC}"
echo ""

# Check namespace exists
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo -e "${RED}✗ Namespace '$NAMESPACE' does not exist${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Namespace exists${NC}"
echo ""

# Check pods
echo -e "${YELLOW}Checking pods...${NC}"
kubectl get pods -n "$NAMESPACE"
echo ""

POSTGRES_STATUS=$(kubectl get pods -l app=postgres -n "$NAMESPACE" -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "NotFound")
REDIS_STATUS=$(kubectl get pods -l app=redis -n "$NAMESPACE" -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "NotFound")
BACKEND_STATUS=$(kubectl get pods -l app=backend -n "$NAMESPACE" -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "NotFound")

echo -e "${YELLOW}Pod Status:${NC}"
echo -e "PostgreSQL: $([ "$POSTGRES_STATUS" = "Running" ] && echo -e "${GREEN}$POSTGRES_STATUS${NC}" || echo -e "${RED}$POSTGRES_STATUS${NC}")"
echo -e "Redis:      $([ "$REDIS_STATUS" = "Running" ] && echo -e "${GREEN}$REDIS_STATUS${NC}" || echo -e "${RED}$REDIS_STATUS${NC}")"
echo -e "Backend:    $([ "$BACKEND_STATUS" = "Running" ] && echo -e "${GREEN}$BACKEND_STATUS${NC}" || echo -e "${RED}$BACKEND_STATUS${NC}")"
echo ""

# Check services
echo -e "${YELLOW}Checking services...${NC}"
kubectl get svc -n "$NAMESPACE"
echo ""

# Check ingress
echo -e "${YELLOW}Checking ingress...${NC}"
if kubectl get ingress -n "$NAMESPACE" &> /dev/null; then
    kubectl get ingress -n "$NAMESPACE"
else
    echo -e "${YELLOW}No ingress found${NC}"
fi
echo ""

# Test backend health endpoint
echo -e "${YELLOW}Testing backend health endpoint...${NC}"

BACKEND_POD=$(kubectl get pods -l app=backend -n "$NAMESPACE" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

if [ -n "$BACKEND_POD" ]; then
    HEALTH_RESPONSE=$(kubectl exec -n "$NAMESPACE" "$BACKEND_POD" -- curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "Failed")

    if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
        echo -e "${GREEN}✓ Backend is healthy${NC}"
        echo "$HEALTH_RESPONSE"
    else
        echo -e "${RED}✗ Backend health check failed${NC}"
        echo "$HEALTH_RESPONSE"
    fi
else
    echo -e "${RED}✗ Backend pod not found${NC}"
fi
echo ""

# Check persistent volumes
echo -e "${YELLOW}Checking persistent volumes...${NC}"
kubectl get pvc -n "$NAMESPACE"
echo ""

# Check events for errors
echo -e "${YELLOW}Recent events (last 10):${NC}"
kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' | tail -10
echo ""

# Summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Health Check Summary${NC}"
echo -e "${GREEN}========================================${NC}"

TOTAL_PODS=$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l)
RUNNING_PODS=$(kubectl get pods -n "$NAMESPACE" --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l)

echo -e "Total Pods:   ${YELLOW}${TOTAL_PODS}${NC}"
echo -e "Running Pods: ${GREEN}${RUNNING_PODS}${NC}"

if [ "$TOTAL_PODS" -eq "$RUNNING_PODS" ] && [ "$TOTAL_PODS" -gt 0 ]; then
    echo -e ""
    echo -e "${GREEN}✓ All systems operational${NC}"
    exit 0
else
    echo -e ""
    echo -e "${RED}✗ Some pods are not running${NC}"
    exit 1
fi
