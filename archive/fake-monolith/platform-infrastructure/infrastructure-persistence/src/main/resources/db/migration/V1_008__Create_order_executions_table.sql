-- Create order_executions table for tracking partial fills and execution details
-- This table links to orders table and tracks all execution events

CREATE TABLE order_executions (
    id BIGSERIAL PRIMARY KEY,

    -- Link to parent order
    order_id BIGINT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    -- Execution Details
    execution_id VARCHAR(100) UNIQUE, -- Broker/exchange execution ID
    external_execution_id VARCHAR(100), -- External system execution ID

    -- Fill Information
    executed_quantity INTEGER NOT NULL CHECK (executed_quantity > 0),
    execution_price DECIMAL(15,6) NOT NULL CHECK (execution_price > 0),
    execution_value DECIMAL(20,6) GENERATED ALWAYS AS (executed_quantity * execution_price) STORED,

    -- Timing
    execution_time TIMESTAMP WITH TIME ZONE NOT NULL,
    trade_date DATE NOT NULL, -- Settlement date
    value_date DATE, -- Value date for settlement

    -- Counterparty Information
    counterparty_id VARCHAR(100), -- Counterparty identifier
    counterparty_type VARCHAR(20), -- CLIENT, MARKET_MAKER, PROPRIETARY

    -- Commission and Fees
    commission_amount DECIMAL(15,6) DEFAULT 0,
    commission_rate DECIMAL(8,6), -- Commission rate applied
    exchange_fee DECIMAL(15,6) DEFAULT 0,
    clearing_fee DECIMAL(15,6) DEFAULT 0,
    other_fees DECIMAL(15,6) DEFAULT 0,
    total_fees DECIMAL(15,6) GENERATED ALWAYS AS (
        COALESCE(commission_amount, 0) +
        COALESCE(exchange_fee, 0) +
        COALESCE(clearing_fee, 0) +
        COALESCE(other_fees, 0)
    ) STORED,

    -- Net Settlement Amount
    net_amount DECIMAL(20,6) GENERATED ALWAYS AS (
        execution_value - total_fees
    ) STORED,

    -- Execution Context
    execution_venue VARCHAR(50), -- BIST, OTC, DARK_POOL
    execution_type VARCHAR(30) DEFAULT 'MARKET', -- MARKET, LIMIT, STOP, ICEBERG
    liquidity_flag VARCHAR(10), -- MAKER, TAKER, UNKNOWN

    -- Market Data Context
    best_bid_price DECIMAL(15,6), -- Market best bid at execution
    best_ask_price DECIMAL(15,6), -- Market best ask at execution
    last_price DECIMAL(15,6), -- Last traded price before execution
    market_spread DECIMAL(15,6), -- Bid-ask spread at execution time

    -- Order Book Context
    order_book_side VARCHAR(4) CHECK (order_book_side IN ('BID', 'ASK')),
    order_book_level INTEGER, -- Level in order book where filled

    -- Settlement Information
    settlement_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SETTLED, FAILED
    settlement_date DATE,
    settlement_reference VARCHAR(100),

    -- Partial Fill Tracking
    is_partial_fill BOOLEAN DEFAULT false,
    remaining_quantity INTEGER,
    fill_sequence INTEGER, -- Sequence number for this order's fills

    -- Risk and Compliance
    risk_check_passed BOOLEAN DEFAULT true,
    compliance_flags JSONB DEFAULT '[]'::JSONB,

    -- Source and Audit
    execution_source VARCHAR(50) DEFAULT 'ALGOLAB', -- ALGOLAB, MANUAL, API
    reported_by VARCHAR(100), -- User or system that reported execution

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Additional execution context as JSON
    execution_context JSONB DEFAULT '{}'::JSONB
);

-- Performance indexes
CREATE INDEX idx_order_executions_order_id ON order_executions(order_id);
CREATE INDEX idx_order_executions_execution_time ON order_executions(execution_time);
CREATE INDEX idx_order_executions_trade_date ON order_executions(trade_date);
CREATE INDEX idx_order_executions_venue ON order_executions(execution_venue);
CREATE INDEX idx_order_executions_settlement_status ON order_executions(settlement_status);
CREATE INDEX idx_order_executions_external_id ON order_executions(external_execution_id);

-- Composite indexes for common queries
CREATE INDEX idx_order_executions_order_sequence ON order_executions(order_id, fill_sequence);
CREATE INDEX idx_order_executions_date_venue ON order_executions(trade_date, execution_venue);

-- Update trigger
CREATE OR REPLACE FUNCTION update_order_executions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_order_executions_updated_at
    BEFORE UPDATE ON order_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_order_executions_updated_at();

-- Function to auto-update fill sequence
CREATE OR REPLACE FUNCTION set_fill_sequence()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto-increment fill sequence for the order
    SELECT COALESCE(MAX(fill_sequence), 0) + 1
    INTO NEW.fill_sequence
    FROM order_executions
    WHERE order_id = NEW.order_id;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_set_fill_sequence
    BEFORE INSERT ON order_executions
    FOR EACH ROW
    EXECUTE FUNCTION set_fill_sequence();

-- Comments
COMMENT ON TABLE order_executions IS 'Tracks all order executions and partial fills';
COMMENT ON COLUMN order_executions.execution_id IS 'Unique execution identifier from broker/exchange';
COMMENT ON COLUMN order_executions.executed_quantity IS 'Quantity filled in this execution';
COMMENT ON COLUMN order_executions.execution_price IS 'Price at which execution occurred';
COMMENT ON COLUMN order_executions.liquidity_flag IS 'Whether this execution added or removed liquidity';
COMMENT ON COLUMN order_executions.fill_sequence IS 'Sequential number for fills of the same order';
COMMENT ON COLUMN order_executions.execution_context IS 'Additional execution metadata as JSON';