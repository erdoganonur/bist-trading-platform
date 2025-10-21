package com.bisttrading.broker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for AlgoLab authentication status check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AlgoLab authentication status")
public class BrokerAuthStatusResponse {

    @Schema(description = "Whether user is authenticated with AlgoLab", example = "true")
    private boolean authenticated;

    @Schema(description = "Whether AlgoLab session is active", example = "true")
    private boolean sessionActive;

    @Schema(description = "Whether connection to AlgoLab is alive", example = "true")
    private boolean connectionAlive;

    @Schema(description = "AlgoLab username (if authenticated)", example = "52738096404")
    private String username;

    @Schema(description = "Session ID (if authenticated)", example = "abc123def456")
    private String sessionId;

    @Schema(description = "Whether WebSocket is connected", example = "true")
    private boolean websocketConnected;

    @Schema(description = "Last session refresh time", example = "2025-10-15T10:25:00Z")
    private Instant lastRefreshTime;

    @Schema(description = "Session expiration time (if known)", example = "2025-10-16T12:00:00Z")
    private Instant expiresAt;

    @Schema(description = "Broker name", example = "AlgoLab")
    @Builder.Default
    private String brokerName = "AlgoLab";

    @Schema(description = "Current timestamp", example = "2025-10-15T10:30:00Z")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "Additional status message")
    private String message;
}
