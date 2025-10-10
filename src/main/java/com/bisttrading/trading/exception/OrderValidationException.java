package com.bisttrading.trading.exception;

/**
 * Exception thrown when order validation fails.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class OrderValidationException extends RuntimeException {

    private final String field;

    public OrderValidationException(String message) {
        super(message);
        this.field = null;
    }

    public OrderValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.field = field;
    }

    public OrderValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }

    public String getField() {
        return field;
    }
}
