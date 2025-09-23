package com.bisttrading.core.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * JWT configuration properties for the BIST Trading Platform.
 * Handles token settings including expiry times, secrets, and security parameters.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bist.security.jwt")
public class JwtProperties {

    /**
     * JWT secret key for signing tokens (RS256).
     * Should be a strong, randomly generated key in production.
     */
    @NotBlank(message = "JWT secret anahtarı boş olamaz")
    private String secret = "bist-trading-platform-jwt-secret-key-2024-change-in-production";

    /**
     * Access token expiry duration (15 minutes default).
     */
    @NotNull(message = "Access token süresi boş olamaz")
    private Duration accessTokenExpiry = Duration.ofMinutes(15);

    /**
     * Refresh token expiry duration (7 days default).
     */
    @NotNull(message = "Refresh token süresi boş olamaz")
    private Duration refreshTokenExpiry = Duration.ofDays(7);

    /**
     * Token issuer name.
     */
    @NotBlank(message = "Token issuer boş olamaz")
    private String issuer = "bist-trading-platform";

    /**
     * Token audience.
     */
    @NotBlank(message = "Token audience boş olamaz")
    private String audience = "bist-trading-users";

    /**
     * JWT header name in HTTP requests.
     */
    @NotBlank(message = "Header adı boş olamaz")
    private String headerName = "Authorization";

    /**
     * JWT token prefix (e.g., "Bearer ").
     */
    @NotBlank(message = "Token prefix boş olamaz")
    private String tokenPrefix = "Bearer ";

    /**
     * Clock skew tolerance for token validation.
     */
    private Duration clockSkew = Duration.ofMinutes(2);

    /**
     * Maximum number of refresh attempts per user per hour.
     */
    @Positive(message = "Maksimum refresh sayısı pozitif olmalı")
    private int maxRefreshAttemptsPerHour = 10;

    /**
     * Whether to enable token blacklist functionality.
     */
    private boolean enableBlacklist = true;

    /**
     * Blacklist cleanup interval.
     */
    private Duration blacklistCleanupInterval = Duration.ofHours(1);

    /**
     * Token length for random token generation.
     */
    @Positive(message = "Token uzunluğu pozitif olmalı")
    private int tokenLength = 32;

    /**
     * Whether to include user details in token claims.
     */
    private boolean includeUserDetailsInToken = false;

    /**
     * Custom claims configuration.
     */
    private Claims claims = new Claims();

    /**
     * Rate limiting configuration.
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * Custom claims configuration.
     */
    @Data
    public static class Claims {
        /**
         * Whether to include user roles in token.
         */
        private boolean includeRoles = true;

        /**
         * Whether to include user permissions in token.
         */
        private boolean includePermissions = false;

        /**
         * Whether to include user profile info in token.
         */
        private boolean includeProfile = false;

        /**
         * Whether to include last login time in token.
         */
        private boolean includeLastLogin = false;

        /**
         * Custom claim prefix.
         */
        private String customClaimPrefix = "bist_";
    }

    /**
     * Rate limiting configuration for JWT endpoints.
     */
    @Data
    public static class RateLimit {
        /**
         * Whether to enable rate limiting.
         */
        private boolean enabled = true;

        /**
         * Number of requests allowed per time window.
         */
        @Positive(message = "İstek sayısı pozitif olmalı")
        private int requestsPerWindow = 5;

        /**
         * Time window duration for rate limiting.
         */
        private Duration windowDuration = Duration.ofMinutes(1);

        /**
         * Number of requests allowed for login attempts.
         */
        @Positive(message = "Login istek sayısı pozitif olmalı")
        private int loginRequestsPerWindow = 3;

        /**
         * Time window for login rate limiting.
         */
        private Duration loginWindowDuration = Duration.ofMinutes(5);

        /**
         * Number of requests allowed for registration.
         */
        @Positive(message = "Kayıt istek sayısı pozitif olmalı")
        private int registrationRequestsPerWindow = 2;

        /**
         * Time window for registration rate limiting.
         */
        private Duration registrationWindowDuration = Duration.ofMinutes(10);
    }

    /**
     * Gets access token expiry in seconds.
     *
     * @return Access token expiry in seconds
     */
    public long getAccessTokenExpiryInSeconds() {
        return accessTokenExpiry.getSeconds();
    }

    /**
     * Gets refresh token expiry in seconds.
     *
     * @return Refresh token expiry in seconds
     */
    public long getRefreshTokenExpiryInSeconds() {
        return refreshTokenExpiry.getSeconds();
    }

    /**
     * Gets clock skew in seconds.
     *
     * @return Clock skew in seconds
     */
    public long getClockSkewInSeconds() {
        return clockSkew.getSeconds();
    }

    /**
     * Checks if the secret key is using the default value.
     *
     * @return true if using default secret
     */
    public boolean isUsingDefaultSecret() {
        return secret != null && secret.contains("change-in-production");
    }

    /**
     * Validates the JWT properties configuration.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (isUsingDefaultSecret()) {
            throw new IllegalStateException(
                "Üretim ortamında varsayılan JWT secret anahtarı kullanılamaz. " +
                "Güvenli bir anahtar tanımlayın."
            );
        }

        if (accessTokenExpiry.isZero() || accessTokenExpiry.isNegative()) {
            throw new IllegalStateException("Access token süresi pozitif olmalıdır");
        }

        if (refreshTokenExpiry.isZero() || refreshTokenExpiry.isNegative()) {
            throw new IllegalStateException("Refresh token süresi pozitif olmalıdır");
        }

        if (refreshTokenExpiry.compareTo(accessTokenExpiry) <= 0) {
            throw new IllegalStateException(
                "Refresh token süresi access token süresinden uzun olmalıdır"
            );
        }

        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret anahtarı en az 32 karakter olmalıdır"
            );
        }
    }

    /**
     * Gets the token prefix without trailing space.
     *
     * @return Clean token prefix
     */
    public String getCleanTokenPrefix() {
        return tokenPrefix.trim();
    }

    /**
     * Checks if a token type is supported.
     *
     * @param tokenType Token type to check
     * @return true if supported
     */
    public boolean isTokenTypeSupported(String tokenType) {
        return "access".equalsIgnoreCase(tokenType) ||
               "refresh".equalsIgnoreCase(tokenType);
    }

    /**
     * Gets token expiry for given type.
     *
     * @param tokenType Token type (access/refresh)
     * @return Token expiry duration
     */
    public Duration getTokenExpiry(String tokenType) {
        return switch (tokenType.toLowerCase()) {
            case "access" -> accessTokenExpiry;
            case "refresh" -> refreshTokenExpiry;
            default -> throw new IllegalArgumentException("Desteklenmeyen token türü: " + tokenType);
        };
    }
}