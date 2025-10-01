package com.bisttrading.caching.strategy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache invalidation statistics for monitoring and performance analysis
 * Thread-safe implementation for concurrent access
 */
public class CacheInvalidationStats {

    private final AtomicLong totalInvalidations = new AtomicLong(0);
    private final AtomicLong patternInvalidations = new AtomicLong(0);
    private final AtomicLong tagInvalidations = new AtomicLong(0);
    private final AtomicLong batchInvalidations = new AtomicLong(0);
    private final AtomicLong scheduledInvalidations = new AtomicLong(0);
    private final AtomicLong cacheWarmUps = new AtomicLong(0);
    private final AtomicLong failedInvalidations = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private volatile Instant lastInvalidation;
    private volatile Instant statsStartTime;

    public CacheInvalidationStats() {
        this.statsStartTime = Instant.now();
        this.lastInvalidation = null;
    }

    // Increment methods
    public void incrementTotalInvalidations() {
        totalInvalidations.incrementAndGet();
        updateLastInvalidation();
    }

    public void incrementPatternInvalidations() {
        patternInvalidations.incrementAndGet();
        incrementTotalInvalidations();
    }

    public void incrementTagInvalidations() {
        tagInvalidations.incrementAndGet();
        incrementTotalInvalidations();
    }

    public void incrementBatchInvalidations(int batchSize) {
        batchInvalidations.incrementAndGet();
        totalInvalidations.addAndGet(batchSize);
        updateLastInvalidation();
    }

    public void incrementScheduledInvalidations() {
        scheduledInvalidations.incrementAndGet();
        incrementTotalInvalidations();
    }

    public void incrementCacheWarmUps() {
        cacheWarmUps.incrementAndGet();
    }

    public void incrementFailedInvalidations() {
        failedInvalidations.incrementAndGet();
    }

    public void addProcessingTime(long processingTimeMs) {
        totalProcessingTimeMs.addAndGet(processingTimeMs);
    }

    // Getter methods
    public long getTotalInvalidations() {
        return totalInvalidations.get();
    }

    public long getPatternInvalidations() {
        return patternInvalidations.get();
    }

    public long getTagInvalidations() {
        return tagInvalidations.get();
    }

    public long getBatchInvalidations() {
        return batchInvalidations.get();
    }

    public long getScheduledInvalidations() {
        return scheduledInvalidations.get();
    }

    public long getCacheWarmUps() {
        return cacheWarmUps.get();
    }

    public long getFailedInvalidations() {
        return failedInvalidations.get();
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs.get();
    }

    public Instant getLastInvalidation() {
        return lastInvalidation;
    }

    public Instant getStatsStartTime() {
        return statsStartTime;
    }

    // Calculated metrics
    public double getAverageProcessingTimeMs() {
        long total = getTotalInvalidations();
        return total > 0 ? (double) getTotalProcessingTimeMs() / total : 0.0;
    }

    public double getFailureRate() {
        long total = getTotalInvalidations();
        return total > 0 ? (double) getFailedInvalidations() / total : 0.0;
    }

    public double getInvalidationsPerSecond() {
        long durationSeconds = java.time.Duration.between(statsStartTime, Instant.now()).toSeconds();
        return durationSeconds > 0 ? (double) getTotalInvalidations() / durationSeconds : 0.0;
    }

    // Reset methods
    public void reset() {
        totalInvalidations.set(0);
        patternInvalidations.set(0);
        tagInvalidations.set(0);
        batchInvalidations.set(0);
        scheduledInvalidations.set(0);
        cacheWarmUps.set(0);
        failedInvalidations.set(0);
        totalProcessingTimeMs.set(0);
        statsStartTime = Instant.now();
        lastInvalidation = null;
    }

    private void updateLastInvalidation() {
        lastInvalidation = Instant.now();
    }

    @Override
    public String toString() {
        return String.format(
            "CacheInvalidationStats{" +
            "totalInvalidations=%d, " +
            "patternInvalidations=%d, " +
            "tagInvalidations=%d, " +
            "batchInvalidations=%d, " +
            "scheduledInvalidations=%d, " +
            "cacheWarmUps=%d, " +
            "failedInvalidations=%d, " +
            "averageProcessingTime=%.2fms, " +
            "failureRate=%.2f%%, " +
            "invalidationsPerSecond=%.2f, " +
            "lastInvalidation=%s" +
            "}",
            getTotalInvalidations(),
            getPatternInvalidations(),
            getTagInvalidations(),
            getBatchInvalidations(),
            getScheduledInvalidations(),
            getCacheWarmUps(),
            getFailedInvalidations(),
            getAverageProcessingTimeMs(),
            getFailureRate() * 100,
            getInvalidationsPerSecond(),
            lastInvalidation
        );
    }
}