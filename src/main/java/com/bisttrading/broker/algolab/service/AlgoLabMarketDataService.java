package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.config.RedisCacheConfig;
import com.bisttrading.broker.algolab.exception.AlgoLabApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AlgoLab market data service with Redis caching support.
 *
 * Caching Strategy:
 * - Symbol info: 1 hour TTL (data rarely changes)
 * - Candle data: 5 minutes TTL (updates every bar)
 * - Cache invalidation on updates via WebSocket
 */
@Service
@Slf4j
public class AlgoLabMarketDataService {

    private final AlgoLabRestClient restClient;
    private final AlgoLabCacheService cacheService;

    public AlgoLabMarketDataService(
            AlgoLabRestClient restClient,
            @Lazy AlgoLabCacheService cacheService) {
        this.restClient = restClient;
        this.cacheService = cacheService;
    }

    /**
     * Gets symbol (equity/stock) information with caching.
     *
     * Cache: 1 hour TTL (symbol info rarely changes)
     *
     * @param symbol Symbol code (e.g., "AKBNK", "GARAN")
     * @return Response with symbol info (price, ceiling, floor, volume, etc.)
     */
    @Cacheable(
        value = RedisCacheConfig.CacheNames.SYMBOLS,
        key = "#symbol",
        unless = "#result == null || #result.isEmpty()"
    )
    public Map<String, Object> getSymbolInfo(String symbol) {
        log.debug("Cache MISS - Fetching symbol info from API: {}", symbol);
        cacheService.recordCacheMiss();

        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/GetEquityInfo",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from GetEquityInfo", 500);
            }

            log.debug("Symbol info fetched successfully for: {}", symbol);
            return body;

        } catch (Exception e) {
            log.error("GetEquityInfo failed for symbol: {}", symbol, e);
            throw e;
        }
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use {@link #getSymbolInfo(String)} instead
     */
    @Deprecated
    public Map<String, Object> getEquityInfo(String symbol) {
        return getSymbolInfo(symbol);
    }

    /**
     * Gets candle (OHLCV) data with caching.
     * Returns last 250 bars.
     *
     * Cache: 5 minutes TTL (updates every bar completion)
     *
     * @param symbol Symbol code
     * @param periodMinutes Period in minutes (1, 5, 15, 30, 60, 120, 240, 480, 1440 for daily)
     * @return Response with candles
     */
    @Cacheable(
        value = RedisCacheConfig.CacheNames.CANDLES,
        key = "#symbol + '_' + #periodMinutes",
        unless = "#result == null || #result.isEmpty()"
    )
    public Map<String, Object> getCandleData(String symbol, int periodMinutes) {
        log.debug("Cache MISS - Fetching candle data from API: {} - {} minutes", symbol, periodMinutes);
        cacheService.recordCacheMiss();

        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("period", String.valueOf(periodMinutes));

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/GetCandleData",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from GetCandleData", 500);
            }

            log.debug("Candle data fetched successfully for: {} - {} minutes", symbol, periodMinutes);
            return body;

        } catch (Exception e) {
            log.error("GetCandleData failed for symbol: {}, period: {}", symbol, periodMinutes, e);
            throw e;
        }
    }

    // ===== Cache Management Methods =====

    /**
     * Updates symbol info in cache (for real-time WebSocket updates).
     *
     * @param symbol Symbol code
     * @param data Updated symbol data
     * @return Updated data
     */
    @CachePut(
        value = RedisCacheConfig.CacheNames.SYMBOLS,
        key = "#symbol"
    )
    public Map<String, Object> updateSymbolInfoCache(String symbol, Map<String, Object> data) {
        log.debug("Updating symbol info cache for: {}", symbol);
        return data;
    }

    /**
     * Updates market data cache (for real-time WebSocket tick updates).
     *
     * @param symbol Symbol code
     * @param data Updated market data
     * @return Updated data
     */
    @CachePut(
        value = RedisCacheConfig.CacheNames.MARKET_DATA,
        key = "#symbol"
    )
    public Map<String, Object> updateMarketDataCache(String symbol, Map<String, Object> data) {
        log.debug("Updating market data cache for: {}", symbol);
        return data;
    }

    /**
     * Invalidates symbol cache entry (manual refresh).
     *
     * @param symbol Symbol to invalidate
     */
    @CacheEvict(
        value = RedisCacheConfig.CacheNames.SYMBOLS,
        key = "#symbol"
    )
    public void invalidateSymbolCache(String symbol) {
        log.debug("Invalidated symbol cache for: {}", symbol);
    }

    /**
     * Invalidates candle cache entry (on bar completion).
     *
     * @param symbol Symbol code
     * @param periodMinutes Period in minutes
     */
    @CacheEvict(
        value = RedisCacheConfig.CacheNames.CANDLES,
        key = "#symbol + '_' + #periodMinutes"
    )
    public void invalidateCandleCache(String symbol, int periodMinutes) {
        log.debug("Invalidated candle cache for: {} - {} minutes", symbol, periodMinutes);
    }

    /**
     * Invalidates all symbol cache entries.
     */
    @CacheEvict(
        value = RedisCacheConfig.CacheNames.SYMBOLS,
        allEntries = true
    )
    public void invalidateAllSymbolCache() {
        log.info("Invalidated all symbol cache entries");
    }

    /**
     * Invalidates all candle cache entries.
     */
    @CacheEvict(
        value = RedisCacheConfig.CacheNames.CANDLES,
        allEntries = true
    )
    public void invalidateAllCandleCache() {
        log.info("Invalidated all candle cache entries");
    }

    /**
     * Invalidates all caches for a specific symbol (symbol info, candles, market data).
     *
     * @param symbol Symbol to invalidate
     */
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CacheNames.SYMBOLS, key = "#symbol"),
        @CacheEvict(value = RedisCacheConfig.CacheNames.MARKET_DATA, key = "#symbol")
    })
    public void invalidateAllCachesForSymbol(String symbol) {
        log.info("Invalidated all caches for symbol: {}", symbol);

        // Also invalidate candles for common periods
        int[] commonPeriods = {1, 5, 15, 30, 60, 240, 1440};
        for (int period : commonPeriods) {
            invalidateCandleCache(symbol, period);
        }
    }
}

