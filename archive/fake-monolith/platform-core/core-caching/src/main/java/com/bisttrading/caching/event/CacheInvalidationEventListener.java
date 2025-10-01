package com.bisttrading.caching.event;

import com.bisttrading.caching.strategy.CacheInvalidationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Event listener for cache invalidation events
 * Handles automatic cache invalidation based on domain events
 */
@Component
public class CacheInvalidationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationEventListener.class);

    private final CacheInvalidationStrategy invalidationStrategy;

    public CacheInvalidationEventListener(CacheInvalidationStrategy invalidationStrategy) {
        this.invalidationStrategy = invalidationStrategy;
    }

    /**
     * Handle cache invalidation events asynchronously
     * Prevents blocking the main application flow
     *
     * @param event the cache invalidation event
     */
    @Async
    @EventListener
    public void handleCacheInvalidationEvent(CacheInvalidationEvent event) {
        logger.debug("Processing cache invalidation event: {}", event);

        CompletableFuture<Void> invalidationFuture = switch (event.getType()) {
            case KEY -> invalidationStrategy.invalidateByKey(event.getKey());
            case PATTERN -> invalidationStrategy.invalidateByPattern(event.getPattern());
            case TAG -> invalidationStrategy.invalidateByTag(event.getTag());
            case BATCH -> invalidationStrategy.invalidateBatch(event.getKeys());
        };

        // Handle completion and errors
        invalidationFuture
            .thenRun(() -> {
                logger.debug("Cache invalidation completed successfully: {}", event.getInvalidationTarget());
            })
            .exceptionally(throwable -> {
                logger.error("Cache invalidation failed for {}: {}",
                           event.getInvalidationTarget(), throwable.getMessage(), throwable);
                return null;
            });
    }

    /**
     * Handle market data update events
     * Specialized handling for high-frequency market data invalidation
     *
     * @param event market data cache invalidation event
     */
    @Async
    @EventListener(condition = "#event.reason.contains('Market data updated')")
    public void handleMarketDataInvalidation(CacheInvalidationEvent event) {
        logger.debug("Processing market data invalidation: {}", event);

        // For market data, we might want to implement immediate warm-up
        // after invalidation to ensure fresh data is immediately available
        invalidationStrategy.invalidateByPattern(event.getPattern())
            .thenCompose(result -> {
                // Extract symbol from pattern for warm-up
                String pattern = event.getPattern();
                if (pattern.startsWith("market:data:") && pattern.endsWith(":*")) {
                    String symbol = pattern.substring(12, pattern.length() - 2);
                    String marketDataKey = "market:data:" + symbol + ":latest";
                    return invalidationStrategy.warmUpCache(marketDataKey);
                }
                return CompletableFuture.completedFuture(null);
            })
            .exceptionally(throwable -> {
                logger.error("Market data cache invalidation failed: {}", throwable.getMessage(), throwable);
                return null;
            });
    }

    /**
     * Handle order book update events
     * Specialized handling for order book cache invalidation
     *
     * @param event order book cache invalidation event
     */
    @Async
    @EventListener(condition = "#event.reason.contains('Order book updated')")
    public void handleOrderBookInvalidation(CacheInvalidationEvent event) {
        logger.debug("Processing order book invalidation: {}", event);

        // Order book updates are critical for trading decisions
        // Implement priority invalidation with immediate warm-up
        invalidationStrategy.invalidateByPattern(event.getPattern())
            .thenRun(() -> {
                logger.debug("Order book cache invalidated: {}", event.getPattern());
            })
            .exceptionally(throwable -> {
                logger.error("Order book cache invalidation failed: {}", throwable.getMessage(), throwable);
                return null;
            });
    }

    /**
     * Handle user profile update events
     * Specialized handling for user-related cache invalidation
     *
     * @param event user profile cache invalidation event
     */
    @Async
    @EventListener(condition = "#event.reason.contains('User profile updated')")
    public void handleUserProfileInvalidation(CacheInvalidationEvent event) {
        logger.debug("Processing user profile invalidation: {}", event);

        // User profile updates should invalidate related caches
        invalidationStrategy.invalidateByPattern(event.getPattern())
            .thenRun(() -> {
                logger.debug("User profile cache invalidated: {}", event.getPattern());
            })
            .exceptionally(throwable -> {
                logger.error("User profile cache invalidation failed: {}", throwable.getMessage(), throwable);
                return null;
            });
    }

    /**
     * Handle system maintenance events
     * Comprehensive cache clearing for maintenance mode
     *
     * @param event system maintenance cache invalidation event
     */
    @Async
    @EventListener(condition = "#event.reason.contains('System maintenance')")
    public void handleSystemMaintenanceInvalidation(CacheInvalidationEvent event) {
        logger.warn("Processing system maintenance cache invalidation: {}", event);

        // For system maintenance, clear all caches
        invalidationStrategy.invalidateByPattern("*")
            .thenRun(() -> {
                logger.warn("All caches cleared for system maintenance");
                // Clear invalidation statistics for fresh start
                invalidationStrategy.clearStats();
            })
            .exceptionally(throwable -> {
                logger.error("System maintenance cache invalidation failed: {}", throwable.getMessage(), throwable);
                return null;
            });
    }

    /**
     * Handle trading session end events
     * Clean up session-related cached data
     *
     * @param event trading session end cache invalidation event
     */
    @Async
    @EventListener(condition = "#event.reason.contains('Trading session ended')")
    public void handleTradingSessionEndInvalidation(CacheInvalidationEvent event) {
        logger.info("Processing trading session end invalidation: {}", event);

        // Invalidate session-specific data but keep market data
        invalidationStrategy.invalidateByTag(event.getTag())
            .thenRun(() -> {
                logger.info("Trading session cache cleared");
            })
            .exceptionally(throwable -> {
                logger.error("Trading session cache invalidation failed: {}", throwable.getMessage(), throwable);
                return null;
            });
    }
}