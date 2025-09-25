#!/bin/bash
# =============================================================================
# Redis Cluster Health Check Script for BIST Trading Platform
# =============================================================================

set -euo pipefail

# Configuration
REDIS_NODES=(
    "localhost:7001"
    "localhost:7002"
    "localhost:7003"
    "localhost:7004"
    "localhost:7005"
    "localhost:7006"
)

REDIS_CLI="docker exec -it bist-redis-1 redis-cli"
LOG_FILE="/var/log/redis/cluster-health.log"
ALERT_THRESHOLD=50  # Alert if less than 50% nodes are healthy

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Check if Redis CLI is available
check_redis_cli() {
    if ! command -v redis-cli &> /dev/null; then
        log "ERROR: redis-cli not found. Please install Redis CLI."
        exit 1
    fi
}

# Check individual node health
check_node_health() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    if timeout 5 redis-cli -h "$host" -p "$port" ping > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Get cluster info
get_cluster_info() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    redis-cli -h "$host" -p "$port" cluster info 2>/dev/null || echo "cluster_state:fail"
}

# Get cluster nodes
get_cluster_nodes() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    redis-cli -h "$host" -p "$port" cluster nodes 2>/dev/null || echo "failed"
}

# Check cluster slot coverage
check_slot_coverage() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    local slots_covered=$(redis-cli -h "$host" -p "$port" cluster nodes 2>/dev/null | grep master | awk '{print $9}' | grep -o '[0-9]\+-[0-9]\+' | wc -l)
    echo "$slots_covered"
}

# Get memory usage
get_memory_usage() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    local memory_info=$(redis-cli -h "$host" -p "$port" info memory 2>/dev/null | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    echo "${memory_info:-N/A}"
}

# Check replication status
check_replication() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}

    local role=$(redis-cli -h "$host" -p "$port" info replication 2>/dev/null | grep "role:" | cut -d: -f2 | tr -d '\r')
    local connected_slaves=$(redis-cli -h "$host" -p "$port" info replication 2>/dev/null | grep "connected_slaves:" | cut -d: -f2 | tr -d '\r')

    if [[ "$role" == "master" ]]; then
        echo "Master (slaves: ${connected_slaves:-0})"
    elif [[ "$role" == "slave" ]]; then
        local master_link=$(redis-cli -h "$host" -p "$port" info replication 2>/dev/null | grep "master_link_status:" | cut -d: -f2 | tr -d '\r')
        echo "Slave (link: ${master_link:-unknown})"
    else
        echo "Unknown"
    fi
}

# Main health check function
perform_health_check() {
    log "Starting Redis cluster health check..."

    local healthy_nodes=0
    local total_nodes=${#REDIS_NODES[@]}
    local cluster_state="unknown"
    local all_slots_covered=true

    echo -e "\n${YELLOW}Redis Cluster Health Report${NC}"
    echo "=================================="

    # Check each node
    for node in "${REDIS_NODES[@]}"; do
        echo -n "Node $node: "

        if check_node_health "$node"; then
            echo -e "${GREEN}HEALTHY${NC}"
            healthy_nodes=$((healthy_nodes + 1))

            # Get additional info from healthy nodes
            local memory=$(get_memory_usage "$node")
            local replication=$(check_replication "$node")

            echo "  Memory: $memory"
            echo "  Role: $replication"

            # Get cluster state from first healthy node
            if [[ "$cluster_state" == "unknown" ]]; then
                local info=$(get_cluster_info "$node")
                cluster_state=$(echo "$info" | grep cluster_state | cut -d: -f2 | tr -d '\r')
            fi

        else
            echo -e "${RED}UNHEALTHY${NC}"
        fi
        echo ""
    done

    # Overall health summary
    echo "=================================="
    echo "Healthy nodes: $healthy_nodes/$total_nodes"
    echo "Cluster state: $cluster_state"

    local health_percentage=$((healthy_nodes * 100 / total_nodes))
    echo "Health percentage: $health_percentage%"

    # Check cluster configuration
    if [[ "$healthy_nodes" -gt 0 ]]; then
        local first_healthy=""
        for node in "${REDIS_NODES[@]}"; do
            if check_node_health "$node"; then
                first_healthy=$node
                break
            fi
        done

        if [[ -n "$first_healthy" ]]; then
            echo ""
            echo "Cluster Configuration:"
            echo "====================="
            get_cluster_nodes "$first_healthy" | while read -r line; do
                echo "$line"
            done
        fi
    fi

    # Alert if health is below threshold
    if [[ "$health_percentage" -lt "$ALERT_THRESHOLD" ]]; then
        log "ALERT: Cluster health below threshold ($health_percentage% < $ALERT_THRESHOLD%)"
        echo -e "\n${RED}ALERT: Cluster health critical!${NC}"
        return 1
    elif [[ "$cluster_state" != "ok" ]]; then
        log "WARNING: Cluster state is not OK ($cluster_state)"
        echo -e "\n${YELLOW}WARNING: Cluster state issues detected${NC}"
        return 1
    else
        log "Cluster health check passed ($health_percentage% healthy)"
        echo -e "\n${GREEN}Cluster is healthy${NC}"
        return 0
    fi
}

# Fix cluster function
fix_cluster() {
    log "Attempting to fix cluster issues..."

    # Try to fix cluster configuration
    for node in "${REDIS_NODES[@]}"; do
        if check_node_health "$node"; then
            local host=${node%:*}
            local port=${node#*:}

            echo "Attempting cluster fix on $node..."
            redis-cli -h "$host" -p "$port" cluster fix --cluster-yes 2>/dev/null || true
            break
        fi
    done
}

# Reset cluster function (dangerous - use with caution)
reset_cluster() {
    read -p "Are you sure you want to reset the cluster? This will delete all data! (yes/no): " -r
    if [[ $REPLY == "yes" ]]; then
        log "Resetting cluster..."

        for node in "${REDIS_NODES[@]}"; do
            if check_node_health "$node"; then
                local host=${node%:*}
                local port=${node#*:}

                echo "Resetting node $node..."
                redis-cli -h "$host" -p "$port" cluster reset hard 2>/dev/null || true
                redis-cli -h "$host" -p "$port" flushall 2>/dev/null || true
            fi
        done

        echo "Cluster reset complete. You may need to recreate the cluster."
    else
        echo "Cluster reset cancelled."
    fi
}

# Show usage
usage() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  -h, --help       Show this help message"
    echo "  -c, --check      Perform health check (default)"
    echo "  -f, --fix        Attempt to fix cluster issues"
    echo "  -r, --reset      Reset cluster (WARNING: deletes all data)"
    echo "  -w, --watch      Continuous monitoring (every 30 seconds)"
    echo ""
    echo "Examples:"
    echo "  $0                # Perform health check"
    echo "  $0 --check       # Same as above"
    echo "  $0 --fix         # Try to fix cluster issues"
    echo "  $0 --watch       # Continuous monitoring"
}

# Continuous monitoring
watch_cluster() {
    echo "Starting continuous cluster monitoring (press Ctrl+C to stop)..."

    while true; do
        clear
        echo "Redis Cluster Monitor - $(date)"
        echo "========================================"

        perform_health_check

        echo ""
        echo "Next check in 30 seconds..."
        sleep 30
    done
}

# Main script
main() {
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true

    case "${1:-check}" in
        -h|--help|help)
            usage
            ;;
        -c|--check|check)
            perform_health_check
            ;;
        -f|--fix|fix)
            fix_cluster
            ;;
        -r|--reset|reset)
            reset_cluster
            ;;
        -w|--watch|watch)
            watch_cluster
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"