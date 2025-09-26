package com.bisttrading.gateway.ratelimit;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Rate Limit Statistics for monitoring and analytics.
 */
@Data
@Builder
@Jacksonized
public class RateLimitStatistics {
    private int totalKeys;
    private long totalRequests;
    private double averageRequests;
    private String keyPattern;
    private boolean error;
    private String errorMessage;
    private Instant generatedAt;

    public static RateLimitStatistics empty() {
        return RateLimitStatistics.builder()
            .totalKeys(0)
            .totalRequests(0)
            .averageRequests(0.0)
            .generatedAt(Instant.now())
            .build();
    }

    public static RateLimitStatistics error(String message) {
        return RateLimitStatistics.builder()
            .error(true)
            .errorMessage(message)
            .generatedAt(Instant.now())
            .build();
    }
}