-- Create broker_api_logs table for comprehensive API monitoring and debugging
-- This table tracks all API calls to external brokers and exchanges

CREATE TABLE broker_api_logs (
    id BIGSERIAL PRIMARY KEY,

    -- Request Identification
    request_id VARCHAR(100) NOT NULL UNIQUE, -- UUID for request tracking
    correlation_id VARCHAR(100), -- For correlating related requests
    session_id VARCHAR(100), -- User session identifier

    -- API Endpoint Information
    api_provider VARCHAR(50) NOT NULL, -- ALGOLAB, BLOOMBERG, REUTERS, BIST
    api_version VARCHAR(20), -- API version used
    endpoint_path VARCHAR(500) NOT NULL, -- Full endpoint path
    http_method VARCHAR(10) NOT NULL, -- GET, POST, PUT, DELETE
    base_url VARCHAR(255), -- Base URL of the API

    -- Request Details
    request_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_headers JSONB DEFAULT '{}'::JSONB, -- HTTP headers (sanitized)
    request_parameters JSONB DEFAULT '{}'::JSONB, -- Query parameters
    request_body TEXT, -- Request body (may be large JSON/XML)
    request_size_bytes INTEGER, -- Size of request payload

    -- Response Details
    response_timestamp TIMESTAMP WITH TIME ZONE,
    response_status_code INTEGER, -- HTTP status code
    response_headers JSONB DEFAULT '{}'::JSONB, -- Response headers
    response_body TEXT, -- Response body (may be large)
    response_size_bytes INTEGER, -- Size of response payload

    -- Performance Metrics
    latency_ms INTEGER GENERATED ALWAYS AS (
        CASE
            WHEN response_timestamp IS NOT NULL
            THEN EXTRACT(MILLISECONDS FROM (response_timestamp - request_timestamp))::INTEGER
            ELSE NULL
        END
    ) STORED,

    dns_lookup_ms INTEGER, -- DNS lookup time
    tcp_connect_ms INTEGER, -- TCP connection time
    tls_handshake_ms INTEGER, -- TLS handshake time
    time_to_first_byte_ms INTEGER, -- TTFB

    -- Error Tracking
    is_error BOOLEAN GENERATED ALWAYS AS (
        CASE
            WHEN response_status_code IS NOT NULL
            THEN response_status_code >= 400
            ELSE false
        END
    ) STORED,

    error_code VARCHAR(50), -- Application-specific error code
    error_message TEXT, -- Error description
    error_type VARCHAR(100), -- NETWORK, AUTHENTICATION, RATE_LIMIT, SERVER_ERROR, etc.
    retry_count INTEGER DEFAULT 0, -- Number of retries attempted
    is_retried BOOLEAN DEFAULT false, -- Whether this request was retried

    -- Rate Limiting
    rate_limit_remaining INTEGER, -- Requests remaining in window
    rate_limit_reset_timestamp TIMESTAMP WITH TIME ZONE, -- When rate limit resets
    rate_limit_exceeded BOOLEAN DEFAULT false, -- Whether rate limit was hit

    -- Authentication Context
    auth_method VARCHAR(50), -- API_KEY, OAUTH2, JWT, BASIC_AUTH
    user_id VARCHAR(100), -- User making the request
    api_key_id VARCHAR(100), -- API key identifier (not the key itself)

    -- Business Context
    operation_type VARCHAR(100), -- PLACE_ORDER, CANCEL_ORDER, GET_POSITIONS, etc.
    affected_symbols JSONB DEFAULT '[]'::JSONB, -- Symbols involved in the operation
    trade_session VARCHAR(20), -- PRE_MARKET, MARKET, POST_MARKET

    -- Request Classification
    request_priority VARCHAR(20) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, CRITICAL
    is_automated BOOLEAN DEFAULT true, -- Whether automated or manual request
    request_source VARCHAR(50), -- WEB_UI, MOBILE_APP, API_CLIENT, SCHEDULER

    -- Caching
    cache_hit BOOLEAN DEFAULT false, -- Whether response came from cache
    cache_key VARCHAR(255), -- Cache key used
    cache_ttl_seconds INTEGER, -- TTL for cached response

    -- Security and Compliance
    contains_pii BOOLEAN DEFAULT false, -- Whether request/response contains PII
    is_sensitive BOOLEAN DEFAULT false, -- Whether contains sensitive data
    compliance_flags JSONB DEFAULT '[]'::JSONB, -- Compliance-related flags

    -- Circuit Breaker and Health
    circuit_breaker_state VARCHAR(20), -- CLOSED, OPEN, HALF_OPEN
    health_check BOOLEAN DEFAULT false, -- Whether this was a health check

    -- Data Quality
    response_validation_passed BOOLEAN, -- Whether response passed validation
    response_schema_version VARCHAR(20), -- Schema version for response validation

    -- Debugging Information
    client_ip_address INET, -- Client IP (if applicable)
    user_agent VARCHAR(500), -- User agent string
    request_trace TEXT, -- Stack trace or debug info
    additional_metadata JSONB DEFAULT '{}'::JSONB, -- Additional debug data

    -- Timestamps for data lifecycle
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP + INTERVAL '90 days') -- Data retention
);

-- Partitioning by date for performance (monthly partitions)
-- Note: In production, consider implementing table partitioning

-- Performance indexes
CREATE INDEX idx_broker_api_logs_request_timestamp ON broker_api_logs(request_timestamp DESC);
CREATE INDEX idx_broker_api_logs_provider ON broker_api_logs(api_provider);
CREATE INDEX idx_broker_api_logs_endpoint ON broker_api_logs(endpoint_path);
CREATE INDEX idx_broker_api_logs_status ON broker_api_logs(response_status_code);
CREATE INDEX idx_broker_api_logs_user ON broker_api_logs(user_id);
CREATE INDEX idx_broker_api_logs_session ON broker_api_logs(session_id);
CREATE INDEX idx_broker_api_logs_correlation ON broker_api_logs(correlation_id);

-- Composite indexes for common queries
CREATE INDEX idx_broker_api_logs_provider_time ON broker_api_logs(api_provider, request_timestamp DESC);
CREATE INDEX idx_broker_api_logs_errors ON broker_api_logs(is_error, request_timestamp DESC) WHERE is_error = true;
CREATE INDEX idx_broker_api_logs_slow_requests ON broker_api_logs(latency_ms DESC, request_timestamp DESC) WHERE latency_ms > 1000;
CREATE INDEX idx_broker_api_logs_rate_limited ON broker_api_logs(rate_limit_exceeded, request_timestamp DESC) WHERE rate_limit_exceeded = true;

-- Indexes for monitoring and alerting
CREATE INDEX idx_broker_api_logs_critical_errors ON broker_api_logs(error_type, request_timestamp DESC)
    WHERE is_error = true AND error_type IN ('NETWORK', 'TIMEOUT', 'SERVER_ERROR');

-- GIN indexes for JSON searches
CREATE INDEX idx_broker_api_logs_symbols ON broker_api_logs USING GIN(affected_symbols);
CREATE INDEX idx_broker_api_logs_metadata ON broker_api_logs USING GIN(additional_metadata);
CREATE INDEX idx_broker_api_logs_compliance ON broker_api_logs USING GIN(compliance_flags);

-- Partial indexes for performance
CREATE INDEX idx_broker_api_logs_recent_errors ON broker_api_logs(request_timestamp DESC)
    WHERE is_error = true AND request_timestamp > (CURRENT_TIMESTAMP - INTERVAL '7 days');

-- Function for automatic cleanup of old logs
CREATE OR REPLACE FUNCTION cleanup_old_broker_api_logs()
RETURNS void AS $$
BEGIN
    DELETE FROM broker_api_logs
    WHERE expires_at < CURRENT_TIMESTAMP;
END;
$$ language 'plpgsql';

-- View for API performance monitoring
CREATE OR REPLACE VIEW broker_api_performance_summary AS
SELECT
    api_provider,
    endpoint_path,
    DATE_TRUNC('hour', request_timestamp) as hour_bucket,
    COUNT(*) as total_requests,
    COUNT(*) FILTER (WHERE is_error) as error_count,
    ROUND(AVG(latency_ms)::numeric, 2) as avg_latency_ms,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms)::numeric, 2) as p95_latency_ms,
    ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms)::numeric, 2) as p99_latency_ms,
    COUNT(*) FILTER (WHERE rate_limit_exceeded) as rate_limited_count,
    COUNT(*) FILTER (WHERE cache_hit) as cache_hits
FROM broker_api_logs
WHERE request_timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY api_provider, endpoint_path, hour_bucket
ORDER BY hour_bucket DESC, total_requests DESC;

-- View for error analysis
CREATE OR REPLACE VIEW broker_api_error_analysis AS
SELECT
    api_provider,
    error_type,
    error_code,
    COUNT(*) as error_count,
    ROUND(AVG(latency_ms)::numeric, 2) as avg_latency_ms,
    MIN(request_timestamp) as first_occurrence,
    MAX(request_timestamp) as last_occurrence,
    COUNT(DISTINCT user_id) as affected_users
FROM broker_api_logs
WHERE is_error = true
AND request_timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY api_provider, error_type, error_code
ORDER BY error_count DESC;

-- Comments
COMMENT ON TABLE broker_api_logs IS 'Comprehensive logging of all broker and exchange API calls';
COMMENT ON COLUMN broker_api_logs.request_id IS 'Unique identifier for each API request';
COMMENT ON COLUMN broker_api_logs.latency_ms IS 'Total request latency in milliseconds';
COMMENT ON COLUMN broker_api_logs.rate_limit_remaining IS 'API calls remaining in current rate limit window';
COMMENT ON COLUMN broker_api_logs.operation_type IS 'Business operation being performed';
COMMENT ON COLUMN broker_api_logs.affected_symbols IS 'JSON array of symbols involved in the API call';
COMMENT ON COLUMN broker_api_logs.expires_at IS 'When this log entry should be automatically deleted';

-- Create scheduled cleanup job (requires pg_cron extension)
-- SELECT cron.schedule('cleanup-broker-logs', '0 2 * * *', 'SELECT cleanup_old_broker_api_logs();');