package com.bisttrading.broker.algolab.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderSubmissionRequest {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("side")
    private String side; // BUY, SELL

    @JsonProperty("order_type")
    private String orderType; // MARKET, LIMIT, STOP_LOSS, etc.

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("price")
    private BigDecimal price; // null for market orders

    @JsonProperty("stop_price")
    private BigDecimal stopPrice; // for stop orders

    @JsonProperty("time_in_force")
    private String timeInForce; // DAY, GTC, IOC, FOK

    @JsonProperty("order_id")
    private String orderId; // client order ID

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("notes")
    private String notes;
}