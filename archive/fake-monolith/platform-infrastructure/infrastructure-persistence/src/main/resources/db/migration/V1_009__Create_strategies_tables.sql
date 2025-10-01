-- Create strategies and strategy_executions tables
-- For algorithmic trading strategy management and tracking

-- Main strategies table
CREATE TABLE strategies (
    id BIGSERIAL PRIMARY KEY,

    -- Basic Strategy Information
    strategy_name VARCHAR(255) NOT NULL,
    strategy_code VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'MEAN_REVERT_001'
    strategy_version VARCHAR(20) DEFAULT '1.0.0',

    -- Strategy Classification
    strategy_type VARCHAR(50) NOT NULL, -- ALGORITHMIC, MANUAL, HYBRID
    strategy_category VARCHAR(50), -- ARBITRAGE, MOMENTUM, MEAN_REVERSION, PAIRS
    risk_profile VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, EXTREME

    -- Strategy Definition
    description TEXT,
    strategy_parameters JSONB NOT NULL DEFAULT '{}'::JSONB, -- Strategy-specific parameters
    entry_rules JSONB DEFAULT '{}'::JSONB, -- Entry condition rules
    exit_rules JSONB DEFAULT '{}'::JSONB, -- Exit condition rules
    risk_rules JSONB DEFAULT '{}'::JSONB, -- Risk management rules

    -- Trading Specifications
    applicable_symbols JSONB DEFAULT '[]'::JSONB, -- Symbol codes where strategy applies
    applicable_markets JSONB DEFAULT '["BIST"]'::JSONB, -- Markets where strategy can trade
    timeframes JSONB DEFAULT '[]'::JSONB, -- Supported timeframes

    -- Risk Limits
    max_position_size DECIMAL(20,2), -- Maximum position size
    max_daily_volume DECIMAL(20,2), -- Maximum daily trading volume
    max_drawdown_percentage DECIMAL(5,2), -- Maximum allowed drawdown
    stop_loss_percentage DECIMAL(5,2), -- Global stop-loss
    take_profit_percentage DECIMAL(5,2), -- Global take-profit

    -- Capital Allocation
    allocated_capital DECIMAL(20,2), -- Capital allocated to this strategy
    max_capital_usage_percentage DECIMAL(5,2) DEFAULT 100.0, -- Max % of allocated capital to use

    -- Strategy Status
    is_active BOOLEAN DEFAULT false, -- Whether strategy is currently active
    is_backtested BOOLEAN DEFAULT false, -- Whether strategy has been backtested
    is_paper_trading BOOLEAN DEFAULT true, -- Whether in paper trading mode
    is_live_trading BOOLEAN DEFAULT false, -- Whether in live trading mode

    -- Performance Tracking
    total_trades INTEGER DEFAULT 0,
    winning_trades INTEGER DEFAULT 0,
    losing_trades INTEGER DEFAULT 0,
    total_pnl DECIMAL(20,6) DEFAULT 0,
    max_drawdown DECIMAL(20,6) DEFAULT 0,
    sharpe_ratio DECIMAL(8,4),
    win_rate DECIMAL(5,2),

    -- Developer and Ownership
    created_by VARCHAR(100) NOT NULL,
    strategy_owner VARCHAR(100), -- Strategy owner/manager
    development_team JSONB DEFAULT '[]'::JSONB, -- Team members

    -- Execution Settings
    execution_frequency VARCHAR(20) DEFAULT 'REAL_TIME', -- REAL_TIME, MINUTE, HOURLY, DAILY
    max_orders_per_day INTEGER DEFAULT 100,
    order_size_method VARCHAR(30) DEFAULT 'FIXED', -- FIXED, PERCENTAGE, KELLY, VOLATILITY

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_executed_at TIMESTAMP WITH TIME ZONE,

    -- Version control
    parent_strategy_id BIGINT, -- For strategy versioning
    FOREIGN KEY (parent_strategy_id) REFERENCES strategies(id)
);

-- Strategy executions tracking table
CREATE TABLE strategy_executions (
    id BIGSERIAL PRIMARY KEY,

    -- Link to strategy
    strategy_id BIGINT NOT NULL,
    FOREIGN KEY (strategy_id) REFERENCES strategies(id) ON DELETE CASCADE,

    -- Execution Session
    execution_session_id VARCHAR(100) NOT NULL, -- Unique session identifier
    execution_type VARCHAR(20) DEFAULT 'LIVE', -- LIVE, BACKTEST, PAPER

    -- Timing
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    execution_duration_seconds INTEGER GENERATED ALWAYS AS (
        CASE
            WHEN ended_at IS NOT NULL
            THEN EXTRACT(EPOCH FROM (ended_at - started_at))::INTEGER
            ELSE NULL
        END
    ) STORED,

    -- Execution Context
    market_session VARCHAR(20), -- PRE_MARKET, MARKET_OPEN, POST_MARKET
    market_conditions JSONB DEFAULT '{}'::JSONB, -- Market volatility, volume, etc.
    strategy_parameters_used JSONB DEFAULT '{}'::JSONB, -- Parameters used in this execution

    -- Performance Results
    orders_generated INTEGER DEFAULT 0,
    orders_executed INTEGER DEFAULT 0,
    execution_success_rate DECIMAL(5,2),

    -- Financial Performance
    realized_pnl DECIMAL(20,6) DEFAULT 0,
    unrealized_pnl DECIMAL(20,6) DEFAULT 0,
    total_pnl DECIMAL(20,6) GENERATED ALWAYS AS (realized_pnl + unrealized_pnl) STORED,

    commission_paid DECIMAL(15,6) DEFAULT 0,
    slippage_cost DECIMAL(15,6) DEFAULT 0,
    net_pnl DECIMAL(20,6) GENERATED ALWAYS AS (
        total_pnl - COALESCE(commission_paid, 0) - COALESCE(slippage_cost, 0)
    ) STORED,

    -- Risk Metrics
    max_position_held DECIMAL(20,2),
    max_drawdown_reached DECIMAL(20,6),
    var_95 DECIMAL(20,6), -- Value at Risk 95%
    var_99 DECIMAL(20,6), -- Value at Risk 99%

    -- Execution Statistics
    total_volume_traded DECIMAL(25,2) DEFAULT 0,
    average_trade_size DECIMAL(15,2),
    largest_win DECIMAL(20,6),
    largest_loss DECIMAL(20,6),

    -- Market Impact
    market_impact_bps DECIMAL(8,4), -- Market impact in basis points
    implementation_shortfall DECIMAL(8,4), -- Implementation shortfall

    -- Status and Errors
    execution_status VARCHAR(20) DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED, STOPPED
    error_count INTEGER DEFAULT 0,
    last_error TEXT,
    warnings JSONB DEFAULT '[]'::JSONB,

    -- Signal Performance
    signals_generated INTEGER DEFAULT 0,
    signals_acted_upon INTEGER DEFAULT 0,
    signal_accuracy DECIMAL(5,2),

    -- Execution Notes
    execution_notes TEXT,
    stop_reason VARCHAR(100), -- Why execution stopped

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_strategies_code ON strategies(strategy_code);
CREATE INDEX idx_strategies_type ON strategies(strategy_type);
CREATE INDEX idx_strategies_active ON strategies(is_active);
CREATE INDEX idx_strategies_owner ON strategies(strategy_owner);
CREATE INDEX idx_strategies_performance ON strategies(total_pnl DESC);

CREATE INDEX idx_strategy_executions_strategy_id ON strategy_executions(strategy_id);
CREATE INDEX idx_strategy_executions_session ON strategy_executions(execution_session_id);
CREATE INDEX idx_strategy_executions_started_at ON strategy_executions(started_at);
CREATE INDEX idx_strategy_executions_status ON strategy_executions(execution_status);
CREATE INDEX idx_strategy_executions_performance ON strategy_executions(net_pnl DESC);

-- Composite indexes
CREATE INDEX idx_strategies_active_type ON strategies(is_active, strategy_type) WHERE is_active = true;
CREATE INDEX idx_strategy_executions_recent ON strategy_executions(strategy_id, started_at DESC);

-- GIN indexes for JSON fields
CREATE INDEX idx_strategies_parameters ON strategies USING GIN(strategy_parameters);
CREATE INDEX idx_strategies_symbols ON strategies USING GIN(applicable_symbols);
CREATE INDEX idx_strategy_executions_conditions ON strategy_executions USING GIN(market_conditions);

-- Update triggers
CREATE OR REPLACE FUNCTION update_strategies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_strategies_updated_at
    BEFORE UPDATE ON strategies
    FOR EACH ROW
    EXECUTE FUNCTION update_strategies_updated_at();

CREATE OR REPLACE FUNCTION update_strategy_executions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_strategy_executions_updated_at
    BEFORE UPDATE ON strategy_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_strategy_executions_updated_at();

-- Function to update strategy performance metrics
CREATE OR REPLACE FUNCTION update_strategy_performance()
RETURNS TRIGGER AS $$
BEGIN
    -- Update parent strategy performance when execution ends
    IF NEW.execution_status = 'COMPLETED' AND (OLD.execution_status IS NULL OR OLD.execution_status != 'COMPLETED') THEN
        UPDATE strategies
        SET
            total_pnl = COALESCE(total_pnl, 0) + COALESCE(NEW.net_pnl, 0),
            last_executed_at = NEW.ended_at,
            total_trades = (
                SELECT COUNT(*)
                FROM strategy_executions
                WHERE strategy_id = NEW.strategy_id
                AND execution_status = 'COMPLETED'
            )
        WHERE id = NEW.strategy_id;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_strategy_performance
    AFTER UPDATE ON strategy_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_strategy_performance();

-- Comments
COMMENT ON TABLE strategies IS 'Algorithmic trading strategies definitions and configurations';
COMMENT ON TABLE strategy_executions IS 'Execution tracking and performance results for strategies';
COMMENT ON COLUMN strategies.strategy_parameters IS 'Strategy-specific configuration parameters as JSON';
COMMENT ON COLUMN strategies.applicable_symbols IS 'JSON array of symbol codes where strategy can trade';
COMMENT ON COLUMN strategy_executions.market_conditions IS 'Market conditions during execution as JSON';
COMMENT ON COLUMN strategy_executions.execution_session_id IS 'Unique identifier for execution session';