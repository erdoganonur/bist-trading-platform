package com.bisttrading.caching.warming;

import com.bisttrading.caching.strategy.CacheInvalidationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Cache warming service for BIST Trading Platform
 * Proactively loads frequently accessed data into cache
 */
@Service
public class CacheWarmingService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmingService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheInvalidationStrategy invalidationStrategy;
    private final CacheWarmingConfig warmingConfig;

    // Market data that should be pre-loaded
    private static final List<String> HIGH_PRIORITY_SYMBOLS = List.of(
        "AKBNK", "THYAO", "GARAN", "ISCTR", "KCHOL", "ARCLK", "TUPRS", "YKBNK", "SAHOL", "VACBT"
    );

    public CacheWarmingService(
            RedisTemplate<String, Object> redisTemplate,
            CacheInvalidationStrategy invalidationStrategy,
            CacheWarmingConfig warmingConfig) {
        this.redisTemplate = redisTemplate;
        this.invalidationStrategy = invalidationStrategy;
        this.warmingConfig = warmingConfig;
    }

    /**
     * Warm up critical caches during application startup
     */
    public CompletableFuture<Void> warmUpOnStartup() {
        logger.info("Starting cache warm-up process...");

        return CompletableFuture.allOf(
            warmUpMarketDataCache(),
            warmUpSymbolCache(),
            warmUpUserSessionCache(),
            warmUpTradingParametersCache()
        ).thenRun(() -> {
            logger.info("Cache warm-up process completed successfully");
        }).exceptionally(throwable -> {
            logger.error("Cache warm-up process failed: {}", throwable.getMessage(), throwable);
            return null;
        });
    }

    /**
     * Warm up market data cache with latest prices and OHLCV data
     */
    private CompletableFuture<Void> warmUpMarketDataCache() {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Warming up market data cache...");

            try {
                for (String symbol : HIGH_PRIORITY_SYMBOLS) {
                    // Warm up latest price data
                    String priceKey = "market:data:" + symbol + ":latest";
                    warmUpCacheKey(priceKey, () -> loadLatestMarketData(symbol));

                    // Warm up OHLCV data for different timeframes
                    warmUpOhlcvData(symbol, "1m");
                    warmUpOhlcvData(symbol, "5m");
                    warmUpOhlcvData(symbol, "1h");
                    warmUpOhlcvData(symbol, "1d");

                    // Add small delay to prevent overwhelming the system
                    Thread.sleep(warmingConfig.getSymbolDelayMs());
                }

                logger.debug("Market data cache warm-up completed");
            } catch (Exception e) {
                logger.error("Failed to warm up market data cache", e);
                throw new RuntimeException("Market data cache warm-up failed", e);
            }
        });
    }

    /**
     * Warm up symbol information cache
     */
    private CompletableFuture<Void> warmUpSymbolCache() {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Warming up symbol cache...");

            try {
                // Warm up symbol list
                String symbolListKey = "market:symbols:active";
                warmUpCacheKey(symbolListKey, this::loadActiveSymbols);

                // Warm up individual symbol details
                for (String symbol : HIGH_PRIORITY_SYMBOLS) {
                    String symbolKey = "market:symbols:" + symbol;
                    warmUpCacheKey(symbolKey, () -> loadSymbolDetails(symbol));
                }

                logger.debug("Symbol cache warm-up completed");
            } catch (Exception e) {
                logger.error("Failed to warm up symbol cache", e);
                throw new RuntimeException("Symbol cache warm-up failed", e);
            }
        });
    }

    /**
     * Warm up user session cache for active users
     */
    private CompletableFuture<Void> warmUpUserSessionCache() {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Warming up user session cache...");

            try {
                // This would typically query the database for active sessions
                // and preload them into cache
                String activeSessionsKey = "user:sessions:active";
                warmUpCacheKey(activeSessionsKey, this::loadActiveSessions);

                logger.debug("User session cache warm-up completed");
            } catch (Exception e) {
                logger.error("Failed to warm up user session cache", e);
                throw new RuntimeException("User session cache warm-up failed", e);
            }
        });
    }

    /**
     * Warm up trading parameters cache
     */
    private CompletableFuture<Void> warmUpTradingParametersCache() {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Warming up trading parameters cache...");

            try {
                // Trading hours
                String tradingHoursKey = "trading:hours:current";
                warmUpCacheKey(tradingHoursKey, this::loadTradingHours);

                // Market status
                String marketStatusKey = "trading:market:status";
                warmUpCacheKey(marketStatusKey, this::loadMarketStatus);

                // Trading fees
                String tradingFeesKey = "trading:fees:current";
                warmUpCacheKey(tradingFeesKey, this::loadTradingFees);

                logger.debug("Trading parameters cache warm-up completed");
            } catch (Exception e) {
                logger.error("Failed to warm up trading parameters cache", e);
                throw new RuntimeException("Trading parameters cache warm-up failed", e);
            }
        });
    }

    /**
     * Scheduled cache refresh for frequently accessed data
     */
    @Scheduled(fixedRateString = "#{@cacheWarmingConfig.getRefreshIntervalMs()}")
    public void scheduledCacheRefresh() {
        if (!warmingConfig.isScheduledRefreshEnabled()) {
            return;
        }

        logger.debug("Starting scheduled cache refresh...");

        // Refresh high-priority market data
        refreshMarketDataCache();

        // Refresh system parameters
        refreshSystemParametersCache();

        logger.debug("Scheduled cache refresh completed");
    }

    /**
     * Warm up specific OHLCV data for a symbol and timeframe
     */
    private void warmUpOhlcvData(String symbol, String timeframe) {
        String ohlcvKey = "market:ohlcv:" + symbol + ":" + timeframe;
        warmUpCacheKey(ohlcvKey, () -> loadOhlcvData(symbol, timeframe));
    }

    /**
     * Generic method to warm up a cache key with data loader
     */
    private void warmUpCacheKey(String key, CacheDataLoader loader) {
        try {
            // Check if key already exists and is not expired
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl > warmingConfig.getMinTtlSecondsForSkip()) {
                    logger.debug("Skipping warm-up for key {} (TTL: {}s)", key, ttl);
                    return;
                }
            }

            // Load data and cache it
            Object data = loader.loadData();
            if (data != null) {
                Duration ttl = warmingConfig.getDefaultTtlForKey(key);
                if (ttl.isPositive()) {
                    redisTemplate.opsForValue().set(key, data, ttl);
                } else {
                    redisTemplate.opsForValue().set(key, data);
                }
                logger.debug("Cache key warmed up: {}", key);
            } else {
                logger.debug("No data to warm up for key: {}", key);
            }

        } catch (Exception e) {
            logger.error("Failed to warm up cache key: {}", key, e);
        }
    }

    /**
     * Refresh market data cache for active symbols
     */
    private void refreshMarketDataCache() {
        for (String symbol : HIGH_PRIORITY_SYMBOLS) {
            try {
                String priceKey = "market:data:" + symbol + ":latest";
                Object freshData = loadLatestMarketData(symbol);
                if (freshData != null) {
                    redisTemplate.opsForValue().set(priceKey, freshData,
                        warmingConfig.getMarketDataTtl());
                }
            } catch (Exception e) {
                logger.error("Failed to refresh market data for symbol: {}", symbol, e);
            }
        }
    }

    /**
     * Refresh system parameters cache
     */
    private void refreshSystemParametersCache() {
        try {
            // Refresh trading hours
            String tradingHoursKey = "trading:hours:current";
            Object tradingHours = loadTradingHours();
            if (tradingHours != null) {
                redisTemplate.opsForValue().set(tradingHoursKey, tradingHours,
                    warmingConfig.getSystemParametersTtl());
            }

            // Refresh market status
            String marketStatusKey = "trading:market:status";
            Object marketStatus = loadMarketStatus();
            if (marketStatus != null) {
                redisTemplate.opsForValue().set(marketStatusKey, marketStatus,
                    warmingConfig.getSystemParametersTtl());
            }

        } catch (Exception e) {
            logger.error("Failed to refresh system parameters cache", e);
        }
    }

    // Data loader placeholder methods - would be implemented with actual data sources
    private Object loadLatestMarketData(String symbol) {
        // Placeholder - would load from database or external API
        return new MarketDataPoint(symbol, 100.0, Instant.now());
    }

    private Object loadOhlcvData(String symbol, String timeframe) {
        // Placeholder - would load OHLCV data from database
        return new OhlcvData(symbol, timeframe, Instant.now());
    }

    private Object loadActiveSymbols() {
        // Placeholder - would load from database
        return HIGH_PRIORITY_SYMBOLS;
    }

    private Object loadSymbolDetails(String symbol) {
        // Placeholder - would load from database
        return new SymbolDetails(symbol, symbol + " Company", "BIST", true);
    }

    private Object loadActiveSessions() {
        // Placeholder - would load from database
        return Set.of();
    }

    private Object loadTradingHours() {
        // Placeholder - would load from configuration or database
        return new TradingHours("09:30", "18:00", "Europe/Istanbul");
    }

    private Object loadMarketStatus() {
        // Placeholder - would determine current market status
        return new MarketStatus("OPEN", Instant.now());
    }

    private Object loadTradingFees() {
        // Placeholder - would load from configuration or database
        return new TradingFees(0.001, 0.002, 0.0);
    }

    // Functional interface for data loading
    @FunctionalInterface
    private interface CacheDataLoader {
        Object loadData() throws Exception;
    }

    // Placeholder data classes
    private record MarketDataPoint(String symbol, Double price, Instant timestamp) {}
    private record OhlcvData(String symbol, String timeframe, Instant timestamp) {}
    private record SymbolDetails(String symbol, String name, String market, Boolean active) {}
    private record TradingHours(String openTime, String closeTime, String timezone) {}
    private record MarketStatus(String status, Instant timestamp) {}
    private record TradingFees(Double buyFee, Double sellFee, Double minimumFee) {}
}