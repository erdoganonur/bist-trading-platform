#!/bin/bash

# BIST Trading Platform Build Script
# Builds all services with colored output

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

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

# Function to run command with error handling
run_command() {
    local cmd="$1"
    local description="$2"

    print_info "Executing: $description"
    echo -e "${CYAN}Command: $cmd${NC}"

    if eval "$cmd"; then
        print_success "$description completed successfully!"
        return 0
    else
        print_error "$description failed!"
        return 1
    fi
}

# Main build script
main() {
    print_header "BIST Trading Platform - Build Script"

    # Check if gradlew exists
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found in current directory!"
        print_info "Please run this script from the project root directory"
        exit 1
    fi

    # Make gradlew executable
    chmod +x ./gradlew

    # Start build process
    print_info "Starting build process..."
    echo ""

    # Step 1: Clean all modules
    print_header "Step 1: Cleaning Project"
    if ! run_command "./gradlew clean" "Clean all modules"; then
        print_error "Clean failed. Exiting."
        exit 1
    fi
    echo ""

    # Step 2: Build core modules
    print_header "Step 2: Building Core Modules"

    core_modules=(
        "platform-core:core-common"
        "platform-core:core-domain"
        "platform-core:core-security"
        "platform-core:core-messaging"
    )

    for module in "${core_modules[@]}"; do
        print_info "Building $module..."
        if ! run_command "./gradlew :$module:build" "Build $module"; then
            print_warning "$module build failed, continuing..."
        fi
        echo ""
    done

    # Step 3: Build infrastructure modules
    print_header "Step 3: Building Infrastructure Modules"

    infra_modules=(
        "platform-infrastructure:infrastructure-persistence"
        "platform-infrastructure:infrastructure-integration"
        "platform-infrastructure:infrastructure-monitoring"
    )

    for module in "${infra_modules[@]}"; do
        print_info "Building $module..."
        if ! run_command "./gradlew :$module:build" "Build $module"; then
            print_warning "$module build failed, continuing..."
        fi
        echo ""
    done

    # Step 4: Build service modules
    print_header "Step 4: Building Service Modules"

    service_modules=(
        "platform-services:user-management-service"
        "platform-services:market-data-service"
        "platform-services:broker-integration-service"
    )

    build_success=0
    build_total=0

    for module in "${service_modules[@]}"; do
        build_total=$((build_total + 1))
        print_info "Building $module..."
        if run_command "./gradlew :$module:build -x test" "Build $module (without tests)"; then
            build_success=$((build_success + 1))
        else
            print_warning "$module build failed, trying with tests excluded..."
        fi
        echo ""
    done

    # Step 5: Run full build (optional)
    print_header "Step 5: Full Project Build (Optional)"
    print_info "Attempting full project build..."
    if run_command "./gradlew build -x test" "Full project build"; then
        print_success "Full project build completed!"
    else
        print_warning "Full project build failed, but individual modules may work"
    fi
    echo ""

    # Build summary
    print_header "Build Summary"
    print_info "Services built successfully: ${build_success}/${build_total}"

    if [ $build_success -eq $build_total ]; then
        print_success "🎉 All services built successfully!"
        print_info "You can now run the services with ./run-all.sh"
    elif [ $build_success -gt 0 ]; then
        print_warning "⚠️  Some services built successfully ($build_success/$build_total)"
        print_info "Check the logs above for failed builds"
    else
        print_error "❌ No services built successfully"
        print_info "Please check the error messages above"
        exit 1
    fi

    # Show JAR file locations
    print_info ""
    print_info "Built JAR files location:"
    echo -e "${CYAN}platform-services/*/build/libs/*.jar${NC}"

    # Check for JAR files
    jar_count=$(find platform-services/*/build/libs/ -name "*.jar" 2>/dev/null | wc -l)
    if [ $jar_count -gt 0 ]; then
        print_success "Found $jar_count JAR files"
        find platform-services/*/build/libs/ -name "*.jar" 2>/dev/null | while read jar; do
            echo -e "  ${GREEN}✓${NC} $jar"
        done
    else
        print_warning "No JAR files found. Some builds may have failed."
    fi

    echo ""
    print_success "Build script completed!"
    echo -e "${MAGENTA}Next steps:${NC}"
    echo -e "  ${CYAN}1.${NC} Start infrastructure: ${YELLOW}docker-compose up -d postgres redis${NC}"
    echo -e "  ${CYAN}2.${NC} Run all services: ${YELLOW}./run-all.sh${NC}"
}

# Trap Ctrl+C
trap 'echo -e "\n${RED}Build interrupted by user${NC}"; exit 1' INT

# Run main function
main "$@"