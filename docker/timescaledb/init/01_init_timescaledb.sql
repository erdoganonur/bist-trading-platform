-- =============================================================================
-- BIST Trading Platform - TimescaleDB Initialization
-- Market Data Time-Series Database Setup
-- =============================================================================

-- Create TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS uuid-ossp;

-- =============================================================================
-- DATABASE USERS AND PERMISSIONS
-- =============================================================================

-- Create application user for market data service
CREATE USER market_data_user WITH
    PASSWORD 'SecureMarketDataPassword123!'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    CONNECTION LIMIT 50;

-- Create read-only user for analytics
CREATE USER market_data_readonly WITH
    PASSWORD 'SecureReadOnlyPassword123!'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    CONNECTION LIMIT 20;

-- Create replication user
CREATE USER replicator WITH
    PASSWORD 'SecureReplicaPassword123!'
    REPLICATION
    LOGIN
    CONNECTION LIMIT 5;

-- =============================================================================
-- SCHEMAS
-- =============================================================================

-- Create schemas for different types of market data
CREATE SCHEMA market_data;      -- Real-time market data
CREATE SCHEMA analytics;        -- Calculated indicators and analytics
CREATE SCHEMA reference;        -- Reference data (symbols, exchanges)
CREATE SCHEMA audit;            -- Audit and monitoring data

-- Grant schema usage
GRANT USAGE ON SCHEMA market_data TO market_data_user, market_data_readonly;
GRANT USAGE ON SCHEMA analytics TO market_data_user, market_data_readonly;
GRANT USAGE ON SCHEMA reference TO market_data_user, market_data_readonly;
GRANT USAGE ON SCHEMA audit TO market_data_user, market_data_readonly;

-- =============================================================================
-- REFERENCE DATA TABLES
-- =============================================================================

-- Exchanges table
CREATE TABLE reference.exchanges (
    exchange_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exchange_code VARCHAR(10) NOT NULL UNIQUE,
    exchange_name VARCHAR(100) NOT NULL,
    country_code VARCHAR(3) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    trading_hours JSONB,
    market_holidays JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Market data symbols (optimized for lookups)
CREATE TABLE reference.symbols (
    symbol_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol VARCHAR(20) NOT NULL,
    exchange_id UUID NOT NULL REFERENCES reference.exchanges(exchange_id),
    symbol_type VARCHAR(20) NOT NULL DEFAULT 'EQUITY', -- EQUITY, BOND, INDEX, CURRENCY
    full_name VARCHAR(255),
    isin_code VARCHAR(12),

    -- Trading specifications
    lot_size INTEGER NOT NULL DEFAULT 1,
    tick_size DECIMAL(10,6) NOT NULL DEFAULT 0.01,
    price_precision INTEGER NOT NULL DEFAULT 2,

    -- Market data settings
    data_feed_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_trade_date DATE,

    -- Metadata
    sector VARCHAR(100),
    industry VARCHAR(100),
    market_cap DECIMAL(20,2),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    UNIQUE(symbol, exchange_id)
);

-- Create indexes for fast symbol lookups
CREATE INDEX idx_symbols_symbol ON reference.symbols(symbol);
CREATE INDEX idx_symbols_exchange ON reference.symbols(exchange_id);
CREATE INDEX idx_symbols_isin ON reference.symbols(isin_code);
CREATE INDEX idx_symbols_active ON reference.symbols(symbol, exchange_id) WHERE data_feed_enabled = TRUE;

-- =============================================================================
-- MARKET DATA HYPERTABLES
-- =============================================================================

-- Raw market ticks (highest frequency data)
CREATE TABLE market_data.market_ticks (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Price data
    price DECIMAL(20,6) NOT NULL,
    quantity INTEGER NOT NULL,

    -- Trade information
    trade_id VARCHAR(50),
    trade_type CHAR(1) NOT NULL, -- 'B' = Buy, 'S' = Sell, 'U' = Unknown
    trade_condition VARCHAR(10), -- Market condition codes

    -- Microstructure data
    bid_price DECIMAL(20,6),
    ask_price DECIMAL(20,6),
    bid_size INTEGER,
    ask_size INTEGER,

    -- Metadata
    exchange_timestamp TIMESTAMPTZ,
    sequence_number BIGINT,
    source VARCHAR(20) NOT NULL DEFAULT 'BIST',

    -- Constraints
    CHECK (price > 0),
    CHECK (quantity > 0),
    CHECK (trade_type IN ('B', 'S', 'U'))
);

-- Convert to hypertable with 1-hour chunks
SELECT create_hypertable('market_data.market_ticks', 'time', chunk_time_interval => INTERVAL '1 hour');

-- Add space partitioning by symbol_id for better performance
SELECT add_dimension('market_data.market_ticks', 'symbol_id', number_partitions => 4);

-- OHLCV candles (1-minute aggregates)
CREATE TABLE market_data.ohlcv_1m (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- OHLCV data
    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,

    -- Additional metrics
    trade_count INTEGER NOT NULL DEFAULT 0,
    vwap DECIMAL(20,6), -- Volume Weighted Average Price

    -- Market microstructure
    bid_close DECIMAL(20,6),
    ask_close DECIMAL(20,6),
    spread_close DECIMAL(20,6),

    -- Constraints
    CHECK (high_price >= low_price),
    CHECK (high_price >= open_price),
    CHECK (high_price >= close_price),
    CHECK (low_price <= open_price),
    CHECK (low_price <= close_price),
    CHECK (volume >= 0),
    CHECK (trade_count >= 0)
);

-- Convert to hypertable with 1-day chunks
SELECT create_hypertable('market_data.ohlcv_1m', 'time', chunk_time_interval => INTERVAL '1 day');
SELECT add_dimension('market_data.ohlcv_1m', 'symbol_id', number_partitions => 4);

-- OHLCV candles (5-minute aggregates)
CREATE TABLE market_data.ohlcv_5m (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    trade_count INTEGER NOT NULL DEFAULT 0,
    vwap DECIMAL(20,6),

    CHECK (high_price >= low_price),
    CHECK (volume >= 0)
);

SELECT create_hypertable('market_data.ohlcv_5m', 'time', chunk_time_interval => INTERVAL '7 days');
SELECT add_dimension('market_data.ohlcv_5m', 'symbol_id', number_partitions => 4);

-- Daily OHLCV with additional statistics
CREATE TABLE market_data.ohlcv_daily (
    time DATE NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Standard OHLCV
    open_price DECIMAL(20,6) NOT NULL,
    high_price DECIMAL(20,6) NOT NULL,
    low_price DECIMAL(20,6) NOT NULL,
    close_price DECIMAL(20,6) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    trade_count INTEGER NOT NULL DEFAULT 0,

    -- Additional daily statistics
    vwap DECIMAL(20,6),
    previous_close DECIMAL(20,6),
    price_change DECIMAL(20,6),
    price_change_percent DECIMAL(8,4),

    -- Market statistics
    high_time TIMESTAMPTZ,
    low_time TIMESTAMPTZ,
    first_trade_time TIMESTAMPTZ,
    last_trade_time TIMESTAMPTZ,

    -- Volume statistics
    volume_weighted_spread DECIMAL(20,6),
    average_trade_size DECIMAL(15,2),

    CHECK (high_price >= low_price),
    CHECK (volume >= 0)
);

SELECT create_hypertable('market_data.ohlcv_daily', 'time', chunk_time_interval => INTERVAL '30 days');

-- Order book snapshots (Level 2 market data)
CREATE TABLE market_data.order_book_snapshots (
    time TIMESTAMPTZ NOT NULL,
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Best bid/ask
    best_bid DECIMAL(20,6),
    best_ask DECIMAL(20,6),
    bid_size INTEGER,
    ask_size INTEGER,

    -- Order book depth (JSON for flexibility)
    bids JSONB, -- Array of [price, size] pairs
    asks JSONB, -- Array of [price, size] pairs

    -- Market quality metrics
    spread DECIMAL(20,6),
    mid_price DECIMAL(20,6),

    -- Sequence information
    sequence_number BIGINT,

    CHECK (bids IS NOT NULL OR asks IS NOT NULL)
);

SELECT create_hypertable('market_data.order_book_snapshots', 'time', chunk_time_interval => INTERVAL '1 hour');
SELECT add_dimension('market_data.order_book_snapshots', 'symbol_id', number_partitions => 4);

-- =============================================================================
-- ANALYTICS TABLES
-- =============================================================================

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

    -- Momentum indicators
    rsi_14 DECIMAL(8,4),
    macd DECIMAL(20,6),
    macd_signal DECIMAL(20,6),
    macd_histogram DECIMAL(20,6),

    -- Volatility indicators
    bollinger_upper DECIMAL(20,6),
    bollinger_middle DECIMAL(20,6),
    bollinger_lower DECIMAL(20,6),
    atr_14 DECIMAL(20,6),

    -- Volume indicators
    volume_sma_20 BIGINT,
    volume_ratio DECIMAL(8,4),

    CHECK (timeframe IN ('1m', '5m', '15m', '1h', '4h', '1d'))
);

SELECT create_hypertable('analytics.technical_indicators', 'time', chunk_time_interval => INTERVAL '7 days');

-- Market statistics and analytics
CREATE TABLE analytics.market_stats (
    time TIMESTAMPTZ NOT NULL,
    calculation_type VARCHAR(50) NOT NULL,

    -- Market-wide statistics
    total_volume BIGINT,
    total_trades INTEGER,
    active_symbols INTEGER,

    -- Price movements
    gainers_count INTEGER,
    losers_count INTEGER,
    unchanged_count INTEGER,

    -- Volatility metrics
    market_volatility DECIMAL(8,4),
    average_spread DECIMAL(8,4),

    -- Additional metrics stored as JSON
    metrics JSONB,

    PRIMARY KEY (time, calculation_type)
);

SELECT create_hypertable('analytics.market_stats', 'time', chunk_time_interval => INTERVAL '1 day');

-- =============================================================================
-- AUDIT AND MONITORING TABLES
-- =============================================================================

-- Data quality metrics
CREATE TABLE audit.data_quality (
    time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    table_name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,6),
    details JSONB,

    PRIMARY KEY (time, table_name, metric_name)
);

SELECT create_hypertable('audit.data_quality', 'time', chunk_time_interval => INTERVAL '1 day');

-- =============================================================================
-- INDEXES FOR OPTIMAL QUERY PERFORMANCE
-- =============================================================================

-- Market ticks indexes
CREATE INDEX idx_market_ticks_symbol_time ON market_data.market_ticks (symbol_id, time DESC);
CREATE INDEX idx_market_ticks_trade_id ON market_data.market_ticks (trade_id) WHERE trade_id IS NOT NULL;
CREATE INDEX idx_market_ticks_price ON market_data.market_ticks (price, time) WHERE price IS NOT NULL;

-- OHLCV indexes
CREATE INDEX idx_ohlcv_1m_symbol_time ON market_data.ohlcv_1m (symbol_id, time DESC);
CREATE INDEX idx_ohlcv_5m_symbol_time ON market_data.ohlcv_5m (symbol_id, time DESC);
CREATE INDEX idx_ohlcv_daily_symbol_time ON market_data.ohlcv_daily (symbol_id, time DESC);

-- Order book indexes
CREATE INDEX idx_order_book_symbol_time ON market_data.order_book_snapshots (symbol_id, time DESC);
CREATE INDEX idx_order_book_spread ON market_data.order_book_snapshots (spread, time) WHERE spread IS NOT NULL;

-- Analytics indexes
CREATE INDEX idx_technical_indicators_symbol_timeframe ON analytics.technical_indicators (symbol_id, timeframe, time DESC);

-- =============================================================================
-- HELPER FUNCTIONS
-- =============================================================================

-- Function to get latest price for a symbol
CREATE OR REPLACE FUNCTION market_data.get_latest_price(p_symbol_id UUID)
RETURNS DECIMAL(20,6) AS $$
DECLARE
    latest_price DECIMAL(20,6);
BEGIN
    SELECT price INTO latest_price
    FROM market_data.market_ticks
    WHERE symbol_id = p_symbol_id
    ORDER BY time DESC
    LIMIT 1;

    RETURN latest_price;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate VWAP for a time period
CREATE OR REPLACE FUNCTION market_data.calculate_vwap(
    p_symbol_id UUID,
    p_start_time TIMESTAMPTZ,
    p_end_time TIMESTAMPTZ
)
RETURNS DECIMAL(20,6) AS $$
DECLARE
    vwap_result DECIMAL(20,6);
BEGIN
    SELECT
        SUM(price * quantity)::DECIMAL / SUM(quantity)::DECIMAL
    INTO vwap_result
    FROM market_data.market_ticks
    WHERE symbol_id = p_symbol_id
    AND time >= p_start_time
    AND time <= p_end_time;

    RETURN vwap_result;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- GRANT PERMISSIONS
-- =============================================================================

-- Grant table permissions to application user
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA market_data TO market_data_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA analytics TO market_data_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA reference TO market_data_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA audit TO market_data_user;

-- Grant sequence permissions
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA market_data TO market_data_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA analytics TO market_data_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA reference TO market_data_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA audit TO market_data_user;

-- Grant function permissions
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA market_data TO market_data_user;

-- Grant read-only permissions
GRANT SELECT ON ALL TABLES IN SCHEMA market_data TO market_data_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO market_data_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA reference TO market_data_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO market_data_readonly;

-- Grant function execute for read-only user
GRANT EXECUTE ON FUNCTION market_data.get_latest_price(UUID) TO market_data_readonly;
GRANT EXECUTE ON FUNCTION market_data.calculate_vwap(UUID, TIMESTAMPTZ, TIMESTAMPTZ) TO market_data_readonly;

-- =============================================================================
-- DEFAULT PRIVILEGES FOR FUTURE OBJECTS
-- =============================================================================

ALTER DEFAULT PRIVILEGES IN SCHEMA market_data GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO market_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO market_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA reference GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO market_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO market_data_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA market_data GRANT SELECT ON TABLES TO market_data_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT ON TABLES TO market_data_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA reference GRANT SELECT ON TABLES TO market_data_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO market_data_readonly;

-- =============================================================================
-- INSERT SAMPLE REFERENCE DATA
-- =============================================================================

-- Insert BIST exchange
INSERT INTO reference.exchanges (exchange_code, exchange_name, country_code, timezone, currency, trading_hours) VALUES
('BIST', 'Borsa Istanbul', 'TUR', 'Europe/Istanbul', 'TRY',
 '{"pre_market": {"start": "09:30", "end": "10:00"}, "main_session": {"start": "10:00", "end": "18:00"}, "post_market": {"start": "18:00", "end": "18:30"}}');

-- Get the exchange ID for subsequent inserts
DO $$
DECLARE
    bist_exchange_id UUID;
BEGIN
    SELECT exchange_id INTO bist_exchange_id FROM reference.exchanges WHERE exchange_code = 'BIST';

    -- Insert popular BIST symbols
    INSERT INTO reference.symbols (symbol, exchange_id, full_name, isin_code, sector, industry) VALUES
    ('GARAN', bist_exchange_id, 'Türkiye Garanti Bankası A.Ş.', 'TREGARAN00017', 'Banking', 'Commercial Banks'),
    ('THYAO', bist_exchange_id, 'Türk Hava Yolları A.O.', 'TRETHYAO00013', 'Transportation', 'Airlines'),
    ('AKBNK', bist_exchange_id, 'Akbank T.A.Ş.', 'TREAKBNK00015', 'Banking', 'Commercial Banks'),
    ('ISCTR', bist_exchange_id, 'Türkiye İş Bankası A.Ş.', 'TREISCTR00014', 'Banking', 'Commercial Banks'),
    ('ASELS', bist_exchange_id, 'Aselsan Elektronik Sanayi ve Ticaret A.Ş.', 'TREASELS00011', 'Technology', 'Defense Electronics'),
    ('KOZAL', bist_exchange_id, 'Koza Altın İşletmeleri A.Ş.', 'TREKOZAL00010', 'Mining', 'Gold Mining'),
    ('PETKM', bist_exchange_id, 'Petkim Petrokimya Holding A.Ş.', 'TREPETKM00014', 'Chemicals', 'Petrochemicals'),
    ('TUPRS', bist_exchange_id, 'Türkiye Petrol Rafinerileri A.Ş.', 'TRETUPRS00015', 'Energy', 'Oil Refining');
END $$;

-- =============================================================================
-- COMPLETION MESSAGE
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'TimescaleDB initialization completed successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Created hypertables:';
    RAISE NOTICE '  - market_data.market_ticks (1-hour chunks)';
    RAISE NOTICE '  - market_data.ohlcv_1m (1-day chunks)';
    RAISE NOTICE '  - market_data.ohlcv_5m (7-day chunks)';
    RAISE NOTICE '  - market_data.ohlcv_daily (30-day chunks)';
    RAISE NOTICE '  - market_data.order_book_snapshots (1-hour chunks)';
    RAISE NOTICE '  - analytics.technical_indicators (7-day chunks)';
    RAISE NOTICE '  - analytics.market_stats (1-day chunks)';
    RAISE NOTICE '  - audit.data_quality (1-day chunks)';
    RAISE NOTICE '';
    RAISE NOTICE 'Created users:';
    RAISE NOTICE '  - market_data_user: Full access for market data service';
    RAISE NOTICE '  - market_data_readonly: Read-only access for analytics';
    RAISE NOTICE '  - replicator: Replication user';
    RAISE NOTICE '';
    RAISE NOTICE 'Inserted reference data for BIST exchange and 8 popular symbols';
    RAISE NOTICE '=============================================================================';
END $$;