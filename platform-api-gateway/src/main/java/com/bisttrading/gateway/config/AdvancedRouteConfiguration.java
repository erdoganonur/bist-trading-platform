package com.bisttrading.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Advanced Route Configuration for BIST Trading Platform.
 *
 * This configuration provides sophisticated routing capabilities including:
 * - Time-based routing (trading hours)
 * - Service-specific rate limiting
 * - Custom predicates for complex routing logic
 * - Fallback routes for circuit breaker patterns
 */
@Slf4j
@Configuration
public class AdvancedRouteConfiguration {

    /**
     * Advanced programmatic routes with custom predicates and filters.
     */
    @Bean
    public RouteLocator advancedRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            // Trading Hours Route - Only allow trading during market hours
            .route("trading-hours-check", r -> r
                .path("/api/v1/orders/**")
                .and().method(HttpMethod.POST, HttpMethod.PUT)
                .and().predicate(this::isTradingHours)
                .filters(f -> f
                    .addRequestHeader("X-Trading-Session", getCurrentTradingSession())
                    .addRequestHeader("X-Gateway-Route", "trading-hours")
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(tradingRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("http://localhost:8082"))

            // Emergency Circuit Breaker Routes
            .route("emergency-readonly", r -> r
                .path("/api/v1/**")
                .and().method(HttpMethod.GET)
                .and().header("X-Emergency-Mode", "readonly")
                .filters(f -> f
                    .addRequestHeader("X-Emergency-Mode", "readonly")
                    .addResponseHeader("X-Service-Status", "readonly")
                    .setPath("/api/v1/readonly${path}"))
                .uri("http://localhost:8081"))

            // Load Balancing Routes for Multiple Instances
            .route("user-service-lb", r -> r
                .path("/api/v1/users/**", "/api/v1/auth/**")
                .filters(f -> f
                    .addRequestHeader("X-Load-Balanced", "true")
                    .addRequestHeader("X-Service-Discovery", "enabled"))
                .uri("lb://user-management-service"))

            // API Version Routing
            .route("api-v2-routes", r -> r
                .path("/api/v2/**")
                .filters(f -> f
                    .rewritePath("/api/v2/(?<path>.*)", "/api/v1/${path}")
                    .addRequestHeader("X-API-Version", "v2")
                    .addRequestHeader("X-Compatibility-Mode", "v1"))
                .uri("http://localhost:8081"))

            // Mobile Client Routes (different rate limits)
            .route("mobile-client", r -> r
                .path("/api/mobile/**")
                .and().header("User-Agent", ".*Mobile.*")
                .filters(f -> f
                    .rewritePath("/api/mobile/(?<path>.*)", "/api/v1/${path}")
                    .addRequestHeader("X-Client-Type", "mobile")
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(mobileRateLimiter())
                        .setKeyResolver(mobileKeyResolver())))
                .uri("http://localhost:8081"))

            // Batch Operations Route
            .route("batch-operations", r -> r
                .path("/api/v1/batch/**")
                .and().method(HttpMethod.POST)
                .filters(f -> f
                    .addRequestHeader("X-Operation-Type", "batch")
                    .addRequestHeader("X-Timeout", "60")
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(batchRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("http://localhost:8082"))

            // WebSocket Connection Upgrade
            .route("websocket-upgrade", r -> r
                .path("/ws/**")
                .and().header("Upgrade", "websocket")
                .filters(f -> f
                    .addRequestHeader("X-Protocol", "websocket")
                    .addRequestHeader("X-Connection-Type", "upgrade"))
                .uri("ws://localhost:8083"))

            .build();
    }

    /**
     * Rate limiter configurations for different service types.
     */

    @Bean
    public RedisRateLimiter tradingRateLimiter() {
        return new RedisRateLimiter(
            10,  // 10 requests per second for trading operations
            20,  // Burst of 20 requests
            1    // 1 token per request
        );
    }

    @Bean
    public RedisRateLimiter userBasedRateLimiter() {
        return new RedisRateLimiter(100, 200, 1); // Standard user limits
    }

    @Bean
    public RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(20, 40, 1); // Admin limits
    }

    @Bean
    public RedisRateLimiter publicDataRateLimiter() {
        return new RedisRateLimiter(200, 400, 1); // Higher limits for public data
    }

    @Bean
    public RedisRateLimiter realtimeDataRateLimiter() {
        return new RedisRateLimiter(50, 100, 1); // Real-time data limits
    }

    @Bean
    public RedisRateLimiter mobileRateLimiter() {
        return new RedisRateLimiter(50, 100, 1); // Mobile client limits
    }

    @Bean
    public RedisRateLimiter batchRateLimiter() {
        return new RedisRateLimiter(2, 5, 1); // Very restrictive for batch operations
    }

    @Bean
    public RedisRateLimiter ipBasedRateLimiter() {
        return new RedisRateLimiter(50, 100, 1); // IP-based limits for public endpoints
    }

    /**
     * Key resolver implementations for different rate limiting strategies.
     */

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Extract user ID from JWT token
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    // TODO: Implement JWT token parsing to extract user ID
                    // For now, use a combination of IP and session ID
                    String ip = getClientIp(exchange);
                    String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-ID");
                    return Mono.just(ip + ":" + (sessionId != null ? sessionId : "anonymous"));
                } catch (Exception e) {
                    log.warn("Failed to extract user ID from token", e);
                }
            }
            return ipKeyResolver().resolve(exchange);
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(getClientIp(exchange));
    }

    @Bean
    public KeyResolver adminKeyResolver() {
        return exchange -> {
            String ip = getClientIp(exchange);
            String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
            return Mono.just("admin:" + ip + ":" + (userAgent != null ? userAgent.hashCode() : "unknown"));
        };
    }

    @Bean
    public KeyResolver mobileKeyResolver() {
        return exchange -> {
            String deviceId = exchange.getRequest().getHeaders().getFirst("X-Device-ID");
            if (deviceId != null) {
                return Mono.just("mobile:" + deviceId);
            }
            return Mono.just("mobile:" + getClientIp(exchange));
        };
    }

    /**
     * Custom predicates for advanced routing logic.
     */

    /**
     * Check if current time is within trading hours.
     * BIST trading hours: 09:30 - 18:00 (Turkey time)
     */
    private boolean isTradingHours(org.springframework.web.server.ServerWebExchange exchange) {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Europe/Istanbul"));
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(18, 0);

        boolean isTradingHours = now.isAfter(marketOpen) && now.isBefore(marketClose);

        // Check if it's a weekday (Monday = 1, Sunday = 7)
        java.time.DayOfWeek dayOfWeek = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).getDayOfWeek();
        boolean isWeekday = dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5;

        return isTradingHours && isWeekday;
    }

    /**
     * Get current trading session information.
     */
    private String getCurrentTradingSession() {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Europe/Istanbul"));

        if (now.isBefore(LocalTime.of(9, 30))) {
            return "PRE_MARKET";
        } else if (now.isBefore(LocalTime.of(12, 30))) {
            return "MORNING_SESSION";
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            return "LUNCH_BREAK";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            return "AFTERNOON_SESSION";
        } else {
            return "POST_MARKET";
        }
    }

    /**
     * Extract client IP address from request headers.
     */
    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}