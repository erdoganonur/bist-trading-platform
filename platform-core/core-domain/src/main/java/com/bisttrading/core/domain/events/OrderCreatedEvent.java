package com.bisttrading.core.domain.events;

import com.bisttrading.core.domain.valueobjects.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the creation of a trading order.
 * Published when a new order is created in the system.
 */
@Getter
public class OrderCreatedEvent extends BaseDomainEvent {

    private final String orderId;
    private final String userId;
    private final Symbol symbol;
    private final OrderType orderType;
    private final OrderSide orderSide;
    private final Quantity quantity;
    private final Price price;
    private final Money totalValue;
    private final LocalDateTime orderTime;
    private final String portfolioId;

    public OrderCreatedEvent(String orderId, String userId, Symbol symbol, OrderType orderType,
                           OrderSide orderSide, Quantity quantity, Price price, Money totalValue,
                           LocalDateTime orderTime, String portfolioId) {
        super(orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.orderType = orderType;
        this.orderSide = orderSide;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = totalValue;
        this.orderTime = orderTime;
        this.portfolioId = portfolioId;
    }

    /**
     * Reconstructs event from stored data.
     */
    public OrderCreatedEvent(UUID eventId, LocalDateTime occurredOn, String orderId, String userId,
                           Symbol symbol, OrderType orderType, OrderSide orderSide, Quantity quantity,
                           Price price, Money totalValue, LocalDateTime orderTime, String portfolioId) {
        super(eventId, occurredOn, orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.orderType = orderType;
        this.orderSide = orderSide;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = totalValue;
        this.orderTime = orderTime;
        this.portfolioId = portfolioId;
    }

    /**
     * Order type enumeration.
     */
    public enum OrderType {
        MARKET("MARKET", "Piyasa Emri"),
        LIMIT("LIMIT", "Limitli Emir"),
        STOP("STOP", "Stop Emir"),
        STOP_LIMIT("STOP_LIMIT", "Stop Limitli Emir");

        private final String code;
        private final String description;

        OrderType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * Order side enumeration.
     */
    public enum OrderSide {
        BUY("BUY", "Alış"),
        SELL("SELL", "Satış");

        private final String code;
        private final String description;

        OrderSide(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    @Override
    public String toString() {
        return String.format("OrderCreatedEvent{orderId='%s', userId='%s', symbol=%s, type=%s, side=%s, quantity=%s, price=%s}",
            orderId, userId, symbol.getCode(), orderType, orderSide, quantity, price);
    }
}