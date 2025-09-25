package com.bisttrading.oms.model;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OMS Order entity
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "oms_orders")
@EqualsAndHashCode(callSuper = true)
public class OMSOrder extends BaseEntity {

    @Id
    private String orderId;

    @Column(name = "client_order_id", nullable = false)
    private String clientOrderId;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrderType type;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 8)
    private BigDecimal price;

    @Column(name = "stop_price", precision = 15, scale = 8)
    private BigDecimal stopPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false)
    private TimeInForce timeInForce;

    @Column(name = "filled_quantity", precision = 15, scale = 2)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "remaining_quantity", precision = 15, scale = 2)
    private BigDecimal remainingQuantity;

    @Column(name = "average_price", precision = 15, scale = 8)
    private BigDecimal averagePrice;

    @Column(name = "commission", precision = 10, scale = 4)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "account_id", length = 50)
    private String accountId;

    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;

    @Column(name = "strategy_id", length = 50)
    private String strategyId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_active")
    private boolean active = true;

    // Enums
    public enum OrderSide {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT, STOP, STOP_LIMIT
    }

    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED, EXPIRED
    }

    public enum TimeInForce {
        DAY, GTC, IOC, FOK
    }

    // Business methods
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
        if (averagePrice != null && filledQuantity != null) {
            return averagePrice.multiply(filledQuantity);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getFillPercentage() {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0 && filledQuantity != null) {
            return filledQuantity.divide(quantity, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    @PrePersist
    protected void prePersist() {
        if (remainingQuantity == null && quantity != null) {
            remainingQuantity = quantity.subtract(filledQuantity != null ? filledQuantity : BigDecimal.ZERO);
        }
    }

    @PreUpdate
    protected void preUpdate() {
        if (quantity != null) {
            remainingQuantity = quantity.subtract(filledQuantity != null ? filledQuantity : BigDecimal.ZERO);
        }
    }
}