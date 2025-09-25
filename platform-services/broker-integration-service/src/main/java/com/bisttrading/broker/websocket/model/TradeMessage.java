package com.bisttrading.broker.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
public class TradeMessage extends WebSocketMessage {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("trade_id")
    private String tradeId;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("side")
    private String side; // "BUY" or "SELL"

    @JsonProperty("trade_time")
    private Instant tradeTime;

    @JsonProperty("value")
    private BigDecimal value;
}