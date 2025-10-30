#!/bin/bash

# BIST Trading Platform - Kubernetes Cleanup Script
# Usage: ./cleanup.sh [namespace]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="${1:-bist-trading}"
HELM_RELEASE="bist-trading"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}BIST Trading Platform - Cleanup${NC}"
echo -e "${YELLOW}========================================${NC}"
echo -e "Namespace: ${RED}${NAMESPACE}${NC}"
echo ""

# Confirmation
read -p "Are you sure you want to delete all resources in namespace '$NAMESPACE'? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${GREEN}Cleanup cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${YELLOW}Uninstalling Helm release...${NC}"

if helm list -n "$NAMESPACE" | grep -q "$HELM_RELEASE"; then
    helm uninstall "$HELM_RELEASE" -n "$NAMESPACE"
    echo -e "${GREEN}✓ Helm release uninstalled${NC}"
else
    echo -e "${YELLOW}Helm release not found, skipping...${NC}"
fi

echo ""
echo -e "${YELLOW}Deleting namespace...${NC}"

if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    kubectl delete namespace "$NAMESPACE"
    echo -e "${GREEN}✓ Namespace deleted${NC}"
else
    echo -e "${YELLOW}Namespace not found, skipping...${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Cleanup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
