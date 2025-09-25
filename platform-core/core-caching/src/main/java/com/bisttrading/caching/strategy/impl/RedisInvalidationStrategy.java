package com.bisttrading.caching.strategy.impl;

import com.bisttrading.caching.strategy.CacheInvalidationStrategy;
import com.bisttrading.caching.strategy.CacheInvalidationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based cache invalidation strategy implementation
 * Optimized for high-performance trading applications
 */
@Component
public class RedisInvalidationStrategy implements CacheInvalidationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RedisInvalidationStrategy.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor asyncExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final CacheInvalidationStats stats;

    // Redis Lua scripts for atomic operations
    private final DefaultRedisScript<Long> patternDeleteScript;
    private final DefaultRedisScript<Long> tagDeleteScript;

    public RedisInvalidationStrategy(
            RedisTemplate<String, Object> redisTemplate,
            Executor asyncExecutor,
            ScheduledExecutorService scheduledExecutor) {
        this.redisTemplate = redisTemplate;
        this.asyncExecutor = asyncExecutor;
        this.scheduledExecutor = scheduledExecutor;
        this.stats = new CacheInvalidationStats();

        // Initialize Lua scripts
        this.patternDeleteScript = createPatternDeleteScript();
        this.tagDeleteScript = createTagDeleteScript();
    }

    @Override
    public CompletableFuture<Void> invalidateByKey(String key) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Boolean deleted = redisTemplate.delete(key);
                if (Boolean.TRUE.equals(deleted)) {
                    logger.debug("Cache key invalidated: {}", key);
                } else {
                    logger.debug("Cache key not found for invalidation: {}", key);
                }
                stats.incrementTotalInvalidations();
            } catch (Exception e) {
                logger.error("Failed to invalidate cache key: {}", key, e);
                stats.incrementFailedInvalidations();
                throw new RuntimeException("Cache invalidation failed", e);
            } finally {
                stats.addProcessingTime(System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> invalidateByPattern(String pattern) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Long deletedCount = redisTemplate.execute(patternDeleteScript, List.of(), pattern);
                logger.debug("Pattern invalidation completed: {} (deleted {} keys)", pattern, deletedCount);
                stats.incrementPatternInvalidations();
            } catch (Exception e) {
                logger.error("Failed to invalidate by pattern: {}", pattern, e);
                stats.incrementFailedInvalidations();
                throw new RuntimeException("Pattern invalidation failed", e);
            } finally {
                stats.addProcessingTime(System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> invalidateByTag(String tag) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Get all keys associated with the tag
                String tagKey = "tag:" + tag;
                Set<Object> taggedKeys = redisTemplate.opsForSet().members(tagKey);

                if (taggedKeys != null && !taggedKeys.isEmpty()) {
                    // Convert to String array for batch deletion
                    String[] keysArray = taggedKeys.stream()
                            .map(Object::toString)
                            .toArray(String[]::new);

                    // Delete tagged keys and the tag itself
                    Long deletedCount = redisTemplate.delete(List.of(keysArray));
                    redisTemplate.delete(tagKey);

                    logger.debug("Tag invalidation completed: {} (deleted {} keys)", tag, deletedCount);
                } else {
                    logger.debug("No keys found for tag: {}", tag);
                }

                stats.incrementTagInvalidations();
            } catch (Exception e) {
                logger.error("Failed to invalidate by tag: {}", tag, e);
                stats.incrementFailedInvalidations();
                throw new RuntimeException("Tag invalidation failed", e);
            } finally {
                stats.addProcessingTime(System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> invalidateBatch(Set<String> keys) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                if (keys.isEmpty()) {
                    logger.debug("Empty batch invalidation request");
                    return;
                }

                Long deletedCount = redisTemplate.delete(keys);
                logger.debug("Batch invalidation completed: {} keys requested, {} deleted",
                           keys.size(), deletedCount);
                stats.incrementBatchInvalidations(keys.size());
            } catch (Exception e) {
                logger.error("Failed to invalidate batch of {} keys", keys.size(), e);
                stats.incrementFailedInvalidations();
                throw new RuntimeException("Batch invalidation failed", e);
            } finally {
                stats.addProcessingTime(System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> scheduleInvalidation(String key, Duration delay) {
        return CompletableFuture.runAsync(() -> {
            try {
                scheduledExecutor.schedule(
                    () -> {
                        try {
                            invalidateByKey(key).get();
                            logger.debug("Scheduled invalidation completed for key: {}", key);
                        } catch (Exception e) {
                            logger.error("Failed scheduled invalidation for key: {}", key, e);
                            stats.incrementFailedInvalidations();
                        }
                    },
                    delay.toMillis(),
                    TimeUnit.MILLISECONDS
                );
                stats.incrementScheduledInvalidations();
                logger.debug("Scheduled invalidation for key: {} in {}", key, delay);
            } catch (Exception e) {
                logger.error("Failed to schedule invalidation for key: {}", key, e);
                stats.incrementFailedInvalidations();
                throw new RuntimeException("Schedule invalidation failed", e);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> warmUpCache(String key) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // This is a placeholder - actual implementation would depend on
                // the specific cache warming strategy for each data type
                logger.debug("Cache warm-up initiated for key: {}", key);

                // Example: Check if key exists, if not, trigger loading
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.FALSE.equals(exists)) {
                    // Here you would implement the actual data loading logic
                    // This could involve calling repository methods, external APIs, etc.
                    logger.debug("Key {} not in cache, warm-up needed", key);
                }

                stats.incrementCacheWarmUps();
            } catch (Exception e) {
                logger.error("Failed to warm up cache for key: {}", key, e);
                throw new RuntimeException("Cache warm-up failed", e);
            } finally {
                stats.addProcessingTime(System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CacheInvalidationStats getStats() {
        return stats;
    }

    @Override
    public void clearStats() {
        stats.reset();
    }

    @Override
    public CompletableFuture<Boolean> exists(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return redisTemplate.hasKey(key);
            } catch (Exception e) {
                logger.error("Failed to check key existence: {}", key, e);
                return false;
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Long> getTtl(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return redisTemplate.getExpire(key);
            } catch (Exception e) {
                logger.error("Failed to get TTL for key: {}", key, e);
                return -1L;
            }
        }, asyncExecutor);
    }

    /**
     * Create Lua script for pattern-based deletion
     */
    private DefaultRedisScript<Long> createPatternDeleteScript() {
        String luaScript = """
            local pattern = ARGV[1]
            local keys = redis.call('KEYS', pattern)
            local deletedCount = 0

            for i = 1, #keys do
                if redis.call('DEL', keys[i]) == 1 then
                    deletedCount = deletedCount + 1
                end
            end

            return deletedCount
            """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * Create Lua script for tag-based deletion
     */
    private DefaultRedisScript<Long> createTagDeleteScript() {
        String luaScript = """
            local tagKey = 'tag:' .. ARGV[1]
            local keys = redis.call('SMEMBERS', tagKey)
            local deletedCount = 0

            -- Delete all tagged keys
            for i = 1, #keys do
                if redis.call('DEL', keys[i]) == 1 then
                    deletedCount = deletedCount + 1
                end
            end

            -- Delete the tag set itself
            redis.call('DEL', tagKey)

            return deletedCount
            """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        return script;
    }
}