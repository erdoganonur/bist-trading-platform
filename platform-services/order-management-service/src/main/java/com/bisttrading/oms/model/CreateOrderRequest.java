package com.bisttrading.oms.model;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request for creating a new order
 */
@Data
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @Size(max = 50, message = "Client order ID cannot exceed 50 characters")
    private String clientOrderId;

    @NotBlank(message = "Symbol cannot be blank")
    @Size(max = 20, message = "Symbol cannot exceed 20 characters")
    private String symbol;

    @NotNull(message = "Order side is required")
    private OMSOrder.OrderSide side;

    @NotNull(message = "Order type is required")
    private OMSOrder.OrderType type;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Stop price must be greater than 0")
    private BigDecimal stopPrice;

    @NotNull(message = "Time in force is required")
    private OMSOrder.TimeInForce timeInForce;

    private LocalDateTime expireTime;

    @Size(max = 50, message = "Account ID cannot exceed 50 characters")
    private String accountId;

    @Size(max = 50, message = "Portfolio ID cannot exceed 50 characters")
    private String portfolioId;

    @Size(max = 50, message = "Strategy ID cannot exceed 50 characters")
    private String strategyId;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    // Validation methods
    public boolean isMarketOrder() {
        return type == OMSOrder.OrderType.MARKET;
    }

    public boolean isLimitOrder() {
        return type == OMSOrder.OrderType.LIMIT;
    }

    public boolean isStopOrder() {
        return type == OMSOrder.OrderType.STOP || type == OMSOrder.OrderType.STOP_LIMIT;
    }

    public boolean requiresPrice() {
        return type == OMSOrder.OrderType.LIMIT || type == OMSOrder.OrderType.STOP_LIMIT;
    }

    public boolean requiresStopPrice() {
        return type == OMSOrder.OrderType.STOP || type == OMSOrder.OrderType.STOP_LIMIT;
    }
}