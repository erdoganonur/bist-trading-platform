package com.bisttrading.broker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AlgoLab API configuration properties
 * Python karşılığı: config.py dosyasındaki ayarlar
 */
@Data
@Component
@ConfigurationProperties(prefix = "bist.integration.algolab")
public class AlgoLabProperties {

    private boolean enabled = true;
    private String baseUrl = "https://www.algolab.com.tr";
    private String hostname = "www.algolab.com.tr";
    private String apiUrl = "https://www.algolab.com.tr/api";
    private String socketUrl = "wss://www.algolab.com.tr/api/ws";

    // API anahtarları
    private String apiKey;
    private String username; // TC Kimlik No
    private String password; // DenizBank hesap şifresi

    // Timeout ayarları
    private Timeout timeout = new Timeout();

    // Rate limiting
    private RateLimit rateLimit = new RateLimit();

    // Retry ayarları
    private Retry retry = new Retry();

    // Security ayarları
    private Security security = new Security();

    // Session ayarları
    private Session session = new Session();

    // Circuit breaker ayarları
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class Timeout {
        private Duration connect = Duration.ofSeconds(15);
        private Duration read = Duration.ofSeconds(30);
        private Duration write = Duration.ofSeconds(10);
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 1000;
        private int burstCapacity = 100;
        private Duration requestDelay = Duration.ofMillis(5000); // Python: 5 saniye bekleme
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private Duration backoffDelay = Duration.ofSeconds(1);
        private double backoffMultiplier = 2.0;
    }

    @Data
    public static class Security {
        private boolean sslVerification = true;
        private String certificatePath;
    }

    @Data
    public static class Session {
        private boolean autoLogin = true;
        private boolean keepAlive = false;
        private Duration keepAliveInterval = Duration.ofMinutes(5);
        private Duration sessionTimeout = Duration.ofHours(24);
        private String dataFilePath = "./algolab-session-data.json";
    }

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int slidingWindowSize = 20;
        private int minimumNumberOfCalls = 10;
    }

    /**
     * API Key'den API Code'u çıkarır (Python'daki gibi)
     */
    public String getApiCode() {
        if (apiKey != null && apiKey.startsWith("API-")) {
            return apiKey.substring(4);
        }
        return apiKey;
    }

    /**
     * API Key'i formatlar (Python'daki gibi)
     */
    public String getFormattedApiKey() {
        if (apiKey != null && !apiKey.startsWith("API-")) {
            return "API-" + apiKey;
        }
        return apiKey;
    }
}