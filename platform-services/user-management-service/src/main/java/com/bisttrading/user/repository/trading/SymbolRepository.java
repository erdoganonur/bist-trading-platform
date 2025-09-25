package com.bisttrading.user.repository.trading;

import com.bisttrading.user.entity.trading.Symbol;
import com.bisttrading.user.entity.trading.enums.MarketType;
import com.bisttrading.user.entity.trading.enums.SymbolStatus;
import com.bisttrading.user.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Symbol entity operations
 * Provides symbol lookup and market data queries
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface SymbolRepository extends BaseRepository<Symbol, UUID> {

    // Basic symbol lookups

    /**
     * Find symbol by symbol code (case insensitive)
     */
    @Query("SELECT s FROM Symbol s WHERE UPPER(s.symbol) = UPPER(:symbol) AND s.deletedAt IS NULL")
    Optional<Symbol> findBySymbol(@Param("symbol") String symbol);

    /**
     * Find symbol by ISIN code
     */
    Optional<Symbol> findByIsinCodeAndDeletedAtIsNull(String isinCode);

    /**
     * Find symbols by exchange
     */
    List<Symbol> findByExchangeAndDeletedAtIsNull(String exchange);

    /**
     * Find symbols by market type
     */
    List<Symbol> findByMarketTypeAndDeletedAtIsNull(MarketType marketType);

    /**
     * Find symbols by status
     */
    List<Symbol> findBySymbolStatusAndDeletedAtIsNull(SymbolStatus status);

    /**
     * Find tradeable symbols (ACTIVE, PRE_TRADING, POST_TRADING)
     */
    @Query("SELECT s FROM Symbol s WHERE s.symbolStatus IN ('ACTIVE', 'PRE_TRADING', 'POST_TRADING') AND s.deletedAt IS NULL")
    List<Symbol> findTradeableSymbols();

    // Sector and industry queries

    /**
     * Find symbols by sector
     */
    List<Symbol> findBySectorAndDeletedAtIsNull(String sector);

    /**
     * Find symbols by industry
     */
    List<Symbol> findByIndustryAndDeletedAtIsNull(String industry);

    /**
     * Get all unique sectors
     */
    @Query("SELECT DISTINCT s.sector FROM Symbol s WHERE s.sector IS NOT NULL AND s.deletedAt IS NULL ORDER BY s.sector")
    List<String> findAllSectors();

    /**
     * Get all unique industries
     */
    @Query("SELECT DISTINCT s.industry FROM Symbol s WHERE s.industry IS NOT NULL AND s.deletedAt IS NULL ORDER BY s.industry")
    List<String> findAllIndustries();

    // Index membership queries

    /**
     * Find symbols that are members of a specific index
     */
    @Query("SELECT s FROM Symbol s WHERE JSON_CONTAINS(s.indexMemberships, JSON_QUOTE(:indexCode)) = 1 AND s.deletedAt IS NULL")
    List<Symbol> findByIndexMembership(@Param("indexCode") String indexCode);

    /**
     * Find XU100 symbols (most liquid Turkish stocks)
     */
    @Query("SELECT s FROM Symbol s WHERE JSON_CONTAINS(s.indexMemberships, '\"XU100\"') = 1 AND s.deletedAt IS NULL")
    List<Symbol> findXU100Symbols();

    /**
     * Find XU030 symbols (top 30 liquid Turkish stocks)
     */
    @Query("SELECT s FROM Symbol s WHERE JSON_CONTAINS(s.indexMemberships, '\"XU030\"') = 1 AND s.deletedAt IS NULL")
    List<Symbol> findXU030Symbols();

    // Market cap and financial queries

    /**
     * Find symbols with market cap greater than threshold
     */
    @Query("SELECT s FROM Symbol s WHERE s.marketCap > :threshold AND s.deletedAt IS NULL ORDER BY s.marketCap DESC")
    List<Symbol> findByMarketCapGreaterThan(@Param("threshold") BigDecimal threshold);

    /**
     * Find symbols with dividend yield greater than threshold
     */
    @Query("SELECT s FROM Symbol s WHERE s.dividendYield > :threshold AND s.deletedAt IS NULL ORDER BY s.dividendYield DESC")
    List<Symbol> findByDividendYieldGreaterThan(@Param("threshold") BigDecimal threshold);

    /**
     * Find top market cap symbols
     */
    @Query("SELECT s FROM Symbol s WHERE s.marketCap IS NOT NULL AND s.deletedAt IS NULL ORDER BY s.marketCap DESC")
    List<Symbol> findTopByMarketCap(org.springframework.data.domain.Pageable pageable);

    // Price limit queries

    /**
     * Find symbols with price within daily limits
     */
    @Query("SELECT s FROM Symbol s WHERE " +
           "(:price IS NULL OR (s.floorPrice IS NULL OR :price >= s.floorPrice)) AND " +
           "(:price IS NULL OR (s.ceilingPrice IS NULL OR :price <= s.ceilingPrice)) AND " +
           "s.deletedAt IS NULL")
    List<Symbol> findSymbolsWithPriceInLimits(@Param("price") BigDecimal price);

    // Currency queries

    /**
     * Find symbols by currency code
     */
    List<Symbol> findByCurrencyCodeAndDeletedAtIsNull(String currencyCode);

    /**
     * Find TRY denominated symbols
     */
    @Query("SELECT s FROM Symbol s WHERE s.currencyCode = 'TRY' AND s.deletedAt IS NULL")
    List<Symbol> findTurkishLiraSymbols();

    // Search queries

    /**
     * Search symbols by name or symbol code (case insensitive)
     */
    @Query("SELECT s FROM Symbol s WHERE " +
           "(UPPER(s.symbol) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(s.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(s.localName) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) AND " +
           "s.deletedAt IS NULL " +
           "ORDER BY " +
           "CASE WHEN UPPER(s.symbol) = UPPER(:searchTerm) THEN 1 " +
           "     WHEN UPPER(s.symbol) LIKE UPPER(CONCAT(:searchTerm, '%')) THEN 2 " +
           "     WHEN UPPER(s.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) THEN 3 " +
           "     ELSE 4 END, " +
           "s.symbol")
    List<Symbol> searchSymbols(@Param("searchTerm") String searchTerm);

    /**
     * Search symbols with pagination
     */
    @Query("SELECT s FROM Symbol s WHERE " +
           "(UPPER(s.symbol) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(s.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(s.localName) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) AND " +
           "s.deletedAt IS NULL " +
           "ORDER BY s.symbol")
    org.springframework.data.domain.Page<Symbol> searchSymbols(
        @Param("searchTerm") String searchTerm,
        org.springframework.data.domain.Pageable pageable);

    // Statistics queries

    /**
     * Count symbols by market type
     */
    @Query("SELECT s.marketType, COUNT(s) FROM Symbol s WHERE s.deletedAt IS NULL GROUP BY s.marketType")
    List<Object[]> countByMarketType();

    /**
     * Count symbols by status
     */
    @Query("SELECT s.symbolStatus, COUNT(s) FROM Symbol s WHERE s.deletedAt IS NULL GROUP BY s.symbolStatus")
    List<Object[]> countByStatus();

    /**
     * Count symbols by sector
     */
    @Query("SELECT s.sector, COUNT(s) FROM Symbol s WHERE s.sector IS NOT NULL AND s.deletedAt IS NULL GROUP BY s.sector ORDER BY COUNT(s) DESC")
    List<Object[]> countBySector();

    // Bulk operations

    /**
     * Update symbol status for multiple symbols
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Symbol s SET s.symbolStatus = :status, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id IN :symbolIds")
    int updateSymbolStatus(@Param("symbolIds") List<UUID> symbolIds, @Param("status") SymbolStatus status);

    /**
     * Update price limits for a symbol
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Symbol s SET s.ceilingPrice = :ceilingPrice, s.floorPrice = :floorPrice, " +
           "s.referencePrice = :referencePrice, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :symbolId")
    int updatePriceLimits(@Param("symbolId") UUID symbolId,
                         @Param("ceilingPrice") BigDecimal ceilingPrice,
                         @Param("floorPrice") BigDecimal floorPrice,
                         @Param("referencePrice") BigDecimal referencePrice);

    // Custom native queries for complex operations

    /**
     * Get symbols with their order statistics
     */
    @Query(value = """
        SELECT s.symbol, s.name,
               COALESCE(order_stats.total_orders, 0) as total_orders,
               COALESCE(order_stats.active_orders, 0) as active_orders,
               COALESCE(order_stats.avg_order_value, 0) as avg_order_value
        FROM symbols s
        LEFT JOIN (
            SELECT symbol_id,
                   COUNT(*) as total_orders,
                   COUNT(CASE WHEN order_status IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED') THEN 1 END) as active_orders,
                   AVG(quantity * COALESCE(price, stop_price, trigger_price)) as avg_order_value
            FROM orders
            WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY symbol_id
        ) order_stats ON s.symbol_id = order_stats.symbol_id
        WHERE s.deleted_at IS NULL
        ORDER BY order_stats.total_orders DESC NULLS LAST
        """, nativeQuery = true)
    List<Object[]> getSymbolOrderStatistics();

    /**
     * Get popular symbols by trading volume
     */
    @Query(value = """
        SELECT s.symbol, s.name,
               COALESCE(SUM(oe.execution_quantity * oe.execution_price), 0) as volume
        FROM symbols s
        LEFT JOIN orders o ON s.symbol_id = o.symbol_id
        LEFT JOIN order_executions oe ON o.order_id = oe.order_id
        WHERE s.deleted_at IS NULL
        AND (oe.execution_time IS NULL OR oe.execution_time >= CURRENT_DATE - INTERVAL '30 days')
        GROUP BY s.symbol_id, s.symbol, s.name
        ORDER BY volume DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getPopularSymbolsByVolume(@Param("limit") int limit);
}