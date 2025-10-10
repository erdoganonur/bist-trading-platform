# AlgoLab API Resilience Patterns

## Overview

This document describes the resilience patterns implemented for AlgoLab API integration using Resilience4j. The implementation provides fault tolerance, graceful degradation, and improved system reliability.

**Date:** 2025-10-10
**Version:** 1.0.0
**Status:** ✅ Implemented

---

## Table of Contents

1. [Implemented Patterns](#implemented-patterns)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [Monitoring & Metrics](#monitoring--metrics)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## Implemented Patterns

### 1. Circuit Breaker Pattern

**Purpose:** Prevents cascading failures by stopping requests to a failing service.

**Configuration:**
- **Failure Rate Threshold:** 50% (opens when half of calls fail)
- **Slow Call Threshold:** 5 seconds
- **Wait Duration (Open State):** 60 seconds (30s in dev)
- **Half-Open State:** 10 permitted calls (5 in dev)
- **Sliding Window:** 100 calls (50 in dev)

**States:**
- **CLOSED:** Normal operation, all requests pass through
- **OPEN:** Service is failing, requests are rejected immediately
- **HALF_OPEN:** Testing if service has recovered

### 2. Retry Pattern

**Purpose:** Automatically retries failed requests with exponential backoff.

**Configuration:**
- **Max Attempts:** 3 retries (2 in dev)
- **Initial Wait:** 2 seconds (1s in dev)
- **Backoff Multiplier:** 2x (2s → 4s → 8s)
- **Retry On:**
  - `IOException`
  - `TimeoutException`
  - `ResourceAccessException`
- **Don't Retry On:**
  - `IllegalArgumentException`
  - `AlgoLabAuthenticationException`

### 3. Time Limiter Pattern

**Purpose:** Prevents requests from hanging indefinitely.

**Configuration:**
- **Timeout:** 10 seconds (8s in dev)
- **Cancel Future:** Yes (cancels running request on timeout)

### 4. Fallback Pattern

**Purpose:** Provides graceful degradation when service is unavailable.

**Features:**
- In-memory cache (5 minutes TTL)
- User-friendly error messages
- Cached data for read operations
- Safe failure for write operations

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│              Application Layer                          │
│         (Controllers, Services)                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           AlgoLabRestClient                             │
│   @CircuitBreaker   @Retry   @TimeLimiter              │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌────────────────┐
│ Circuit      │ │ Retry    │ │ Time Limiter   │
│ Breaker      │ │ Logic    │ │ Logic          │
└──────┬───────┘ └────┬─────┘ └────┬───────────┘
       │              │            │
       │              ▼            │
       │     ┌─────────────────┐  │
       │     │  Fallback       │  │
       │     │  Service        │  │
       │     │  (Cache)        │  │
       │     └─────────────────┘  │
       │                           │
       └───────────┬───────────────┘
                   │
                   ▼
         ┌──────────────────┐
         │   AlgoLab API    │
         └──────────────────┘
```

### Request Flow

1. **Normal Request:**
   ```
   Request → Rate Limiter → Circuit Breaker (CLOSED)
         → Retry (if fail) → Time Limiter → AlgoLab API
         → Cache Response → Return Success
   ```

2. **Circuit Breaker OPEN:**
   ```
   Request → Circuit Breaker (OPEN) → Reject Immediately
         → Fallback Service → Return Cached Data or Error
   ```

3. **Retry Scenario:**
   ```
   Request → Attempt 1 (Fail) → Wait 2s
         → Attempt 2 (Fail) → Wait 4s
         → Attempt 3 (Fail) → Fallback
   ```

---

## Configuration

### application.yml

```yaml
resilience4j:
  circuitbreaker:
    instances:
      algolab:
        failure-rate-threshold: 50
        slow-call-duration-threshold: 5s
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 10
        sliding-window-size: 100
        minimum-number-of-calls: 5

  retry:
    instances:
      algolab:
        max-attempts: 3
        wait-duration: 2s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2

  timelimiter:
    instances:
      algolab:
        timeout-duration: 10s
```

### AlgoLabProperties

```yaml
algolab:
  resilience:
    enabled: true
    circuit-breaker:
      failure-rate-threshold: 50
      slow-call-duration-threshold: 5000
      wait-duration-in-open-state: 60000
    retry:
      max-attempts: 3
      wait-duration: 2000
      enable-exponential-backoff: true
    time-limiter:
      timeout-duration: 10000
```

---

## Usage Examples

### 1. Basic Usage (Automatic)

Resilience patterns are applied automatically via annotations:

```java
@Autowired
private AlgoLabRestClient restClient;

// This call is automatically protected by circuit breaker, retry, and timeout
ResponseEntity<Map> response = restClient.post(
    "/api/GetEquityInfo",
    payload,
    true,
    Map.class
);
```

### 2. Async Usage

```java
CompletableFuture<ResponseEntity<Map>> futureResponse = restClient.postAsync(
    "/api/GetEquityInfo",
    payload,
    true,
    Map.class
);

futureResponse.thenAccept(response -> {
    // Handle success
}).exceptionally(throwable -> {
    // Handle failure (after retries and circuit breaker)
    return null;
});
```

### 3. Fallback Handling

Fallback is triggered automatically when:
- Circuit breaker is OPEN
- All retry attempts exhausted
- Request times out

```java
// Fallback returns cached data if available
// or meaningful error message
```

---

## Monitoring & Metrics

### REST API Endpoints

All resilience monitoring endpoints are under `/api/v1/broker/resilience`:

#### Circuit Breaker Status
```bash
GET /api/v1/broker/resilience/circuit-breaker/status
```

**Response:**
```json
{
  "name": "algolab",
  "state": "CLOSED",
  "metrics": {
    "failureRate": 12.5,
    "slowCallRate": 0.0,
    "numberOfSuccessfulCalls": 87,
    "numberOfFailedCalls": 13,
    "numberOfSlowCalls": 0,
    "numberOfNotPermittedCalls": 0
  }
}
```

#### Retry Statistics
```bash
GET /api/v1/broker/resilience/retry/status
```

#### Fallback Cache Stats
```bash
GET /api/v1/broker/resilience/fallback/cache/stats
```

**Response:**
```json
{
  "totalCached": 15,
  "validCached": 12,
  "expiredCached": 3,
  "cacheTtlMs": 300000
}
```

#### Health Check
```bash
GET /api/v1/broker/resilience/health
```

**Response:**
```json
{
  "circuitBreaker": {
    "state": "CLOSED",
    "healthy": true
  },
  "retry": {
    "configured": true,
    "healthy": true
  },
  "fallbackCache": {
    "totalCached": 15,
    "validCached": 12
  },
  "overallStatus": "HEALTHY"
}
```

### Manual Circuit Breaker Control

**Transition to OPEN (for testing):**
```bash
POST /api/v1/broker/resilience/circuit-breaker/transition/OPEN
```

**Reset Circuit Breaker:**
```bash
POST /api/v1/broker/resilience/circuit-breaker/reset
```

**Clear Fallback Cache:**
```bash
POST /api/v1/broker/resilience/fallback/cache/clear
```

### Prometheus Metrics

Available metrics:

- `algolab.api.request` - Request duration timer
  - Tags: `endpoint`, `status`, `error_code`, `exception`

- `algolab.api.fallback` - Fallback counter
  - Tags: `endpoint`, `reason`

- `resilience4j.circuitbreaker.*` - Circuit breaker metrics
- `resilience4j.retry.*` - Retry metrics
- `resilience4j.timelimiter.*` - Time limiter metrics

---

## Testing

### 1. Test Circuit Breaker

**Trigger OPEN state:**

```bash
# 1. Force circuit breaker open
curl -X POST http://localhost:8081/api/v1/broker/resilience/circuit-breaker/transition/OPEN

# 2. Try to make a request (should fail immediately with fallback)
curl -X GET http://localhost:8081/api/v1/broker/portfolio

# 3. Check circuit breaker status
curl -X GET http://localhost:8081/api/v1/broker/resilience/circuit-breaker/status

# 4. Reset circuit breaker
curl -X POST http://localhost:8081/api/v1/broker/resilience/circuit-breaker/reset
```

### 2. Test Retry Mechanism

The retry mechanism is tested automatically when API calls fail. Watch logs:

```
WARN  Retry algolab attempt 1 after error: Connection refused
WARN  Retry algolab attempt 2 after error: Connection refused
ERROR Retry algolab failed after 3 attempts
```

### 3. Test Fallback

```bash
# 1. Stop AlgoLab API or disconnect network
# 2. Make a request
curl -X GET http://localhost:8081/api/v1/broker/portfolio

# 3. Should receive cached data or meaningful error:
{
  "success": false,
  "message": "AlgoLab service is temporarily unavailable. Circuit breaker is OPEN.",
  "error": "SERVICE_UNAVAILABLE",
  "timestamp": "2025-10-10T10:00:00Z"
}
```

### 4. Test Timeout

```java
// Simulate slow response (>10 seconds)
// Should timeout and trigger fallback
```

---

## Troubleshooting

### Circuit Breaker Stuck OPEN

**Problem:** Circuit breaker stays in OPEN state.

**Solutions:**
1. Check AlgoLab API availability
2. Review failure metrics: `/resilience/circuit-breaker/status`
3. Wait for automatic transition to HALF_OPEN (60 seconds)
4. Manually reset: `POST /resilience/circuit-breaker/reset`

### Too Many Retries

**Problem:** Requests are retried too many times, causing delays.

**Solution:** Adjust retry configuration in `application-dev.yml`:
```yaml
resilience4j:
  retry:
    instances:
      algolab:
        max-attempts: 2  # Reduce from 3
        wait-duration: 1s  # Reduce from 2s
```

### Fallback Cache Not Working

**Problem:** Fallback returns errors instead of cached data.

**Causes:**
1. Cache expired (TTL: 5 minutes)
2. No successful request to cache
3. Cache was manually cleared

**Solution:**
```bash
# Check cache stats
curl http://localhost:8081/api/v1/broker/resilience/fallback/cache/stats
```

### Timeouts Too Aggressive

**Problem:** Requests timeout before completing.

**Solution:** Increase timeout in `application.yml`:
```yaml
resilience4j:
  timelimiter:
    instances:
      algolab:
        timeout-duration: 15s  # Increase from 10s
```

---

## Metrics Dashboard

### Grafana Queries

**Circuit Breaker State:**
```promql
resilience4j_circuitbreaker_state{name="algolab"}
```

**Failure Rate:**
```promql
resilience4j_circuitbreaker_failure_rate{name="algolab"}
```

**Request Duration:**
```promql
histogram_quantile(0.95,
  rate(algolab_api_request_seconds_bucket[5m])
)
```

**Fallback Rate:**
```promql
rate(algolab_api_fallback_total[5m])
```

---

## Best Practices

### 1. Don't Retry Everything
- ✅ Retry: Network errors, timeouts, 5xx errors
- ❌ Don't Retry: Authentication errors, validation errors, 4xx errors

### 2. Cache Read Operations Only
- ✅ Cache: Get positions, market data, account info
- ❌ Don't Cache: Order placement, modifications, cancellations

### 3. Monitor Circuit Breaker
- Set up alerts when circuit breaker opens
- Review metrics regularly
- Adjust thresholds based on actual usage

### 4. Test Failure Scenarios
- Regularly test circuit breaker behavior
- Verify fallback responses
- Ensure proper error messages

---

## Integration with Existing Code

The resilience patterns are transparent to existing code. All calls to `AlgoLabRestClient.post()` are automatically protected.

**Before:**
```java
ResponseEntity<Map> response = restClient.post(...);
```

**After (same code, resilience added automatically):**
```java
ResponseEntity<Map> response = restClient.post(...);
// Now includes: circuit breaker, retry, timeout, fallback
```

---

## Summary

**Implemented Features:**
- ✅ Circuit Breaker with 3 states (CLOSED, OPEN, HALF_OPEN)
- ✅ Retry with exponential backoff (3 attempts, 2s → 4s → 8s)
- ✅ Time Limiter (10 second timeout)
- ✅ Fallback with in-memory cache (5 minute TTL)
- ✅ Comprehensive metrics and monitoring
- ✅ REST API for resilience management
- ✅ Prometheus integration
- ✅ Configurable via YAML

**Benefits:**
- 🛡️ Protection against cascading failures
- 🔄 Automatic recovery from transient failures
- ⏱️ Prevention of hanging requests
- 📊 Visibility into system health
- 🎯 Graceful degradation
- 💾 Cached data for high availability

---

**Documentation Date:** 2025-10-10
**Last Updated:** 2025-10-10
**Status:** Production Ready ✅
