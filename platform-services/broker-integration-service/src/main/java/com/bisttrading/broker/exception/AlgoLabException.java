package com.bisttrading.broker.exception;

/**
 * AlgoLab API işlemlerinde oluşan hatalar için özel exception
 */
public class AlgoLabException extends RuntimeException {

    public AlgoLabException(String message) {
        super(message);
    }

    public AlgoLabException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * API hatasından AlgoLab exception oluşturur
     */
    public static AlgoLabException fromApiError(String apiErrorMessage) {
        return new AlgoLabException("AlgoLab API Hatası: " + apiErrorMessage);
    }

    /**
     * Network hatasından AlgoLab exception oluşturur
     */
    public static AlgoLabException fromNetworkError(Throwable networkError) {
        return new AlgoLabException("AlgoLab bağlantı hatası: " + networkError.getMessage(), networkError);
    }

    /**
     * Authentication hatasından AlgoLab exception oluşturur
     */
    public static AlgoLabException authenticationError(String message) {
        return new AlgoLabException("AlgoLab kimlik doğrulama hatası: " + message);
    }
}