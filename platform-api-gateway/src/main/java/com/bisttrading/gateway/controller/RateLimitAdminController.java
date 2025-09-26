package com.bisttrading.gateway.controller;

import com.bisttrading.gateway.ratelimit.RateLimitConfig;
import com.bisttrading.gateway.ratelimit.RedisRateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Admin Controller for Rate Limiting Management in BIST Trading Platform Gateway.
 *
 * Provides administrative endpoints for:
 * - Monitoring rate limit status
 * - Resetting rate limits
 * - Getting statistics
 * - Dynamic configuration updates
 */
@Slf4j
@RestController
@RequestMapping("/admin/rate-limits")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final RedisRateLimitingService rateLimitingService;

    /**
     * Get rate limit status for a specific key.
     */
    @GetMapping("/status/{keyType}/{keyValue}")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimitStatus(
        @PathVariable String keyType,
        @PathVariable String keyValue,
        @RequestParam(defaultValue = "default") String configName
    ) {
        String rateLimitKey = keyValue;
        RateLimitConfig config = getConfigByName(configName);

        return rateLimitingService.getStatus(rateLimitKey, config)
            .map(status -> {
                Map<String, Object> configuration = new java.util.HashMap<>();
                configuration.put("requestsPerSecond", config.getRequestsPerSecond());
                configuration.put("burstCapacity", config.getBurstCapacity());
                configuration.put("windowSize", config.getWindowSize().toString());
                configuration.put("algorithm", config.isEnableSlidingWindow() ? "sliding-window" : "fixed-window");

                Map<String, Object> response = new java.util.HashMap<>();
                response.put("timestamp", Instant.now());
                response.put("keyType", keyType);
                response.put("keyValue", keyValue);
                response.put("status", status);
                response.put("configuration", configuration);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error in rate limit operation", error);
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", "Failed to get status");
                return Mono.just(ResponseEntity.<Map<String, Object>>status(500).body(errorResponse));
            });
    }

    /**
     * Reset rate limit for a specific key.
     */
    @DeleteMapping("/reset/{keyType}/{keyValue}")
    public Mono<ResponseEntity<Map<String, Object>>> resetRateLimit(
        @PathVariable String keyType,
        @PathVariable String keyValue,
        @RequestParam(defaultValue = "default") String configName
    ) {
        String rateLimitKey = keyValue;
        RateLimitConfig config = getConfigByName(configName);

        log.info("Admin reset rate limit for key: {} (type: {})", rateLimitKey, keyType);

        return rateLimitingService.resetRateLimit(rateLimitKey, config)
            .then(Mono.fromCallable(() -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("timestamp", Instant.now());
                response.put("message", "Rate limit reset successfully");
                response.put("keyType", keyType);
                response.put("keyValue", keyValue);
                response.put("configName", configName);
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(error -> {
                log.error("Error in rate limit operation", error);
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", "Failed to reset rate limit");
                return Mono.just(ResponseEntity.<Map<String, Object>>status(500).body(errorResponse));
            });
    }

    /**
     * Get rate limiting statistics for a key pattern.
     */
    @GetMapping("/statistics")
    public Mono<ResponseEntity<Map<String, Object>>> getStatistics(
        @RequestParam String keyPattern
    ) {
        return rateLimitingService.getStatistics(keyPattern)
            .map(statistics -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("timestamp", Instant.now());
                response.put("keyPattern", keyPattern);
                response.put("statistics", statistics);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error in rate limit operation", error);
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", "Failed to get statistics");
                return Mono.just(ResponseEntity.<Map<String, Object>>status(500).body(errorResponse));
            });
    }

    /**
     * Test rate limiting for a specific key (dry run).
     */
    @PostMapping("/test/{keyType}/{keyValue}")
    public Mono<ResponseEntity<Map<String, Object>>> testRateLimit(
        @PathVariable String keyType,
        @PathVariable String keyValue,
        @RequestParam(defaultValue = "default") String configName,
        @RequestParam(defaultValue = "1") int tokenCount
    ) {
        String rateLimitKey = keyValue;
        RateLimitConfig config = getConfigByName(configName)
            .withTokensPerRequest(tokenCount);

        return rateLimitingService.isAllowed(rateLimitKey, config)
            .map(result -> {
                Map<String, Object> response = Map.of(
                    "timestamp", Instant.now(),
                    "keyType", keyType,
                    "keyValue", keyValue,
                    "configName", configName,
                    "tokenCount", tokenCount,
                    "result", Map.of(
                        "allowed", result.isAllowed(),
                        "remainingTokens", result.getRemainingTokens(),
                        "retryAfterMs", result.getRetryAfter().toMillis(),
                        "algorithm", result.getAlgorithm(),
                        "userMessage", result.getUserMessage()
                    )
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Error in rate limit operation", error);
                return Mono.just(ResponseEntity.<Map<String, Object>>internalServerError().build());
            });
    }

    /**
     * Get all available rate limit configurations.
     */
    @GetMapping("/configurations")
    public ResponseEntity<Map<String, Object>> getConfigurations() {
        Map<String, Object> configurations = Map.of(
            "timestamp", Instant.now(),
            "configurations", Map.of(
                "default", RateLimitConfig.Defaults.standardUser(),
                "trading", RateLimitConfig.Defaults.tradingUser(),
                "public-api", RateLimitConfig.Defaults.publicApi(),
                "admin", RateLimitConfig.Defaults.adminUser(),
                "mobile", RateLimitConfig.Defaults.mobileClient(),
                "batch", RateLimitConfig.Defaults.batchOperations()
            )
        );

        return ResponseEntity.ok(configurations);
    }

    /**
     * Health check for rate limiting service.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        // Test with a health check key
        String healthKey = "health-check-" + System.currentTimeMillis();
        RateLimitConfig healthConfig = RateLimitConfig.Defaults.standardUser();

        return rateLimitingService.isAllowed(healthKey, healthConfig)
            .map(result -> {
                Map<String, Object> health = Map.of(
                    "timestamp", Instant.now(),
                    "status", "UP",
                    "rateLimitingService", "operational",
                    "testResult", Map.of(
                        "allowed", result.isAllowed(),
                        "algorithm", result.getAlgorithm()
                    )
                );
                return ResponseEntity.ok(health);
            })
            .onErrorResume(error -> {
                log.error("Rate limiting health check failed", error);
                Map<String, Object> health = Map.of(
                    "timestamp", Instant.now(),
                    "status", "DOWN",
                    "rateLimitingService", "error",
                    "error", error.getMessage()
                );
                return Mono.just(ResponseEntity.status(503).body(health));
            });
    }

    /**
     * Get configuration by name.
     */
    private RateLimitConfig getConfigByName(String configName) {
        return switch (configName.toLowerCase()) {
            case "trading" -> RateLimitConfig.Defaults.tradingUser();
            case "public-api", "public" -> RateLimitConfig.Defaults.publicApi();
            case "admin" -> RateLimitConfig.Defaults.adminUser();
            case "mobile" -> RateLimitConfig.Defaults.mobileClient();
            case "batch" -> RateLimitConfig.Defaults.batchOperations();
            default -> RateLimitConfig.Defaults.standardUser();
        };
    }
}