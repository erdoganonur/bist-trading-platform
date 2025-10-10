# Redis Caching Implementation Guide

**Date:** 2025-10-10
**Version:** 1.0.0
**Status:** ✅ Production Ready

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Cache Configuration](#cache-configuration)
4. [Usage Examples](#usage-examples)
5. [API Endpoints](#api-endpoints)
6. [Monitoring](#monitoring)
7. [Performance Metrics](#performance-metrics)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

---

## Overview

Redis caching layer for AlgoLab API integration provides:

- **Automatic caching** with Spring Cache annotations
- **Different TTLs** per cache region (30s to 1 hour)
- **Cache warmup** on startup (BIST30 popular symbols)
- **Scheduled refresh** every 30 minutes
- **Cache statistics** tracking (hits, misses, hit rate)
- **Manual cache management** via REST API
- **Graceful degradation** if Redis unavailable

### Performance Impact

**Without Cache:**
- API call latency: ~50-200ms per request
- Rate limited: 1 request per 5 seconds
- 1000 requests = ~50-200 seconds

**With Cache:**
- Cache hit latency: <5ms
- 1000 requests (90% cache hit) = ~5 seconds
- Rate limit impact: only 10% of requests hit API

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────┐
│  AlgoLabMarketDataService                               │
│  ┌──────────────────────────────────────────────┐       │
│  │  @Cacheable getSymbolInfo(symbol)            │       │
│  │  @Cacheable getCandleData(symbol, period)    │       │
│  │  @CachePut updateSymbolInfoCache()           │       │
│  │  @CacheEvict invalidateSymbolCache()         │       │
│  └──────────────────────────────────────────────┘       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  RedisCacheManager     │
        │  ┌──────────────────┐  │
        │  │ symbols (1h)     │  │
        │  │ candles (5m)     │  │
        │  │ positions (1m)   │  │
        │  │ orders (30s)     │  │
        │  │ market-data (30s)│  │
        │  │ account-info (5m)│  │
        │  └──────────────────┘  │
        └────────────┬───────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  Redis Server          │
        │  localhost:6379        │
        │  Password: redis_pass  │
        └────────────────────────┘
```

### Cache Regions

| Cache Name | TTL | Use Case | Update Frequency |
|------------|-----|----------|------------------|
| `algolab:symbols` | 1 hour | Symbol info (price, ceiling, floor) | Rarely changes |
| `algolab:candles` | 5 minutes | OHLCV candle data | Every bar completion |
| `algolab:positions` | 1 minute | User positions | Every trade |
| `algolab:orders` | 30 seconds | Order status | Very frequent |
| `algolab:market-data` | 30 seconds | Real-time tick data | Continuous |
| `algolab:account-info` | 5 minutes | Balance, buying power | After trades |

---

## Cache Configuration

### Spring Configuration

**File:** `RedisCacheConfig.java`

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    // Cache names
    public static class CacheNames {
        public static final String SYMBOLS = "algolab:symbols";
        public static final String CANDLES = "algolab:candles";
        // ... other caches
    }

    // TTL configurations
    public static class CacheTTL {
        public static final Duration SYMBOLS = Duration.ofHours(1);
        public static final Duration CANDLES = Duration.ofMinutes(5);
        // ... other TTLs
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // Configure cache manager with custom TTLs
    }
}
```

### Redis Configuration

**File:** `application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redis_password
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

**File:** `application-dev.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # No password in dev
      database: 1  # Different database
```

---

## Usage Examples

### Example 1: Basic Caching

```java
// First call - Cache MISS (hits API)
Map<String, Object> symbolInfo1 = marketDataService.getSymbolInfo("GARAN");
// Log: "Cache MISS - Fetching symbol info from API: GARAN"
// API call latency: ~100ms

// Second call - Cache HIT (from Redis)
Map<String, Object> symbolInfo2 = marketDataService.getSymbolInfo("GARAN");
// No API call, data from cache
// Cache hit latency: <5ms
```

### Example 2: Cache Invalidation

```java
// Invalidate specific symbol
marketDataService.invalidateSymbolCache("GARAN");

// Next call will be cache MISS (hits API again)
Map<String, Object> symbolInfo = marketDataService.getSymbolInfo("GARAN");
```

### Example 3: Cache Update (WebSocket)

```java
// When WebSocket receives real-time update
WebSocketMessage tickData = parseTickData(message);

// Update cache without waiting for next API call
marketDataService.updateSymbolInfoCache(
    tickData.getSymbol(),
    tickData.getData()
);
```

### Example 4: Bulk Invalidation

```java
// Invalidate all caches for a symbol (symbol, candles, market data)
marketDataService.invalidateAllCachesForSymbol("GARAN");

// Or invalidate specific cache region
marketDataService.invalidateAllSymbolCache();
```

---

## API Endpoints

### Cache Statistics

```bash
# Get cache statistics
GET /api/v1/cache/statistics

# Response
{
  "cacheHits": 8542,
  "cacheMisses": 1458,
  "totalRequests": 10000,
  "hitRate": "85.42",
  "cacheNames": {
    "symbols": "active",
    "candles": "active",
    "positions": "active",
    "orders": "active",
    "marketData": "active",
    "accountInfo": "active"
  }
}
```

### Cache Invalidation

```bash
# Invalidate specific symbol
DELETE /api/v1/cache/symbols/GARAN
Authorization: Bearer <token>

# Invalidate specific candle
DELETE /api/v1/cache/candles/GARAN/5M
Authorization: Bearer <token>

# Clear all symbol cache (Admin only)
DELETE /api/v1/cache/symbols
Authorization: Bearer <admin_token>

# Clear ALL caches (Admin only, DANGEROUS)
DELETE /api/v1/cache/all
Authorization: Bearer <admin_token>
```

### Cache Warmup

```bash
# Trigger manual warmup
POST /api/v1/cache/warmup
Authorization: Bearer <token>

# Response
{
  "success": "true",
  "message": "Cache warmup triggered (running in background)"
}
```

### Cache Health

```bash
# Check cache health
GET /api/v1/cache/health

# Response
{
  "status": "UP",
  "totalRequests": 10000,
  "hitRate": "85.42%",
  "cacheRegions": {
    "symbols": "active",
    "candles": "active",
    ...
  }
}
```

---

## Monitoring

### Application Logs

**Startup:**
```log
INFO  RedisCacheConfig - Configuring Redis cache manager for AlgoLab integration
INFO  RedisCacheConfig - Redis cache manager configured with 6 cache regions
INFO  AlgoLabCacheService - Starting cache warmup for AlgoLab data...
INFO  AlgoLabCacheService - Cache warmup completed. Warmed 10 / 10 symbols in 1234ms
```

**Cache Operations:**
```log
DEBUG AlgoLabMarketDataService - Cache MISS - Fetching symbol info from API: GARAN
DEBUG AlgoLabMarketDataService - Symbol info fetched successfully for: GARAN
DEBUG AlgoLabMarketDataService - Invalidated symbol cache for: GARAN
```

**Scheduled Tasks:**
```log
INFO  AlgoLabCacheService - === AlgoLab Cache Statistics ===
INFO  AlgoLabCacheService - Total Requests: 10000
INFO  AlgoLabCacheService - Cache Hits: 8542
INFO  AlgoLabCacheService - Cache Misses: 1458
INFO  AlgoLabCacheService - Hit Rate: 85.42%
INFO  AlgoLabCacheService - ================================
```

### Redis CLI Monitoring

```bash
# Connect to Redis
redis-cli -h localhost -p 6379 -a redis_password

# Check cached keys
KEYS bist:algolab:*

# Example output
1) "bist:algolab:algolab:symbols::GARAN"
2) "bist:algolab:algolab:symbols::AKBNK"
3) "bist:algolab:algolab:candles::GARAN_5"

# Get cache entry
GET "bist:algolab:algolab:symbols::GARAN"

# Check TTL (time to live)
TTL "bist:algolab:algolab:symbols::GARAN"
# Returns seconds until expiration
# Example: 3456 (57 minutes remaining)

# Monitor cache operations in real-time
MONITOR

# Get cache size
DBSIZE

# Get memory usage
INFO memory
```

### Prometheus Metrics

Cache metrics are exposed via Spring Boot Actuator:

```bash
# Get cache metrics
curl http://localhost:8080/actuator/metrics/cache.gets?tag=name:algolab:symbols

# Response
{
  "name": "cache.gets",
  "measurements": [
    {"statistic": "COUNT", "value": 1000}
  ],
  "availableTags": [
    {"tag": "result", "values": ["hit", "miss"]}
  ]
}

# Get hit rate
curl http://localhost:8080/actuator/metrics/cache.gets?tag=name:algolab:symbols&tag=result:hit
```

---

## Performance Metrics

### Expected Cache Hit Rates

Based on typical trading patterns:

| Cache | Expected Hit Rate | Reasoning |
|-------|------------------|-----------|
| Symbols | >90% | Symbol info rarely changes |
| Candles | >70% | Updates every 5 minutes |
| Positions | >50% | Frequently updated but repeatedly queried |
| Orders | >40% | Very dynamic but high query frequency |
| Market Data | >60% | 30s TTL with continuous queries |
| Account Info | >80% | Infrequent updates, frequent queries |

### Performance Comparison

**Scenario: 1000 requests for GARAN symbol info**

| Metric | Without Cache | With Cache (85% hit rate) |
|--------|--------------|---------------------------|
| Total API Calls | 1000 | 150 |
| Total Time | ~50-200 seconds | ~5 seconds |
| Average Latency | 50-200ms | ~8ms |
| API Rate Limit Impact | HIGH (blocked) | LOW (minimal) |

### Memory Usage

**Per Cache Entry:**
- Symbol info: ~2-5 KB
- Candle data (250 bars): ~50-100 KB
- Total memory (10 popular symbols): ~500 KB - 1 MB

**Redis Memory Configuration:**
```yaml
# application.yml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8  # Max connections
          max-idle: 8
          min-idle: 2
```

---

## Troubleshooting

### Problem 1: Redis Connection Failed

**Symptom:**
```log
ERROR - Could not connect to Redis at localhost:6379
```

**Solutions:**

1. **Check Redis is running:**
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

2. **Check Redis password:**
   ```bash
   redis-cli -a redis_password ping
   ```

3. **Verify configuration:**
   ```yaml
   spring:
     data:
       redis:
         host: localhost
         port: 6379
         password: redis_password
   ```

4. **Graceful Degradation:**
   - Application continues without cache
   - Falls back to AlgoLabFallbackService in-memory cache
   - Logs warning: "Redis unavailable, using fallback cache"

### Problem 2: Low Cache Hit Rate

**Symptom:**
```log
Cache hit rate: 25.42% (expected >80%)
```

**Causes & Solutions:**

1. **TTL too short:**
   - Increase TTL in `RedisCacheConfig.CacheTTL`
   - Symbols: 1 hour → 2 hours
   - Candles: 5 minutes → 10 minutes

2. **Cache keys not matching:**
   - Verify symbol names are consistent (case-sensitive)
   - "GARAN" ≠ "garan"
   - Check logs for cache key format

3. **Frequent cache invalidation:**
   - Review WebSocket update logic
   - Reduce aggressive cache eviction

4. **High unique symbol requests:**
   - Normal if users query many different symbols
   - Consider expanding warmup symbols

### Problem 3: Cache Not Expiring

**Symptom:**
```log
Symbol data is stale, but cache still serving old data
```

**Solutions:**

1. **Manual invalidation:**
   ```bash
   DELETE /api/v1/cache/symbols/GARAN
   ```

2. **Check TTL:**
   ```bash
   redis-cli
   TTL "bist:algolab:algolab:symbols::GARAN"
   # Should return seconds remaining
   ```

3. **Force eviction:**
   ```java
   marketDataService.invalidateSymbolCache("GARAN");
   ```

4. **Clear all caches:**
   ```bash
   DELETE /api/v1/cache/all
   ```

### Problem 4: High Memory Usage

**Symptom:**
```log
Redis memory usage: 512 MB (expected <100 MB)
```

**Solutions:**

1. **Check cache size:**
   ```bash
   redis-cli
   DBSIZE
   # Returns number of keys
   ```

2. **Find large keys:**
   ```bash
   redis-cli --bigkeys
   ```

3. **Reduce TTL:**
   - Shorten cache expiration times
   - Symbols: 1 hour → 30 minutes

4. **Configure max memory:**
   ```bash
   # redis.conf
   maxmemory 256mb
   maxmemory-policy allkeys-lru
   ```

---

## Best Practices

### 1. Cache Key Design

**Good:**
```java
@Cacheable(key = "#symbol")  // Simple, consistent
@Cacheable(key = "#symbol + '_' + #period")  // Composite key
```

**Bad:**
```java
@Cacheable(key = "#symbol.toLowerCase()")  // Inconsistent casing
@Cacheable(key = "#symbol + Math.random()")  // Non-deterministic
```

### 2. Cache Eviction Strategy

**Proactive Eviction:**
```java
// On WebSocket bar completion
@Override
public void onBarComplete(String symbol, String period) {
    marketDataService.invalidateCandleCache(symbol, periodToMinutes(period));
}
```

**Lazy Eviction:**
```java
// Let TTL handle expiration (preferred for most cases)
@Cacheable(value = "algolab:symbols")  // Expires after 1 hour automatically
```

### 3. Cache Warmup

**On Startup:**
```java
@PostConstruct
public void warmupCache() {
    // Warm up popular symbols
    for (String symbol : BIST30_SYMBOLS) {
        marketDataService.getSymbolInfo(symbol);
    }
}
```

**Periodic Refresh:**
```java
@Scheduled(cron = "0 */30 * * * *")
public void periodicCacheWarmup() {
    // Refresh popular symbols before cache expires
}
```

### 4. Error Handling

**With Fallback:**
```java
@Cacheable(value = "algolab:symbols", unless = "#result == null")
public Map<String, Object> getSymbolInfo(String symbol) {
    try {
        return apiClient.getSymbolInfo(symbol);
    } catch (Exception e) {
        // Don't cache errors
        log.error("API call failed", e);
        return fallbackService.getSymbolInfo(symbol);  // From in-memory cache
    }
}
```

### 5. Monitoring

**Track Metrics:**
```java
@Cacheable(value = "algolab:symbols")
public Map<String, Object> getSymbolInfo(String symbol) {
    cacheService.recordCacheMiss();  // Track manually
    return apiClient.getSymbolInfo(symbol);
}
```

**Log Important Events:**
```java
log.info("Cache hit rate: {}%", hitRate);  // Hourly
log.warn("Cache hit rate below threshold: {}%", hitRate);  // Alert if <80%
```

### 6. Security

**Protect Admin Endpoints:**
```java
@DeleteMapping("/cache/all")
@PreAuthorize("hasRole('ADMIN')")  // Only admins can clear all caches
public void clearAllCaches() {
    cacheService.clearAllCaches();
}
```

**Audit Cache Operations:**
```java
@DeleteMapping("/cache/symbols/{symbol}")
public void invalidateSymbol(@PathVariable String symbol) {
    log.info("User {} invalidated symbol cache: {}",
        SecurityContextHolder.getContext().getAuthentication().getName(),
        symbol
    );
    cacheService.invalidateSymbol(symbol);
}
```

---

## Success Criteria

✅ **All criteria met:**

- [x] Cache hit rate >80% after 1 hour of operation
- [x] Cache warmup completes in <5 seconds on startup
- [x] Cache statistics logged every hour
- [x] Manual cache management methods work
- [x] No errors in logs related to caching
- [x] Performance improvement visible (lower API call count)
- [x] Graceful degradation if Redis unavailable
- [x] All cache regions properly configured with TTLs
- [x] REST API endpoints for cache management
- [x] Comprehensive monitoring and logging

---

## References

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [Resilience4j Integration](https://resilience4j.readme.io/docs/cache)

---

**Last Updated:** 2025-10-10
**Status:** ✅ Production Ready
**Next Review:** 2025-11-10
