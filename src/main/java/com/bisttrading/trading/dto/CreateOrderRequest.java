package com.bisttrading.trading.dto;

import com.bisttrading.entity.trading.enums.OrderSide;
import com.bisttrading.entity.trading.enums.OrderType;
import com.bisttrading.entity.trading.enums.TimeInForce;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Request DTO for creating a new order.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

    /**
     * Symbol code
     */
    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    @Schema(description = "Symbol code (e.g., AKBNK, GARAN)", example = "AKBNK", required = true)
    private String symbol;

    /**
     * Order side (BUY/SELL)
     */
    @NotNull(message = "Order side is required")
    @Schema(description = "Order side", example = "BUY", required = true)
    private OrderSide side;

    /**
     * Order type
     */
    @NotNull(message = "Order type is required")
    @Schema(description = "Order type", example = "LIMIT", required = true)
    private OrderType orderType;

    /**
     * Order quantity
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Schema(description = "Order quantity (number of shares)", example = "1000", required = true)
    private Integer quantity;

    /**
     * Limit price (required for LIMIT orders)
     */
    @Positive(message = "Price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid price format")
    @Schema(description = "Limit price (required for LIMIT orders)", example = "15.75")
    private BigDecimal price;

    /**
     * Stop price (required for STOP orders)
     */
    @Positive(message = "Stop price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid stop price format")
    @Schema(description = "Stop price (required for STOP orders)", example = "14.50")
    private BigDecimal stopPrice;

    /**
     * Time in force
     */
    @Builder.Default
    @Schema(description = "Time in force", example = "DAY", defaultValue = "DAY")
    private TimeInForce timeInForce = TimeInForce.DAY;

    /**
     * Expiry date (for GTD orders)
     */
    @Schema(description = "Expiry date for GTD orders", example = "2024-12-31T17:00:00Z")
    private ZonedDateTime expiresAt;

    /**
     * Client-provided order ID (optional)
     */
    @Size(max = 50, message = "Client order ID must not exceed 50 characters")
    @Schema(description = "Client-provided order ID (optional)", example = "MY-ORD-123")
    private String clientOrderId;

    /**
     * Account ID
     */
    @NotBlank(message = "Account ID is required")
    @Size(max = 50, message = "Account ID must not exceed 50 characters")
    @Schema(description = "Broker account ID", example = "ACC-1001", required = true)
    private String accountId;

    /**
     * Sub-account ID (optional)
     */
    @Size(max = 50, message = "Sub-account ID must not exceed 50 characters")
    @Schema(description = "Sub-account ID (optional)", example = "SUB-001")
    private String subAccountId;

    /**
     * Send SMS notification
     */
    @Builder.Default
    @Schema(description = "Send SMS notification on execution", example = "false", defaultValue = "false")
    private Boolean smsNotification = false;

    /**
     * Send email notification
     */
    @Builder.Default
    @Schema(description = "Send email notification on execution", example = "true", defaultValue = "false")
    private Boolean emailNotification = false;

    /**
     * Strategy ID (for algorithmic orders)
     */
    @Size(max = 100, message = "Strategy ID must not exceed 100 characters")
    @Schema(description = "Strategy ID for algorithmic orders", example = "STRAT-001")
    private String strategyId;

    /**
     * Algorithm ID (for algorithmic orders)
     */
    @Size(max = 100, message = "Algorithm ID must not exceed 100 characters")
    @Schema(description = "Algorithm ID for algorithmic orders", example = "ALGO-001")
    private String algoId;

    /**
     * Parent order ID (for bracket/OCO orders)
     */
    @Schema(description = "Parent order ID for bracket/OCO orders", example = "ORD-12345")
    private String parentOrderId;

    /**
     * Validate the order request
     */
    public void validate() {
        // Validate price requirements based on order type
        if (orderType != null) {
            if (orderType.requiresLimitPrice() && price == null) {
                throw new IllegalArgumentException(
                    String.format("Order type %s requires limit price", orderType)
                );
            }

            if (orderType.requiresStopPrice() && stopPrice == null) {
                throw new IllegalArgumentException(
                    String.format("Order type %s requires stop price", orderType)
                );
            }
        }

        // Validate expiry date for GTD orders
        if (timeInForce == TimeInForce.GTD && expiresAt == null) {
            throw new IllegalArgumentException("GTD orders require expiry date");
        }

        // Validate expiry date is in the future
        if (expiresAt != null && expiresAt.isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }

        // Validate quantity is positive
        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate prices are positive
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        if (stopPrice != null && stopPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stop price must be positive");
        }
    }
}
