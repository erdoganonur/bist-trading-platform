package com.bisttrading.infrastructure.persistence.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * User session entity for BIST Trading Platform.
 * Tracks active user sessions for security and audit purposes.
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_session_token", columnList = "session_token"),
    @Index(name = "idx_user_sessions_status", columnList = "status"),
    @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at"),
    @Index(name = "idx_user_sessions_created_at", columnList = "created_at"),
    @Index(name = "idx_user_sessions_ip_address", columnList = "ip_address")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserSessionEntity {

    /**
     * Primary key - UUID.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    /**
     * User reference.
     */
    @Column(name = "user_id", nullable = false, length = 36)
    @NotBlank(message = "Kullanıcı ID'si boş olamaz")
    private String userId;

    /**
     * Session token (JWT ID or session identifier).
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 500)
    @NotBlank(message = "Session token boş olamaz")
    private String sessionToken;

    /**
     * Refresh token for session renewal.
     */
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    /**
     * Session status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Session durumu boş olamaz")
    private SessionStatus status;

    /**
     * Client IP address.
     */
    @Column(name = "ip_address", length = 45)
    @Size(max = 45, message = "IP adresi en fazla 45 karakter olabilir")
    private String ipAddress;

    /**
     * User agent string.
     */
    @Column(name = "user_agent", length = 1000)
    @Size(max = 1000, message = "User agent en fazla 1000 karakter olabilir")
    private String userAgent;

    /**
     * Device fingerprint for additional security.
     */
    @Column(name = "device_fingerprint", length = 255)
    @Size(max = 255, message = "Device fingerprint en fazla 255 karakter olabilir")
    private String deviceFingerprint;

    /**
     * Device type (DESKTOP, MOBILE, TABLET).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    /**
     * Operating system information.
     */
    @Column(name = "operating_system", length = 100)
    @Size(max = 100, message = "İşletim sistemi bilgisi en fazla 100 karakter olabilir")
    private String operatingSystem;

    /**
     * Browser information.
     */
    @Column(name = "browser", length = 100)
    @Size(max = 100, message = "Tarayıcı bilgisi en fazla 100 karakter olabilir")
    private String browser;

    /**
     * Geographic location (country code).
     */
    @Column(name = "location_country", length = 3)
    @Size(max = 3, message = "Ülke kodu en fazla 3 karakter olabilir")
    private String locationCountry;

    /**
     * Geographic location (city).
     */
    @Column(name = "location_city", length = 100)
    @Size(max = 100, message = "Şehir bilgisi en fazla 100 karakter olabilir")
    private String locationCity;

    /**
     * Session start time.
     */
    @Column(name = "started_at", nullable = false)
    @NotNull(message = "Session başlangıç zamanı boş olamaz")
    private LocalDateTime startedAt;

    /**
     * Session expiry time.
     */
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Session bitiş zamanı boş olamaz")
    private LocalDateTime expiresAt;

    /**
     * Last activity time.
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * Session end time.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Logout reason.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "logout_reason", length = 20)
    private LogoutReason logoutReason;

    /**
     * Whether session is remembered (remember me).
     */
    @Column(name = "is_remembered", nullable = false)
    @Builder.Default
    private Boolean isRemembered = false;

    /**
     * Whether this is a trusted device.
     */
    @Column(name = "is_trusted_device", nullable = false)
    @Builder.Default
    private Boolean isTrustedDevice = false;

    /**
     * Two-factor authentication status for this session.
     */
    @Column(name = "two_factor_verified", nullable = false)
    @Builder.Default
    private Boolean twoFactorVerified = false;

    /**
     * Session security level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "security_level", length = 20)
    private SecurityLevel securityLevel;

    /**
     * Number of session extensions/renewals.
     */
    @Column(name = "renewal_count", nullable = false)
    @Builder.Default
    @PositiveOrZero(message = "Yenileme sayısı negatif olamaz")
    private Integer renewalCount = 0;

    /**
     * Additional session metadata (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Record creation timestamp.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last modification timestamp.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Session status enumeration.
     */
    public enum SessionStatus {
        ACTIVE,      // Session is active and valid
        EXPIRED,     // Session has expired
        TERMINATED,  // Session was terminated by user
        INVALIDATED, // Session was invalidated by system
        SUSPENDED    // Session is temporarily suspended
    }

    /**
     * Device type enumeration.
     */
    public enum DeviceType {
        DESKTOP,     // Desktop computer
        MOBILE,      // Mobile phone
        TABLET,      // Tablet device
        UNKNOWN      // Unknown device type
    }

    /**
     * Logout reason enumeration.
     */
    public enum LogoutReason {
        USER_LOGOUT,        // User initiated logout
        SESSION_TIMEOUT,    // Session expired due to timeout
        SECURITY_LOGOUT,    // Security-related logout
        ADMIN_LOGOUT,       // Administrative logout
        DEVICE_CHANGE,      // Device change detected
        CONCURRENT_LOGIN,   // Too many concurrent sessions
        SYSTEM_MAINTENANCE, // System maintenance
        PASSWORD_CHANGE,    // Password was changed
        ACCOUNT_SUSPENDED   // Account was suspended
    }

    /**
     * Security level enumeration.
     */
    public enum SecurityLevel {
        LOW,         // Basic authentication
        MEDIUM,      // Standard authentication with device verification
        HIGH,        // Two-factor authentication required
        CRITICAL     // Enhanced security for privileged operations
    }

    /**
     * Checks if session is currently active.
     *
     * @return true if session is active and not expired
     */
    public boolean isActive() {
        return SessionStatus.ACTIVE.equals(status) &&
               expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Checks if session has expired.
     *
     * @return true if session has expired
     */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now()) ||
               SessionStatus.EXPIRED.equals(status);
    }

    /**
     * Gets session duration in minutes.
     *
     * @return Session duration in minutes
     */
    public long getDurationMinutes() {
        LocalDateTime endTime = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMinutes();
    }

    /**
     * Gets time until session expires in minutes.
     *
     * @return Minutes until expiry, 0 if already expired
     */
    public long getMinutesUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    /**
     * Checks if session was inactive for specified minutes.
     *
     * @param minutes Inactivity threshold in minutes
     * @return true if inactive longer than threshold
     */
    public boolean isInactiveFor(int minutes) {
        if (lastActivityAt == null) {
            return false;
        }
        return lastActivityAt.isBefore(LocalDateTime.now().minusMinutes(minutes));
    }

    /**
     * Updates last activity timestamp.
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Terminates the session.
     *
     * @param reason Logout reason
     */
    public void terminate(LogoutReason reason) {
        this.status = SessionStatus.TERMINATED;
        this.endedAt = LocalDateTime.now();
        this.logoutReason = reason;
    }

    /**
     * Marks session as expired.
     */
    public void expire() {
        this.status = SessionStatus.EXPIRED;
        this.endedAt = LocalDateTime.now();
        this.logoutReason = LogoutReason.SESSION_TIMEOUT;
    }

    /**
     * Invalidates the session.
     *
     * @param reason Logout reason
     */
    public void invalidate(LogoutReason reason) {
        this.status = SessionStatus.INVALIDATED;
        this.endedAt = LocalDateTime.now();
        this.logoutReason = reason;
    }

    /**
     * Extends session expiry time.
     *
     * @param minutes Minutes to extend
     */
    public void extendSession(int minutes) {
        this.expiresAt = this.expiresAt.plusMinutes(minutes);
        this.renewalCount = this.renewalCount + 1;
        updateLastActivity();
    }

    /**
     * Gets a masked version of the session token for logging.
     *
     * @return Masked session token
     */
    public String getMaskedSessionToken() {
        if (sessionToken == null || sessionToken.length() < 10) {
            return "***";
        }
        return sessionToken.substring(0, 4) + "***" + sessionToken.substring(sessionToken.length() - 4);
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void prePersist() {
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = startedAt;
        }
        if (securityLevel == null) {
            securityLevel = SecurityLevel.MEDIUM;
        }
        if (deviceType == null) {
            deviceType = DeviceType.UNKNOWN;
        }
    }
}