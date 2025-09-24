package com.bisttrading.marketdata.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Market tick verilerini TimescaleDB'de saklayan entity
 * Enhanced entity with comprehensive market data fields
 * Hypertable: market_ticks (time partitioned by 'time')
 */
@Entity
@Table(name = "market_ticks", indexes = {
    @Index(name = "idx_market_ticks_symbol_time", columnList = "symbol, time"),
    @Index(name = "idx_market_ticks_time", columnList = "time"),
    @Index(name = "idx_market_ticks_symbol", columnList = "symbol"),
    @Index(name = "idx_market_ticks_total_volume", columnList = "totalVolume"),
    @Index(name = "idx_market_ticks_price", columnList = "price")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"symbol", "time"})
public class MarketTick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime time;

    @Column(nullable = false, length = 20)
    private String symbol;

    // Current price information
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private Long quantity;

    // Bid/Ask information
    @Column(precision = 18, scale = 8)
    private BigDecimal bid;

    @Column(precision = 18, scale = 8)
    private BigDecimal ask;

    @Column(name = "bid_size")
    private Long bidSize;

    @Column(name = "ask_size")
    private Long askSize;

    // Volume and trade information
    @Column(name = "total_volume", nullable = false)
    private Long totalVolume;

    @Column(name = "total_trades", nullable = false)
    private Long totalTrades;

    @Column(precision = 18, scale = 8)
    private BigDecimal vwap; // Volume Weighted Average Price

    // Trade details
    @Column(length = 10)
    private String direction; // BUY/SELL

    @Column(precision = 18, scale = 8)
    private BigDecimal value;

    @Column(length = 100)
    private String buyer;

    @Column(length = 100)
    private String seller;

    @Column(name = "trade_time", length = 50)
    private String tradeTime;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (time == null) {
            time = OffsetDateTime.now();
        }
        if (totalVolume == null) {
            totalVolume = quantity != null ? quantity : 0L;
        }
        if (totalTrades == null) {
            totalTrades = 1L;
        }
    }

    /**
     * Builder pattern için convenience constructor
     */
    public static MarketTick builder() {
        return new MarketTick();
    }

    public MarketTick symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public MarketTick price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public MarketTick quantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }

    public MarketTick direction(String direction) {
        this.direction = direction;
        return this;
    }

    public MarketTick value(BigDecimal value) {
        this.value = value;
        return this;
    }

    public MarketTick buyer(String buyer) {
        this.buyer = buyer;
        return this;
    }

    public MarketTick seller(String seller) {
        this.seller = seller;
        return this;
    }

    public MarketTick tradeTime(String tradeTime) {
        this.tradeTime = tradeTime;
        return this;
    }

}