package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.repository.MarketCandleRepository;
import com.bisttrading.marketdata.repository.MarketTickRepository;
import com.bisttrading.marketdata.repository.OrderBookSnapshotRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Advanced market data aggregation service
 * TimescaleDB'de kompleks analitik query'ler ve aggregation operations
 */
@Slf4j
@Service
public class MarketDataAggregationService {

    private final MarketTickRepository marketTickRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final OrderBookSnapshotRepository orderBookRepository;

    public MarketDataAggregationService(MarketTickRepository marketTickRepository,
                                       MarketCandleRepository marketCandleRepository,
                                       OrderBookSnapshotRepository orderBookRepository) {
        this.marketTickRepository = marketTickRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.orderBookRepository = orderBookRepository;
    }

    // =====================================
    // OHLCV AGGREGATIONS
    // =====================================

    /**
     * Comprehensive OHLCV analysis with multiple timeframes
     */
    @Async
    public CompletableFuture<Map<String, List<OHLCVData>>> getMultiTimeframeOHLCV(
            String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {

        Map<String, List<OHLCVData>> result = new HashMap<>();
        String[] timeframes = {"1 minute", "5 minutes", "15 minutes", "1 hour", "1 day"};

        try {
            for (String interval : timeframes) {
                List<Object[]> rawData = marketTickRepository.calculateOHLCV(symbol, startTime, endTime, interval);
                List<OHLCVData> ohlcvData = rawData.stream()
                    .map(this::mapToOHLCVData)
                    .collect(Collectors.toList());
                result.put(interval, ohlcvData);
            }

            log.debug("Multi-timeframe OHLCV calculated for {}: {} timeframes", symbol, timeframes.length);

        } catch (Exception e) {
            log.error("Multi-timeframe OHLCV calculation failed for {}: {}", symbol, e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Volume-based analysis
     */
    public VolumeAnalysis analyzeVolume(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            // Daily volume
            Long dailyVolume = marketTickRepository.calculateDailyVolume(symbol, startTime, endTime);

            // VWAP calculation
            BigDecimal vwap = marketTickRepository.calculateVWAP(symbol, startTime, endTime);

            // Top volume periods
            List<Object[]> topVolumePeriods = marketTickRepository.getTimeBucketedData(
                symbol, startTime, endTime, "1 hour"
            );

            // Price impact analysis
            List<Object[]> priceChanges = marketTickRepository.analyzePriceChanges(symbol, startTime, endTime);

            return VolumeAnalysis.builder()
                .symbol(symbol)
                .totalVolume(dailyVolume)
                .vwap(vwap)
                .topVolumePeriods(topVolumePeriods)
                .priceImpactData(priceChanges)
                .analysisTime(OffsetDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Volume analysis failed for {}: {}", symbol, e.getMessage(), e);
            return VolumeAnalysis.builder().symbol(symbol).build();
        }
    }

    // =====================================
    // TECHNICAL INDICATORS
    // =====================================

    /**
     * Comprehensive technical analysis
     */
    @Async
    public CompletableFuture<TechnicalAnalysis> calculateTechnicalIndicators(
            String symbol, String timeframe, OffsetDateTime startTime, OffsetDateTime endTime) {

        try {
            // SMA calculations (multiple periods)
            List<Object[]> sma20 = marketCandleRepository.calculateSMA(symbol, timeframe, 20, startTime, endTime);
            List<Object[]> sma50 = marketCandleRepository.calculateSMA(symbol, timeframe, 50, startTime, endTime);

            // RSI calculation
            List<Object[]> rsi = marketCandleRepository.calculateRSI(symbol, timeframe, 14, startTime, endTime);

            // Bollinger Bands
            List<Object[]> bollinger = marketCandleRepository.calculateBollingerBands(symbol, timeframe, 20, startTime, endTime);

            // Candlestick patterns
            List<Object[]> patterns = marketCandleRepository.detectCandlestickPatterns(symbol, timeframe, startTime, endTime);

            // Volume profile
            List<Object[]> volumeProfile = marketCandleRepository.analyzeVolumeProfile(
                symbol, timeframe, startTime, endTime, BigDecimal.valueOf(0.01), 20
            );

            TechnicalAnalysis analysis = TechnicalAnalysis.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .sma20(sma20)
                .sma50(sma50)
                .rsi(rsi)
                .bollingerBands(bollinger)
                .candlestickPatterns(patterns)
                .volumeProfile(volumeProfile)
                .calculatedAt(OffsetDateTime.now())
                .build();

            log.debug("Technical analysis calculated for {} - {}", symbol, timeframe);
            return CompletableFuture.completedFuture(analysis);

        } catch (Exception e) {
            log.error("Technical analysis failed for {} - {}: {}", symbol, timeframe, e.getMessage(), e);
            return CompletableFuture.completedFuture(TechnicalAnalysis.builder().symbol(symbol).build());
        }
    }

    /**
     * Market trend analysis
     */
    public TrendAnalysis analyzeTrend(String symbol, String timeframe, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            // Latest N candles for trend determination
            List<Object[]> recentCandles = marketCandleRepository.findBySymbolAndTimeframeAndTimeBetween(
                symbol, timeframe, startTime, endTime
            ).stream().map(candle -> new Object[]{
                candle.getTime(), candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume()
            }).collect(Collectors.toList());

            if (recentCandles.size() < 3) {
                return TrendAnalysis.builder().symbol(symbol).trend("INSUFFICIENT_DATA").build();
            }

            // Simple trend analysis based on recent closes
            String trend = determineTrend(recentCandles);
            double strength = calculateTrendStrength(recentCandles);

            // Volume trend
            String volumeTrend = analyzeVolumeTrend(recentCandles);

            return TrendAnalysis.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .trend(trend)
                .strength(strength)
                .volumeTrend(volumeTrend)
                .analysisTime(OffsetDateTime.now())
                .dataPoints(recentCandles.size())
                .build();

        } catch (Exception e) {
            log.error("Trend analysis failed for {} - {}: {}", symbol, timeframe, e.getMessage(), e);
            return TrendAnalysis.builder().symbol(symbol).trend("ERROR").build();
        }
    }

    // =====================================
    // ORDER BOOK ANALYTICS
    // =====================================

    /**
     * Advanced order book analysis
     */
    public OrderBookAnalysis analyzeOrderBook(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            // Spread analysis
            List<Object[]> spreadAnalysis = orderBookRepository.analyzeSpreadOverTime(symbol, startTime, endTime);

            // Depth analysis
            List<Object[]> depthAnalysis = orderBookRepository.analyzeOrderBookDepth(symbol, startTime, endTime);

            // Liquidity imbalance
            List<Object[]> imbalanceData = orderBookRepository.trackLiquidityImbalance(symbol, startTime, endTime);

            // Price level analysis
            List<Object[]> priceLevels = orderBookRepository.analyzePriceLevels(symbol, startTime, endTime);

            // Tick direction correlation
            List<Object[]> tickCorrelation = orderBookRepository.analyzeTickDirectionCorrelation(symbol, startTime, endTime);

            return OrderBookAnalysis.builder()
                .symbol(symbol)
                .spreadAnalysis(spreadAnalysis)
                .depthAnalysis(depthAnalysis)
                .imbalanceData(imbalanceData)
                .priceLevelAnalysis(priceLevels)
                .tickCorrelation(tickCorrelation)
                .analysisTime(OffsetDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Order book analysis failed for {}: {}", symbol, e.getMessage(), e);
            return OrderBookAnalysis.builder().symbol(symbol).build();
        }
    }

    /**
     * Market microstructure analysis
     */
    public MicrostructureAnalysis analyzeMicrostructure(String symbol, OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            // Spread statistics over time
            List<Object[]> spreadStats = orderBookRepository.analyzeSpreadOverTime(symbol, startTime, endTime);

            // Order book thickness
            List<Object[]> thickness = orderBookRepository.analyzeOrderBookThickness(
                symbol, startTime, endTime, org.springframework.data.domain.PageRequest.of(0, 100)
            );

            // Bid-ask spread from tick data
            List<Object[]> tickSpread = marketTickRepository.analyzeBidAskSpread(symbol, startTime, endTime);

            return MicrostructureAnalysis.builder()
                .symbol(symbol)
                .spreadStatistics(spreadStats)
                .orderBookThickness(thickness)
                .tickBasedSpread(tickSpread)
                .samplingPeriod(ChronoUnit.MINUTES.between(startTime, endTime))
                .analysisTime(OffsetDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Microstructure analysis failed for {}: {}", symbol, e.getMessage(), e);
            return MicrostructureAnalysis.builder().symbol(symbol).build();
        }
    }

    // =====================================
    // MARKET OVERVIEW ANALYTICS
    // =====================================

    /**
     * Market-wide statistics
     */
    public MarketOverview getMarketOverview(OffsetDateTime startTime, OffsetDateTime endTime) {
        try {
            // Active symbols
            List<String> activeSymbols = marketTickRepository.findActiveSymbolsSince(startTime);

            // Top volume symbols
            List<Object[]> topVolume = marketTickRepository.findTopVolumeSymbols(startTime, endTime, 10);

            // Database metrics
            List<Object[]> dbMetrics = marketTickRepository.getDatabaseMetrics();

            // Order book metrics
            List<Object[]> orderBookMetrics = orderBookRepository.getDatabaseMetrics();

            return MarketOverview.builder()
                .activeSymbolCount(activeSymbols.size())
                .activeSymbols(activeSymbols)
                .topVolumeSymbols(topVolume)
                .databaseMetrics(dbMetrics)
                .orderBookMetrics(orderBookMetrics)
                .analysisTime(OffsetDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Market overview analysis failed: {}", e.getMessage(), e);
            return MarketOverview.builder().build();
        }
    }

    // =====================================
    // UTILITY METHODS
    // =====================================

    private OHLCVData mapToOHLCVData(Object[] row) {
        return OHLCVData.builder()
            .period((OffsetDateTime) row[0])
            .symbol((String) row[1])
            .open((BigDecimal) row[2])
            .high((BigDecimal) row[3])
            .low((BigDecimal) row[4])
            .close((BigDecimal) row[5])
            .volume((Long) row[6])
            .tradesCount((Long) row[7])
            .avgPrice((BigDecimal) row[8])
            .build();
    }

    private String determineTrend(List<Object[]> candles) {
        if (candles.size() < 3) return "NEUTRAL";

        List<BigDecimal> closes = candles.stream()
            .map(row -> (BigDecimal) row[4]) // close price
            .collect(Collectors.toList());

        // Simple trend based on first and last closes
        BigDecimal first = closes.get(0);
        BigDecimal last = closes.get(closes.size() - 1);

        double changePercent = last.subtract(first).divide(first, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100)).doubleValue();

        if (changePercent > 1.0) return "BULLISH";
        else if (changePercent < -1.0) return "BEARISH";
        else return "NEUTRAL";
    }

    private double calculateTrendStrength(List<Object[]> candles) {
        // Simple trend strength calculation based on price volatility
        List<BigDecimal> closes = candles.stream()
            .map(row -> (BigDecimal) row[4])
            .collect(Collectors.toList());

        if (closes.size() < 2) return 0.0;

        double variance = 0.0;
        BigDecimal mean = closes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(closes.size()), BigDecimal.ROUND_HALF_UP);

        for (BigDecimal close : closes) {
            double diff = close.subtract(mean).doubleValue();
            variance += diff * diff;
        }

        double stddev = Math.sqrt(variance / closes.size());
        return Math.min(1.0, stddev / mean.doubleValue() * 10); // Normalized strength
    }

    private String analyzeVolumeTrend(List<Object[]> candles) {
        if (candles.size() < 3) return "NEUTRAL";

        List<Long> volumes = candles.stream()
            .map(row -> (Long) row[5]) // volume
            .collect(Collectors.toList());

        // Compare recent average to earlier average
        int half = volumes.size() / 2;
        double firstHalfAvg = volumes.subList(0, half).stream().mapToLong(Long::longValue).average().orElse(0.0);
        double secondHalfAvg = volumes.subList(half, volumes.size()).stream().mapToLong(Long::longValue).average().orElse(0.0);

        double change = (secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100;

        if (change > 20) return "INCREASING";
        else if (change < -20) return "DECREASING";
        else return "STABLE";
    }

    // =====================================
    // DTO CLASSES
    // =====================================

    @Builder
    @Data
    public static class OHLCVData {
        private OffsetDateTime period;
        private String symbol;
        private BigDecimal open, high, low, close;
        private Long volume, tradesCount;
        private BigDecimal avgPrice;
    }

    @Builder
    @Data
    public static class VolumeAnalysis {
        private String symbol;
        private Long totalVolume;
        private BigDecimal vwap;
        private List<Object[]> topVolumePeriods;
        private List<Object[]> priceImpactData;
        private OffsetDateTime analysisTime;
    }

    @Builder
    @Data
    public static class TechnicalAnalysis {
        private String symbol, timeframe;
        private List<Object[]> sma20, sma50, rsi, bollingerBands, candlestickPatterns, volumeProfile;
        private OffsetDateTime calculatedAt;
    }

    @Builder
    @Data
    public static class TrendAnalysis {
        private String symbol, timeframe, trend, volumeTrend;
        private double strength;
        private OffsetDateTime analysisTime;
        private int dataPoints;
    }

    @Builder
    @Data
    public static class OrderBookAnalysis {
        private String symbol;
        private List<Object[]> spreadAnalysis, depthAnalysis, imbalanceData, priceLevelAnalysis, tickCorrelation;
        private OffsetDateTime analysisTime;
    }

    @Builder
    @Data
    public static class MicrostructureAnalysis {
        private String symbol;
        private List<Object[]> spreadStatistics, orderBookThickness, tickBasedSpread;
        private long samplingPeriod;
        private OffsetDateTime analysisTime;
    }

    @Builder
    @Data
    public static class MarketOverview {
        private int activeSymbolCount;
        private List<String> activeSymbols;
        private List<Object[]> topVolumeSymbols, databaseMetrics, orderBookMetrics;
        private OffsetDateTime analysisTime;
    }
}