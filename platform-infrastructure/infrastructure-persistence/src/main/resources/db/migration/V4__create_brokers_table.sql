-- Create brokers table
-- This table stores external broker and trading service provider information

CREATE TABLE brokers (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    broker_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description VARCHAR(1000),
    broker_type VARCHAR(20) NOT NULL DEFAULT 'TRADITIONAL' CHECK (broker_type IN ('TRADITIONAL', 'DIGITAL', 'ROBO_ADVISOR', 'BANK', 'FINTECH', 'MARKET_MAKER', 'ECN', 'OTHER')),
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DEPRECATED', 'SUSPENDED', 'TESTING')),
    website_url VARCHAR(255),
    api_endpoint VARCHAR(255),
    api_version VARCHAR(20),
    api_key VARCHAR(500), -- Encrypted field
    api_secret VARCHAR(500), -- Encrypted field
    ssl_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    connection_timeout INTEGER DEFAULT 30000,
    read_timeout INTEGER DEFAULT 60000,
    max_connections INTEGER DEFAULT 10,
    rate_limit_per_minute INTEGER DEFAULT 60,
    commission_rate DECIMAL(5,4) CHECK (commission_rate >= 0 AND commission_rate <= 100),
    minimum_commission DECIMAL(10,2) CHECK (minimum_commission >= 0),
    maximum_commission DECIMAL(10,2) CHECK (maximum_commission >= 0),
    commission_currency VARCHAR(3) DEFAULT 'TRY',
    supports_realtime_data BOOLEAN NOT NULL DEFAULT FALSE,
    supports_historical_data BOOLEAN NOT NULL DEFAULT FALSE,
    supports_order_management BOOLEAN NOT NULL DEFAULT FALSE,
    supports_portfolio_tracking BOOLEAN NOT NULL DEFAULT FALSE,
    supported_order_types JSONB, -- Array of supported order types
    supported_markets JSONB, -- Array of supported markets/exchanges
    supported_instruments JSONB, -- Array of supported instrument types
    contact_email VARCHAR(255),
    contact_phone VARCHAR(500), -- Encrypted field
    support_email VARCHAR(255),
    support_phone VARCHAR(500), -- Encrypted field
    last_connection_at TIMESTAMP,
    last_connection_error VARCHAR(1000),
    connection_success_rate DECIMAL(5,2) CHECK (connection_success_rate >= 0 AND connection_success_rate <= 100),
    average_response_time INTEGER CHECK (average_response_time >= 0),
    trading_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    priority_order INTEGER CHECK (priority_order >= 0),
    configuration JSONB, -- Broker-specific configuration
    metadata JSONB, -- Additional metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_brokers_broker_code ON brokers(broker_code);
CREATE INDEX idx_brokers_name ON brokers(name);
CREATE INDEX idx_brokers_status ON brokers(status);
CREATE INDEX idx_brokers_broker_type ON brokers(broker_type);
CREATE INDEX idx_brokers_created_at ON brokers(created_at);
CREATE INDEX idx_brokers_trading_enabled ON brokers(trading_enabled);
CREATE INDEX idx_brokers_priority_order ON brokers(priority_order) WHERE priority_order IS NOT NULL;
CREATE INDEX idx_brokers_deleted_at ON brokers(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_brokers_last_connection_at ON brokers(last_connection_at) WHERE last_connection_at IS NOT NULL;

-- Create compound indexes for common query patterns
CREATE INDEX idx_brokers_status_active ON brokers(status, deleted_at) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_brokers_trading_status ON brokers(status, trading_enabled) WHERE status = 'ACTIVE' AND trading_enabled = TRUE;
CREATE INDEX idx_brokers_api_configured ON brokers(status, api_endpoint) WHERE status = 'ACTIVE' AND api_endpoint IS NOT NULL;
CREATE INDEX idx_brokers_performance ON brokers(connection_success_rate, average_response_time) WHERE connection_success_rate IS NOT NULL;

-- Create partial indexes for specific use cases
CREATE INDEX idx_brokers_realtime_data ON brokers(supports_realtime_data) WHERE supports_realtime_data = TRUE;
CREATE INDEX idx_brokers_order_management ON brokers(supports_order_management) WHERE supports_order_management = TRUE;
CREATE INDEX idx_brokers_low_commission ON brokers(commission_rate) WHERE commission_rate IS NOT NULL AND commission_rate <= 0.1;
CREATE INDEX idx_brokers_reliable ON brokers(connection_success_rate) WHERE connection_success_rate >= 95;

-- Add comments for documentation
COMMENT ON TABLE brokers IS 'External brokers and trading service providers';
COMMENT ON COLUMN brokers.id IS 'Primary key - UUID format';
COMMENT ON COLUMN brokers.broker_code IS 'Unique broker code identifier';
COMMENT ON COLUMN brokers.api_key IS 'API authentication key - encrypted with AES-256-GCM';
COMMENT ON COLUMN brokers.api_secret IS 'API authentication secret - encrypted with AES-256-GCM';
COMMENT ON COLUMN brokers.contact_phone IS 'Contact phone number - encrypted with AES-256-GCM';
COMMENT ON COLUMN brokers.support_phone IS 'Support phone number - encrypted with AES-256-GCM';
COMMENT ON COLUMN brokers.commission_rate IS 'Commission rate as percentage (0-100)';
COMMENT ON COLUMN brokers.connection_success_rate IS 'Connection success rate percentage (0-100)';
COMMENT ON COLUMN brokers.average_response_time IS 'Average API response time in milliseconds';
COMMENT ON COLUMN brokers.supported_order_types IS 'Array of supported order types stored as JSONB';
COMMENT ON COLUMN brokers.supported_markets IS 'Array of supported markets/exchanges stored as JSONB';
COMMENT ON COLUMN brokers.supported_instruments IS 'Array of supported instrument types stored as JSONB';
COMMENT ON COLUMN brokers.configuration IS 'Broker-specific configuration stored as JSONB';
COMMENT ON COLUMN brokers.metadata IS 'Additional broker metadata stored as JSONB';
COMMENT ON COLUMN brokers.deleted_at IS 'Soft delete timestamp - NULL means not deleted';

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER trigger_brokers_updated_at
    BEFORE UPDATE ON brokers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to validate commission configuration
CREATE OR REPLACE FUNCTION validate_broker_commission()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate commission rate and amounts
    IF NEW.minimum_commission IS NOT NULL AND NEW.maximum_commission IS NOT NULL THEN
        IF NEW.minimum_commission > NEW.maximum_commission THEN
            RAISE EXCEPTION 'Minimum commission cannot be greater than maximum commission';
        END IF;
    END IF;

    -- Validate connection timeouts
    IF NEW.connection_timeout IS NOT NULL AND NEW.connection_timeout <= 0 THEN
        RAISE EXCEPTION 'Connection timeout must be positive';
    END IF;

    IF NEW.read_timeout IS NOT NULL AND NEW.read_timeout <= 0 THEN
        RAISE EXCEPTION 'Read timeout must be positive';
    END IF;

    -- Validate max connections
    IF NEW.max_connections IS NOT NULL AND NEW.max_connections <= 0 THEN
        RAISE EXCEPTION 'Maximum connections must be positive';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for commission validation
CREATE TRIGGER trigger_broker_validation
    BEFORE INSERT OR UPDATE ON brokers
    FOR EACH ROW
    EXECUTE FUNCTION validate_broker_commission();

-- Create view for active brokers with capabilities
CREATE VIEW active_brokers_view AS
SELECT
    b.*,
    CASE
        WHEN b.last_connection_at IS NULL THEN 'NEVER_CONNECTED'
        WHEN b.last_connection_at > CURRENT_TIMESTAMP - INTERVAL '1 hour' THEN 'RECENTLY_ACTIVE'
        WHEN b.last_connection_at > CURRENT_TIMESTAMP - INTERVAL '24 hours' THEN 'ACTIVE_TODAY'
        WHEN b.last_connection_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 'ACTIVE_THIS_WEEK'
        ELSE 'INACTIVE'
    END as connection_status,
    CASE
        WHEN b.api_endpoint IS NOT NULL AND b.api_key IS NOT NULL THEN TRUE
        ELSE FALSE
    END as api_configured,
    CASE
        WHEN b.connection_success_rate IS NULL THEN 'NO_DATA'
        WHEN b.connection_success_rate >= 95 THEN 'EXCELLENT'
        WHEN b.connection_success_rate >= 90 THEN 'GOOD'
        WHEN b.connection_success_rate >= 80 THEN 'FAIR'
        ELSE 'POOR'
    END as reliability_rating
FROM brokers b
WHERE b.status = 'ACTIVE' AND b.deleted_at IS NULL;

COMMENT ON VIEW active_brokers_view IS 'View of active brokers with calculated status and capability indicators';

-- Create view for broker performance statistics
CREATE VIEW broker_performance_view AS
SELECT
    b.id,
    b.broker_code,
    b.name,
    b.broker_type,
    b.connection_success_rate,
    b.average_response_time,
    b.last_connection_at,
    b.commission_rate,
    b.supports_realtime_data,
    b.supports_order_management,
    b.trading_enabled,
    CASE
        WHEN b.connection_success_rate IS NOT NULL AND b.average_response_time IS NOT NULL THEN
            (b.connection_success_rate * 0.7) + ((1000.0 / GREATEST(b.average_response_time, 1)) * 0.3)
        WHEN b.connection_success_rate IS NOT NULL THEN
            b.connection_success_rate * 0.7
        ELSE NULL
    END as performance_score
FROM brokers b
WHERE b.status = 'ACTIVE' AND b.deleted_at IS NULL
ORDER BY performance_score DESC NULLS LAST;

COMMENT ON VIEW broker_performance_view IS 'Broker performance metrics with calculated performance score';

-- Create function to update broker connection statistics
CREATE OR REPLACE FUNCTION update_broker_connection_stats(
    p_broker_id VARCHAR(36),
    p_success BOOLEAN,
    p_response_time INTEGER DEFAULT NULL,
    p_error_message VARCHAR(1000) DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    current_success_rate DECIMAL(5,2);
    current_avg_response_time INTEGER;
BEGIN
    -- Get current statistics
    SELECT connection_success_rate, average_response_time
    INTO current_success_rate, current_avg_response_time
    FROM brokers
    WHERE id = p_broker_id;

    -- Update based on success/failure
    UPDATE brokers
    SET
        last_connection_at = CURRENT_TIMESTAMP,
        last_connection_error = CASE WHEN p_success THEN NULL ELSE p_error_message END,
        average_response_time = CASE
            WHEN p_success AND p_response_time IS NOT NULL THEN
                CASE
                    WHEN current_avg_response_time IS NULL THEN p_response_time
                    ELSE (current_avg_response_time + p_response_time) / 2
                END
            ELSE current_avg_response_time
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_broker_id;

    -- Note: Success rate calculation would typically be done by a background job
    -- that analyzes historical connection attempts over a specific time window

END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_broker_connection_stats(VARCHAR(36), BOOLEAN, INTEGER, VARCHAR(1000)) IS 'Updates broker connection statistics after connection attempt';