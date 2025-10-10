package com.bisttrading.trading.exception;

/**
 * Exception thrown when an order cannot be found.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super(String.format("Order not found with ID: %s", orderId));
        this.orderId = orderId;
    }

    public OrderNotFoundException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
