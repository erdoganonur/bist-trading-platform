package com.bisttrading.user.entity.trading;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import com.bisttrading.user.entity.User;
import com.bisttrading.user.entity.Organization;
import com.bisttrading.user.entity.trading.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a trading order with complete lifecycle tracking
 * Supports all major order types and multi-broker integration
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id, createdAt"),
    @Index(name = "idx_orders_account_id", columnList = "account_id, createdAt"),
    @Index(name = "idx_orders_symbol_id", columnList = "symbol_id, createdAt"),
    @Index(name = "idx_orders_status", columnList = "order_status, createdAt"),
    @Index(name = "idx_orders_client_order_id", columnList = "client_order_id"),
    @Index(name = "idx_orders_broker_order_id", columnList = "broker_order_id"),
    @Index(name = "idx_orders_user_status", columnList = "user_id, order_status, createdAt"),
    @Index(name = "idx_orders_active", columnList = "user_id, symbol_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order extends BaseEntity {

    /**
     * Client-provided order ID (optional)
     */
    @Column(name = "client_order_id", length = 50)
    @Size(max = 50, message = "Client order ID must not exceed 50 characters")
    private String clientOrderId;

    /**
     * Parent order ID for bracket/OCO orders
     */
    @Column(name = "parent_order_id")
    private String parentOrderId;

    /**
     * Reference to parent order entity
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Order parentOrder;

    /**
     * Child orders for bracket/OCO strategies
     */
    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Order> childOrders = new ArrayList<>();

    /**
     * User who placed the order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @ToString.Exclude
    private User user;

    /**
     * Organization (if placed on behalf of organization)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @ToString.Exclude
    private Organization organization;

    /**
     * Broker account ID
     */
    @Column(name = "account_id", length = 50, nullable = false)
    @NotBlank(message = "Account ID is required")
    @Size(max = 50, message = "Account ID must not exceed 50 characters")
    private String accountId;

    /**
     * Sub-account ID (if applicable)
     */
    @Column(name = "sub_account_id", length = 50)
    @Size(max = 50, message = "Sub-account ID must not exceed 50 characters")
    private String subAccountId;

    /**
     * Symbol being traded
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    @NotNull(message = "Symbol is required")
    @ToString.Exclude
    private Symbol symbol;

    /**
     * Order type (MARKET, LIMIT, STOP, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    @NotNull(message = "Order type is required")
    private OrderType orderType;

    /**
     * Order side (BUY/SELL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false)
    @NotNull(message = "Order side is required")
    private OrderSide orderSide;

    /**
     * Time in force
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false)
    @Builder.Default
    private TimeInForce timeInForce = TimeInForce.DAY;

    /**
     * Order quantity
     */
    @Column(name = "quantity", nullable = false)
    @Positive(message = "Quantity must be positive")
    @NotNull(message = "Quantity is required")
    private Integer quantity;

    /**
     * Limit price (for limit orders)
     */
    @Column(name = "price", precision = 20, scale = 6)
    @Positive(message = "Price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid price format")
    private BigDecimal price;

    /**
     * Stop price (for stop orders)
     */
    @Column(name = "stop_price", precision = 20, scale = 6)
    @Positive(message = "Stop price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid stop price format")
    private BigDecimal stopPrice;

    /**
     * Trigger price (for complex orders)
     */
    @Column(name = "trigger_price", precision = 20, scale = 6)
    @Positive(message = "Trigger price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid trigger price format")
    private BigDecimal triggerPrice;

    /**
     * Quantity filled so far
     */
    @Column(name = "filled_quantity", nullable = false)
    @PositiveOrZero(message = "Filled quantity must be non-negative")
    @Builder.Default
    private Integer filledQuantity = 0;

    /**
     * Remaining quantity to be filled
     */
    @Column(name = "remaining_quantity", nullable = false)
    @PositiveOrZero(message = "Remaining quantity must be non-negative")
    private Integer remainingQuantity;

    /**
     * Average fill price
     */
    @Column(name = "average_fill_price", precision = 20, scale = 6)
    @Positive(message = "Average fill price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid average fill price format")
    private BigDecimal averageFillPrice;

    /**
     * Last fill price
     */
    @Column(name = "last_fill_price", precision = 20, scale = 6)
    @Positive(message = "Last fill price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid last fill price format")
    private BigDecimal lastFillPrice;

    /**
     * Last fill quantity
     */
    @Column(name = "last_fill_quantity")
    @Positive(message = "Last fill quantity must be positive")
    private Integer lastFillQuantity;

    /**
     * Current order status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    /**
     * Status change reason
     */
    @Column(name = "status_reason")
    @Size(max = 255, message = "Status reason must not exceed 255 characters")
    private String statusReason;

    /**
     * When order was submitted to broker
     */
    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    /**
     * When order was accepted by exchange
     */
    @Column(name = "accepted_at")
    private ZonedDateTime acceptedAt;

    /**
     * First fill timestamp
     */
    @Column(name = "first_fill_at")
    private ZonedDateTime firstFillAt;

    /**
     * Last fill timestamp
     */
    @Column(name = "last_fill_at")
    private ZonedDateTime lastFillAt;

    /**
     * When order was completed
     */
    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    /**
     * Expiry date (for GTD orders)
     */
    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    /**
     * Broker identifier
     */
    @Column(name = "broker_id", length = 50)
    @Size(max = 50, message = "Broker ID must not exceed 50 characters")
    private String brokerId;

    /**
     * Broker's order ID
     */
    @Column(name = "broker_order_id", length = 100)
    @Size(max = 100, message = "Broker order ID must not exceed 100 characters")
    private String brokerOrderId;

    /**
     * Exchange order ID
     */
    @Column(name = "exchange_order_id", length = 100)
    @Size(max = 100, message = "Exchange order ID must not exceed 100 characters")
    private String exchangeOrderId;

    /**
     * Commission charged
     */
    @Column(name = "commission", precision = 10, scale = 4, nullable = false)
    @PositiveOrZero(message = "Commission must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid commission format")
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO;

    /**
     * Brokerage fee
     */
    @Column(name = "brokerage_fee", precision = 10, scale = 4, nullable = false)
    @PositiveOrZero(message = "Brokerage fee must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid brokerage fee format")
    @Builder.Default
    private BigDecimal brokerageFee = BigDecimal.ZERO;

    /**
     * Exchange fee
     */
    @Column(name = "exchange_fee", precision = 10, scale = 4, nullable = false)
    @PositiveOrZero(message = "Exchange fee must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid exchange fee format")
    @Builder.Default
    private BigDecimal exchangeFee = BigDecimal.ZERO;

    /**
     * Tax amount
     */
    @Column(name = "tax_amount", precision = 10, scale = 4, nullable = false)
    @PositiveOrZero(message = "Tax amount must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid tax amount format")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Total order cost
     */
    @Column(name = "total_cost", precision = 20, scale = 6)
    @Positive(message = "Total cost must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid total cost format")
    private BigDecimal totalCost;

    /**
     * Risk check passed flag
     */
    @Column(name = "risk_check_passed", nullable = false)
    @Builder.Default
    private Boolean riskCheckPassed = false;

    /**
     * Risk rejection reason
     */
    @Column(name = "risk_rejection_reason")
    @Size(max = 255, message = "Risk rejection reason must not exceed 255 characters")
    private String riskRejectionReason;

    /**
     * Buying power check passed
     */
    @Column(name = "buying_power_check", nullable = false)
    @Builder.Default
    private Boolean buyingPowerCheck = false;

    /**
     * Position limit check passed
     */
    @Column(name = "position_limit_check", nullable = false)
    @Builder.Default
    private Boolean positionLimitCheck = false;

    /**
     * Strategy ID (if part of algorithmic strategy)
     */
    @Column(name = "strategy_id", length = 100)
    @Size(max = 100, message = "Strategy ID must not exceed 100 characters")
    private String strategyId;

    /**
     * Algorithm ID (if algorithmic order)
     */
    @Column(name = "algo_id", length = 100)
    @Size(max = 100, message = "Algorithm ID must not exceed 100 characters")
    private String algoId;

    /**
     * Additional metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode metadata = null;

    /**
     * Error message (if any)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    @PositiveOrZero(message = "Retry count must be non-negative")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Order executions
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<OrderExecution> executions = new ArrayList<>();

    // Business methods

    /**
     * Check if order is still active
     */
    public boolean isActive() {
        return orderStatus.isActive();
    }

    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return orderStatus.isCompleted();
    }

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return orderStatus.isCancellable();
    }

    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return orderStatus.isModifiable();
    }

    /**
     * Get fill percentage
     */
    public BigDecimal getFillPercentage() {
        if (quantity == null || quantity == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(filledQuantity)
                         .divide(BigDecimal.valueOf(quantity), 4, java.math.RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate total fees
     */
    public BigDecimal getTotalFees() {
        BigDecimal total = BigDecimal.ZERO;
        if (commission != null) total = total.add(commission);
        if (brokerageFee != null) total = total.add(brokerageFee);
        if (exchangeFee != null) total = total.add(exchangeFee);
        if (taxAmount != null) total = total.add(taxAmount);
        return total;
    }

    /**
     * Calculate gross amount (before fees)
     */
    public BigDecimal getGrossAmount() {
        if (averageFillPrice == null || filledQuantity == null || filledQuantity == 0) {
            return BigDecimal.ZERO;
        }
        return averageFillPrice.multiply(BigDecimal.valueOf(filledQuantity));
    }

    /**
     * Calculate net amount (after fees)
     */
    public BigDecimal getNetAmount() {
        BigDecimal gross = getGrossAmount();
        if (orderSide == OrderSide.BUY) {
            return gross.add(getTotalFees()); // Add fees for buy orders
        } else {
            return gross.subtract(getTotalFees()); // Subtract fees for sell orders
        }
    }

    /**
     * Update order status with validation
     */
    public void updateStatus(OrderStatus newStatus, String reason) {
        if (!this.orderStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", this.orderStatus, newStatus)
            );
        }
        this.orderStatus = newStatus;
        this.statusReason = reason;

        // Set completion timestamp for final statuses
        if (newStatus.isCompleted() && this.completedAt == null) {
            this.completedAt = ZonedDateTime.now();
        }
    }

    /**
     * Add execution and update order state
     */
    public void addExecution(OrderExecution execution) {
        if (executions == null) {
            executions = new ArrayList<>();
        }
        executions.add(execution);
        execution.setOrder(this);

        // Update fill information
        this.filledQuantity = (this.filledQuantity == null ? 0 : this.filledQuantity) + execution.getExecutionQuantity();
        this.remainingQuantity = this.quantity - this.filledQuantity;

        // Update pricing information
        updateAveragePrice(execution.getExecutionPrice(), execution.getExecutionQuantity());
        this.lastFillPrice = execution.getExecutionPrice();
        this.lastFillQuantity = execution.getExecutionQuantity();
        this.lastFillAt = execution.getExecutionTime();

        if (this.firstFillAt == null) {
            this.firstFillAt = execution.getExecutionTime();
        }

        // Update order status based on fill
        if (this.remainingQuantity == 0) {
            updateStatus(OrderStatus.FILLED, "Order completely filled");
        } else if (this.filledQuantity > 0) {
            updateStatus(OrderStatus.PARTIALLY_FILLED, "Order partially filled");
        }
    }

    /**
     * Update average fill price with new execution
     */
    private void updateAveragePrice(BigDecimal executionPrice, Integer executionQuantity) {
        if (this.averageFillPrice == null) {
            this.averageFillPrice = executionPrice;
        } else {
            // Calculate weighted average
            BigDecimal currentValue = this.averageFillPrice.multiply(BigDecimal.valueOf(this.filledQuantity - executionQuantity));
            BigDecimal newValue = executionPrice.multiply(BigDecimal.valueOf(executionQuantity));
            this.averageFillPrice = currentValue.add(newValue).divide(BigDecimal.valueOf(this.filledQuantity), 6, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Check if order requires limit price
     */
    public boolean requiresLimitPrice() {
        return orderType.requiresLimitPrice();
    }

    /**
     * Check if order requires stop price
     */
    public boolean requiresStopPrice() {
        return orderType.requiresStopPrice();
    }

    /**
     * Check if order is part of strategy
     */
    public boolean isStrategyOrder() {
        return strategyId != null || algoId != null;
    }

    /**
     * Check if order is a parent order
     */
    public boolean isParentOrder() {
        return childOrders != null && !childOrders.isEmpty();
    }

    /**
     * Check if order is a child order
     */
    public boolean isChildOrder() {
        return parentOrderId != null;
    }

    /**
     * Get order value (quantity * price)
     */
    public BigDecimal getOrderValue() {
        if (price == null || quantity == null) return null;
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    // Validation methods

    @PrePersist
    @PreUpdate
    private void validate() {
        // Set remaining quantity if not set
        if (remainingQuantity == null && quantity != null && filledQuantity != null) {
            remainingQuantity = quantity - filledQuantity;
        }

        // Validate quantities
        if (filledQuantity != null && quantity != null && filledQuantity > quantity) {
            throw new IllegalArgumentException("Filled quantity cannot exceed order quantity");
        }

        // Validate price requirements
        if (requiresLimitPrice() && price == null) {
            throw new IllegalArgumentException(String.format("Order type %s requires limit price", orderType));
        }

        if (requiresStopPrice() && stopPrice == null) {
            throw new IllegalArgumentException(String.format("Order type %s requires stop price", orderType));
        }

        // Validate expiry date for GTD orders
        if (timeInForce == TimeInForce.GTD && expiresAt == null) {
            throw new IllegalArgumentException("GTD orders require expiry date");
        }

        // Validate risk checks for submitted orders
        if (orderStatus == OrderStatus.SUBMITTED && !riskCheckPassed) {
            throw new IllegalArgumentException("Risk checks must pass before order submission");
        }
    }
}