package com.bisttrading.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Telegram user session stored in Redis.
 * Contains authentication tokens and user context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Telegram user ID
     */
    private Long telegramUserId;

    /**
     * Telegram chat ID (for sending messages)
     */
    private Long chatId;

    /**
     * Platform user ID (after login)
     */
    private String platformUserId;

    /**
     * Platform username
     */
    private String username;

    /**
     * JWT access token
     */
    private String jwtToken;

    /**
     * JWT refresh token
     */
    private String refreshToken;

    /**
     * AlgoLab authentication status
     */
    private boolean algoLabAuthenticated;

    /**
     * AlgoLab authentication token
     */
    private String algoLabToken;

    /**
     * AlgoLab authentication hash
     */
    private String algoLabHash;

    /**
     * AlgoLab session expiration
     */
    private LocalDateTime algoLabSessionExpires;

    /**
     * Session creation time
     */
    @Builder.Default
    private LocalDateTime loggedInAt = LocalDateTime.now();

    /**
     * Last activity time
     */
    @Builder.Default
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return jwtToken != null && platformUserId != null;
    }

    /**
     * Update last activity timestamp
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Check if AlgoLab session is valid
     */
    public boolean isAlgoLabSessionValid() {
        if (!algoLabAuthenticated || algoLabSessionExpires == null) {
            return false;
        }
        if (algoLabToken == null || algoLabHash == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(algoLabSessionExpires);
    }

    /**
     * Set AlgoLab session data
     */
    public void setAlgoLabSession(String token, String hash, LocalDateTime expiresAt) {
        this.algoLabToken = token;
        this.algoLabHash = hash;
        this.algoLabSessionExpires = expiresAt;
        this.algoLabAuthenticated = true;
    }

    /**
     * Clear AlgoLab session
     */
    public void clearAlgoLabSession() {
        this.algoLabToken = null;
        this.algoLabHash = null;
        this.algoLabSessionExpires = null;
        this.algoLabAuthenticated = false;
    }
}
