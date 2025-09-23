package com.bisttrading.user.exception;

/**
 * Base exception for user service related errors.
 * Used for business logic validation and user-specific errors.
 */
public class UserServiceException extends RuntimeException {

    public UserServiceException(String message) {
        super(message);
    }

    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}