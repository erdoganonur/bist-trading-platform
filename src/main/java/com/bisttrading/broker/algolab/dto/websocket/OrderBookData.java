package com.bisttrading.broker.algolab.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Real-time order book (Level 2) data from AlgoLab WebSocket.
 * Contains bid and ask levels with price and quantity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBookData {

    /**
     * Symbol code
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Bid levels (sorted by price descending)
     */
    @JsonProperty("bids")
    private List<OrderBookLevel> bids;

    /**
     * Ask levels (sorted by price ascending)
     */
    @JsonProperty("asks")
    private List<OrderBookLevel> asks;

    /**
     * Timestamp of the order book snapshot
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Sequence number for ordering updates
     */
    @JsonProperty("sequence")
    private Long sequence;

    /**
     * Represents a single level in the order book
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderBookLevel {

        /**
         * Price level
         */
        @JsonProperty("price")
        private BigDecimal price;

        /**
         * Total quantity at this price level
         */
        @JsonProperty("quantity")
        private Long quantity;

        /**
         * Number of orders at this level
         */
        @JsonProperty("orderCount")
        private Integer orderCount;

        /**
         * Side (BID or ASK)
         */
        @JsonProperty("side")
        private String side;
    }

    /**
     * Calculates bid-ask spread
     */
    public BigDecimal getSpread() {
        if (asks == null || asks.isEmpty() || bids == null || bids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal bestAsk = asks.get(0).getPrice();
        BigDecimal bestBid = bids.get(0).getPrice();
        return bestAsk.subtract(bestBid);
    }

    /**
     * Calculates mid price (average of best bid and ask)
     */
    public BigDecimal getMidPrice() {
        if (asks == null || asks.isEmpty() || bids == null || bids.isEmpty()) {
            return null;
        }
        BigDecimal bestAsk = asks.get(0).getPrice();
        BigDecimal bestBid = bids.get(0).getPrice();
        return bestAsk.add(bestBid).divide(BigDecimal.valueOf(2));
    }
}
