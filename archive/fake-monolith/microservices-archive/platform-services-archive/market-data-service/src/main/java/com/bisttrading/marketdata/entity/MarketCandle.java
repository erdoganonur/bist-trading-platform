package com.bisttrading.marketdata.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Market candle (OHLCV) verilerini TimescaleDB'de saklayan entity
 * Multiple timeframes için candlestick data
 * Hypertable: market_candles (time partitioned by 'time')
 */
@Entity
@Table(name = "market_candles", indexes = {
    @Index(name = "idx_market_candles_symbol_timeframe_time", columnList = "symbol, timeframe, time"),
    @Index(name = "idx_market_candles_time", columnList = "time"),
    @Index(name = "idx_market_candles_symbol", columnList = "symbol"),
    @Index(name = "idx_market_candles_timeframe", columnList = "timeframe"),
    @Index(name = "idx_market_candles_volume", columnList = "volume")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_candle_symbol_timeframe_time", columnNames = {"symbol", "timeframe", "time"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"symbol", "timeframe", "time"})
public class MarketCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime time;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String timeframe; // 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w, 1M

    // OHLCV data
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    // Additional metrics
    @Column(name = "trades_count", nullable = false)
    private Long tradesCount;

    @Column(precision = 18, scale = 8)
    private BigDecimal vwap; // Volume Weighted Average Price

    // Technical indicators
    @Column(precision = 18, scale = 8)
    private BigDecimal ema20; // Exponential Moving Average 20

    @Column(precision = 18, scale = 8)
    private BigDecimal ema50; // Exponential Moving Average 50

    @Column(precision = 18, scale = 8)
    private BigDecimal rsi14; // Relative Strength Index 14

    @Column(precision = 18, scale = 8)
    private BigDecimal sma20; // Simple Moving Average 20

    @Column(precision = 18, scale = 8)
    private BigDecimal sma50; // Simple Moving Average 50

    // Bollinger Bands
    @Column(name = "bb_upper", precision = 18, scale = 8)
    private BigDecimal bollingerUpper;

    @Column(name = "bb_middle", precision = 18, scale = 8)
    private BigDecimal bollingerMiddle;

    @Column(name = "bb_lower", precision = 18, scale = 8)
    private BigDecimal bollingerLower;

    // Additional technical indicators
    @Column(precision = 18, scale = 8)
    private BigDecimal macd; // MACD Line

    @Column(name = "macd_signal", precision = 18, scale = 8)
    private BigDecimal macdSignal; // MACD Signal Line

    @Column(name = "macd_histogram", precision = 18, scale = 8)
    private BigDecimal macdHistogram; // MACD Histogram

    @Column(name = "stochastic_k", precision = 18, scale = 8)
    private BigDecimal stochasticK;

    @Column(name = "stochastic_d", precision = 18, scale = 8)
    private BigDecimal stochasticD;

    // Market strength indicators
    @Column(name = "money_flow", precision = 18, scale = 8)
    private BigDecimal moneyFlow;

    @Column(name = "accumulation_distribution", precision = 18, scale = 8)
    private BigDecimal accumulationDistribution;

    // Metadata
    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete = false; // Candle tamamlandı mı?

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (time == null) {
            time = now;
        }
        if (tradesCount == null) {
            tradesCount = 1L;
        }
        if (isComplete == null) {
            isComplete = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Builder pattern için convenience methods
     */
    public static MarketCandle builder() {
        return new MarketCandle();
    }

    public MarketCandle symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public MarketCandle timeframe(String timeframe) {
        this.timeframe = timeframe;
        return this;
    }

    public MarketCandle time(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    public MarketCandle open(BigDecimal open) {
        this.open = open;
        return this;
    }

    public MarketCandle high(BigDecimal high) {
        this.high = high;
        return this;
    }

    public MarketCandle low(BigDecimal low) {
        this.low = low;
        return this;
    }

    public MarketCandle close(BigDecimal close) {
        this.close = close;
        return this;
    }

    public MarketCandle volume(Long volume) {
        this.volume = volume;
        return this;
    }

    public MarketCandle tradesCount(Long tradesCount) {
        this.tradesCount = tradesCount;
        return this;
    }

    public MarketCandle vwap(BigDecimal vwap) {
        this.vwap = vwap;
        return this;
    }

    public MarketCandle isComplete(Boolean isComplete) {
        this.isComplete = isComplete;
        return this;
    }

    /**
     * Candle duration'u dakika cinsinden döndürür
     */
    public int getDurationInMinutes() {
        return switch (timeframe.toLowerCase()) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "1h" -> 60;
            case "4h" -> 240;
            case "1d" -> 1440;
            case "1w" -> 10080;
            case "1M" -> 43200; // Approximate
            default -> 1;
        };
    }

    /**
     * Price range hesaplar
     */
    public BigDecimal getPriceRange() {
        if (high != null && low != null) {
            return high.subtract(low);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Body (open-close) range hesaplar
     */
    public BigDecimal getBodyRange() {
        if (open != null && close != null) {
            return close.subtract(open).abs();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Bullish/Bearish durumu
     */
    public boolean isBullish() {
        return close != null && open != null && close.compareTo(open) > 0;
    }

    public boolean isBearish() {
        return close != null && open != null && close.compareTo(open) < 0;
    }

    public boolean isDoji() {
        return close != null && open != null && close.compareTo(open) == 0;
    }
}