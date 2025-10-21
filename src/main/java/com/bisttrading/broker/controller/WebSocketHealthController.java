package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.service.*;
import com.bisttrading.broker.algolab.websocket.AlgoLabWebSocketClient;
import com.bisttrading.broker.dto.WebSocketHealthResponse;
import com.bisttrading.broker.dto.WebSocketHealthResponse.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * WebSocket health monitoring and status endpoints.
 */
@RestController
@RequestMapping("/api/v1/broker/websocket")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket Health", description = "WebSocket connection health and monitoring")
public class WebSocketHealthController {

    private final AlgoLabAuthService authService;
    private final AlgoLabWebSocketClient webSocketClient;
    private final SubscriptionManager subscriptionManager;
    private final WebSocketMessageBuffer messageBuffer;
    private final Optional<RedisTickCacheService> redisCacheService;

    @GetMapping("/health")
    @Operation(summary = "Get WebSocket health status", description = "Quick health check for WebSocket connection")
    public ResponseEntity<WebSocketHealthResponse> getHealth() {
        try {
            boolean authenticated = authService.isAuthenticated();
            boolean websocketConnected = webSocketClient.isConnected();

            // Determine overall status
            String status;
            if (authenticated && websocketConnected) {
                status = "HEALTHY";
            } else if (authenticated || websocketConnected) {
                status = "DEGRADED";
            } else {
                status = "DOWN";
            }

            // Get cache stats
            Map<String, Object> cacheStats;
            if (redisCacheService.isPresent()) {
                cacheStats = redisCacheService.get().getStats();
            } else {
                cacheStats = messageBuffer.getStats();
            }

            // Get last tick time
            Instant lastTickReceived = null;
            if (redisCacheService.isPresent()) {
                var symbols = redisCacheService.get().getActiveSymbols();
                if (!symbols.isEmpty()) {
                    lastTickReceived = symbols.stream()
                        .map(symbol -> redisCacheService.get().getLastTickTime(symbol))
                        .filter(java.util.Objects::nonNull)
                        .max(Instant::compareTo)
                        .orElse(null);
                }
            }

            WebSocketHealthResponse response = WebSocketHealthResponse.builder()
                .authenticated(authenticated)
                .websocketConnected(websocketConnected)
                .subscriptions(subscriptionManager.getActiveSymbols())
                .sessionExpiresAt(getSessionExpiry())
                .lastTickReceived(lastTickReceived)
                .bufferedSymbols(getBufferedSymbolCount())
                .cacheStats(cacheStats)
                .status(status)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health/detailed")
    @Operation(summary = "Get detailed health status", description = "Comprehensive health information with all details")
    public ResponseEntity<DetailedHealth> getDetailedHealth() {
        try {
            // Auth status
            AuthStatus authStatus = AuthStatus.builder()
                .status(authService.isAuthenticated() ? "ACTIVE" : "NONE")
                .tokenExpiry(getSessionExpiry())
                .sessionAge(getSessionAge())
                .lastRefresh("N/A") // TODO: Track last refresh
                .build();

            // WebSocket status
            WebSocketStatus wsStatus = WebSocketStatus.builder()
                .connected(webSocketClient.isConnected())
                .url(webSocketClient.getWebSocketUrl())
                .lastHeartbeat(webSocketClient.getLastHeartbeat())
                .reconnectAttempts(0) // TODO: Track reconnect attempts
                .uptime("N/A") // TODO: Track uptime
                .messageCount(webSocketClient.getMessageCount())
                .build();

            // Subscription info
            List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptions().stream()
                .map(sub -> SubscriptionInfo.builder()
                    .symbol(sub.getSymbol())
                    .channel(sub.getChannel())
                    .subscribedAt(sub.getSubscribedAt() != null ? sub.getSubscribedAt().toString() : "N/A")
                    .lastDataReceived(getLastDataReceived(sub.getSymbol()))
                    .messageCount(getMessageCount(sub.getSymbol()))
                    .build())
                .collect(Collectors.toList());

            // Cache status
            Map<String, Object> stats;
            String cacheType;
            boolean cacheConnected;

            if (redisCacheService.isPresent()) {
                stats = redisCacheService.get().getStats();
                cacheType = "REDIS";
                cacheConnected = redisCacheService.get().isHealthy();
            } else {
                stats = messageBuffer.getStats();
                cacheType = "IN_MEMORY";
                cacheConnected = true;
            }

            CacheStatus cacheStatus = CacheStatus.builder()
                .type(cacheType)
                .connected(cacheConnected)
                .stats(stats)
                .build();

            // Overall status
            String overallStatus = determineOverallStatus(
                authStatus.getStatus().equals("ACTIVE"),
                wsStatus.isConnected(),
                cacheConnected
            );

            DetailedHealth detailedHealth = DetailedHealth.builder()
                .auth(authStatus)
                .websocket(wsStatus)
                .subscriptions(subscriptions)
                .cache(cacheStatus)
                .overallStatus(overallStatus)
                .build();

            return ResponseEntity.ok(detailedHealth);

        } catch (Exception e) {
            log.error("Failed to get detailed health", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/health/refresh")
    @Operation(summary = "Refresh health checks", description = "Force refresh all health checks")
    public ResponseEntity<Map<String, String>> refreshHealth() {
        try {
            // Trigger health checks
            boolean authValid = authService.isAuthenticated();
            boolean wsConnected = webSocketClient.isConnected();

            return ResponseEntity.ok(Map.of(
                "status", "refreshed",
                "authenticated", String.valueOf(authValid),
                "websocketConnected", String.valueOf(wsConnected),
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("Failed to refresh health", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods

    private String getSessionExpiry() {
        // TODO: Parse JWT and get expiry
        return "N/A";
    }

    private String getSessionAge() {
        // TODO: Calculate session age
        return "N/A";
    }

    private int getBufferedSymbolCount() {
        if (redisCacheService.isPresent()) {
            return redisCacheService.get().getActiveSymbols().size();
        } else {
            return messageBuffer.getActiveTickSymbols().size();
        }
    }

    private String getLastDataReceived(String symbol) {
        if (redisCacheService.isPresent()) {
            Instant lastTick = redisCacheService.get().getLastTickTime(symbol);
            return lastTick != null ? lastTick.toString() : "Never";
        }
        return "N/A";
    }

    private long getMessageCount(String symbol) {
        if (redisCacheService.isPresent()) {
            return redisCacheService.get().getMessageCount(symbol);
        }
        return 0;
    }

    private String determineOverallStatus(boolean auth, boolean ws, boolean cache) {
        if (auth && ws && cache) {
            return "HEALTHY";
        } else if (!auth && !ws) {
            return "DOWN";
        } else {
            return "DEGRADED";
        }
    }
}
