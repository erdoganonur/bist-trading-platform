package com.bisttrading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository interface for REAL monolith.
 * Provides JPA operations and Specification support for dynamic queries.
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 *
 * @author BIST Trading Platform
 * @version 2.0
 * @since Real Monolith Simplification
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    // JpaRepository + JpaSpecificationExecutor for comprehensive data access
}