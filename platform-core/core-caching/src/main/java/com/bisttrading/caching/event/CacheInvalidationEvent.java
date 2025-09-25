package com.bisttrading.caching.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Set;

/**
 * Cache invalidation event for event-driven cache management
 * Enables coordinated cache invalidation across the trading platform
 */
public class CacheInvalidationEvent extends ApplicationEvent {

    private final InvalidationType type;
    private final String key;
    private final String pattern;
    private final String tag;
    private final Set<String> keys;
    private final String reason;
    private final Instant timestamp;
    private final String source;

    // Private constructors to enforce factory methods
    private CacheInvalidationEvent(Object source, InvalidationType type, String key,
                                   String pattern, String tag, Set<String> keys,
                                   String reason, String sourceName) {
        super(source);
        this.type = type;
        this.key = key;
        this.pattern = pattern;
        this.tag = tag;
        this.keys = keys;
        this.reason = reason;
        this.source = sourceName;
        this.timestamp = Instant.now();
    }

    // Factory methods for different invalidation types
    public static CacheInvalidationEvent forKey(Object source, String key, String reason) {
        return new CacheInvalidationEvent(source, InvalidationType.KEY, key,
                null, null, null, reason, source.getClass().getSimpleName());
    }

    public static CacheInvalidationEvent forPattern(Object source, String pattern, String reason) {
        return new CacheInvalidationEvent(source, InvalidationType.PATTERN, null,
                pattern, null, null, reason, source.getClass().getSimpleName());
    }

    public static CacheInvalidationEvent forTag(Object source, String tag, String reason) {
        return new CacheInvalidationEvent(source, InvalidationType.TAG, null,
                null, tag, null, reason, source.getClass().getSimpleName());
    }

    public static CacheInvalidationEvent forBatch(Object source, Set<String> keys, String reason) {
        return new CacheInvalidationEvent(source, InvalidationType.BATCH, null,
                null, null, keys, reason, source.getClass().getSimpleName());
    }

    // Market data specific factory methods
    public static CacheInvalidationEvent forMarketDataUpdate(Object source, String symbol) {
        String pattern = "market:data:" + symbol + ":*";
        return forPattern(source, pattern, "Market data updated for " + symbol);
    }

    public static CacheInvalidationEvent forOrderBookUpdate(Object source, String symbol) {
        String pattern = "market:orderbook:" + symbol + ":*";
        return forPattern(source, pattern, "Order book updated for " + symbol);
    }

    public static CacheInvalidationEvent forUserProfileUpdate(Object source, String userId) {
        String pattern = "user:" + userId + ":*";
        return forPattern(source, pattern, "User profile updated for " + userId);
    }

    public static CacheInvalidationEvent forTradingSessionEnd(Object source) {
        return forTag(source, "trading-session", "Trading session ended");
    }

    public static CacheInvalidationEvent forSystemMaintenance(Object source) {
        return forPattern(source, "*", "System maintenance mode");
    }

    // Getters
    public InvalidationType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTag() {
        return tag;
    }

    public Set<String> getKeys() {
        return keys;
    }

    public String getReason() {
        return reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSourceName() {
        return source;
    }

    // Convenience methods
    public boolean isKeyInvalidation() {
        return type == InvalidationType.KEY;
    }

    public boolean isPatternInvalidation() {
        return type == InvalidationType.PATTERN;
    }

    public boolean isTagInvalidation() {
        return type == InvalidationType.TAG;
    }

    public boolean isBatchInvalidation() {
        return type == InvalidationType.BATCH;
    }

    public String getInvalidationTarget() {
        return switch (type) {
            case KEY -> key;
            case PATTERN -> pattern;
            case TAG -> tag;
            case BATCH -> keys != null ? String.format("%d keys", keys.size()) : "empty batch";
        };
    }

    @Override
    public String toString() {
        return String.format(
            "CacheInvalidationEvent{type=%s, target='%s', reason='%s', source='%s', timestamp=%s}",
            type, getInvalidationTarget(), reason, source, timestamp
        );
    }

    /**
     * Cache invalidation types
     */
    public enum InvalidationType {
        /**
         * Invalidate a single cache key
         */
        KEY,

        /**
         * Invalidate all keys matching a pattern
         */
        PATTERN,

        /**
         * Invalidate all keys associated with a tag
         */
        TAG,

        /**
         * Invalidate multiple specific keys in batch
         */
        BATCH
    }
}