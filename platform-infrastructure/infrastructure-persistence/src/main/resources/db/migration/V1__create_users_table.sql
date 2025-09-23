-- Create users table with all required fields
-- This table stores user information for the BIST Trading Platform

CREATE TABLE users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    organization_id VARCHAR(36),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    tc_kimlik_no VARCHAR(500), -- Encrypted field
    phone_number VARCHAR(500), -- Encrypted field
    birth_date TIMESTAMP,
    gender CHAR(1) CHECK (gender IN ('M', 'F', 'O')),
    nationality VARCHAR(3),
    preferred_language VARCHAR(5) DEFAULT 'tr',
    timezone VARCHAR(50) DEFAULT 'Europe/Istanbul',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'BANNED', 'CLOSED')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified_at TIMESTAMP,
    kyc_completed BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_completed_at TIMESTAMP,
    kyc_level VARCHAR(20) CHECK (kyc_level IN ('NONE', 'BASIC', 'INTERMEDIATE', 'ADVANCED')),
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(500), -- Encrypted field
    risk_profile VARCHAR(20) DEFAULT 'MODERATE' CHECK (risk_profile IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    professional_investor BOOLEAN NOT NULL DEFAULT FALSE,
    investment_experience VARCHAR(20) DEFAULT 'BEGINNER' CHECK (investment_experience IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    annual_income VARCHAR(20) CHECK (annual_income IN ('RANGE_0_50K', 'RANGE_50K_100K', 'RANGE_100K_250K', 'RANGE_250K_500K', 'RANGE_500K_1M', 'RANGE_1M_PLUS')),
    net_worth VARCHAR(20) CHECK (net_worth IN ('RANGE_0_50K', 'RANGE_50K_100K', 'RANGE_100K_250K', 'RANGE_250K_500K', 'RANGE_500K_1M', 'RANGE_1M_PLUS')),
    employment_status VARCHAR(50),
    occupation VARCHAR(100),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    password_expires_at TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    terms_accepted_at TIMESTAMP,
    privacy_accepted_at TIMESTAMP,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consent_at TIMESTAMP,
    preferences JSONB, -- PostgreSQL JSONB for user preferences
    metadata JSONB, -- Additional metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username) WHERE username IS NOT NULL;
CREATE INDEX idx_users_organization_id ON users(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at) WHERE last_login_at IS NOT NULL;
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_kyc_completed ON users(kyc_completed);
CREATE INDEX idx_users_professional_investor ON users(professional_investor);
CREATE INDEX idx_users_risk_profile ON users(risk_profile);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;

-- Create compound indexes for common query patterns
CREATE INDEX idx_users_status_active ON users(status, deleted_at) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_users_verification_status ON users(email_verified, phone_verified, kyc_completed);
CREATE INDEX idx_users_trading_eligible ON users(status, email_verified, phone_verified, kyc_completed, must_change_password, account_locked_until);

-- Create partial indexes for specific use cases
CREATE INDEX idx_users_pending_verification ON users(status) WHERE status = 'PENDING' AND email_verified = FALSE;
CREATE INDEX idx_users_incomplete_kyc ON users(status) WHERE status = 'ACTIVE' AND kyc_completed = FALSE;
CREATE INDEX idx_users_locked_accounts ON users(account_locked_until) WHERE account_locked_until > CURRENT_TIMESTAMP;
CREATE INDEX idx_users_expired_passwords ON users(password_expires_at) WHERE password_expires_at < CURRENT_TIMESTAMP AND status = 'ACTIVE';

-- Enable TimescaleDB for time-series functionality (if TimescaleDB is available)
-- This will be executed conditionally based on extension availability
DO $$
BEGIN
    -- Check if TimescaleDB extension exists
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
        -- Create hypertable for time-series user analytics
        PERFORM create_hypertable('users', 'created_at', chunk_time_interval => INTERVAL '1 month', if_not_exists => TRUE);
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- TimescaleDB not available, continue without hypertable
        RAISE NOTICE 'TimescaleDB not available, users table created as regular table';
END
$$;

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts for BIST Trading Platform with encrypted PII fields';
COMMENT ON COLUMN users.id IS 'Primary key - UUID format';
COMMENT ON COLUMN users.organization_id IS 'Reference to organization (nullable for individual users)';
COMMENT ON COLUMN users.tc_kimlik_no IS 'Turkish identity number - encrypted with AES-256-GCM';
COMMENT ON COLUMN users.phone_number IS 'Phone number - encrypted with AES-256-GCM';
COMMENT ON COLUMN users.two_factor_secret IS 'TOTP secret for 2FA - encrypted with AES-256-GCM';
COMMENT ON COLUMN users.preferences IS 'User preferences stored as JSONB';
COMMENT ON COLUMN users.metadata IS 'Additional user metadata stored as JSONB';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp - NULL means not deleted';

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();