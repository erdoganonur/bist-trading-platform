package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.entity.AlgoLabSessionEntity;
import com.bisttrading.broker.algolab.repository.AlgoLabSessionRepository;
import com.bisttrading.broker.algolab.service.AlgoLabSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for AlgoLab session management.
 */
@RestController
@RequestMapping("/api/v1/broker/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "Manage AlgoLab authentication sessions")
public class SessionController {

    private final AlgoLabSessionManager sessionManager;
    private final AlgoLabSessionRepository sessionRepository;

    @GetMapping("/statistics")
    @Operation(summary = "Get session statistics", description = "Returns statistics about active sessions")
    public ResponseEntity<AlgoLabSessionManager.SessionStatistics> getStatistics() {
        return ResponseEntity.ok(sessionManager.getStatistics());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active sessions", description = "Returns all active sessions (requires database storage)")
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions() {
        try {
            List<AlgoLabSessionEntity> sessions = sessionRepository.findSessionsWithActiveWebSocket();

            List<Map<String, Object>> result = sessions.stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to get active sessions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user sessions", description = "Returns all sessions for a specific user")
    public ResponseEntity<List<Map<String, Object>>> getUserSessions(@PathVariable UUID userId) {
        try {
            List<AlgoLabSessionEntity> sessions =
                sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

            List<Map<String, Object>> result = sessions.stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to get user sessions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Trigger session cleanup", description = "Manually triggers cleanup of expired sessions")
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {
            sessionManager.cleanupExpiredSessions();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session cleanup triggered successfully",
                "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Failed to trigger cleanup", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Failed to trigger cleanup: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Deactivate user sessions", description = "Deactivates all active sessions for a user")
    public ResponseEntity<Map<String, Object>> deactivateUserSessions(@PathVariable UUID userId) {
        try {
            int deactivated = sessionRepository.deactivateAllUserSessions(
                userId,
                "ADMIN_REVOKE",
                LocalDateTime.now()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sessions deactivated successfully",
                "deactivatedCount", deactivated
            ));

        } catch (Exception e) {
            log.error("Failed to deactivate user sessions", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Failed to deactivate sessions: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired sessions", description = "Returns sessions that are expired but still marked active")
    public ResponseEntity<List<Map<String, Object>>> getExpiredSessions() {
        try {
            List<AlgoLabSessionEntity> sessions =
                sessionRepository.findExpiredActiveSessions(LocalDateTime.now());

            List<Map<String, Object>> result = sessions.stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to get expired sessions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/set-user/{userId}")
    @Operation(summary = "Set current user ID", description = "Sets the current user ID for session management")
    public ResponseEntity<Map<String, String>> setCurrentUser(@PathVariable UUID userId) {
        try {
            sessionManager.setCurrentUserId(userId);

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Current user ID set",
                "userId", userId.toString()
            ));

        } catch (Exception e) {
            log.error("Failed to set current user", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to set user: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Session storage health", description = "Returns health status of session storage")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            var stats = sessionManager.getStatistics();

            Map<String, Object> health = new HashMap<>();
            health.put("storage", stats.getStorage());
            health.put("healthy", stats.getError() == null);
            health.put("activeSessions", stats.getTotalActiveSessions());
            health.put("error", stats.getError());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "storage", "unknown",
                    "healthy", false,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Converts session entity to info map.
     */
    private Map<String, Object> toSessionInfo(AlgoLabSessionEntity session) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", session.getId());
        info.put("userId", session.getUserId());
        info.put("createdAt", session.getCreatedAt());
        info.put("expiresAt", session.getExpiresAt());
        info.put("lastRefreshAt", session.getLastRefreshAt());
        info.put("active", session.isActive());
        info.put("websocketConnected", session.isWebsocketConnected());
        info.put("websocketLastConnected", session.getWebsocketLastConnected());
        info.put("ipAddress", session.getIpAddress());
        info.put("terminationReason", session.getTerminationReason());
        info.put("isExpired", session.isExpired());
        info.put("isValid", session.isValid());

        return info;
    }
}
