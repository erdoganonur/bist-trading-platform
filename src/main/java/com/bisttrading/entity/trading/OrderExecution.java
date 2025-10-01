package com.bisttrading.entity.trading;

import com.bisttrading.entity.trading.enums.ExecutionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity representing individual trade executions for orders
 * Tracks detailed execution information with financial breakdown
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Entity
@Table(name = "order_executions", indexes = {
    @Index(name = "idx_order_executions_order_id", columnList = "order_id, execution_time"),
    @Index(name = "idx_order_executions_trade_id", columnList = "trade_id"),
    @Index(name = "idx_order_executions_execution_time", columnList = "execution_time"),
    @Index(name = "idx_order_executions_execution_type", columnList = "execution_type, execution_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderExecution {

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "execution_id")
    private UUID executionId;

    /**
     * Order that this execution belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    @ToString.Exclude
    private Order order;

    /**
     * Type of execution
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false)
    @NotNull(message = "Execution type is required")
    private ExecutionType executionType;

    /**
     * Quantity executed
     */
    @Column(name = "execution_quantity", nullable = false)
    @Positive(message = "Execution quantity must be positive")
    @NotNull(message = "Execution quantity is required")
    private Integer executionQuantity;

    /**
     * Execution price
     */
    @Column(name = "execution_price", nullable = false, precision = 20, scale = 6)
    @Positive(message = "Execution price must be positive")
    @NotNull(message = "Execution price is required")
    @Digits(integer = 14, fraction = 6, message = "Invalid execution price format")
    private BigDecimal executionPrice;

    /**
     * When the execution occurred
     */
    @Column(name = "execution_time", nullable = false)
    @NotNull(message = "Execution time is required")
    @Builder.Default
    private ZonedDateTime executionTime = ZonedDateTime.now();

    /**
     * Exchange trade ID
     */
    @Column(name = "trade_id", length = 100)
    @Size(max = 100, message = "Trade ID must not exceed 100 characters")
    private String tradeId;

    /**
     * Broker execution ID
     */
    @Column(name = "broker_execution_id", length = 100)
    @Size(max = 100, message = "Broker execution ID must not exceed 100 characters")
    private String brokerExecutionId;

    /**
     * Counterparty broker
     */
    @Column(name = "contra_broker", length = 50)
    @Size(max = 50, message = "Contra broker must not exceed 50 characters")
    private String contraBroker;

    /**
     * Gross trade amount (quantity * price)
     */
    @Column(name = "gross_amount", nullable = false, precision = 20, scale = 6)
    @Positive(message = "Gross amount must be positive")
    @NotNull(message = "Gross amount is required")
    @Digits(integer = 14, fraction = 6, message = "Invalid gross amount format")
    private BigDecimal grossAmount;

    /**
     * Commission for this execution
     */
    @Column(name = "commission", nullable = false, precision = 10, scale = 4)
    @PositiveOrZero(message = "Commission must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid commission format")
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO;

    /**
     * Brokerage fee for this execution
     */
    @Column(name = "brokerage_fee", nullable = false, precision = 10, scale = 4)
    @PositiveOrZero(message = "Brokerage fee must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid brokerage fee format")
    @Builder.Default
    private BigDecimal brokerageFee = BigDecimal.ZERO;

    /**
     * Exchange fee for this execution
     */
    @Column(name = "exchange_fee", nullable = false, precision = 10, scale = 4)
    @PositiveOrZero(message = "Exchange fee must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid exchange fee format")
    @Builder.Default
    private BigDecimal exchangeFee = BigDecimal.ZERO;

    /**
     * Tax amount for this execution
     */
    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 4)
    @PositiveOrZero(message = "Tax amount must be non-negative")
    @Digits(integer = 6, fraction = 4, message = "Invalid tax amount format")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Net amount (gross amount ± fees)
     */
    @Column(name = "net_amount", nullable = false, precision = 20, scale = 6)
    @Positive(message = "Net amount must be positive")
    @NotNull(message = "Net amount is required")
    @Digits(integer = 14, fraction = 6, message = "Invalid net amount format")
    private BigDecimal netAmount;

    /**
     * Best bid at time of execution
     */
    @Column(name = "bid_price", precision = 20, scale = 6)
    @Positive(message = "Bid price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid bid price format")
    private BigDecimal bidPrice;

    /**
     * Best ask at time of execution
     */
    @Column(name = "ask_price", precision = 20, scale = 6)
    @Positive(message = "Ask price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid ask price format")
    private BigDecimal askPrice;

    /**
     * Market price at time of execution
     */
    @Column(name = "market_price", precision = 20, scale = 6)
    @Positive(message = "Market price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid market price format")
    private BigDecimal marketPrice;

    /**
     * Additional metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode metadata = null;

    /**
     * When this execution record was created
     */
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // Business methods

    /**
     * Calculate total fees for this execution
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
     * Calculate spread at time of execution
     */
    public BigDecimal getSpread() {
        if (bidPrice == null || askPrice == null) return null;
        return askPrice.subtract(bidPrice);
    }

    /**
     * Calculate spread as percentage
     */
    public BigDecimal getSpreadPercentage() {
        BigDecimal spread = getSpread();
        if (spread == null || bidPrice == null || bidPrice.equals(BigDecimal.ZERO)) return null;
        return spread.divide(bidPrice, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if execution was inside the spread
     */
    public boolean isInsideSpread() {
        if (bidPrice == null || askPrice == null || executionPrice == null) return false;
        return executionPrice.compareTo(bidPrice) >= 0 && executionPrice.compareTo(askPrice) <= 0;
    }

    /**
     * Get price improvement (for market orders)
     */
    public BigDecimal getPriceImprovement() {
        if (order == null || marketPrice == null || executionPrice == null) return null;

        if (order.getOrderSide() == com.bisttrading.entity.trading.enums.OrderSide.BUY) {
            // For buy orders, improvement is market price - execution price
            return marketPrice.subtract(executionPrice);
        } else {
            // For sell orders, improvement is execution price - market price
            return executionPrice.subtract(marketPrice);
        }
    }

    /**
     * Check if this execution type represents a trade
     */
    public boolean isTrade() {
        return executionType.isTrade();
    }

    /**
     * Check if this execution affects position
     */
    public boolean affectsPosition() {
        return executionType.affectsPosition();
    }

    /**
     * Get execution value as percentage of order
     */
    public BigDecimal getExecutionPercentage() {
        if (order == null || order.getQuantity() == null || order.getQuantity() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(executionQuantity)
                         .divide(BigDecimal.valueOf(order.getQuantity()), 4, java.math.RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate per-share fee
     */
    public BigDecimal getPerShareFee() {
        if (executionQuantity == null || executionQuantity == 0) return BigDecimal.ZERO;
        return getTotalFees().divide(BigDecimal.valueOf(executionQuantity), 6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get effective execution price (including fees)
     */
    public BigDecimal getEffectivePrice() {
        if (executionQuantity == null || executionQuantity == 0 || order == null) {
            return executionPrice;
        }

        BigDecimal feePerShare = getPerShareFee();
        if (order.getOrderSide() == com.bisttrading.entity.trading.enums.OrderSide.BUY) {
            return executionPrice.add(feePerShare);
        } else {
            return executionPrice.subtract(feePerShare);
        }
    }

    // Validation methods

    @PreUpdate
    private void validate() {
        // Validate that gross amount matches quantity * price
        if (executionQuantity != null && executionPrice != null) {
            BigDecimal calculatedGross = executionPrice.multiply(BigDecimal.valueOf(executionQuantity));
            if (grossAmount != null && grossAmount.compareTo(calculatedGross) != 0) {
                throw new IllegalArgumentException("Gross amount must equal execution price * quantity");
            }
            if (grossAmount == null) {
                grossAmount = calculatedGross;
            }
        }

        // Validate net amount calculation
        if (grossAmount != null && netAmount != null) {
            BigDecimal calculatedNet = grossAmount.subtract(getTotalFees());
            if (calculatedNet.compareTo(netAmount) != 0) {
                throw new IllegalArgumentException("Net amount calculation is incorrect");
            }
        }

        // Validate bid/ask spread
        if (bidPrice != null && askPrice != null && bidPrice.compareTo(askPrice) > 0) {
            throw new IllegalArgumentException("Bid price cannot be greater than ask price");
        }

        // Set created timestamp if not set
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }

        // Set execution time if not set
        if (executionTime == null) {
            executionTime = ZonedDateTime.now();
        }
    }

    @PrePersist
    private void prePersist() {
        if (executionId == null) {
            executionId = UUID.randomUUID();
        }
    }
}