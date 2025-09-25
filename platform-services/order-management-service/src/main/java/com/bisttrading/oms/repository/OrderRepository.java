package com.bisttrading.oms.repository;

import com.bisttrading.oms.model.OMSOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order Management System orders
 */
@Repository
public interface OrderRepository extends JpaRepository<OMSOrder, String> {

    /**
     * Find orders by user ID
     */
    Page<OMSOrder> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find active orders for user
     */
    @Query("SELECT o FROM OMSOrder o WHERE o.userId = :userId AND o.status IN ('NEW', 'PARTIALLY_FILLED') ORDER BY o.createdAt DESC")
    List<OMSOrder> findActiveOrdersByUserId(@Param("userId") String userId);

    /**
     * Find orders by symbol
     */
    Page<OMSOrder> findBySymbolOrderByCreatedAtDesc(String symbol, Pageable pageable);

    /**
     * Find orders by user and status
     */
    @Query("SELECT o FROM OMSOrder o WHERE o.userId = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<OMSOrder> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") String status, Pageable pageable);

    /**
     * Find orders by date range
     */
    @Query("SELECT o FROM OMSOrder o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<OMSOrder> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);

    /**
     * Count active orders for user
     */
    @Query("SELECT COUNT(o) FROM OMSOrder o WHERE o.userId = :userId AND o.status IN ('NEW', 'PARTIALLY_FILLED')")
    long countActiveOrdersByUserId(@Param("userId") String userId);

    /**
     * Find order by external order ID
     */
    Optional<OMSOrder> findByExternalOrderId(String externalOrderId);

    /**
     * Find orders by client order ID
     */
    List<OMSOrder> findByClientOrderId(String clientOrderId);
}