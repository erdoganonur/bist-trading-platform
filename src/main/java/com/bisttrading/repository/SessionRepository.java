package com.bisttrading.repository;

import com.bisttrading.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserSessionEntity operations
 * Simplified version with only basic JpaRepository operations
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface SessionRepository extends JpaRepository<UserSessionEntity, String> {

    // Basic session lookups using Spring Data JPA method naming conventions

    /**
     * Find session by session token
     */
    Optional<UserSessionEntity> findBySessionToken(String sessionToken);

    /**
     * Find session by refresh token
     */
    Optional<UserSessionEntity> findByRefreshToken(String refreshToken);

    /**
     * Find sessions by user ID
     */
    List<UserSessionEntity> findByUserId(String userId);

    /**
     * Find sessions by status
     */
    List<UserSessionEntity> findByStatus(UserSessionEntity.SessionStatus status);

    /**
     * Find sessions by IP address
     */
    List<UserSessionEntity> findByIpAddress(String ipAddress);

    /**
     * Count sessions by status
     */
    long countByStatus(UserSessionEntity.SessionStatus status);

    /**
     * Find session by ID and user ID
     */
    Optional<UserSessionEntity> findByIdAndUserId(String id, String userId);
}