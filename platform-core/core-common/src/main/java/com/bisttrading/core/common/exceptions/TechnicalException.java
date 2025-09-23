package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;

/**
 * Exception thrown when technical issues occur.
 * This exception represents system-level errors such as database connectivity,
 * external service failures, configuration issues, etc.
 */
public class TechnicalException extends BaseException {

    /**
     * Creates a technical exception with error code.
     *
     * @param errorCode The error code
     */
    public TechnicalException(ErrorCodes errorCode) {
        super(errorCode);
    }

    /**
     * Creates a technical exception with error code and custom message.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     */
    public TechnicalException(ErrorCodes errorCode, String userMessage) {
        super(errorCode, userMessage);
    }

    /**
     * Creates a technical exception with error code and message parameters.
     *
     * @param errorCode         The error code
     * @param messageParameters Parameters for formatting the error message
     */
    public TechnicalException(ErrorCodes errorCode, Object... messageParameters) {
        super(errorCode, messageParameters);
    }

    /**
     * Creates a technical exception with error code, custom message and cause.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     * @param cause       The root cause
     */
    public TechnicalException(ErrorCodes errorCode, String userMessage, Throwable cause) {
        super(errorCode, userMessage, cause);
    }

    /**
     * Creates a technical exception with error code, message parameters and cause.
     *
     * @param errorCode         The error code
     * @param cause             The root cause
     * @param messageParameters Parameters for formatting the error message
     */
    public TechnicalException(ErrorCodes errorCode, Throwable cause, Object... messageParameters) {
        super(errorCode, cause, messageParameters);
    }

    // Convenience factory methods for common technical exceptions

    /**
     * Creates a technical exception for database connectivity issues.
     *
     * @param cause The underlying database exception
     * @return TechnicalException
     */
    public static TechnicalException databaseError(Throwable cause) {
        return new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                "Veritabanı bağlantı hatası", cause);
    }

    /**
     * Creates a technical exception for external service failures.
     *
     * @param serviceName The name of the external service
     * @param cause       The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException externalServiceError(String serviceName, Throwable cause) {
        return new TechnicalException(ErrorCodes.EXTERNAL_SERVICE_ERROR,
                String.format("%s servisi ile bağlantı hatası", serviceName), cause);
    }

    /**
     * Creates a technical exception for BIST API errors.
     *
     * @param cause The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException bistApiError(Throwable cause) {
        return new TechnicalException(ErrorCodes.BIST_API_ERROR,
                "BIST API bağlantı hatası", cause);
    }

    /**
     * Creates a technical exception for bank API errors.
     *
     * @param bankName The bank name
     * @param cause    The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException bankApiError(String bankName, Throwable cause) {
        return new TechnicalException(ErrorCodes.BANK_API_ERROR,
                String.format("%s banka API hatası", bankName), cause);
    }

    /**
     * Creates a technical exception for SMS service errors.
     *
     * @param cause The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException smsServiceError(Throwable cause) {
        return new TechnicalException(ErrorCodes.SMS_SERVICE_ERROR,
                "SMS servisi hatası", cause);
    }

    /**
     * Creates a technical exception for email service errors.
     *
     * @param cause The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException emailServiceError(Throwable cause) {
        return new TechnicalException(ErrorCodes.EMAIL_SERVICE_ERROR,
                "E-posta servisi hatası", cause);
    }

    /**
     * Creates a technical exception for configuration errors.
     *
     * @param configProperty The configuration property that caused the error
     * @return TechnicalException
     */
    public static TechnicalException configurationError(String configProperty) {
        return new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                String.format("Konfigürasyon hatası: %s", configProperty));
    }

    /**
     * Creates a technical exception for service unavailable scenarios.
     *
     * @param serviceName The name of the unavailable service
     * @return TechnicalException
     */
    public static TechnicalException serviceUnavailable(String serviceName) {
        return new TechnicalException(ErrorCodes.SERVICE_UNAVAILABLE,
                String.format("%s servisi şu anda kullanılamıyor", serviceName));
    }

    /**
     * Creates a technical exception for timeout scenarios.
     *
     * @param operation The operation that timed out
     * @param timeout   The timeout duration
     * @return TechnicalException
     */
    public static TechnicalException timeoutError(String operation, String timeout) {
        return new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                String.format("%s işlemi zaman aşımına uğradı (%s)", operation, timeout));
    }

    /**
     * Creates a technical exception for serialization/deserialization errors.
     *
     * @param dataType The type of data being processed
     * @param cause    The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException serializationError(String dataType, Throwable cause) {
        return new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                String.format("%s veri işleme hatası", dataType), cause);
    }

    /**
     * Creates a technical exception for security-related technical issues.
     *
     * @param operation The security operation that failed
     * @param cause     The underlying exception
     * @return TechnicalException
     */
    public static TechnicalException securityError(String operation, Throwable cause) {
        return new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                String.format("Güvenlik hatası: %s", operation), cause);
    }
}