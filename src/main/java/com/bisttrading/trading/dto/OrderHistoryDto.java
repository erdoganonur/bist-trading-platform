package com.bisttrading.trading.dto;

import com.bisttrading.entity.trading.enums.OrderSide;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.entity.trading.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO for order history information.
 * Provides comprehensive order details including executions.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order history with execution details")
public class OrderHistoryDto {

    /**
     * Order ID
     */
    @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private String orderId;

    /**
     * Client-provided order ID
     */
    @Schema(description = "Client order ID", example = "ORD-12345")
    private String clientOrderId;

    /**
     * Broker's order ID
     */
    @Schema(description = "Broker order ID", example = "BRK-98765")
    private String brokerOrderId;

    /**
     * Exchange order ID
     */
    @Schema(description = "Exchange order ID", example = "EXCH-45678")
    private String exchangeOrderId;

    // ===== Order Details =====

    /**
     * Symbol code
     */
    @Schema(description = "Symbol code", example = "AKBNK", required = true)
    private String symbol;

    /**
     * Order side (BUY/SELL)
     */
    @Schema(description = "Order side", example = "BUY", required = true)
    private OrderSide side;

    /**
     * Order type
     */
    @Schema(description = "Order type", example = "LIMIT", required = true)
    private OrderType orderType;

    /**
     * Current order status
     */
    @Schema(description = "Order status", example = "FILLED", required = true)
    private OrderStatus status;

    /**
     * Status reason
     */
    @Schema(description = "Status reason", example = "Order completely filled")
    private String statusReason;

    // ===== Quantities and Prices =====

    /**
     * Original order quantity
     */
    @Schema(description = "Order quantity", example = "1000", required = true)
    private Integer quantity;

    /**
     * Limit price
     */
    @Schema(description = "Limit price", example = "15.75")
    private BigDecimal price;

    /**
     * Stop price
     */
    @Schema(description = "Stop price", example = "14.50")
    private BigDecimal stopPrice;

    /**
     * Quantity filled
     */
    @Schema(description = "Filled quantity", example = "1000")
    private Integer filledQuantity;

    /**
     * Remaining quantity
     */
    @Schema(description = "Remaining quantity", example = "0")
    private Integer remainingQuantity;

    /**
     * Average fill price
     */
    @Schema(description = "Average fill price", example = "15.73")
    private BigDecimal averageFillPrice;

    // ===== Costs and Fees =====

    /**
     * Total gross cost (quantity * average price)
     */
    @Schema(description = "Total cost before fees", example = "15730.00")
    private BigDecimal totalCost;

    /**
     * Total commission
     */
    @Schema(description = "Total commission", example = "15.73")
    private BigDecimal commission;

    /**
     * Total brokerage fee
     */
    @Schema(description = "Brokerage fee", example = "7.87")
    private BigDecimal brokerageFee;

    /**
     * Total exchange fee
     */
    @Schema(description = "Exchange fee", example = "1.57")
    private BigDecimal exchangeFee;

    /**
     * Total tax
     */
    @Schema(description = "Tax amount", example = "31.46")
    private BigDecimal tax;

    // ===== Executions =====

    /**
     * List of executions for this order
     */
    @Schema(description = "Order executions")
    private List<ExecutionDto> executions;

    // ===== Timestamps =====

    /**
     * Order creation timestamp
     */
    @Schema(description = "Order created at", example = "2024-09-24T10:30:00Z", required = true)
    private ZonedDateTime createdAt;

    /**
     * Order submission timestamp
     */
    @Schema(description = "Order submitted at", example = "2024-09-24T10:30:01Z")
    private ZonedDateTime submittedAt;

    /**
     * Order acceptance timestamp
     */
    @Schema(description = "Order accepted at", example = "2024-09-24T10:30:02Z")
    private ZonedDateTime acceptedAt;

    /**
     * First fill timestamp
     */
    @Schema(description = "First fill at", example = "2024-09-24T10:30:15Z")
    private ZonedDateTime firstFillAt;

    /**
     * Last fill timestamp
     */
    @Schema(description = "Last fill at", example = "2024-09-24T10:32:45Z")
    private ZonedDateTime lastFillAt;

    /**
     * Order completion timestamp
     */
    @Schema(description = "Order completed at", example = "2024-09-24T10:32:45Z")
    private ZonedDateTime completedAt;

    /**
     * Cancellation timestamp
     */
    @Schema(description = "Order cancelled at", example = "2024-09-24T10:35:00Z")
    private ZonedDateTime cancelledAt;

    // ===== Computed Fields =====

    /**
     * Calculate net cost (total cost + all fees)
     */
    public BigDecimal getNetCost() {
        BigDecimal net = totalCost != null ? totalCost : BigDecimal.ZERO;
        if (commission != null) net = net.add(commission);
        if (brokerageFee != null) net = net.add(brokerageFee);
        if (exchangeFee != null) net = net.add(exchangeFee);
        if (tax != null) net = net.add(tax);
        return net;
    }

    /**
     * Calculate total fees
     */
    public BigDecimal getTotalFees() {
        BigDecimal fees = BigDecimal.ZERO;
        if (commission != null) fees = fees.add(commission);
        if (brokerageFee != null) fees = fees.add(brokerageFee);
        if (exchangeFee != null) fees = fees.add(exchangeFee);
        if (tax != null) fees = fees.add(tax);
        return fees;
    }

    /**
     * Calculate fill percentage
     */
    public BigDecimal getFillPercentage() {
        if (quantity == null || quantity == 0 || filledQuantity == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(filledQuantity)
                .divide(BigDecimal.valueOf(quantity), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if order is still active
     */
    public boolean isActive() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.SUBMITTED ||
               status == OrderStatus.ACCEPTED ||
               status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return status == OrderStatus.FILLED ||
               status == OrderStatus.CANCELLED ||
               status == OrderStatus.REJECTED ||
               status == OrderStatus.EXPIRED ||
               status == OrderStatus.ERROR;
    }

    /**
     * Get execution count
     */
    public int getExecutionCount() {
        return executions != null ? executions.size() : 0;
    }
}
