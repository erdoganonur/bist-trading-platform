-- =============================================================================
-- BIST Trading Platform - Trading Domain Schema
-- Version: 2.0.0
-- Description: Comprehensive trading domain with orders, positions, and symbols
-- =============================================================================

-- =============================================================================
-- ENUM TYPES FOR TRADING DOMAIN
-- =============================================================================

-- Order type enumeration
CREATE TYPE order_type AS ENUM (
    'MARKET',                -- Market order - immediate execution at best price
    'LIMIT',                 -- Limit order - execute at specified price or better
    'STOP',                  -- Stop order - becomes market order when trigger hit
    'STOP_LIMIT',            -- Stop-limit order - becomes limit when trigger hit
    'TRAILING_STOP',         -- Trailing stop order
    'ICEBERG',               -- Iceberg order - hidden quantity
    'BRACKET',               -- Bracket order (parent with profit target and stop loss)
    'OCO'                    -- One-cancels-other order
);

-- Order side enumeration
CREATE TYPE order_side AS ENUM (
    'BUY',                   -- Buy order
    'SELL'                   -- Sell order
);

-- Time in force enumeration
CREATE TYPE time_in_force AS ENUM (
    'DAY',                   -- Valid for trading day
    'GTC',                   -- Good till cancelled
    'IOC',                   -- Immediate or cancel
    'FOK',                   -- Fill or kill
    'GTD',                   -- Good till date
    'OPG',                   -- At the opening
    'CLS'                    -- At the close
);

-- Order status enumeration
CREATE TYPE order_status AS ENUM (
    'PENDING',               -- Order created, not yet submitted
    'SUBMITTED',             -- Submitted to broker
    'ACCEPTED',              -- Accepted by exchange
    'REJECTED',              -- Rejected by broker/exchange
    'PARTIALLY_FILLED',      -- Partially executed
    'FILLED',                -- Completely filled
    'CANCELLED',             -- Cancelled by user/system
    'REPLACED',              -- Order was replaced (modified)
    'SUSPENDED',             -- Suspended (e.g., market halt)
    'EXPIRED',               -- Expired (GTD orders)
    'ERROR'                  -- System error
);

-- Position side enumeration
CREATE TYPE position_side AS ENUM (
    'LONG',                  -- Long position
    'SHORT',                 -- Short position
    'FLAT'                   -- No position
);

-- Market type enumeration
CREATE TYPE market_type AS ENUM (
    'EQUITY',                -- Stock market
    'BOND',                  -- Bond market
    'DERIVATIVE',            -- Futures/Options
    'CURRENCY',              -- FX market
    'COMMODITY',             -- Commodity market
    'CRYPTO'                 -- Cryptocurrency
);

-- Symbol status enumeration
CREATE TYPE symbol_status AS ENUM (
    'ACTIVE',                -- Actively trading
    'SUSPENDED',             -- Trading suspended
    'HALTED',                -- Trading halted
    'DELISTED',              -- Delisted symbol
    'PRE_TRADING',           -- Pre-market trading
    'POST_TRADING'           -- After-hours trading
);

-- Execution type enumeration
CREATE TYPE execution_type AS ENUM (
    'NEW',                   -- New order execution
    'PARTIAL_FILL',          -- Partial fill
    'FILL',                  -- Complete fill
    'DONE_FOR_DAY',          -- Order done for day
    'CANCELED',              -- Order canceled
    'REPLACED',              -- Order replaced
    'PENDING_CANCEL',        -- Pending cancellation
    'STOPPED',               -- Order stopped
    'REJECTED',              -- Order rejected
    'SUSPENDED',             -- Order suspended
    'PENDING_NEW',           -- Pending new order
    'CALCULATED',            -- Calculated price
    'EXPIRED',               -- Order expired
    'RESTATED',              -- Order restated
    'PENDING_REPLACE',       -- Pending order replacement
    'TRADE',                 -- Trade execution
    'TRADE_CORRECT',         -- Trade correction
    'TRADE_CANCEL',          -- Trade cancellation
    'ORDER_STATUS'           -- Order status update
);

-- =============================================================================
-- TABLE: symbols
-- =============================================================================

CREATE TABLE symbols (
    -- Primary identification
    symbol_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Symbol information
    symbol VARCHAR(20) NOT NULL UNIQUE,              -- GARAN, THYAO, etc.
    name VARCHAR(255) NOT NULL,                      -- Company/instrument name
    local_name VARCHAR(255),                         -- Turkish name
    isin_code VARCHAR(12),                           -- International Securities ID

    -- Market information
    market_type market_type NOT NULL DEFAULT 'EQUITY',
    exchange VARCHAR(50) NOT NULL DEFAULT 'BIST',    -- BIST, VIOP, etc.
    sector VARCHAR(100),                             -- Banking, Technology, etc.
    industry VARCHAR(100),                           -- More specific classification

    -- Trading specifications
    lot_size INTEGER NOT NULL DEFAULT 1,            -- Minimum trading unit
    tick_size DECIMAL(10,6) NOT NULL DEFAULT 0.01,  -- Minimum price increment
    min_order_quantity INTEGER NOT NULL DEFAULT 1,   -- Minimum order size
    max_order_quantity INTEGER,                      -- Maximum order size (if any)

    -- Price limits (BIST specific)
    ceiling_price DECIMAL(20,6),                    -- Daily upper limit
    floor_price DECIMAL(20,6),                      -- Daily lower limit
    reference_price DECIMAL(20,6),                  -- Reference price for limits

    -- Market data
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TRY', -- TRY, USD, EUR
    market_cap DECIMAL(20,2),                        -- Market capitalization
    free_float_ratio DECIMAL(5,4),                   -- Free float percentage

    -- Index memberships (JSONB for flexibility)
    index_memberships JSONB NOT NULL DEFAULT '[]',   -- Array of index codes

    -- Corporate actions tracking
    dividend_yield DECIMAL(8,4),                     -- Annual dividend yield
    last_dividend_date DATE,                         -- Last dividend payment
    split_ratio DECIMAL(10,6),                       -- Stock split ratio

    -- Trading status and metadata
    symbol_status symbol_status NOT NULL DEFAULT 'ACTIVE',
    trading_start_time TIME,                         -- Daily trading start
    trading_end_time TIME,                           -- Daily trading end

    -- Additional metadata (JSONB for flexibility)
    metadata JSONB NOT NULL DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,  -- Soft delete

    -- Constraints
    CONSTRAINT symbols_lot_size_positive CHECK (lot_size > 0),
    CONSTRAINT symbols_tick_size_positive CHECK (tick_size > 0),
    CONSTRAINT symbols_min_qty_positive CHECK (min_order_quantity > 0),
    CONSTRAINT symbols_max_qty_valid CHECK (max_order_quantity IS NULL OR max_order_quantity >= min_order_quantity),
    CONSTRAINT symbols_price_limits_valid CHECK (
        ceiling_price IS NULL OR floor_price IS NULL OR ceiling_price >= floor_price
    ),
    CONSTRAINT symbols_free_float_valid CHECK (
        free_float_ratio IS NULL OR (free_float_ratio >= 0 AND free_float_ratio <= 1)
    )
);

-- =============================================================================
-- TABLE: orders (with monthly partitioning)
-- =============================================================================

CREATE TABLE orders (
    -- Primary identification
    order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_order_id VARCHAR(50),                    -- Client-provided ID
    parent_order_id UUID,                           -- For bracket/OCO orders

    -- User and account information
    user_id UUID NOT NULL REFERENCES users(user_id),
    organization_id UUID REFERENCES organizations(organization_id),
    account_id VARCHAR(50) NOT NULL,                -- Broker account ID
    sub_account_id VARCHAR(50),                     -- Sub-account if applicable

    -- Order details
    symbol_id UUID NOT NULL REFERENCES symbols(symbol_id),
    order_type order_type NOT NULL,
    order_side order_side NOT NULL,
    time_in_force time_in_force NOT NULL DEFAULT 'DAY',

    -- Quantity and pricing
    quantity INTEGER NOT NULL,                       -- Order quantity
    price DECIMAL(20,6),                            -- Limit price (if applicable)
    stop_price DECIMAL(20,6),                       -- Stop price (if applicable)
    trigger_price DECIMAL(20,6),                    -- Trigger price for complex orders

    -- Execution tracking
    filled_quantity INTEGER NOT NULL DEFAULT 0,     -- Quantity filled
    remaining_quantity INTEGER NOT NULL,            -- Quantity remaining
    average_fill_price DECIMAL(20,6),              -- Average execution price
    last_fill_price DECIMAL(20,6),                 -- Last execution price
    last_fill_quantity INTEGER,                     -- Last execution quantity

    -- Order lifecycle
    order_status order_status NOT NULL DEFAULT 'PENDING',
    status_reason VARCHAR(255),                     -- Reason for status change
    submitted_at TIMESTAMP WITH TIME ZONE,          -- When submitted to broker
    accepted_at TIMESTAMP WITH TIME ZONE,           -- When accepted by exchange
    first_fill_at TIMESTAMP WITH TIME ZONE,        -- First execution time
    last_fill_at TIMESTAMP WITH TIME ZONE,         -- Last execution time
    completed_at TIMESTAMP WITH TIME ZONE,          -- When order completed
    expires_at TIMESTAMP WITH TIME ZONE,            -- Expiry for GTD orders

    -- Broker integration
    broker_id VARCHAR(50),                          -- Broker identifier
    broker_order_id VARCHAR(100),                   -- Broker's order ID
    exchange_order_id VARCHAR(100),                 -- Exchange order ID

    -- Financial calculations
    commission DECIMAL(10,4) NOT NULL DEFAULT 0,    -- Commission charged
    brokerage_fee DECIMAL(10,4) NOT NULL DEFAULT 0, -- Brokerage fee
    exchange_fee DECIMAL(10,4) NOT NULL DEFAULT 0,  -- Exchange fee
    tax_amount DECIMAL(10,4) NOT NULL DEFAULT 0,    -- Tax amount
    total_cost DECIMAL(20,6),                       -- Total order cost

    -- Risk management
    risk_check_passed BOOLEAN NOT NULL DEFAULT FALSE,
    risk_rejection_reason VARCHAR(255),
    buying_power_check BOOLEAN NOT NULL DEFAULT FALSE,
    position_limit_check BOOLEAN NOT NULL DEFAULT FALSE,

    -- Strategy linkage
    strategy_id VARCHAR(100),                       -- Trading strategy ID
    algo_id VARCHAR(100),                          -- Algorithm ID

    -- Additional metadata and error handling
    metadata JSONB NOT NULL DEFAULT '{}',          -- Flexible metadata
    error_message TEXT,                            -- Error details
    retry_count INTEGER NOT NULL DEFAULT 0,        -- Number of retries

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT orders_filled_quantity_valid CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    CONSTRAINT orders_remaining_quantity_valid CHECK (remaining_quantity >= 0 AND remaining_quantity <= quantity),
    CONSTRAINT orders_price_positive CHECK (price IS NULL OR price > 0),
    CONSTRAINT orders_stop_price_positive CHECK (stop_price IS NULL OR stop_price > 0),
    CONSTRAINT orders_average_fill_positive CHECK (average_fill_price IS NULL OR average_fill_price > 0),
    CONSTRAINT orders_fees_non_negative CHECK (
        commission >= 0 AND brokerage_fee >= 0 AND exchange_fee >= 0 AND tax_amount >= 0
    ),
    CONSTRAINT orders_retry_count_valid CHECK (retry_count >= 0),
    CONSTRAINT orders_quantities_consistent CHECK (filled_quantity + remaining_quantity = quantity)
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for orders table (performance optimization)
CREATE TABLE orders_y2024m01 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE orders_y2024m02 PARTITION OF orders
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE orders_y2024m03 PARTITION OF orders
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE orders_y2024m04 PARTITION OF orders
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE orders_y2024m05 PARTITION OF orders
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE orders_y2024m06 PARTITION OF orders
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE orders_y2024m07 PARTITION OF orders
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE orders_y2024m08 PARTITION OF orders
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE orders_y2024m09 PARTITION OF orders
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE orders_y2024m10 PARTITION OF orders
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE orders_y2024m11 PARTITION OF orders
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE orders_y2024m12 PARTITION OF orders
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Add foreign key constraint for parent orders
ALTER TABLE orders ADD CONSTRAINT fk_orders_parent
    FOREIGN KEY (parent_order_id) REFERENCES orders(order_id);

-- =============================================================================
-- TABLE: order_executions
-- =============================================================================

CREATE TABLE order_executions (
    -- Primary identification
    execution_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,

    -- Execution details
    execution_type execution_type NOT NULL,
    execution_quantity INTEGER NOT NULL,
    execution_price DECIMAL(20,6) NOT NULL,
    execution_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Trade identification
    trade_id VARCHAR(100),                          -- Exchange trade ID
    broker_execution_id VARCHAR(100),               -- Broker execution ID
    contra_broker VARCHAR(50),                      -- Counterparty broker

    -- Financial details
    gross_amount DECIMAL(20,6) NOT NULL,            -- Gross trade amount
    commission DECIMAL(10,4) NOT NULL DEFAULT 0,    -- Commission on this execution
    brokerage_fee DECIMAL(10,4) NOT NULL DEFAULT 0, -- Brokerage fee
    exchange_fee DECIMAL(10,4) NOT NULL DEFAULT 0,  -- Exchange fee
    tax_amount DECIMAL(10,4) NOT NULL DEFAULT 0,    -- Tax amount
    net_amount DECIMAL(20,6) NOT NULL,              -- Net amount (gross - fees)

    -- Market microstructure
    bid_price DECIMAL(20,6),                        -- Best bid at execution
    ask_price DECIMAL(20,6),                        -- Best ask at execution
    market_price DECIMAL(20,6),                     -- Market price at execution

    -- Additional metadata
    metadata JSONB NOT NULL DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT order_executions_quantity_positive CHECK (execution_quantity > 0),
    CONSTRAINT order_executions_price_positive CHECK (execution_price > 0),
    CONSTRAINT order_executions_gross_amount_positive CHECK (gross_amount > 0),
    CONSTRAINT order_executions_net_amount_positive CHECK (net_amount > 0),
    CONSTRAINT order_executions_fees_non_negative CHECK (
        commission >= 0 AND brokerage_fee >= 0 AND exchange_fee >= 0 AND tax_amount >= 0
    ),
    CONSTRAINT order_executions_bid_ask_valid CHECK (
        bid_price IS NULL OR ask_price IS NULL OR bid_price <= ask_price
    )
);

-- =============================================================================
-- TABLE: positions
-- =============================================================================

CREATE TABLE positions (
    -- Primary identification
    position_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User and account information
    user_id UUID NOT NULL REFERENCES users(user_id),
    organization_id UUID REFERENCES organizations(organization_id),
    account_id VARCHAR(50) NOT NULL,
    sub_account_id VARCHAR(50),

    -- Symbol and position details
    symbol_id UUID NOT NULL REFERENCES symbols(symbol_id),
    position_side position_side NOT NULL DEFAULT 'FLAT',

    -- Quantity and pricing
    quantity INTEGER NOT NULL DEFAULT 0,            -- Current position quantity
    available_quantity INTEGER NOT NULL DEFAULT 0,  -- Available for trading
    blocked_quantity INTEGER NOT NULL DEFAULT 0,    -- Blocked by pending orders

    -- Cost basis tracking (FIFO/LIFO support)
    average_cost DECIMAL(20,6),                     -- Average cost per share
    total_cost DECIMAL(20,6),                       -- Total cost of position
    realized_pnl DECIMAL(20,6) NOT NULL DEFAULT 0,  -- Realized P&L
    unrealized_pnl DECIMAL(20,6),                   -- Unrealized P&L

    -- Market valuation
    market_price DECIMAL(20,6),                     -- Current market price
    market_value DECIMAL(20,6),                     -- Current market value
    day_change DECIMAL(20,6),                       -- Day change in value
    day_change_percent DECIMAL(8,4),                -- Day change percentage

    -- Last update tracking
    last_trade_price DECIMAL(20,6),                -- Last trade price
    last_trade_time TIMESTAMP WITH TIME ZONE,       -- Last trade timestamp
    price_updated_at TIMESTAMP WITH TIME ZONE,      -- Last price update

    -- Risk metrics
    value_at_risk DECIMAL(20,6),                   -- VaR calculation
    maximum_drawdown DECIMAL(20,6),                -- Maximum drawdown

    -- Additional metadata
    metadata JSONB NOT NULL DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT positions_quantities_valid CHECK (
        quantity = available_quantity + blocked_quantity
    ),
    CONSTRAINT positions_average_cost_positive CHECK (
        average_cost IS NULL OR average_cost > 0
    ),
    CONSTRAINT positions_total_cost_valid CHECK (
        (quantity = 0 AND total_cost = 0) OR
        (quantity != 0 AND total_cost IS NOT NULL)
    ),
    CONSTRAINT positions_market_price_positive CHECK (
        market_price IS NULL OR market_price > 0
    ),

    -- Unique constraint: one position per account-symbol combination
    CONSTRAINT positions_unique_account_symbol UNIQUE (user_id, account_id, symbol_id)
);

-- =============================================================================
-- TABLE: broker_accounts (for multi-broker support)
-- =============================================================================

CREATE TABLE broker_accounts (
    -- Primary identification
    broker_account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    organization_id UUID REFERENCES organizations(organization_id),

    -- Broker information
    broker_name VARCHAR(100) NOT NULL,              -- AlgoLab, DenizBank, etc.
    account_number VARCHAR(50) NOT NULL,            -- Broker account number
    sub_account_number VARCHAR(50),                 -- Sub-account if applicable

    -- Encrypted credentials (using pgcrypto)
    encrypted_api_key TEXT,                         -- Encrypted API key
    encrypted_api_secret TEXT,                      -- Encrypted API secret
    encrypted_username TEXT,                        -- Encrypted username
    encrypted_password TEXT,                        -- Encrypted password

    -- Account status and limits
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    trading_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Trading limits
    daily_trade_limit DECIMAL(20,2),               -- Daily trading limit
    position_limit DECIMAL(20,2),                  -- Maximum position value
    max_order_value DECIMAL(20,2),                 -- Maximum single order value

    -- Account balances (cached from broker)
    cash_balance DECIMAL(20,2),                     -- Available cash
    buying_power DECIMAL(20,2),                     -- Available buying power
    total_equity DECIMAL(20,2),                     -- Total account equity
    margin_used DECIMAL(20,2),                      -- Used margin
    margin_available DECIMAL(20,2),                 -- Available margin

    -- Connection tracking
    last_sync_at TIMESTAMP WITH TIME ZONE,          -- Last successful sync
    connection_status VARCHAR(20) DEFAULT 'DISCONNECTED', -- CONNECTED, DISCONNECTED, ERROR
    error_message TEXT,                             -- Last error message

    -- Additional metadata
    broker_metadata JSONB NOT NULL DEFAULT '{}',    -- Broker-specific settings

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,  -- Soft delete

    -- Constraints
    CONSTRAINT broker_accounts_limits_positive CHECK (
        daily_trade_limit IS NULL OR daily_trade_limit > 0
    ),
    CONSTRAINT broker_accounts_balances_non_negative CHECK (
        cash_balance IS NULL OR cash_balance >= 0
    ),
    CONSTRAINT broker_accounts_unique_account UNIQUE (user_id, broker_name, account_number)
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

-- Symbols table indexes
CREATE INDEX idx_symbols_symbol ON symbols(symbol) WHERE deleted_at IS NULL;
CREATE INDEX idx_symbols_exchange ON symbols(exchange) WHERE deleted_at IS NULL;
CREATE INDEX idx_symbols_market_type ON symbols(market_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_symbols_status ON symbols(symbol_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_symbols_sector ON symbols(sector) WHERE deleted_at IS NULL;
CREATE INDEX idx_symbols_isin ON symbols(isin_code) WHERE deleted_at IS NULL;

-- JSONB index for symbol metadata
CREATE INDEX idx_symbols_index_memberships_gin ON symbols USING GIN(index_memberships);
CREATE INDEX idx_symbols_metadata_gin ON symbols USING GIN(metadata);

-- Orders table indexes (per partition for better performance)
CREATE INDEX idx_orders_user_id ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_account_id ON orders(account_id, created_at DESC);
CREATE INDEX idx_orders_symbol_id ON orders(symbol_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(order_status, created_at DESC);
CREATE INDEX idx_orders_client_order_id ON orders(client_order_id) WHERE client_order_id IS NOT NULL;
CREATE INDEX idx_orders_broker_order_id ON orders(broker_order_id) WHERE broker_order_id IS NOT NULL;
CREATE INDEX idx_orders_parent_id ON orders(parent_order_id) WHERE parent_order_id IS NOT NULL;
CREATE INDEX idx_orders_strategy_id ON orders(strategy_id) WHERE strategy_id IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_orders_user_status ON orders(user_id, order_status, created_at DESC);
CREATE INDEX idx_orders_symbol_status ON orders(symbol_id, order_status, created_at DESC);
CREATE INDEX idx_orders_account_status ON orders(account_id, order_status, created_at DESC);

-- Active orders index (most frequently queried)
CREATE INDEX idx_orders_active ON orders(user_id, symbol_id)
    WHERE order_status IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED');

-- JSONB index for order metadata
CREATE INDEX idx_orders_metadata_gin ON orders USING GIN(metadata);

-- Order executions indexes
CREATE INDEX idx_order_executions_order_id ON order_executions(order_id, execution_time DESC);
CREATE INDEX idx_order_executions_trade_id ON order_executions(trade_id) WHERE trade_id IS NOT NULL;
CREATE INDEX idx_order_executions_execution_time ON order_executions(execution_time DESC);
CREATE INDEX idx_order_executions_execution_type ON order_executions(execution_type, execution_time DESC);

-- Positions table indexes
CREATE INDEX idx_positions_user_id ON positions(user_id);
CREATE INDEX idx_positions_account_id ON positions(account_id);
CREATE INDEX idx_positions_symbol_id ON positions(symbol_id);
CREATE INDEX idx_positions_side ON positions(position_side) WHERE position_side != 'FLAT';

-- Active positions index
CREATE INDEX idx_positions_active ON positions(user_id, account_id) WHERE quantity != 0;

-- Broker accounts indexes
CREATE INDEX idx_broker_accounts_user_id ON broker_accounts(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_broker_accounts_broker ON broker_accounts(broker_name) WHERE deleted_at IS NULL;
CREATE INDEX idx_broker_accounts_active ON broker_accounts(is_active, user_id) WHERE deleted_at IS NULL;

-- =============================================================================
-- TRIGGERS
-- =============================================================================

-- Add updated_at triggers for all tables
CREATE TRIGGER trigger_symbols_updated_at
    BEFORE UPDATE ON symbols
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_broker_accounts_updated_at
    BEFORE UPDATE ON broker_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- FUNCTIONS FOR BUSINESS LOGIC
-- =============================================================================

-- Function to calculate position P&L
CREATE OR REPLACE FUNCTION calculate_position_pnl(
    p_position_id UUID,
    p_current_price DECIMAL(20,6)
) RETURNS TABLE(
    unrealized_pnl DECIMAL(20,6),
    market_value DECIMAL(20,6),
    day_change DECIMAL(20,6),
    day_change_percent DECIMAL(8,4)
) AS $$
DECLARE
    pos_record RECORD;
BEGIN
    SELECT
        quantity,
        average_cost,
        last_trade_price
    INTO pos_record
    FROM positions
    WHERE position_id = p_position_id;

    IF NOT FOUND THEN
        RETURN;
    END IF;

    -- Calculate unrealized P&L
    unrealized_pnl := (p_current_price - pos_record.average_cost) * pos_record.quantity;

    -- Calculate market value
    market_value := p_current_price * pos_record.quantity;

    -- Calculate day change
    IF pos_record.last_trade_price IS NOT NULL THEN
        day_change := (p_current_price - pos_record.last_trade_price) * pos_record.quantity;
        day_change_percent := ((p_current_price - pos_record.last_trade_price) / pos_record.last_trade_price) * 100;
    ELSE
        day_change := NULL;
        day_change_percent := NULL;
    END IF;

    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

-- Function to update position after trade
CREATE OR REPLACE FUNCTION update_position_after_trade(
    p_user_id UUID,
    p_account_id VARCHAR(50),
    p_symbol_id UUID,
    p_side order_side,
    p_quantity INTEGER,
    p_price DECIMAL(20,6)
) RETURNS VOID AS $$
DECLARE
    existing_pos RECORD;
    new_quantity INTEGER;
    new_total_cost DECIMAL(20,6);
    new_average_cost DECIMAL(20,6);
BEGIN
    -- Get existing position
    SELECT * INTO existing_pos
    FROM positions
    WHERE user_id = p_user_id
    AND account_id = p_account_id
    AND symbol_id = p_symbol_id;

    IF NOT FOUND THEN
        -- Create new position
        IF p_side = 'BUY' THEN
            new_quantity := p_quantity;
            new_total_cost := p_quantity * p_price;
            new_average_cost := p_price;
        ELSE
            new_quantity := -p_quantity;
            new_total_cost := -(p_quantity * p_price);
            new_average_cost := p_price;
        END IF;

        INSERT INTO positions (
            user_id, account_id, symbol_id,
            position_side, quantity, available_quantity,
            average_cost, total_cost
        ) VALUES (
            p_user_id, p_account_id, p_symbol_id,
            CASE WHEN new_quantity > 0 THEN 'LONG'::position_side
                 WHEN new_quantity < 0 THEN 'SHORT'::position_side
                 ELSE 'FLAT'::position_side END,
            new_quantity, new_quantity,
            new_average_cost, new_total_cost
        );
    ELSE
        -- Update existing position
        IF p_side = 'BUY' THEN
            new_quantity := existing_pos.quantity + p_quantity;
            new_total_cost := existing_pos.total_cost + (p_quantity * p_price);
        ELSE
            new_quantity := existing_pos.quantity - p_quantity;
            new_total_cost := existing_pos.total_cost - (p_quantity * p_price);
        END IF;

        IF new_quantity != 0 THEN
            new_average_cost := ABS(new_total_cost / new_quantity);
        ELSE
            new_average_cost := NULL;
            new_total_cost := 0;
        END IF;

        UPDATE positions SET
            quantity = new_quantity,
            available_quantity = new_quantity,
            position_side = CASE
                WHEN new_quantity > 0 THEN 'LONG'::position_side
                WHEN new_quantity < 0 THEN 'SHORT'::position_side
                ELSE 'FLAT'::position_side END,
            average_cost = new_average_cost,
            total_cost = new_total_cost,
            updated_at = CURRENT_TIMESTAMP
        WHERE position_id = existing_pos.position_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- VIEWS FOR COMMON QUERIES
-- =============================================================================

-- Active orders view
CREATE VIEW active_orders AS
SELECT
    o.*,
    s.symbol,
    s.name as symbol_name,
    u.email as user_email
FROM orders o
JOIN symbols s ON o.symbol_id = s.symbol_id
JOIN users u ON o.user_id = u.user_id
WHERE o.order_status IN ('SUBMITTED', 'ACCEPTED', 'PARTIALLY_FILLED')
AND s.deleted_at IS NULL
AND u.deleted_at IS NULL;

-- Portfolio summary view
CREATE VIEW portfolio_summary AS
SELECT
    p.user_id,
    p.account_id,
    COUNT(*) as position_count,
    SUM(CASE WHEN p.quantity > 0 THEN p.market_value ELSE 0 END) as long_market_value,
    SUM(CASE WHEN p.quantity < 0 THEN ABS(p.market_value) ELSE 0 END) as short_market_value,
    SUM(p.market_value) as total_market_value,
    SUM(p.unrealized_pnl) as total_unrealized_pnl,
    SUM(p.day_change) as total_day_change
FROM positions p
WHERE p.quantity != 0
GROUP BY p.user_id, p.account_id;

-- Order execution summary view
CREATE VIEW order_execution_summary AS
SELECT
    o.order_id,
    o.user_id,
    o.symbol_id,
    s.symbol,
    o.order_side,
    o.quantity as order_quantity,
    o.filled_quantity,
    o.average_fill_price,
    COUNT(oe.execution_id) as execution_count,
    SUM(oe.gross_amount) as total_gross_amount,
    SUM(oe.commission + oe.brokerage_fee + oe.exchange_fee + oe.tax_amount) as total_fees,
    SUM(oe.net_amount) as total_net_amount
FROM orders o
LEFT JOIN order_executions oe ON o.order_id = oe.order_id
LEFT JOIN symbols s ON o.symbol_id = s.symbol_id
WHERE o.filled_quantity > 0
GROUP BY o.order_id, o.user_id, o.symbol_id, s.symbol, o.order_side, o.quantity, o.filled_quantity, o.average_fill_price;

-- =============================================================================
-- SAMPLE DATA FOR TESTING
-- =============================================================================

-- Insert popular BIST symbols
INSERT INTO symbols (symbol, name, local_name, isin_code, market_type, sector, lot_size, tick_size, currency_code) VALUES
('GARAN', 'Garanti BBVA', 'Türkiye Garanti Bankası A.Ş.', 'TREGARAN00017', 'EQUITY', 'Banking', 1, 0.01, 'TRY'),
('THYAO', 'Türk Hava Yolları', 'Türk Hava Yolları A.O.', 'TRETHYAO00013', 'EQUITY', 'Transportation', 1, 0.01, 'TRY'),
('AKBNK', 'Akbank', 'Akbank T.A.Ş.', 'TREAKBNK00015', 'EQUITY', 'Banking', 1, 0.01, 'TRY'),
('ISCTR', 'İş Bankası', 'Türkiye İş Bankası A.Ş.', 'TREISCTR00014', 'EQUITY', 'Banking', 1, 0.01, 'TRY'),
('ASELS', 'Aselsan', 'Aselsan Elektronik Sanayi ve Ticaret A.Ş.', 'TREASELS00011', 'EQUITY', 'Technology', 1, 0.01, 'TRY'),
('KOZAL', 'Koza Altın', 'Koza Altın İşletmeleri A.Ş.', 'TREKOZAL00010', 'EQUITY', 'Mining', 1, 0.01, 'TRY'),
('PETKM', 'Petkim', 'Petkim Petrokimya Holding A.Ş.', 'TREPETKM00014', 'EQUITY', 'Chemicals', 1, 0.01, 'TRY'),
('TUPRS', 'Tüpraş', 'Türkiye Petrol Rafinerileri A.Ş.', 'TRETUPRS00015', 'EQUITY', 'Oil & Gas', 1, 0.01, 'TRY');

-- Update symbols with index memberships
UPDATE symbols SET
    index_memberships = '["XU100", "XU030"]',
    metadata = '{"market_maker": true, "high_liquidity": true}'
WHERE symbol IN ('GARAN', 'AKBNK', 'ISCTR');

UPDATE symbols SET
    index_memberships = '["XU100"]',
    metadata = '{"volatility": "high", "sector_leader": true}'
WHERE symbol IN ('THYAO', 'ASELS', 'TUPRS');

-- =============================================================================
-- ROW LEVEL SECURITY POLICIES
-- =============================================================================

-- Enable RLS on trading tables
ALTER TABLE symbols ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE positions ENABLE ROW LEVEL SECURITY;
ALTER TABLE broker_accounts ENABLE ROW LEVEL SECURITY;

-- Symbols are visible to all users (public market data)
CREATE POLICY symbols_public_policy ON symbols FOR SELECT TO app_user USING (true);

-- Users can only see their own orders
CREATE POLICY orders_own_data_policy ON orders
    FOR ALL TO app_user
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- Users can only see executions of their own orders
CREATE POLICY order_executions_own_data_policy ON order_executions
    FOR ALL TO app_user
    USING (order_id IN (
        SELECT order_id FROM orders
        WHERE user_id = current_setting('app.current_user_id')::UUID
    ));

-- Users can only see their own positions
CREATE POLICY positions_own_data_policy ON positions
    FOR ALL TO app_user
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- Users can only see their own broker accounts
CREATE POLICY broker_accounts_own_data_policy ON broker_accounts
    FOR ALL TO app_user
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- =============================================================================
-- COMMENTS FOR DOCUMENTATION
-- =============================================================================

-- Table comments
COMMENT ON TABLE symbols IS 'Market symbols with trading specifications and market data';
COMMENT ON TABLE orders IS 'Trading orders with complete lifecycle tracking and partitioning by month';
COMMENT ON TABLE order_executions IS 'Individual trade executions with detailed financial breakdown';
COMMENT ON TABLE positions IS 'Real-time position tracking with P&L calculations and risk metrics';
COMMENT ON TABLE broker_accounts IS 'Multi-broker account management with encrypted credentials';

-- Important column comments
COMMENT ON COLUMN symbols.index_memberships IS 'JSONB array of index codes this symbol belongs to';
COMMENT ON COLUMN orders.metadata IS 'JSONB flexible metadata for strategy parameters and custom fields';
COMMENT ON COLUMN orders.parent_order_id IS 'Links child orders to parent for bracket and OCO strategies';
COMMENT ON COLUMN positions.available_quantity IS 'Position quantity available for trading (not blocked by orders)';
COMMENT ON COLUMN broker_accounts.encrypted_api_key IS 'AES-256-GCM encrypted broker API credentials';

-- =============================================================================
-- FINAL VERIFICATION
-- =============================================================================

DO $$
DECLARE
    table_count INTEGER;
    index_count INTEGER;
    trigger_count INTEGER;
    function_count INTEGER;
BEGIN
    -- Count created objects
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_name IN ('symbols', 'orders', 'order_executions', 'positions', 'broker_accounts');

    SELECT COUNT(*) INTO index_count
    FROM pg_indexes
    WHERE schemaname = 'public'
    AND indexname LIKE 'idx_%';

    SELECT COUNT(*) INTO trigger_count
    FROM information_schema.triggers
    WHERE trigger_schema = 'public'
    AND trigger_name LIKE 'trigger_%';

    SELECT COUNT(*) INTO function_count
    FROM information_schema.routines
    WHERE routine_schema = 'public'
    AND routine_name IN ('calculate_position_pnl', 'update_position_after_trade');

    -- Report results
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Trading Domain Schema Migration Completed Successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Tables created: %', table_count;
    RAISE NOTICE 'Indexes created: %', index_count;
    RAISE NOTICE 'Triggers created: %', trigger_count;
    RAISE NOTICE 'Functions created: %', function_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Features implemented:';
    RAISE NOTICE '  ✅ Symbol master data with BIST specifications';
    RAISE NOTICE '  ✅ Order management with monthly partitioning';
    RAISE NOTICE '  ✅ Trade execution tracking with financial details';
    RAISE NOTICE '  ✅ Real-time position management with P&L calculations';
    RAISE NOTICE '  ✅ Multi-broker account support with encrypted credentials';
    RAISE NOTICE '  ✅ Comprehensive indexing for high-performance queries';
    RAISE NOTICE '  ✅ Row Level Security for data isolation';
    RAISE NOTICE '  ✅ Business logic functions for position management';
    RAISE NOTICE '=============================================================================';
END $$;