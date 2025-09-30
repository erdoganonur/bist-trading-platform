package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@Jacksonized
public class OrderResponse {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("client_order_id")
    private String clientOrderId;

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

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("time_in_force")
    private TimeInForce timeInForce;

    @JsonProperty("filled_quantity")
    private Integer filledQuantity;

    @JsonProperty("remaining_quantity")
    private Integer remainingQuantity;

    @JsonProperty("average_price")
    private BigDecimal averagePrice;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("instruction")
    private OrderInstruction instruction;

    @JsonProperty("commission")
    private BigDecimal commission;

    @JsonProperty("reject_reason")
    private String rejectReason;

    @JsonProperty("success")
    private boolean success;

    public boolean isActive() {
        return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED;
    }

    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isRejected() {
        return status == OrderStatus.REJECTED;
    }

    public BigDecimal getTotalValue() {
        if (averagePrice != null && filledQuantity != null && filledQuantity > 0) {
            return averagePrice.multiply(BigDecimal.valueOf(filledQuantity));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getFillPercentage() {
        if (quantity != null && quantity > 0 && filledQuantity != null) {
            return BigDecimal.valueOf(filledQuantity)
                    .divide(BigDecimal.valueOf(quantity), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}