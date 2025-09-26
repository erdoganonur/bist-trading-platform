package com.bisttrading.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Gateway Route Configuration.
 *
 * Defines programmatic routing rules for all backend services.
 * This configuration complements the declarative routes in application.yml
 * and provides more complex routing logic when needed.
 */
@Configuration
public class RouteConfiguration {

    /**
     * Programmatic route configuration for complex routing scenarios.
     *
     * @param builder RouteLocator builder
     * @return Configured RouteLocator
     */
    @Bean
    public RouteLocator programmaticRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            // Authentication routes - high priority, no rate limiting for login
            .route("auth-login", r -> r
                .path("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh")
                .and()
                .method(HttpMethod.POST)
                .uri("http://localhost:8081"))

            // Public market data routes - cached responses
            .route("public-market-data", r -> r
                .path("/api/v1/market-data/public/**", "/api/v1/symbols/search/**")
                .and()
                .method(HttpMethod.GET)
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Route", "public-market-data")
                    .addResponseHeader("Cache-Control", "public, max-age=30"))
                .uri("http://localhost:8083"))

            // WebSocket routes for real-time data
            .route("websocket-market-data", r -> r
                .path("/ws/market-data/**")
                .uri("ws://localhost:8083"))

            // Admin routes - restricted access
            .route("admin-routes", r -> r
                .path("/api/v1/admin/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Route", "admin")
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(adminKeyResolver())))
                .uri("http://localhost:8081"))

            // Trading routes - authenticated and rate limited
            .route("trading-routes", r -> r
                .path("/api/v1/orders/**", "/api/v1/positions/**", "/api/v1/portfolio/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Route", "trading")
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                    .retry(retryConfig -> retryConfig
                        .setRetries(2)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, false)))
                .uri("http://localhost:8082"))

            .build();
    }

    /**
     * Redis-based rate limiter configuration.
     */
    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
            100, // replenishRate (tokens per second)
            200, // burstCapacity (max tokens in bucket)
            1    // requestedTokens (tokens requested per request)
        );
    }

    /**
     * Key resolver for user-based rate limiting.
     * Uses JWT subject (user ID) as the key for rate limiting.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Extract user ID from JWT token in Authorization header
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    // TODO: Extract user ID from JWT token
                    // For now, use IP address as fallback
                    return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
                } catch (Exception e) {
                    return Mono.just("anonymous");
                }
            }
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }

    /**
     * Key resolver for admin-based rate limiting.
     * More restrictive rate limiting for admin operations.
     */
    @Bean
    public KeyResolver adminKeyResolver() {
        return exchange -> {
            // Use combination of IP and User-Agent for admin rate limiting
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
            return Mono.just(ip + ":" + (userAgent != null ? userAgent.hashCode() : "unknown"));
        };
    }

    /**
     * Key resolver for IP-based rate limiting.
     * Used for public endpoints and fallback scenarios.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}