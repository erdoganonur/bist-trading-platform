package com.bisttrading.marketdata.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Order book snapshot verilerini TimescaleDB'de saklayan entity
 * Enhanced entity with comprehensive order book metrics
 * Hypertable: order_book_snapshots (time partitioned by 'time')
 */
@Entity
@Table(name = "order_book_snapshots", indexes = {
    @Index(name = "idx_order_book_symbol_time", columnList = "symbol, time"),
    @Index(name = "idx_order_book_time", columnList = "time"),
    @Index(name = "idx_order_book_symbol", columnList = "symbol"),
    @Index(name = "idx_order_book_spread", columnList = "bidAskSpread"),
    @Index(name = "idx_order_book_imbalance", columnList = "imbalanceRatio")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"symbol", "time"})
public class OrderBookSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime time;

    @Column(nullable = false, length = 20)
    private String symbol;

    // Order book data (JSONB for better performance and querying)
    @Column(name = "bids", columnDefinition = "JSONB")
    private String bids; // JSON array: [{"price": 15.45, "quantity": 100, "orders": 5}, ...]

    @Column(name = "asks", columnDefinition = "JSONB")
    private String asks; // JSON array: [{"price": 15.50, "quantity": 200, "orders": 3}, ...]

    // Order book metrics
    @Column(name = "bid_count")
    private Integer bidCount;

    @Column(name = "ask_count")
    private Integer askCount;

    @Column(name = "total_bid_volume", precision = 18, scale = 0)
    private Long totalBidVolume;

    @Column(name = "total_ask_volume", precision = 18, scale = 0)
    private Long totalAskVolume;

    // Best bid/ask prices
    @Column(name = "best_bid", precision = 18, scale = 8)
    private BigDecimal bestBid;

    @Column(name = "best_ask", precision = 18, scale = 8)
    private BigDecimal bestAsk;

    @Column(name = "best_bid_size", precision = 18, scale = 0)
    private Long bestBidSize;

    @Column(name = "best_ask_size", precision = 18, scale = 0)
    private Long bestAskSize;

    // Calculated metrics
    @Column(name = "bid_ask_spread", precision = 18, scale = 8)
    private BigDecimal bidAskSpread; // askPrice - bidPrice

    @Column(name = "mid_price", precision = 18, scale = 8)
    private BigDecimal midPrice; // (bestBid + bestAsk) / 2

    @Column(name = "imbalance_ratio", precision = 18, scale = 8)
    private BigDecimal imbalanceRatio; // bidVolume / (bidVolume + askVolume)

    @Column(name = "spread_percentage", precision = 18, scale = 8)
    private BigDecimal spreadPercentage; // (spread / midPrice) * 100

    // Depth metrics (5, 10, 20 levels)
    @Column(name = "bid_volume_5", precision = 18, scale = 0)
    private Long bidVolume5; // İlk 5 seviye bid volume

    @Column(name = "ask_volume_5", precision = 18, scale = 0)
    private Long askVolume5; // İlk 5 seviye ask volume

    @Column(name = "bid_volume_10", precision = 18, scale = 0)
    private Long bidVolume10;

    @Column(name = "ask_volume_10", precision = 18, scale = 0)
    private Long askVolume10;

    @Column(name = "bid_volume_20", precision = 18, scale = 0)
    private Long bidVolume20;

    @Column(name = "ask_volume_20", precision = 18, scale = 0)
    private Long askVolume20;

    // Liquidity metrics
    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "average_order_size", precision = 18, scale = 8)
    private BigDecimal averageOrderSize;

    // Market microstructure
    @Column(name = "tick_direction") // 1: uptick, -1: downtick, 0: no change
    private Integer tickDirection;

    @Column(name = "last_update_type", length = 10) // INSERT, UPDATE, DELETE
    private String lastUpdateType;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (time == null) {
            time = now;
        }

        // Calculate derived metrics
        calculateDerivedMetrics();
    }

    /**
     * Derived metrics hesaplama
     */
    private void calculateDerivedMetrics() {
        // Bid-Ask Spread
        if (bestBid != null && bestAsk != null) {
            bidAskSpread = bestAsk.subtract(bestBid);

            // Mid Price
            midPrice = bestBid.add(bestAsk).divide(BigDecimal.valueOf(2));

            // Spread Percentage
            if (midPrice.compareTo(BigDecimal.ZERO) > 0) {
                spreadPercentage = bidAskSpread.divide(midPrice, 8, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }

        // Imbalance Ratio
        if (totalBidVolume != null && totalAskVolume != null &&
            (totalBidVolume + totalAskVolume) > 0) {
            long totalVolume = totalBidVolume + totalAskVolume;
            imbalanceRatio = BigDecimal.valueOf(totalBidVolume)
                .divide(BigDecimal.valueOf(totalVolume), 8, BigDecimal.ROUND_HALF_UP);
        }

        // Average Order Size
        if (totalOrders != null && totalOrders > 0 &&
            totalBidVolume != null && totalAskVolume != null) {
            long totalVol = totalBidVolume + totalAskVolume;
            averageOrderSize = BigDecimal.valueOf(totalVol)
                .divide(BigDecimal.valueOf(totalOrders), 8, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Builder pattern için convenience constructor
     */
    public static OrderBookSnapshot builder() {
        return new OrderBookSnapshot();
    }

    public OrderBookSnapshot symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public OrderBookSnapshot bids(String bids) {
        this.bids = bids;
        return this;
    }

    public OrderBookSnapshot asks(String asks) {
        this.asks = asks;
        return this;
    }

    public OrderBookSnapshot bidCount(Integer bidCount) {
        this.bidCount = bidCount;
        return this;
    }

    public OrderBookSnapshot askCount(Integer askCount) {
        this.askCount = askCount;
        return this;
    }

}