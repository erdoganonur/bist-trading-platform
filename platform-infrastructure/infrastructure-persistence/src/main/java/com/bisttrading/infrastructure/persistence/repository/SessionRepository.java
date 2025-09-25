package com.bisttrading.infrastructure.persistence.repository;

import com.bisttrading.infrastructure.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserSessionEntity operations.
 * Provides custom query methods for session management.
 */
@Repository
public interface SessionRepository extends JpaRepository<UserSessionEntity, String> {

    /**
     * Finds session by session token.
     *
     * @param sessionToken Session token
     * @return Optional UserSessionEntity
     */
    Optional<UserSessionEntity> findBySessionToken(String sessionToken);

    /**
     * Finds session by refresh token.
     *
     * @param refreshToken Refresh token
     * @return Optional UserSessionEntity
     */
    Optional<UserSessionEntity> findByRefreshToken(String refreshToken);

    /**
     * Finds all sessions for a user.
     *
     * @param userId User ID
     * @return List of user sessions
     */
    List<UserSessionEntity> findByUserId(String userId);

    /**
     * Finds active sessions for a user.
     *
     * @param userId User ID
     * @return List of active user sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND s.expiresAt > CURRENT_TIMESTAMP")
    List<UserSessionEntity> findActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * Finds all sessions by status.
     *
     * @param status Session status
     * @return List of sessions with specified status
     */
    List<UserSessionEntity> findByStatus(UserSessionEntity.SessionStatus status);

    /**
     * Finds expired sessions.
     *
     * @param now Current timestamp
     * @return List of expired sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.expiresAt < :now AND s.status = 'ACTIVE'")
    List<UserSessionEntity> findExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Finds sessions by IP address.
     *
     * @param ipAddress IP address
     * @return List of sessions from specified IP
     */
    List<UserSessionEntity> findByIpAddress(String ipAddress);

    /**
     * Finds sessions by device fingerprint.
     *
     * @param deviceFingerprint Device fingerprint
     * @return List of sessions from specified device
     */
    List<UserSessionEntity> findByDeviceFingerprint(String deviceFingerprint);

    /**
     * Finds sessions by device type.
     *
     * @param deviceType Device type
     * @return List of sessions from specified device type
     */
    List<UserSessionEntity> findByDeviceType(UserSessionEntity.DeviceType deviceType);

    /**
     * Finds sessions by location country.
     *
     * @param locationCountry Country code
     * @return List of sessions from specified country
     */
    List<UserSessionEntity> findByLocationCountry(String locationCountry);

    /**
     * Finds sessions created within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of sessions created in date range
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.startedAt BETWEEN :startDate AND :endDate")
    List<UserSessionEntity> findByStartedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Finds long-running sessions (active for more than specified hours).
     *
     * @param hours Hours threshold
     * @return List of long-running sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.status = 'ACTIVE' AND s.startedAt < :cutoffTime")
    List<UserSessionEntity> findLongRunningSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Finds inactive sessions (no activity for specified minutes).
     *
     * @param cutoffTime Cutoff time for last activity
     * @return List of inactive sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.status = 'ACTIVE' AND " +
           "(s.lastActivityAt < :cutoffTime OR s.lastActivityAt IS NULL)")
    List<UserSessionEntity> findInactiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Finds sessions by security level.
     *
     * @param securityLevel Security level
     * @return List of sessions with specified security level
     */
    List<UserSessionEntity> findBySecurityLevel(UserSessionEntity.SecurityLevel securityLevel);

    /**
     * Finds remembered sessions.
     *
     * @return List of remembered sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.isRemembered = true")
    List<UserSessionEntity> findRememberedSessions();

    /**
     * Finds trusted device sessions.
     *
     * @return List of trusted device sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.isTrustedDevice = true")
    List<UserSessionEntity> findTrustedDeviceSessions();

    /**
     * Finds sessions with two-factor verification.
     *
     * @return List of sessions with 2FA verification
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.twoFactorVerified = true")
    List<UserSessionEntity> findTwoFactorVerifiedSessions();

    /**
     * Finds sessions by logout reason.
     *
     * @param logoutReason Logout reason
     * @return List of sessions with specified logout reason
     */
    List<UserSessionEntity> findByLogoutReason(UserSessionEntity.LogoutReason logoutReason);

    /**
     * Counts active sessions for a user.
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    @Query("SELECT COUNT(s) FROM UserSessionEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND s.expiresAt > CURRENT_TIMESTAMP")
    long countActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * Counts sessions by status.
     *
     * @param status Session status
     * @return Number of sessions with specified status
     */
    long countByStatus(UserSessionEntity.SessionStatus status);

    /**
     * Counts sessions by device type.
     *
     * @param deviceType Device type
     * @return Number of sessions from specified device type
     */
    long countByDeviceType(UserSessionEntity.DeviceType deviceType);

    /**
     * Terminates all active sessions for a user.
     *
     * @param userId User ID
     * @param logoutReason Logout reason
     * @param endedAt End timestamp
     * @return Number of terminated sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.status = 'TERMINATED', s.endedAt = :endedAt, s.logoutReason = :logoutReason " +
           "WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int terminateAllUserSessions(@Param("userId") String userId,
                                @Param("logoutReason") UserSessionEntity.LogoutReason logoutReason,
                                @Param("endedAt") LocalDateTime endedAt);

    /**
     * Terminates specific session.
     *
     * @param sessionToken Session token
     * @param logoutReason Logout reason
     * @param endedAt End timestamp
     * @return Number of terminated sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.status = 'TERMINATED', s.endedAt = :endedAt, s.logoutReason = :logoutReason " +
           "WHERE s.sessionToken = :sessionToken AND s.status = 'ACTIVE'")
    int terminateSession(@Param("sessionToken") String sessionToken,
                        @Param("logoutReason") UserSessionEntity.LogoutReason logoutReason,
                        @Param("endedAt") LocalDateTime endedAt);

    /**
     * Expires sessions that have passed their expiry time.
     *
     * @param now Current timestamp
     * @return Number of expired sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.status = 'EXPIRED', s.endedAt = :now, s.logoutReason = 'SESSION_TIMEOUT' " +
           "WHERE s.expiresAt < :now AND s.status = 'ACTIVE'")
    int expireOldSessions(@Param("now") LocalDateTime now);

    /**
     * Updates session last activity.
     *
     * @param sessionToken Session token
     * @param lastActivityAt Last activity timestamp
     * @return Number of updated sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.lastActivityAt = :lastActivityAt WHERE s.sessionToken = :sessionToken")
    int updateLastActivity(@Param("sessionToken") String sessionToken,
                          @Param("lastActivityAt") LocalDateTime lastActivityAt);

    /**
     * Extends session expiry time.
     *
     * @param sessionToken Session token
     * @param newExpiryTime New expiry time
     * @return Number of updated sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.expiresAt = :newExpiryTime, s.renewalCount = s.renewalCount + 1 " +
           "WHERE s.sessionToken = :sessionToken AND s.status = 'ACTIVE'")
    int extendSession(@Param("sessionToken") String sessionToken,
                     @Param("newExpiryTime") LocalDateTime newExpiryTime);

    /**
     * Updates session refresh token.
     *
     * @param sessionToken Session token
     * @param refreshToken New refresh token
     * @return Number of updated sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.refreshToken = :refreshToken WHERE s.sessionToken = :sessionToken")
    int updateRefreshToken(@Param("sessionToken") String sessionToken,
                          @Param("refreshToken") String refreshToken);

    /**
     * Deletes old ended sessions (cleanup).
     *
     * @param cutoffDate Cutoff date for deletion
     * @return Number of deleted sessions
     */
    @Query("DELETE FROM UserSessionEntity s WHERE s.endedAt < :cutoffDate AND s.status IN ('TERMINATED', 'EXPIRED', 'INVALIDATED')")
    int deleteOldEndedSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Gets session statistics by status.
     *
     * @return List of objects containing session statistics
     */
    @Query("SELECT s.status, COUNT(s) as count FROM UserSessionEntity s GROUP BY s.status")
    List<Object[]> getSessionStatisticsByStatus();

    /**
     * Gets session statistics by device type.
     *
     * @return List of objects containing device type statistics
     */
    @Query("SELECT s.deviceType, COUNT(s) as count FROM UserSessionEntity s GROUP BY s.deviceType")
    List<Object[]> getSessionStatisticsByDeviceType();

    /**
     * Gets concurrent sessions report.
     *
     * @param now Current timestamp
     * @return List of users with concurrent session counts
     */
    @Query("SELECT s.userId, COUNT(s) as sessionCount FROM UserSessionEntity s " +
           "WHERE s.status = 'ACTIVE' AND s.expiresAt > :now GROUP BY s.userId HAVING COUNT(s) > 1")
    List<Object[]> getConcurrentSessionsReport(@Param("now") LocalDateTime now);

    /**
     * Finds suspicious sessions (multiple IPs for same user).
     *
     * @param userId User ID
     * @param timeWindow Time window in hours to check
     * @return List of sessions with different IPs
     */
    @Query("SELECT DISTINCT s.ipAddress FROM UserSessionEntity s " +
           "WHERE s.userId = :userId AND s.startedAt > :sinceTime AND s.status = 'ACTIVE'")
    List<String> findDistinctIpAddressesForUser(@Param("userId") String userId,
                                               @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Finds sessions that may indicate account sharing.
     *
     * @param userId User ID
     * @param timeWindow Time window in minutes
     * @return List of overlapping sessions
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND " +
           "EXISTS (SELECT s2 FROM UserSessionEntity s2 WHERE s2.userId = :userId AND s2.id != s.id AND " +
           "s2.status = 'ACTIVE' AND s2.lastActivityAt > :sinceTime AND s.lastActivityAt > :sinceTime)")
    List<UserSessionEntity> findPotentialAccountSharingSessions(@Param("userId") String userId,
                                                               @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Ends all active sessions except current one for a user.
     *
     * @param userId User ID
     * @param endedAt End timestamp
     * @return Number of ended sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.status = 'ENDED', s.endedAt = :endedAt " +
           "WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int endAllActiveSessionsExceptCurrent(@Param("userId") String userId,
                                         @Param("endedAt") LocalDateTime endedAt);

    /**
     * Ends all active sessions for a user.
     *
     * @param userId User ID
     * @param endedAt End timestamp
     * @return Number of ended sessions
     */
    @Query("UPDATE UserSessionEntity s SET s.status = 'ENDED', s.endedAt = :endedAt " +
           "WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int endAllActiveSessionsForUser(@Param("userId") String userId,
                                   @Param("endedAt") LocalDateTime endedAt);

    /**
     * Finds session by ID and user ID.
     *
     * @param id Session ID
     * @param userId User ID
     * @return Optional UserSessionEntity
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.id = :id AND s.userId = :userId")
    Optional<UserSessionEntity> findByIdAndUserId(@Param("id") String id, @Param("userId") String userId);
}