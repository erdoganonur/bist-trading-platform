#!/bin/bash

# BIST Trading Platform - Complete System Starter
# Author: Development Team
# Purpose: Start all services with proper dependency order

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
export SPRING_PROFILES_ACTIVE=development
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# Service definitions with ports
declare -A SERVICES=(
    ["user-management-service"]=8081
    ["order-management-service"]=8082
    ["market-data-service"]=8083
    ["broker-integration-service"]=8084
    ["portfolio-service"]=8085
    ["notification-service"]=8086
    ["api-gateway"]=8080
    ["graphql-gateway"]=8090
)

# Service start order (respecting dependencies)
SERVICE_ORDER=(
    "user-management-service"
    "market-data-service"
    "broker-integration-service"
    "order-management-service"
    "portfolio-service"
    "notification-service"
    "api-gateway"
    "graphql-gateway"
)

# Function to print colored output
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_header() {
    echo -e "\n${MAGENTA}===============================================${NC}"
    echo -e "${MAGENTA}$1${NC}"
    echo -e "${MAGENTA}===============================================${NC}"
}

# Check if port is in use
check_port() {
    local port=$1
    if lsof -i :$port >/dev/null 2>&1; then
        return 1  # Port in use
    else
        return 0  # Port available
    fi
}

# Wait for service health
wait_for_health() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=0

    print_info "Waiting for $service to be healthy on port $port..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            local status=$(curl -s "http://localhost:$port/actuator/health" | jq -r '.status' 2>/dev/null)
            if [ "$status" = "UP" ]; then
                print_success "$service is healthy! ✅"
                return 0
            fi
        fi
        printf "."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo ""
    print_error "$service failed to become healthy ❌"
    return 1
}

# Start a single service
start_service() {
    local service=$1
    local port=${SERVICES[$service]}
    
    print_info "Starting $service on port $port..."
    
    # Check if already running
    if ! check_port $port; then
        print_warning "Port $port already in use. $service might be running."
        if wait_for_health $service $port; then
            return 0
        fi
    fi
    
    # Determine the correct module path
    local module_path=""
    case $service in
        "api-gateway")
            module_path="platform-api-gateway"
            ;;
        "graphql-gateway")
            module_path="platform-graphql-gateway"
            ;;
        *)
            module_path="platform-services:$service"
            ;;
    esac
    
    # Start the service
    mkdir -p logs pids
    nohup ./gradlew :${module_path}:bootRun > logs/${service}.log 2>&1 &
    local pid=$!
    echo $pid > pids/${service}.pid
    
    print_info "$service started with PID $pid"
    
    # Wait for health check
    if wait_for_health $service $port; then
        return 0
    else
        print_error "Failed to start $service"
        kill $pid 2>/dev/null
        return 1
    fi
}

# Stop all services
stop_all() {
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
                    sleep 1
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

# Show service status
show_status() {
    print_header "System Status"
    
    # Infrastructure
    echo -e "\n${CYAN}Infrastructure Services:${NC}"
    for service in postgres redis kafka zookeeper; do
        if docker ps | grep -q "bist-$service"; then
            echo -e "  ${GREEN}✅${NC} $service - Running"
        else
            echo -e "  ${RED}❌${NC} $service - Stopped"
        fi
    done
    
    # Application Services
    echo -e "\n${CYAN}Application Services:${NC}"
    for service in "${SERVICE_ORDER[@]}"; do
        local port=${SERVICES[$service]}
        if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            echo -e "  ${GREEN}✅${NC} $service - http://localhost:$port (UP)"
        else
            echo -e "  ${RED}❌${NC} $service - http://localhost:$port (DOWN)"
        fi
    done
    
    # Special endpoints
    echo -e "\n${CYAN}Service Endpoints:${NC}"
    echo -e "  ${BLUE}GraphQL Playground:${NC} http://localhost:8090/graphiql"
    echo -e "  ${BLUE}API Gateway:${NC} http://localhost:8080"
    echo -e "  ${BLUE}Swagger UI (User):${NC} http://localhost:8081/swagger-ui.html"
    echo -e "  ${BLUE}Swagger UI (Market):${NC} http://localhost:8083/swagger-ui.html"
    
    if [ -f "docker-compose.yml" ]; then
        if docker ps | grep -q "bist-pgadmin"; then
            echo -e "  ${BLUE}pgAdmin:${NC} http://localhost:5050"
        fi
        if docker ps | grep -q "bist-kafka-ui"; then
            echo -e "  ${BLUE}Kafka UI:${NC} http://localhost:8080"
        fi
    fi
}

# Main execution
main() {
    local command=${1:-start}
    
    case $command in
        "start")
            print_header "Starting BIST Trading Platform"
            
            # Step 1: Infrastructure
            print_info "Starting infrastructure services..."
            docker-compose up -d postgres redis
            
            print_info "Waiting for database to be ready..."
            sleep 10
            
            # Optional: Kafka for real-time features
            read -p "Start Kafka for real-time features? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                docker-compose up -d zookeeper kafka
                sleep 5
            fi
            
            # Step 2: Database Migration
            print_info "Running database migrations..."
            ./gradlew flywayMigrate || print_warning "Migration failed, continuing..."
            
            # Step 3: Start Services
            local success_count=0
            local total_count=${#SERVICE_ORDER[@]}
            
            for service in "${SERVICE_ORDER[@]}"; do
                echo -e "\n"
                if start_service $service; then
                    success_count=$((success_count + 1))
                else
                    print_warning "Failed to start $service"
                fi
                sleep 3  # Give services time to stabilize
            done
            
            # Step 4: Final Status
            echo -e "\n"
            print_header "Startup Complete"
            
            if [ $success_count -eq $total_count ]; then
                print_success "🎉 All services started successfully! ($success_count/$total_count)"
            else
                print_warning "⚠️  Some services failed ($success_count/$total_count started)"
            fi
            
            show_status
            
            echo -e "\n${GREEN}You can now access:${NC}"
            echo -e "  ${CYAN}GraphQL Playground:${NC} http://localhost:8090/graphiql"
            echo -e "  ${CYAN}REST API Gateway:${NC} http://localhost:8080"
            echo -e "\n${YELLOW}To stop all services:${NC} ./start-all-services.sh stop"
            ;;
            
        "stop")
            stop_all
            ;;
            
        "restart")
            stop_all
            sleep 3
            main start
            ;;
            
        "status")
            show_status
            ;;
            
        "logs")
            local service=${2:-graphql-gateway}
            if [ -f "logs/${service}.log" ]; then
                tail -f "logs/${service}.log"
            else
                print_error "Log file not found for $service"
            fi
            ;;
            
        *)
            echo "Usage: $0 [start|stop|restart|status|logs <service>]"
            echo ""
            echo "Commands:"
            echo "  start   - Start all services"
            echo "  stop    - Stop all services"
            echo "  restart - Restart all services"
            echo "  status  - Show service status"
            echo "  logs    - Tail logs for a service"
            exit 1
            ;;
    esac
}

# Check dependencies
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose not found. Please install Docker Desktop"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    print_warning "jq not found. Installing..."
    brew install jq 2>/dev/null || apt-get install jq 2>/dev/null || print_warning "Please install jq manually"
fi

if [ ! -f "./gradlew" ]; then
    print_error "gradlew not found. Please run from project root"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Trap Ctrl+C
trap 'echo -e "\n${RED}Interrupted by user${NC}"; stop_all; exit 1' INT

# Run main function
main "$@"
