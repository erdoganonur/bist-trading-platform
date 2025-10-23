package com.bisttrading.telegram.dto;

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
public class TelegramUserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Telegram user ID
     */
    private Long telegramUserId;

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
        return LocalDateTime.now().isBefore(algoLabSessionExpires);
    }
}
