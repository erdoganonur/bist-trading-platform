package com.bisttrading.broker.algolab.exception;

public class AlgoLabAuthenticationException extends AlgoLabException {

    public AlgoLabAuthenticationException(String message) {
        super(message, "AUTHENTICATION_ERROR", 401);
    }

    public AlgoLabAuthenticationException(String message, Throwable cause) {
        super(message, "AUTHENTICATION_ERROR", 401, cause);
    }
}