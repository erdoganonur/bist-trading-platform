package com.bisttrading.gateway.ratelimit;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;

/**
 * Rate Limit Result for BIST Trading Platform Gateway.
 *
 * Contains the result of a rate limiting check including:
 * - Whether the request is allowed
 * - Remaining capacity information
 * - Retry timing information
 * - Algorithm and metadata
 */
@Data
@Builder
@Jacksonized
public class RateLimitResult {

    // Decision
    private boolean allowed;              // Whether the request is allowed
    private String reason;               // Reason if request is denied

    // Capacity Information
    private long remainingTokens;        // Remaining tokens in current window
    private long totalRequests;          // Total requests in current window
    private long requestsPerSecond;      // Configured requests per second

    // Timing Information
    private Duration retryAfter;         // How long to wait before retrying
    private Duration windowSize;         // Size of the rate limiting window
    private Instant windowResetTime;     // When the current window resets

    // Algorithm Information
    private String algorithm;            // Algorithm used (sliding-window, token-bucket, etc.)
    private String rateLimitKey;         // Key used for rate limiting

    // Headers for Client Response
    private RateLimitHeaders headers;

    /**
     * Create an allowed result.
     */
    public static RateLimitResult allowed(long remainingTokens, Duration retryAfter) {
        return RateLimitResult.builder()
            .allowed(true)
            .remainingTokens(remainingTokens)
            .retryAfter(retryAfter != null ? retryAfter : Duration.ZERO)
            .build();
    }

    /**
     * Create a denied result.
     */
    public static RateLimitResult denied(String reason, Duration retryAfter) {
        return RateLimitResult.builder()
            .allowed(false)
            .reason(reason)
            .remainingTokens(0)
            .retryAfter(retryAfter != null ? retryAfter : Duration.ofMinutes(1))
            .build();
    }

    /**
     * Create an error result.
     */
    public static RateLimitResult error(String reason) {
        return RateLimitResult.builder()
            .allowed(true) // Fail open - allow request on error
            .reason(reason)
            .remainingTokens(-1) // Indicate error state
            .retryAfter(Duration.ZERO)
            .algorithm("error")
            .build();
    }

    /**
     * Generate HTTP headers for rate limiting information.
     */
    public RateLimitHeaders generateHeaders() {
        if (headers != null) {
            return headers;
        }

        RateLimitHeaders.RateLimitHeadersBuilder builder = RateLimitHeaders.builder();

        // Standard rate limit headers
        if (requestsPerSecond > 0) {
            builder.xRateLimitLimit(String.valueOf(requestsPerSecond));
        }

        builder.xRateLimitRemaining(String.valueOf(Math.max(0, remainingTokens)))
            .xRateLimitReset(String.valueOf(
                windowResetTime != null ? windowResetTime.getEpochSecond() :
                Instant.now().plus(retryAfter).getEpochSecond()
            ));

        // Retry-After header for denied requests
        if (!allowed && retryAfter != null && !retryAfter.isZero()) {
            builder.retryAfter(String.valueOf(retryAfter.getSeconds()));
        }

        // Custom headers with additional information
        builder.xRateLimitAlgorithm(algorithm != null ? algorithm : "unknown")
            .xRateLimitWindow(windowSize != null ? String.valueOf(windowSize.getSeconds()) : "60");

        // BIST-specific headers
        builder.xBistRateLimitStatus(allowed ? "allowed" : "denied");

        if (reason != null) {
            builder.xBistRateLimitReason(reason);
        }

        this.headers = builder.build();
        return this.headers;
    }

    /**
     * Check if this result indicates an error state.
     */
    public boolean isError() {
        return remainingTokens < 0 || "error".equals(algorithm);
    }

    /**
     * Get user-friendly message for the result.
     */
    public String getUserMessage() {
        if (allowed) {
            return "Request allowed";
        }

        if (reason != null) {
            return reason;
        }

        if (retryAfter != null && !retryAfter.isZero()) {
            return String.format("Rate limit exceeded. Please try again in %d seconds.",
                retryAfter.getSeconds());
        }

        return "Rate limit exceeded. Please reduce request frequency.";
    }

    /**
     * Get technical details for logging.
     */
    public String getTechnicalDetails() {
        return String.format(
            "RateLimit[key=%s, allowed=%s, remaining=%d, algorithm=%s, retryAfter=%s]",
            rateLimitKey, allowed, remainingTokens, algorithm,
            retryAfter != null ? retryAfter.toString() : "none"
        );
    }

    /**
     * Check if burst protection was triggered.
     */
    public boolean isBurstProtectionTriggered() {
        return !allowed && totalRequests > requestsPerSecond &&
               (reason == null || reason.contains("burst"));
    }

    /**
     * Get percentage of capacity used.
     */
    public double getCapacityUsedPercentage() {
        if (requestsPerSecond <= 0) return 0.0;
        return Math.min(100.0, (double) totalRequests / requestsPerSecond * 100.0);
    }

    /**
     * HTTP Headers for rate limiting information.
     */
    @Data
    @Builder
    @Jacksonized
    public static class RateLimitHeaders {
        // Standard headers (RFC 6585)
        private String xRateLimitLimit;      // "X-RateLimit-Limit"
        private String xRateLimitRemaining;  // "X-RateLimit-Remaining"
        private String xRateLimitReset;      // "X-RateLimit-Reset"
        private String retryAfter;           // "Retry-After"

        // Algorithm-specific headers
        private String xRateLimitAlgorithm;  // "X-RateLimit-Algorithm"
        private String xRateLimitWindow;     // "X-RateLimit-Window"

        // BIST-specific headers
        private String xBistRateLimitStatus; // "X-BIST-RateLimit-Status"
        private String xBistRateLimitReason; // "X-BIST-RateLimit-Reason"
        private String xBistRateLimitKey;    // "X-BIST-RateLimit-Key" (for debugging)

        /**
         * Convert to Map for easy header setting.
         */
        public java.util.Map<String, String> toMap() {
            java.util.Map<String, String> headerMap = new java.util.HashMap<>();

            if (xRateLimitLimit != null) headerMap.put("X-RateLimit-Limit", xRateLimitLimit);
            if (xRateLimitRemaining != null) headerMap.put("X-RateLimit-Remaining", xRateLimitRemaining);
            if (xRateLimitReset != null) headerMap.put("X-RateLimit-Reset", xRateLimitReset);
            if (retryAfter != null) headerMap.put("Retry-After", retryAfter);

            if (xRateLimitAlgorithm != null) headerMap.put("X-RateLimit-Algorithm", xRateLimitAlgorithm);
            if (xRateLimitWindow != null) headerMap.put("X-RateLimit-Window", xRateLimitWindow);

            if (xBistRateLimitStatus != null) headerMap.put("X-BIST-RateLimit-Status", xBistRateLimitStatus);
            if (xBistRateLimitReason != null) headerMap.put("X-BIST-RateLimit-Reason", xBistRateLimitReason);
            if (xBistRateLimitKey != null) headerMap.put("X-BIST-RateLimit-Key", xBistRateLimitKey);

            return headerMap;
        }
    }
}

