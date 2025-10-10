-- AlgoLab Session Storage Table
-- Stores authentication sessions for AlgoLab API integration
-- Migration: V003
-- Author: BIST Trading Platform Team
-- Date: 2025-10-10

-- Create algolab_sessions table
CREATE TABLE IF NOT EXISTS algolab_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    token VARCHAR(500) NOT NULL,
    hash VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_refresh_at TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    websocket_connected BOOLEAN DEFAULT false,
    websocket_last_connected TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    termination_reason VARCHAR(100),
    terminated_at TIMESTAMP,

    -- Foreign key constraint (if user_entity table exists)
    CONSTRAINT fk_algolab_session_user FOREIGN KEY (user_id)
        REFERENCES user_entity(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_algolab_user_id
    ON algolab_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_algolab_active
    ON algolab_sessions(active);

CREATE INDEX IF NOT EXISTS idx_algolab_expires_at
    ON algolab_sessions(expires_at);

CREATE INDEX IF NOT EXISTS idx_algolab_user_active
    ON algolab_sessions(user_id, active);

CREATE INDEX IF NOT EXISTS idx_algolab_token
    ON algolab_sessions(token) WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_algolab_websocket
    ON algolab_sessions(websocket_connected) WHERE active = true;

-- Add comments for documentation
COMMENT ON TABLE algolab_sessions IS 'Stores AlgoLab API authentication sessions with tokens and hashes';
COMMENT ON COLUMN algolab_sessions.id IS 'Primary key - UUID';
COMMENT ON COLUMN algolab_sessions.user_id IS 'Foreign key to user_entity table (nullable for system sessions)';
COMMENT ON COLUMN algolab_sessions.token IS 'AlgoLab authentication token from LoginUser response';
COMMENT ON COLUMN algolab_sessions.hash IS 'AlgoLab authorization hash from LoginUserControl response';
COMMENT ON COLUMN algolab_sessions.created_at IS 'Session creation timestamp';
COMMENT ON COLUMN algolab_sessions.expires_at IS 'Session expiration timestamp (default: 24 hours)';
COMMENT ON COLUMN algolab_sessions.last_refresh_at IS 'Last session refresh timestamp (keep-alive)';
COMMENT ON COLUMN algolab_sessions.updated_at IS 'Last update timestamp';
COMMENT ON COLUMN algolab_sessions.active IS 'Session active status (false when logged out or expired)';
COMMENT ON COLUMN algolab_sessions.websocket_connected IS 'WebSocket connection status';
COMMENT ON COLUMN algolab_sessions.websocket_last_connected IS 'Last WebSocket connection timestamp';
COMMENT ON COLUMN algolab_sessions.ip_address IS 'IP address from which session was created';
COMMENT ON COLUMN algolab_sessions.user_agent IS 'User agent string for security audit';
COMMENT ON COLUMN algolab_sessions.termination_reason IS 'Reason for session termination (LOGOUT, EXPIRED, REVOKED)';
COMMENT ON COLUMN algolab_sessions.terminated_at IS 'Session termination timestamp';

-- Create trigger to automatically update updated_at
CREATE OR REPLACE FUNCTION update_algolab_session_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_algolab_session_timestamp
    BEFORE UPDATE ON algolab_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_algolab_session_timestamp();

-- Create view for active sessions summary
CREATE OR REPLACE VIEW v_algolab_active_sessions AS
SELECT
    s.id,
    s.user_id,
    s.created_at,
    s.expires_at,
    s.last_refresh_at,
    s.websocket_connected,
    s.ip_address,
    EXTRACT(EPOCH FROM (s.expires_at - CURRENT_TIMESTAMP)) / 3600 AS hours_until_expiry,
    CASE
        WHEN s.expires_at < CURRENT_TIMESTAMP THEN 'EXPIRED'
        WHEN s.expires_at < CURRENT_TIMESTAMP + INTERVAL '1 hour' THEN 'EXPIRING_SOON'
        ELSE 'ACTIVE'
    END AS status
FROM algolab_sessions s
WHERE s.active = true
ORDER BY s.created_at DESC;

COMMENT ON VIEW v_algolab_active_sessions IS 'Summary view of active AlgoLab sessions with expiry status';
