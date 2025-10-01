package com.bisttrading.oms.exception;

/**
 * General exception for order management operations
 */
public class OrderManagementException extends RuntimeException {

    public OrderManagementException(String message) {
        super(message);
    }

    public OrderManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}