package com.bisttrading.broker.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket health status response.
 */
@Data
@Builder
public class WebSocketHealthResponse {
    private boolean authenticated;
    private boolean websocketConnected;
    private Set<String> subscriptions;
    private String sessionExpiresAt;
    private Instant lastTickReceived;
    private int bufferedSymbols;
    private Map<String, Object> cacheStats;
    private String status; // HEALTHY, DEGRADED, DOWN

    @Data
    @Builder
    public static class DetailedHealth {
        private AuthStatus auth;
        private WebSocketStatus websocket;
        private List<SubscriptionInfo> subscriptions;
        private CacheStatus cache;
        private String overallStatus;
    }

    @Data
    @Builder
    public static class AuthStatus {
        private String status; // ACTIVE, EXPIRED, NONE
        private String tokenExpiry;
        private String sessionAge;
        private String lastRefresh;
    }

    @Data
    @Builder
    public static class WebSocketStatus {
        private boolean connected;
        private String url;
        private String lastHeartbeat;
        private int reconnectAttempts;
        private String uptime;
        private long messageCount;
    }

    @Data
    @Builder
    public static class SubscriptionInfo {
        private String symbol;
        private String channel;
        private String subscribedAt;
        private String lastDataReceived;
        private long messageCount;
    }

    @Data
    @Builder
    public static class CacheStatus {
        private String type; // REDIS, IN_MEMORY
        private boolean connected;
        private Map<String, Object> stats;
    }
}
