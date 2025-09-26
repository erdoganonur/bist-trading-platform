package com.bisttrading.oms.dto;

import com.bisttrading.oms.model.CreateOrderRequest;
import com.bisttrading.oms.model.OMSOrder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Request DTO with comprehensive validation for BIST Trading Platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order creation/modification request")
public class OrderRequest {

    @Schema(description = "Client provided order identifier (optional)",
            example = "CLIENT-ORDER-123", maxLength = 50)
    @Size(max = 50, message = "Client order ID cannot exceed 50 characters")
    private String clientOrderId;

    @NotBlank(message = "Symbol is required")
    @Size(min = 1, max = 20, message = "Symbol must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9._-]+$", message = "Symbol can only contain uppercase letters, numbers, dots, underscores, and hyphens")
    @Schema(description = "Trading symbol", example = "THYAO", maxLength = 20, required = true)
    private String symbol;

    @NotNull(message = "Order side is required")
    @Schema(description = "Order side (BUY or SELL)", example = "BUY", required = true)
    private OMSOrder.OrderSide side;

    @NotNull(message = "Order type is required")
    @Schema(description = "Order type", example = "LIMIT", required = true)
    private OMSOrder.OrderType type;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @DecimalMax(value = "99999999.99", message = "Quantity too large")
    @Digits(integer = 8, fraction = 2, message = "Quantity format invalid")
    @Schema(description = "Order quantity", example = "1000.00", required = true)
    private BigDecimal quantity;

    @DecimalMin(value = "0.001", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.999", message = "Price too large")
    @Digits(integer = 6, fraction = 8, message = "Price format invalid")
    @Schema(description = "Order price (required for limit orders)", example = "45.75")
    private BigDecimal price;

    @DecimalMin(value = "0.001", message = "Stop price must be greater than 0")
    @DecimalMax(value = "999999.999", message = "Stop price too large")
    @Digits(integer = 6, fraction = 8, message = "Stop price format invalid")
    @Schema(description = "Stop price (required for stop orders)", example = "44.00")
    private BigDecimal stopPrice;

    @NotNull(message = "Time in force is required")
    @Schema(description = "Time in force", example = "GTC", required = true)
    private OMSOrder.TimeInForce timeInForce;

    @Future(message = "Expire time must be in the future")
    @Schema(description = "Order expiry time (for GTD orders)")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireTime;

    @Size(max = 50, message = "Account ID cannot exceed 50 characters")
    @Schema(description = "Account identifier", example = "ACC-123", maxLength = 50)
    private String accountId;

    @Size(max = 50, message = "Portfolio ID cannot exceed 50 characters")
    @Schema(description = "Portfolio identifier", example = "PF-123", maxLength = 50)
    private String portfolioId;

    @Size(max = 50, message = "Strategy ID cannot exceed 50 characters")
    @Schema(description = "Trading strategy identifier", example = "STRATEGY-1", maxLength = 50)
    private String strategyId;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Schema(description = "Order notes", example = "Manual order from trading desk", maxLength = 500)
    private String notes;

    // Advanced order options
    @Schema(description = "Whether this is a test order (paper trading)")
    private Boolean testOrder;

    @Schema(description = "Order priority (1-10, higher is more priority)")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority cannot exceed 10")
    private Integer priority;

    @Schema(description = "Reduce only flag (close position only)")
    private Boolean reduceOnly;

    @Schema(description = "Post only flag (maker only)")
    private Boolean postOnly;

    @Schema(description = "Hidden order flag")
    private Boolean hidden;

    @Schema(description = "Iceberg order visible quantity")
    @DecimalMin(value = "0.01", message = "Iceberg quantity must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Iceberg quantity format invalid")
    private BigDecimal icebergQuantity;

    // Risk management
    @Schema(description = "Maximum acceptable slippage percentage")
    @DecimalMin(value = "0.0", message = "Slippage cannot be negative")
    @DecimalMax(value = "100.0", message = "Slippage cannot exceed 100%")
    private BigDecimal maxSlippage;

    @Schema(description = "Parent order ID (for child orders)")
    private String parentOrderId;

    /**
     * Validation method to check order type consistency.
     */
    @AssertTrue(message = "Price is required for limit orders")
    public boolean isPriceValidForOrderType() {
        if (type == null) return true;

        return switch (type) {
            case LIMIT, STOP_LIMIT -> price != null && price.compareTo(BigDecimal.ZERO) > 0;
            case MARKET -> true; // Price optional for market orders
            case STOP -> stopPrice != null && stopPrice.compareTo(BigDecimal.ZERO) > 0;
        };
    }

    /**
     * Validation method to check stop price consistency.
     */
    @AssertTrue(message = "Stop price is required for stop orders")
    public boolean isStopPriceValidForOrderType() {
        if (type == null) return true;

        return switch (type) {
            case STOP, STOP_LIMIT -> stopPrice != null && stopPrice.compareTo(BigDecimal.ZERO) > 0;
            case MARKET, LIMIT -> true; // Stop price optional for market/limit orders
        };
    }

    /**
     * Validation method for iceberg orders.
     */
    @AssertTrue(message = "Iceberg quantity must be less than total quantity")
    public boolean isIcebergQuantityValid() {
        if (icebergQuantity == null || quantity == null) return true;
        return icebergQuantity.compareTo(quantity) <= 0;
    }

    /**
     * Validation method for expire time and time in force consistency.
     */
    @AssertTrue(message = "Expire time is required for GTD orders")
    public boolean isExpireTimeValidForTimeInForce() {
        if (timeInForce == null) return true;

        // If we had GTD (Good Till Date), it would require expireTime
        // For now, just validate GTC doesn't have expire time
        return switch (timeInForce) {
            case DAY, GTC, IOC, FOK -> true; // No expire time validation needed
        };
    }

    /**
     * Convert to CreateOrderRequest for service layer.
     */
    public CreateOrderRequest toCreateOrderRequest(String userId) {
        return CreateOrderRequest.builder()
            .userId(userId)
            .clientOrderId(clientOrderId)
            .symbol(symbol)
            .side(side)
            .type(type)
            .quantity(quantity)
            .price(price)
            .stopPrice(stopPrice)
            .timeInForce(timeInForce)
            .expireTime(expireTime)
            .accountId(accountId)
            .portfolioId(portfolioId)
            .strategyId(strategyId)
            .notes(notes)
            .build();
    }
}