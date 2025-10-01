package com.bisttrading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Simplified base repository interface for REAL monolith
 * Only basic JpaRepository operations
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 *
 * @author BIST Trading Platform
 * @version 2.0
 * @since Real Monolith Simplification
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // Only basic JpaRepository operations
}