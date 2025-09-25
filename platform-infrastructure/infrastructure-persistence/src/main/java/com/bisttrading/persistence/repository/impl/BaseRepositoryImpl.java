package com.bisttrading.persistence.repository.impl;

import com.bisttrading.persistence.repository.BaseRepository;
import com.bisttrading.persistence.repository.RepositoryStatistics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository implementation for BIST Trading Platform
 * Provides common CRUD operations and soft delete functionality
 *
 * @param <T> Entity type
 */
@Transactional(readOnly = true)
public class BaseRepositoryImpl<T> extends SimpleJpaRepository<T, UUID> implements BaseRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(BaseRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaEntityInformation<T, UUID> entityInformation;
    private final String entityName;
    private final String tableName;

    public BaseRepositoryImpl(JpaEntityInformation<T, UUID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
        this.entityName = entityInformation.getEntityName();
        this.tableName = getTableName();
    }

    @Override
    public List<T> findAllActive() {
        try {
            String jpql = String.format("SELECT e FROM %s e WHERE e.isActive = true ORDER BY e.createdAt DESC", entityName);
            return entityManager.createQuery(jpql, getDomainClass()).getResultList();
        } catch (Exception e) {
            logger.debug("Entity {} does not have isActive field, falling back to findAll", entityName);
            return findAll();
        }
    }

    @Override
    public Page<T> findAllActive(Pageable pageable) {
        try {
            Specification<T> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isActive"), true);
            return findAll(spec, pageable);
        } catch (Exception e) {
            logger.debug("Entity {} does not have isActive field, falling back to findAll with pagination", entityName);
            return findAll(pageable);
        }
    }

    @Override
    public Optional<T> findByIdActive(UUID id) {
        try {
            String jpql = String.format("SELECT e FROM %s e WHERE e.id = :id AND e.isActive = true", entityName);
            T result = entityManager.createQuery(jpql, getDomainClass())
                .setParameter("id", id)
                .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            logger.debug("Entity {} does not have isActive field, falling back to findById", entityName);
            return findById(id);
        }
    }

    @Override
    @Transactional
    public int softDeleteById(UUID id) {
        try {
            String jpql = String.format("UPDATE %s e SET e.isActive = false, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id", entityName);
            int updated = entityManager.createQuery(jpql)
                .setParameter("id", id)
                .executeUpdate();

            if (updated > 0) {
                logger.debug("Soft deleted entity {} with id: {}", entityName, id);
            }

            return updated;
        } catch (Exception e) {
            logger.warn("Soft delete not supported for entity {}, performing hard delete", entityName);
            Optional<T> entity = findById(id);
            if (entity.isPresent()) {
                delete(entity.get());
                return 1;
            }
            return 0;
        }
    }

    @Override
    @Transactional
    public int softDeleteByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        try {
            String jpql = String.format("UPDATE %s e SET e.isActive = false, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id IN :ids", entityName);
            int updated = entityManager.createQuery(jpql)
                .setParameter("ids", ids)
                .executeUpdate();

            if (updated > 0) {
                logger.debug("Soft deleted {} entities of type {} with ids: {}", updated, entityName, ids);
            }

            return updated;
        } catch (Exception e) {
            logger.warn("Soft delete not supported for entity {}, performing hard delete", entityName);
            List<T> entities = findAllById(ids);
            deleteAll(entities);
            return entities.size();
        }
    }

    @Override
    @Transactional
    public int restoreById(UUID id) {
        try {
            String jpql = String.format("UPDATE %s e SET e.isActive = true, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id", entityName);
            int updated = entityManager.createQuery(jpql)
                .setParameter("id", id)
                .executeUpdate();

            if (updated > 0) {
                logger.debug("Restored entity {} with id: {}", entityName, id);
            }

            return updated;
        } catch (Exception e) {
            logger.debug("Restore not supported for entity {}", entityName);
            return 0;
        }
    }

    @Override
    public long countActive() {
        try {
            String jpql = String.format("SELECT COUNT(e) FROM %s e WHERE e.isActive = true", entityName);
            return entityManager.createQuery(jpql, Long.class).getSingleResult();
        } catch (Exception e) {
            logger.debug("Entity {} does not have isActive field, falling back to count", entityName);
            return count();
        }
    }

    @Override
    public boolean existsByIdActive(UUID id) {
        try {
            String jpql = String.format("SELECT COUNT(e) FROM %s e WHERE e.id = :id AND e.isActive = true", entityName);
            Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            logger.debug("Entity {} does not have isActive field, falling back to existsById", entityName);
            return existsById(id);
        }
    }

    @Override
    public List<T> findCreatedAfter(Instant date) {
        try {
            String jpql = String.format("SELECT e FROM %s e WHERE e.createdAt > :date ORDER BY e.createdAt DESC", entityName);
            return entityManager.createQuery(jpql, getDomainClass())
                .setParameter("date", date)
                .getResultList();
        } catch (Exception e) {
            logger.debug("Entity {} does not have createdAt field", entityName);
            return List.of();
        }
    }

    @Override
    public List<T> findUpdatedAfter(Instant date) {
        try {
            String jpql = String.format("SELECT e FROM %s e WHERE e.updatedAt > :date ORDER BY e.updatedAt DESC", entityName);
            return entityManager.createQuery(jpql, getDomainClass())
                .setParameter("date", date)
                .getResultList();
        } catch (Exception e) {
            logger.debug("Entity {} does not have updatedAt field", entityName);
            return List.of();
        }
    }

    @Override
    @Transactional
    public int bulkUpdate(String field, Object value, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        try {
            String jpql = String.format("UPDATE %s e SET e.%s = :value, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id IN :ids",
                entityName, field);
            int updated = entityManager.createQuery(jpql)
                .setParameter("value", value)
                .setParameter("ids", ids)
                .executeUpdate();

            logger.debug("Bulk updated field '{}' for {} entities of type {}", field, updated, entityName);
            return updated;
        } catch (Exception e) {
            logger.error("Bulk update failed for entity {} field {}", entityName, field, e);
            return 0;
        }
    }

    @Override
    public List<Object[]> executeNativeQuery(String query, Object... parameters) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(query);
            for (int i = 0; i < parameters.length; i++) {
                nativeQuery.setParameter(i + 1, parameters[i]);
            }
            return nativeQuery.getResultList();
        } catch (Exception e) {
            logger.error("Native query execution failed: {}", query, e);
            return List.of();
        }
    }

    @Override
    public Optional<Object[]> executeNativeQuerySingle(String query, Object... parameters) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(query);
            for (int i = 0; i < parameters.length; i++) {
                nativeQuery.setParameter(i + 1, parameters[i]);
            }
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Native query execution failed: {}", query, e);
            return Optional.empty();
        }
    }

    @Override
    public RepositoryStatistics getStatistics() {
        try {
            // Get basic counts
            long totalCount = count();
            long activeCount = countActive();
            long inactiveCount = totalCount - activeCount;

            // Get date statistics
            Instant oldestCreated = getOldestCreatedDate();
            Instant newestCreated = getNewestCreatedDate();
            Instant lastUpdated = getLastUpdatedDate();

            // Calculate growth metrics
            double averageRecordsPerDay = calculateAverageRecordsPerDay(oldestCreated, totalCount);
            double growthRate = calculateGrowthRate();

            return new RepositoryStatistics(
                totalCount, activeCount, inactiveCount,
                oldestCreated, newestCreated, lastUpdated,
                averageRecordsPerDay, growthRate
            );
        } catch (Exception e) {
            logger.warn("Failed to calculate statistics for entity {}", entityName, e);
            return RepositoryStatistics.empty();
        }
    }

    @Override
    public void refresh(T entity) {
        if (entityManager.contains(entity)) {
            entityManager.refresh(entity);
        }
    }

    @Override
    public void detach(T entity) {
        if (entityManager.contains(entity)) {
            entityManager.detach(entity);
        }
    }

    @Override
    public boolean isManaged(T entity) {
        return entityManager.contains(entity);
    }

    // Helper methods

    private String getTableName() {
        try {
            jakarta.persistence.Table table = getDomainClass().getAnnotation(jakarta.persistence.Table.class);
            if (table != null && !table.name().isEmpty()) {
                return table.name();
            }
            return entityName.toLowerCase();
        } catch (Exception e) {
            return entityName.toLowerCase();
        }
    }

    private Instant getOldestCreatedDate() {
        try {
            String jpql = String.format("SELECT MIN(e.createdAt) FROM %s e", entityName);
            return entityManager.createQuery(jpql, Instant.class).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private Instant getNewestCreatedDate() {
        try {
            String jpql = String.format("SELECT MAX(e.createdAt) FROM %s e", entityName);
            return entityManager.createQuery(jpql, Instant.class).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private Instant getLastUpdatedDate() {
        try {
            String jpql = String.format("SELECT MAX(e.updatedAt) FROM %s e", entityName);
            return entityManager.createQuery(jpql, Instant.class).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateAverageRecordsPerDay(Instant oldestCreated, long totalCount) {
        if (oldestCreated == null || totalCount == 0) {
            return 0.0;
        }

        long daysSinceOldest = Duration.between(oldestCreated, Instant.now()).toDays();
        return daysSinceOldest > 0 ? (double) totalCount / daysSinceOldest : totalCount;
    }

    private double calculateGrowthRate() {
        try {
            // Get count from last 30 days vs previous 30 days
            Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
            Instant sixtyDaysAgo = Instant.now().minus(Duration.ofDays(60));

            String recentJpql = String.format("SELECT COUNT(e) FROM %s e WHERE e.createdAt > :recent", entityName);
            String previousJpql = String.format("SELECT COUNT(e) FROM %s e WHERE e.createdAt BETWEEN :previous AND :recent", entityName);

            long recentCount = entityManager.createQuery(recentJpql, Long.class)
                .setParameter("recent", thirtyDaysAgo)
                .getSingleResult();

            long previousCount = entityManager.createQuery(previousJpql, Long.class)
                .setParameter("previous", sixtyDaysAgo)
                .setParameter("recent", thirtyDaysAgo)
                .getSingleResult();

            if (previousCount == 0) {
                return recentCount > 0 ? 100.0 : 0.0;
            }

            return ((double) (recentCount - previousCount) / previousCount) * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}