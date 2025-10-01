package com.bisttrading.oms.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Exception thrown when order validation fails.
 */
@Getter
public class OrderValidationException extends RuntimeException {

    private final Map<String, String> validationErrors;

    public OrderValidationException(String message) {
        super(message);
        this.validationErrors = Map.of();
    }

    public OrderValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? validationErrors : Map.of();
    }

    public OrderValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = Map.of();
    }
}