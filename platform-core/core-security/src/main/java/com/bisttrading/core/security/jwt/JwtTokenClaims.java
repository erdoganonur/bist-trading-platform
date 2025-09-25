package com.bisttrading.core.security.jwt;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT Token Claims wrapper for easier access to token data
 */
@Data
@Builder
public class JwtTokenClaims {

    private String userId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String tokenType;
    private String jwtId;
    private LocalDateTime issuedAt;
    private LocalDateTime expiry;
    private String issuer;
    private String audience;
    private boolean active;
    private LocalDateTime lastLogin;

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDateTime.now());
    }

    /**
     * Check if token is access token
     */
    public boolean isAccessToken() {
        return "access".equals(tokenType);
    }

    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken() {
        return "refresh".equals(tokenType);
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null) return false;
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}