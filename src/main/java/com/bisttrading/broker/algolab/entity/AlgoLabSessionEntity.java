package com.bisttrading.broker.algolab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for AlgoLab session storage.
 * Stores authentication tokens and hashes with expiration tracking.
 */
@Entity
@Table(
    name = "algolab_sessions",
    indexes = {
        @Index(name = "idx_algolab_user_id", columnList = "user_id"),
        @Index(name = "idx_algolab_active", columnList = "active"),
        @Index(name = "idx_algolab_expires_at", columnList = "expires_at"),
        @Index(name = "idx_algolab_user_active", columnList = "user_id,active")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoLabSessionEntity {

    /**
     * Primary key - UUID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User ID (foreign key to user_entity table)
     * Can be null for system-level sessions
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * AlgoLab authentication token from LoginUser response
     * Encrypted token from SMS authentication flow
     */
    @Column(name = "token", nullable = false, length = 500)
    private String token;

    /**
     * AlgoLab authorization hash from LoginUserControl response
     * Used in Authorization header for authenticated requests
     */
    @Column(name = "hash", nullable = false, length = 1000)
    private String hash;

    /**
     * Session creation timestamp
     * Automatically set on insert
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Session expiration timestamp
     * Default: 24 hours from creation
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Last session refresh timestamp
     * Updated when session is refreshed via keep-alive
     */
    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;

    /**
     * Last update timestamp
     * Automatically updated on any change
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Session active status
     * False when session is explicitly logged out or expired
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * WebSocket connection status
     * True when WebSocket is connected using this session
     */
    @Column(name = "websocket_connected")
    @Builder.Default
    private boolean websocketConnected = false;

    /**
     * WebSocket last connection timestamp
     */
    @Column(name = "websocket_last_connected")
    private LocalDateTime websocketLastConnected;

    /**
     * IP address from which session was created
     * For security audit trail
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string
     * For security audit trail
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Session termination reason
     * e.g., "LOGOUT", "EXPIRED", "REVOKED"
     */
    @Column(name = "termination_reason", length = 100)
    private String terminationReason;

    /**
     * Session termination timestamp
     */
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    /**
     * Checks if session is expired.
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if session is valid (active and not expired).
     */
    @Transient
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Deactivates the session with reason.
     */
    public void deactivate(String reason) {
        this.active = false;
        this.terminationReason = reason;
        this.terminatedAt = LocalDateTime.now();
    }

    /**
     * Refreshes the session (extends expiration and updates last refresh time).
     */
    public void refresh(int extensionHours) {
        this.lastRefreshAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(extensionHours);
    }

    /**
     * Updates WebSocket connection status.
     */
    public void updateWebSocketStatus(boolean connected) {
        this.websocketConnected = connected;
        if (connected) {
            this.websocketLastConnected = LocalDateTime.now();
        }
    }
}
