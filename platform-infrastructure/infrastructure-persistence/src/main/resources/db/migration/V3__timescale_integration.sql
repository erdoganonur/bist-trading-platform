-- =============================================================================
-- BIST Trading Platform - TimescaleDB Integration
-- Version: V3 - Market data time-series tables and hypertables
-- =============================================================================

-- =============================================================================
-- ENABLE TIMESCALEDB EXTENSION
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- =============================================================================
-- MARKET DATA SCHEMA
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS market_data;

-- =============================================================================
-- MARKET DATA TIME-SERIES TABLES
-- =============================================================================

-- Raw market ticks (highest frequency data)
CREATE TABLE market_data.market_ticks (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Price data
    price DECIMAL(20,6) NOT NULL,
    quantity INTEGER NOT NULL,
    volume DECIMAL(20,2) GENERATED ALWAYS AS (price * quantity) STORED,

    -- Order book data
    bid_price DECIMAL(20,6),
    ask_price DECIMAL(20,6),
    bid_size INTEGER,
    ask_size INTEGER,
    spread DECIMAL(20,6) GENERATED ALWAYS AS (ask_price - bid_price) STORED,

    -- Trade classification
    trade_type VARCHAR(10) DEFAULT 'REGULAR',
    trade_condition VARCHAR(20),
    is_buyer_maker BOOLEAN,

    -- Market microstructure
    sequence_number BIGINT,
    exchange_timestamp TIMESTAMPTZ,

    -- Data quality
    data_source VARCHAR(50) NOT NULL DEFAULT 'BIST',
    quality_score DECIMAL(3,2) DEFAULT 1.00,

    -- Constraints
    CONSTRAINT chk_price CHECK (price > 0),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bid_price CHECK (bid_price IS NULL OR bid_price > 0),
    CONSTRAINT chk_ask_price CHECK (ask_price IS NULL OR ask_price > 0),
    CONSTRAINT chk_spread CHECK (spread IS NULL OR spread >= 0),
    CONSTRAINT chk_trade_type CHECK (trade_type IN ('REGULAR', 'BLOCK', 'CROSS', 'AUCTION')),
    CONSTRAINT chk_quality_score CHECK (quality_score >= 0 AND quality_score <= 1.00),
    CONSTRAINT chk_sequence_number CHECK (sequence_number > 0)
);

-- Convert to hypertable with 1-hour chunks
SELECT create_hypertable('market_data.market_ticks', 'time',
    chunk_time_interval => INTERVAL '1 hour',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- OHLCV 1-minute aggregated data
CREATE TABLE market_data.ohlcv_1m (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- OHLC prices
    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,

    -- Volume data
    volume DECIMAL(20,2) NOT NULL DEFAULT 0,
    trade_count INTEGER NOT NULL DEFAULT 0,
    vwap DECIMAL(20,6),

    -- Order book summary
    bid_close DECIMAL(20,6),
    ask_close DECIMAL(20,6),
    spread_close DECIMAL(20,6) GENERATED ALWAYS AS (ask_close - bid_close) STORED,

    -- Price change metrics
    price_change DECIMAL(20,6) GENERATED ALWAYS AS (close_price - open_price) STORED,
    price_change_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN open_price > 0
        THEN ((close_price - open_price) / open_price) * 100
        ELSE 0 END
    ) STORED,

    -- Data quality
    data_completeness DECIMAL(3,2) DEFAULT 1.00,

    -- Constraints
    CONSTRAINT chk_ohlc_relationship CHECK (
        high_price >= GREATEST(open_price, close_price) AND
        low_price <= LEAST(open_price, close_price)
    ),
    CONSTRAINT chk_ohlc_prices CHECK (
        open_price > 0 AND high_price > 0 AND
        low_price > 0 AND close_price > 0
    ),
    CONSTRAINT chk_volume CHECK (volume >= 0),
    CONSTRAINT chk_trade_count CHECK (trade_count >= 0),
    CONSTRAINT chk_vwap CHECK (vwap IS NULL OR vwap > 0),
    CONSTRAINT chk_data_completeness CHECK (data_completeness >= 0 AND data_completeness <= 1.00)
);

-- Convert to hypertable with 1-day chunks
SELECT create_hypertable('market_data.ohlcv_1m', 'time',
    chunk_time_interval => INTERVAL '1 day',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- OHLCV 5-minute aggregated data
CREATE TABLE market_data.ohlcv_5m (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- OHLC prices
    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,

    -- Volume data
    volume DECIMAL(20,2) NOT NULL DEFAULT 0,
    trade_count INTEGER NOT NULL DEFAULT 0,
    vwap DECIMAL(20,6),

    -- Price metrics
    price_change DECIMAL(20,6) GENERATED ALWAYS AS (close_price - open_price) STORED,
    price_change_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN open_price > 0
        THEN ((close_price - open_price) / open_price) * 100
        ELSE 0 END
    ) STORED,

    -- Volatility metrics
    high_low_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN low_price > 0
        THEN ((high_price - low_price) / low_price) * 100
        ELSE 0 END
    ) STORED,

    -- Data quality
    data_completeness DECIMAL(3,2) DEFAULT 1.00,

    -- Same constraints as 1m table
    CONSTRAINT chk_ohlc_relationship_5m CHECK (
        high_price >= GREATEST(open_price, close_price) AND
        low_price <= LEAST(open_price, close_price)
    ),
    CONSTRAINT chk_ohlc_prices_5m CHECK (
        open_price > 0 AND high_price > 0 AND
        low_price > 0 AND close_price > 0
    ),
    CONSTRAINT chk_volume_5m CHECK (volume >= 0),
    CONSTRAINT chk_trade_count_5m CHECK (trade_count >= 0)
);

-- Convert to hypertable with 7-day chunks
SELECT create_hypertable('market_data.ohlcv_5m', 'time',
    chunk_time_interval => INTERVAL '7 days',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- OHLCV daily data
CREATE TABLE market_data.ohlcv_daily (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- OHLC prices
    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,

    -- Volume and activity
    volume DECIMAL(20,2) NOT NULL DEFAULT 0,
    trade_count INTEGER NOT NULL DEFAULT 0,
    vwap DECIMAL(20,6),
    turnover DECIMAL(20,2) GENERATED ALWAYS AS (volume) STORED,

    -- Price metrics
    price_change DECIMAL(20,6) GENERATED ALWAYS AS (close_price - open_price) STORED,
    price_change_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN open_price > 0
        THEN ((close_price - open_price) / open_price) * 100
        ELSE 0 END
    ) STORED,

    -- Volatility and range
    high_low_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN low_price > 0
        THEN ((high_price - low_price) / low_price) * 100
        ELSE 0 END
    ) STORED,
    true_range DECIMAL(20,6),

    -- Previous day comparison
    prev_close DECIMAL(20,6),
    gap_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN prev_close > 0
        THEN ((open_price - prev_close) / prev_close) * 100
        ELSE 0 END
    ) STORED,

    -- Market cap and valuation (for stocks)
    shares_outstanding BIGINT,
    market_cap DECIMAL(20,2) GENERATED ALWAYS AS (
        CASE WHEN shares_outstanding IS NOT NULL
        THEN close_price * shares_outstanding
        ELSE NULL END
    ) STORED,

    -- Trading session info
    session_type VARCHAR(20) DEFAULT 'REGULAR',
    is_trading_day BOOLEAN DEFAULT true,

    -- Same base constraints
    CONSTRAINT chk_ohlc_relationship_daily CHECK (
        high_price >= GREATEST(open_price, close_price) AND
        low_price <= LEAST(open_price, close_price)
    ),
    CONSTRAINT chk_ohlc_prices_daily CHECK (
        open_price > 0 AND high_price > 0 AND
        low_price > 0 AND close_price > 0
    ),
    CONSTRAINT chk_volume_daily CHECK (volume >= 0),
    CONSTRAINT chk_trade_count_daily CHECK (trade_count >= 0),
    CONSTRAINT chk_session_type CHECK (session_type IN ('REGULAR', 'EXTENDED', 'PRE_MARKET', 'POST_MARKET')),
    CONSTRAINT chk_shares_outstanding CHECK (shares_outstanding IS NULL OR shares_outstanding > 0)
);

-- Convert to hypertable with 30-day chunks
SELECT create_hypertable('market_data.ohlcv_daily', 'time',
    chunk_time_interval => INTERVAL '30 days',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- =============================================================================
-- ORDER BOOK DATA TABLES
-- =============================================================================

-- Order book snapshots
CREATE TABLE market_data.order_book_snapshots (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Best bid/ask
    best_bid DECIMAL(20,6),
    best_ask DECIMAL(20,6),
    bid_size INTEGER,
    ask_size INTEGER,
    spread DECIMAL(20,6) GENERATED ALWAYS AS (best_ask - best_bid) STORED,
    spread_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN best_bid > 0 AND best_ask > 0
        THEN ((best_ask - best_bid) / best_bid) * 100
        ELSE NULL END
    ) STORED,

    -- Order book depth
    total_bid_volume INTEGER DEFAULT 0,
    total_ask_volume INTEGER DEFAULT 0,
    bid_ask_ratio DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN total_ask_volume > 0
        THEN total_bid_volume::DECIMAL / total_ask_volume
        ELSE NULL END
    ) STORED,

    -- Market depth (5 levels)
    bid_levels JSONB, -- [{price: 100.50, size: 1000, orders: 5}, ...]
    ask_levels JSONB, -- [{price: 100.55, size: 800, orders: 3}, ...]

    -- Market microstructure
    mid_price DECIMAL(20,6) GENERATED ALWAYS AS (
        CASE WHEN best_bid IS NOT NULL AND best_ask IS NOT NULL
        THEN (best_bid + best_ask) / 2
        ELSE NULL END
    ) STORED,

    -- Data source
    data_source VARCHAR(50) NOT NULL DEFAULT 'BIST',
    sequence_number BIGINT,

    -- Constraints
    CONSTRAINT chk_best_prices CHECK (
        (best_bid IS NULL OR best_bid > 0) AND
        (best_ask IS NULL OR best_ask > 0) AND
        (best_bid IS NULL OR best_ask IS NULL OR best_ask >= best_bid)
    ),
    CONSTRAINT chk_sizes CHECK (
        (bid_size IS NULL OR bid_size > 0) AND
        (ask_size IS NULL OR ask_size > 0)
    ),
    CONSTRAINT chk_volumes CHECK (
        total_bid_volume >= 0 AND total_ask_volume >= 0
    )
);

-- Convert to hypertable with 1-hour chunks
SELECT create_hypertable('market_data.order_book_snapshots', 'time',
    chunk_time_interval => INTERVAL '1 hour',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

-- Market ticks indexes
CREATE INDEX idx_market_ticks_symbol_time ON market_data.market_ticks (symbol_id, time DESC);
CREATE INDEX idx_market_ticks_sequence ON market_data.market_ticks (sequence_number) WHERE sequence_number IS NOT NULL;
CREATE INDEX idx_market_ticks_price ON market_data.market_ticks (price);
CREATE INDEX idx_market_ticks_volume ON market_data.market_ticks (volume);

-- OHLCV indexes
CREATE INDEX idx_ohlcv_1m_symbol_time ON market_data.ohlcv_1m (symbol_id, time DESC);
CREATE INDEX idx_ohlcv_5m_symbol_time ON market_data.ohlcv_5m (symbol_id, time DESC);
CREATE INDEX idx_ohlcv_daily_symbol_time ON market_data.ohlcv_daily (symbol_id, time DESC);

-- Order book indexes
CREATE INDEX idx_order_book_symbol_time ON market_data.order_book_snapshots (symbol_id, time DESC);
CREATE INDEX idx_order_book_spread ON market_data.order_book_snapshots (spread) WHERE spread IS NOT NULL;

-- =============================================================================
-- CONTINUOUS AGGREGATES (Real-time Materialized Views)
-- =============================================================================

-- Real-time 1-minute OHLCV from ticks
CREATE MATERIALIZED VIEW market_data.ohlcv_1m_realtime
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS time,
    symbol_id,
    FIRST(price, time) AS open_price,
    MAX(price) AS high_price,
    MIN(price) AS low_price,
    LAST(price, time) AS close_price,
    SUM(volume) AS volume,
    COUNT(*) AS trade_count,
    (SUM(price * quantity) / SUM(quantity))::DECIMAL(20,6) AS vwap,
    LAST(bid_price, time) AS bid_close,
    LAST(ask_price, time) AS ask_close
FROM market_data.market_ticks
GROUP BY time_bucket('1 minute', time), symbol_id;

-- Add refresh policy for 1-minute aggregation
SELECT add_continuous_aggregate_policy('ohlcv_1m_realtime',
    start_offset => INTERVAL '2 minutes',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute'
);

-- Real-time 5-minute OHLCV from 1-minute data
CREATE MATERIALIZED VIEW market_data.ohlcv_5m_realtime
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
SELECT add_continuous_aggregate_policy('ohlcv_5m_realtime',
    start_offset => INTERVAL '10 minutes',
    end_offset => INTERVAL '5 minutes',
    schedule_interval => INTERVAL '5 minutes'
);

-- =============================================================================
-- ANALYTICS SCHEMA AND TABLES
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS analytics;

-- Technical indicators
CREATE TABLE analytics.technical_indicators (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),
    timeframe VARCHAR(10) NOT NULL, -- '1m', '5m', '1h', '1d'

    -- Moving averages
    sma_20 DECIMAL(20,6),
    sma_50 DECIMAL(20,6),
    sma_200 DECIMAL(20,6),
    ema_12 DECIMAL(20,6),
    ema_26 DECIMAL(20,6),

    -- MACD
    macd_line DECIMAL(20,6),
    macd_signal DECIMAL(20,6),
    macd_histogram DECIMAL(20,6),

    -- RSI
    rsi_14 DECIMAL(5,2),

    -- Bollinger Bands
    bb_upper DECIMAL(20,6),
    bb_middle DECIMAL(20,6),
    bb_lower DECIMAL(20,6),
    bb_width DECIMAL(10,4),

    -- Volume indicators
    volume_sma_20 DECIMAL(20,2),
    volume_ratio DECIMAL(10,4),

    -- Volatility
    atr_14 DECIMAL(20,6),
    volatility_20d DECIMAL(10,4),

    -- Support/Resistance levels
    support_levels DECIMAL(20,6)[],
    resistance_levels DECIMAL(20,6)[],

    -- Constraints
    CONSTRAINT chk_timeframe CHECK (timeframe IN ('1m', '5m', '15m', '1h', '4h', '1d')),
    CONSTRAINT chk_rsi CHECK (rsi_14 IS NULL OR (rsi_14 >= 0 AND rsi_14 <= 100)),
    CONSTRAINT chk_atr CHECK (atr_14 IS NULL OR atr_14 >= 0),
    CONSTRAINT chk_volatility CHECK (volatility_20d IS NULL OR volatility_20d >= 0)
);

-- Convert to hypertable
SELECT create_hypertable('analytics.technical_indicators', 'time',
    chunk_time_interval => INTERVAL '7 days',
    partitioning_column => 'symbol_id',
    number_partitions => 4
);

-- Market statistics
CREATE TABLE analytics.market_stats (
    time TIMESTAMPTZ NOT NULL,
    calculation_type VARCHAR(50) NOT NULL, -- 'daily_summary', 'sector_performance', etc.

    -- Market overview
    total_volume DECIMAL(20,2),
    total_trades INTEGER,
    advancing_issues INTEGER,
    declining_issues INTEGER,
    unchanged_issues INTEGER,

    -- Price metrics
    avg_price_change DECIMAL(10,4),
    median_price_change DECIMAL(10,4),
    volatility_index DECIMAL(10,4),

    -- Volume metrics
    total_turnover DECIMAL(20,2),
    avg_trade_size DECIMAL(20,2),
    volume_weighted_price DECIMAL(20,6),

    -- Sector breakdown
    sector_performance JSONB, -- {sector_id: {volume, trades, avg_change, etc.}}

    -- Additional metrics
    additional_metrics JSONB DEFAULT '{}'::jsonb,

    -- Constraints
    CONSTRAINT chk_calculation_type CHECK (LENGTH(calculation_type) > 0),
    CONSTRAINT chk_counts CHECK (
        (advancing_issues IS NULL OR advancing_issues >= 0) AND
        (declining_issues IS NULL OR declining_issues >= 0) AND
        (unchanged_issues IS NULL OR unchanged_issues >= 0) AND
        (total_trades IS NULL OR total_trades >= 0)
    ),
    CONSTRAINT chk_volumes CHECK (
        (total_volume IS NULL OR total_volume >= 0) AND
        (total_turnover IS NULL OR total_turnover >= 0) AND
        (avg_trade_size IS NULL OR avg_trade_size >= 0)
    )
);

-- Convert to hypertable
SELECT create_hypertable('analytics.market_stats', 'time',
    chunk_time_interval => INTERVAL '30 days'
);

-- Create indexes for analytics
CREATE INDEX idx_technical_indicators_symbol_timeframe ON analytics.technical_indicators (symbol_id, timeframe, time DESC);
CREATE INDEX idx_market_stats_type_time ON analytics.market_stats (calculation_type, time DESC);

-- =============================================================================
-- COMMENTS AND DOCUMENTATION
-- =============================================================================

COMMENT ON SCHEMA market_data IS 'TimescaleDB time-series market data storage';
COMMENT ON SCHEMA analytics IS 'Analytics and technical indicators';

COMMENT ON TABLE market_data.market_ticks IS 'Raw market tick data with microsecond precision';
COMMENT ON TABLE market_data.ohlcv_1m IS '1-minute OHLCV candlestick data';
COMMENT ON TABLE market_data.ohlcv_5m IS '5-minute OHLCV candlestick data';
COMMENT ON TABLE market_data.ohlcv_daily IS 'Daily OHLCV candlestick data with extended metrics';
COMMENT ON TABLE market_data.order_book_snapshots IS 'Order book snapshots with market depth';
COMMENT ON TABLE analytics.technical_indicators IS 'Technical analysis indicators by timeframe';
COMMENT ON TABLE analytics.market_stats IS 'Market-wide statistics and summaries';

-- Column comments
COMMENT ON COLUMN market_data.market_ticks.sequence_number IS 'Exchange sequence number for trade ordering';
COMMENT ON COLUMN market_data.market_ticks.quality_score IS 'Data quality score from 0.0 to 1.0';
COMMENT ON COLUMN market_data.ohlcv_daily.true_range IS 'True Range for volatility calculation';
COMMENT ON COLUMN market_data.order_book_snapshots.bid_levels IS 'JSON array of bid price levels';
COMMENT ON COLUMN market_data.order_book_snapshots.ask_levels IS 'JSON array of ask price levels';

-- =============================================================================
-- TIMESCALE INTEGRATION COMPLETE
-- =============================================================================