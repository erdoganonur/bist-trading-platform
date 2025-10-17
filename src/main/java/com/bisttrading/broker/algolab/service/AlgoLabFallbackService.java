package com.bisttrading.broker.algolab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback service for AlgoLab API when circuit breaker is open or service is unavailable.
 * Provides graceful degradation with cached data and meaningful error responses.
 */
@Service
@Slf4j
public class AlgoLabFallbackService {

    // Simple in-memory cache for fallback data
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();

    // Cache TTL - 5 minutes
    private static final long CACHE_TTL_MS = 300000;

    /**
     * Fallback for REST API POST requests.
     * Returns cached data if available, otherwise returns error response.
     */
    public <T> ResponseEntity<T> fallbackPost(
            String endpoint,
            Object payload,
            boolean authenticated,
            Class<T> responseType,
            Throwable throwable) {

        log.warn("Fallback triggered for POST {} - Reason: {}",
            endpoint,
            throwable.getMessage());

        // Try to get cached response
        CachedResponse cached = getCachedResponse(endpoint);
        if (cached != null && cached.isValid()) {
            log.info("Returning cached response for {}", endpoint);
            try {
                @SuppressWarnings("unchecked")
                T cachedData = (T) cached.getData();
                return ResponseEntity.ok(cachedData);
            } catch (ClassCastException e) {
                log.error("Cached response type mismatch for {}", endpoint, e);
            }
        }

        // Return error response
        return createErrorResponse(responseType, endpoint, throwable);
    }

    /**
     * Caches a successful response.
     */
    public <T> void cacheResponse(String endpoint, T response) {
        if (response != null) {
            responseCache.put(endpoint, new CachedResponse(response, System.currentTimeMillis()));
            log.debug("Cached response for {}", endpoint);
        }
    }

    /**
     * Gets cached response if valid.
     */
    private CachedResponse getCachedResponse(String endpoint) {
        CachedResponse cached = responseCache.get(endpoint);
        if (cached != null && cached.isValid()) {
            return cached;
        }
        return null;
    }

    /**
     * Creates an error response with meaningful message.
     */
    private <T> ResponseEntity<T> createErrorResponse(
            Class<T> responseType,
            String endpoint,
            Throwable throwable) {

        String errorMessage = buildErrorMessage(throwable);

        // Try to create error response instance
        try {
            if (Map.class.isAssignableFrom(responseType)) {
                @SuppressWarnings("unchecked")
                T errorMap = (T) Map.of(
                    "success", false,
                    "message", errorMessage,
                    "error", "SERVICE_UNAVAILABLE",
                    "timestamp", Instant.now().toString(),
                    "endpoint", endpoint
                );
                return new ResponseEntity<>(errorMap, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (Exception e) {
            log.error("Failed to create error response", e);
        }

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Builds user-friendly error message based on exception type.
     */
    private String buildErrorMessage(Throwable throwable) {
        if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return "AlgoLab service is temporarily unavailable. Circuit breaker is OPEN. " +
                   "Please try again in a few moments.";
        }

        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return "AlgoLab service request timed out. The service is experiencing high load. " +
                   "Please try again later.";
        }

        if (throwable instanceof java.io.IOException) {
            return "Unable to connect to AlgoLab service. Network connection error. " +
                   "Please check your connection and try again.";
        }

        if (throwable instanceof org.springframework.web.client.ResourceAccessException) {
            return "AlgoLab service is not accessible. The service may be down or unreachable. " +
                   "Please contact support if this persists.";
        }

        return "AlgoLab service is temporarily unavailable. " +
               "We are working to restore service. Please try again later.";
    }

    /**
     * Fallback for authentication requests.
     * Cannot use cache for auth, returns error immediately.
     */
    public ResponseEntity<Map> fallbackAuth(String endpoint, Object payload, Throwable throwable) {
        log.error("Authentication fallback triggered for {} - Reason: {}",
            endpoint,
            throwable.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "message", "Authentication service is temporarily unavailable. Please try again.",
                "error", "AUTH_SERVICE_UNAVAILABLE",
                "timestamp", Instant.now().toString()
            ));
    }

    /**
     * Fallback for order operations.
     * Critical operation - do not retry, return error immediately.
     */
    public ResponseEntity<Map> fallbackOrder(String operation, Object payload, Throwable throwable) {
        log.error("Order operation fallback triggered for {} - Reason: {}",
            operation,
            throwable.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "message", "Order service is temporarily unavailable. Your order was NOT placed. " +
                          "Please wait and try again when service is restored.",
                "error", "ORDER_SERVICE_UNAVAILABLE",
                "operation", operation,
                "timestamp", Instant.now().toString()
            ));
    }

    /**
     * Fallback for position queries.
     * Can use cached data safely, or return mock data for development.
     */
    public ResponseEntity<Map> fallbackPositions(String subAccount, Throwable throwable) {
        log.warn("Positions fallback triggered - Reason: {}", throwable.getMessage());

        CachedResponse cached = getCachedResponse("/api/InstantPosition");
        if (cached != null && cached.isValid()) {
            log.info("Returning cached positions data");
            @SuppressWarnings("unchecked")
            Map<String, Object> cachedData = (Map<String, Object>) cached.getData();
            return ResponseEntity.ok(cachedData);
        }

        // Return mock positions for development/testing
        log.info("Returning mock positions data (no cache available)");
        return ResponseEntity.ok(createMockPositionsResponse());
    }

    /**
     * Creates mock positions response for development/testing.
     */
    private Map<String, Object> createMockPositionsResponse() {
        List<Map<String, Object>> mockPositions = List.of(
            Map.of(
                "symbol", "AKBNK",
                "quantity", 1000,
                "averagePrice", 45.50,
                "lastPrice", 48.75,
                "profitLoss", 3250.00,
                "profitLossPercent", 7.14,
                "totalValue", 48750.00
            ),
            Map.of(
                "symbol", "GARAN",
                "quantity", 500,
                "averagePrice", 95.20,
                "lastPrice", 92.80,
                "profitLoss", -1200.00,
                "profitLossPercent", -2.52,
                "totalValue", 46400.00
            ),
            Map.of(
                "symbol", "THYAO",
                "quantity", 250,
                "averagePrice", 285.00,
                "lastPrice", 305.50,
                "profitLoss", 5125.00,
                "profitLossPercent", 7.19,
                "totalValue", 76375.00
            )
        );

        return Map.of(
            "success", true,
            "content", Map.of(
                "positions", mockPositions,
                "totalValue", 171525.00,
                "totalProfitLoss", 7175.00,
                "totalProfitLossPercent", 4.36
            ),
            "message", "Mock positions data (development mode)",
            "timestamp", Instant.now().toString()
        );
    }

    /**
     * Clears all cached responses.
     */
    public void clearCache() {
        responseCache.clear();
        log.info("Fallback cache cleared");
    }

    /**
     * Clears cached response for specific endpoint.
     */
    public void clearCache(String endpoint) {
        responseCache.remove(endpoint);
        log.debug("Cleared cache for {}", endpoint);
    }

    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        long validCount = responseCache.values().stream()
            .filter(CachedResponse::isValid)
            .count();

        return Map.of(
            "totalCached", responseCache.size(),
            "validCached", validCount,
            "expiredCached", responseCache.size() - validCount,
            "cacheTtlMs", CACHE_TTL_MS
        );
    }

    /**
     * Cached response wrapper.
     */
    private static class CachedResponse {
        private final Object data;
        private final long timestamp;

        public CachedResponse(Object data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        public Object getData() {
            return data;
        }

        public boolean isValid() {
            return (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS;
        }

        public long getAge() {
            return System.currentTimeMillis() - timestamp;
        }
    }
}
