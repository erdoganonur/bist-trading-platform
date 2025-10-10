package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.service.AlgoLabCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for cache management and monitoring.
 *
 * Provides endpoints for:
 * - Cache statistics
 * - Manual cache invalidation
 * - Cache warmup triggers
 */
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cache Management", description = "Redis cache management and monitoring")
public class CacheManagementController {

    private final AlgoLabCacheService cacheService;

    /**
     * Gets cache statistics.
     *
     * Returns:
     * - Total requests
     * - Cache hits/misses
     * - Hit rate percentage
     * - Cache status per region
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get cache statistics",
        description = "Returns cache hit/miss statistics and cache region status"
    )
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        log.debug("Getting cache statistics");
        return ResponseEntity.ok(cacheService.getCacheStatistics());
    }

    /**
     * Resets cache statistics.
     *
     * Admin only operation.
     */
    @PostMapping("/statistics/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Reset cache statistics",
        description = "Resets cache hit/miss counters (Admin only)"
    )
    public ResponseEntity<Map<String, String>> resetStatistics() {
        log.info("Resetting cache statistics");
        cacheService.resetStatistics();
        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Cache statistics reset"
        ));
    }

    /**
     * Invalidates symbol cache for a specific symbol.
     */
    @DeleteMapping("/symbols/{symbol}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRADER')")
    @Operation(
        summary = "Invalidate symbol cache",
        description = "Invalidates cache entry for a specific symbol"
    )
    public ResponseEntity<Map<String, String>> invalidateSymbol(
            @Parameter(description = "Symbol code (e.g., GARAN, AKBNK)")
            @PathVariable String symbol) {

        log.info("Invalidating symbol cache for: {}", symbol);
        cacheService.invalidateSymbol(symbol);

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Symbol cache invalidated",
            "symbol", symbol
        ));
    }

    /**
     * Invalidates candle cache for a specific symbol and period.
     */
    @DeleteMapping("/candles/{symbol}/{period}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRADER')")
    @Operation(
        summary = "Invalidate candle cache",
        description = "Invalidates candle cache for a specific symbol and period"
    )
    public ResponseEntity<Map<String, String>> invalidateCandle(
            @Parameter(description = "Symbol code")
            @PathVariable String symbol,
            @Parameter(description = "Period (e.g., 1D, 1H, 5M)")
            @PathVariable String period) {

        log.info("Invalidating candle cache for: {} - {}", symbol, period);
        cacheService.invalidateCandle(symbol, period);

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Candle cache invalidated",
            "symbol", symbol,
            "period", period
        ));
    }

    /**
     * Clears all symbol cache entries.
     */
    @DeleteMapping("/symbols")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear all symbol cache",
        description = "Clears all symbol cache entries (Admin only)"
    )
    public ResponseEntity<Map<String, String>> clearSymbolCache() {
        log.warn("Clearing all symbol cache");
        cacheService.clearSymbolCache();

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "All symbol cache cleared"
        ));
    }

    /**
     * Clears all candle cache entries.
     */
    @DeleteMapping("/candles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear all candle cache",
        description = "Clears all candle cache entries (Admin only)"
    )
    public ResponseEntity<Map<String, String>> clearCandleCache() {
        log.warn("Clearing all candle cache");
        cacheService.clearCandleCache();

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "All candle cache cleared"
        ));
    }

    /**
     * Clears ALL caches.
     *
     * DANGEROUS: Only use in development or emergency.
     */
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear ALL caches",
        description = "Clears ALL cache regions and resets statistics (Admin only, USE WITH CAUTION)"
    )
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        log.error("CLEARING ALL CACHES - This is a destructive operation!");
        cacheService.clearAllCaches();

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "ALL caches cleared and statistics reset",
            "warning", "This was a destructive operation"
        ));
    }

    /**
     * Triggers manual cache warmup.
     */
    @PostMapping("/warmup")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRADER')")
    @Operation(
        summary = "Trigger cache warmup",
        description = "Manually triggers cache warmup for popular symbols"
    )
    public ResponseEntity<Map<String, String>> triggerCacheWarmup() {
        log.info("Triggering manual cache warmup");

        // Run in separate thread to not block the request
        new Thread(() -> {
            try {
                cacheService.warmupCache();
            } catch (Exception e) {
                log.error("Cache warmup failed", e);
            }
        }).start();

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Cache warmup triggered (running in background)"
        ));
    }

    /**
     * Health check for cache system.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Cache health check",
        description = "Returns health status of cache system"
    )
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> stats = cacheService.getCacheStatistics();

        Map<String, Object> health = Map.of(
            "status", "UP",
            "totalRequests", stats.get("totalRequests"),
            "hitRate", stats.get("hitRate") + "%",
            "cacheRegions", stats.get("cacheNames")
        );

        return ResponseEntity.ok(health);
    }
}
