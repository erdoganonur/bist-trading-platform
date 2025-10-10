package com.bisttrading.broker.algolab.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Real-time tick data from AlgoLab WebSocket.
 * Represents a single price update for a symbol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickData {

    /**
     * Symbol code (e.g., "AKBNK", "THYAO")
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Last traded price
     */
    @JsonProperty("last")
    private BigDecimal lastPrice;

    /**
     * Last trade volume
     */
    @JsonProperty("lastVolume")
    private Long lastVolume;

    /**
     * Best bid price
     */
    @JsonProperty("bid")
    private BigDecimal bidPrice;

    /**
     * Best ask price
     */
    @JsonProperty("ask")
    private BigDecimal askPrice;

    /**
     * Bid size
     */
    @JsonProperty("bidSize")
    private Long bidSize;

    /**
     * Ask size
     */
    @JsonProperty("askSize")
    private Long askSize;

    /**
     * Daily high price
     */
    @JsonProperty("high")
    private BigDecimal highPrice;

    /**
     * Daily low price
     */
    @JsonProperty("low")
    private BigDecimal lowPrice;

    /**
     * Opening price
     */
    @JsonProperty("open")
    private BigDecimal openPrice;

    /**
     * Previous close price
     */
    @JsonProperty("close")
    private BigDecimal previousClose;

    /**
     * Total volume for the day
     */
    @JsonProperty("volume")
    private Long totalVolume;

    /**
     * Price change from previous close
     */
    @JsonProperty("change")
    private BigDecimal change;

    /**
     * Percentage change from previous close
     */
    @JsonProperty("changePercent")
    private BigDecimal changePercent;

    /**
     * Timestamp of the tick
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Market status (OPEN, CLOSED, PRE_OPEN, etc.)
     */
    @JsonProperty("marketStatus")
    private String marketStatus;
}
