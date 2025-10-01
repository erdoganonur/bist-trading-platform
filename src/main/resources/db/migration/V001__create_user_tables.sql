-- =============================================================================
-- BIST Trading Platform - User Management Schema
-- Version: 1.0.0
-- Description: Comprehensive user management with encryption, audit, and compliance
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- =============================================================================
-- ENUM TYPES
-- =============================================================================

-- User status enumeration
CREATE TYPE user_status AS ENUM (
    'PENDING_VERIFICATION',    -- Waiting for email/phone verification
    'ACTIVE',                 -- Active user
    'SUSPENDED',              -- Temporarily suspended
    'DEACTIVATED',           -- User-initiated deactivation
    'BLOCKED',               -- Admin blocked
    'KYC_PENDING',           -- KYC verification pending
    'KYC_REJECTED',          -- KYC verification rejected
    'CLOSED'                 -- Account permanently closed
);

-- Account type enumeration
CREATE TYPE account_type AS ENUM (
    'INDIVIDUAL',            -- Individual trader account
    'CORPORATE',             -- Corporate account
    'PROFESSIONAL',          -- Professional trader
    'INSTITUTIONAL'          -- Institutional investor
);

-- KYC status enumeration
CREATE TYPE kyc_status AS ENUM (
    'NOT_STARTED',           -- KYC process not initiated
    'IN_PROGRESS',           -- Documents submitted, under review
    'PENDING_DOCUMENTS',     -- Additional documents required
    'APPROVED',              -- KYC approved
    'REJECTED',              -- KYC rejected
    'EXPIRED'                -- KYC approval expired
);

-- Organization role enumeration
CREATE TYPE organization_role AS ENUM (
    'OWNER',                 -- Organization owner
    'ADMIN',                 -- Admin privileges
    'TRADER',                -- Trading privileges
    'VIEWER',                -- View-only access
    'COMPLIANCE_OFFICER',    -- Compliance management
    'RISK_MANAGER'           -- Risk management
);

-- Session type enumeration
CREATE TYPE session_type AS ENUM (
    'WEB',                   -- Web browser session
    'MOBILE_APP',            -- Mobile application
    'API',                   -- API access
    'DESKTOP_APP'            -- Desktop application
);

-- MFA method enumeration
CREATE TYPE mfa_method AS ENUM (
    'SMS',                   -- SMS verification
    'EMAIL',                 -- Email verification
    'TOTP',                  -- Time-based OTP (Google Authenticator)
    'HARDWARE_TOKEN',        -- Hardware security key
    'BIOMETRIC'              -- Biometric authentication
);

-- =============================================================================
-- ENCRYPTION FUNCTIONS
-- =============================================================================

-- Function to encrypt sensitive data using AES-256-GCM
CREATE OR REPLACE FUNCTION encrypt_sensitive_data(
    plaintext TEXT,
    encryption_key TEXT DEFAULT current_setting('app.encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    IF plaintext IS NULL OR plaintext = '' THEN
        RETURN NULL;
    END IF;

    -- Use AES-256-GCM with random IV
    RETURN encode(
        encrypt_iv(
            plaintext::bytea,
            decode(encryption_key, 'hex'),
            gen_random_bytes(16),  -- 16-byte IV for AES
            'aes-gcm'
        ),
        'hex'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to decrypt sensitive data
CREATE OR REPLACE FUNCTION decrypt_sensitive_data(
    ciphertext TEXT,
    encryption_key TEXT DEFAULT current_setting('app.encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    IF ciphertext IS NULL OR ciphertext = '' THEN
        RETURN NULL;
    END IF;

    RETURN convert_from(
        decrypt_iv(
            decode(ciphertext, 'hex'),
            decode(encryption_key, 'hex'),
            'aes-gcm'
        ),
        'UTF8'
    );
EXCEPTION
    WHEN OTHERS THEN
        -- Return null if decryption fails (corrupted data, wrong key, etc.)
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to hash passwords using bcrypt
CREATE OR REPLACE FUNCTION hash_password(password TEXT) RETURNS TEXT AS $$
BEGIN
    RETURN crypt(password, gen_salt('bf', 12));  -- bcrypt with cost 12
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to verify password
CREATE OR REPLACE FUNCTION verify_password(password TEXT, hash TEXT) RETURNS BOOLEAN AS $$
BEGIN
    RETURN hash = crypt(password, hash);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================================================
-- AUDIT TRIGGER FUNCTION
-- =============================================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- TABLE: users
-- =============================================================================

CREATE TABLE users (
    -- Primary identification
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Login credentials
    email VARCHAR(320) NOT NULL UNIQUE,  -- RFC 5321 maximum length
    password_hash TEXT NOT NULL,

    -- Encrypted personal information (PII)
    encrypted_first_name TEXT,           -- Encrypted with AES-256-GCM
    encrypted_last_name TEXT,            -- Encrypted with AES-256-GCM
    encrypted_phone_number TEXT,         -- Encrypted with AES-256-GCM
    encrypted_tckn TEXT,                 -- Turkish Citizenship Number (encrypted)

    -- Account information
    account_type account_type NOT NULL DEFAULT 'INDIVIDUAL',
    user_status user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    kyc_status kyc_status NOT NULL DEFAULT 'NOT_STARTED',

    -- Verification flags
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,

    -- Preferences and settings (JSONB for flexibility)
    preferences JSONB NOT NULL DEFAULT '{
        "language": "tr",
        "timezone": "Europe/Istanbul",
        "currency": "TRY",
        "notifications": {
            "email": true,
            "sms": false,
            "push": true,
            "trading_alerts": true,
            "news_updates": false,
            "marketing": false
        },
        "trading": {
            "default_order_type": "LIMIT",
            "confirmation_required": true,
            "advanced_interface": false
        }
    }',

    -- Security settings
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_methods JSONB NOT NULL DEFAULT '[]',  -- Array of enabled MFA methods
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,

    -- Compliance and KYC data
    kyc_data JSONB,  -- Store KYC documents metadata, verification results
    compliance_notes TEXT,
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 100),

    -- Address information (can be multiple addresses)
    addresses JSONB NOT NULL DEFAULT '[]',

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,  -- Soft delete

    -- Constraints
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_login_attempts CHECK (login_attempts >= 0),
    CONSTRAINT users_soft_delete_check CHECK (
        (deleted_at IS NULL AND user_status != 'CLOSED') OR
        (deleted_at IS NOT NULL AND user_status = 'CLOSED')
    )
);

-- =============================================================================
-- TABLE: user_sessions
-- =============================================================================

CREATE TABLE user_sessions (
    -- Primary identification
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,

    -- Session information
    session_token_hash TEXT NOT NULL UNIQUE,  -- Hashed session token
    refresh_token_hash TEXT UNIQUE,           -- Hashed refresh token
    session_type session_type NOT NULL DEFAULT 'WEB',

    -- Device and location information
    device_info JSONB NOT NULL DEFAULT '{}',  -- Browser, OS, device fingerprint
    ip_address INET,
    user_agent TEXT,
    geolocation JSONB,  -- Country, city, coordinates (if available)

    -- Session lifecycle
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,  -- Session termination time

    -- Security flags
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    force_logout BOOLEAN NOT NULL DEFAULT FALSE,  -- Force logout on next request

    -- Constraints
    CONSTRAINT user_sessions_valid_duration CHECK (expires_at > created_at),
    CONSTRAINT user_sessions_activity_check CHECK (last_activity_at >= created_at),
    CONSTRAINT user_sessions_end_check CHECK (
        (ended_at IS NULL AND is_active = TRUE) OR
        (ended_at IS NOT NULL AND is_active = FALSE)
    )
);

-- =============================================================================
-- TABLE: organizations
-- =============================================================================

CREATE TABLE organizations (
    -- Primary identification
    organization_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Organization details
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_number VARCHAR(20),  -- Turkish tax number or international equivalent
    trade_registry_number VARCHAR(50),

    -- Contact information
    email VARCHAR(320),
    phone VARCHAR(20),
    website VARCHAR(255),

    -- Address information
    address JSONB NOT NULL DEFAULT '{}',

    -- Organization settings
    settings JSONB NOT NULL DEFAULT '{
        "trading_limits": {
            "daily_limit": 1000000,
            "position_limit": 5000000,
            "leverage_limit": 3
        },
        "compliance": {
            "kyc_required": true,
            "dual_approval": false,
            "trade_reporting": true
        }
    }',

    -- Status and verification
    status user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_data JSONB,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,  -- Soft delete

    -- Constraints
    CONSTRAINT organizations_name_length CHECK (char_length(name) >= 2),
    CONSTRAINT organizations_email_format CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    ),
    CONSTRAINT organizations_website_format CHECK (
        website IS NULL OR website ~* '^https?://[A-Za-z0-9.-]+\.[A-Za-z]{2,}'
    )
);

-- =============================================================================
-- TABLE: organization_members
-- =============================================================================

CREATE TABLE organization_members (
    -- Primary identification
    member_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(organization_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,

    -- Role and permissions
    role organization_role NOT NULL DEFAULT 'VIEWER',
    permissions JSONB NOT NULL DEFAULT '[]',  -- Additional granular permissions

    -- Member settings
    trading_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_limit DECIMAL(20,2),
    position_limit DECIMAL(20,2),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    invited_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    joined_at TIMESTAMP WITH TIME ZONE,
    left_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT organization_members_unique UNIQUE (organization_id, user_id),
    CONSTRAINT organization_members_limits_positive CHECK (
        daily_limit IS NULL OR daily_limit > 0
    ),
    CONSTRAINT organization_members_position_positive CHECK (
        position_limit IS NULL OR position_limit > 0
    ),
    CONSTRAINT organization_members_status_check CHECK (
        (is_active = TRUE AND left_at IS NULL) OR
        (is_active = FALSE AND left_at IS NOT NULL)
    )
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

-- Users table indexes
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(user_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_kyc_status ON users(kyc_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login ON users(last_login_at);
CREATE INDEX idx_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX idx_users_soft_delete ON users(deleted_at) WHERE deleted_at IS NOT NULL;

-- Partial index for active users only (most common queries)
CREATE INDEX idx_users_active ON users(user_id, email, user_status)
    WHERE deleted_at IS NULL AND user_status = 'ACTIVE';

-- JSONB indexes for preferences
CREATE INDEX idx_users_preferences_gin ON users USING GIN(preferences);
CREATE INDEX idx_users_kyc_data_gin ON users USING GIN(kyc_data) WHERE kyc_data IS NOT NULL;

-- User sessions indexes
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token_hash ON user_sessions(session_token_hash);
CREATE INDEX idx_user_sessions_refresh_hash ON user_sessions(refresh_token_hash)
    WHERE refresh_token_hash IS NOT NULL;
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_last_activity ON user_sessions(last_activity_at);
CREATE INDEX idx_user_sessions_active ON user_sessions(is_active, user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_sessions_device_gin ON user_sessions USING GIN(device_info);

-- Organizations indexes
CREATE INDEX idx_organizations_name ON organizations(name) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_status ON organizations(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_verified ON organizations(verified) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_tax_number ON organizations(tax_number)
    WHERE tax_number IS NOT NULL AND deleted_at IS NULL;

-- Organization members indexes
CREATE INDEX idx_organization_members_org_id ON organization_members(organization_id);
CREATE INDEX idx_organization_members_user_id ON organization_members(user_id);
CREATE INDEX idx_organization_members_role ON organization_members(role);
CREATE INDEX idx_organization_members_active ON organization_members(is_active, organization_id)
    WHERE is_active = TRUE;

-- =============================================================================
-- TRIGGERS
-- =============================================================================

-- Add updated_at triggers for all tables
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_organization_members_updated_at
    BEFORE UPDATE ON organization_members
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- ROW LEVEL SECURITY (RLS)
-- =============================================================================

-- Enable RLS on all tables for enhanced security
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_members ENABLE ROW LEVEL SECURITY;

-- Create policies for application users
-- Note: These policies should be customized based on your application's security requirements

-- Users can only see their own data
CREATE POLICY users_own_data_policy ON users
    FOR ALL TO app_user
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- Users can only see their own sessions
CREATE POLICY user_sessions_own_data_policy ON user_sessions
    FOR ALL TO app_user
    USING (user_id = current_setting('app.current_user_id')::UUID);

-- Users can see organizations they belong to
CREATE POLICY organizations_member_policy ON organizations
    FOR SELECT TO app_user
    USING (organization_id IN (
        SELECT organization_id
        FROM organization_members
        WHERE user_id = current_setting('app.current_user_id')::UUID
        AND is_active = TRUE
    ));

-- Users can see organization members of their organizations
CREATE POLICY organization_members_same_org_policy ON organization_members
    FOR SELECT TO app_user
    USING (organization_id IN (
        SELECT organization_id
        FROM organization_members
        WHERE user_id = current_setting('app.current_user_id')::UUID
        AND is_active = TRUE
    ));

-- =============================================================================
-- COMMENTS FOR DOCUMENTATION
-- =============================================================================

-- Table comments
COMMENT ON TABLE users IS 'User accounts with encrypted PII data, KYC compliance, and security features';
COMMENT ON TABLE user_sessions IS 'Active user sessions with device tracking and security controls';
COMMENT ON TABLE organizations IS 'Corporate entities with trading capabilities and compliance settings';
COMMENT ON TABLE organization_members IS 'User memberships in organizations with role-based permissions';

-- Important column comments
COMMENT ON COLUMN users.encrypted_first_name IS 'AES-256-GCM encrypted first name for PII protection';
COMMENT ON COLUMN users.encrypted_last_name IS 'AES-256-GCM encrypted last name for PII protection';
COMMENT ON COLUMN users.encrypted_phone_number IS 'AES-256-GCM encrypted phone number for PII protection';
COMMENT ON COLUMN users.encrypted_tckn IS 'AES-256-GCM encrypted Turkish Citizenship Number';
COMMENT ON COLUMN users.preferences IS 'JSONB user preferences including notifications and trading settings';
COMMENT ON COLUMN users.kyc_data IS 'JSONB KYC documentation metadata and verification results';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp - NULL means active user';

COMMENT ON COLUMN user_sessions.session_token_hash IS 'Bcrypt hashed session token for security';
COMMENT ON COLUMN user_sessions.device_info IS 'JSONB device fingerprint and browser information';
COMMENT ON COLUMN user_sessions.force_logout IS 'Flag to force user logout on next request';

COMMENT ON COLUMN organizations.settings IS 'JSONB organization-level trading limits and compliance settings';
COMMENT ON COLUMN organization_members.permissions IS 'JSONB additional granular permissions beyond role';

-- =============================================================================
-- INITIAL DATA
-- =============================================================================

-- Create a system admin role for initial setup
INSERT INTO users (
    user_id,
    email,
    password_hash,
    encrypted_first_name,
    encrypted_last_name,
    account_type,
    user_status,
    kyc_status,
    email_verified,
    preferences
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@bisttrading.com',
    hash_password('TempPassword123!'),  -- Should be changed immediately
    encrypt_sensitive_data('System'),
    encrypt_sensitive_data('Administrator'),
    'INSTITUTIONAL',
    'ACTIVE',
    'APPROVED',
    TRUE,
    '{
        "language": "en",
        "timezone": "Europe/Istanbul",
        "currency": "TRY",
        "notifications": {
            "email": true,
            "sms": false,
            "push": false,
            "trading_alerts": false,
            "news_updates": false,
            "marketing": false
        },
        "trading": {
            "default_order_type": "LIMIT",
            "confirmation_required": false,
            "advanced_interface": true
        },
        "admin": true
    }'
) ON CONFLICT DO NOTHING;

-- =============================================================================
-- SECURITY NOTES
-- =============================================================================

/*
IMPORTANT SECURITY CONSIDERATIONS:

1. Encryption Key Management:
   - The encryption key should be stored securely outside the database
   - Use environment variables or a dedicated key management service
   - Rotate encryption keys regularly with proper migration procedures

2. Password Security:
   - Bcrypt cost factor is set to 12 (recommended minimum)
   - Consider increasing based on your security requirements and server performance
   - Implement password complexity rules in application logic

3. Session Management:
   - Session tokens should be cryptographically random
   - Implement proper session timeout and cleanup procedures
   - Consider implementing IP address validation for sessions

4. Row Level Security:
   - The RLS policies provided are basic examples
   - Customize policies based on your specific security requirements
   - Test policies thoroughly in your application context

5. Database Permissions:
   - Create specific database users for your application
   - Grant minimal required permissions
   - Never use superuser accounts for application connections

6. Audit and Monitoring:
   - Implement audit logging for sensitive operations
   - Monitor failed login attempts and suspicious activities
   - Set up alerting for security-related events

7. Compliance:
   - Ensure GDPR/KVKK compliance for PII data handling
   - Implement data retention and deletion policies
   - Maintain audit trails for regulatory requirements
*/