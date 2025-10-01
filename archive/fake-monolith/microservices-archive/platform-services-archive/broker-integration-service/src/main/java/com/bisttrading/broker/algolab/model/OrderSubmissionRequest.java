package com.bisttrading.broker.algolab.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderSubmissionRequest {

    private String userId;
    private String accountId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private TimeInForce timeInForce;
    private Instant goodTillDate;
    private Integer icebergQuantity;
    private OrderInstruction instruction;
    private Boolean marginBuy;
    private Boolean shortSale;

    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        if (side == null) {
            throw new IllegalArgumentException("Order side is required");
        }

        if (orderType == null) {
            throw new IllegalArgumentException("Order type is required");
        }

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate price requirements based on order type
        if (orderType.requiresPrice() && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Price is required for " + orderType + " orders");
        }

        if (orderType.requiresStopPrice() && (stopPrice == null || stopPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Stop price is required for " + orderType + " orders");
        }

        // Default time in force if not specified
        if (timeInForce == null) {
            timeInForce = TimeInForce.DAY;
        }

        // Validate GTD requires expiration date
        if (timeInForce == TimeInForce.GTD && goodTillDate == null) {
            throw new IllegalArgumentException("Good till date is required for GTD orders");
        }
    }

    public boolean isBuyOrder() {
        return side == OrderSide.BUY;
    }

    public boolean isSellOrder() {
        return side == OrderSide.SELL;
    }

    public boolean isMarketOrder() {
        return orderType == OrderType.MARKET;
    }

    public boolean isLimitOrder() {
        return orderType == OrderType.LIMIT;
    }

    public BigDecimal getEstimatedValue() {
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return null;
    }
}