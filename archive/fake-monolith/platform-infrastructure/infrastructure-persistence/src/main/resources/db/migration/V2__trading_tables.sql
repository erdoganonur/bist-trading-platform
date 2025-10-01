-- =============================================================================
-- BIST Trading Platform - Trading Domain Tables
-- Version: V2 - Portfolio, Orders, Positions, and Trading History
-- =============================================================================

-- =============================================================================
-- PORTFOLIO MANAGEMENT TABLES
-- =============================================================================

-- User portfolios
CREATE TABLE trading.portfolios (
    portfolio_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_management.users(user_id),

    -- Portfolio details
    portfolio_name VARCHAR(100) NOT NULL,
    portfolio_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    base_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Status and configuration
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_margin_enabled BOOLEAN NOT NULL DEFAULT false,
    max_leverage DECIMAL(5,2) DEFAULT 1.00,

    -- Risk management
    max_daily_loss DECIMAL(20,2),
    max_position_size DECIMAL(20,2),
    risk_tolerance VARCHAR(20) DEFAULT 'MODERATE',

    -- Performance tracking
    initial_balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    current_value DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    realized_pnl DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    unrealized_pnl DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    total_fees_paid DECIMAL(20,2) NOT NULL DEFAULT 0.00,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_portfolio_type CHECK (portfolio_type IN ('STANDARD', 'MARGIN', 'DAY_TRADING', 'SWING_TRADING')),
    CONSTRAINT chk_base_currency CHECK (LENGTH(base_currency) = 3),
    CONSTRAINT chk_max_leverage CHECK (max_leverage >= 1.00 AND max_leverage <= 10.00),
    CONSTRAINT chk_risk_tolerance CHECK (risk_tolerance IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    CONSTRAINT chk_initial_balance CHECK (initial_balance >= 0),
    CONSTRAINT chk_current_value CHECK (current_value >= 0),
    CONSTRAINT uk_user_portfolio_name UNIQUE (user_id, portfolio_name)
);

-- Indexes for portfolios
CREATE INDEX idx_portfolios_user ON trading.portfolios (user_id) WHERE is_active = true;
CREATE INDEX idx_portfolios_type ON trading.portfolios (portfolio_type);
CREATE INDEX idx_portfolios_active ON trading.portfolios (is_active);
CREATE INDEX idx_portfolios_updated ON trading.portfolios (updated_at);

-- Portfolio holdings (positions)
CREATE TABLE trading.positions (
    position_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id UUID NOT NULL REFERENCES trading.portfolios(portfolio_id),
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Position details
    position_type VARCHAR(10) NOT NULL DEFAULT 'LONG',
    quantity DECIMAL(20,6) NOT NULL,
    average_price DECIMAL(20,6) NOT NULL,
    current_price DECIMAL(20,6),

    -- Financial calculations
    market_value DECIMAL(20,2) GENERATED ALWAYS AS (quantity * current_price) STORED,
    cost_basis DECIMAL(20,2) GENERATED ALWAYS AS (quantity * average_price) STORED,
    unrealized_pnl DECIMAL(20,2) GENERATED ALWAYS AS ((current_price - average_price) * quantity) STORED,
    unrealized_pnl_percent DECIMAL(10,4) GENERATED ALWAYS AS (
        CASE WHEN average_price > 0
        THEN ((current_price - average_price) / average_price) * 100
        ELSE 0 END
    ) STORED,

    -- Position status
    is_active BOOLEAN NOT NULL DEFAULT true,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_position_type CHECK (position_type IN ('LONG', 'SHORT')),
    CONSTRAINT chk_quantity CHECK (quantity != 0),
    CONSTRAINT chk_average_price CHECK (average_price > 0),
    CONSTRAINT chk_current_price CHECK (current_price IS NULL OR current_price > 0),
    CONSTRAINT chk_closed_position CHECK (
        (is_active = true AND closed_at IS NULL) OR
        (is_active = false AND closed_at IS NOT NULL)
    ),
    CONSTRAINT uk_portfolio_symbol_active UNIQUE (portfolio_id, symbol_id, is_active)
        DEFERRABLE INITIALLY DEFERRED
);

-- Indexes for positions
CREATE INDEX idx_positions_portfolio ON trading.positions (portfolio_id);
CREATE INDEX idx_positions_symbol ON trading.positions (symbol_id);
CREATE INDEX idx_positions_active ON trading.positions (is_active) WHERE is_active = true;
CREATE INDEX idx_positions_opened_at ON trading.positions (opened_at);
CREATE INDEX idx_positions_unrealized_pnl ON trading.positions (unrealized_pnl);

-- =============================================================================
-- ORDER MANAGEMENT TABLES
-- =============================================================================

-- Trading orders
CREATE TABLE trading.orders (
    order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id UUID NOT NULL REFERENCES trading.portfolios(portfolio_id),
    symbol_id UUID NOT NULL REFERENCES reference.symbols(symbol_id),

    -- Order identification
    client_order_id VARCHAR(50),
    broker_order_id VARCHAR(50),
    parent_order_id UUID REFERENCES trading.orders(order_id),

    -- Order details
    order_type VARCHAR(20) NOT NULL,
    side VARCHAR(4) NOT NULL,
    quantity DECIMAL(20,6) NOT NULL,
    price DECIMAL(20,6),
    stop_price DECIMAL(20,6),

    -- Order parameters
    time_in_force VARCHAR(10) NOT NULL DEFAULT 'DAY',
    instruction VARCHAR(20),

    -- Order status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filled_quantity DECIMAL(20,6) NOT NULL DEFAULT 0,
    remaining_quantity DECIMAL(20,6) GENERATED ALWAYS AS (quantity - filled_quantity) STORED,
    average_fill_price DECIMAL(20,6),

    -- Financial details
    total_cost DECIMAL(20,2),
    commission DECIMAL(20,2) NOT NULL DEFAULT 0,
    fees DECIMAL(20,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(20,2),

    -- Risk management
    risk_category VARCHAR(20) DEFAULT 'NORMAL',
    pre_trade_risk_check BOOLEAN NOT NULL DEFAULT false,

    -- Timing
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    filled_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Error handling
    rejection_reason TEXT,
    cancellation_reason TEXT,

    -- Metadata
    additional_info JSONB DEFAULT '{}'::jsonb,

    -- Constraints
    CONSTRAINT chk_order_type CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT', 'TRAILING_STOP')),
    CONSTRAINT chk_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_time_in_force CHECK (time_in_force IN ('DAY', 'GTC', 'IOC', 'FOK')),
    CONSTRAINT chk_instruction CHECK (instruction IS NULL OR instruction IN ('OPEN', 'CLOSE', 'INCREASE', 'DECREASE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_risk_category CHECK (risk_category IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_price CHECK (price IS NULL OR price > 0),
    CONSTRAINT chk_stop_price CHECK (stop_price IS NULL OR stop_price > 0),
    CONSTRAINT chk_filled_quantity CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    CONSTRAINT chk_average_fill_price CHECK (average_fill_price IS NULL OR average_fill_price > 0),
    CONSTRAINT chk_limit_order_price CHECK (order_type != 'LIMIT' OR price IS NOT NULL),
    CONSTRAINT chk_stop_order_price CHECK (order_type NOT IN ('STOP', 'STOP_LIMIT') OR stop_price IS NOT NULL),
    CONSTRAINT chk_timing_sequence CHECK (
        submitted_at IS NULL OR submitted_at >= created_at
    )
);

-- Indexes for orders
CREATE INDEX idx_orders_portfolio ON trading.orders (portfolio_id);
CREATE INDEX idx_orders_symbol ON trading.orders (symbol_id);
CREATE INDEX idx_orders_status ON trading.orders (status);
CREATE INDEX idx_orders_side ON trading.orders (side);
CREATE INDEX idx_orders_created_at ON trading.orders (created_at);
CREATE INDEX idx_orders_client_id ON trading.orders (client_order_id) WHERE client_order_id IS NOT NULL;
CREATE INDEX idx_orders_broker_id ON trading.orders (broker_order_id) WHERE broker_order_id IS NOT NULL;
CREATE INDEX idx_orders_parent ON trading.orders (parent_order_id) WHERE parent_order_id IS NOT NULL;
CREATE INDEX idx_orders_active ON trading.orders (status) WHERE status IN ('PENDING', 'SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED');

-- Order executions (fills)
CREATE TABLE trading.executions (
    execution_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES trading.orders(order_id),

    -- Execution details
    execution_price DECIMAL(20,6) NOT NULL,
    execution_quantity DECIMAL(20,6) NOT NULL,
    execution_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Broker details
    broker_execution_id VARCHAR(50),
    counterparty VARCHAR(100),

    -- Financial details
    gross_amount DECIMAL(20,2) GENERATED ALWAYS AS (execution_price * execution_quantity) STORED,
    commission DECIMAL(20,2) NOT NULL DEFAULT 0,
    fees DECIMAL(20,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(20,2) GENERATED ALWAYS AS (
        (execution_price * execution_quantity) - commission - fees
    ) STORED,

    -- Settlement
    settlement_date DATE NOT NULL DEFAULT CURRENT_DATE + 2,
    settlement_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Metadata
    additional_info JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_execution_price CHECK (execution_price > 0),
    CONSTRAINT chk_execution_quantity CHECK (execution_quantity > 0),
    CONSTRAINT chk_settlement_currency CHECK (LENGTH(settlement_currency) = 3),
    CONSTRAINT chk_settlement_date CHECK (settlement_date >= CURRENT_DATE)
);

-- Indexes for executions
CREATE INDEX idx_executions_order ON trading.executions (order_id);
CREATE INDEX idx_executions_time ON trading.executions (execution_time);
CREATE INDEX idx_executions_settlement ON trading.executions (settlement_date);
CREATE INDEX idx_executions_broker_id ON trading.executions (broker_execution_id) WHERE broker_execution_id IS NOT NULL;

-- =============================================================================
-- TRANSACTION AND CASH FLOW TABLES
-- =============================================================================

-- Portfolio transactions
CREATE TABLE trading.transactions (
    transaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id UUID NOT NULL REFERENCES trading.portfolios(portfolio_id),
    execution_id UUID REFERENCES trading.executions(execution_id),

    -- Transaction details
    transaction_type VARCHAR(20) NOT NULL,
    symbol_id UUID REFERENCES reference.symbols(symbol_id),
    quantity DECIMAL(20,6),
    price DECIMAL(20,6),

    -- Financial impact
    amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Cash flow impact
    cash_impact DECIMAL(20,2) NOT NULL,
    position_impact JSONB,

    -- Status and timing
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    settlement_date DATE NOT NULL,
    value_date DATE NOT NULL,

    -- References
    reference_number VARCHAR(50),
    description TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('BUY', 'SELL', 'DEPOSIT', 'WITHDRAWAL', 'DIVIDEND', 'INTEREST', 'FEE', 'ADJUSTMENT')),
    CONSTRAINT chk_transaction_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'SETTLED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_trade_transaction CHECK (
        (transaction_type IN ('BUY', 'SELL') AND symbol_id IS NOT NULL AND quantity IS NOT NULL AND price IS NOT NULL) OR
        (transaction_type NOT IN ('BUY', 'SELL') AND (symbol_id IS NULL OR quantity IS NULL OR price IS NULL))
    ),
    CONSTRAINT chk_settlement_value_dates CHECK (settlement_date >= transaction_date AND value_date >= transaction_date)
);

-- Indexes for transactions
CREATE INDEX idx_transactions_portfolio ON trading.transactions (portfolio_id);
CREATE INDEX idx_transactions_execution ON trading.transactions (execution_id);
CREATE INDEX idx_transactions_type ON trading.transactions (transaction_type);
CREATE INDEX idx_transactions_date ON trading.transactions (transaction_date);
CREATE INDEX idx_transactions_settlement ON trading.transactions (settlement_date);
CREATE INDEX idx_transactions_status ON trading.transactions (status);
CREATE INDEX idx_transactions_symbol ON trading.transactions (symbol_id) WHERE symbol_id IS NOT NULL;

-- Cash balances
CREATE TABLE trading.cash_balances (
    balance_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id UUID NOT NULL REFERENCES trading.portfolios(portfolio_id),
    currency VARCHAR(3) NOT NULL,

    -- Balance details
    available_balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    total_balance DECIMAL(20,2) GENERATED ALWAYS AS (available_balance + reserved_balance) STORED,

    -- Credit facilities
    credit_limit DECIMAL(20,2) DEFAULT 0.00,
    used_credit DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    available_credit DECIMAL(20,2) GENERATED ALWAYS AS (GREATEST(credit_limit - used_credit, 0)) STORED,

    -- Interest tracking
    accrued_interest DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    last_interest_date DATE,

    -- Metadata
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_balance_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_reserved_balance CHECK (reserved_balance >= 0),
    CONSTRAINT chk_credit_limit CHECK (credit_limit >= 0),
    CONSTRAINT chk_used_credit CHECK (used_credit >= 0 AND used_credit <= COALESCE(credit_limit, used_credit)),
    CONSTRAINT uk_portfolio_currency UNIQUE (portfolio_id, currency)
);

-- Indexes for cash balances
CREATE INDEX idx_cash_balances_portfolio ON trading.cash_balances (portfolio_id);
CREATE INDEX idx_cash_balances_currency ON trading.cash_balances (currency);
CREATE INDEX idx_cash_balances_updated ON trading.cash_balances (updated_at);

-- =============================================================================
-- PERFORMANCE AND ANALYTICS TABLES
-- =============================================================================

-- Portfolio performance snapshots
CREATE TABLE trading.portfolio_snapshots (
    snapshot_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id UUID NOT NULL REFERENCES trading.portfolios(portfolio_id),

    -- Snapshot details
    snapshot_date DATE NOT NULL,
    snapshot_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Portfolio values
    total_value DECIMAL(20,2) NOT NULL,
    cash_value DECIMAL(20,2) NOT NULL,
    positions_value DECIMAL(20,2) NOT NULL,

    -- Performance metrics
    daily_pnl DECIMAL(20,2),
    daily_pnl_percent DECIMAL(10,4),
    total_pnl DECIMAL(20,2),
    total_pnl_percent DECIMAL(10,4),

    -- Risk metrics
    var_1day DECIMAL(20,2),
    beta DECIMAL(10,6),
    sharpe_ratio DECIMAL(10,6),
    max_drawdown DECIMAL(10,4),

    -- Activity metrics
    trades_count INTEGER DEFAULT 0,
    volume_traded DECIMAL(20,2) DEFAULT 0,
    fees_paid DECIMAL(20,2) DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_snapshot_date CHECK (snapshot_date <= CURRENT_DATE),
    CONSTRAINT chk_total_value CHECK (total_value >= 0),
    CONSTRAINT chk_cash_value CHECK (cash_value >= 0),
    CONSTRAINT chk_positions_value CHECK (positions_value >= 0),
    CONSTRAINT chk_trades_count CHECK (trades_count >= 0),
    CONSTRAINT chk_volume_traded CHECK (volume_traded >= 0),
    CONSTRAINT chk_fees_paid CHECK (fees_paid >= 0),
    CONSTRAINT uk_portfolio_snapshot_date UNIQUE (portfolio_id, snapshot_date)
);

-- Indexes for portfolio snapshots
CREATE INDEX idx_portfolio_snapshots_portfolio ON trading.portfolio_snapshots (portfolio_id);
CREATE INDEX idx_portfolio_snapshots_date ON trading.portfolio_snapshots (snapshot_date);
CREATE INDEX idx_portfolio_snapshots_time ON trading.portfolio_snapshots (snapshot_time);

-- =============================================================================
-- TRIGGERS FOR UPDATED_AT
-- =============================================================================

CREATE TRIGGER trigger_portfolios_updated_at BEFORE UPDATE ON trading.portfolios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_positions_updated_at BEFORE UPDATE ON trading.positions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_orders_updated_at BEFORE UPDATE ON trading.orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_transactions_updated_at BEFORE UPDATE ON trading.transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_cash_balances_updated_at BEFORE UPDATE ON trading.cash_balances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- COMMENTS ON TABLES
-- =============================================================================

COMMENT ON TABLE trading.portfolios IS 'User trading portfolios with risk management settings';
COMMENT ON TABLE trading.positions IS 'Current and historical position holdings';
COMMENT ON TABLE trading.orders IS 'Trading orders with full lifecycle tracking';
COMMENT ON TABLE trading.executions IS 'Order execution details and fills';
COMMENT ON TABLE trading.transactions IS 'All portfolio transactions and cash flows';
COMMENT ON TABLE trading.cash_balances IS 'Portfolio cash balances by currency';
COMMENT ON TABLE trading.portfolio_snapshots IS 'Daily portfolio performance snapshots';

-- Column comments
COMMENT ON COLUMN trading.positions.quantity IS 'Position size (positive for long, negative for short)';
COMMENT ON COLUMN trading.positions.unrealized_pnl IS 'Calculated unrealized profit/loss';
COMMENT ON COLUMN trading.orders.time_in_force IS 'Order validity period (DAY, GTC, IOC, FOK)';
COMMENT ON COLUMN trading.orders.instruction IS 'Position instruction (OPEN, CLOSE, INCREASE, DECREASE)';
COMMENT ON COLUMN trading.cash_balances.reserved_balance IS 'Cash reserved for pending orders';

-- =============================================================================
-- TRADING TABLES MIGRATION COMPLETE
-- =============================================================================