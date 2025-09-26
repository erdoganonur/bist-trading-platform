package com.bisttrading.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for API Gateway.
 *
 * Provides detailed health information about the gateway and its dependencies.
 * Used for load balancer health checks and monitoring.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final RouteLocator routeLocator;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Simple health check endpoint.
     * Returns 200 OK if the gateway is running.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "bist-trading-gateway");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check including dependencies.
     * Checks Redis connectivity and route configuration.
     */
    @GetMapping("/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", Instant.now());
        health.put("service", "bist-trading-gateway");
        health.put("version", "1.0.0");

        return checkRedisHealth()
            .map(redisHealth -> {
                health.put("redis", redisHealth);
                health.put("routes", checkRoutesHealth());

                // Determine overall status
                boolean allHealthy = isHealthy(redisHealth) && isHealthy(checkRoutesHealth());
                health.put("status", allHealthy ? "UP" : "DOWN");

                return ResponseEntity.ok(health);
            })
            .onErrorReturn(ResponseEntity.status(503).body(
                Map.of(
                    "status", "DOWN",
                    "timestamp", Instant.now(),
                    "error", "Health check failed"
                )
            ));
    }

    /**
     * Check Redis connectivity.
     */
    private Mono<Map<String, Object>> checkRedisHealth() {
        return redisTemplate.opsForValue()
            .set("gateway:health:test", "test", Duration.ofSeconds(30))
            .then(redisTemplate.opsForValue().get("gateway:health:test"))
            .map(result -> {
                Map<String, Object> redisHealth = new HashMap<>();
                redisHealth.put("status", "UP");
                redisHealth.put("connection", "healthy");
                redisHealth.put("testResult", "test".equals(result) ? "passed" : "failed");
                return redisHealth;
            })
            .onErrorReturn(Map.of(
                "status", "DOWN",
                "connection", "failed",
                "error", "Cannot connect to Redis"
            ));
    }

    /**
     * Check route configuration health.
     */
    private Map<String, Object> checkRoutesHealth() {
        Map<String, Object> routesHealth = new HashMap<>();

        try {
            long routeCount = routeLocator.getRoutes().count().block(Duration.ofSeconds(5));
            routesHealth.put("status", "UP");
            routesHealth.put("routeCount", routeCount);
            routesHealth.put("configuration", "loaded");
        } catch (Exception e) {
            routesHealth.put("status", "DOWN");
            routesHealth.put("error", "Route configuration error");
        }

        return routesHealth;
    }

    /**
     * Check if a health component is healthy.
     */
    private boolean isHealthy(Map<String, Object> healthComponent) {
        return "UP".equals(healthComponent.get("status"));
    }

    /**
     * Readiness probe endpoint.
     * Returns 200 when the gateway is ready to accept requests.
     */
    @GetMapping("/ready")
    public Mono<ResponseEntity<Map<String, String>>> ready() {
        // Check if essential services are available
        return checkRedisHealth()
            .map(redisHealth -> {
                if (isHealthy(redisHealth)) {
                    return ResponseEntity.ok(Map.of(
                        "status", "READY",
                        "timestamp", Instant.now().toString()
                    ));
                } else {
                    return ResponseEntity.status(503).body(Map.of(
                        "status", "NOT_READY",
                        "reason", "Redis unavailable",
                        "timestamp", Instant.now().toString()
                    ));
                }
            })
            .onErrorReturn(ResponseEntity.status(503).body(Map.of(
                "status", "NOT_READY",
                "reason", "Health check failed",
                "timestamp", Instant.now().toString()
            )));
    }

    /**
     * Liveness probe endpoint.
     * Returns 200 if the gateway process is alive.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", Instant.now().toString()
        ));
    }
}