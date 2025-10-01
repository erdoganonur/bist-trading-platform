package com.bisttrading.core.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Size;

/**
 * Logout request DTO.
 * Used for user logout and token invalidation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    /**
     * The refresh token to invalidate (optional).
     * If provided, the refresh token will be blacklisted.
     */
    @Size(min = 10, message = "Geçersiz refresh token formatı")
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Whether to logout from all devices.
     * If true, all active sessions for the user will be terminated.
     */
    @JsonProperty("logoutFromAllDevices")
    private boolean logoutFromAllDevices = false;

    /**
     * Client IP address for security logging.
     */
    @JsonProperty("clientIp")
    private String clientIp;

    /**
     * User agent for security logging.
     */
    @JsonProperty("userAgent")
    private String userAgent;

    /**
     * Device fingerprint for security tracking.
     */
    @JsonProperty("deviceFingerprint")
    private String deviceFingerprint;

    /**
     * Reason for logout (optional).
     * Can be used for analytics and security monitoring.
     */
    @Size(max = 100, message = "Logout sebebi en fazla 100 karakter olabilir")
    @JsonProperty("reason")
    private String reason;

    /**
     * Gets a masked version of the refresh token for logging.
     *
     * @return Masked refresh token
     */
    public String getMaskedRefreshToken() {
        if (refreshToken == null || refreshToken.length() < 10) {
            return "***";
        }

        // Show first 4 and last 4 characters, mask the middle
        String start = refreshToken.substring(0, 4);
        String end = refreshToken.substring(refreshToken.length() - 4);
        String middle = "*".repeat(Math.max(0, refreshToken.length() - 8));

        return start + middle + end;
    }

    /**
     * Checks if refresh token is provided.
     *
     * @return true if refresh token is provided
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.trim().isEmpty();
    }

    /**
     * Gets the clean refresh token (trimmed).
     *
     * @return Clean refresh token
     */
    public String getCleanRefreshToken() {
        return refreshToken != null ? refreshToken.trim() : null;
    }

    /**
     * Creates a string representation for logging (without sensitive data).
     *
     * @return Log-safe string
     */
    public String toLogString() {
        return String.format("LogoutRequest{hasRefreshToken=%b, logoutFromAllDevices=%b, " +
                           "reason='%s', clientIp='%s'}",
            hasRefreshToken(), logoutFromAllDevices, reason, clientIp);
    }
}