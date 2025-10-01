package com.bisttrading.core.domain.events;

import com.bisttrading.core.domain.valueobjects.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the cancellation of a trading order.
 * Published when an order is cancelled by user or system.
 */
@Getter
public class OrderCancelledEvent extends BaseDomainEvent {

    private final String orderId;
    private final String userId;
    private final Symbol symbol;
    private final OrderCreatedEvent.OrderSide orderSide;
    private final Quantity originalQuantity;
    private final Quantity cancelledQuantity;
    private final Quantity filledQuantity;
    private final Price orderPrice;
    private final LocalDateTime cancellationTime;
    private final CancellationReason cancellationReason;
    private final String cancellationComment;
    private final String portfolioId;

    public OrderCancelledEvent(String orderId, String userId, Symbol symbol,
                             OrderCreatedEvent.OrderSide orderSide, Quantity originalQuantity,
                             Quantity cancelledQuantity, Quantity filledQuantity, Price orderPrice,
                             LocalDateTime cancellationTime, CancellationReason cancellationReason,
                             String cancellationComment, String portfolioId) {
        super(orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.orderSide = orderSide;
        this.originalQuantity = originalQuantity;
        this.cancelledQuantity = cancelledQuantity;
        this.filledQuantity = filledQuantity;
        this.orderPrice = orderPrice;
        this.cancellationTime = cancellationTime;
        this.cancellationReason = cancellationReason;
        this.cancellationComment = cancellationComment;
        this.portfolioId = portfolioId;
    }

    /**
     * Reconstructs event from stored data.
     */
    public OrderCancelledEvent(UUID eventId, LocalDateTime occurredOn, String orderId, String userId,
                             Symbol symbol, OrderCreatedEvent.OrderSide orderSide, Quantity originalQuantity,
                             Quantity cancelledQuantity, Quantity filledQuantity, Price orderPrice,
                             LocalDateTime cancellationTime, CancellationReason cancellationReason,
                             String cancellationComment, String portfolioId) {
        super(eventId, occurredOn, orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.orderSide = orderSide;
        this.originalQuantity = originalQuantity;
        this.cancelledQuantity = cancelledQuantity;
        this.filledQuantity = filledQuantity;
        this.orderPrice = orderPrice;
        this.cancellationTime = cancellationTime;
        this.cancellationReason = cancellationReason;
        this.cancellationComment = cancellationComment;
        this.portfolioId = portfolioId;
    }

    /**
     * Cancellation reason enumeration.
     */
    public enum CancellationReason {
        USER_REQUESTED("USER_REQUESTED", "Kullanıcı talebi"),
        INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Yetersiz bakiye"),
        INVALID_PRICE("INVALID_PRICE", "Geçersiz fiyat"),
        MARKET_CLOSED("MARKET_CLOSED", "Piyasa kapalı"),
        DAILY_LIMIT_EXCEEDED("DAILY_LIMIT_EXCEEDED", "Günlük limit aşımı"),
        SYSTEM_ERROR("SYSTEM_ERROR", "Sistem hatası"),
        RISK_LIMIT_EXCEEDED("RISK_LIMIT_EXCEEDED", "Risk limiti aşımı"),
        SYMBOL_SUSPENDED("SYMBOL_SUSPENDED", "Sembol askıya alındı"),
        EXPIRED("EXPIRED", "Süre dolumu"),
        COMPLIANCE_VIOLATION("COMPLIANCE_VIOLATION", "Uyumluluk ihlali");

        private final String code;
        private final String description;

        CancellationReason(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public boolean isUserInitiated() {
            return this == USER_REQUESTED;
        }

        public boolean isSystemInitiated() {
            return this != USER_REQUESTED;
        }
    }

    /**
     * Checks if the order was partially filled before cancellation.
     *
     * @return true if partially filled
     */
    public boolean wasPartiallyFilled() {
        return filledQuantity.isPositive();
    }

    /**
     * Gets the fill percentage before cancellation.
     *
     * @return Fill percentage as double (0.0 to 1.0)
     */
    public double getFillPercentage() {
        if (originalQuantity.isZero()) {
            return 0.0;
        }
        return filledQuantity.percentageOf(originalQuantity).doubleValue() / 100.0;
    }

    /**
     * Gets the cancellation percentage.
     *
     * @return Cancellation percentage as double (0.0 to 1.0)
     */
    public double getCancellationPercentage() {
        if (originalQuantity.isZero()) {
            return 0.0;
        }
        return cancelledQuantity.percentageOf(originalQuantity).doubleValue() / 100.0;
    }

    /**
     * Checks if this was a user-initiated cancellation.
     *
     * @return true if user initiated
     */
    public boolean isUserInitiated() {
        return cancellationReason.isUserInitiated();
    }

    /**
     * Checks if this was a system-initiated cancellation.
     *
     * @return true if system initiated
     */
    public boolean isSystemInitiated() {
        return cancellationReason.isSystemInitiated();
    }

    @Override
    public String toString() {
        return String.format("OrderCancelledEvent{orderId='%s', symbol=%s, side=%s, " +
                           "cancelled=%s, filled=%s, reason=%s}",
            orderId, symbol.getCode(), orderSide, cancelledQuantity,
            filledQuantity, cancellationReason);
    }
}