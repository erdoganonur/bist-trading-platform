package com.bisttrading.broker.algolab.exception;

/**
 * Exception thrown when AlgoLab API request fails.
 */
public class AlgoLabApiException extends AlgoLabException {
    private final int statusCode;

    public AlgoLabApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AlgoLabApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
