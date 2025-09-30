package com.bisttrading.broker.adapter;

import java.util.List;
import java.util.Map;

/**
 * Broker entegrasyon adapter interface'i.
 * Farklı broker'lar için ortak API sağlar.
 */
public interface BrokerAdapter {

    /**
     * Broker'a kimlik doğrulama yapar.
     *
     * @param credentials Kimlik doğrulama bilgileri (username, password, vb.)
     * @return Başarı durumu ve session bilgileri
     * @throws BrokerAuthenticationException Kimlik doğrulama hatası
     */
    AuthenticationResult authenticate(Map<String, String> credentials);

    /**
     * Broker'a emir gönderir.
     *
     * @param orderData Gönderilecek emir bilgileri (Map format)
     * @return Emir gönderim sonucu
     * @throws BrokerOrderException Emir gönderim hatası
     */
    OrderResult sendOrder(Map<String, Object> orderData);

    /**
     * Broker'da mevcut emri iptal eder.
     *
     * @param orderId İptal edilecek emir ID'si
     * @return İptal işlemi sonucu
     * @throws BrokerOrderException Emir iptal hatası
     */
    CancelOrderResult cancelOrder(String orderId);

    /**
     * Belirtilen sembol için piyasa verisi alır.
     *
     * @param symbol Piyasa verisi istenilen sembol
     * @return Piyasa verisi
     * @throws BrokerMarketDataException Piyasa verisi alma hatası
     */
    MarketDataResult getMarketData(String symbol);

    /**
     * Mevcut pozisyonları alır.
     *
     * @return Pozisyon listesi (Map format)
     * @throws BrokerPositionException Pozisyon alma hatası
     */
    List<Map<String, Object>> getPositions();

    /**
     * Broker bağlantısını test eder.
     *
     * @return Bağlantı durumu
     */
    boolean isConnected();

    /**
     * Broker bağlantısını kapatır.
     */
    void disconnect();

    /**
     * Kimlik doğrulama sonucu
     */
    record AuthenticationResult(
        boolean success,
        String sessionId,
        String accessToken,
        long expiresAt,
        String errorMessage
    ) {}

    /**
     * Emir gönderim sonucu
     */
    record OrderResult(
        boolean success,
        String orderId,
        String brokerOrderId,
        OrderStatus status,
        String errorMessage
    ) {}

    /**
     * Emir iptal sonucu
     */
    record CancelOrderResult(
        boolean success,
        String orderId,
        OrderStatus newStatus,
        String errorMessage
    ) {}

    /**
     * Piyasa verisi sonucu
     */
    record MarketDataResult(
        String symbol,
        double lastPrice,
        double bidPrice,
        double askPrice,
        long volume,
        long timestamp,
        boolean success,
        String errorMessage
    ) {}

    /**
     * Emir durumu enum'ı
     */
    enum OrderStatus {
        PENDING,
        SUBMITTED,
        PARTIALLY_FILLED,
        FILLED,
        CANCELLED,
        REJECTED,
        EXPIRED
    }

    /**
     * Broker kimlik doğrulama hatası
     */
    class BrokerAuthenticationException extends RuntimeException {
        public BrokerAuthenticationException(String message) {
            super(message);
        }

        public BrokerAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Broker emir hatası
     */
    class BrokerOrderException extends RuntimeException {
        public BrokerOrderException(String message) {
            super(message);
        }

        public BrokerOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Broker piyasa verisi hatası
     */
    class BrokerMarketDataException extends RuntimeException {
        public BrokerMarketDataException(String message) {
            super(message);
        }

        public BrokerMarketDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Broker pozisyon hatası
     */
    class BrokerPositionException extends RuntimeException {
        public BrokerPositionException(String message) {
            super(message);
        }

        public BrokerPositionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}