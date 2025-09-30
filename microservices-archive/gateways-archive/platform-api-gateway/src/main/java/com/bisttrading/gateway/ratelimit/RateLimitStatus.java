package com.bisttrading.gateway.ratelimit;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Rate Limit Status for monitoring and status checks.
 */
@Data
@Builder
@Jacksonized
public class RateLimitStatus {
    private String rateLimitKey;
    private long currentRequests;
    private long remainingCapacity;
    private Instant resetTime;
    private RateLimitConfig config;
    private boolean error;
    private String errorMessage;

    public static RateLimitStatus error(String message) {
        return RateLimitStatus.builder()
            .error(true)
            .errorMessage(message)
            .build();
    }
}