package com.bisttrading.marketdata.repository;

import com.bisttrading.marketdata.entity.OrderBookSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OrderBookSnapshot entity için TimescaleDB repository
 * Enhanced with comprehensive order book analysis queries
 */
@Repository
public interface OrderBookSnapshotRepository extends JpaRepository<OrderBookSnapshot, Long> {

    // =====================================
    // BASIC CRUD OPERATIONS
    // =====================================

    /**
     * Sembol için en son order book snapshot'ını getirir
     */
    @Query("SELECT o FROM OrderBookSnapshot o WHERE o.symbol = :symbol " +
           "ORDER BY o.time DESC LIMIT 1")
    Optional<OrderBookSnapshot> findLatestBySymbol(@Param("symbol") String symbol);

    /**
     * Sembol ve zaman aralığına göre order book snapshot'larını getirir
     */
    @Query("SELECT o FROM OrderBookSnapshot o WHERE o.symbol = :symbol " +
           "AND o.time BETWEEN :startTime AND :endTime " +
           "ORDER BY o.time DESC")
    List<OrderBookSnapshot> findBySymbolAndTimeBetween(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        Pageable pageable
    );

    /**
     * Sembol için son N order book snapshot'ını getirir
     */
    @Query("SELECT o FROM OrderBookSnapshot o WHERE o.symbol = :symbol " +
           "ORDER BY o.time DESC")
    List<OrderBookSnapshot> findLatestSnapshotsBySymbol(@Param("symbol") String symbol, Pageable pageable);

    /**
     * Tüm semboller için en son snapshot'ları getirir
     */
    @Query("SELECT o FROM OrderBookSnapshot o WHERE o.id IN (" +
           "SELECT MAX(o2.id) FROM OrderBookSnapshot o2 GROUP BY o2.symbol" +
           ") ORDER BY o.symbol")
    List<OrderBookSnapshot> findLatestSnapshotsForAllSymbols();

    // =====================================
    // BATCH OPERATIONS WITH COMPRESSION
    // =====================================

    /**
     * Batch insert with ON CONFLICT DO UPDATE for order book snapshots
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO order_book_snapshots (
            time, symbol, bids, asks, bid_count, ask_count,
            total_bid_volume, total_ask_volume, best_bid, best_ask,
            best_bid_size, best_ask_size, bid_ask_spread, mid_price,
            imbalance_ratio, spread_percentage, sequence_number
        ) VALUES (
            :time, :symbol, :bids::jsonb, :asks::jsonb, :bidCount, :askCount,
            :totalBidVolume, :totalAskVolume, :bestBid, :bestAsk,
            :bestBidSize, :bestAskSize, :bidAskSpread, :midPrice,
            :imbalanceRatio, :spreadPercentage, :sequenceNumber
        ) ON CONFLICT (symbol, time) DO UPDATE SET
            bids = EXCLUDED.bids,
            asks = EXCLUDED.asks,
            bid_count = EXCLUDED.bid_count,
            ask_count = EXCLUDED.ask_count,
            total_bid_volume = EXCLUDED.total_bid_volume,
            total_ask_volume = EXCLUDED.total_ask_volume,
            best_bid = EXCLUDED.best_bid,
            best_ask = EXCLUDED.best_ask,
            best_bid_size = EXCLUDED.best_bid_size,
            best_ask_size = EXCLUDED.best_ask_size,
            bid_ask_spread = EXCLUDED.bid_ask_spread,
            mid_price = EXCLUDED.mid_price,
            imbalance_ratio = EXCLUDED.imbalance_ratio,
            spread_percentage = EXCLUDED.spread_percentage,
            sequence_number = EXCLUDED.sequence_number
        """, nativeQuery = true)
    void upsertOrderBookSnapshot(
        @Param("time") OffsetDateTime time,
        @Param("symbol") String symbol,
        @Param("bids") String bids,
        @Param("asks") String asks,
        @Param("bidCount") Integer bidCount,
        @Param("askCount") Integer askCount,
        @Param("totalBidVolume") Long totalBidVolume,
        @Param("totalAskVolume") Long totalAskVolume,
        @Param("bestBid") BigDecimal bestBid,
        @Param("bestAsk") BigDecimal bestAsk,
        @Param("bestBidSize") Long bestBidSize,
        @Param("bestAskSize") Long bestAskSize,
        @Param("bidAskSpread") BigDecimal bidAskSpread,
        @Param("midPrice") BigDecimal midPrice,
        @Param("imbalanceRatio") BigDecimal imbalanceRatio,
        @Param("spreadPercentage") BigDecimal spreadPercentage,
        @Param("sequenceNumber") Long sequenceNumber
    );

    // =====================================
    // LIQUIDITY ANALYSIS QUERIES
    // =====================================

    /**
     * Bid-Ask spread analysis over time
     */
    @Query(value = """
        SELECT
            time_bucket('1 minute', time) as minute,
            symbol,
            AVG(bid_ask_spread) as avg_spread,
            MIN(bid_ask_spread) as min_spread,
            MAX(bid_ask_spread) as max_spread,
            AVG(spread_percentage) as avg_spread_pct,
            AVG(mid_price) as avg_mid_price
        FROM order_book_snapshots
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        GROUP BY minute, symbol
        ORDER BY minute ASC
        """, nativeQuery = true)
    List<Object[]> analyzeSpreadOverTime(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Order book depth analysis
     */
    @Query(value = """
        SELECT
            symbol,
            AVG(total_bid_volume) as avg_bid_volume,
            AVG(total_ask_volume) as avg_ask_volume,
            AVG(bid_count) as avg_bid_levels,
            AVG(ask_count) as avg_ask_levels,
            AVG(imbalance_ratio) as avg_imbalance,
            AVG(bid_volume_5) as avg_bid_vol_5,
            AVG(ask_volume_5) as avg_ask_vol_5,
            AVG(bid_volume_10) as avg_bid_vol_10,
            AVG(ask_volume_10) as avg_ask_vol_10
        FROM order_book_snapshots
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        GROUP BY symbol
        """, nativeQuery = true)
    List<Object[]> analyzeOrderBookDepth(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Liquidity imbalance tracking
     */
    @Query(value = """
        SELECT
            time_bucket('5 minutes', time) as period,
            symbol,
            AVG(imbalance_ratio) as avg_imbalance,
            COUNT(CASE WHEN imbalance_ratio > 0.6 THEN 1 END) as bid_heavy_count,
            COUNT(CASE WHEN imbalance_ratio < 0.4 THEN 1 END) as ask_heavy_count,
            COUNT(CASE WHEN imbalance_ratio BETWEEN 0.4 AND 0.6 THEN 1 END) as balanced_count
        FROM order_book_snapshots
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        AND imbalance_ratio IS NOT NULL
        GROUP BY period, symbol
        ORDER BY period ASC
        """, nativeQuery = true)
    List<Object[]> trackLiquidityImbalance(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // HISTORICAL DEPTH ANALYSIS
    // =====================================

    /**
     * Price level analysis using JSONB queries
     */
    @Query(value = """
        WITH bid_levels AS (
            SELECT
                time,
                symbol,
                jsonb_array_elements(bids::jsonb) as bid_level
            FROM order_book_snapshots
            WHERE symbol = :symbol
            AND time BETWEEN :startTime AND :endTime
            AND bids IS NOT NULL
        ),
        ask_levels AS (
            SELECT
                time,
                symbol,
                jsonb_array_elements(asks::jsonb) as ask_level
            FROM order_book_snapshots
            WHERE symbol = :symbol
            AND time BETWEEN :startTime AND :endTime
            AND asks IS NOT NULL
        )
        SELECT
            'BID' as side,
            (bid_level->>'price')::numeric as price_level,
            AVG((bid_level->>'quantity')::numeric) as avg_quantity,
            COUNT(*) as appearance_count
        FROM bid_levels
        GROUP BY price_level
        UNION ALL
        SELECT
            'ASK' as side,
            (ask_level->>'price')::numeric as price_level,
            AVG((ask_level->>'quantity')::numeric) as avg_quantity,
            COUNT(*) as appearance_count
        FROM ask_levels
        GROUP BY price_level
        ORDER BY side, price_level
        """, nativeQuery = true)
    List<Object[]> analyzePriceLevels(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Order book thickness analysis
     */
    @Query(value = """
        SELECT
            symbol,
            time,
            best_bid,
            best_ask,
            (bid_volume_5 + ask_volume_5) as depth_5,
            (bid_volume_10 + ask_volume_10) as depth_10,
            (bid_volume_20 + ask_volume_20) as depth_20,
            total_bid_volume + total_ask_volume as total_depth
        FROM order_book_snapshots
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        ORDER BY time DESC
        """, nativeQuery = true)
    List<Object[]> analyzeOrderBookThickness(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        Pageable pageable
    );

    // =====================================
    // MARKET MICROSTRUCTURE
    // =====================================

    /**
     * Tick direction and order book changes correlation
     */
    @Query(value = """
        WITH order_changes AS (
            SELECT
                time,
                symbol,
                best_bid,
                best_ask,
                LAG(best_bid) OVER (PARTITION BY symbol ORDER BY time) as prev_best_bid,
                LAG(best_ask) OVER (PARTITION BY symbol ORDER BY time) as prev_best_ask,
                tick_direction
            FROM order_book_snapshots
            WHERE symbol = :symbol
            AND time BETWEEN :startTime AND :endTime
            ORDER BY time
        )
        SELECT
            symbol,
            COUNT(*) as total_updates,
            COUNT(CASE WHEN tick_direction = 1 THEN 1 END) as upticks,
            COUNT(CASE WHEN tick_direction = -1 THEN 1 END) as downticks,
            COUNT(CASE WHEN tick_direction = 0 THEN 1 END) as no_change,
            COUNT(CASE WHEN best_bid > prev_best_bid THEN 1 END) as bid_improvements,
            COUNT(CASE WHEN best_ask < prev_best_ask THEN 1 END) as ask_improvements
        FROM order_changes
        WHERE prev_best_bid IS NOT NULL
        GROUP BY symbol
        """, nativeQuery = true)
    List<Object[]> analyzeTickDirectionCorrelation(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // DATA MANAGEMENT
    // =====================================

    /**
     * Aktif semboller (son X dakikada order book güncellemesi olan)
     */
    @Query("SELECT DISTINCT o.symbol FROM OrderBookSnapshot o " +
           "WHERE o.time > :since " +
           "ORDER BY o.symbol")
    List<String> findActiveSymbolsSince(@Param("since") OffsetDateTime since);

    /**
     * Eski snapshot'ları temizleme (data retention)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderBookSnapshot o WHERE o.time < :cutoffTime")
    int deleteOldSnapshots(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * Sembol istatistikleri
     */
    @Query("SELECT COUNT(o) FROM OrderBookSnapshot o WHERE o.symbol = :symbol " +
           "AND o.time BETWEEN :startTime AND :endTime")
    long countBySymbolAndTimeBetween(@Param("symbol") String symbol,
                                    @Param("startTime") OffsetDateTime startTime,
                                    @Param("endTime") OffsetDateTime endTime);

    /**
     * Database performance metrics
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_snapshots,
            COUNT(DISTINCT symbol) as unique_symbols,
            MIN(time) as earliest_snapshot,
            MAX(time) as latest_snapshot,
            AVG(bid_count + ask_count) as avg_levels_per_snapshot,
            pg_size_pretty(pg_total_relation_size('order_book_snapshots')) as table_size
        FROM order_book_snapshots
        """, nativeQuery = true)
    List<Object[]> getDatabaseMetrics();

    /**
     * Compression ratio analysis
     */
    @Query(value = """
        SELECT
            symbol,
            COUNT(*) as snapshot_count,
            AVG(pg_column_size(bids)) as avg_bids_size,
            AVG(pg_column_size(asks)) as avg_asks_size,
            AVG(bid_count) as avg_bid_levels,
            AVG(ask_count) as avg_ask_levels
        FROM order_book_snapshots
        WHERE symbol = :symbol
        GROUP BY symbol
        """, nativeQuery = true)
    List<Object[]> analyzeCompressionRatio(@Param("symbol") String symbol);
}