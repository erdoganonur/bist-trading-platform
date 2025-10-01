package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.OrderExecution;
import com.bisttrading.entity.trading.enums.ExecutionType;
import com.bisttrading.repository.BaseRepository;
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
}