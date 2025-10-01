package com.bisttrading.entity;

import com.bisttrading.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionEntity extends BaseEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private String userId;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "device_description")
    private String deviceDescription;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "logout_reason")
    private LogoutReason logoutReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return active && !isExpired();
    }

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET
    }

    public enum SecurityLevel {
        STANDARD, HIGH
    }

    public enum SessionStatus {
        ACTIVE, INACTIVE, EXPIRED, TERMINATED, ENDED
    }

    public enum LogoutReason {
        USER_LOGOUT, EXPIRED, ADMIN_LOGOUT, SECURITY_VIOLATION, OTHER, SESSION_TIMEOUT
    }
}