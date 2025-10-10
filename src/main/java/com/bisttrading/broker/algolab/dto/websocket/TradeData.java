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
 * Real-time trade data from AlgoLab WebSocket.
 * Represents individual executed trades.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeData {

    /**
     * Symbol code
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Trade ID (unique identifier)
     */
    @JsonProperty("tradeId")
    private String tradeId;

    /**
     * Trade price
     */
    @JsonProperty("price")
    private BigDecimal price;

    /**
     * Trade quantity
     */
    @JsonProperty("quantity")
    private Long quantity;

    /**
     * Trade side (BUY or SELL from taker's perspective)
     */
    @JsonProperty("side")
    private String side;

    /**
     * Trade timestamp
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Is this trade a buyer-initiated trade (aggressive buy)
     */
    @JsonProperty("isBuyerMaker")
    private Boolean isBuyerMaker;

    /**
     * Trade amount (price * quantity)
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Sequence number for ordering trades
     */
    @JsonProperty("sequence")
    private Long sequence;

    /**
     * Calculates trade amount if not provided
     */
    public BigDecimal calculateAmount() {
        if (amount != null) {
            return amount;
        }
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
