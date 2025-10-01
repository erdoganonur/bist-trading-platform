package com.bisttrading.user.repository.trading;

import com.bisttrading.user.entity.trading.Order;
import com.bisttrading.user.entity.trading.enums.OrderSide;
import com.bisttrading.user.entity.trading.enums.OrderStatus;
import com.bisttrading.user.entity.trading.enums.OrderType;
import com.bisttrading.user.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Repository interface for Order entity operations
 * Provides comprehensive order management and trading queries
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface OrderRepository extends BaseRepository<Order, UUID> {

    // Basic order lookups

    /**
     * Find order by client order ID
     */
    Optional<Order> findByClientOrderId(String clientOrderId);

    /**
     * Find order by broker order ID
     */
    Optional<Order> findByBrokerOrderId(String brokerOrderId);

    /**
     * Find order by exchange order ID
     */
    Optional<Order> findByExchangeOrderId(String exchangeOrderId);

    // User orders

    /**
     * Find all orders for a user
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find orders for user by status
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderStatus = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndOrderStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);

    /**
     * Find orders for user by multiple statuses
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderStatus IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndOrderStatusIn(@Param("userId") UUID userId, @Param("statuses") List<OrderStatus> statuses);

    /**
     * Stream active orders for user (for real-time updates)
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.createdAt")
    Stream<Order> streamActiveOrdersByUserId(@Param("userId") UUID userId);

    // Account orders

    /**
     * Find orders by account ID
     */
    @Query("SELECT o FROM Order o WHERE o.accountId = :accountId ORDER BY o.createdAt DESC")
    Page<Order> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    /**
     * Find active orders by account ID
     */
    @Query("SELECT o FROM Order o WHERE o.accountId = :accountId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.createdAt")
    List<Order> findActiveOrdersByAccountId(@Param("accountId") String accountId);

    // Symbol orders

    /**
     * Find orders by symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId ORDER BY o.createdAt DESC")
    Page<Order> findBySymbolId(@Param("symbolId") UUID symbolId, Pageable pageable);

    /**
     * Find active orders by symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.createdAt")
    List<Order> findActiveOrdersBySymbolId(@Param("symbolId") UUID symbolId);

    // Order book queries

    /**
     * Get order book depth for a symbol (buy orders)
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.orderSide = 'BUY' AND " +
           "o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') AND o.price IS NOT NULL " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> getBuyOrderBookDepth(@Param("symbolId") UUID symbolId, Pageable pageable);

    /**
     * Get order book depth for a symbol (sell orders)
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.orderSide = 'SELL' AND " +
           "o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') AND o.price IS NOT NULL " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> getSellOrderBookDepth(@Param("symbolId") UUID symbolId, Pageable pageable);

    /**
     * Get aggregated order book data
     */
    @Query(value = """
        SELECT o.price, o.order_side as side,
               SUM(o.remaining_quantity) as total_quantity,
               COUNT(*) as order_count
        FROM orders o
        JOIN symbols s ON o.symbol_id = s.symbol_id
        WHERE s.symbol = :symbol
        AND o.order_status IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED')
        AND o.price IS NOT NULL
        GROUP BY o.price, o.order_side
        ORDER BY CASE WHEN o.order_side = 'BUY' THEN o.price END DESC,
                 CASE WHEN o.order_side = 'SELL' THEN o.price END ASC
        """, nativeQuery = true)
    List<Object[]> getOrderBookAggregated(@Param("symbol") String symbol);

    // Time-based queries

    /**
     * Find orders created within date range
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByCreatedAtBetween(@Param("startDate") ZonedDateTime startDate,
                                      @Param("endDate") ZonedDateTime endDate,
                                      Pageable pageable);

    /**
     * Find orders for user within date range
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                               @Param("startDate") ZonedDateTime startDate,
                                               @Param("endDate") ZonedDateTime endDate);

    /**
     * Find orders expiring soon
     */
    @Query("SELECT o FROM Order o WHERE o.expiresAt IS NOT NULL AND o.expiresAt <= :expiryTime AND " +
           "o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.expiresAt")
    List<Order> findExpiringOrders(@Param("expiryTime") ZonedDateTime expiryTime);

    // Parent-child order queries

    /**
     * Find child orders of a parent order
     */
    @Query("SELECT o FROM Order o WHERE o.parentOrderId = :parentOrderId ORDER BY o.createdAt")
    List<Order> findChildOrders(@Param("parentOrderId") String parentOrderId);

    /**
     * Find all orders in a bracket/OCO group
     */
    @Query("SELECT o FROM Order o WHERE o.parentOrderId = :parentOrderId OR o.id = :parentOrderId ORDER BY o.createdAt")
    List<Order> findOrderGroup(@Param("parentOrderId") UUID parentOrderId);

    // Strategy orders

    /**
     * Find orders by strategy ID
     */
    List<Order> findByStrategyIdOrderByCreatedAtDesc(String strategyId);

    /**
     * Find orders by algorithm ID
     */
    List<Order> findByAlgoIdOrderByCreatedAtDesc(String algoId);

    // Risk and compliance queries

    /**
     * Find orders that failed risk checks
     */
    @Query("SELECT o FROM Order o WHERE o.riskCheckPassed = false ORDER BY o.createdAt DESC")
    List<Order> findFailedRiskCheckOrders();

    /**
     * Find orders by user with total value greater than threshold
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND " +
           "(o.quantity * COALESCE(o.price, o.stopPrice, o.triggerPrice)) > :threshold " +
           "ORDER BY o.createdAt DESC")
    List<Order> findLargeOrdersByUserId(@Param("userId") UUID userId, @Param("threshold") BigDecimal threshold);

    // Statistics queries

    /**
     * Count orders by status for a user
     */
    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.user.id = :userId GROUP BY o.orderStatus")
    List<Object[]> countOrdersByStatusForUser(@Param("userId") UUID userId);

    /**
     * Count orders by type for a user
     */
    @Query("SELECT o.orderType, COUNT(o) FROM Order o WHERE o.user.id = :userId GROUP BY o.orderType")
    List<Object[]> countOrdersByTypeForUser(@Param("userId") UUID userId);

    /**
     * Get daily order statistics
     */
    @Query(value = """
        SELECT DATE(o.created_at) as order_date,
               COUNT(*) as total_orders,
               COUNT(CASE WHEN o.order_status = 'FILLED' THEN 1 END) as filled_orders,
               COUNT(CASE WHEN o.order_status = 'CANCELLED' THEN 1 END) as cancelled_orders,
               AVG(o.quantity * COALESCE(o.price, o.stop_price, o.trigger_price)) as avg_order_value,
               SUM(CASE WHEN o.order_status = 'FILLED' THEN o.quantity * o.average_fill_price ELSE 0 END) as total_volume
        FROM orders o
        WHERE o.created_at >= :startDate AND o.created_at <= :endDate
        GROUP BY DATE(o.created_at)
        ORDER BY order_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyOrderStatistics(@Param("startDate") ZonedDateTime startDate,
                                          @Param("endDate") ZonedDateTime endDate);

    // Bulk operations

    /**
     * Cancel multiple orders by IDs
     */
    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = 'CANCELLED', o.statusReason = :reason, " +
           "o.completedAt = CURRENT_TIMESTAMP, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id IN :orderIds")
    int cancelOrdersByIds(@Param("orderIds") List<UUID> orderIds, @Param("reason") String reason);

    /**
     * Cancel all active orders for a user
     */
    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = 'CANCELLED', o.statusReason = 'Bulk cancellation', " +
           "o.completedAt = CURRENT_TIMESTAMP, o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.user.id = :userId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED')")
    int cancelAllActiveOrdersForUser(@Param("userId") UUID userId);

    /**
     * Cancel all active orders for a symbol
     */
    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = 'CANCELLED', o.statusReason = 'Market halt', " +
           "o.completedAt = CURRENT_TIMESTAMP, o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.symbol.id = :symbolId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED')")
    int cancelAllActiveOrdersForSymbol(@Param("symbolId") UUID symbolId);

    // Advanced queries for trading algorithms

    /**
     * Find similar orders (same user, symbol, side, similar price)
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.symbol.id = :symbolId AND " +
           "o.orderSide = :side AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') AND " +
           "ABS(o.price - :price) <= :priceThreshold ORDER BY o.createdAt DESC")
    List<Order> findSimilarActiveOrders(@Param("userId") UUID userId, @Param("symbolId") UUID symbolId,
                                       @Param("side") OrderSide side, @Param("price") BigDecimal price,
                                       @Param("priceThreshold") BigDecimal priceThreshold);

    /**
     * Get order fill rate for a user
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_orders,
            COUNT(CASE WHEN order_status = 'FILLED' THEN 1 END) as filled_orders,
            COUNT(CASE WHEN order_status = 'PARTIALLY_FILLED' THEN 1 END) as partially_filled,
            AVG(CASE WHEN filled_quantity > 0 THEN filled_quantity::decimal / quantity ELSE 0 END) as avg_fill_rate
        FROM orders
        WHERE user_id = :userId AND created_at >= :startDate
        """, nativeQuery = true)
    Object[] getOrderFillRateForUser(@Param("userId") UUID userId, @Param("startDate") ZonedDateTime startDate);

    /**
     * Get order execution latency statistics
     */
    @Query(value = """
        SELECT
            AVG(EXTRACT(EPOCH FROM (accepted_at - submitted_at))) as avg_acceptance_latency,
            AVG(EXTRACT(EPOCH FROM (first_fill_at - accepted_at))) as avg_fill_latency,
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (first_fill_at - submitted_at))) as median_total_latency
        FROM orders
        WHERE user_id = :userId AND submitted_at IS NOT NULL AND first_fill_at IS NOT NULL
        AND created_at >= :startDate
        """, nativeQuery = true)
    Object[] getOrderLatencyStatistics(@Param("userId") UUID userId, @Param("startDate") ZonedDateTime startDate);

    // Real-time monitoring queries

    /**
     * Find orders with unusual patterns (for fraud detection)
     */
    @Query(value = """
        WITH user_avg AS (
            SELECT user_id, AVG(quantity * COALESCE(price, stop_price, trigger_price)) as avg_order_value
            FROM orders
            WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY user_id
        )
        SELECT o.* FROM orders o
        JOIN user_avg ua ON o.user_id = ua.user_id
        WHERE o.created_at >= CURRENT_DATE - INTERVAL '1 day'
        AND (o.quantity * COALESCE(o.price, o.stop_price, o.trigger_price)) > ua.avg_order_value * 5
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<Order> findUnusualOrders();

    /**
     * Find orders pending for too long
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'SUBMITTED' AND " +
           "o.submittedAt < :timeThreshold ORDER BY o.submittedAt")
    List<Order> findStaleSubmittedOrders(@Param("timeThreshold") ZonedDateTime timeThreshold);

    /**
     * Get real-time order metrics
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_active_orders,
            COUNT(CASE WHEN order_type = 'MARKET' THEN 1 END) as market_orders,
            COUNT(CASE WHEN order_type = 'LIMIT' THEN 1 END) as limit_orders,
            COUNT(CASE WHEN order_side = 'BUY' THEN 1 END) as buy_orders,
            COUNT(CASE WHEN order_side = 'SELL' THEN 1 END) as sell_orders,
            SUM(quantity * COALESCE(price, stop_price, trigger_price)) as total_order_value
        FROM orders
        WHERE order_status IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED')
        """, nativeQuery = true)
    Object[] getRealTimeOrderMetrics();
}