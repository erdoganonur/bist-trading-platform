package com.bisttrading.trading.exception;

/**
 * Exception thrown when order cancellation fails at the broker level.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class OrderCancellationFailedException extends RuntimeException {

    public OrderCancellationFailedException(String message) {
        super(message);
    }

    public OrderCancellationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
