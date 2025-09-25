package com.bisttrading.caching.warming;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for cache warming functionality
 * Allows fine-tuning of cache warming behavior per environment
 */
@Configuration
@ConfigurationProperties(prefix = "cache.warming")
public class CacheWarmingConfig {

    /**
     * Enable or disable cache warming on startup
     */
    private boolean startupWarmingEnabled = true;

    /**
     * Enable or disable scheduled cache refresh
     */
    private boolean scheduledRefreshEnabled = true;

    /**
     * Delay between warming up different symbols (milliseconds)
     */
    private long symbolDelayMs = 100;

    /**
     * Interval for scheduled cache refresh (milliseconds)
     */
    private long refreshIntervalMs = 300_000; // 5 minutes

    /**
     * Minimum TTL in seconds to skip warming up existing cache entries
     */
    private long minTtlSecondsForSkip = 300; // 5 minutes

    /**
     * Default TTL for different cache key patterns
     */
    private Map<String, Duration> ttlByPattern = new HashMap<>();

    /**
     * Thread pool size for cache warming operations
     */
    private int warmingThreadPoolSize = 4;

    /**
     * Maximum time to wait for cache warming completion (milliseconds)
     */
    private long warmingTimeoutMs = 30_000; // 30 seconds

    public CacheWarmingConfig() {
        // Initialize default TTL values
        initializeDefaultTtl();
    }

    private void initializeDefaultTtl() {
        // Market data - short TTL for real-time data
        ttlByPattern.put("market:data:*", Duration.ofSeconds(30));
        ttlByPattern.put("market:orderbook:*", Duration.ofSeconds(10));

        // OHLCV data - medium TTL based on timeframe
        ttlByPattern.put("market:ohlcv:*:1m", Duration.ofMinutes(1));
        ttlByPattern.put("market:ohlcv:*:5m", Duration.ofMinutes(5));
        ttlByPattern.put("market:ohlcv:*:1h", Duration.ofHours(1));
        ttlByPattern.put("market:ohlcv:*:1d", Duration.ofHours(24));

        // Symbol data - longer TTL as it changes infrequently
        ttlByPattern.put("market:symbols:*", Duration.ofHours(1));

        // User sessions - medium TTL
        ttlByPattern.put("user:sessions:*", Duration.ofMinutes(30));
        ttlByPattern.put("user:*:profile", Duration.ofMinutes(15));

        // Trading parameters - long TTL as they're mostly static
        ttlByPattern.put("trading:hours:*", Duration.ofHours(24));
        ttlByPattern.put("trading:fees:*", Duration.ofHours(24));
        ttlByPattern.put("trading:market:status", Duration.ofMinutes(5));

        // Analytics data - medium TTL
        ttlByPattern.put("analytics:*", Duration.ofMinutes(10));

        // Default fallback TTL
        ttlByPattern.put("*", Duration.ofMinutes(10));
    }

    /**
     * Get appropriate TTL for a cache key based on pattern matching
     *
     * @param key the cache key
     * @return the TTL duration
     */
    public Duration getDefaultTtlForKey(String key) {
        // Find the most specific pattern match
        Duration ttl = null;
        int longestMatch = 0;

        for (Map.Entry<String, Duration> entry : ttlByPattern.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(key, pattern) && pattern.length() > longestMatch) {
                ttl = entry.getValue();
                longestMatch = pattern.length();
            }
        }

        return ttl != null ? ttl : Duration.ofMinutes(10); // Default fallback
    }

    /**
     * Check if a key matches a pattern (supports wildcards)
     */
    private boolean matchesPattern(String key, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return key.startsWith(prefix);
        }

        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return key.endsWith(suffix);
        }

        // Check for middle wildcards (e.g., "market:ohlcv:*:1m")
        if (pattern.contains("*")) {
            String[] patternParts = pattern.split("\\*");
            if (patternParts.length == 2) {
                return key.startsWith(patternParts[0]) && key.endsWith(patternParts[1]);
            }
        }

        return key.equals(pattern);
    }

    // Specific TTL getters for common use cases
    public Duration getMarketDataTtl() {
        return ttlByPattern.get("market:data:*");
    }

    public Duration getOrderBookTtl() {
        return ttlByPattern.get("market:orderbook:*");
    }

    public Duration getSymbolDataTtl() {
        return ttlByPattern.get("market:symbols:*");
    }

    public Duration getUserSessionTtl() {
        return ttlByPattern.get("user:sessions:*");
    }

    public Duration getSystemParametersTtl() {
        return ttlByPattern.get("trading:hours:*");
    }

    public Duration getAnalyticsTtl() {
        return ttlByPattern.get("analytics:*");
    }

    // Getters and setters
    public boolean isStartupWarmingEnabled() {
        return startupWarmingEnabled;
    }

    public void setStartupWarmingEnabled(boolean startupWarmingEnabled) {
        this.startupWarmingEnabled = startupWarmingEnabled;
    }

    public boolean isScheduledRefreshEnabled() {
        return scheduledRefreshEnabled;
    }

    public void setScheduledRefreshEnabled(boolean scheduledRefreshEnabled) {
        this.scheduledRefreshEnabled = scheduledRefreshEnabled;
    }

    public long getSymbolDelayMs() {
        return symbolDelayMs;
    }

    public void setSymbolDelayMs(long symbolDelayMs) {
        this.symbolDelayMs = symbolDelayMs;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public long getMinTtlSecondsForSkip() {
        return minTtlSecondsForSkip;
    }

    public void setMinTtlSecondsForSkip(long minTtlSecondsForSkip) {
        this.minTtlSecondsForSkip = minTtlSecondsForSkip;
    }

    public Map<String, Duration> getTtlByPattern() {
        return ttlByPattern;
    }

    public void setTtlByPattern(Map<String, Duration> ttlByPattern) {
        this.ttlByPattern = ttlByPattern;
    }

    public int getWarmingThreadPoolSize() {
        return warmingThreadPoolSize;
    }

    public void setWarmingThreadPoolSize(int warmingThreadPoolSize) {
        this.warmingThreadPoolSize = warmingThreadPoolSize;
    }

    public long getWarmingTimeoutMs() {
        return warmingTimeoutMs;
    }

    public void setWarmingTimeoutMs(long warmingTimeoutMs) {
        this.warmingTimeoutMs = warmingTimeoutMs;
    }
}