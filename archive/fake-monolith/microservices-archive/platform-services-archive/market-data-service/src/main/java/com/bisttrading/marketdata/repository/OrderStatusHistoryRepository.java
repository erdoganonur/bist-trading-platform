package com.bisttrading.marketdata.repository;

import com.bisttrading.marketdata.entity.OrderStatusHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderStatusHistory entity için TimescaleDB repository
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Order ID'ye göre status geçmişini getirir
     */
    @Query("SELECT o FROM OrderStatusHistory o WHERE o.orderId = :orderId " +
           "ORDER BY o.timestamp DESC")
    List<OrderStatusHistory> findByOrderIdOrderByTimestampDesc(@Param("orderId") String orderId);

    /**
     * Order ID için son status'u getirir
     */
    @Query("SELECT o FROM OrderStatusHistory o WHERE o.orderId = :orderId " +
           "ORDER BY o.timestamp DESC LIMIT 1")
    OrderStatusHistory findLatestByOrderId(@Param("orderId") String orderId);

    /**
     * Sembol ve zaman aralığına göre order status'larını getirir
     */
    @Query("SELECT o FROM OrderStatusHistory o WHERE o.symbol = :symbol " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY o.timestamp DESC")
    List<OrderStatusHistory> findBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * Status'a göre order'ları getirir
     */
    @Query("SELECT o FROM OrderStatusHistory o WHERE o.status = :status " +
           "AND o.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY o.timestamp DESC")
    List<OrderStatusHistory> findByStatusAndTimestampBetween(
        @Param("status") Integer status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * Order ID listesi için son status'ları getirir
     */
    @Query("SELECT o FROM OrderStatusHistory o WHERE o.id IN (" +
           "SELECT MAX(o2.id) FROM OrderStatusHistory o2 " +
           "WHERE o2.orderId IN :orderIds GROUP BY o2.orderId" +
           ") ORDER BY o.timestamp DESC")
    List<OrderStatusHistory> findLatestStatusForOrders(@Param("orderIds") List<String> orderIds);

    /**
     * Batch insert için native query
     */
    @Query(value = "INSERT INTO order_status_history (order_id, symbol, status, status_text, quantity, filled_quantity, price, timestamp, created_at) " +
                   "VALUES (:orderId, :symbol, :status, :statusText, :quantity, :filledQuantity, :price, :timestamp, :createdAt)",
           nativeQuery = true)
    void insertOrderStatus(@Param("orderId") String orderId,
                          @Param("symbol") String symbol,
                          @Param("status") Integer status,
                          @Param("statusText") String statusText,
                          @Param("quantity") Long quantity,
                          @Param("filledQuantity") Long filledQuantity,
                          @Param("price") java.math.BigDecimal price,
                          @Param("timestamp") LocalDateTime timestamp,
                          @Param("createdAt") LocalDateTime createdAt);

    /**
     * Eski order status verilerini temizleme
     */
    @Query("DELETE FROM OrderStatusHistory o WHERE o.timestamp < :cutoffTime")
    int deleteOldOrderStatuses(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Order status istatistikleri
     */
    @Query("SELECT o.status, COUNT(o) FROM OrderStatusHistory o " +
           "WHERE o.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY o.status")
    List<Object[]> getStatusStatistics(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * Sembol bazında order count
     */
    @Query("SELECT COUNT(DISTINCT o.orderId) FROM OrderStatusHistory o " +
           "WHERE o.symbol = :symbol " +
           "AND o.timestamp BETWEEN :startTime AND :endTime")
    long countUniqueOrdersBySymbol(@Param("symbol") String symbol,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
}