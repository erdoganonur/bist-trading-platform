package com.bisttrading.marketdata.repository;

import com.bisttrading.marketdata.entity.MarketTick;
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

/**
 * MarketTick entity için TimescaleDB repository
 * Enhanced with comprehensive market data queries
 */
@Repository
public interface MarketTickRepository extends JpaRepository<MarketTick, Long> {

    // =====================================
    // BASIC CRUD OPERATIONS
    // =====================================

    /**
     * Sembol ve zaman aralığına göre tick verilerini getirir
     */
    @Query("SELECT m FROM MarketTick m WHERE m.symbol = :symbol " +
           "AND m.time BETWEEN :startTime AND :endTime " +
           "ORDER BY m.time DESC")
    List<MarketTick> findBySymbolAndTimeBetween(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        Pageable pageable
    );

    /**
     * Sembol için son N tick verisini getirir
     */
    @Query("SELECT m FROM MarketTick m WHERE m.symbol = :symbol " +
           "ORDER BY m.time DESC")
    List<MarketTick> findLatestBySymbol(@Param("symbol") String symbol, Pageable pageable);

    /**
     * Belirtilen zamandan sonraki tüm tick verilerini getirir
     */
    @Query("SELECT m FROM MarketTick m WHERE m.time > :time " +
           "ORDER BY m.time DESC")
    List<MarketTick> findByTimeAfter(@Param("time") OffsetDateTime time, Pageable pageable);

    // =====================================
    // BATCH INSERT OPERATIONS
    // =====================================

    /**
     * Batch insert with ON CONFLICT DO NOTHING for high performance
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO market_ticks (
            time, symbol, price, quantity, bid, ask, bid_size, ask_size,
            total_volume, total_trades, vwap, direction, value, buyer, seller, trade_time
        ) VALUES (
            :time, :symbol, :price, :quantity, :bid, :ask, :bidSize, :askSize,
            :totalVolume, :totalTrades, :vwap, :direction, :value, :buyer, :seller, :tradeTime
        ) ON CONFLICT (symbol, time) DO NOTHING
        """, nativeQuery = true)
    void insertTickWithConflictIgnore(
        @Param("time") OffsetDateTime time,
        @Param("symbol") String symbol,
        @Param("price") BigDecimal price,
        @Param("quantity") Long quantity,
        @Param("bid") BigDecimal bid,
        @Param("ask") BigDecimal ask,
        @Param("bidSize") Long bidSize,
        @Param("askSize") Long askSize,
        @Param("totalVolume") Long totalVolume,
        @Param("totalTrades") Long totalTrades,
        @Param("vwap") BigDecimal vwap,
        @Param("direction") String direction,
        @Param("value") BigDecimal value,
        @Param("buyer") String buyer,
        @Param("seller") String seller,
        @Param("tradeTime") String tradeTime
    );

    // =====================================
    // AGGREGATION QUERIES (OHLCV)
    // =====================================

    /**
     * OHLCV verilerini belirtilen zaman aralığı için hesaplar
     */
    @Query(value = """
        SELECT
            date_trunc(:interval, time) as period,
            symbol,
            FIRST(price, time) as open,
            MAX(price) as high,
            MIN(price) as low,
            LAST(price, time) as close,
            SUM(quantity) as volume,
            COUNT(*) as trades_count,
            AVG(price) as avg_price
        FROM market_ticks
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        GROUP BY period, symbol
        ORDER BY period ASC
        """, nativeQuery = true)
    List<Object[]> calculateOHLCV(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        @Param("interval") String interval // '1 minute', '5 minutes', '1 hour', etc.
    );

    /**
     * VWAP (Volume Weighted Average Price) hesaplar
     */
    @Query(value = """
        SELECT
            SUM(price * quantity) / SUM(quantity) as vwap
        FROM market_ticks
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        """, nativeQuery = true)
    BigDecimal calculateVWAP(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Time bucketed aggregations (TimescaleDB time_bucket function)
     */
    @Query(value = """
        SELECT
            time_bucket(:bucketSize, time) as bucket,
            symbol,
            FIRST(price, time) as open,
            MAX(price) as high,
            MIN(price) as low,
            LAST(price, time) as close,
            SUM(quantity) as volume,
            COUNT(*) as trades,
            SUM(price * quantity) / SUM(quantity) as vwap
        FROM market_ticks
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        GROUP BY bucket, symbol
        ORDER BY bucket ASC
        """, nativeQuery = true)
    List<Object[]> getTimeBucketedData(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        @Param("bucketSize") String bucketSize // '1 minute', '5 minutes', etc.
    );

    // =====================================
    // VOLUME AND TRADE ANALYSIS
    // =====================================

    /**
     * Sembol için günlük trade volume'unu hesaplar
     */
    @Query("SELECT COALESCE(SUM(m.totalVolume), 0) FROM MarketTick m " +
           "WHERE m.symbol = :symbol " +
           "AND m.time BETWEEN :startOfDay AND :endOfDay")
    Long calculateDailyVolume(@Param("symbol") String symbol,
                             @Param("startOfDay") OffsetDateTime startOfDay,
                             @Param("endOfDay") OffsetDateTime endOfDay);

    /**
     * Top volume semboller
     */
    @Query(value = """
        SELECT
            symbol,
            SUM(total_volume) as total_volume,
            SUM(total_trades) as total_trades,
            AVG(price) as avg_price
        FROM market_ticks
        WHERE time BETWEEN :startTime AND :endTime
        GROUP BY symbol
        ORDER BY total_volume DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopVolumeSymbols(
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        @Param("limit") int limit
    );

    /**
     * Price change analysis
     */
    @Query(value = """
        WITH price_changes AS (
            SELECT
                symbol,
                time,
                price,
                LAG(price) OVER (PARTITION BY symbol ORDER BY time) as prev_price
            FROM market_ticks
            WHERE symbol = :symbol
            AND time BETWEEN :startTime AND :endTime
        )
        SELECT
            symbol,
            COUNT(*) as total_changes,
            COUNT(CASE WHEN price > prev_price THEN 1 END) as upticks,
            COUNT(CASE WHEN price < prev_price THEN 1 END) as downticks,
            COUNT(CASE WHEN price = prev_price THEN 1 END) as neutral_ticks
        FROM price_changes
        WHERE prev_price IS NOT NULL
        GROUP BY symbol
        """, nativeQuery = true)
    List<Object[]> analyzePriceChanges(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // LIQUIDITY ANALYSIS
    // =====================================

    /**
     * Bid-Ask spread analysis
     */
    @Query(value = """
        SELECT
            symbol,
            AVG(ask - bid) as avg_spread,
            MIN(ask - bid) as min_spread,
            MAX(ask - bid) as max_spread,
            STDDEV(ask - bid) as spread_volatility
        FROM market_ticks
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        AND bid IS NOT NULL AND ask IS NOT NULL
        GROUP BY symbol
        """, nativeQuery = true)
    List<Object[]> analyzeBidAskSpread(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // DATA MANAGEMENT
    // =====================================

    /**
     * Aktif semboller listesi (son N saatte tick verisi olan)
     */
    @Query("SELECT DISTINCT m.symbol FROM MarketTick m " +
           "WHERE m.time > :since " +
           "ORDER BY m.symbol")
    List<String> findActiveSymbolsSince(@Param("since") OffsetDateTime since);

    /**
     * Eski verileri temizleme (data retention)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketTick m WHERE m.time < :cutoffTime")
    int deleteOldData(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * Sembol istatistikleri
     */
    @Query("SELECT COUNT(m) FROM MarketTick m WHERE m.symbol = :symbol " +
           "AND m.time BETWEEN :startTime AND :endTime")
    long countBySymbolAndTimeBetween(@Param("symbol") String symbol,
                                    @Param("startTime") OffsetDateTime startTime,
                                    @Param("endTime") OffsetDateTime endTime);

    /**
     * Database size ve performance metrics
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_records,
            COUNT(DISTINCT symbol) as unique_symbols,
            MIN(time) as earliest_record,
            MAX(time) as latest_record,
            pg_size_pretty(pg_total_relation_size('market_ticks')) as table_size
        FROM market_ticks
        """, nativeQuery = true)
    List<Object[]> getDatabaseMetrics();

    // =====================================
    // CONTINUOUS AGGREGATES SUPPORT
    // =====================================

    /**
     * Materialized view için minute-level aggregation
     */
    @Query(value = """
        SELECT
            time_bucket('1 minute', time) as minute_bucket,
            symbol,
            FIRST(price, time) as open,
            MAX(price) as high,
            MIN(price) as low,
            LAST(price, time) as close,
            SUM(quantity) as volume
        FROM market_ticks
        WHERE time >= :startTime
        GROUP BY minute_bucket, symbol
        ORDER BY minute_bucket DESC, symbol
        """, nativeQuery = true)
    List<Object[]> getMinuteAggregations(@Param("startTime") OffsetDateTime startTime);
}