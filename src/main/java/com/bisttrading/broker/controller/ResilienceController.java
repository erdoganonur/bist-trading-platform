package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.service.AlgoLabFallbackService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring and managing resilience patterns.
 * Provides endpoints to check circuit breaker status, retry statistics, and cache management.
 */
@RestController
@RequestMapping("/api/v1/broker/resilience")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resilience Management", description = "Monitor and manage circuit breaker, retry, and fallback mechanisms")
public class ResilienceController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final AlgoLabFallbackService fallbackService;

    @GetMapping("/circuit-breaker/status")
    @Operation(summary = "Get circuit breaker status", description = "Returns current state and metrics of AlgoLab circuit breaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("algolab");

        Map<String, Object> status = new HashMap<>();
        status.put("name", circuitBreaker.getName());
        status.put("state", circuitBreaker.getState().toString());

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", metrics.getFailureRate());
        metricsMap.put("slowCallRate", metrics.getSlowCallRate());
        metricsMap.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
        metricsMap.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());

        status.put("metrics", metricsMap);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/circuit-breaker/events")
    @Operation(summary = "Get circuit breaker events", description = "Returns recent circuit breaker state transitions")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerEvents() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("algolab");

        Map<String, Object> events = new HashMap<>();
        events.put("name", circuitBreaker.getName());
        events.put("currentState", circuitBreaker.getState().toString());

        // Get event consumer statistics
        var eventPublisher = circuitBreaker.getEventPublisher();
        events.put("hasEventConsumers", eventPublisher != null);

        return ResponseEntity.ok(events);
    }

    @PostMapping("/circuit-breaker/transition/{state}")
    @Operation(summary = "Manually transition circuit breaker state", description = "Forces circuit breaker to specific state (for testing)")
    public ResponseEntity<Map<String, String>> transitionCircuitBreaker(@PathVariable String state) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("algolab");

        try {
            switch (state.toUpperCase()) {
                case "CLOSED":
                    circuitBreaker.transitionToClosedState();
                    break;
                case "OPEN":
                    circuitBreaker.transitionToOpenState();
                    break;
                case "HALF_OPEN":
                    circuitBreaker.transitionToHalfOpenState();
                    break;
                case "FORCED_OPEN":
                    circuitBreaker.transitionToForcedOpenState();
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", "false",
                            "message", "Invalid state. Use: CLOSED, OPEN, HALF_OPEN, or FORCED_OPEN"
                        ));
            }

            log.info("Circuit breaker manually transitioned to {}", state);

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Circuit breaker transitioned to " + state,
                "currentState", circuitBreaker.getState().toString()
            ));

        } catch (Exception e) {
            log.error("Failed to transition circuit breaker", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to transition: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/circuit-breaker/reset")
    @Operation(summary = "Reset circuit breaker", description = "Resets circuit breaker to closed state and clears metrics")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("algolab");

        try {
            circuitBreaker.reset();
            log.info("Circuit breaker reset");

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Circuit breaker reset successfully",
                "currentState", circuitBreaker.getState().toString()
            ));

        } catch (Exception e) {
            log.error("Failed to reset circuit breaker", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to reset: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/retry/status")
    @Operation(summary = "Get retry status", description = "Returns retry configuration and statistics")
    public ResponseEntity<Map<String, Object>> getRetryStatus() {
        Retry retry = retryRegistry.retry("algolab");

        Map<String, Object> status = new HashMap<>();
        status.put("name", retry.getName());

        Retry.Metrics metrics = retry.getMetrics();
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("numberOfSuccessfulCallsWithRetryAttempt",
            metrics.getNumberOfSuccessfulCallsWithRetryAttempt());
        metricsMap.put("numberOfFailedCallsWithRetryAttempt",
            metrics.getNumberOfFailedCallsWithRetryAttempt());
        metricsMap.put("numberOfSuccessfulCallsWithoutRetryAttempt",
            metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
        metricsMap.put("numberOfFailedCallsWithoutRetryAttempt",
            metrics.getNumberOfFailedCallsWithoutRetryAttempt());

        status.put("metrics", metricsMap);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/fallback/cache/stats")
    @Operation(summary = "Get fallback cache statistics", description = "Returns cache usage and statistics")
    public ResponseEntity<Map<String, Object>> getFallbackCacheStats() {
        return ResponseEntity.ok(fallbackService.getCacheStats());
    }

    @PostMapping("/fallback/cache/clear")
    @Operation(summary = "Clear fallback cache", description = "Clears all cached fallback data")
    public ResponseEntity<Map<String, String>> clearFallbackCache() {
        try {
            fallbackService.clearCache();
            log.info("Fallback cache cleared");

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Fallback cache cleared successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to clear fallback cache", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to clear cache: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/fallback/cache/clear/{endpoint}")
    @Operation(summary = "Clear cache for endpoint", description = "Clears cached data for specific endpoint")
    public ResponseEntity<Map<String, String>> clearEndpointCache(@PathVariable String endpoint) {
        try {
            fallbackService.clearCache("/" + endpoint);
            log.info("Cache cleared for endpoint: {}", endpoint);

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Cache cleared for endpoint: " + endpoint
            ));

        } catch (Exception e) {
            log.error("Failed to clear endpoint cache", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to clear cache: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Get overall resilience health", description = "Returns health status of all resilience components")
    public ResponseEntity<Map<String, Object>> getResilienceHealth() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("algolab");
        Retry retry = retryRegistry.retry("algolab");

        Map<String, Object> health = new HashMap<>();
        health.put("circuitBreaker", Map.of(
            "state", circuitBreaker.getState().toString(),
            "healthy", circuitBreaker.getState() != CircuitBreaker.State.OPEN
        ));

        health.put("retry", Map.of(
            "configured", true,
            "healthy", true
        ));

        health.put("fallbackCache", fallbackService.getCacheStats());

        boolean overallHealthy = circuitBreaker.getState() != CircuitBreaker.State.OPEN;
        health.put("overallStatus", overallHealthy ? "HEALTHY" : "DEGRADED");

        return ResponseEntity.ok(health);
    }
}
