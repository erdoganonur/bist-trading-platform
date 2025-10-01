package com.bisttrading.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface for BIST Trading Platform
 * Provides common CRUD and query operations for all entities
 *
 * @param <T> Entity type
 */
@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {

    /**
     * Find all active entities (where is_active = true)
     *
     * @return List of active entities
     */
    List<T> findAllActive();

    /**
     * Find active entities with pagination
     *
     * @param pageable Pagination parameters
     * @return Page of active entities
     */
    Page<T> findAllActive(Pageable pageable);

    /**
     * Find entity by ID only if active
     *
     * @param id Entity ID
     * @return Optional active entity
     */
    Optional<T> findByIdActive(UUID id);

    /**
     * Soft delete entity by setting is_active = false
     *
     * @param id Entity ID
     * @return Number of affected rows
     */
    int softDeleteById(UUID id);

    /**
     * Soft delete multiple entities
     *
     * @param ids List of entity IDs
     * @return Number of affected rows
     */
    int softDeleteByIds(List<UUID> ids);

    /**
     * Restore soft deleted entity
     *
     * @param id Entity ID
     * @return Number of affected rows
     */
    int restoreById(UUID id);

    /**
     * Count active entities
     *
     * @return Number of active entities
     */
    long countActive();

    /**
     * Check if entity exists and is active
     *
     * @param id Entity ID
     * @return True if entity exists and is active
     */
    boolean existsByIdActive(UUID id);

    /**
     * Find entities created after specified date
     *
     * @param date Date threshold
     * @return List of entities
     */
    List<T> findCreatedAfter(java.time.Instant date);

    /**
     * Find entities updated after specified date
     *
     * @param date Date threshold
     * @return List of entities
     */
    List<T> findUpdatedAfter(java.time.Instant date);

    /**
     * Bulk update operation using native query
     *
     * @param field Field name to update
     * @param value New value
     * @param ids List of entity IDs
     * @return Number of affected rows
     */
    int bulkUpdate(String field, Object value, List<UUID> ids);

    /**
     * Execute custom native query with parameters
     *
     * @param query Native SQL query
     * @param parameters Query parameters
     * @return List of results
     */
    List<Object[]> executeNativeQuery(String query, Object... parameters);

    /**
     * Execute custom native query returning single result
     *
     * @param query Native SQL query
     * @param parameters Query parameters
     * @return Single result
     */
    Optional<Object[]> executeNativeQuerySingle(String query, Object... parameters);

    /**
     * Get entity statistics (count, creation dates, etc.)
     *
     * @return Statistics object
     */
    RepositoryStatistics getStatistics();

    /**
     * Refresh entity from database (useful after bulk operations)
     *
     * @param entity Entity to refresh
     */
    void refresh(T entity);

    /**
     * Detach entity from persistence context
     *
     * @param entity Entity to detach
     */
    void detach(T entity);

    /**
     * Check if entity is managed by persistence context
     *
     * @param entity Entity to check
     * @return True if managed
     */
    boolean isManaged(T entity);
}