package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.dto.websocket.TickData;
import com.bisttrading.broker.algolab.dto.websocket.OrderBookData;
import com.bisttrading.broker.algolab.dto.websocket.TradeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-based cache for WebSocket messages.
 * Provides persistent storage across restarts with TTL support.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "algolab.cache.enabled", havingValue = "true", matchIfMissing = false)
public class RedisTickCacheService {

    private static final String TICK_KEY_PREFIX = "algolab:tick:";
    private static final String ORDERBOOK_KEY_PREFIX = "algolab:orderbook:";
    private static final String TRADE_KEY_PREFIX = "algolab:trade:";
    private static final String SYMBOLS_SET_KEY = "algolab:symbols:active";

    private static final long DEFAULT_TTL_MINUTES = 5;
    private static final int MAX_ITEMS_PER_SYMBOL = 100;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTickCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        log.info("Redis Tick Cache Service initialized");
    }

    /**
     * Cache tick data with timestamp-based sorting.
     */
    public void cacheTick(String symbol, TickData data) {
        try {
            String key = TICK_KEY_PREFIX + symbol;
            double score = System.currentTimeMillis();

            // Store in sorted set (timestamp as score)
            redisTemplate.opsForZSet().add(key, data, score);

            // Set TTL
            redisTemplate.expire(key, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);

            // Track active symbol
            redisTemplate.opsForSet().add(SYMBOLS_SET_KEY, symbol);
            redisTemplate.expire(SYMBOLS_SET_KEY, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);

            // Trim old data
            trimCache(key, MAX_ITEMS_PER_SYMBOL);

            log.trace("Cached tick for {}: Price={}", symbol, data.getLastPrice());

        } catch (Exception e) {
            log.error("Failed to cache tick for {}", symbol, e);
        }
    }

    /**
     * Get recent tick messages for a symbol.
     */
    public List<TickData> getRecentTicks(String symbol, int limit) {
        try {
            String key = TICK_KEY_PREFIX + symbol;

            // Get most recent items (highest scores)
            Set<Object> results = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);

            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }

            return results.stream()
                .map(obj -> objectMapper.convertValue(obj, TickData.class))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get ticks for {}", symbol, e);
            return Collections.emptyList();
        }
    }

    /**
     * Cache order book data.
     */
    public void cacheOrderBook(String symbol, OrderBookData data) {
        try {
            String key = ORDERBOOK_KEY_PREFIX + symbol;
            double score = System.currentTimeMillis();

            redisTemplate.opsForZSet().add(key, data, score);
            redisTemplate.expire(key, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);

            trimCache(key, MAX_ITEMS_PER_SYMBOL);

            log.trace("Cached order book for {}", symbol);

        } catch (Exception e) {
            log.error("Failed to cache order book for {}", symbol, e);
        }
    }

    /**
     * Get recent order book messages.
     */
    public List<OrderBookData> getRecentOrderBooks(String symbol, int limit) {
        try {
            String key = ORDERBOOK_KEY_PREFIX + symbol;

            Set<Object> results = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);

            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }

            return results.stream()
                .map(obj -> objectMapper.convertValue(obj, OrderBookData.class))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get order books for {}", symbol, e);
            return Collections.emptyList();
        }
    }

    /**
     * Cache trade data.
     */
    public void cacheTrade(String symbol, TradeData data) {
        try {
            String key = TRADE_KEY_PREFIX + symbol;
            double score = System.currentTimeMillis();

            redisTemplate.opsForZSet().add(key, data, score);
            redisTemplate.expire(key, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);

            trimCache(key, MAX_ITEMS_PER_SYMBOL);

            log.trace("Cached trade for {}", symbol);

        } catch (Exception e) {
            log.error("Failed to cache trade for {}", symbol, e);
        }
    }

    /**
     * Get recent trade messages.
     */
    public List<TradeData> getRecentTrades(String symbol, int limit) {
        try {
            String key = TRADE_KEY_PREFIX + symbol;

            Set<Object> results = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);

            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }

            return results.stream()
                .map(obj -> objectMapper.convertValue(obj, TradeData.class))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get trades for {}", symbol, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all active symbols with cached data.
     */
    public Set<String> getActiveSymbols() {
        try {
            Set<Object> symbols = redisTemplate.opsForSet().members(SYMBOLS_SET_KEY);
            if (symbols == null) {
                return Collections.emptySet();
            }
            return symbols.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get active symbols", e);
            return Collections.emptySet();
        }
    }

    /**
     * Clear all cached data.
     */
    public void clearAll() {
        try {
            Set<String> keys = redisTemplate.keys("algolab:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared all cached data: {} keys deleted", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
        }
    }

    /**
     * Clear cache for specific symbol.
     */
    public void clearSymbol(String symbol) {
        try {
            redisTemplate.delete(TICK_KEY_PREFIX + symbol);
            redisTemplate.delete(ORDERBOOK_KEY_PREFIX + symbol);
            redisTemplate.delete(TRADE_KEY_PREFIX + symbol);
            redisTemplate.opsForSet().remove(SYMBOLS_SET_KEY, symbol);

            log.info("Cleared cache for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Failed to clear cache for {}", symbol, e);
        }
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getStats() {
        try {
            Set<String> tickKeys = redisTemplate.keys(TICK_KEY_PREFIX + "*");
            Set<String> orderBookKeys = redisTemplate.keys(ORDERBOOK_KEY_PREFIX + "*");
            Set<String> tradeKeys = redisTemplate.keys(TRADE_KEY_PREFIX + "*");

            long totalTickMessages = tickKeys != null ? tickKeys.stream()
                .mapToLong(key -> redisTemplate.opsForZSet().size(key))
                .sum() : 0;

            long totalOrderBookMessages = orderBookKeys != null ? orderBookKeys.stream()
                .mapToLong(key -> redisTemplate.opsForZSet().size(key))
                .sum() : 0;

            long totalTradeMessages = tradeKeys != null ? tradeKeys.stream()
                .mapToLong(key -> redisTemplate.opsForZSet().size(key))
                .sum() : 0;

            return Map.of(
                "cacheType", "REDIS",
                "tickSymbols", tickKeys != null ? tickKeys.size() : 0,
                "orderBookSymbols", orderBookKeys != null ? orderBookKeys.size() : 0,
                "tradeSymbols", tradeKeys != null ? tradeKeys.size() : 0,
                "totalTickMessages", totalTickMessages,
                "totalOrderBookMessages", totalOrderBookMessages,
                "totalTradeMessages", totalTradeMessages,
                "activeSymbols", getActiveSymbols().size()
            );
        } catch (Exception e) {
            log.error("Failed to get cache stats", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get timestamp of last received tick for a symbol.
     */
    public Instant getLastTickTime(String symbol) {
        try {
            String key = TICK_KEY_PREFIX + symbol;
            Set<Object> latest = redisTemplate.opsForZSet().reverseRange(key, 0, 0);

            if (latest != null && !latest.isEmpty()) {
                // Get the score (timestamp) of the most recent item
                Double score = redisTemplate.opsForZSet().score(key, latest.iterator().next());
                return score != null ? Instant.ofEpochMilli(score.longValue()) : null;
            }
        } catch (Exception e) {
            log.error("Failed to get last tick time for {}", symbol, e);
        }
        return null;
    }

    /**
     * Get message count for a symbol.
     */
    public long getMessageCount(String symbol) {
        try {
            String key = TICK_KEY_PREFIX + symbol;
            Long size = redisTemplate.opsForZSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get message count for {}", symbol, e);
            return 0;
        }
    }

    /**
     * Trim cache to max items.
     */
    private void trimCache(String key, int maxItems) {
        try {
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null && size > maxItems) {
                // Remove oldest items
                redisTemplate.opsForZSet().removeRange(key, 0, size - maxItems - 1);
            }
        } catch (Exception e) {
            log.error("Failed to trim cache for key: {}", key, e);
        }
    }

    /**
     * Check Redis connection health.
     */
    public boolean isHealthy() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return true;
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }
}
