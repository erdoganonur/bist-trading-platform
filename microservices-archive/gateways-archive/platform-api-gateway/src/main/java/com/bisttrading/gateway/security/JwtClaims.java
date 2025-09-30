package com.bisttrading.gateway.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JWT Claims Model for BIST Trading Platform.
 *
 * Contains all user and session information extracted from JWT tokens.
 * Supports multi-tenancy and custom claims for flexible user management.
 */
@Data
@Builder
@Jacksonized
public class JwtClaims {

    // Standard JWT Claims
    private String jwtId;           // jti - JWT ID for blacklisting
    private String subject;         // sub - Subject (usually user ID)
    private String issuer;          // iss - Token issuer
    private Set<String> audience;   // aud - Intended audience
    private Instant issuedAt;       // iat - Issued at timestamp
    private Instant expiresAt;      // exp - Expiration timestamp
    private Instant notBefore;      // nbf - Not valid before

    // User Identity Claims
    private String userId;          // Unique user identifier
    private String username;        // Username for login
    private String email;           // User email address
    private String firstName;       // User's first name
    private String lastName;        // User's last name

    // Authorization Claims
    @Builder.Default
    private List<String> roles = List.of();      // User roles (ADMIN, USER, TRADER, etc.)
    @Builder.Default
    private List<String> permissions = List.of(); // Specific permissions

    // Multi-tenancy Support
    private String organizationId;   // Organization/company ID
    private String organizationName; // Organization display name
    private String organizationType; // BROKER, BANK, INDIVIDUAL, etc.

    // User Status Claims
    private Boolean active;          // User account active status
    private Boolean verified;        // Email verification status
    private Boolean kycCompleted;    // KYC completion status
    private Boolean mfaEnabled;      // Multi-factor authentication status

    // Trading-specific Claims
    private String tradingStatus;    // ACTIVE, SUSPENDED, RESTRICTED
    private List<String> tradingPermissions; // Specific trading permissions
    private String riskProfile;     // CONSERVATIVE, MODERATE, AGGRESSIVE
    private Boolean professionalInvestor; // Professional investor status

    // Session Information
    private String sessionId;       // Session identifier
    private String deviceId;        // Device identifier for mobile/web
    private String ipAddress;       // IP address when token was issued
    private String userAgent;       // User agent information

    // Custom Claims for Extensibility
    @Builder.Default
    private Map<String, Object> customClaims = Map.of();

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null) return false;
        return java.util.Arrays.stream(roles)
            .anyMatch(this.roles::contains);
    }

    /**
     * Check if user has all specified roles.
     */
    public boolean hasAllRoles(String... roles) {
        if (this.roles == null) return false;
        return java.util.Arrays.stream(roles)
            .allMatch(this.roles::contains);
    }

    /**
     * Check if user has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Check if user has any of the specified permissions.
     */
    public boolean hasAnyPermission(String... permissions) {
        if (this.permissions == null) return false;
        return java.util.Arrays.stream(permissions)
            .anyMatch(this.permissions::contains);
    }

    /**
     * Check if user is admin.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    /**
     * Check if user can trade.
     */
    public boolean canTrade() {
        return Boolean.TRUE.equals(active) &&
               Boolean.TRUE.equals(kycCompleted) &&
               ("ACTIVE".equals(tradingStatus) || tradingStatus == null) &&
               (tradingPermissions == null || !tradingPermissions.isEmpty());
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is not yet valid.
     */
    public boolean isNotYetValid() {
        return notBefore != null && Instant.now().isBefore(notBefore);
    }

    /**
     * Get user's full name.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username != null ? username : email;
        }
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    /**
     * Get user's display name for UI.
     */
    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return email != null ? email : userId;
    }

    /**
     * Check if user belongs to specific organization.
     */
    public boolean belongsToOrganization(String orgId) {
        return organizationId != null && organizationId.equals(orgId);
    }

    /**
     * Get custom claim value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomClaim(String key, Class<T> type) {
        if (customClaims == null) return null;
        Object value = customClaims.get(key);
        if (value == null || !type.isInstance(value)) return null;
        return (T) value;
    }

    /**
     * Check if user has trading permission for specific instrument type.
     */
    public boolean canTradeInstrument(String instrumentType) {
        if (!canTrade()) return false;
        if (tradingPermissions == null) return true; // No restrictions
        return tradingPermissions.contains("ALL") ||
               tradingPermissions.contains(instrumentType.toUpperCase());
    }

    /**
     * Get remaining token lifetime in seconds.
     */
    public long getRemainingLifetimeSeconds() {
        if (expiresAt == null) return -1;
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /**
     * Create a summary string for logging (without sensitive info).
     */
    public String toLogString() {
        return String.format("JwtClaims[userId=%s, email=%s, roles=%s, org=%s, expires=%s]",
            userId,
            email != null ? email.substring(0, Math.min(email.indexOf('@'), 3)) + "***" : null,
            roles,
            organizationId,
            expiresAt);
    }
}