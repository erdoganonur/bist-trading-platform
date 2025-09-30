package com.bisttrading.gateway.filter;

import com.bisttrading.gateway.ratelimit.RateLimitConfig;
import com.bisttrading.gateway.ratelimit.RateLimitResult;
import com.bisttrading.gateway.ratelimit.RedisRateLimitingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Rate Limiting Gateway Filter Factory for BIST Trading Platform.
 *
 * Provides comprehensive rate limiting with configurable strategies:
 * - Per User (authenticated requests)
 * - Per IP Address (anonymous requests)
 * - Per API Key (third-party integrations)
 * - Per Endpoint (different limits per operation)
 * - Combined strategies
 */
@Slf4j
@Component
public class RateLimitingGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitingGatewayFilterFactory.Config> {

    private final RedisRateLimitingService rateLimitingService;

    public RateLimitingGatewayFilterFactory(RedisRateLimitingService rateLimitingService) {
        super(Config.class);
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("enabled", "configName", "keyResolver", "skipPaths");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            String path = exchange.getRequest().getPath().value();

            // Skip rate limiting for configured paths
            if (config.getSkipPaths().stream().anyMatch(path::matches)) {
                log.debug("Skipping rate limiting for path: {}", path);
                return chain.filter(exchange);
            }

            // Resolve rate limit key based on strategy
            String rateLimitKey = resolveRateLimitKey(exchange, config);
            if (rateLimitKey == null) {
                log.warn("Could not resolve rate limit key for request: {}", path);
                return chain.filter(exchange); // Allow request if key cannot be resolved
            }

            // Get rate limit configuration
            RateLimitConfig rateLimitConfig = getRateLimitConfig(config, path);

            // Check rate limit
            return rateLimitingService.isAllowed(rateLimitKey, rateLimitConfig)
                .flatMap(result -> {
                    // Add rate limit headers
                    addRateLimitHeaders(exchange, result);

                    if (result.isAllowed()) {
                        // Request allowed, continue with filter chain
                        log.debug("Rate limit check passed for key: {} (remaining: {})",
                            rateLimitKey, result.getRemainingTokens());
                        return chain.filter(exchange);
                    } else {
                        // Rate limit exceeded
                        return handleRateLimitExceeded(exchange, result);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Rate limiting error for key: " + rateLimitKey, error);
                    // On error, allow the request (fail-open strategy)
                    return chain.filter(exchange);
                });
        };
    }

    /**
     * Resolve rate limit key based on configured strategy.
     */
    private String resolveRateLimitKey(
        org.springframework.web.server.ServerWebExchange exchange,
        Config config
    ) {
        return switch (config.getKeyResolver()) {
            case "user" -> {
                // Try to get user ID from JWT claims
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
                if (userId != null) {
                    yield userId;
                }
                // Fallback to IP address for anonymous requests
                yield getClientIpAddress(exchange);
            }
            case "ip" -> getClientIpAddress(exchange);
            case "api-key" -> {
                String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
                if (apiKey != null) {
                    yield apiKey;
                }
                // Fallback to IP address
                yield getClientIpAddress(exchange);
            }
            case "endpoint" -> {
                String path = exchange.getRequest().getPath().value();
                String method = exchange.getRequest().getMethod().name();
                yield method + ":" + path;
            }
            case "combined" -> {
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
                String ip = getClientIpAddress(exchange);
                if (userId != null) {
                    yield userId + ":" + ip;
                }
                yield ip;
            }
            default -> getClientIpAddress(exchange);
        };
    }

    /**
     * Get client IP address from request headers or connection info.
     */
    private String getClientIpAddress(org.springframework.web.server.ServerWebExchange exchange) {
        var request = exchange.getRequest();

        // Check X-Forwarded-For header first
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // Take the first IP in case of multiple IPs
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp;
        }

        // Fallback to remote address
        return request.getRemoteAddress() != null ?
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Get rate limit configuration based on path and config name.
     */
    private RateLimitConfig getRateLimitConfig(Config config, String path) {
        return switch (config.getConfigName()) {
            case "trading" -> RateLimitConfig.Defaults.tradingUser();
            case "market-data" -> RateLimitConfig.Defaults.standardUser();
            case "public-api" -> RateLimitConfig.Defaults.publicApi();
            case "admin" -> RateLimitConfig.Defaults.adminUser();
            case "mobile" -> RateLimitConfig.Defaults.mobileClient();
            case "batch" -> RateLimitConfig.Defaults.batchOperations();
            default -> {
                // Path-based configuration
                if (path.startsWith("/api/v1/orders")) {
                    yield RateLimitConfig.Defaults.tradingUser();
                } else if (path.startsWith("/api/v1/market-data/public")) {
                    yield RateLimitConfig.Defaults.publicApi();
                } else if (path.startsWith("/api/v1/market-data")) {
                    yield RateLimitConfig.Defaults.standardUser();
                } else if (path.startsWith("/api/v1/admin")) {
                    yield RateLimitConfig.Defaults.adminUser();
                } else if (path.startsWith("/api/v1/auth")) {
                    yield RateLimitConfig.Defaults.publicApi()
                        .withRequestsPerSecond(5)
                        .withBurstCapacity(10);
                } else {
                    yield RateLimitConfig.Defaults.standardUser();
                }
            }
        };
    }

    /**
     * Add rate limit headers to response.
     */
    private void addRateLimitHeaders(
        org.springframework.web.server.ServerWebExchange exchange,
        RateLimitResult result
    ) {
        var headers = result.generateHeaders().toMap();
        headers.forEach((name, value) -> {
            exchange.getResponse().getHeaders().add(name, value);
        });
    }

    /**
     * Handle rate limit exceeded response.
     */
    private Mono<Void> handleRateLimitExceeded(
        org.springframework.web.server.ServerWebExchange exchange,
        RateLimitResult result
    ) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String errorResponse = String.format("""
            {
                "timestamp": "%s",
                "status": 429,
                "error": "Too Many Requests",
                "message": "%s",
                "path": "%s",
                "rateLimitDetails": {
                    "remainingTokens": %d,
                    "retryAfterSeconds": %d,
                    "algorithm": "%s",
                    "windowSize": "%s"
                }
            }
            """,
            Instant.now(),
            result.getUserMessage(),
            exchange.getRequest().getPath().value(),
            result.getRemainingTokens(),
            result.getRetryAfter().getSeconds(),
            result.getAlgorithm(),
            result.getWindowSize() != null ? result.getWindowSize().toString() : "60s"
        );

        org.springframework.core.io.buffer.DataBuffer buffer =
            response.bufferFactory().wrap(errorResponse.getBytes());

        log.info("Rate limit exceeded for path: {} - {}",
            exchange.getRequest().getPath().value(),
            result.getTechnicalDetails());

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Configuration for Rate Limiting Filter.
     */
    @Data
    public static class Config {
        private boolean enabled = true;
        private String configName = "default";
        private String keyResolver = "user"; // user, ip, api-key, endpoint, combined
        private List<String> skipPaths = Collections.emptyList();
        private Map<String, Object> customConfig = Collections.emptyMap();
    }
}