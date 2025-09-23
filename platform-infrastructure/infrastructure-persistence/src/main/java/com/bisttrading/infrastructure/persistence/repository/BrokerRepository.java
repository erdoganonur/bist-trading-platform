package com.bisttrading.infrastructure.persistence.repository;

import com.bisttrading.infrastructure.persistence.entity.BrokerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BrokerEntity operations.
 * Provides custom query methods for broker management.
 */
@Repository
public interface BrokerRepository extends JpaRepository<BrokerEntity, String> {

    /**
     * Finds broker by broker code.
     *
     * @param brokerCode Broker code
     * @return Optional BrokerEntity
     */
    Optional<BrokerEntity> findByBrokerCode(String brokerCode);

    /**
     * Finds broker by name.
     *
     * @param name Broker name
     * @return Optional BrokerEntity
     */
    Optional<BrokerEntity> findByName(String name);

    /**
     * Checks if broker exists by code.
     *
     * @param brokerCode Broker code
     * @return true if exists
     */
    boolean existsByBrokerCode(String brokerCode);

    /**
     * Checks if broker exists by name.
     *
     * @param name Broker name
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Finds all brokers by type.
     *
     * @param brokerType Broker type
     * @return List of brokers
     */
    List<BrokerEntity> findByBrokerType(BrokerEntity.BrokerType brokerType);

    /**
     * Finds all brokers by status.
     *
     * @param status Broker status
     * @return List of brokers
     */
    List<BrokerEntity> findByStatus(BrokerEntity.BrokerStatus status);

    /**
     * Finds all active brokers (not deleted).
     *
     * @return List of active brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.status = 'ACTIVE' AND b.deletedAt IS NULL")
    List<BrokerEntity> findAllActive();

    /**
     * Finds brokers with trading enabled.
     *
     * @return List of trading-enabled brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.tradingEnabled = true AND b.status = 'ACTIVE' AND b.deletedAt IS NULL")
    List<BrokerEntity> findTradingEnabledBrokers();

    /**
     * Finds brokers that support real-time data.
     *
     * @return List of brokers supporting real-time data
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.supportsRealtimeData = true AND b.status = 'ACTIVE'")
    List<BrokerEntity> findBrokersWithRealtimeData();

    /**
     * Finds brokers that support historical data.
     *
     * @return List of brokers supporting historical data
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.supportsHistoricalData = true AND b.status = 'ACTIVE'")
    List<BrokerEntity> findBrokersWithHistoricalData();

    /**
     * Finds brokers that support order management.
     *
     * @return List of brokers supporting order management
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.supportsOrderManagement = true AND b.status = 'ACTIVE'")
    List<BrokerEntity> findBrokersWithOrderManagement();

    /**
     * Finds brokers that support portfolio tracking.
     *
     * @return List of brokers supporting portfolio tracking
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.supportsPortfolioTracking = true AND b.status = 'ACTIVE'")
    List<BrokerEntity> findBrokersWithPortfolioTracking();

    /**
     * Finds brokers with API endpoint configured.
     *
     * @return List of brokers with API configuration
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.apiEndpoint IS NOT NULL AND b.apiEndpoint != '' AND b.apiKey IS NOT NULL")
    List<BrokerEntity> findBrokersWithApiConfiguration();

    /**
     * Finds brokers by commission rate range.
     *
     * @param minRate Minimum commission rate
     * @param maxRate Maximum commission rate
     * @return List of brokers within commission rate range
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.commissionRate BETWEEN :minRate AND :maxRate")
    List<BrokerEntity> findByCommissionRateBetween(@Param("minRate") BigDecimal minRate,
                                                  @Param("maxRate") BigDecimal maxRate);

    /**
     * Finds brokers with low commission rates.
     *
     * @param maxRate Maximum commission rate threshold
     * @return List of low-commission brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.commissionRate <= :maxRate AND b.status = 'ACTIVE' ORDER BY b.commissionRate ASC")
    List<BrokerEntity> findLowCommissionBrokers(@Param("maxRate") BigDecimal maxRate);

    /**
     * Finds brokers by connection success rate.
     *
     * @param minSuccessRate Minimum success rate threshold
     * @return List of reliable brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.connectionSuccessRate >= :minSuccessRate AND b.status = 'ACTIVE'")
    List<BrokerEntity> findReliableBrokers(@Param("minSuccessRate") BigDecimal minSuccessRate);

    /**
     * Finds brokers with recent connection activity.
     *
     * @param since Timestamp threshold for recent activity
     * @return List of recently active brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.lastConnectionAt > :since")
    List<BrokerEntity> findBrokersWithRecentActivity(@Param("since") LocalDateTime since);

    /**
     * Finds brokers with connection problems.
     *
     * @param since Timestamp to check for connection errors
     * @return List of brokers with recent connection issues
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.lastConnectionError IS NOT NULL AND " +
           "(b.lastConnectionAt IS NULL OR b.lastConnectionAt < :since)")
    List<BrokerEntity> findBrokersWithConnectionProblems(@Param("since") LocalDateTime since);

    /**
     * Finds brokers ordered by priority.
     *
     * @return List of brokers ordered by priority (ascending)
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.status = 'ACTIVE' AND b.deletedAt IS NULL " +
           "ORDER BY b.priorityOrder ASC NULLS LAST, b.name ASC")
    List<BrokerEntity> findBrokersOrderedByPriority();

    /**
     * Finds brokers by API version.
     *
     * @param apiVersion API version
     * @return List of brokers supporting specified API version
     */
    List<BrokerEntity> findByApiVersion(String apiVersion);

    /**
     * Finds brokers with SSL enabled.
     *
     * @return List of brokers with SSL/TLS enabled
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.sslEnabled = true")
    List<BrokerEntity> findBrokersWithSslEnabled();

    /**
     * Counts brokers by status.
     *
     * @param status Broker status
     * @return Number of brokers with specified status
     */
    long countByStatus(BrokerEntity.BrokerStatus status);

    /**
     * Counts brokers by type.
     *
     * @param brokerType Broker type
     * @return Number of brokers of specified type
     */
    long countByBrokerType(BrokerEntity.BrokerType brokerType);

    /**
     * Counts active brokers.
     *
     * @return Number of active brokers
     */
    @Query("SELECT COUNT(b) FROM BrokerEntity b WHERE b.status = 'ACTIVE' AND b.deletedAt IS NULL")
    long countActiveBrokers();

    /**
     * Counts trading-enabled brokers.
     *
     * @return Number of brokers with trading enabled
     */
    @Query("SELECT COUNT(b) FROM BrokerEntity b WHERE b.tradingEnabled = true AND b.status = 'ACTIVE'")
    long countTradingEnabledBrokers();

    /**
     * Updates broker last connection information.
     *
     * @param brokerId Broker ID
     * @param lastConnectionAt Last connection timestamp
     * @param lastConnectionError Connection error message (null if successful)
     * @return Number of updated records
     */
    @Query("UPDATE BrokerEntity b SET b.lastConnectionAt = :lastConnectionAt, b.lastConnectionError = :lastConnectionError " +
           "WHERE b.id = :brokerId")
    int updateLastConnection(@Param("brokerId") String brokerId,
                            @Param("lastConnectionAt") LocalDateTime lastConnectionAt,
                            @Param("lastConnectionError") String lastConnectionError);

    /**
     * Updates broker connection statistics.
     *
     * @param brokerId Broker ID
     * @param successRate Connection success rate
     * @param averageResponseTime Average response time
     * @return Number of updated records
     */
    @Query("UPDATE BrokerEntity b SET b.connectionSuccessRate = :successRate, b.averageResponseTime = :averageResponseTime " +
           "WHERE b.id = :brokerId")
    int updateConnectionStatistics(@Param("brokerId") String brokerId,
                                  @Param("successRate") BigDecimal successRate,
                                  @Param("averageResponseTime") Integer averageResponseTime);

    /**
     * Updates broker trading status.
     *
     * @param brokerId Broker ID
     * @param tradingEnabled Trading enabled status
     * @return Number of updated records
     */
    @Query("UPDATE BrokerEntity b SET b.tradingEnabled = :tradingEnabled WHERE b.id = :brokerId")
    int updateTradingStatus(@Param("brokerId") String brokerId,
                           @Param("tradingEnabled") boolean tradingEnabled);

    /**
     * Updates broker priority order.
     *
     * @param brokerId Broker ID
     * @param priorityOrder New priority order
     * @return Number of updated records
     */
    @Query("UPDATE BrokerEntity b SET b.priorityOrder = :priorityOrder WHERE b.id = :brokerId")
    int updatePriorityOrder(@Param("brokerId") String brokerId,
                           @Param("priorityOrder") Integer priorityOrder);

    /**
     * Soft deletes broker by setting deleted timestamp.
     *
     * @param brokerId Broker ID
     * @param deletedAt Deletion timestamp
     * @return Number of updated records
     */
    @Query("UPDATE BrokerEntity b SET b.deletedAt = :deletedAt, b.status = 'DEPRECATED' " +
           "WHERE b.id = :brokerId")
    int softDeleteBroker(@Param("brokerId") String brokerId,
                        @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Searches brokers by partial name or code match.
     *
     * @param searchTerm Search term
     * @return List of matching brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.brokerCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "b.deletedAt IS NULL")
    List<BrokerEntity> searchBrokers(@Param("searchTerm") String searchTerm);

    /**
     * Gets broker performance statistics.
     *
     * @return List of objects containing broker performance metrics
     */
    @Query("SELECT b.id, b.name, b.connectionSuccessRate, b.averageResponseTime, b.lastConnectionAt " +
           "FROM BrokerEntity b WHERE b.status = 'ACTIVE' ORDER BY b.connectionSuccessRate DESC")
    List<Object[]> getBrokerPerformanceStatistics();

    /**
     * Gets broker distribution by type and status.
     *
     * @return List of objects containing broker distribution
     */
    @Query("SELECT b.brokerType, b.status, COUNT(b) as count FROM BrokerEntity b " +
           "WHERE b.deletedAt IS NULL GROUP BY b.brokerType, b.status")
    List<Object[]> getBrokerDistribution();

    /**
     * Finds best performing brokers.
     *
     * @param limit Number of top brokers to return
     * @return List of best performing brokers
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.status = 'ACTIVE' AND b.connectionSuccessRate IS NOT NULL " +
           "ORDER BY b.connectionSuccessRate DESC, b.averageResponseTime ASC")
    List<BrokerEntity> findBestPerformingBrokers(@Param("limit") int limit);

    /**
     * Finds brokers requiring maintenance.
     *
     * @param maxSuccessRate Maximum acceptable success rate
     * @param maxResponseTime Maximum acceptable response time
     * @return List of brokers that may need maintenance
     */
    @Query("SELECT b FROM BrokerEntity b WHERE b.status = 'ACTIVE' AND " +
           "(b.connectionSuccessRate < :maxSuccessRate OR b.averageResponseTime > :maxResponseTime)")
    List<BrokerEntity> findBrokersRequiringMaintenance(@Param("maxSuccessRate") BigDecimal maxSuccessRate,
                                                       @Param("maxResponseTime") Integer maxResponseTime);
}