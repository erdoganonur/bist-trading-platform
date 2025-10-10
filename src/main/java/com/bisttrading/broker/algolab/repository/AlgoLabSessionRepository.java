package com.bisttrading.broker.algolab.repository;

import com.bisttrading.broker.algolab.entity.AlgoLabSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AlgoLab session management.
 */
@Repository
public interface AlgoLabSessionRepository extends JpaRepository<AlgoLabSessionEntity, UUID> {

    /**
     * Finds active session for a user.
     * Returns the most recently created active session if multiple exist.
     *
     * @param userId User ID
     * @return Optional containing the active session
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.active = true " +
           "AND s.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY s.createdAt DESC")
    Optional<AlgoLabSessionEntity> findActiveSessionByUserId(@Param("userId") UUID userId);

    /**
     * Finds the most recent active session (for system-level sessions where userId is null).
     *
     * @return Optional containing the active session
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.active = true " +
           "AND s.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY s.createdAt DESC")
    Optional<AlgoLabSessionEntity> findMostRecentActiveSession();

    /**
     * Finds all active sessions for a user.
     *
     * @param userId User ID
     * @return List of active sessions
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.active = true " +
           "AND s.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY s.createdAt DESC")
    List<AlgoLabSessionEntity> findAllActiveSessionsByUserId(@Param("userId") UUID userId);

    /**
     * Finds all sessions for a user (active and inactive).
     *
     * @param userId User ID
     * @return List of all sessions
     */
    List<AlgoLabSessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds expired but still marked as active sessions.
     *
     * @param now Current timestamp
     * @return List of expired sessions
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.active = true " +
           "AND s.expiresAt < :now")
    List<AlgoLabSessionEntity> findExpiredActiveSessions(@Param("now") LocalDateTime now);

    /**
     * Finds sessions that need cleanup (expired and inactive for more than specified days).
     *
     * @param cutoffDate Cutoff date for cleanup
     * @return List of sessions to cleanup
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.active = false " +
           "AND (s.terminatedAt < :cutoffDate OR s.expiresAt < :cutoffDate)")
    List<AlgoLabSessionEntity> findSessionsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Deactivates all active sessions for a user.
     * Used when user logs out from all devices.
     *
     * @param userId User ID
     * @param reason Termination reason
     * @param now Current timestamp
     * @return Number of sessions deactivated
     */
    @Modifying
    @Query("UPDATE AlgoLabSessionEntity s " +
           "SET s.active = false, " +
           "    s.terminationReason = :reason, " +
           "    s.terminatedAt = :now " +
           "WHERE s.userId = :userId " +
           "AND s.active = true")
    int deactivateAllUserSessions(
        @Param("userId") UUID userId,
        @Param("reason") String reason,
        @Param("now") LocalDateTime now
    );

    /**
     * Deactivates expired sessions.
     *
     * @param now Current timestamp
     * @return Number of sessions deactivated
     */
    @Modifying
    @Query("UPDATE AlgoLabSessionEntity s " +
           "SET s.active = false, " +
           "    s.terminationReason = 'EXPIRED', " +
           "    s.terminatedAt = :now " +
           "WHERE s.active = true " +
           "AND s.expiresAt < :now")
    int deactivateExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Deletes old inactive sessions (hard delete).
     * Used for periodic cleanup of old data.
     *
     * @param cutoffDate Cutoff date for deletion
     * @return Number of sessions deleted
     */
    @Modifying
    @Query("DELETE FROM AlgoLabSessionEntity s " +
           "WHERE s.active = false " +
           "AND (s.terminatedAt < :cutoffDate OR s.expiresAt < :cutoffDate)")
    int deleteOldInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Counts active sessions for a user.
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    @Query("SELECT COUNT(s) FROM AlgoLabSessionEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.active = true " +
           "AND s.expiresAt > CURRENT_TIMESTAMP")
    long countActiveSessionsByUserId(@Param("userId") UUID userId);

    /**
     * Counts all active sessions in the system.
     *
     * @return Total number of active sessions
     */
    @Query("SELECT COUNT(s) FROM AlgoLabSessionEntity s " +
           "WHERE s.active = true " +
           "AND s.expiresAt > CURRENT_TIMESTAMP")
    long countAllActiveSessions();

    /**
     * Finds sessions with active WebSocket connections.
     *
     * @return List of sessions with WebSocket connected
     */
    @Query("SELECT s FROM AlgoLabSessionEntity s " +
           "WHERE s.active = true " +
           "AND s.websocketConnected = true " +
           "ORDER BY s.websocketLastConnected DESC")
    List<AlgoLabSessionEntity> findSessionsWithActiveWebSocket();

    /**
     * Checks if a session exists by token.
     *
     * @param token AlgoLab token
     * @return true if session exists
     */
    boolean existsByTokenAndActiveTrue(String token);

    /**
     * Finds session by token.
     *
     * @param token AlgoLab token
     * @return Optional containing the session
     */
    Optional<AlgoLabSessionEntity> findByTokenAndActiveTrue(String token);
}
