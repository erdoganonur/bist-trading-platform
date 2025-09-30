package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order request for AlgoLab API
 */
@Data
@Builder
@Jacksonized
public class OrderRequest {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("side")
    private OrderSide side;

    @JsonProperty("type")
    private OrderType type;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("stop_price")
    private BigDecimal stopPrice;

    @JsonProperty("time_in_force")
    private TimeInForce timeInForce;

    @JsonProperty("client_order_id")
    private String clientOrderId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("good_till_date")
    private Instant goodTillDate;

    @JsonProperty("iceberg_qty")
    private Integer icebergQuantity;

    @JsonProperty("instruction")
    private OrderInstruction instruction;

    @JsonProperty("margin_buy")
    private Boolean marginBuy;

    @JsonProperty("short_sale")
    private Boolean shortSale;

    // Validation methods

    /**
     * Validate order request
     */
    public void validate() {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        if (side == null) {
            throw new IllegalArgumentException("Side is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Order type is required");
        }

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate price for limit orders
        if ((type == OrderType.LIMIT || type == OrderType.STOP_LIMIT) &&
            (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Price is required for limit orders");
        }

        // Validate stop price for stop orders
        if ((type == OrderType.STOP || type == OrderType.STOP_LIMIT) &&
            (stopPrice == null || stopPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Stop price is required for stop orders");
        }

        // Default time in force
        if (timeInForce == null) {
            timeInForce = TimeInForce.DAY;
        }
    }

    /**
     * Check if this is a buy order
     */
    public boolean isBuyOrder() {
        return side == OrderSide.BUY;
    }

    /**
     * Check if this is a sell order
     */
    public boolean isSellOrder() {
        return side == OrderSide.SELL;
    }

    /**
     * Check if this is a market order
     */
    public boolean isMarketOrder() {
        return type == OrderType.MARKET;
    }

    /**
     * Check if this is a limit order
     */
    public boolean isLimitOrder() {
        return type == OrderType.LIMIT;
    }

    /**
     * Calculate estimated order value
     */
    public BigDecimal getEstimatedValue() {
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return null;
    }
}