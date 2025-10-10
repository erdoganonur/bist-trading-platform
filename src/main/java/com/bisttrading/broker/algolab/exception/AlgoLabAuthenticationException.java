package com.bisttrading.broker.algolab.exception;

/**
 * Exception thrown when AlgoLab authentication fails.
 */
public class AlgoLabAuthenticationException extends AlgoLabException {
    public AlgoLabAuthenticationException(String message) {
        super(message);
    }

    public AlgoLabAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
