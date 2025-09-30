package com.bisttrading.gateway.ratelimit;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.Map;

/**
 * Rate Limiting Configuration for BIST Trading Platform Gateway.
 *
 * Provides flexible rate limiting configuration supporting multiple strategies:
 * - Per user (authenticated requests)
 * - Per IP address (anonymous requests)
 * - Per API key (third-party integrations)
 * - Per endpoint (different limits per operation)
 * - Sliding window algorithm with burst handling
 */
@Data
@Builder
@Jacksonized
public class RateLimitConfig {

    // Rate Limiting Strategy
    private RateLimitStrategy strategy;

    // Basic Rate Limits
    private int requestsPerSecond;     // Sustained rate (tokens replenished per second)
    private int burstCapacity;         // Maximum burst size (bucket size)
    private int tokensPerRequest;      // Tokens consumed per request (usually 1)

    // Time Window Configuration
    private Duration windowSize;       // Time window for rate limiting (default: 1 minute)
    private Duration penaltyDuration;  // Penalty duration when limit exceeded

    // Advanced Configuration
    private boolean enableBurstProtection;  // Enable burst protection
    private double burstMultiplier;         // Multiplier for burst capacity (default: 2.0)
    private boolean enableGradualRecovery;  // Gradual recovery after rate limit hit

    // Sliding Window Configuration
    private int numberOfSubWindows;    // Number of sub-windows for sliding window
    private boolean enableSlidingWindow; // Use sliding window vs fixed window

    // Endpoint-specific Configuration
    private Map<String, EndpointRateLimit> endpointLimits;

    // Error Response Configuration
    private String rateLimitMessage;
    private Map<String, Object> customHeaders;

    /**
     * Rate limiting strategies.
     */
    public enum RateLimitStrategy {
        PER_USER,           // Rate limit per authenticated user
        PER_IP,             // Rate limit per IP address
        PER_API_KEY,        // Rate limit per API key
        PER_ENDPOINT,       // Different limits per endpoint
        COMBINED,           // Combined strategy (user + IP)
        CUSTOM              // Custom key resolver
    }

    /**
     * Endpoint-specific rate limit configuration.
     */
    @Data
    @Builder
    @Jacksonized
    public static class EndpointRateLimit {
        private String pathPattern;
        private int requestsPerSecond;
        private int burstCapacity;
        private RateLimitStrategy strategy;
        private boolean enabled;
        private String description;

        public static EndpointRateLimit trading() {
            return EndpointRateLimit.builder()
                .pathPattern("/api/v1/orders/**")
                .requestsPerSecond(10)
                .burstCapacity(20)
                .strategy(RateLimitStrategy.PER_USER)
                .enabled(true)
                .description("Trading operations - strict limits")
                .build();
        }

        public static EndpointRateLimit marketData() {
            return EndpointRateLimit.builder()
                .pathPattern("/api/v1/market-data/**")
                .requestsPerSecond(100)
                .burstCapacity(200)
                .strategy(RateLimitStrategy.PER_USER)
                .enabled(true)
                .description("Market data - high throughput")
                .build();
        }

        public static EndpointRateLimit publicData() {
            return EndpointRateLimit.builder()
                .pathPattern("/api/v1/market-data/public/**")
                .requestsPerSecond(200)
                .burstCapacity(400)
                .strategy(RateLimitStrategy.PER_IP)
                .enabled(true)
                .description("Public market data - very high throughput")
                .build();
        }

        public static EndpointRateLimit authentication() {
            return EndpointRateLimit.builder()
                .pathPattern("/api/v1/auth/**")
                .requestsPerSecond(5)
                .burstCapacity(10)
                .strategy(RateLimitStrategy.PER_IP)
                .enabled(true)
                .description("Authentication - prevent brute force")
                .build();
        }

        public static EndpointRateLimit admin() {
            return EndpointRateLimit.builder()
                .pathPattern("/api/v1/admin/**")
                .requestsPerSecond(20)
                .burstCapacity(40)
                .strategy(RateLimitStrategy.COMBINED)
                .enabled(true)
                .description("Admin operations - moderate limits")
                .build();
        }
    }

    /**
     * Default configurations for different service types.
     */
    public static class Defaults {

        public static RateLimitConfig standardUser() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.PER_USER)
                .requestsPerSecond(100)
                .burstCapacity(200)
                .tokensPerRequest(1)
                .windowSize(Duration.ofMinutes(1))
                .penaltyDuration(Duration.ofMinutes(5))
                .enableBurstProtection(true)
                .burstMultiplier(2.0)
                .enableSlidingWindow(true)
                .numberOfSubWindows(6) // 10-second sub-windows in 1-minute window
                .rateLimitMessage("Rate limit exceeded. Please reduce request frequency.")
                .build();
        }

        public static RateLimitConfig tradingUser() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.PER_USER)
                .requestsPerSecond(10)
                .burstCapacity(20)
                .tokensPerRequest(1)
                .windowSize(Duration.ofMinutes(1))
                .penaltyDuration(Duration.ofMinutes(10))
                .enableBurstProtection(true)
                .burstMultiplier(1.5)
                .enableSlidingWindow(true)
                .numberOfSubWindows(12) // 5-second sub-windows
                .rateLimitMessage("Trading rate limit exceeded. Please slow down your order placement.")
                .build();
        }

        public static RateLimitConfig publicApi() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.PER_IP)
                .requestsPerSecond(200)
                .burstCapacity(400)
                .tokensPerRequest(1)
                .windowSize(Duration.ofMinutes(1))
                .penaltyDuration(Duration.ofMinutes(2))
                .enableBurstProtection(false)
                .enableSlidingWindow(true)
                .numberOfSubWindows(6)
                .rateLimitMessage("Public API rate limit exceeded.")
                .build();
        }

        public static RateLimitConfig adminUser() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.COMBINED)
                .requestsPerSecond(50)
                .burstCapacity(100)
                .tokensPerRequest(1)
                .windowSize(Duration.ofMinutes(1))
                .penaltyDuration(Duration.ofMinutes(15))
                .enableBurstProtection(true)
                .burstMultiplier(2.0)
                .enableSlidingWindow(true)
                .numberOfSubWindows(6)
                .rateLimitMessage("Admin rate limit exceeded.")
                .build();
        }

        public static RateLimitConfig mobileClient() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.PER_USER)
                .requestsPerSecond(50)
                .burstCapacity(100)
                .tokensPerRequest(1)
                .windowSize(Duration.ofMinutes(1))
                .penaltyDuration(Duration.ofMinutes(3))
                .enableBurstProtection(true)
                .burstMultiplier(1.8)
                .enableSlidingWindow(true)
                .numberOfSubWindows(6)
                .rateLimitMessage("Mobile API rate limit exceeded.")
                .build();
        }

        public static RateLimitConfig batchOperations() {
            return RateLimitConfig.builder()
                .strategy(RateLimitStrategy.PER_USER)
                .requestsPerSecond(2)
                .burstCapacity(5)
                .tokensPerRequest(5) // Batch operations consume more tokens
                .windowSize(Duration.ofMinutes(5))
                .penaltyDuration(Duration.ofMinutes(30))
                .enableBurstProtection(true)
                .burstMultiplier(1.2)
                .enableSlidingWindow(false) // Fixed window for batch operations
                .rateLimitMessage("Batch operation rate limit exceeded. Please reduce batch frequency.")
                .build();
        }
    }

    /**
     * Get rate limit key prefix based on strategy.
     */
    public String getKeyPrefix() {
        return switch (strategy) {
            case PER_USER -> "rate_limit:user:";
            case PER_IP -> "rate_limit:ip:";
            case PER_API_KEY -> "rate_limit:api_key:";
            case PER_ENDPOINT -> "rate_limit:endpoint:";
            case COMBINED -> "rate_limit:combined:";
            case CUSTOM -> "rate_limit:custom:";
        };
    }

    /**
     * Calculate actual burst capacity with multiplier.
     */
    public int getCalculatedBurstCapacity() {
        if (!enableBurstProtection) {
            return burstCapacity;
        }
        return (int) Math.ceil(burstCapacity * burstMultiplier);
    }

    /**
     * Get sub-window duration for sliding window algorithm.
     */
    public Duration getSubWindowDuration() {
        if (!enableSlidingWindow || numberOfSubWindows <= 0) {
            return windowSize;
        }
        return windowSize.dividedBy(numberOfSubWindows);
    }

    /**
     * Check if this configuration is valid.
     */
    public boolean isValid() {
        return requestsPerSecond > 0 &&
               burstCapacity >= requestsPerSecond &&
               tokensPerRequest > 0 &&
               windowSize != null &&
               !windowSize.isNegative() &&
               strategy != null;
    }

    /**
     * Create a copy with modified parameters.
     */
    public RateLimitConfig withRequestsPerSecond(int newRate) {
        return RateLimitConfig.builder()
            .strategy(this.strategy)
            .requestsPerSecond(newRate)
            .burstCapacity(this.burstCapacity)
            .tokensPerRequest(this.tokensPerRequest)
            .windowSize(this.windowSize)
            .penaltyDuration(this.penaltyDuration)
            .enableBurstProtection(this.enableBurstProtection)
            .burstMultiplier(this.burstMultiplier)
            .enableGradualRecovery(this.enableGradualRecovery)
            .numberOfSubWindows(this.numberOfSubWindows)
            .enableSlidingWindow(this.enableSlidingWindow)
            .endpointLimits(this.endpointLimits)
            .rateLimitMessage(this.rateLimitMessage)
            .customHeaders(this.customHeaders)
            .build();
    }

    public RateLimitConfig withBurstCapacity(int newCapacity) {
        return RateLimitConfig.builder()
            .strategy(this.strategy)
            .requestsPerSecond(this.requestsPerSecond)
            .burstCapacity(newCapacity)
            .tokensPerRequest(this.tokensPerRequest)
            .windowSize(this.windowSize)
            .penaltyDuration(this.penaltyDuration)
            .enableBurstProtection(this.enableBurstProtection)
            .burstMultiplier(this.burstMultiplier)
            .enableGradualRecovery(this.enableGradualRecovery)
            .numberOfSubWindows(this.numberOfSubWindows)
            .enableSlidingWindow(this.enableSlidingWindow)
            .endpointLimits(this.endpointLimits)
            .rateLimitMessage(this.rateLimitMessage)
            .customHeaders(this.customHeaders)
            .build();
    }

    public RateLimitConfig withStrategy(RateLimitStrategy newStrategy) {
        return RateLimitConfig.builder()
            .strategy(newStrategy)
            .requestsPerSecond(this.requestsPerSecond)
            .burstCapacity(this.burstCapacity)
            .tokensPerRequest(this.tokensPerRequest)
            .windowSize(this.windowSize)
            .penaltyDuration(this.penaltyDuration)
            .enableBurstProtection(this.enableBurstProtection)
            .burstMultiplier(this.burstMultiplier)
            .enableGradualRecovery(this.enableGradualRecovery)
            .numberOfSubWindows(this.numberOfSubWindows)
            .enableSlidingWindow(this.enableSlidingWindow)
            .endpointLimits(this.endpointLimits)
            .rateLimitMessage(this.rateLimitMessage)
            .customHeaders(this.customHeaders)
            .build();
    }

    public RateLimitConfig withTokensPerRequest(int newTokens) {
        return RateLimitConfig.builder()
            .strategy(this.strategy)
            .requestsPerSecond(this.requestsPerSecond)
            .burstCapacity(this.burstCapacity)
            .tokensPerRequest(newTokens)
            .windowSize(this.windowSize)
            .penaltyDuration(this.penaltyDuration)
            .enableBurstProtection(this.enableBurstProtection)
            .burstMultiplier(this.burstMultiplier)
            .enableGradualRecovery(this.enableGradualRecovery)
            .numberOfSubWindows(this.numberOfSubWindows)
            .enableSlidingWindow(this.enableSlidingWindow)
            .endpointLimits(this.endpointLimits)
            .rateLimitMessage(this.rateLimitMessage)
            .customHeaders(this.customHeaders)
            .build();
    }
}