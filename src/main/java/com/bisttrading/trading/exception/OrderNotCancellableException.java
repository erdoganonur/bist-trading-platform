package com.bisttrading.trading.exception;

import com.bisttrading.entity.trading.enums.OrderStatus;

/**
 * Exception thrown when attempting to cancel an order that cannot be cancelled.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class OrderNotCancellableException extends RuntimeException {

    private final String orderId;
    private final OrderStatus currentStatus;

    public OrderNotCancellableException(String orderId, OrderStatus currentStatus) {
        super(String.format("Order %s cannot be cancelled in current status: %s", orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public OrderNotCancellableException(String orderId, OrderStatus currentStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
