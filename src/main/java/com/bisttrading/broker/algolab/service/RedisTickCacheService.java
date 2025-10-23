package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.dto.websocket.TickData;
import com.bisttrading.broker.algolab.dto.websocket.OrderBookData;
import com.bisttrading.broker.algolab.dto.websocket.TradeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    // Metrik key'leri
    private static final String METRICS_TOTAL_KEY = "algolab:metrics:tick:total";
    private static final String METRICS_SYMBOL_COUNTS_KEY = "algolab:metrics:symbol:counts";
    private static final String METRICS_LAST_TIME_KEY = "algolab:metrics:tick:last-time";
    private static final String METRICS_FIRST_TIME_KEY = "algolab:metrics:tick:first-time";
    private static final String METRICS_LAST_MINUTE_KEY = "algolab:metrics:tick:last-minute";
    private static final String METRICS_SYMBOLS_KEY = "algolab:metrics:symbols:active";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTickCacheService(@Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        log.info("Redis Tick Cache Service initialized");
    }

    /**
     * Cache tick data with timestamp-based sorting AND update metrics.
     * Uses Redis pipelining to reduce connection overhead.
     */
    public void cacheTick(String symbol, TickData data) {
        try {
            long timestamp = System.currentTimeMillis();
            String key = TICK_KEY_PREFIX + symbol;

            // Execute all operations in a single pipeline to reuse connection
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] keyBytes = key.getBytes();
                byte[] symbolBytes = symbol.getBytes();
                byte[] timestampBytes = String.valueOf(timestamp).getBytes();
                byte[] symbolsSetKeyBytes = SYMBOLS_SET_KEY.getBytes();

                // 1. Store tick in sorted set
                connection.zSetCommands().zAdd(keyBytes, timestamp,
                    serializeTickData(data));

                // 2. Set TTL for tick key
                connection.expire(keyBytes, DEFAULT_TTL_MINUTES * 60);

                // 3. Add symbol to active symbols set
                connection.setCommands().sAdd(symbolsSetKeyBytes, symbolBytes);

                // 4. Set TTL for symbols set
                connection.expire(symbolsSetKeyBytes, DEFAULT_TTL_MINUTES * 60);

                // 5. Trim cache (remove old items if needed)
                Long size = connection.zSetCommands().zCard(keyBytes);
                if (size != null && size > MAX_ITEMS_PER_SYMBOL) {
                    connection.zSetCommands().zRemRange(keyBytes, 0, size - MAX_ITEMS_PER_SYMBOL - 1);
                }

                // 6-12. Update metrics (all in same pipeline)
                byte[] totalKey = METRICS_TOTAL_KEY.getBytes();
                byte[] symbolCountsKey = METRICS_SYMBOL_COUNTS_KEY.getBytes();
                byte[] lastTimeKey = METRICS_LAST_TIME_KEY.getBytes();
                byte[] firstTimeKey = METRICS_FIRST_TIME_KEY.getBytes();
                byte[] lastMinuteKey = METRICS_LAST_MINUTE_KEY.getBytes();
                byte[] symbolsKey = METRICS_SYMBOLS_KEY.getBytes();

                connection.incr(totalKey);
                connection.hashCommands().hIncrBy(symbolCountsKey, symbolBytes, 1);
                connection.stringCommands().set(lastTimeKey, timestampBytes);
                connection.stringCommands().setNX(firstTimeKey, timestampBytes);
                connection.zSetCommands().zAdd(lastMinuteKey, timestamp, timestampBytes);

                // Cleanup old metrics (1 minute sliding window)
                long oneMinuteAgo = timestamp - 60000;
                connection.zSetCommands().zRemRangeByScore(lastMinuteKey, 0, oneMinuteAgo);

                connection.setCommands().sAdd(symbolsKey, symbolBytes);
                connection.expire(lastMinuteKey, 120);  // 2 minutes TTL
                connection.expire(symbolsKey, DEFAULT_TTL_MINUTES * 60);

                return null;
            });

            log.trace("Cached tick for {}: Price={}", symbol, data.getLastPrice());

        } catch (Exception e) {
            log.error("Failed to cache tick for {}", symbol, e);
        }
    }

    /**
     * Serialize TickData to byte array for Redis storage.
     */
    private byte[] serializeTickData(TickData data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Failed to serialize tick data", e);
            return new byte[0];
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

            // Trim old data inline
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null && size > MAX_ITEMS_PER_SYMBOL) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_ITEMS_PER_SYMBOL - 1);
            }

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

            // Trim old data inline
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null && size > MAX_ITEMS_PER_SYMBOL) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_ITEMS_PER_SYMBOL - 1);
            }

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
    /**
     * Removed trimCache() - now integrated into cacheTick() pipeline for better performance.
     */

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

    // ==================== METRICS METHODS ====================

    /**
     * Update all metrics atomically using Redis pipeline.
     */
    /**
     * Removed updateMetrics() - now integrated into cacheTick() pipeline for better performance.
     */

    /**
     * Get real-time metrics from Redis.
     */
    public Map<String, Object> getRealTimeMetrics() {
        try {
            // Temel metrikler
            String totalTicksStr = (String) redisTemplate.opsForValue().get(METRICS_TOTAL_KEY);
            Long totalTicks = totalTicksStr != null ? Long.parseLong(totalTicksStr) : 0L;

            String lastTimeStr = (String) redisTemplate.opsForValue().get(METRICS_LAST_TIME_KEY);
            String firstTimeStr = (String) redisTemplate.opsForValue().get(METRICS_FIRST_TIME_KEY);

            // Son 1 dakika tick sayısı
            Long lastMinuteTicks = redisTemplate.opsForZSet()
                .count(METRICS_LAST_MINUTE_KEY,
                       System.currentTimeMillis() - 60000,
                       System.currentTimeMillis());

            // Aktif semboller
            Set<Object> activeSymbols = redisTemplate.opsForSet().members(METRICS_SYMBOLS_KEY);

            // Sembol sayaçları
            Map<Object, Object> symbolCounts = redisTemplate.opsForHash()
                .entries(METRICS_SYMBOL_COUNTS_KEY);

            // Hesaplamalar
            long lastMinuteValue = lastMinuteTicks != null ? lastMinuteTicks : 0;

            // Ortalama tick/saniye hesapla
            double ticksPerSecond = lastMinuteValue / 60.0;
            double overallTicksPerSecond = 0.0;

            if (firstTimeStr != null && lastTimeStr != null) {
                long firstTime = Long.parseLong(firstTimeStr);
                long lastTime = Long.parseLong(lastTimeStr);
                long elapsedSeconds = (lastTime - firstTime) / 1000;

                if (elapsedSeconds > 0) {
                    overallTicksPerSecond = totalTicks / (double) elapsedSeconds;
                }
            }

            // Top 10 en aktif semboller
            List<Map<String, Object>> topSymbols = symbolCounts.entrySet().stream()
                .map(e -> {
                    String sym = e.getKey().toString();
                    Long count = Long.parseLong(e.getValue().toString());
                    return Map.<String, Object>of("symbol", sym, "count", count);
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(10)
                .toList();

            return Map.of(
                "totalTicks", totalTicks,
                "lastMinuteTicks", lastMinuteValue,
                "activeSymbolCount", activeSymbols != null ? activeSymbols.size() : 0,
                "ticksPerSecond", String.format("%.2f", ticksPerSecond),
                "overallTicksPerSecond", String.format("%.2f", overallTicksPerSecond),
                "activeSymbols", activeSymbols != null ? activeSymbols : Set.of(),
                "topSymbols", topSymbols,
                "lastTickTime", lastTimeStr != null ?
                    Instant.ofEpochMilli(Long.parseLong(lastTimeStr)).toString() : null,
                "sessionStartTime", firstTimeStr != null ?
                    Instant.ofEpochMilli(Long.parseLong(firstTimeStr)).toString() : null
            );

        } catch (Exception e) {
            log.error("Failed to get real-time metrics", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get symbol-specific metrics from Redis.
     */
    public Map<String, Object> getSymbolMetrics(String symbol) {
        try {
            // Sembol tick sayısı
            Object countObj = redisTemplate.opsForHash()
                .get(METRICS_SYMBOL_COUNTS_KEY, symbol);
            Long count = countObj != null ? Long.parseLong(countObj.toString()) : 0L;

            // Son tick zamanı
            Instant lastTickTime = getLastTickTime(symbol);

            // Son tick verisi
            List<TickData> lastTick = getRecentTicks(symbol, 1);

            return Map.of(
                "symbol", symbol,
                "tickCount", count,
                "lastTickTime", lastTickTime != null ? lastTickTime.toString() : "N/A",
                "lastTickData", !lastTick.isEmpty() ? lastTick.get(0) : "N/A"
            );

        } catch (Exception e) {
            log.error("Failed to get symbol metrics for {}", symbol, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Reset all metrics (for new session).
     */
    public void resetMetrics() {
        try {
            redisTemplate.delete(METRICS_TOTAL_KEY);
            redisTemplate.delete(METRICS_SYMBOL_COUNTS_KEY);
            redisTemplate.delete(METRICS_LAST_TIME_KEY);
            redisTemplate.delete(METRICS_FIRST_TIME_KEY);
            redisTemplate.delete(METRICS_LAST_MINUTE_KEY);
            redisTemplate.delete(METRICS_SYMBOLS_KEY);

            log.info("Metrics reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset metrics", e);
        }
    }
}
