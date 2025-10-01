package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.dto.MarketMessage;
import com.bisttrading.marketdata.entity.MarketCandle;
import com.bisttrading.marketdata.entity.MarketTick;
import com.bisttrading.marketdata.entity.OrderBookSnapshot;
import com.bisttrading.marketdata.repository.MarketCandleRepository;
import com.bisttrading.marketdata.repository.MarketTickRepository;
import com.bisttrading.marketdata.repository.OrderBookSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TimescaleDB operations için comprehensive service layer
 * Batch processing, aggregation, ve analitik query'ler
 */
@Slf4j
@Service
public class TimescaleDbService {

    private final MarketTickRepository marketTickRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final OrderBookSnapshotRepository orderBookRepository;
    private final ObjectMapper objectMapper;

    // Configuration
    private final int batchSize;
    private final boolean enableAsyncProcessing;

    public TimescaleDbService(MarketTickRepository marketTickRepository,
                             MarketCandleRepository marketCandleRepository,
                             OrderBookSnapshotRepository orderBookRepository,
                             ObjectMapper objectMapper,
                             @Value("${timescaledb.batch.size:1000}") int batchSize,
                             @Value("${timescaledb.async.enabled:true}") boolean enableAsyncProcessing) {
        this.marketTickRepository = marketTickRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.orderBookRepository = orderBookRepository;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.enableAsyncProcessing = enableAsyncProcessing;
    }

    // =====================================
    // MARKET TICK OPERATIONS
    // =====================================

    /**
     * Market tick verisini TimescaleDB'ye kaydeder (batch optimized)
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveTick(MarketMessage.TickData tickData) {
        try {
            if (tickData.getSymbol() == null || tickData.getPrice() == null) {
                log.warn("Geçersiz tick data: {}", tickData);
                return CompletableFuture.completedFuture(null);
            }

            OffsetDateTime now = OffsetDateTime.now();

            marketTickRepository.insertTickWithConflictIgnore(
                now,
                tickData.getSymbol(),
                tickData.getPrice(),
                tickData.getVolume() != null ? tickData.getVolume() : 0L,
                extractBid(tickData),
                extractAsk(tickData),
                extractBidSize(tickData),
                extractAskSize(tickData),
                tickData.getVolume() != null ? tickData.getVolume() : 0L, // totalVolume
                1L, // totalTrades
                tickData.getPrice(), // VWAP (will be calculated later)
                tickData.getDirection(),
                tickData.getValue() != null ? tickData.getValue() : tickData.getPrice(),
                tickData.getBuyer(),
                tickData.getSeller(),
                tickData.getTime()
            );

            log.trace("Tick kaydedildi: {} @ {}", tickData.getSymbol(), tickData.getPrice());

        } catch (Exception e) {
            log.error("Tick kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Order book snapshot'ı kaydeder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveOrderBookSnapshot(MarketMessage.DepthData depthData) {
        try {
            if (depthData.getSymbol() == null) {
                log.warn("Geçersiz depth data: {}", depthData);
                return CompletableFuture.completedFuture(null);
            }

            OffsetDateTime now = OffsetDateTime.now();

            String bidsJson = null;
            String asksJson = null;
            int bidCount = 0;
            int askCount = 0;
            long totalBidVolume = 0L;
            long totalAskVolume = 0L;
            BigDecimal bestBid = null;
            BigDecimal bestAsk = null;
            Long bestBidSize = null;
            Long bestAskSize = null;

            // Process bids
            if (depthData.getBids() != null && depthData.getBids().length > 0) {
                bidsJson = objectMapper.writeValueAsString(depthData.getBids());
                bidCount = depthData.getBids().length;

                // Calculate bid metrics
                for (MarketMessage.BookEntry bid : depthData.getBids()) {
                    totalBidVolume += bid.getQuantity() != null ? bid.getQuantity() : 0L;
                    if (bestBid == null || (bid.getPrice() != null && bid.getPrice().compareTo(bestBid) > 0)) {
                        bestBid = bid.getPrice();
                        bestBidSize = bid.getQuantity();
                    }
                }
            }

            // Process asks
            if (depthData.getAsks() != null && depthData.getAsks().length > 0) {
                asksJson = objectMapper.writeValueAsString(depthData.getAsks());
                askCount = depthData.getAsks().length;

                // Calculate ask metrics
                for (MarketMessage.BookEntry ask : depthData.getAsks()) {
                    totalAskVolume += ask.getQuantity() != null ? ask.getQuantity() : 0L;
                    if (bestAsk == null || (ask.getPrice() != null && ask.getPrice().compareTo(bestAsk) < 0)) {
                        bestAsk = ask.getPrice();
                        bestAskSize = ask.getQuantity();
                    }
                }
            }

            // Calculate derived metrics
            BigDecimal bidAskSpread = null;
            BigDecimal midPrice = null;
            BigDecimal imbalanceRatio = null;
            BigDecimal spreadPercentage = null;

            if (bestBid != null && bestAsk != null) {
                bidAskSpread = bestAsk.subtract(bestBid);
                midPrice = bestBid.add(bestAsk).divide(BigDecimal.valueOf(2));

                if (midPrice.compareTo(BigDecimal.ZERO) > 0) {
                    spreadPercentage = bidAskSpread.divide(midPrice, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                }
            }

            if (totalBidVolume > 0 || totalAskVolume > 0) {
                long totalVolume = totalBidVolume + totalAskVolume;
                imbalanceRatio = BigDecimal.valueOf(totalBidVolume)
                    .divide(BigDecimal.valueOf(totalVolume), 8, RoundingMode.HALF_UP);
            }

            orderBookRepository.upsertOrderBookSnapshot(
                now,
                depthData.getSymbol(),
                bidsJson,
                asksJson,
                bidCount,
                askCount,
                totalBidVolume,
                totalAskVolume,
                bestBid,
                bestAsk,
                bestBidSize,
                bestAskSize,
                bidAskSpread,
                midPrice,
                imbalanceRatio,
                spreadPercentage,
                System.currentTimeMillis() // sequence number
            );

            log.trace("Order book snapshot kaydedildi: {} - Bids: {}, Asks: {}",
                depthData.getSymbol(), bidCount, askCount);

        } catch (JsonProcessingException e) {
            log.error("Order book JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Order book snapshot kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    // =====================================
    // CANDLE GENERATION & MANAGEMENT
    // =====================================

    /**
     * Tick verilerinden candle oluşturur
     */
    @Async
    @Transactional
    public CompletableFuture<Void> generateCandleFromTicks(String symbol, String timeframe,
                                                          OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            String interval = mapTimeframeToInterval(timeframe);

            marketCandleRepository.createCandlesFromTicks(
                symbol, timeframe, interval, startTime, endTime
            );

            log.debug("Candle oluşturuldu: {} - {} - {} to {}", symbol, timeframe, startTime, endTime);

        } catch (Exception e) {
            log.error("Candle oluşturma hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Aktif candle'ı günceller (gerçek zamanlı)
     */
    @Transactional
    public void updateActiveCandle(String symbol, String timeframe, MarketMessage.TickData tickData) {
        try {
            OffsetDateTime candleTime = alignToCandleBoundary(OffsetDateTime.now(), timeframe);

            marketCandleRepository.upsertCandle(
                candleTime,
                symbol,
                timeframe,
                tickData.getPrice(), // open (will be handled by upsert logic)
                tickData.getPrice(), // high
                tickData.getPrice(), // low
                tickData.getPrice(), // close
                tickData.getVolume() != null ? tickData.getVolume() : 0L,
                1L, // trades count
                tickData.getPrice(), // vwap
                false // not complete
            );

        } catch (Exception e) {
            log.error("Aktif candle güncelleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Tamamlanmış candle'ları işaretle
     */
    @Transactional
    public void markCandlesAsComplete(String timeframe) {
        try {
            OffsetDateTime completionTime = alignToCandleBoundary(
                OffsetDateTime.now().minus(getDurationFromTimeframe(timeframe)),
                timeframe
            );

            int updatedCount = marketCandleRepository.markCandlesAsComplete(completionTime, OffsetDateTime.now());

            if (updatedCount > 0) {
                log.debug("Candle'lar tamamlandı: {} adet - {}", updatedCount, timeframe);
            }

        } catch (Exception e) {
            log.error("Candle tamamlama hatası: {}", e.getMessage(), e);
        }
    }

    // =====================================
    // AGGREGATION & ANALYTICS
    // =====================================

    /**
     * OHLCV verileri hesaplar
     */
    public List<Object[]> calculateOHLCV(String symbol, OffsetDateTime startTime, OffsetDateTime endTime, String interval) {
        try {
            return marketTickRepository.calculateOHLCV(symbol, startTime, endTime, interval);
        } catch (Exception e) {
            log.error("OHLCV hesaplama hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * VWAP hesaplar
     */
    public BigDecimal calculateVWAP(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return marketTickRepository.calculateVWAP(symbol, startTime, endTime);
        } catch (Exception e) {
            log.error("VWAP hesaplama hatası: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Technical indicators hesaplar
     */
    public List<Object[]> calculateSMA(String symbol, String timeframe, int period,
                                      OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return marketCandleRepository.calculateSMA(symbol, timeframe, period, startTime, endTime);
        } catch (Exception e) {
            log.error("SMA hesaplama hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * RSI hesaplar
     */
    public List<Object[]> calculateRSI(String symbol, String timeframe, int period,
                                      OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return marketCandleRepository.calculateRSI(symbol, timeframe, period, startTime, endTime);
        } catch (Exception e) {
            log.error("RSI hesaplama hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Bollinger Bands hesaplar
     */
    public List<Object[]> calculateBollingerBands(String symbol, String timeframe, int period,
                                                  OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return marketCandleRepository.calculateBollingerBands(symbol, timeframe, period, startTime, endTime);
        } catch (Exception e) {
            log.error("Bollinger Bands hesaplama hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =====================================
    // ORDER BOOK ANALYTICS
    // =====================================

    /**
     * Spread analysis
     */
    public List<Object[]> analyzeSpread(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return orderBookRepository.analyzeSpreadOverTime(symbol, startTime, endTime);
        } catch (Exception e) {
            log.error("Spread analysis hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Order book depth analysis
     */
    public List<Object[]> analyzeOrderBookDepth(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return orderBookRepository.analyzeOrderBookDepth(symbol, startTime, endTime);
        } catch (Exception e) {
            log.error("Order book depth analysis hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Liquidity imbalance tracking
     */
    public List<Object[]> trackLiquidityImbalance(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            return orderBookRepository.trackLiquidityImbalance(symbol, startTime, endTime);
        } catch (Exception e) {
            log.error("Liquidity imbalance tracking hatası: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =====================================
    // DATA MANAGEMENT
    // =====================================

    /**
     * Data retention policy uygular
     */
    @Transactional
    public void applyDataRetentionPolicy(int retentionDays) {
        try {
            OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);

            // Market tick cleanup
            int deletedTicks = marketTickRepository.deleteOldData(cutoffTime);
            log.info("Market tick cleanup: {} kayıt silindi", deletedTicks);

            // Order book cleanup
            int deletedSnapshots = orderBookRepository.deleteOldSnapshots(cutoffTime);
            log.info("Order book cleanup: {} snapshot silindi", deletedSnapshots);

            // Candle cleanup (farklı timeframe'ler için farklı retention)
            cleanupCandlesByTimeframe(cutoffTime);

        } catch (Exception e) {
            log.error("Data retention policy hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Database performance metrics
     */
    public DatabaseMetrics getDatabaseMetrics() {
        try {
            var tickMetrics = marketTickRepository.getDatabaseMetrics();
            var candleMetrics = marketCandleRepository.getCandleStatistics(null, null);
            var orderBookMetrics = orderBookRepository.getDatabaseMetrics();

            return DatabaseMetrics.builder()
                .tickMetrics(tickMetrics)
                .candleMetrics(candleMetrics)
                .orderBookMetrics(orderBookMetrics)
                .build();

        } catch (Exception e) {
            log.error("Database metrics alma hatası: {}", e.getMessage(), e);
            return DatabaseMetrics.builder().build();
        }
    }

    // =====================================
    // UTILITY METHODS
    // =====================================

    private String mapTimeframeToInterval(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m" -> "1 minute";
            case "5m" -> "5 minutes";
            case "15m" -> "15 minutes";
            case "30m" -> "30 minutes";
            case "1h" -> "1 hour";
            case "4h" -> "4 hours";
            case "1d" -> "1 day";
            case "1w" -> "1 week";
            case "1M" -> "1 month";
            default -> "1 minute";
        };
    }

    private OffsetDateTime alignToCandleBoundary(OffsetDateTime time, String timeframe) {
        // Candle boundary'sine hizala (örnek implementasyon)
        int minutes = switch (timeframe.toLowerCase()) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "1h" -> 60;
            case "4h" -> 240;
            default -> 1;
        };

        int alignedMinute = (time.getMinute() / minutes) * minutes;
        return time.withMinute(alignedMinute).withSecond(0).withNano(0);
    }

    private java.time.Duration getDurationFromTimeframe(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m" -> java.time.Duration.ofMinutes(1);
            case "5m" -> java.time.Duration.ofMinutes(5);
            case "15m" -> java.time.Duration.ofMinutes(15);
            case "30m" -> java.time.Duration.ofMinutes(30);
            case "1h" -> java.time.Duration.ofHours(1);
            case "4h" -> java.time.Duration.ofHours(4);
            case "1d" -> java.time.Duration.ofDays(1);
            default -> java.time.Duration.ofMinutes(1);
        };
    }

    private void cleanupCandlesByTimeframe(OffsetDateTime baseCutoff) {
        // Farklı timeframe'ler için farklı retention periods
        String[] timeframes = {"1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w", "1M"};
        int[] retentionDays = {7, 30, 90, 180, 365, 365*2, 365*5, 365*10, 365*10};

        for (int i = 0; i < timeframes.length; i++) {
            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays[i]);
            int deleted = marketCandleRepository.deleteOldCandles(cutoff, timeframes[i]);
            if (deleted > 0) {
                log.info("Candle cleanup {}: {} kayıt silindi", timeframes[i], deleted);
            }
        }
    }

    private BigDecimal extractBid(MarketMessage.TickData tickData) {
        // Tick data'dan bid fiyatını çıkar (implementation depends on data structure)
        return null; // TODO: Implement based on actual data structure
    }

    private BigDecimal extractAsk(MarketMessage.TickData tickData) {
        // Tick data'dan ask fiyatını çıkar
        return null; // TODO: Implement based on actual data structure
    }

    private Long extractBidSize(MarketMessage.TickData tickData) {
        return null; // TODO: Implement based on actual data structure
    }

    private Long extractAskSize(MarketMessage.TickData tickData) {
        return null; // TODO: Implement based on actual data structure
    }

    /**
     * Database metrics DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class DatabaseMetrics {
        private List<Object[]> tickMetrics;
        private List<Object[]> candleMetrics;
        private List<Object[]> orderBookMetrics;

    }
}