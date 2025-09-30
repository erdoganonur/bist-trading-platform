package com.bisttrading.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AlgoLab WebSocket mesajları için base class
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketMessage {

    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private String content;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Tick data mesajı (Type: "T")
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TickData {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("price")
        private BigDecimal price;

        @JsonProperty("volume")
        private Long volume;

        @JsonProperty("direction")
        private String direction; // BUY/SELL

        @JsonProperty("time")
        private String time;

        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("buyer")
        private String buyer;

        @JsonProperty("seller")
        private String seller;
    }

    /**
     * Depth data mesajı (Type: "D")
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepthData {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("bids")
        private BookEntry[] bids;

        @JsonProperty("asks")
        private BookEntry[] asks;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
    }

    /**
     * Order book entry
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookEntry {
        @JsonProperty("price")
        private BigDecimal price;

        @JsonProperty("quantity")
        private Long quantity;

        @JsonProperty("orders")
        private Integer orders;
    }

    /**
     * Order status mesajı (Type: "O")
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderStatus {
        @JsonProperty("orderId")
        private String orderId;

        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("status")
        private Integer status;

        @JsonProperty("statusText")
        private String statusText;

        @JsonProperty("quantity")
        private Long quantity;

        @JsonProperty("filledQuantity")
        private Long filledQuantity;

        @JsonProperty("price")
        private BigDecimal price;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
    }

    /**
     * Heartbeat mesajı (Type: "H")
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Heartbeat {
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        @JsonProperty("status")
        private String status = "alive";
    }

    /**
     * Subscription request mesajı
     */
    @Data
    public static class SubscriptionRequest {
        @JsonProperty("token")
        private String token;

        @JsonProperty("Type")
        private String type;

        @JsonProperty("Symbols")
        private String[] symbols;
    }
}