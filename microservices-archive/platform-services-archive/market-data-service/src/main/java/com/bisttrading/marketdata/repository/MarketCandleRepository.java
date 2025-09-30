package com.bisttrading.marketdata.repository;

import com.bisttrading.marketdata.entity.MarketCandle;
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
 * MarketCandle entity için TimescaleDB repository
 * Multiple timeframes için candlestick data operations
 */
@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {

    // =====================================
    // BASIC CRUD OPERATIONS
    // =====================================

    /**
     * Sembol, timeframe ve zaman aralığına göre candle verilerini getirir
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.time BETWEEN :startTime AND :endTime " +
           "ORDER BY c.time ASC")
    List<MarketCandle> findBySymbolAndTimeframeAndTimeBetween(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Sembol ve timeframe için son N candle'ı getirir
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "ORDER BY c.time DESC")
    List<MarketCandle> findLatestBySymbolAndTimeframe(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        Pageable pageable
    );

    /**
     * En son candle'ı getirir (aktif candle için)
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "ORDER BY c.time DESC LIMIT 1")
    Optional<MarketCandle> findLatestCandle(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe
    );

    /**
     * Tamamlanmamış candle'ları getirir
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.isComplete = false " +
           "ORDER BY c.time DESC")
    List<MarketCandle> findIncompleteCandles(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe
    );

    // =====================================
    // MULTIPLE TIMEFRAME OPERATIONS
    // =====================================

    /**
     * Tüm timeframe'ler için candle verileri
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.time BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timeframe, c.time ASC")
    List<MarketCandle> findAllTimeframes(
        @Param("symbol") String symbol,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Belirli timeframe listesi için candle verileri
     */
    @Query("SELECT c FROM MarketCandle c WHERE c.symbol = :symbol " +
           "AND c.timeframe IN :timeframes " +
           "AND c.time BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timeframe, c.time ASC")
    List<MarketCandle> findByTimeframes(
        @Param("symbol") String symbol,
        @Param("timeframes") List<String> timeframes,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // BATCH OPERATIONS
    // =====================================

    /**
     * Batch upsert with ON CONFLICT UPDATE
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO market_candles (
            time, symbol, timeframe, open, high, low, close, volume, trades_count, vwap, is_complete
        ) VALUES (
            :time, :symbol, :timeframe, :open, :high, :low, :close, :volume, :tradesCount, :vwap, :isComplete
        ) ON CONFLICT (symbol, timeframe, time) DO UPDATE SET
            high = GREATEST(market_candles.high, EXCLUDED.high),
            low = LEAST(market_candles.low, EXCLUDED.low),
            close = EXCLUDED.close,
            volume = market_candles.volume + EXCLUDED.volume,
            trades_count = market_candles.trades_count + EXCLUDED.trades_count,
            vwap = EXCLUDED.vwap,
            is_complete = EXCLUDED.is_complete,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertCandle(
        @Param("time") OffsetDateTime time,
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("open") BigDecimal open,
        @Param("high") BigDecimal high,
        @Param("low") BigDecimal low,
        @Param("close") BigDecimal close,
        @Param("volume") Long volume,
        @Param("tradesCount") Long tradesCount,
        @Param("vwap") BigDecimal vwap,
        @Param("isComplete") Boolean isComplete
    );

    /**
     * Bulk candle creation from tick data
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO market_candles (time, symbol, timeframe, open, high, low, close, volume, trades_count, vwap, is_complete)
        SELECT
            time_bucket(:interval, time) as candle_time,
            symbol,
            :timeframe,
            FIRST(price, time) as open,
            MAX(price) as high,
            MIN(price) as low,
            LAST(price, time) as close,
            SUM(quantity) as volume,
            COUNT(*) as trades_count,
            SUM(price * quantity) / SUM(quantity) as vwap,
            true as is_complete
        FROM market_ticks
        WHERE symbol = :symbol
        AND time BETWEEN :startTime AND :endTime
        GROUP BY candle_time, symbol
        ON CONFLICT (symbol, timeframe, time) DO UPDATE SET
            high = GREATEST(market_candles.high, EXCLUDED.high),
            low = LEAST(market_candles.low, EXCLUDED.low),
            close = EXCLUDED.close,
            volume = EXCLUDED.volume,
            trades_count = EXCLUDED.trades_count,
            vwap = EXCLUDED.vwap,
            is_complete = EXCLUDED.is_complete,
            updated_at = NOW()
        """, nativeQuery = true)
    void createCandlesFromTicks(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("interval") String interval,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // TECHNICAL ANALYSIS QUERIES
    // =====================================

    /**
     * Moving average hesaplama
     */
    @Query(value = """
        SELECT
            symbol,
            timeframe,
            time,
            close,
            AVG(close) OVER (
                PARTITION BY symbol, timeframe
                ORDER BY time
                ROWS BETWEEN :period - 1 PRECEDING AND CURRENT ROW
            ) as sma
        FROM market_candles
        WHERE symbol = :symbol
        AND timeframe = :timeframe
        AND time BETWEEN :startTime AND :endTime
        ORDER BY time ASC
        """, nativeQuery = true)
    List<Object[]> calculateSMA(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("period") int period,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Bollinger Bands hesaplama
     */
    @Query(value = """
        WITH sma_stddev AS (
            SELECT
                symbol, timeframe, time, close,
                AVG(close) OVER (
                    PARTITION BY symbol, timeframe
                    ORDER BY time
                    ROWS BETWEEN :period - 1 PRECEDING AND CURRENT ROW
                ) as sma,
                STDDEV(close) OVER (
                    PARTITION BY symbol, timeframe
                    ORDER BY time
                    ROWS BETWEEN :period - 1 PRECEDING AND CURRENT ROW
                ) as stddev
            FROM market_candles
            WHERE symbol = :symbol
            AND timeframe = :timeframe
            AND time BETWEEN :startTime AND :endTime
        )
        SELECT
            symbol, timeframe, time, close, sma,
            sma + (2 * stddev) as bb_upper,
            sma as bb_middle,
            sma - (2 * stddev) as bb_lower
        FROM sma_stddev
        ORDER BY time ASC
        """, nativeQuery = true)
    List<Object[]> calculateBollingerBands(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("period") int period,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * RSI (Relative Strength Index) hesaplama
     */
    @Query(value = """
        WITH price_changes AS (
            SELECT
                symbol, timeframe, time, close,
                close - LAG(close, 1) OVER (PARTITION BY symbol, timeframe ORDER BY time) as price_change
            FROM market_candles
            WHERE symbol = :symbol
            AND timeframe = :timeframe
            AND time BETWEEN :startTime AND :endTime
        ),
        gains_losses AS (
            SELECT
                symbol, timeframe, time, close,
                CASE WHEN price_change > 0 THEN price_change ELSE 0 END as gain,
                CASE WHEN price_change < 0 THEN ABS(price_change) ELSE 0 END as loss
            FROM price_changes
            WHERE price_change IS NOT NULL
        ),
        avg_gains_losses AS (
            SELECT
                symbol, timeframe, time, close,
                AVG(gain) OVER (
                    PARTITION BY symbol, timeframe
                    ORDER BY time
                    ROWS BETWEEN :period - 1 PRECEDING AND CURRENT ROW
                ) as avg_gain,
                AVG(loss) OVER (
                    PARTITION BY symbol, timeframe
                    ORDER BY time
                    ROWS BETWEEN :period - 1 PRECEDING AND CURRENT ROW
                ) as avg_loss
            FROM gains_losses
        )
        SELECT
            symbol, timeframe, time, close,
            CASE
                WHEN avg_loss = 0 THEN 100
                ELSE 100 - (100 / (1 + (avg_gain / avg_loss)))
            END as rsi
        FROM avg_gains_losses
        ORDER BY time ASC
        """, nativeQuery = true)
    List<Object[]> calculateRSI(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("period") int period,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // PATTERN RECOGNITION
    // =====================================

    /**
     * Candlestick pattern detection
     */
    @Query(value = """
        SELECT
            symbol, timeframe, time,
            open, high, low, close,
            CASE
                WHEN close = open THEN 'DOJI'
                WHEN close > open AND (close - open) / (high - low) > 0.7 THEN 'BULLISH_MARUBOZU'
                WHEN open > close AND (open - close) / (high - low) > 0.7 THEN 'BEARISH_MARUBOZU'
                WHEN close > open THEN 'BULLISH'
                WHEN open > close THEN 'BEARISH'
                ELSE 'NEUTRAL'
            END as pattern_type,
            (high - low) as range_size,
            ABS(close - open) as body_size
        FROM market_candles
        WHERE symbol = :symbol
        AND timeframe = :timeframe
        AND time BETWEEN :startTime AND :endTime
        ORDER BY time ASC
        """, nativeQuery = true)
    List<Object[]> detectCandlestickPatterns(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // =====================================
    // VOLUME ANALYSIS
    // =====================================

    /**
     * Volume profile analysis
     */
    @Query(value = """
        SELECT
            symbol,
            timeframe,
            ROUND(close / :priceStep) * :priceStep as price_level,
            SUM(volume) as total_volume,
            COUNT(*) as candle_count,
            AVG(volume) as avg_volume
        FROM market_candles
        WHERE symbol = :symbol
        AND timeframe = :timeframe
        AND time BETWEEN :startTime AND :endTime
        GROUP BY symbol, timeframe, price_level
        ORDER BY total_volume DESC
        LIMIT :topLevels
        """, nativeQuery = true)
    List<Object[]> analyzeVolumeProfile(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime,
        @Param("priceStep") BigDecimal priceStep,
        @Param("topLevels") int topLevels
    );

    // =====================================
    // DATA MANAGEMENT
    // =====================================

    /**
     * Aktif timeframe'ler
     */
    @Query("SELECT DISTINCT c.timeframe FROM MarketCandle c " +
           "WHERE c.symbol = :symbol " +
           "ORDER BY c.timeframe")
    List<String> findActiveTimeframes(@Param("symbol") String symbol);

    /**
     * Sembol ve timeframe istatistikleri
     */
    @Query(value = """
        SELECT
            symbol,
            timeframe,
            COUNT(*) as candle_count,
            MIN(time) as earliest_candle,
            MAX(time) as latest_candle,
            AVG(volume) as avg_volume,
            MAX(high) as highest_price,
            MIN(low) as lowest_price
        FROM market_candles
        WHERE symbol = :symbol
        AND (:timeframe IS NULL OR timeframe = :timeframe)
        GROUP BY symbol, timeframe
        ORDER BY symbol, timeframe
        """, nativeQuery = true)
    List<Object[]> getCandleStatistics(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe
    );

    /**
     * Data retention - eski candle'ları temizle
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketCandle c WHERE c.time < :cutoffTime AND c.timeframe = :timeframe")
    int deleteOldCandles(
        @Param("cutoffTime") OffsetDateTime cutoffTime,
        @Param("timeframe") String timeframe
    );

    /**
     * Tamamlanmamış candle'ları tamamla
     */
    @Modifying
    @Transactional
    @Query("UPDATE MarketCandle c SET c.isComplete = true, c.updatedAt = :now " +
           "WHERE c.time < :completionTime AND c.isComplete = false")
    int markCandlesAsComplete(
        @Param("completionTime") OffsetDateTime completionTime,
        @Param("now") OffsetDateTime now
    );
}