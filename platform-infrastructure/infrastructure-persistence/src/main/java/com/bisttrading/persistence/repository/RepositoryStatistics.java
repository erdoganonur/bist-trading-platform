package com.bisttrading.persistence.repository;

import java.time.Instant;

/**
 * Repository statistics data transfer object
 * Contains aggregate information about repository data
 */
public record RepositoryStatistics(
    long totalCount,
    long activeCount,
    long inactiveCount,
    Instant oldestCreated,
    Instant newestCreated,
    Instant lastUpdated,
    double averageRecordsPerDay,
    double growthRate
) {

    /**
     * Calculate activity rate (active records / total records)
     *
     * @return Activity rate as percentage
     */
    public double getActivityRate() {
        return totalCount > 0 ? (activeCount * 100.0) / totalCount : 0.0;
    }

    /**
     * Calculate inactive rate (inactive records / total records)
     *
     * @return Inactive rate as percentage
     */
    public double getInactiveRate() {
        return totalCount > 0 ? (inactiveCount * 100.0) / totalCount : 0.0;
    }

    /**
     * Check if repository has data
     *
     * @return True if repository contains records
     */
    public boolean hasData() {
        return totalCount > 0;
    }

    /**
     * Check if repository is healthy (has active records)
     *
     * @return True if repository has active records
     */
    public boolean isHealthy() {
        return activeCount > 0;
    }

    /**
     * Get data age in days
     *
     * @return Number of days since oldest record was created
     */
    public long getDataAgeInDays() {
        if (oldestCreated == null) {
            return 0;
        }
        return java.time.Duration.between(oldestCreated, Instant.now()).toDays();
    }

    /**
     * Get freshness in hours
     *
     * @return Number of hours since last update
     */
    public long getFreshnessInHours() {
        if (lastUpdated == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(lastUpdated, Instant.now()).toHours();
    }

    /**
     * Create empty statistics
     *
     * @return Empty statistics object
     */
    public static RepositoryStatistics empty() {
        return new RepositoryStatistics(0, 0, 0, null, null, null, 0.0, 0.0);
    }

    /**
     * Create statistics with basic counts
     *
     * @param total Total records
     * @param active Active records
     * @param inactive Inactive records
     * @return Basic statistics object
     */
    public static RepositoryStatistics withCounts(long total, long active, long inactive) {
        return new RepositoryStatistics(total, active, inactive, null, null, null, 0.0, 0.0);
    }

    @Override
    public String toString() {
        return String.format(
            "RepositoryStatistics{total=%d, active=%d, inactive=%d, activityRate=%.2f%%, " +
            "dataAge=%d days, freshness=%d hours, growthRate=%.2f%%}",
            totalCount, activeCount, inactiveCount, getActivityRate(),
            getDataAgeInDays(), getFreshnessInHours(), growthRate
        );
    }
}