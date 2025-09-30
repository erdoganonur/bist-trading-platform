package com.bisttrading.marketdata.exception;

/**
 * AlgoLab WebSocket operations için özel exception
 */
public class AlgoLabWebSocketException extends RuntimeException {

    public AlgoLabWebSocketException(String message) {
        super(message);
    }

    public AlgoLabWebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * WebSocket bağlantı hatasından exception oluşturur
     */
    public static AlgoLabWebSocketException connectionError(String message) {
        return new AlgoLabWebSocketException("WebSocket bağlantı hatası: " + message);
    }

    /**
     * Authentication hatasından exception oluşturur
     */
    public static AlgoLabWebSocketException authenticationError(String message) {
        return new AlgoLabWebSocketException("WebSocket kimlik doğrulama hatası: " + message);
    }

    /**
     * Mesaj işleme hatasından exception oluşturur
     */
    public static AlgoLabWebSocketException messageError(String message) {
        return new AlgoLabWebSocketException("WebSocket mesaj hatası: " + message);
    }

    /**
     * Subscription hatasından exception oluşturur
     */
    public static AlgoLabWebSocketException subscriptionError(String message) {
        return new AlgoLabWebSocketException("WebSocket subscription hatası: " + message);
    }
}