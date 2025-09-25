package com.bisttrading.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface providing common operations for all entities
 * Supports soft delete, audit fields, and batch operations
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Find all entities excluding soft deleted ones
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAllActive();

    /**
     * Find entity by ID excluding soft deleted ones
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<T> findActiveById(@Param("id") ID id);

    /**
     * Find entities created within date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate AND e.deletedAt IS NULL")
    List<T> findByCreatedAtBetween(@Param("startDate") ZonedDateTime startDate,
                                   @Param("endDate") ZonedDateTime endDate);

    /**
     * Find entities updated within date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt BETWEEN :startDate AND :endDate AND e.deletedAt IS NULL")
    List<T> findByUpdatedAtBetween(@Param("startDate") ZonedDateTime startDate,
                                   @Param("endDate") ZonedDateTime endDate);

    /**
     * Count active entities
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    long countActive();

    /**
     * Soft delete entity by ID
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id);

    /**
     * Soft delete multiple entities by IDs
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id IN :ids")
    int softDeleteByIds(@Param("ids") List<ID> ids);

    /**
     * Restore soft deleted entity
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = NULL WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    /**
     * Check if entity exists and is active
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id AND e.deletedAt IS NULL")
    boolean existsActive(@Param("id") ID id);

    /**
     * Find entities created after specific date
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt > :date AND e.deletedAt IS NULL")
    List<T> findCreatedAfter(@Param("date") ZonedDateTime date);

    /**
     * Find entities updated after specific date
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt > :date AND e.deletedAt IS NULL")
    List<T> findUpdatedAfter(@Param("date") ZonedDateTime date);

    /**
     * Bulk update updatedAt timestamp
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.updatedAt = CURRENT_TIMESTAMP WHERE e.id IN :ids")
    int touchByIds(@Param("ids") List<ID> ids);
}