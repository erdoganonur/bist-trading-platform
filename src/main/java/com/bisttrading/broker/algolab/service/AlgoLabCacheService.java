package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.config.RedisCacheConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing AlgoLab cache operations.
 *
 * Features:
 * - Cache warmup on startup with popular BIST symbols
 * - Cache statistics tracking (hits, misses, hit rate)
 * - Scheduled cache refresh and statistics logging
 * - Manual cache eviction methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlgoLabCacheService {

    private final CacheManager cacheManager;
    private final AlgoLabMarketDataService marketDataService;

    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    /**
     * Popular BIST30 symbols for cache warmup.
     * These are the most traded stocks on Borsa Istanbul.
     */
    private static final String[] POPULAR_SYMBOLS = {
        "GARAN",  // Garanti BBVA
        "THYAO",  // Türk Hava Yolları
        "AKBNK",  // Akbank
        "EREGL",  // Ereğli Demir Çelik
        "SAHOL",  // Sabancı Holding
        "ASELS",  // Aselsan
        "KOZAL",  // Koza Altın
        "KCHOL",  // Koç Holding
        "SISE",   // Şişe Cam
        "ENKAI"   // Enka İnşaat
    };

    /**
     * Warms up cache on application startup.
     * Preloads popular symbols to prevent cold start latency.
     */
    @PostConstruct
    public void warmupCache() {
        log.info("Starting cache warmup for AlgoLab data...");
        long startTime = System.currentTimeMillis();
        int successCount = 0;

        for (String symbol : POPULAR_SYMBOLS) {
            try {
                // Preload symbol information
                marketDataService.getSymbolInfo(symbol);
                successCount++;

                // Small delay to respect rate limits
                Thread.sleep(100);

            } catch (Exception e) {
                log.warn("Failed to warm up cache for symbol: {}", symbol, e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Cache warmup completed. Warmed {} / {} symbols in {}ms",
            successCount, POPULAR_SYMBOLS.length, duration);
    }

    /**
     * Periodic cache warmup - refreshes popular symbols every 30 minutes.
     * Prevents cache expiration for frequently accessed data.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void periodicCacheWarmup() {
        log.debug("Running periodic cache warmup...");

        try {
            for (String symbol : POPULAR_SYMBOLS) {
                try {
                    // Refresh symbol cache
                    invalidateSymbol(symbol);
                    marketDataService.getSymbolInfo(symbol);

                    // Small delay
                    Thread.sleep(100);

                } catch (Exception e) {
                    log.debug("Failed to refresh cache for symbol: {}", symbol);
                }
            }

            log.debug("Periodic cache warmup completed for {} symbols", POPULAR_SYMBOLS.length);

        } catch (Exception e) {
            log.error("Error during periodic cache warmup", e);
        }
    }

    /**
     * Logs cache statistics every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logCacheStatistics() {
        Map<String, Object> stats = getCacheStatistics();

        log.info("=== AlgoLab Cache Statistics ===");
        log.info("Total Requests: {}", stats.get("totalRequests"));
        log.info("Cache Hits: {}", stats.get("cacheHits"));
        log.info("Cache Misses: {}", stats.get("cacheMisses"));
        log.info("Hit Rate: {}%", stats.get("hitRate"));
        log.info("================================");
    }

    /**
     * Records a cache hit.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
        totalRequests.incrementAndGet();
    }

    /**
     * Records a cache miss.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
        totalRequests.incrementAndGet();
    }

    /**
     * Gets current cache statistics.
     *
     * @return Map with cache metrics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = totalRequests.get();

        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;

        stats.put("cacheHits", hits);
        stats.put("cacheMisses", misses);
        stats.put("totalRequests", total);
        stats.put("hitRate", String.format("%.2f", hitRate));

        // Add cache-specific statistics
        stats.put("cacheNames", Map.of(
            "symbols", getCacheSize(RedisCacheConfig.CacheNames.SYMBOLS),
            "candles", getCacheSize(RedisCacheConfig.CacheNames.CANDLES),
            "positions", getCacheSize(RedisCacheConfig.CacheNames.POSITIONS),
            "orders", getCacheSize(RedisCacheConfig.CacheNames.ORDERS),
            "marketData", getCacheSize(RedisCacheConfig.CacheNames.MARKET_DATA),
            "accountInfo", getCacheSize(RedisCacheConfig.CacheNames.ACCOUNT_INFO)
        ));

        return stats;
    }

    /**
     * Gets approximate cache size (not all cache implementations support this).
     */
    private String getCacheSize(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            return cache != null ? "active" : "not initialized";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Invalidates a specific symbol from the symbols cache.
     *
     * @param symbol Symbol to invalidate
     */
    public void invalidateSymbol(String symbol) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.SYMBOLS);
        if (cache != null) {
            cache.evict(symbol);
            log.debug("Invalidated symbol cache for: {}", symbol);
        }
    }

    /**
     * Invalidates a specific candle from the candles cache.
     *
     * @param symbol Symbol
     * @param period Period (e.g., "1D", "1H")
     */
    public void invalidateCandle(String symbol, String period) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.CANDLES);
        if (cache != null) {
            String key = symbol + "_" + period;
            cache.evict(key);
            log.debug("Invalidated candle cache for: {} - {}", symbol, period);
        }
    }

    /**
     * Invalidates positions cache for a user.
     *
     * @param userId User ID (optional, if null clears all)
     */
    public void invalidatePositions(String userId) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.POSITIONS);
        if (cache != null) {
            if (userId != null) {
                cache.evict(userId);
                log.debug("Invalidated positions cache for user: {}", userId);
            } else {
                cache.clear();
                log.debug("Cleared all positions cache");
            }
        }
    }

    /**
     * Invalidates orders cache for a user.
     *
     * @param userId User ID (optional, if null clears all)
     */
    public void invalidateOrders(String userId) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.ORDERS);
        if (cache != null) {
            if (userId != null) {
                cache.evict(userId);
                log.debug("Invalidated orders cache for user: {}", userId);
            } else {
                cache.clear();
                log.debug("Cleared all orders cache");
            }
        }
    }

    /**
     * Invalidates market data cache for a symbol.
     *
     * @param symbol Symbol to invalidate
     */
    public void invalidateMarketData(String symbol) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.MARKET_DATA);
        if (cache != null) {
            cache.evict(symbol);
            log.debug("Invalidated market data cache for: {}", symbol);
        }
    }

    /**
     * Invalidates account info cache for a user.
     *
     * @param userId User ID
     */
    public void invalidateAccountInfo(String userId) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.ACCOUNT_INFO);
        if (cache != null) {
            cache.evict(userId);
            log.debug("Invalidated account info cache for user: {}", userId);
        }
    }

    /**
     * Clears all symbols cache.
     */
    public void clearSymbolCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.SYMBOLS);
        if (cache != null) {
            cache.clear();
            log.info("Cleared symbols cache");
        }
    }

    /**
     * Clears all candles cache.
     */
    public void clearCandleCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.CANDLES);
        if (cache != null) {
            cache.clear();
            log.info("Cleared candles cache");
        }
    }

    /**
     * Clears all positions cache.
     */
    public void clearPositionsCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.POSITIONS);
        if (cache != null) {
            cache.clear();
            log.info("Cleared positions cache");
        }
    }

    /**
     * Clears all orders cache.
     */
    public void clearOrdersCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.ORDERS);
        if (cache != null) {
            cache.clear();
            log.info("Cleared orders cache");
        }
    }

    /**
     * Clears all market data cache.
     */
    public void clearMarketDataCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.MARKET_DATA);
        if (cache != null) {
            cache.clear();
            log.info("Cleared market data cache");
        }
    }

    /**
     * Clears all account info cache.
     */
    public void clearAccountInfoCache() {
        Cache cache = cacheManager.getCache(RedisCacheConfig.CacheNames.ACCOUNT_INFO);
        if (cache != null) {
            cache.clear();
            log.info("Cleared account info cache");
        }
    }

    /**
     * Clears ALL caches.
     */
    public void clearAllCaches() {
        log.warn("Clearing ALL caches");

        clearSymbolCache();
        clearCandleCache();
        clearPositionsCache();
        clearOrdersCache();
        clearMarketDataCache();
        clearAccountInfoCache();

        // Reset statistics
        cacheHits.set(0);
        cacheMisses.set(0);
        totalRequests.set(0);

        log.info("All caches cleared and statistics reset");
    }

    /**
     * Resets cache statistics.
     */
    public void resetStatistics() {
        cacheHits.set(0);
        cacheMisses.set(0);
        totalRequests.set(0);
        log.info("Cache statistics reset");
    }
}
