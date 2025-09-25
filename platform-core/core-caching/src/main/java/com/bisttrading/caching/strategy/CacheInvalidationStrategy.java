package com.bisttrading.caching.strategy;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Cache invalidation strategy interface for BIST Trading Platform
 * Provides flexible cache invalidation mechanisms for different data types
 */
public interface CacheInvalidationStrategy {

    /**
     * Invalidate cache entries by exact key
     *
     * @param key the exact cache key to invalidate
     * @return future indicating completion
     */
    CompletableFuture<Void> invalidateByKey(String key);

    /**
     * Invalidate cache entries by pattern
     * Supports wildcards for batch invalidation
     *
     * @param pattern the pattern to match (e.g., "market:data:*", "user:*:profile")
     * @return future indicating completion
     */
    CompletableFuture<Void> invalidateByPattern(String pattern);

    /**
     * Invalidate cache entries by tag
     * Groups related cache entries for coordinated invalidation
     *
     * @param tag the tag to invalidate
     * @return future indicating completion
     */
    CompletableFuture<Void> invalidateByTag(String tag);

    /**
     * Invalidate multiple keys in batch
     * Optimized for bulk operations
     *
     * @param keys the set of keys to invalidate
     * @return future indicating completion
     */
    CompletableFuture<Void> invalidateBatch(Set<String> keys);

    /**
     * Schedule invalidation after delay
     * Useful for temporary data that should expire
     *
     * @param key the cache key
     * @param delay the delay before invalidation
     * @return future indicating completion
     */
    CompletableFuture<Void> scheduleInvalidation(String key, Duration delay);

    /**
     * Warm up cache with fresh data
     * Proactively load data after invalidation
     *
     * @param key the cache key to warm up
     * @return future indicating completion
     */
    CompletableFuture<Void> warmUpCache(String key);

    /**
     * Get cache statistics for monitoring
     *
     * @return cache invalidation statistics
     */
    CacheInvalidationStats getStats();

    /**
     * Clear all invalidation statistics
     */
    void clearStats();

    /**
     * Check if key exists in cache
     *
     * @param key the cache key to check
     * @return true if key exists
     */
    CompletableFuture<Boolean> exists(String key);

    /**
     * Get remaining TTL for cache key
     *
     * @param key the cache key
     * @return remaining TTL in seconds, -1 if key doesn't exist, -2 if no expiry
     */
    CompletableFuture<Long> getTtl(String key);
}