-- Create user_sessions table
-- This table tracks active user sessions for security and audit purposes

CREATE TABLE user_sessions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'TERMINATED', 'INVALIDATED', 'SUSPENDED')),
    ip_address VARCHAR(45),
    user_agent VARCHAR(1000),
    device_fingerprint VARCHAR(255),
    device_type VARCHAR(20) CHECK (device_type IN ('DESKTOP', 'MOBILE', 'TABLET', 'UNKNOWN')),
    operating_system VARCHAR(100),
    browser VARCHAR(100),
    location_country VARCHAR(3),
    location_city VARCHAR(100),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP,
    ended_at TIMESTAMP,
    logout_reason VARCHAR(20) CHECK (logout_reason IN ('USER_LOGOUT', 'SESSION_TIMEOUT', 'SECURITY_LOGOUT', 'ADMIN_LOGOUT', 'DEVICE_CHANGE', 'CONCURRENT_LOGIN', 'SYSTEM_MAINTENANCE', 'PASSWORD_CHANGE', 'ACCOUNT_SUSPENDED')),
    is_remembered BOOLEAN NOT NULL DEFAULT FALSE,
    is_trusted_device BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_verified BOOLEAN NOT NULL DEFAULT FALSE,
    security_level VARCHAR(20) DEFAULT 'MEDIUM' CHECK (security_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    renewal_count INTEGER NOT NULL DEFAULT 0,
    metadata JSONB, -- Additional session metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token) WHERE refresh_token IS NOT NULL;
CREATE INDEX idx_user_sessions_status ON user_sessions(status);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_created_at ON user_sessions(created_at);
CREATE INDEX idx_user_sessions_ip_address ON user_sessions(ip_address) WHERE ip_address IS NOT NULL;
CREATE INDEX idx_user_sessions_device_fingerprint ON user_sessions(device_fingerprint) WHERE device_fingerprint IS NOT NULL;
CREATE INDEX idx_user_sessions_started_at ON user_sessions(started_at);
CREATE INDEX idx_user_sessions_last_activity_at ON user_sessions(last_activity_at) WHERE last_activity_at IS NOT NULL;

-- Create compound indexes for common query patterns
CREATE INDEX idx_user_sessions_active ON user_sessions(user_id, status, expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_user_sessions_user_active ON user_sessions(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_user_sessions_expired ON user_sessions(status, expires_at) WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP;
CREATE INDEX idx_user_sessions_device_tracking ON user_sessions(user_id, device_fingerprint, ip_address);
CREATE INDEX idx_user_sessions_security_audit ON user_sessions(user_id, security_level, two_factor_verified);

-- Create partial indexes for specific use cases
CREATE INDEX idx_user_sessions_long_running ON user_sessions(started_at) WHERE status = 'ACTIVE' AND started_at < CURRENT_TIMESTAMP - INTERVAL '24 hours';
CREATE INDEX idx_user_sessions_inactive ON user_sessions(last_activity_at) WHERE status = 'ACTIVE' AND last_activity_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';
CREATE INDEX idx_user_sessions_trusted_devices ON user_sessions(user_id, device_fingerprint) WHERE is_trusted_device = TRUE;
CREATE INDEX idx_user_sessions_remembered ON user_sessions(user_id) WHERE is_remembered = TRUE;

-- Add foreign key constraint to users table
ALTER TABLE user_sessions ADD CONSTRAINT fk_user_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Enable TimescaleDB for time-series functionality (if TimescaleDB is available)
-- This will be executed conditionally based on extension availability
DO $$
BEGIN
    -- Check if TimescaleDB extension exists
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
        -- Create hypertable for time-series session analytics
        PERFORM create_hypertable('user_sessions', 'started_at', chunk_time_interval => INTERVAL '1 week', if_not_exists => TRUE);

        -- Create retention policy to automatically drop old session data after 1 year
        PERFORM add_retention_policy('user_sessions', INTERVAL '1 year', if_not_exists => TRUE);
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- TimescaleDB not available, continue without hypertable
        RAISE NOTICE 'TimescaleDB not available, user_sessions table created as regular table';
END
$$;

-- Add comments for documentation
COMMENT ON TABLE user_sessions IS 'User session tracking for security and audit purposes';
COMMENT ON COLUMN user_sessions.id IS 'Primary key - UUID format';
COMMENT ON COLUMN user_sessions.user_id IS 'Reference to user who owns this session';
COMMENT ON COLUMN user_sessions.session_token IS 'Unique session identifier (JWT ID or session token)';
COMMENT ON COLUMN user_sessions.refresh_token IS 'Token used for session renewal';
COMMENT ON COLUMN user_sessions.device_fingerprint IS 'Unique device identifier for security tracking';
COMMENT ON COLUMN user_sessions.security_level IS 'Security level required for this session';
COMMENT ON COLUMN user_sessions.renewal_count IS 'Number of times session has been renewed/extended';
COMMENT ON COLUMN user_sessions.metadata IS 'Additional session data stored as JSONB';

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER trigger_user_sessions_updated_at
    BEFORE UPDATE ON user_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to automatically expire old sessions
CREATE OR REPLACE FUNCTION expire_old_sessions()
RETURNS INTEGER AS $$
DECLARE
    expired_count INTEGER;
BEGIN
    UPDATE user_sessions
    SET status = 'EXPIRED',
        ended_at = CURRENT_TIMESTAMP,
        logout_reason = 'SESSION_TIMEOUT'
    WHERE status = 'ACTIVE'
      AND expires_at < CURRENT_TIMESTAMP;

    GET DIAGNOSTICS expired_count = ROW_COUNT;

    RETURN expired_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION expire_old_sessions() IS 'Function to expire sessions that have passed their expiry time';

-- Create function to clean up old ended sessions
CREATE OR REPLACE FUNCTION cleanup_old_sessions(retention_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions
    WHERE ended_at < CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL
      AND status IN ('TERMINATED', 'EXPIRED', 'INVALIDATED');

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_sessions(INTEGER) IS 'Function to clean up old ended sessions (default 90 days retention)';

-- Create view for active sessions with user information
CREATE VIEW active_sessions_view AS
SELECT
    s.id,
    s.user_id,
    u.email as user_email,
    u.first_name,
    u.last_name,
    s.session_token,
    s.ip_address,
    s.device_type,
    s.operating_system,
    s.browser,
    s.location_country,
    s.location_city,
    s.started_at,
    s.expires_at,
    s.last_activity_at,
    s.is_trusted_device,
    s.two_factor_verified,
    s.security_level,
    EXTRACT(EPOCH FROM (s.expires_at - CURRENT_TIMESTAMP))/60 as minutes_until_expiry,
    CASE
        WHEN s.last_activity_at IS NULL THEN 0
        ELSE EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - s.last_activity_at))/60
    END as minutes_since_last_activity
FROM user_sessions s
JOIN users u ON s.user_id = u.id
WHERE s.status = 'ACTIVE' AND s.expires_at > CURRENT_TIMESTAMP;

COMMENT ON VIEW active_sessions_view IS 'View of active sessions with user details and calculated time fields';

-- Create view for session security audit
CREATE VIEW session_security_audit_view AS
SELECT
    s.user_id,
    u.email,
    COUNT(*) as total_sessions,
    COUNT(*) FILTER (WHERE s.status = 'ACTIVE') as active_sessions,
    COUNT(DISTINCT s.ip_address) as distinct_ips,
    COUNT(DISTINCT s.device_fingerprint) as distinct_devices,
    COUNT(*) FILTER (WHERE s.two_factor_verified = FALSE) as sessions_without_2fa,
    COUNT(*) FILTER (WHERE s.security_level = 'LOW') as low_security_sessions,
    MAX(s.started_at) as last_session_start,
    MAX(s.last_activity_at) as last_activity
FROM user_sessions s
JOIN users u ON s.user_id = u.id
WHERE s.started_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY s.user_id, u.email;

COMMENT ON VIEW session_security_audit_view IS 'Security audit view showing session patterns per user over last 30 days';