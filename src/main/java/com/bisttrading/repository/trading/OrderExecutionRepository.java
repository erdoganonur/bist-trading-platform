package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.OrderExecution;
import com.bisttrading.entity.trading.enums.ExecutionType;
import com.bisttrading.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderExecution entity operations
 * Simplified version with only basic JpaRepository operations
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface OrderExecutionRepository extends BaseRepository<OrderExecution, String> {

    // Basic execution lookups using Spring Data JPA method naming conventions

    /**
     * Find all executions for an order by order ID
     */
    List<OrderExecution> findByOrderId(String orderId);

    /**
     * Find execution by trade ID
     */
    List<OrderExecution> findByTradeId(String tradeId);

    /**
     * Find execution by broker execution ID
     */
    List<OrderExecution> findByBrokerExecutionId(String brokerExecutionId);

    /**
     * Find executions by execution type
     */
    List<OrderExecution> findByExecutionType(ExecutionType executionType);

    /**
     * Find all BUY executions for a user and symbol.
     * Used for cost basis calculations.
     *
     * @param userId User ID
     * @param symbol Symbol code
     * @return List of BUY executions ordered by execution time
     */
    @Query("""
        SELECT e FROM OrderExecution e
        JOIN e.order o
        JOIN o.user u
        JOIN o.symbol s
        WHERE u.id = :userId
        AND s.symbol = :symbol
        AND o.orderSide = 'BUY'
        AND e.executionType = 'FILL'
        ORDER BY e.executionTime ASC
        """)
    List<OrderExecution> findBuyExecutionsForUserAndSymbol(
            @Param("userId") String userId,
            @Param("symbol") String symbol
    );
}