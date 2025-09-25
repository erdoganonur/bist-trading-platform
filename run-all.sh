#!/bin/bash

# BIST Trading Platform Run All Services Script
# Starts infrastructure and all services with health checks

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Service configuration
SERVICES=(
    "user-management-service:8081"
    "market-data-service:8082"
    "broker-integration-service:8083"
)

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_header() {
    echo -e "${MAGENTA}===============================================${NC}"
    echo -e "${MAGENTA}$1${NC}"
    echo -e "${MAGENTA}===============================================${NC}"
}

# Function to check if port is available
check_port() {
    local port=$1
    if lsof -i :$port >/dev/null 2>&1; then
        return 1  # Port is in use
    else
        return 0  # Port is available
    fi
}

# Function to wait for service health
wait_for_health() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=0

    print_info "Waiting for $service_name to be healthy on port $port..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            local health_status=$(curl -s "http://localhost:$port/actuator/health" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [ "$health_status" = "UP" ]; then
                print_success "$service_name is healthy! ✅"
                return 0
            fi
        fi

        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done

    echo ""
    print_error "$service_name health check timeout after $((max_attempts * 2)) seconds ❌"
    return 1
}

# Function to start service
start_service() {
    local service=$1
    local port=$2

    print_info "Starting $service on port $port..."

    # Check if port is already in use
    if ! check_port $port; then
        print_warning "Port $port is already in use. Service might already be running."
        if wait_for_health $service $port; then
            return 0
        else
            print_error "Service on port $port is not healthy"
            return 1
        fi
    fi

    # Start service in background
    print_info "Executing: ./gradlew :platform-services:$service:bootRun"
    ./gradlew :platform-services:$service:bootRun > logs/${service}.log 2>&1 &
    local pid=$!
    echo $pid > pids/${service}.pid

    print_info "$service started with PID $pid"

    # Wait for service to be healthy
    if wait_for_health $service $port; then
        print_success "$service started successfully!"
        return 0
    else
        print_error "$service failed to start or become healthy"
        kill $pid 2>/dev/null
        return 1
    fi
}

# Function to stop all services
stop_all_services() {
    print_header "Stopping All Services"

    # Stop Java processes
    if [ -d "pids" ]; then
        for pidfile in pids/*.pid; do
            if [ -f "$pidfile" ]; then
                local pid=$(cat "$pidfile")
                local service=$(basename "$pidfile" .pid)
                if ps -p $pid > /dev/null 2>&1; then
                    print_info "Stopping $service (PID: $pid)"
                    kill $pid
                    sleep 2
                    if ps -p $pid > /dev/null 2>&1; then
                        print_warning "Force killing $service (PID: $pid)"
                        kill -9 $pid
                    fi
                fi
                rm -f "$pidfile"
            fi
        done
    fi

    # Stop Docker containers
    print_info "Stopping Docker containers..."
    docker-compose down

    print_success "All services stopped!"
}

# Function to show service status
show_status() {
    print_header "Service Status"

    # Check infrastructure
    print_info "Infrastructure Services:"
    docker-compose ps postgres redis kafka zookeeper 2>/dev/null | tail -n +2 | while read line; do
        if echo "$line" | grep -q "Up"; then
            echo -e "  ${GREEN}✅${NC} $line"
        else
            echo -e "  ${RED}❌${NC} $line"
        fi
    done

    echo ""
    print_info "Application Services:"
    for service_config in "${SERVICES[@]}"; do
        IFS=':' read -r service port <<< "$service_config"

        if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            local health_status=$(curl -s "http://localhost:$port/actuator/health" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [ "$health_status" = "UP" ]; then
                echo -e "  ${GREEN}✅${NC} $service - http://localhost:$port (Healthy)"
            else
                echo -e "  ${YELLOW}⚠️${NC} $service - http://localhost:$port (Starting...)"
            fi
        else
            echo -e "  ${RED}❌${NC} $service - http://localhost:$port (Down)"
        fi
    done
}

# Main function
main() {
    local command=${1:-start}

    print_header "BIST Trading Platform - Service Runner"

    case $command in
        "start")
            print_info "Starting BIST Trading Platform services..."

            # Create directories
            mkdir -p logs pids

            # Step 1: Start infrastructure
            print_header "Step 1: Starting Infrastructure Services"

            print_info "Starting Docker Compose services..."
            if docker-compose up -d postgres redis; then
                print_success "Infrastructure services started!"
            else
                print_error "Failed to start infrastructure services"
                exit 1
            fi

            # Wait for database
            print_info "Waiting for PostgreSQL to be ready..."
            sleep 10

            # Optional: Start Kafka
            print_info "Starting optional services (Kafka)..."
            docker-compose up -d kafka zookeeper

            echo ""

            # Step 2: Start application services
            print_header "Step 2: Starting Application Services"

            local started_services=0
            local total_services=${#SERVICES[@]}

            for service_config in "${SERVICES[@]}"; do
                IFS=':' read -r service port <<< "$service_config"

                if start_service $service $port; then
                    started_services=$((started_services + 1))
                else
                    print_warning "Failed to start $service, continuing..."
                fi
                echo ""
                sleep 5  # Give time between service starts
            done

            # Step 3: Final status
            print_header "Step 3: Final Status Check"
            show_status

            echo ""
            if [ $started_services -eq $total_services ]; then
                print_success "🎉 All services started successfully! ($started_services/$total_services)"
                print_info ""
                print_info "Service URLs:"
                print_info "  • User Management: ${CYAN}http://localhost:8081${NC}"
                print_info "  • Market Data: ${CYAN}http://localhost:8082${NC}"
                print_info "  • Broker Integration: ${CYAN}http://localhost:8083${NC}"
                print_info ""
                print_info "Management UIs:"
                print_info "  • Swagger UI (User): ${CYAN}http://localhost:8081/swagger-ui.html${NC}"
                print_info "  • Swagger UI (Market): ${CYAN}http://localhost:8082/swagger-ui.html${NC}"
                print_info "  • Swagger UI (Broker): ${CYAN}http://localhost:8083/swagger-ui.html${NC}"
                print_info ""
                print_info "To stop services: ${YELLOW}./run-all.sh stop${NC}"
            else
                print_warning "⚠️  Some services failed to start ($started_services/$total_services)"
                print_info "Check logs in the 'logs' directory for details"
            fi
            ;;

        "stop")
            stop_all_services
            ;;

        "status")
            show_status
            ;;

        "restart")
            print_info "Restarting all services..."
            stop_all_services
            sleep 3
            main start
            ;;

        *)
            echo "Usage: $0 [start|stop|status|restart]"
            echo ""
            echo "Commands:"
            echo "  start   - Start all services (default)"
            echo "  stop    - Stop all services"
            echo "  status  - Show service status"
            echo "  restart - Restart all services"
            exit 1
            ;;
    esac
}

# Trap Ctrl+C
trap 'echo -e "\n${RED}Interrupted by user${NC}"; stop_all_services; exit 1' INT

# Check dependencies
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed"
    exit 1
fi

if ! command -v curl &> /dev/null; then
    print_error "curl is not installed (needed for health checks)"
    exit 1
fi

if [ ! -f "./gradlew" ]; then
    print_error "gradlew not found in current directory!"
    print_info "Please run this script from the project root directory"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Run main function
main "$@"