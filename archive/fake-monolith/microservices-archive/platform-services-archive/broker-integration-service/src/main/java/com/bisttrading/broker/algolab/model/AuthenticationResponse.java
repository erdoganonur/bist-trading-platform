package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

/**
 * Authentication response from AlgoLab API
 */
@Data
public class AuthenticationResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("permissions")
    private String[] permissions;

    @JsonProperty("session_timeout")
    private long sessionTimeout;

    @JsonProperty("heartbeat_interval")
    private long heartbeatInterval;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    /**
     * Get access token (alias for token field)
     */
    public String getAccessToken() {
        return token;
    }

    /**
     * Check if the authentication was successful
     */
    public boolean isSuccessful() {
        return success && token != null && !token.isEmpty();
    }

    /**
     * Check if the session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Get remaining session time in seconds
     */
    public long getRemainingSessionTime() {
        if (expiresAt == null) {
            return sessionTimeout;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}