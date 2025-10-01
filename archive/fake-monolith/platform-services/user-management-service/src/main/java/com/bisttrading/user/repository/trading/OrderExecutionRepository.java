package com.bisttrading.user.repository.trading;

import com.bisttrading.user.entity.trading.OrderExecution;
import com.bisttrading.user.entity.trading.enums.ExecutionType;
import com.bisttrading.user.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OrderExecution entity operations
 * Provides trade execution analysis and reporting queries
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface OrderExecutionRepository extends BaseRepository<OrderExecution, UUID> {

    // Basic execution lookups

    /**
     * Find all executions for an order
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.order.id = :orderId ORDER BY oe.executionTime ASC")
    List<OrderExecution> findByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find execution by trade ID
     */
    List<OrderExecution> findByTradeId(String tradeId);

    /**
     * Find execution by broker execution ID
     */
    List<OrderExecution> findByBrokerExecutionId(String brokerExecutionId);

    // User executions

    /**
     * Find all executions for a user
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.order.user.id = :userId ORDER BY oe.executionTime DESC")
    List<OrderExecution> findByUserId(@Param("userId") UUID userId);

    /**
     * Find executions for user within date range
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.order.user.id = :userId AND " +
           "oe.executionTime BETWEEN :startDate AND :endDate ORDER BY oe.executionTime DESC")
    List<OrderExecution> findByUserIdAndExecutionTimeBetween(@Param("userId") UUID userId,
                                                            @Param("startDate") ZonedDateTime startDate,
                                                            @Param("endDate") ZonedDateTime endDate);

    // Symbol executions

    /**
     * Find executions for a symbol
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.order.symbol.id = :symbolId ORDER BY oe.executionTime DESC")
    List<OrderExecution> findBySymbolId(@Param("symbolId") UUID symbolId);

    /**
     * Find recent executions for a symbol (for price discovery)
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.order.symbol.id = :symbolId AND " +
           "oe.executionTime >= :sinceTime ORDER BY oe.executionTime DESC")
    List<OrderExecution> findRecentExecutionsBySymbolId(@Param("symbolId") UUID symbolId,
                                                        @Param("sinceTime") ZonedDateTime sinceTime);

    // Execution statistics

    /**
     * Get execution summary for an order
     */
    @Query(value = """
        SELECT
            COUNT(*) as execution_count,
            SUM(execution_quantity) as total_quantity,
            AVG(execution_price) as avg_price,
            MIN(execution_price) as min_price,
            MAX(execution_price) as max_price,
            SUM(gross_amount) as total_gross_amount,
            SUM(commission + brokerage_fee + exchange_fee + tax_amount) as total_fees,
            SUM(net_amount) as total_net_amount,
            MIN(execution_time) as first_execution,
            MAX(execution_time) as last_execution
        FROM order_executions
        WHERE order_id = :orderId
        """, nativeQuery = true)
    Object[] getExecutionSummaryForOrder(@Param("orderId") UUID orderId);

    /**
     * Get daily execution statistics for user
     */
    @Query(value = """
        SELECT
            DATE(oe.execution_time) as execution_date,
            COUNT(*) as execution_count,
            SUM(oe.execution_quantity) as total_quantity,
            SUM(oe.gross_amount) as total_volume,
            SUM(oe.commission + oe.brokerage_fee + oe.exchange_fee + oe.tax_amount) as total_fees,
            AVG(oe.execution_price) as avg_price,
            COUNT(DISTINCT oe.order_id) as unique_orders
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.user_id = :userId AND oe.execution_time >= :startDate
        GROUP BY DATE(oe.execution_time)
        ORDER BY execution_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyExecutionStatistics(@Param("userId") UUID userId,
                                              @Param("startDate") ZonedDateTime startDate);

    // Price analysis

    /**
     * Get volume-weighted average price (VWAP) for symbol
     */
    @Query(value = """
        SELECT
            SUM(oe.execution_quantity * oe.execution_price) / SUM(oe.execution_quantity) as vwap,
            SUM(oe.execution_quantity) as total_volume,
            COUNT(*) as trade_count
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.symbol_id = :symbolId AND oe.execution_time >= :startTime
        """, nativeQuery = true)
    Object[] getVWAPForSymbol(@Param("symbolId") UUID symbolId, @Param("startTime") ZonedDateTime startTime);

    /**
     * Get execution price distribution for symbol
     */
    @Query(value = """
        SELECT
            ROUND(execution_price, 2) as price_level,
            SUM(execution_quantity) as volume,
            COUNT(*) as trade_count,
            AVG(execution_quantity) as avg_size
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.symbol_id = :symbolId AND oe.execution_time >= :startTime
        GROUP BY ROUND(execution_price, 2)
        ORDER BY price_level
        """, nativeQuery = true)
    List<Object[]> getPriceDistributionForSymbol(@Param("symbolId") UUID symbolId,
                                                @Param("startTime") ZonedDateTime startTime);

    // Market microstructure analysis

    /**
     * Get execution quality metrics for user
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_executions,
            AVG(CASE WHEN oe.bid_price IS NOT NULL AND oe.ask_price IS NOT NULL
                     THEN oe.ask_price - oe.bid_price ELSE NULL END) as avg_spread,
            AVG(CASE WHEN oe.market_price IS NOT NULL AND o.order_side = 'BUY'
                     THEN oe.market_price - oe.execution_price
                     WHEN oe.market_price IS NOT NULL AND o.order_side = 'SELL'
                     THEN oe.execution_price - oe.market_price
                     ELSE NULL END) as avg_price_improvement,
            COUNT(CASE WHEN oe.bid_price IS NOT NULL AND oe.ask_price IS NOT NULL AND
                           oe.execution_price >= oe.bid_price AND oe.execution_price <= oe.ask_price
                       THEN 1 END)::decimal / COUNT(*) as inside_spread_rate
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.user_id = :userId AND oe.execution_time >= :startDate
        """, nativeQuery = true)
    Object[] getExecutionQualityMetrics(@Param("userId") UUID userId,
                                       @Param("startDate") ZonedDateTime startDate);

    /**
     * Find executions with significant price improvement
     */
    @Query(value = """
        SELECT oe.*, o.symbol_id,
               CASE WHEN o.order_side = 'BUY'
                    THEN oe.market_price - oe.execution_price
                    ELSE oe.execution_price - oe.market_price END as price_improvement
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.user_id = :userId
        AND oe.market_price IS NOT NULL
        AND (
            (o.order_side = 'BUY' AND oe.market_price - oe.execution_price > :threshold) OR
            (o.order_side = 'SELL' AND oe.execution_price - oe.market_price > :threshold)
        )
        AND oe.execution_time >= :startDate
        ORDER BY price_improvement DESC
        """, nativeQuery = true)
    List<Object[]> findExecutionsWithPriceImprovement(@Param("userId") UUID userId,
                                                     @Param("threshold") BigDecimal threshold,
                                                     @Param("startDate") ZonedDateTime startDate);

    // Fee analysis

    /**
     * Get fee breakdown for user
     */
    @Query(value = """
        SELECT
            SUM(oe.commission) as total_commission,
            SUM(oe.brokerage_fee) as total_brokerage_fee,
            SUM(oe.exchange_fee) as total_exchange_fee,
            SUM(oe.tax_amount) as total_tax,
            SUM(oe.commission + oe.brokerage_fee + oe.exchange_fee + oe.tax_amount) as total_fees,
            SUM(oe.gross_amount) as total_volume,
            (SUM(oe.commission + oe.brokerage_fee + oe.exchange_fee + oe.tax_amount) /
             NULLIF(SUM(oe.gross_amount), 0)) * 100 as fee_rate_percentage
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.user_id = :userId AND oe.execution_time >= :startDate
        """, nativeQuery = true)
    Object[] getFeeBreakdownForUser(@Param("userId") UUID userId,
                                   @Param("startDate") ZonedDateTime startDate);

    // Performance analysis

    /**
     * Get execution latency statistics
     */
    @Query(value = """
        SELECT
            AVG(EXTRACT(EPOCH FROM (oe.execution_time - o.submitted_at))) as avg_execution_latency,
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (oe.execution_time - o.submitted_at))) as median_latency,
            PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (oe.execution_time - o.submitted_at))) as p95_latency,
            COUNT(*) as execution_count
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.user_id = :userId AND o.submitted_at IS NOT NULL AND oe.execution_time >= :startDate
        """, nativeQuery = true)
    Object[] getExecutionLatencyStatistics(@Param("userId") UUID userId,
                                          @Param("startDate") ZonedDateTime startDate);

    // Time series analysis

    /**
     * Get execution volume by hour of day
     */
    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM oe.execution_time) as hour_of_day,
            COUNT(*) as execution_count,
            SUM(oe.execution_quantity) as total_quantity,
            SUM(oe.gross_amount) as total_volume,
            AVG(oe.execution_price) as avg_price
        FROM order_executions oe
        JOIN orders o ON oe.order_id = o.order_id
        WHERE o.symbol_id = :symbolId AND oe.execution_time >= :startDate
        GROUP BY EXTRACT(HOUR FROM oe.execution_time)
        ORDER BY hour_of_day
        """, nativeQuery = true)
    List<Object[]> getExecutionVolumeByHour(@Param("symbolId") UUID symbolId,
                                           @Param("startDate") ZonedDateTime startDate);

    // Unusual activity detection

    /**
     * Find large executions (potential block trades)
     */
    @Query("SELECT oe FROM OrderExecution oe WHERE oe.executionQuantity > :sizeThreshold OR " +
           "oe.grossAmount > :valueThreshold ORDER BY oe.executionTime DESC")
    List<OrderExecution> findLargeExecutions(@Param("sizeThreshold") Integer sizeThreshold,
                                            @Param("valueThreshold") BigDecimal valueThreshold);

    /**
     * Find executions outside normal market hours
     */
    @Query(value = """
        SELECT oe.* FROM order_executions oe
        WHERE EXTRACT(HOUR FROM oe.execution_time) < 10
        OR EXTRACT(HOUR FROM oe.execution_time) >= 18
        OR EXTRACT(DOW FROM oe.execution_time) IN (0, 6)
        ORDER BY oe.execution_time DESC
        """, nativeQuery = true)
    List<OrderExecution> findAfterHoursExecutions();
}