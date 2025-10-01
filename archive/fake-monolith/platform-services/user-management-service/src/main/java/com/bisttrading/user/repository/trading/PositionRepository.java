package com.bisttrading.user.repository.trading;

import com.bisttrading.user.entity.trading.Position;
import com.bisttrading.user.entity.trading.enums.PositionSide;
import com.bisttrading.user.repository.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Position entity operations
 * Provides portfolio management and P&L queries
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface PositionRepository extends BaseRepository<Position, UUID> {

    // Basic position lookups

    /**
     * Find position for user, account, and symbol
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.accountId = :accountId AND p.symbol.id = :symbolId")
    Optional<Position> findByUserIdAndAccountIdAndSymbolId(@Param("userId") UUID userId,
                                                          @Param("accountId") String accountId,
                                                          @Param("symbolId") UUID symbolId);

    /**
     * Find all positions for user
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId ORDER BY p.updatedAt DESC")
    List<Position> findByUserId(@Param("userId") UUID userId);

    /**
     * Find all positions for account
     */
    @Query("SELECT p FROM Position p WHERE p.accountId = :accountId ORDER BY p.updatedAt DESC")
    List<Position> findByAccountId(@Param("accountId") String accountId);

    /**
     * Find all positions for symbol
     */
    @Query("SELECT p FROM Position p WHERE p.symbol.id = :symbolId ORDER BY p.updatedAt DESC")
    List<Position> findBySymbolId(@Param("symbolId") UUID symbolId);

    // Active positions (non-zero quantity)

    /**
     * Find active positions for user
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.quantity != 0 ORDER BY p.marketValue DESC NULLS LAST")
    List<Position> findActivePositionsByUserId(@Param("userId") UUID userId);

    /**
     * Find active positions for account
     */
    @Query("SELECT p FROM Position p WHERE p.accountId = :accountId AND p.quantity != 0 ORDER BY p.marketValue DESC NULLS LAST")
    List<Position> findActivePositionsByAccountId(@Param("accountId") String accountId);

    /**
     * Find long positions for user
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.positionSide = 'LONG' AND p.quantity > 0 ORDER BY p.marketValue DESC")
    List<Position> findLongPositionsByUserId(@Param("userId") UUID userId);

    /**
     * Find short positions for user
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.positionSide = 'SHORT' AND p.quantity < 0 ORDER BY p.marketValue DESC")
    List<Position> findShortPositionsByUserId(@Param("userId") UUID userId);

    // P&L queries

    /**
     * Find positions with profit
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.unrealizedPnl > 0 ORDER BY p.unrealizedPnl DESC")
    List<Position> findProfitablePositions(@Param("userId") UUID userId);

    /**
     * Find positions with loss
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.unrealizedPnl < 0 ORDER BY p.unrealizedPnl ASC")
    List<Position> findLosingPositions(@Param("userId") UUID userId);

    /**
     * Find positions with P&L greater than threshold
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND ABS(p.unrealizedPnl) > :threshold ORDER BY ABS(p.unrealizedPnl) DESC")
    List<Position> findSignificantPnlPositions(@Param("userId") UUID userId, @Param("threshold") BigDecimal threshold);

    // Portfolio aggregations

    /**
     * Get portfolio summary for user
     */
    @Query(value = """
        SELECT
            COUNT(CASE WHEN quantity != 0 THEN 1 END) as active_positions,
            COUNT(CASE WHEN position_side = 'LONG' THEN 1 END) as long_positions,
            COUNT(CASE WHEN position_side = 'SHORT' THEN 1 END) as short_positions,
            COALESCE(SUM(market_value), 0) as total_market_value,
            COALESCE(SUM(unrealized_pnl), 0) as total_unrealized_pnl,
            COALESCE(SUM(realized_pnl), 0) as total_realized_pnl,
            COALESCE(SUM(day_change), 0) as total_day_change,
            COALESCE(AVG(day_change_percent), 0) as avg_day_change_percent
        FROM positions
        WHERE user_id = :userId
        """, nativeQuery = true)
    Object[] getPortfolioSummary(@Param("userId") UUID userId);

    /**
     * Get sector exposure for user
     */
    @Query(value = """
        SELECT s.sector,
               SUM(p.market_value) as sector_value,
               COUNT(p.position_id) as position_count,
               SUM(p.unrealized_pnl) as sector_pnl
        FROM positions p
        JOIN symbols s ON p.symbol_id = s.symbol_id
        WHERE p.user_id = :userId AND p.quantity != 0 AND s.sector IS NOT NULL
        GROUP BY s.sector
        ORDER BY sector_value DESC
        """, nativeQuery = true)
    List<Object[]> getSectorExposure(@Param("userId") UUID userId);

    /**
     * Get top positions by market value
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.quantity != 0 ORDER BY ABS(p.marketValue) DESC")
    List<Position> getTopPositionsByValue(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    // Risk queries

    /**
     * Find positions with high concentration (> percentage of portfolio)
     */
    @Query(value = """
        WITH portfolio_total AS (
            SELECT SUM(ABS(market_value)) as total_value
            FROM positions
            WHERE user_id = :userId AND quantity != 0
        )
        SELECT p.* FROM positions p, portfolio_total pt
        WHERE p.user_id = :userId AND p.quantity != 0
        AND (ABS(p.market_value) / NULLIF(pt.total_value, 0)) > :concentrationThreshold / 100.0
        ORDER BY ABS(p.market_value) DESC
        """, nativeQuery = true)
    List<Position> findHighConcentrationPositions(@Param("userId") UUID userId,
                                                 @Param("concentrationThreshold") BigDecimal concentrationThreshold);

    /**
     * Find positions with margin exposure
     */
    @Query("SELECT p FROM Position p WHERE p.user.id = :userId AND p.quantity != 0 AND " +
           "(p.marketValue > :marginThreshold OR p.valueAtRisk > :varThreshold)")
    List<Position> findMarginExposurePositions(@Param("userId") UUID userId,
                                             @Param("marginThreshold") BigDecimal marginThreshold,
                                             @Param("varThreshold") BigDecimal varThreshold);

    // Bulk operations

    /**
     * Update market prices for multiple positions
     */
    @Modifying
    @Query("UPDATE Position p SET p.marketPrice = :price, p.marketValue = :price * ABS(p.quantity), " +
           "p.priceUpdatedAt = CURRENT_TIMESTAMP, p.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE p.symbol.id = :symbolId AND p.quantity != 0")
    int updateMarketPriceForSymbol(@Param("symbolId") UUID symbolId, @Param("price") BigDecimal price);

    /**
     * Recalculate unrealized P&L for all active positions of a symbol
     */
    @Modifying
    @Query(value = """
        UPDATE positions
        SET unrealized_pnl = CASE
            WHEN quantity > 0 THEN (market_price - average_cost) * quantity
            WHEN quantity < 0 THEN (average_cost - market_price) * ABS(quantity)
            ELSE 0
        END,
        updated_at = CURRENT_TIMESTAMP
        WHERE symbol_id = :symbolId AND quantity != 0 AND market_price IS NOT NULL AND average_cost IS NOT NULL
        """, nativeQuery = true)
    int recalculateUnrealizedPnlForSymbol(@Param("symbolId") UUID symbolId);

    /**
     * Block quantity for pending orders
     */
    @Modifying
    @Query("UPDATE Position p SET p.availableQuantity = p.availableQuantity - :quantityToBlock, " +
           "p.blockedQuantity = p.blockedQuantity + :quantityToBlock, p.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE p.id = :positionId AND p.availableQuantity >= :quantityToBlock")
    int blockQuantity(@Param("positionId") UUID positionId, @Param("quantityToBlock") Integer quantityToBlock);

    /**
     * Release blocked quantity
     */
    @Modifying
    @Query("UPDATE Position p SET p.availableQuantity = p.availableQuantity + :quantityToRelease, " +
           "p.blockedQuantity = p.blockedQuantity - :quantityToRelease, p.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE p.id = :positionId AND p.blockedQuantity >= :quantityToRelease")
    int releaseQuantity(@Param("positionId") UUID positionId, @Param("quantityToRelease") Integer quantityToRelease);

    // Statistics and reporting

    /**
     * Get position statistics by time period
     */
    @Query(value = """
        SELECT
            DATE_TRUNC('day', updated_at) as date,
            COUNT(CASE WHEN quantity != 0 THEN 1 END) as active_positions,
            AVG(CASE WHEN quantity != 0 THEN market_value END) as avg_position_value,
            SUM(unrealized_pnl) as total_unrealized_pnl,
            SUM(realized_pnl) as total_realized_pnl
        FROM positions
        WHERE user_id = :userId AND updated_at >= :startDate
        GROUP BY DATE_TRUNC('day', updated_at)
        ORDER BY date DESC
        """, nativeQuery = true)
    List<Object[]> getPositionStatisticsByDate(@Param("userId") UUID userId,
                                              @Param("startDate") java.time.ZonedDateTime startDate);

    /**
     * Get position turnover (how often positions change)
     */
    @Query(value = """
        SELECT s.symbol, s.name,
               COUNT(DISTINCT DATE(p.updated_at)) as trading_days,
               COUNT(p.position_id) as position_updates,
               AVG(ABS(p.quantity)) as avg_position_size,
               MAX(ABS(p.quantity)) as max_position_size
        FROM positions p
        JOIN symbols s ON p.symbol_id = s.symbol_id
        WHERE p.user_id = :userId AND p.updated_at >= :startDate
        GROUP BY s.symbol_id, s.symbol, s.name
        HAVING COUNT(p.position_id) > 1
        ORDER BY position_updates DESC
        """, nativeQuery = true)
    List<Object[]> getPositionTurnover(@Param("userId") UUID userId,
                                      @Param("startDate") java.time.ZonedDateTime startDate);

    // Performance metrics

    /**
     * Get best and worst performing positions
     */
    @Query(value = """
        SELECT
            'BEST' as category,
            s.symbol,
            p.unrealized_pnl,
            (p.unrealized_pnl / NULLIF(ABS(p.total_cost), 0)) * 100 as pnl_percentage
        FROM positions p
        JOIN symbols s ON p.symbol_id = s.symbol_id
        WHERE p.user_id = :userId AND p.quantity != 0 AND p.unrealized_pnl > 0
        ORDER BY p.unrealized_pnl DESC
        LIMIT 5

        UNION ALL

        SELECT
            'WORST' as category,
            s.symbol,
            p.unrealized_pnl,
            (p.unrealized_pnl / NULLIF(ABS(p.total_cost), 0)) * 100 as pnl_percentage
        FROM positions p
        JOIN symbols s ON p.symbol_id = s.symbol_id
        WHERE p.user_id = :userId AND p.quantity != 0 AND p.unrealized_pnl < 0
        ORDER BY p.unrealized_pnl ASC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> getBestAndWorstPositions(@Param("userId") UUID userId);

    /**
     * Get portfolio correlation with market indices
     */
    @Query(value = """
        SELECT
            p.symbol_id,
            s.symbol,
            p.market_value / NULLIF(portfolio_total.total_value, 0) as weight,
            CASE WHEN JSON_CONTAINS(s.index_memberships, '"XU100"') = 1 THEN 1 ELSE 0 END as xu100_member
        FROM positions p
        JOIN symbols s ON p.symbol_id = s.symbol_id
        CROSS JOIN (
            SELECT SUM(ABS(market_value)) as total_value
            FROM positions
            WHERE user_id = :userId AND quantity != 0
        ) portfolio_total
        WHERE p.user_id = :userId AND p.quantity != 0
        ORDER BY weight DESC
        """, nativeQuery = true)
    List<Object[]> getPortfolioComposition(@Param("userId") UUID userId);
}