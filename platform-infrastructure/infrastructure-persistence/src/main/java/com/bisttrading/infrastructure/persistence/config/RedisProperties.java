package com.bisttrading.infrastructure.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Validated
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    /**
     * Redis server host
     */
    @NotBlank
    private String host = "localhost";

    /**
     * Redis server port
     */
    @Min(1)
    @Max(65535)
    private int port = 6379;

    /**
     * Redis password
     */
    private String password;

    /**
     * Database index used by the connection factory
     */
    @Min(0)
    private int database = 0;

    /**
     * Connection timeout in milliseconds
     */
    @Min(1000)
    private long timeout = 5000;

    /**
     * Connection pool settings
     */
    private Pool pool = new Pool();

    /**
     * Cache configuration
     */
    private Cache cache = new Cache();

    @Data
    public static class Pool {
        /**
         * Maximum number of connections that can be allocated by the pool at a given time
         */
        private int maxActive = 20;

        /**
         * Maximum number of idle connections in the pool
         */
        private int maxIdle = 10;

        /**
         * Minimum number of idle connections maintained in the pool
         */
        private int minIdle = 2;

        /**
         * Maximum amount of time (in milliseconds) a connection allocation should block
         */
        private long maxWait = 30000;

        /**
         * Whether to enable test on borrow
         */
        private boolean testOnBorrow = true;

        /**
         * Whether to enable test on return
         */
        private boolean testOnReturn = false;

        /**
         * Whether to enable test while idle
         */
        private boolean testWhileIdle = true;

        /**
         * Time between eviction runs (milliseconds)
         */
        private long timeBetweenEvictionRuns = 60000;

        /**
         * Minimum evictable idle time (milliseconds)
         */
        private long minEvictableIdleTime = 300000;
    }

    @Data
    public static class Cache {
        /**
         * Market data cache TTL in seconds
         */
        private long marketDataTtl = 60;

        /**
         * Session cache TTL in seconds
         */
        private long sessionTtl = 3600;

        /**
         * Portfolio cache TTL in seconds
         */
        private long portfolioTtl = 300;

        /**
         * Order cache TTL in seconds
         */
        private long orderTtl = 1800;

        /**
         * Rate limit cache TTL in seconds
         */
        private long rateLimitTtl = 3600;

        /**
         * Symbol cache TTL in seconds (longer since symbols change rarely)
         */
        private long symbolTtl = 86400;

        /**
         * Default cache TTL in seconds
         */
        private long defaultTtl = 1800;

        /**
         * Enable cache compression
         */
        private boolean compressionEnabled = true;

        /**
         * Cache key prefix
         */
        private String keyPrefix = "bist:";

        /**
         * Cache namespace separator
         */
        private String namespaceSeparator = ":";
    }

    /**
     * Get full cache key with prefix and namespace
     */
    public String getCacheKey(String namespace, String key) {
        return cache.keyPrefix + namespace + cache.namespaceSeparator + key;
    }

    /**
     * Get market data cache key
     */
    public String getMarketDataKey(String symbol) {
        return getCacheKey("market", symbol);
    }

    /**
     * Get session cache key
     */
    public String getSessionKey(String sessionId) {
        return getCacheKey("session", sessionId);
    }

    /**
     * Get portfolio cache key
     */
    public String getPortfolioKey(String accountId, String subAccount) {
        return getCacheKey("portfolio", accountId + ":" + subAccount);
    }

    /**
     * Get order cache key
     */
    public String getOrderKey(String accountId) {
        return getCacheKey("orders:active", accountId);
    }

    /**
     * Get rate limit cache key
     */
    public String getRateLimitKey(String userId, String endpoint, String window) {
        return getCacheKey("rate", userId + ":" + endpoint + ":" + window);
    }

    /**
     * Get symbol cache key
     */
    public String getSymbolKey(String symbolCode) {
        return getCacheKey("symbol", symbolCode);
    }

    /**
     * Get price update channel name
     */
    public String getPriceUpdateChannel() {
        return cache.keyPrefix + "channel:price_updates";
    }

    /**
     * Get order update channel name
     */
    public String getOrderUpdateChannel() {
        return cache.keyPrefix + "channel:order_updates";
    }

    /**
     * Get portfolio update channel name
     */
    public String getPortfolioUpdateChannel() {
        return cache.keyPrefix + "channel:portfolio_updates";
    }

    /**
     * Get system alerts channel name
     */
    public String getSystemAlertsChannel() {
        return cache.keyPrefix + "channel:system_alerts";
    }
}