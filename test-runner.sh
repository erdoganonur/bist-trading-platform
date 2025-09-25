#!/bin/bash

# BIST Trading Platform Test Runner Script
# Runs all tests, generates coverage reports, and creates test-results.md

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test configuration
TEST_RESULTS_FILE="test-results.md"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Module configuration
CORE_MODULES=(
    "platform-core:core-common"
    "platform-core:core-domain"
    "platform-core:core-security"
    "platform-core:core-messaging"
)

INFRASTRUCTURE_MODULES=(
    "platform-infrastructure:infrastructure-persistence"
    "platform-infrastructure:infrastructure-integration"
    "platform-infrastructure:infrastructure-monitoring"
)

SERVICE_MODULES=(
    "platform-services:user-management-service"
    "platform-services:market-data-service"
    "platform-services:broker-integration-service"
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

# Function to initialize test results file
initialize_results_file() {
    cat > "$TEST_RESULTS_FILE" << EOF
# BIST Trading Platform - Test Results

**Test Run Date:** $TIMESTAMP
**Platform:** $(uname -s) $(uname -r)
**Java Version:** $(java -version 2>&1 | head -n 1)

---

## Executive Summary

EOF
}

# Function to extract test statistics from Gradle output
extract_test_stats() {
    local gradle_output="$1"
    local module_name="$2"

    # Extract test counts using different patterns
    local tests_run=0
    local tests_passed=0
    local tests_failed=0
    local tests_skipped=0

    # Pattern 1: "X tests completed, Y failed, Z skipped"
    if echo "$gradle_output" | grep -q "tests completed"; then
        tests_run=$(echo "$gradle_output" | grep "tests completed" | tail -1 | sed 's/.*BUILD SUCCESSFUL.*//' | sed 's/.* \([0-9]\+\) tests completed.*/\1/' | head -1)
        tests_failed=$(echo "$gradle_output" | grep "tests completed" | tail -1 | sed 's/.* \([0-9]\+\) failed.*/\1/' | head -1)
        tests_skipped=$(echo "$gradle_output" | grep "tests completed" | tail -1 | sed 's/.* \([0-9]\+\) skipped.*/\1/' | head -1)
    fi

    # Pattern 2: Look for JUnit output
    if echo "$gradle_output" | grep -q "Test run finished"; then
        tests_run=$(echo "$gradle_output" | grep -o "[0-9]\+ tests" | tail -1 | grep -o "[0-9]\+")
    fi

    # Default values if extraction fails
    if [[ ! "$tests_run" =~ ^[0-9]+$ ]]; then tests_run=0; fi
    if [[ ! "$tests_failed" =~ ^[0-9]+$ ]]; then tests_failed=0; fi
    if [[ ! "$tests_skipped" =~ ^[0-9]+$ ]]; then tests_skipped=0; fi

    tests_passed=$((tests_run - tests_failed - tests_skipped))
    if [ $tests_passed -lt 0 ]; then tests_passed=0; fi

    # Update global counters
    TOTAL_TESTS=$((TOTAL_TESTS + tests_run))
    PASSED_TESTS=$((PASSED_TESTS + tests_passed))
    FAILED_TESTS=$((FAILED_TESTS + tests_failed))
    SKIPPED_TESTS=$((SKIPPED_TESTS + tests_skipped))

    # Return values
    echo "$tests_run:$tests_passed:$tests_failed:$tests_skipped"
}

# Function to extract coverage percentage
extract_coverage() {
    local module_name="$1"
    local coverage_file

    # Look for JaCoCo coverage files
    coverage_file=$(find . -path "*/$module_name/build/reports/jacoco/test/html/index.html" 2>/dev/null | head -1)

    if [ -f "$coverage_file" ]; then
        # Extract coverage percentage from HTML file
        local coverage=$(grep -o "Total[^%]*[0-9]\+%" "$coverage_file" 2>/dev/null | grep -o "[0-9]\+%" | head -1 | grep -o "[0-9]\+")
        if [[ "$coverage" =~ ^[0-9]+$ ]]; then
            echo "${coverage}%"
        else
            echo "N/A"
        fi
    else
        echo "N/A"
    fi
}

# Function to run tests for a module
run_module_tests() {
    local module="$1"
    local module_display_name="$2"
    local test_type="$3"

    print_info "Running $test_type tests for $module_display_name..."

    # Create temporary file for gradle output
    local temp_output=$(mktemp)
    local start_time=$(date +%s)

    # Run tests with coverage
    if ./gradlew ":$module:clean" ":$module:test" ":$module:jacocoTestReport" > "$temp_output" 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))

        print_success "$module_display_name tests completed in ${duration}s"

        # Extract test statistics
        local stats=$(extract_test_stats "$(cat "$temp_output")" "$module")
        IFS=':' read -r tests_run tests_passed tests_failed tests_skipped <<< "$stats"

        # Extract coverage
        local coverage=$(extract_coverage "$module")

        # Determine status
        local status="✅ PASSED"
        local status_color="🟢"
        if [ $tests_failed -gt 0 ]; then
            status="❌ FAILED"
            status_color="🔴"
        elif [ $tests_run -eq 0 ]; then
            status="⚠️ NO TESTS"
            status_color="🟡"
        fi

        # Write to results file
        cat >> "$TEST_RESULTS_FILE" << EOF

### $status_color $module_display_name
- **Status:** $status
- **Duration:** ${duration}s
- **Tests Run:** $tests_run
- **Passed:** $tests_passed
- **Failed:** $tests_failed
- **Skipped:** $tests_skipped
- **Coverage:** $coverage

EOF

        # Add failed test details if any
        if [ $tests_failed -gt 0 ]; then
            echo "**Failed Tests:**" >> "$TEST_RESULTS_FILE"
            echo '```' >> "$TEST_RESULTS_FILE"
            grep -A 5 -B 5 "FAILED\|ERROR" "$temp_output" | head -20 >> "$TEST_RESULTS_FILE" 2>/dev/null
            echo '```' >> "$TEST_RESULTS_FILE"
            echo "" >> "$TEST_RESULTS_FILE"
        fi

        return 0
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))

        print_error "$module_display_name tests failed after ${duration}s"

        # Write failure to results file
        cat >> "$TEST_RESULTS_FILE" << EOF

### 🔴 $module_display_name
- **Status:** ❌ BUILD FAILED
- **Duration:** ${duration}s
- **Error:** Build/Test execution failed

**Error Output:**
\`\`\`
$(tail -30 "$temp_output")
\`\`\`

EOF

        rm -f "$temp_output"
        return 1
    fi

    rm -f "$temp_output"
}

# Function to run integration tests
run_integration_tests() {
    print_header "Integration Tests"

    cat >> "$TEST_RESULTS_FILE" << EOF
## Integration Tests

EOF

    local integration_modules=(
        "platform-services:broker-integration-service"
        "platform-services:market-data-service"
    )

    for module in "${integration_modules[@]}"; do
        local service_name=$(echo "$module" | cut -d':' -f2)
        print_info "Running integration tests for $service_name..."

        local temp_output=$(mktemp)
        local start_time=$(date +%s)

        # Try to run integration tests (may not exist for all modules)
        if ./gradlew ":$module:integrationTest" > "$temp_output" 2>&1; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))

            print_success "$service_name integration tests completed"

            cat >> "$TEST_RESULTS_FILE" << EOF
### 🟢 $service_name Integration Tests
- **Status:** ✅ PASSED
- **Duration:** ${duration}s

EOF
        else
            # Check if integration tests exist
            if grep -q "Task 'integrationTest' not found" "$temp_output"; then
                print_warning "$service_name - No integration tests found"
                cat >> "$TEST_RESULTS_FILE" << EOF
### 🟡 $service_name Integration Tests
- **Status:** ⚠️ NO INTEGRATION TESTS
- **Note:** Integration test task not configured

EOF
            else
                print_error "$service_name integration tests failed"
                cat >> "$TEST_RESULTS_FILE" << EOF
### 🔴 $service_name Integration Tests
- **Status:** ❌ FAILED

**Error Output:**
\`\`\`
$(tail -20 "$temp_output")
\`\`\`

EOF
            fi
        fi

        rm -f "$temp_output"
    done
}

# Function to generate final summary
generate_summary() {
    local overall_status="✅ PASSED"
    local status_color="🟢"

    if [ $FAILED_TESTS -gt 0 ]; then
        overall_status="❌ FAILED"
        status_color="🔴"
    elif [ $TOTAL_TESTS -eq 0 ]; then
        overall_status="⚠️ NO TESTS"
        status_color="🟡"
    fi

    local success_rate=0
    if [ $TOTAL_TESTS -gt 0 ]; then
        success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    fi

    # Update summary in results file
    sed -i '' '/## Executive Summary/,$d' "$TEST_RESULTS_FILE" 2>/dev/null || true

    cat >> "$TEST_RESULTS_FILE" << EOF
## Executive Summary

| Metric | Value |
|--------|-------|
| **Overall Status** | $status_color $overall_status |
| **Total Tests** | $TOTAL_TESTS |
| **Passed** | $PASSED_TESTS |
| **Failed** | $FAILED_TESTS |
| **Skipped** | $SKIPPED_TESTS |
| **Success Rate** | ${success_rate}% |

---

## Test Results by Module

EOF

    print_header "Test Run Summary"
    print_info "Overall Status: $overall_status"
    print_info "Total Tests: $TOTAL_TESTS"
    print_info "Passed: $PASSED_TESTS"
    print_info "Failed: $FAILED_TESTS"
    print_info "Skipped: $SKIPPED_TESTS"
    print_info "Success Rate: ${success_rate}%"
}

# Function to run performance tests
run_performance_tests() {
    print_header "Performance Tests"

    cat >> "$TEST_RESULTS_FILE" << EOF

## Performance Tests

EOF

    local perf_modules=(
        "platform-services:market-data-service"
    )

    for module in "${perf_modules[@]}"; do
        local service_name=$(echo "$module" | cut -d':' -f2)
        print_info "Running performance tests for $service_name..."

        local temp_output=$(mktemp)

        # Look for performance test classes
        if find . -path "*/$service_name/src/test/java/**/*Performance*Test.java" -o -path "*/$service_name/src/test/java/**/*Benchmark*.java" | grep -q .; then
            if ./gradlew ":$module:test" --tests "*Performance*" --tests "*Benchmark*" > "$temp_output" 2>&1; then
                print_success "$service_name performance tests completed"
                cat >> "$TEST_RESULTS_FILE" << EOF
### 🟢 $service_name Performance Tests
- **Status:** ✅ PASSED
- **Note:** Performance benchmarks executed successfully

EOF
            else
                print_error "$service_name performance tests failed"
                cat >> "$TEST_RESULTS_FILE" << EOF
### 🔴 $service_name Performance Tests
- **Status:** ❌ FAILED

EOF
            fi
        else
            print_warning "$service_name - No performance tests found"
            cat >> "$TEST_RESULTS_FILE" << EOF
### 🟡 $service_name Performance Tests
- **Status:** ⚠️ NO PERFORMANCE TESTS

EOF
        fi

        rm -f "$temp_output"
    done
}

# Main function
main() {
    local test_type=${1:-all}

    print_header "BIST Trading Platform - Test Runner"
    print_info "Test type: $test_type"
    print_info "Results will be written to: $TEST_RESULTS_FILE"

    # Check prerequisites
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found in current directory!"
        exit 1
    fi

    chmod +x ./gradlew

    # Initialize results file
    initialize_results_file

    case $test_type in
        "all"|"unit")
            # Run core module tests
            print_header "Core Module Tests"
            cat >> "$TEST_RESULTS_FILE" << EOF
## Core Module Tests

EOF
            for module in "${CORE_MODULES[@]}"; do
                local module_name=$(echo "$module" | cut -d':' -f2)
                run_module_tests "$module" "$module_name" "unit"
            done

            # Run infrastructure module tests
            print_header "Infrastructure Module Tests"
            cat >> "$TEST_RESULTS_FILE" << EOF
## Infrastructure Module Tests

EOF
            for module in "${INFRASTRUCTURE_MODULES[@]}"; do
                local module_name=$(echo "$module" | cut -d':' -f2)
                run_module_tests "$module" "$module_name" "unit"
            done

            # Run service module tests
            print_header "Service Module Tests"
            cat >> "$TEST_RESULTS_FILE" << EOF
## Service Module Tests

EOF
            for module in "${SERVICE_MODULES[@]}"; do
                local module_name=$(echo "$module" | cut -d':' -f2)
                run_module_tests "$module" "$module_name" "unit"
            done

            if [ "$test_type" = "all" ]; then
                run_integration_tests
                run_performance_tests
            fi
            ;;

        "integration")
            run_integration_tests
            ;;

        "performance")
            run_performance_tests
            ;;

        *)
            echo "Usage: $0 [all|unit|integration|performance]"
            echo ""
            echo "Test Types:"
            echo "  all           - Run all tests (default)"
            echo "  unit          - Run only unit tests"
            echo "  integration   - Run only integration tests"
            echo "  performance   - Run only performance tests"
            exit 1
            ;;
    esac

    # Generate final summary
    generate_summary

    # Add footer
    cat >> "$TEST_RESULTS_FILE" << EOF

---

## Coverage Reports

Individual module coverage reports can be found at:
- \`platform-core/*/build/reports/jacoco/test/html/index.html\`
- \`platform-infrastructure/*/build/reports/jacoco/test/html/index.html\`
- \`platform-services/*/build/reports/jacoco/test/html/index.html\`

## Test Reports

Detailed test reports can be found at:
- \`platform-services/*/build/reports/tests/test/index.html\`

---

*Test run completed at: $(date '+%Y-%m-%d %H:%M:%S')*
EOF

    # Final output
    echo ""
    print_success "Test run completed!"
    print_info "Results written to: $TEST_RESULTS_FILE"
    print_info "View with: cat $TEST_RESULTS_FILE"

    # Return appropriate exit code
    if [ $FAILED_TESTS -gt 0 ]; then
        print_error "Some tests failed. Check $TEST_RESULTS_FILE for details."
        exit 1
    else
        print_success "All tests passed! 🎉"
        exit 0
    fi
}

# Trap Ctrl+C
trap 'echo -e "\n${RED}Test run interrupted by user${NC}"; exit 1' INT

# Run main function
main "$@"