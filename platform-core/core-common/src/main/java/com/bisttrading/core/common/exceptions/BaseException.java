package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;
import lombok.Getter;

/**
 * Base exception class for all custom exceptions in the BIST Trading Platform.
 * Provides common fields and behavior for all business and technical exceptions.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCodes errorCode;
    private final String userMessage;
    private final Object[] messageParameters;

    /**
     * Creates a base exception with error code.
     *
     * @param errorCode The error code
     */
    protected BaseException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userMessage = errorCode.getMessage();
        this.messageParameters = new Object[0];
    }

    /**
     * Creates a base exception with error code and custom message.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     */
    protected BaseException(ErrorCodes errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.messageParameters = new Object[0];
    }

    /**
     * Creates a base exception with error code and message parameters.
     *
     * @param errorCode         The error code
     * @param messageParameters Parameters for formatting the error message
     */
    protected BaseException(ErrorCodes errorCode, Object... messageParameters) {
        super(errorCode.getFormattedMessage(messageParameters));
        this.errorCode = errorCode;
        this.userMessage = errorCode.getFormattedMessage(messageParameters);
        this.messageParameters = messageParameters;
    }

    /**
     * Creates a base exception with error code, custom message and cause.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     * @param cause       The root cause
     */
    protected BaseException(ErrorCodes errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.messageParameters = new Object[0];
    }

    /**
     * Creates a base exception with error code, message parameters and cause.
     *
     * @param errorCode         The error code
     * @param cause             The root cause
     * @param messageParameters Parameters for formatting the error message
     */
    protected BaseException(ErrorCodes errorCode, Throwable cause, Object... messageParameters) {
        super(errorCode.getFormattedMessage(messageParameters), cause);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getFormattedMessage(messageParameters);
        this.messageParameters = messageParameters;
    }

    /**
     * Returns the error code string.
     *
     * @return Error code as string
     */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    /**
     * Returns the user-friendly message.
     *
     * @return User message
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the formatted error message with parameters.
     *
     * @return Formatted message
     */
    public String getFormattedMessage() {
        return userMessage;
    }

    /**
     * Checks if this exception has message parameters.
     *
     * @return true if has parameters
     */
    public boolean hasMessageParameters() {
        return messageParameters != null && messageParameters.length > 0;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, message=%s]",
                getClass().getSimpleName(),
                errorCode.getCode(),
                userMessage);
    }
}