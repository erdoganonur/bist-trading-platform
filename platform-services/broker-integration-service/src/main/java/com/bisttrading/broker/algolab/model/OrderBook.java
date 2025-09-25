package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@Jacksonized
public class OrderBook {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("bids")
    private List<OrderBookLevel> bids;

    @JsonProperty("asks")
    private List<OrderBookLevel> asks;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("sequence")
    private Long sequence;

    @Data
    @Builder
    @Jacksonized
    public static class OrderBookLevel {
        @JsonProperty("price")
        private BigDecimal price;

        @JsonProperty("size")
        private Integer size;

        @JsonProperty("count")
        private Integer count;
    }

    public BigDecimal getBestBid() {
        return bids != null && !bids.isEmpty() ? bids.get(0).getPrice() : null;
    }

    public BigDecimal getBestAsk() {
        return asks != null && !asks.isEmpty() ? asks.get(0).getPrice() : null;
    }

    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();
        if (bestBid != null && bestAsk != null) {
            return bestAsk.subtract(bestBid);
        }
        return null;
    }

    public BigDecimal getMidPrice() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();
        if (bestBid != null && bestAsk != null) {
            return bestBid.add(bestAsk).divide(BigDecimal.valueOf(2), BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    public Integer getTotalBidSize() {
        return bids != null ? bids.stream().mapToInt(OrderBookLevel::getSize).sum() : 0;
    }

    public Integer getTotalAskSize() {
        return asks != null ? asks.stream().mapToInt(OrderBookLevel::getSize).sum() : 0;
    }
}