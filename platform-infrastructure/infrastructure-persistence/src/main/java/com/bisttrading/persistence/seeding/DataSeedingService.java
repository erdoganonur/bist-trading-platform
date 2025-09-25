package com.bisttrading.persistence.seeding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Data seeding service for BIST Trading Platform
 * Generates realistic market data and test scenarios
 */
@Service
public class DataSeedingService {

    private static final Logger logger = LoggerFactory.getLogger(DataSeedingService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    @Value("${bist.seeding.enabled:true}")
    private boolean seedingEnabled;

    @Value("${bist.seeding.market-data.enabled:true}")
    private boolean marketDataSeedingEnabled;

    @Value("${bist.seeding.market-data.days:30}")
    private int marketDataDays;

    @Value("${bist.seeding.market-data.ticks-per-day:1000}")
    private int ticksPerDay;

    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    // High-volume Turkish stocks for realistic data
    private static final List<String> MAJOR_SYMBOLS = List.of(
        "AKBNK", "GARAN", "ISCTR", "YKBNK", "HALKB", "VAKBN", // Banking
        "TUPRS", "PETKM", "AKSEN", "ZOREN", // Energy
        "ASELS", "LOGO", "NETAS", "ARMDA", // Technology
        "THYAO", "TTKOM", // Transport/Telecom
        "BIMAS", "MGROS", "SOKM", // Retail
        "ARCLK", "KCHOL", "SAHOL", "EREGL", // Manufacturing
        "ULKER", "FROTO", "CCOLA", "AEFES" // Food & Beverage
    );

    public DataSeedingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Seed data on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // Run after other initializations
    public void onApplicationReady() {
        if (!seedingEnabled) {
            logger.info("Data seeding is disabled");
            return;
        }

        if ("production".equals(activeProfile)) {
            logger.warn("Skipping data seeding in production environment");
            return;
        }

        logger.info("Starting data seeding process...");

        try {
            if (marketDataSeedingEnabled) {
                seedMarketData();
            }

            seedAnalyticsData();
            logger.info("Data seeding completed successfully");
        } catch (Exception e) {
            logger.error("Data seeding failed", e);
        }
    }

    /**
     * Seed realistic market data for major symbols
     */
    @Transactional
    public void seedMarketData() {
        logger.info("Seeding market data for {} symbols over {} days", MAJOR_SYMBOLS.size(), marketDataDays);

        for (String symbol : MAJOR_SYMBOLS) {
            try {
                seedSymbolMarketData(symbol);
                logger.debug("Seeded market data for symbol: {}", symbol);
            } catch (Exception e) {
                logger.error("Failed to seed market data for symbol: {}", symbol, e);
            }
        }

        logger.info("Market data seeding completed");
    }

    /**
     * Seed market data for a specific symbol
     */
    private void seedSymbolMarketData(String symbol) {
        // Get symbol ID
        String symbolId = jdbcTemplate.queryForObject(
            "SELECT symbol_id FROM reference.symbols WHERE symbol = ? AND market_id = (SELECT market_id FROM reference.markets WHERE market_code = 'BIST')",
            String.class, symbol
        );

        if (symbolId == null) {
            logger.warn("Symbol {} not found, skipping market data seeding", symbol);
            return;
        }

        // Get base price for the symbol (realistic Turkish stock prices)
        BigDecimal basePrice = getBasePriceForSymbol(symbol);

        LocalDateTime startTime = LocalDateTime.now().minusDays(marketDataDays);

        // Generate daily OHLCV data first
        for (int day = 0; day < marketDataDays; day++) {
            LocalDate tradingDate = startTime.plusDays(day).toLocalDate();

            // Skip weekends
            if (tradingDate.getDayOfWeek().getValue() >= 6) {
                continue;
            }

            seedDailyOHLCV(symbolId, tradingDate, basePrice, day);
        }

        // Generate intraday tick data for recent days (last 7 days)
        for (int day = Math.max(0, marketDataDays - 7); day < marketDataDays; day++) {
            LocalDate tradingDate = startTime.plusDays(day).toLocalDate();

            if (tradingDate.getDayOfWeek().getValue() >= 6) {
                continue;
            }

            seedIntradayTicks(symbolId, tradingDate, basePrice, day);
        }
    }

    /**
     * Get realistic base price for Turkish stocks
     */
    private BigDecimal getBasePriceForSymbol(String symbol) {
        return switch (symbol) {
            // Banking stocks (typically 15-30 TRY)
            case "AKBNK", "GARAN", "ISCTR", "YKBNK" -> BigDecimal.valueOf(15 + random.nextDouble() * 15);
            case "HALKB", "VAKBN" -> BigDecimal.valueOf(8 + random.nextDouble() * 12);

            // Energy stocks (typically 100-200 TRY)
            case "TUPRS" -> BigDecimal.valueOf(140 + random.nextDouble() * 60);
            case "PETKM" -> BigDecimal.valueOf(8 + random.nextDouble() * 12);
            case "AKSEN", "ZOREN" -> BigDecimal.valueOf(15 + random.nextDouble() * 25);

            // Technology stocks (typically 30-100 TRY)
            case "ASELS" -> BigDecimal.valueOf(65 + random.nextDouble() * 35);
            case "LOGO" -> BigDecimal.valueOf(25 + random.nextDouble() * 15);
            case "NETAS", "ARMDA" -> BigDecimal.valueOf(10 + random.nextDouble() * 20);

            // Transport/Telecom
            case "THYAO" -> BigDecimal.valueOf(80 + random.nextDouble() * 40);
            case "TTKOM" -> BigDecimal.valueOf(12 + random.nextDouble() * 8);

            // Retail
            case "BIMAS" -> BigDecimal.valueOf(160 + random.nextDouble() * 40);
            case "MGROS" -> BigDecimal.valueOf(25 + random.nextDouble() * 15);
            case "SOKM" -> BigDecimal.valueOf(8 + random.nextDouble() * 7);

            // Manufacturing
            case "ARCLK" -> BigDecimal.valueOf(40 + random.nextDouble() * 20);
            case "KCHOL" -> BigDecimal.valueOf(22 + random.nextDouble() * 12);
            case "SAHOL" -> BigDecimal.valueOf(32 + random.nextDouble() * 18);
            case "EREGL" -> BigDecimal.valueOf(35 + random.nextDouble() * 20);

            // Food & Beverage
            case "ULKER" -> BigDecimal.valueOf(18 + random.nextDouble() * 12);
            case "FROTO" -> BigDecimal.valueOf(450 + random.nextDouble() * 200);
            case "CCOLA" -> BigDecimal.valueOf(90 + random.nextDouble() * 40);
            case "AEFES" -> BigDecimal.valueOf(75 + random.nextDouble() * 35);

            default -> BigDecimal.valueOf(20 + random.nextDouble() * 30);
        };
    }

    /**
     * Seed daily OHLCV data
     */
    private void seedDailyOHLCV(String symbolId, LocalDate date, BigDecimal basePrice, int dayOffset) {
        // Apply random walk to base price
        double volatilityFactor = 0.02; // 2% daily volatility
        double priceChange = (random.nextGaussian() * volatilityFactor);
        BigDecimal dayPrice = basePrice.multiply(BigDecimal.valueOf(1 + priceChange * dayOffset * 0.1));

        // Generate OHLC with realistic relationships
        BigDecimal open = dayPrice.multiply(BigDecimal.valueOf(0.98 + random.nextDouble() * 0.04));
        BigDecimal close = dayPrice.multiply(BigDecimal.valueOf(0.98 + random.nextDouble() * 0.04));
        BigDecimal high = dayPrice.multiply(BigDecimal.valueOf(1.00 + random.nextDouble() * 0.05));
        BigDecimal low = dayPrice.multiply(BigDecimal.valueOf(0.95 + random.nextDouble() * 0.03));

        // Ensure OHLC relationships
        high = high.max(open).max(close);
        low = low.min(open).min(close);

        // Generate volume (realistic Turkish stock volumes)
        int volume = (int) (100000 + random.nextDouble() * 1000000);
        int tradeCount = (int) (100 + random.nextDouble() * 500);

        // Calculate VWAP
        BigDecimal vwap = (high.add(low).add(close)).divide(BigDecimal.valueOf(3), 6, BigDecimal.ROUND_HALF_UP);

        jdbcTemplate.update("""
            INSERT INTO market_data.ohlcv_daily (
                time, symbol_id, open_price, high_price, low_price, close_price,
                volume, trade_count, vwap, session_type, is_trading_day
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, ?, 'REGULAR', true)
            ON CONFLICT (symbol_id, time) DO NOTHING
            """,
            date.atTime(18, 0), symbolId, open, high, low, close,
            volume, tradeCount, vwap
        );
    }

    /**
     * Seed intraday tick data
     */
    private void seedIntradayTicks(String symbolId, LocalDate date, BigDecimal basePrice, int dayOffset) {
        LocalDateTime tradingStart = date.atTime(9, 30);
        LocalDateTime tradingEnd = date.atTime(18, 0);

        BigDecimal currentPrice = basePrice.multiply(BigDecimal.valueOf(1 + (random.nextGaussian() * 0.02 * dayOffset * 0.1)));

        // Generate ticks throughout the trading day
        for (int i = 0; i < ticksPerDay; i++) {
            // Random time during trading hours
            long tradingMinutes = java.time.Duration.between(tradingStart, tradingEnd).toMinutes();
            LocalDateTime tickTime = tradingStart.plusMinutes((long) (random.nextDouble() * tradingMinutes));

            // Price walk (small random changes)
            double priceChange = random.nextGaussian() * 0.001; // 0.1% tick volatility
            currentPrice = currentPrice.multiply(BigDecimal.valueOf(1 + priceChange));

            // Ensure price doesn't go negative
            currentPrice = currentPrice.max(BigDecimal.valueOf(0.01));

            // Random quantity (typical Turkish stock trade sizes)
            int quantity = (random.nextInt(10) + 1) * 100; // Multiples of 100

            // Generate bid/ask spread
            BigDecimal spread = currentPrice.multiply(BigDecimal.valueOf(0.001 + random.nextDouble() * 0.002));
            BigDecimal bidPrice = currentPrice.subtract(spread.divide(BigDecimal.valueOf(2)));
            BigDecimal askPrice = currentPrice.add(spread.divide(BigDecimal.valueOf(2)));

            jdbcTemplate.update("""
                INSERT INTO market_data.market_ticks (
                    time, symbol_id, price, quantity, bid_price, ask_price,
                    bid_size, ask_size, trade_type, data_source, quality_score
                ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, 'REGULAR', 'BIST', 1.0)
                """,
                tickTime, symbolId, currentPrice, quantity, bidPrice, askPrice,
                quantity * 2, quantity * 3
            );

            // Occasionally insert order book snapshots
            if (i % 10 == 0) {
                seedOrderBookSnapshot(symbolId, tickTime, bidPrice, askPrice, quantity);
            }
        }
    }

    /**
     * Seed order book snapshot
     */
    private void seedOrderBookSnapshot(String symbolId, LocalDateTime time,
                                     BigDecimal bidPrice, BigDecimal askPrice, int baseSize) {
        jdbcTemplate.update("""
            INSERT INTO market_data.order_book_snapshots (
                time, symbol_id, best_bid, best_ask, bid_size, ask_size,
                total_bid_volume, total_ask_volume, data_source
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, 'BIST')
            """,
            time, symbolId, bidPrice, askPrice, baseSize * 2, baseSize * 3,
            baseSize * 10, baseSize * 8
        );
    }

    /**
     * Seed analytics data (technical indicators, market stats)
     */
    @Transactional
    public void seedAnalyticsData() {
        logger.info("Seeding analytics data...");

        try {
            // Seed market statistics
            seedMarketStatistics();

            // Seed technical indicators for major symbols
            for (String symbol : MAJOR_SYMBOLS.subList(0, 5)) { // Limit to first 5 for performance
                seedTechnicalIndicators(symbol);
            }

        } catch (Exception e) {
            logger.error("Failed to seed analytics data", e);
        }

        logger.info("Analytics data seeding completed");
    }

    /**
     * Seed market statistics
     */
    private void seedMarketStatistics() {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 30; i++) {
            LocalDateTime statTime = now.minusDays(i);

            jdbcTemplate.update("""
                INSERT INTO analytics.market_stats (
                    time, calculation_type, total_volume, total_trades,
                    advancing_issues, declining_issues, unchanged_issues,
                    avg_price_change, median_price_change, volatility_index
                ) VALUES (?, 'daily_summary', ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                statTime,
                (long) (5000000 + random.nextDouble() * 10000000), // Total volume
                (int) (10000 + random.nextDouble() * 20000), // Total trades
                (int) (50 + random.nextDouble() * 100), // Advancing
                (int) (40 + random.nextDouble() * 80), // Declining
                (int) (10 + random.nextDouble() * 20), // Unchanged
                random.nextGaussian() * 2.0, // Avg price change
                random.nextGaussian() * 1.5, // Median price change
                15 + random.nextDouble() * 10 // Volatility index
            );
        }
    }

    /**
     * Seed technical indicators for a symbol
     */
    private void seedTechnicalIndicators(String symbol) {
        String symbolId = jdbcTemplate.queryForObject(
            "SELECT symbol_id FROM reference.symbols WHERE symbol = ? AND market_id = (SELECT market_id FROM reference.markets WHERE market_code = 'BIST')",
            String.class, symbol
        );

        if (symbolId == null) return;

        LocalDateTime now = LocalDateTime.now();

        // Generate indicators for different timeframes
        String[] timeframes = {"1d", "1h", "5m"};

        for (String timeframe : timeframes) {
            for (int i = 0; i < 30; i++) {
                LocalDateTime indicatorTime = now.minusDays(i);

                // Generate realistic technical indicator values
                double basePrice = 50 + random.nextDouble() * 100;

                jdbcTemplate.update("""
                    INSERT INTO analytics.technical_indicators (
                        time, symbol_id, timeframe,
                        sma_20, sma_50, ema_12, ema_26,
                        macd_line, macd_signal, macd_histogram,
                        rsi_14, bb_upper, bb_middle, bb_lower,
                        atr_14, volatility_20d
                    ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT DO NOTHING
                    """,
                    indicatorTime, symbolId, timeframe,
                    basePrice + random.nextGaussian() * 2, // SMA 20
                    basePrice + random.nextGaussian() * 3, // SMA 50
                    basePrice + random.nextGaussian() * 1.5, // EMA 12
                    basePrice + random.nextGaussian() * 2, // EMA 26
                    random.nextGaussian() * 0.5, // MACD line
                    random.nextGaussian() * 0.3, // MACD signal
                    random.nextGaussian() * 0.2, // MACD histogram
                    30 + random.nextDouble() * 40, // RSI (30-70 range)
                    basePrice + 2, // BB upper
                    basePrice, // BB middle
                    basePrice - 2, // BB lower
                    1 + random.nextDouble() * 3, // ATR
                    0.15 + random.nextDouble() * 0.1 // 20-day volatility
                );
            }
        }
    }

    /**
     * Clean up old seeded data (for development/testing)
     */
    public void cleanupSeedData() {
        if ("production".equals(activeProfile)) {
            logger.warn("Cleanup not allowed in production environment");
            return;
        }

        logger.info("Cleaning up seeded market data...");

        jdbcTemplate.update("TRUNCATE TABLE market_data.market_ticks RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE market_data.order_book_snapshots RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE market_data.ohlcv_daily RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE analytics.technical_indicators RESTART IDENTITY CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE analytics.market_stats RESTART IDENTITY CASCADE");

        logger.info("Seeded data cleanup completed");
    }
}