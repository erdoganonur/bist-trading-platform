package com.bisttrading.broker.algolab.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for AlgoLab API integration.
 */
@Component
@ConfigurationProperties(prefix = "algolab")
@Validated
@Data
public class AlgoLabProperties {

    private ApiConfig api = new ApiConfig();
    private AuthConfig auth = new AuthConfig();
    private SessionConfig session = new SessionConfig();

    @Data
    public static class ApiConfig {
        @NotBlank(message = "AlgoLab API key is required")
        private String key;

        @NotBlank(message = "AlgoLab hostname is required")
        private String hostname = "https://www.algolab.com.tr";

        private String url = "https://www.algolab.com.tr/api";

        private String websocketUrl = "wss://www.algolab.com.tr/api/ws";

        /**
         * Rate limit: requests per second (0.2 = 1 request per 5 seconds)
         */
        private double rateLimit = 0.2;
    }

    @Data
    public static class AuthConfig {
        @NotBlank(message = "AlgoLab username (TC Kimlik No) is required")
        private String username;

        @NotBlank(message = "AlgoLab password (Denizbank) is required")
        private String password;

        /**
         * Enable auto-login on startup
         */
        private boolean autoLogin = true;

        /**
         * Enable keep-alive (session refresh every 5 minutes)
         */
        private boolean keepAlive = true;

        /**
         * Session refresh interval in milliseconds (default: 5 minutes)
         */
        private long refreshIntervalMs = 300000L;
    }

    @Data
    public static class SessionConfig {
        /**
         * Session storage type: file or database
         */
        private String storage = "database";

        /**
         * File path for session storage (if storage=file)
         */
        private String filePath = "./algolab-session.json";

        /**
         * Session expiration time in hours (default: 24 hours)
         */
        private int expirationHours = 24;

        /**
         * Session retention period in days (for cleanup of old inactive sessions)
         * Default: 30 days
         */
        private int retentionDays = 30;

        /**
         * Cleanup cron expression
         * Default: Every hour (0 0 * * * *)
         */
        private String cleanupCron = "0 0 * * * *";

        /**
         * Enable automatic session cleanup
         */
        private boolean autoCleanup = true;
    }

    @Data
    public static class WebSocketConfig {
        /**
         * Enable WebSocket connection
         */
        private boolean enabled = true;

        /**
         * WebSocket URL
         */
        private String url = "wss://www.algolab.com.tr/api/ws";

        /**
         * Auto-connect on startup
         */
        private boolean autoConnect = true;

        /**
         * Heartbeat/ping interval in milliseconds (default: 15 minutes = 900000ms)
         */
        private long heartbeatInterval = 900000L;

        /**
         * Connection timeout in milliseconds
         */
        private long connectionTimeout = 30000L;

        /**
         * Reconnect configuration
         */
        private ReconnectConfig reconnect = new ReconnectConfig();

        @Data
        public static class ReconnectConfig {
            /**
             * Enable auto-reconnect
             */
            private boolean enabled = true;

            /**
             * Initial delay before first reconnect attempt (milliseconds)
             */
            private long initialDelay = 1000L;

            /**
             * Maximum delay between reconnect attempts (milliseconds)
             */
            private long maxDelay = 60000L;

            /**
             * Multiplier for exponential backoff
             */
            private double multiplier = 2.0;

            /**
             * Maximum number of reconnect attempts (0 = unlimited)
             */
            private int maxAttempts = 0;
        }
    }

    private WebSocketConfig websocket = new WebSocketConfig();

    @Data
    public static class ResilienceConfig {
        /**
         * Enable resilience patterns (circuit breaker, retry, time limiter)
         */
        private boolean enabled = true;

        /**
         * Circuit breaker configuration
         */
        private CircuitBreakerSettings circuitBreaker = new CircuitBreakerSettings();

        /**
         * Retry configuration
         */
        private RetrySettings retry = new RetrySettings();

        /**
         * Time limiter configuration
         */
        private TimeLimiterSettings timeLimiter = new TimeLimiterSettings();

        @Data
        public static class CircuitBreakerSettings {
            /**
             * Failure rate threshold (percentage)
             */
            private int failureRateThreshold = 50;

            /**
             * Slow call duration threshold (milliseconds)
             */
            private long slowCallDurationThreshold = 5000L;

            /**
             * Slow call rate threshold (percentage)
             */
            private int slowCallRateThreshold = 100;

            /**
             * Wait duration in open state (milliseconds)
             */
            private long waitDurationInOpenState = 60000L;

            /**
             * Permitted number of calls in half-open state
             */
            private int permittedNumberOfCallsInHalfOpenState = 10;

            /**
             * Minimum number of calls before calculating error rate
             */
            private int minimumNumberOfCalls = 5;

            /**
             * Sliding window size
             */
            private int slidingWindowSize = 100;
        }

        @Data
        public static class RetrySettings {
            /**
             * Maximum number of retry attempts
             */
            private int maxAttempts = 3;

            /**
             * Initial wait duration between retries (milliseconds)
             */
            private long waitDuration = 2000L;

            /**
             * Enable exponential backoff
             */
            private boolean enableExponentialBackoff = true;

            /**
             * Exponential backoff multiplier
             */
            private double exponentialBackoffMultiplier = 2.0;
        }

        @Data
        public static class TimeLimiterSettings {
            /**
             * Timeout duration (milliseconds)
             */
            private long timeoutDuration = 10000L;

            /**
             * Cancel running future on timeout
             */
            private boolean cancelRunningFuture = true;
        }
    }

    private ResilienceConfig resilience = new ResilienceConfig();

    /**
     * Gets the API key code (without "API-" prefix)
     */
    public String getApiCode() {
        String key = api.getKey();
        return key.startsWith("API-") ? key.substring(4) : key;
    }
}