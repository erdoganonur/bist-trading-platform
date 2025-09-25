-- Create symbols master table for BIST Trading Platform
-- This table contains all tradeable symbols with their specifications

CREATE TABLE symbols (
    id BIGSERIAL PRIMARY KEY,

    -- Basic Symbol Information
    symbol_code VARCHAR(20) NOT NULL UNIQUE, -- e.g., 'THYAO', 'ISCTR'
    symbol_name VARCHAR(255) NOT NULL, -- e.g., 'Türk Hava Yolları A.O.'
    isin_code VARCHAR(12) UNIQUE, -- International Securities Identification Number

    -- Market Classification
    exchange_code VARCHAR(10) NOT NULL DEFAULT 'BIST', -- BIST, VIOP, etc.
    market_segment VARCHAR(50) NOT NULL, -- ANA PAZAR, YILDIZ PAZAR, GELIŞEN İŞLETMELER PAZARI
    sub_market VARCHAR(50), -- ALT PAZAR classifications

    -- Trading Specifications
    lot_size INTEGER NOT NULL DEFAULT 1, -- Minimum trading unit
    tick_size DECIMAL(10,6) NOT NULL DEFAULT 0.01, -- Minimum price increment
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Price Limits and Circuit Breakers
    min_price DECIMAL(15,6), -- Daily minimum price
    max_price DECIMAL(15,6), -- Daily maximum price
    circuit_breaker_percentage DECIMAL(5,2) DEFAULT 10.0, -- % for circuit breaker

    -- Trading Hours and Status
    trading_start_time TIME DEFAULT '09:30:00',
    trading_end_time TIME DEFAULT '18:00:00',
    is_tradeable BOOLEAN NOT NULL DEFAULT true,
    trading_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, HALTED

    -- Corporate Information
    sector_code VARCHAR(50), -- Sector classification
    industry_code VARCHAR(50), -- Industry classification
    company_name VARCHAR(500), -- Full company name

    -- Index Memberships (stored as JSON array)
    index_memberships JSONB DEFAULT '[]'::JSONB, -- e.g., ["BIST100", "BIST30"]

    -- Market Making and Liquidity
    market_maker_eligible BOOLEAN DEFAULT false,
    continuous_trading BOOLEAN DEFAULT true,
    call_auction_eligible BOOLEAN DEFAULT true,

    -- Risk Parameters
    position_limit BIGINT, -- Maximum position size
    daily_volume_limit BIGINT, -- Maximum daily volume

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Data source tracking
    data_source VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, BIST_API, BLOOMBERG
    last_sync_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for performance
CREATE INDEX idx_symbols_code ON symbols(symbol_code);
CREATE INDEX idx_symbols_isin ON symbols(isin_code);
CREATE INDEX idx_symbols_market ON symbols(market_segment);
CREATE INDEX idx_symbols_sector ON symbols(sector_code);
CREATE INDEX idx_symbols_tradeable ON symbols(is_tradeable);
CREATE INDEX idx_symbols_status ON symbols(trading_status);

-- GIN index for JSON index memberships
CREATE INDEX idx_symbols_index_memberships ON symbols USING GIN(index_memberships);

-- Update trigger
CREATE OR REPLACE FUNCTION update_symbols_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_symbols_updated_at
    BEFORE UPDATE ON symbols
    FOR EACH ROW
    EXECUTE FUNCTION update_symbols_updated_at();

-- Comments
COMMENT ON TABLE symbols IS 'Master table for all tradeable symbols in BIST';
COMMENT ON COLUMN symbols.symbol_code IS 'Unique symbol code (e.g., THYAO, ISCTR)';
COMMENT ON COLUMN symbols.lot_size IS 'Minimum trading unit';
COMMENT ON COLUMN symbols.tick_size IS 'Minimum price increment';
COMMENT ON COLUMN symbols.circuit_breaker_percentage IS 'Circuit breaker trigger percentage';
COMMENT ON COLUMN symbols.index_memberships IS 'JSON array of index codes this symbol belongs to';