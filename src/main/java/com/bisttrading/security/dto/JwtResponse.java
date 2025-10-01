package com.bisttrading.core.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * JWT authentication response DTO.
 * Contains authentication tokens and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    /**
     * Access token for API authentication.
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * Refresh token for token renewal.
     */
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Token type (usually "Bearer").
     */
    @JsonProperty("tokenType")
    private String tokenType = "Bearer";

    /**
     * Access token expiry time in seconds.
     */
    @JsonProperty("expiresIn")
    private long expiresIn;

    /**
     * Access token expiry date/time.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    /**
     * Refresh token expiry time in seconds.
     */
    @JsonProperty("refreshExpiresIn")
    private long refreshExpiresIn;

    /**
     * Refresh token expiry date/time.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("refreshExpiresAt")
    private LocalDateTime refreshExpiresAt;

    /**
     * User information.
     */
    @JsonProperty("user")
    private UserInfo user;

    /**
     * Session information.
     */
    @JsonProperty("session")
    private SessionInfo session;

    /**
     * User information embedded in JWT response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {

        /**
         * User ID.
         */
        @JsonProperty("userId")
        private String userId;

        /**
         * User email.
         */
        @JsonProperty("email")
        private String email;

        /**
         * Username.
         */
        @JsonProperty("username")
        private String username;

        /**
         * User's full name.
         */
        @JsonProperty("fullName")
        private String fullName;

        /**
         * User's first name.
         */
        @JsonProperty("firstName")
        private String firstName;

        /**
         * User's last name.
         */
        @JsonProperty("lastName")
        private String lastName;

        /**
         * User roles.
         */
        @JsonProperty("roles")
        private Set<String> roles;

        /**
         * User permissions.
         */
        @JsonProperty("permissions")
        private Set<String> permissions;

        /**
         * Whether user account is active.
         */
        @JsonProperty("active")
        private boolean active;

        /**
         * Whether email is verified.
         */
        @JsonProperty("emailVerified")
        private boolean emailVerified;

        /**
         * Whether phone is verified.
         */
        @JsonProperty("phoneVerified")
        private boolean phoneVerified;

        /**
         * Whether KYC is completed.
         */
        @JsonProperty("kycCompleted")
        private boolean kycCompleted;

        /**
         * Whether two-factor authentication is enabled.
         */
        @JsonProperty("twoFactorEnabled")
        private boolean twoFactorEnabled;

        /**
         * Whether user must change password.
         */
        @JsonProperty("mustChangePassword")
        private boolean mustChangePassword;

        /**
         * User's preferred language.
         */
        @JsonProperty("preferredLanguage")
        private String preferredLanguage;

        /**
         * User's timezone.
         */
        @JsonProperty("timezone")
        private String timezone;

        /**
         * User's risk profile.
         */
        @JsonProperty("riskProfile")
        private String riskProfile;

        /**
         * Whether user is a professional investor.
         */
        @JsonProperty("professionalInvestor")
        private boolean professionalInvestor;

        /**
         * Last login date.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("lastLoginDate")
        private LocalDateTime lastLoginDate;
    }

    /**
     * Session information for the authentication.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {

        /**
         * Session ID.
         */
        @JsonProperty("sessionId")
        private String sessionId;

        /**
         * Client IP address.
         */
        @JsonProperty("clientIp")
        private String clientIp;

        /**
         * User agent.
         */
        @JsonProperty("userAgent")
        private String userAgent;

        /**
         * Login timestamp.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("loginTime")
        private LocalDateTime loginTime;

        /**
         * Whether session is remembered.
         */
        @JsonProperty("rememberMe")
        private boolean rememberMe;

        /**
         * Device fingerprint.
         */
        @JsonProperty("deviceFingerprint")
        private String deviceFingerprint;

        /**
         * Login method (PASSWORD, 2FA, etc.).
         */
        @JsonProperty("loginMethod")
        private String loginMethod;

        /**
         * Session expiry time.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("sessionExpiry")
        private LocalDateTime sessionExpiry;
    }

    /**
     * Gets the authorization header value.
     *
     * @return Authorization header value
     */
    public String getAuthorizationHeader() {
        return tokenType + " " + accessToken;
    }

    /**
     * Checks if access token is expired.
     *
     * @return true if expired
     */
    public boolean isAccessTokenExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Checks if refresh token is expired.
     *
     * @return true if expired
     */
    public boolean isRefreshTokenExpired() {
        return refreshExpiresAt != null && refreshExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Gets remaining time until access token expiry in seconds.
     *
     * @return Seconds until expiry
     */
    public long getSecondsUntilExpiry() {
        if (expiresAt == null) {
            return 0;
        }
        long secondsUntil = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        return Math.max(0, secondsUntil);
    }

    /**
     * Checks if user needs to complete additional verification steps.
     *
     * @return true if verification is needed
     */
    public boolean needsVerification() {
        if (user == null) {
            return false;
        }
        return !user.emailVerified || !user.phoneVerified || !user.kycCompleted;
    }

    /**
     * Checks if user can perform trading operations.
     *
     * @return true if user can trade
     */
    public boolean canTrade() {
        if (user == null) {
            return false;
        }
        return user.active && user.emailVerified && user.phoneVerified &&
               user.kycCompleted && !user.mustChangePassword &&
               (user.roles.contains("TRADER") || user.roles.contains("CUSTOMER") ||
                user.roles.contains("PROFESSIONAL_TRADER") || user.roles.contains("RETAIL_CUSTOMER"));
    }

    /**
     * Gets user display name.
     *
     * @return Display name
     */
    public String getUserDisplayName() {
        if (user == null) {
            return null;
        }
        if (user.fullName != null && !user.fullName.trim().isEmpty()) {
            return user.fullName;
        }
        if (user.username != null && !user.username.trim().isEmpty()) {
            return user.username;
        }
        return user.email;
    }

    /**
     * Checks if user has admin privileges.
     *
     * @return true if admin
     */
    public boolean isAdmin() {
        return user != null && user.roles != null &&
               (user.roles.contains("ADMIN") || user.roles.contains("SUPER_ADMIN"));
    }

    /**
     * Checks if user is a trader.
     *
     * @return true if trader
     */
    public boolean isTrader() {
        return user != null && user.roles != null &&
               (user.roles.contains("TRADER") || user.roles.contains("PROFESSIONAL_TRADER"));
    }

    /**
     * Checks if user is a customer.
     *
     * @return true if customer
     */
    public boolean isCustomer() {
        return user != null && user.roles != null &&
               (user.roles.contains("CUSTOMER") || user.roles.contains("RETAIL_CUSTOMER"));
    }

    /**
     * Creates a string representation for logging (without sensitive data).
     *
     * @return Log-safe string
     */
    public String toLogString() {
        return String.format("JwtResponse{userId='%s', email='%s', roles=%s, expiresIn=%d, canTrade=%b}",
            user != null ? user.userId : null,
            user != null ? user.email : null,
            user != null ? user.roles : null,
            expiresIn,
            canTrade());
    }
}