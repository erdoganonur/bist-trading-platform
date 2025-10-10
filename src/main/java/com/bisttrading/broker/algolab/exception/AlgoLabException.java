package com.bisttrading.broker.algolab.exception;

/**
 * Base exception for AlgoLab API errors.
 */
public class AlgoLabException extends RuntimeException {
    public AlgoLabException(String message) {
        super(message);
    }

    public AlgoLabException(String message, Throwable cause) {
        super(message, cause);
    }
}