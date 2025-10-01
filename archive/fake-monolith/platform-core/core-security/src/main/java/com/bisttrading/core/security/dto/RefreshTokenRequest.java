package com.bisttrading.core.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Refresh token request DTO.
 * Used for refreshing access tokens using a valid refresh token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * The refresh token to use for generating a new access token.
     */
    @NotBlank(message = "Refresh token boş olamaz")
    @Size(min = 10, message = "Geçersiz refresh token formatı")
    @JsonProperty("refreshToken")
    private String refreshToken;

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
     * Device fingerprint for additional security.
     */
    @JsonProperty("deviceFingerprint")
    private String deviceFingerprint;

    /**
     * Whether to generate a new refresh token as well.
     * If true, both access and refresh tokens will be renewed.
     */
    @JsonProperty("renewRefreshToken")
    private boolean renewRefreshToken = false;

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
     * Validates if the refresh token has a valid format.
     * This is a basic format check, not a cryptographic validation.
     *
     * @return true if format appears valid
     */
    public boolean hasValidFormat() {
        if (refreshToken == null) {
            return false;
        }

        String token = refreshToken.trim();

        // Basic JWT format check (header.payload.signature)
        if (token.split("\\.").length == 3) {
            return true;
        }

        // Basic length check for custom token format
        return token.length() >= 32 && token.length() <= 2048;
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
        return String.format("RefreshTokenRequest{refreshToken='%s', renewRefreshToken=%b, clientIp='%s'}",
            getMaskedRefreshToken(), renewRefreshToken, clientIp);
    }
}