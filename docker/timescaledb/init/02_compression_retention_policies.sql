-- =============================================================================
-- BIST Trading Platform - TimescaleDB Compression and Retention Policies
-- Optimize storage and performance for time-series market data
-- =============================================================================

-- =============================================================================
-- COMPRESSION POLICIES
-- =============================================================================

-- Enable compression on market_ticks table (compress data older than 1 day)
ALTER TABLE market_data.market_ticks SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC, price DESC'
);

-- Add compression policy for market_ticks
SELECT add_compression_policy('market_data.market_ticks', INTERVAL '1 day');

-- Enable compression on OHLCV tables
-- 1-minute data: compress after 7 days
ALTER TABLE market_data.ohlcv_1m SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('market_data.ohlcv_1m', INTERVAL '7 days');

-- 5-minute data: compress after 30 days
ALTER TABLE market_data.ohlcv_5m SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('market_data.ohlcv_5m', INTERVAL '30 days');

-- Daily data: compress after 90 days
ALTER TABLE market_data.ohlcv_daily SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('market_data.ohlcv_daily', INTERVAL '90 days');

-- Order book snapshots: compress after 6 hours (very high frequency)
ALTER TABLE market_data.order_book_snapshots SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC, best_bid DESC, best_ask ASC'
);
SELECT add_compression_policy('market_data.order_book_snapshots', INTERVAL '6 hours');

-- Technical indicators: compress after 30 days
ALTER TABLE analytics.technical_indicators SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id, timeframe',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('analytics.technical_indicators', INTERVAL '30 days');

-- Market stats: compress after 7 days
ALTER TABLE analytics.market_stats SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'calculation_type',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('analytics.market_stats', INTERVAL '7 days');

-- Audit data: compress after 1 day
ALTER TABLE audit.data_quality SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'table_name, metric_name',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('audit.data_quality', INTERVAL '1 day');

-- =============================================================================
-- RETENTION POLICIES
-- =============================================================================

-- Market ticks: Keep raw ticks for 3 months (high-frequency data)
SELECT add_retention_policy('market_data.market_ticks', INTERVAL '3 months');

-- Order book snapshots: Keep for 1 month (very high frequency)
SELECT add_retention_policy('market_data.order_book_snapshots', INTERVAL '1 month');

-- 1-minute OHLCV: Keep for 2 years
SELECT add_retention_policy('market_data.ohlcv_1m', INTERVAL '2 years');

-- 5-minute OHLCV: Keep for 5 years
SELECT add_retention_policy('market_data.ohlcv_5m', INTERVAL '5 years');

-- Daily OHLCV: Keep forever (no retention policy)
-- This is historical data that should be preserved

-- Technical indicators: Keep for 1 year
SELECT add_retention_policy('analytics.technical_indicators', INTERVAL '1 year');

-- Market stats: Keep for 2 years
SELECT add_retention_policy('analytics.market_stats', INTERVAL '2 years');

-- Audit data: Keep for 7 years (compliance requirement)
SELECT add_retention_policy('audit.data_quality', INTERVAL '7 years');

-- =============================================================================
-- CONTINUOUS AGGREGATES (Real-time aggregations)
-- =============================================================================

-- 1-minute OHLCV from market ticks
CREATE MATERIALIZED VIEW market_data.ohlcv_1m_continuous
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS time,
    symbol_id,
    FIRST(price, time) AS open_price,
    MAX(price) AS high_price,
    MIN(price) AS low_price,
    LAST(price, time) AS close_price,
    SUM(quantity) AS volume,
    COUNT(*) AS trade_count,
    (SUM(price * quantity) / SUM(quantity))::DECIMAL(20,6) AS vwap,
    LAST(bid_price, time) AS bid_close,
    LAST(ask_price, time) AS ask_close,
    LAST(ask_price, time) - LAST(bid_price, time) AS spread_close
FROM market_data.market_ticks
GROUP BY time_bucket('1 minute', time), symbol_id;

-- Add refresh policy for 1-minute aggregation (refresh every minute)
SELECT add_continuous_aggregate_policy('ohlcv_1m_continuous',
    start_offset => INTERVAL '2 minutes',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- 5-minute OHLCV from 1-minute data
CREATE MATERIALIZED VIEW market_data.ohlcv_5m_continuous
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('5 minutes', time) AS time,
    symbol_id,
    FIRST(open_price, time) AS open_price,
    MAX(high_price) AS high_price,
    MIN(low_price) AS low_price,
    LAST(close_price, time) AS close_price,
    SUM(volume) AS volume,
    SUM(trade_count) AS trade_count,
    (SUM(volume * vwap) / SUM(volume))::DECIMAL(20,6) AS vwap
FROM market_data.ohlcv_1m
GROUP BY time_bucket('5 minutes', time), symbol_id;

-- Add refresh policy for 5-minute aggregation
SELECT add_continuous_aggregate_policy('ohlcv_5m_continuous',
    start_offset => INTERVAL '10 minutes',
    end_offset => INTERVAL '5 minutes',
    schedule_interval => INTERVAL '5 minutes');

-- Hourly OHLCV
CREATE MATERIALIZED VIEW market_data.ohlcv_1h_continuous
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS time,
    symbol_id,
    FIRST(open_price, time) AS open_price,
    MAX(high_price) AS high_price,
    MIN(low_price) AS low_price,
    LAST(close_price, time) AS close_price,
    SUM(volume) AS volume,
    SUM(trade_count) AS trade_count,
    (SUM(volume * vwap) / SUM(volume))::DECIMAL(20,6) AS vwap
FROM market_data.ohlcv_5m
GROUP BY time_bucket('1 hour', time), symbol_id;

SELECT add_continuous_aggregate_policy('ohlcv_1h_continuous',
    start_offset => INTERVAL '2 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Daily market statistics
CREATE MATERIALIZED VIEW analytics.daily_market_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS time,
    COUNT(DISTINCT symbol_id) AS active_symbols,
    SUM(volume) AS total_volume,
    SUM(trade_count) AS total_trades,
    AVG(vwap) AS average_price,
    STDDEV(vwap) AS price_volatility,
    MIN(low_price) AS market_low,
    MAX(high_price) AS market_high
FROM market_data.ohlcv_1m
GROUP BY time_bucket('1 day', time);

SELECT add_continuous_aggregate_policy('daily_market_summary',
    start_offset => INTERVAL '2 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day');

-- =============================================================================
-- COMPRESSION SETTINGS FOR CONTINUOUS AGGREGATES
-- =============================================================================

-- Enable compression on continuous aggregates
ALTER MATERIALIZED VIEW market_data.ohlcv_1m_continuous SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('ohlcv_1m_continuous', INTERVAL '7 days');

ALTER MATERIALIZED VIEW market_data.ohlcv_5m_continuous SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('ohlcv_5m_continuous', INTERVAL '30 days');

ALTER MATERIALIZED VIEW market_data.ohlcv_1h_continuous SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('ohlcv_1h_continuous', INTERVAL '90 days');

ALTER MATERIALIZED VIEW analytics.daily_market_summary SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('daily_market_summary', INTERVAL '90 days');

-- =============================================================================
-- MONITORING AND MAINTENANCE FUNCTIONS
-- =============================================================================

-- Function to check compression status
CREATE OR REPLACE FUNCTION admin.check_compression_status()
RETURNS TABLE (
    schema_name TEXT,
    table_name TEXT,
    total_chunks BIGINT,
    compressed_chunks BIGINT,
    uncompressed_chunks BIGINT,
    compression_ratio NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ht.schema_name::TEXT,
        ht.table_name::TEXT,
        cs.total_chunks,
        cs.number_compressed_chunks AS compressed_chunks,
        (cs.total_chunks - cs.number_compressed_chunks) AS uncompressed_chunks,
        CASE
            WHEN cs.uncompressed_heap_size > 0 THEN
                ROUND((cs.uncompressed_heap_size::NUMERIC / cs.compressed_heap_size::NUMERIC), 2)
            ELSE 0
        END AS compression_ratio
    FROM timescaledb_information.hypertables ht
    LEFT JOIN timescaledb_information.compression_settings cs ON cs.hypertable_name = ht.table_name
    WHERE ht.schema_name IN ('market_data', 'analytics', 'audit');
END;
$$ LANGUAGE plpgsql;

-- Function to get storage usage by table
CREATE OR REPLACE FUNCTION admin.get_storage_usage()
RETURNS TABLE (
    schema_name TEXT,
    table_name TEXT,
    total_size TEXT,
    table_size TEXT,
    index_size TEXT,
    row_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        schemaname::TEXT,
        tablename::TEXT,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
        pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
        pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) AS index_size,
        COALESCE(n_tup_ins + n_tup_upd - n_tup_del, 0) AS row_count
    FROM pg_tables t
    LEFT JOIN pg_stat_user_tables s ON s.relname = t.tablename AND s.schemaname = t.schemaname
    WHERE t.schemaname IN ('market_data', 'analytics', 'reference', 'audit')
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to manually trigger compression
CREATE OR REPLACE FUNCTION admin.compress_chunks(
    p_table_name TEXT,
    p_older_than INTERVAL DEFAULT INTERVAL '1 day'
)
RETURNS INTEGER AS $$
DECLARE
    compressed_count INTEGER := 0;
    chunk_record RECORD;
BEGIN
    FOR chunk_record IN
        SELECT chunk_schema, chunk_name
        FROM timescaledb_information.chunks
        WHERE hypertable_name = p_table_name
        AND NOT is_compressed
        AND range_end <= NOW() - p_older_than
    LOOP
        PERFORM compress_chunk(chunk_record.chunk_schema||'.'||chunk_record.chunk_name);
        compressed_count := compressed_count + 1;
    END LOOP;

    RETURN compressed_count;
END;
$$ LANGUAGE plpgsql;

-- Grant admin functions to market_data_user
GRANT EXECUTE ON FUNCTION admin.check_compression_status() TO market_data_user;
GRANT EXECUTE ON FUNCTION admin.get_storage_usage() TO market_data_user;
GRANT EXECUTE ON FUNCTION admin.compress_chunks(TEXT, INTERVAL) TO market_data_user;

-- =============================================================================
-- PERFORMANCE OPTIMIZATION VIEWS
-- =============================================================================

-- View for real-time market data summary
CREATE VIEW market_data.live_market_summary AS
SELECT
    s.symbol,
    s.full_name,
    mt.price AS last_price,
    mt.time AS last_trade_time,
    daily.open_price,
    daily.high_price,
    daily.low_price,
    daily.volume AS daily_volume,
    daily.trade_count AS daily_trades,
    ROUND(((mt.price - daily.open_price) / daily.open_price * 100)::NUMERIC, 2) AS change_percent
FROM reference.symbols s
LEFT JOIN LATERAL (
    SELECT price, time
    FROM market_data.market_ticks mt
    WHERE mt.symbol_id = s.symbol_id
    ORDER BY time DESC
    LIMIT 1
) mt ON true
LEFT JOIN LATERAL (
    SELECT open_price, high_price, low_price, volume, trade_count
    FROM market_data.ohlcv_daily od
    WHERE od.symbol_id = s.symbol_id
    AND od.time = CURRENT_DATE
    LIMIT 1
) daily ON true
WHERE s.data_feed_enabled = true;

-- =============================================================================
-- COMPLETION MESSAGE
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'TimescaleDB compression and retention policies configured successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Compression policies:';
    RAISE NOTICE '  - market_ticks: compress after 1 day';
    RAISE NOTICE '  - ohlcv_1m: compress after 7 days';
    RAISE NOTICE '  - ohlcv_5m: compress after 30 days';
    RAISE NOTICE '  - ohlcv_daily: compress after 90 days';
    RAISE NOTICE '  - order_book_snapshots: compress after 6 hours';
    RAISE NOTICE '';
    RAISE NOTICE 'Retention policies:';
    RAISE NOTICE '  - market_ticks: keep for 3 months';
    RAISE NOTICE '  - order_book_snapshots: keep for 1 month';
    RAISE NOTICE '  - ohlcv_1m: keep for 2 years';
    RAISE NOTICE '  - ohlcv_5m: keep for 5 years';
    RAISE NOTICE '  - ohlcv_daily: keep forever';
    RAISE NOTICE '';
    RAISE NOTICE 'Continuous aggregates:';
    RAISE NOTICE '  - 1-minute OHLCV (refresh every minute)';
    RAISE NOTICE '  - 5-minute OHLCV (refresh every 5 minutes)';
    RAISE NOTICE '  - Hourly OHLCV (refresh every hour)';
    RAISE NOTICE '  - Daily market summary (refresh daily)';
    RAISE NOTICE '';
    RAISE NOTICE 'Admin functions created for monitoring and maintenance';
    RAISE NOTICE '=============================================================================';
END $$;