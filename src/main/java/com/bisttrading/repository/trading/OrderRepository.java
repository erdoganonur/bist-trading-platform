package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.Order;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity operations
 * Simplified for successful REAL monolith startup
 *
 * @author BIST Trading Platform
 * @version 2.0
 * @since Sprint 5
 */
@Repository
public interface OrderRepository extends BaseRepository<Order, String> {

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
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.id DESC")
    Page<Order> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find orders for user by status
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderStatus = :status ORDER BY o.id DESC")
    List<Order> findByUserIdAndOrderStatus(@Param("userId") String userId, @Param("status") OrderStatus status);

    /**
     * Find orders for user by multiple statuses
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderStatus IN :statuses ORDER BY o.id DESC")
    List<Order> findByUserIdAndOrderStatusIn(@Param("userId") String userId, @Param("statuses") List<OrderStatus> statuses);

    // Account orders

    /**
     * Find orders by account ID
     */
    @Query("SELECT o FROM Order o WHERE o.accountId = :accountId ORDER BY o.id DESC")
    Page<Order> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    /**
     * Find active orders by account ID
     */
    @Query("SELECT o FROM Order o WHERE o.accountId = :accountId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.id")
    List<Order> findActiveOrdersByAccountId(@Param("accountId") String accountId);

    // Symbol orders

    /**
     * Find orders by symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId ORDER BY o.id DESC")
    Page<Order> findBySymbolId(@Param("symbolId") String symbolId, Pageable pageable);

    /**
     * Find active orders by symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.orderStatus IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') ORDER BY o.id")
    List<Order> findActiveOrdersBySymbolId(@Param("symbolId") String symbolId);

    // Basic queries only

    /**
     * Find orders by strategy ID
     */
    List<Order> findByStrategyIdOrderById(String strategyId);

    /**
     * Find orders by algorithm ID
     */
    List<Order> findByAlgoIdOrderById(String algoId);

    /**
     * Count total orders for a user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countOrdersForUser(@Param("userId") String userId);

    /**
     * Find orders by multiple statuses
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByOrderStatusIn(@Param("statuses") List<OrderStatus> statuses);
}