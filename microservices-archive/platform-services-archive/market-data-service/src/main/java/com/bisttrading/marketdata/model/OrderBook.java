package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order book data for a symbol
 */
@Data
@Builder
public class OrderBook {

    private String symbol;
    private List<OrderBookEntry> bids;
    private List<OrderBookEntry> asks;
    private LocalDateTime timestamp;
    private int depth;

    @Data
    @Builder
    public static class OrderBookEntry {
        private BigDecimal price;
        private BigDecimal quantity;
        private int orderCount;

        public BigDecimal getValue() {
            if (price != null && quantity != null) {
                return price.multiply(quantity);
            }
            return BigDecimal.ZERO;
        }
    }

    // Helper methods
    public BigDecimal getBestBidPrice() {
        if (bids != null && !bids.isEmpty()) {
            return bids.get(0).getPrice();
        }
        return null;
    }

    public BigDecimal getBestAskPrice() {
        if (asks != null && !asks.isEmpty()) {
            return asks.get(0).getPrice();
        }
        return null;
    }

    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBidPrice();
        BigDecimal bestAsk = getBestAskPrice();
        if (bestBid != null && bestAsk != null) {
            return bestAsk.subtract(bestBid);
        }
        return null;
    }

    public BigDecimal getMidPrice() {
        BigDecimal bestBid = getBestBidPrice();
        BigDecimal bestAsk = getBestAskPrice();
        if (bestBid != null && bestAsk != null) {
            return bestBid.add(bestAsk).divide(BigDecimal.valueOf(2), 4, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    public boolean isValid() {
        return symbol != null && timestamp != null &&
               bids != null && !bids.isEmpty() &&
               asks != null && !asks.isEmpty();
    }
}