-- =============================================================================
-- BIST Trading Platform - Initial Database Schema
-- Version: V1 - Initial schema creation with core tables
-- =============================================================================

-- =============================================================================
-- EXTENSIONS
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- CORE REFERENCE TABLES
-- =============================================================================

-- Market definitions
CREATE TABLE reference.markets (
    market_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    market_code VARCHAR(10) NOT NULL UNIQUE,
    market_name VARCHAR(100) NOT NULL,
    country_code VARCHAR(3) NOT NULL DEFAULT 'TR',
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_market_code CHECK (LENGTH(market_code) >= 2),
    CONSTRAINT chk_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_country_code CHECK (LENGTH(country_code) = 3)
);

-- Create index for market lookups
CREATE INDEX idx_markets_code ON reference.markets (market_code) WHERE is_active = true;
CREATE INDEX idx_markets_active ON reference.markets (is_active);

-- Sectors classification
CREATE TABLE reference.sectors (
    sector_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sector_code VARCHAR(20) NOT NULL UNIQUE,
    sector_name VARCHAR(200) NOT NULL,
    parent_sector_id UUID REFERENCES reference.sectors(sector_id),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_sector_code CHECK (LENGTH(sector_code) >= 2)
);

CREATE INDEX idx_sectors_code ON reference.sectors (sector_code) WHERE is_active = true;
CREATE INDEX idx_sectors_parent ON reference.sectors (parent_sector_id);
CREATE INDEX idx_sectors_active ON reference.sectors (is_active);

-- Trading symbols
CREATE TABLE reference.symbols (
    symbol_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol VARCHAR(20) NOT NULL,
    market_id UUID NOT NULL REFERENCES reference.markets(market_id),
    sector_id UUID REFERENCES reference.sectors(sector_id),
    full_name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    isin_code VARCHAR(12),

    -- Trading parameters
    lot_size INTEGER NOT NULL DEFAULT 1,
    tick_size DECIMAL(10,6) NOT NULL DEFAULT 0.01,
    min_order_quantity INTEGER NOT NULL DEFAULT 1,
    max_order_quantity INTEGER NOT NULL DEFAULT 999999999,
    price_precision INTEGER NOT NULL DEFAULT 2,
    quantity_precision INTEGER NOT NULL DEFAULT 0,

    -- Status and configuration
    is_tradeable BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    data_feed_enabled BOOLEAN NOT NULL DEFAULT true,

    -- Metadata
    listing_date DATE,
    delisting_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_symbols_market UNIQUE (symbol, market_id),
    CONSTRAINT chk_symbol_length CHECK (LENGTH(symbol) >= 2 AND LENGTH(symbol) <= 20),
    CONSTRAINT chk_isin_format CHECK (isin_code IS NULL OR LENGTH(isin_code) = 12),
    CONSTRAINT chk_lot_size CHECK (lot_size > 0),
    CONSTRAINT chk_tick_size CHECK (tick_size > 0),
    CONSTRAINT chk_min_quantity CHECK (min_order_quantity > 0),
    CONSTRAINT chk_max_quantity CHECK (max_order_quantity >= min_order_quantity),
    CONSTRAINT chk_precision CHECK (price_precision >= 0 AND price_precision <= 8 AND quantity_precision >= 0 AND quantity_precision <= 8),
    CONSTRAINT chk_listing_delisting CHECK (delisting_date IS NULL OR delisting_date > listing_date)
);

-- Indexes for symbol lookups
CREATE UNIQUE INDEX idx_symbols_symbol_market ON reference.symbols (symbol, market_id);
CREATE INDEX idx_symbols_symbol ON reference.symbols (symbol) WHERE is_active = true;
CREATE INDEX idx_symbols_market ON reference.symbols (market_id) WHERE is_active = true;
CREATE INDEX idx_symbols_sector ON reference.symbols (sector_id);
CREATE INDEX idx_symbols_tradeable ON reference.symbols (is_tradeable) WHERE is_tradeable = true;
CREATE INDEX idx_symbols_isin ON reference.symbols (isin_code) WHERE isin_code IS NOT NULL;

-- =============================================================================
-- USER MANAGEMENT TABLES
-- =============================================================================

-- User roles
CREATE TABLE user_management.roles (
    role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_role_name CHECK (LENGTH(role_name) >= 2)
);

CREATE INDEX idx_roles_name ON user_management.roles (role_name) WHERE is_active = true;

-- Users table
CREATE TABLE user_management.users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Authentication
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,

    -- Personal Information (encrypted)
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    tc_kimlik_no VARCHAR(11),
    birth_date DATE,

    -- Address
    address TEXT,
    city VARCHAR(100),
    postal_code VARCHAR(10),
    country VARCHAR(100) DEFAULT 'Türkiye',

    -- Trading Information
    trading_account_no VARCHAR(50),
    risk_profile VARCHAR(20) DEFAULT 'CONSERVATIVE',

    -- Status and Security
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_email_verified BOOLEAN NOT NULL DEFAULT false,
    is_phone_verified BOOLEAN NOT NULL DEFAULT false,
    is_kyc_completed BOOLEAN NOT NULL DEFAULT false,
    is_trading_enabled BOOLEAN NOT NULL DEFAULT false,

    -- Security
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    last_password_change TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Preferences
    preferred_language VARCHAR(5) DEFAULT 'tr',
    timezone VARCHAR(50) DEFAULT 'Europe/Istanbul',
    notification_preferences JSONB DEFAULT '{}'::jsonb,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,

    -- Constraints
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_phone_format CHECK (phone IS NULL OR phone ~ '^\+?[0-9\s\-\(\)]{10,20}$'),
    CONSTRAINT chk_tc_kimlik CHECK (tc_kimlik_no IS NULL OR LENGTH(tc_kimlik_no) = 11),
    CONSTRAINT chk_risk_profile CHECK (risk_profile IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    CONSTRAINT chk_language CHECK (preferred_language IN ('tr', 'en')),
    CONSTRAINT chk_failed_attempts CHECK (failed_login_attempts >= 0 AND failed_login_attempts <= 10),
    CONSTRAINT chk_birth_date CHECK (birth_date IS NULL OR birth_date < CURRENT_DATE),
    CONSTRAINT chk_password_change CHECK (last_password_change <= NOW())
);

-- Indexes for user management
CREATE UNIQUE INDEX idx_users_email ON user_management.users (LOWER(email));
CREATE UNIQUE INDEX idx_users_phone ON user_management.users (phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX idx_users_tc_kimlik ON user_management.users (tc_kimlik_no) WHERE tc_kimlik_no IS NOT NULL;
CREATE INDEX idx_users_active ON user_management.users (is_active);
CREATE INDEX idx_users_trading_enabled ON user_management.users (is_trading_enabled) WHERE is_trading_enabled = true;
CREATE INDEX idx_users_last_login ON user_management.users (last_login_at);
CREATE INDEX idx_users_created_at ON user_management.users (created_at);

-- User roles association
CREATE TABLE user_management.user_roles (
    user_id UUID NOT NULL REFERENCES user_management.users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES user_management.roles(role_id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES user_management.users(user_id),
    is_active BOOLEAN NOT NULL DEFAULT true,

    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_management.user_roles (user_id) WHERE is_active = true;
CREATE INDEX idx_user_roles_role ON user_management.user_roles (role_id) WHERE is_active = true;

-- User sessions
CREATE TABLE user_management.user_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_management.users(user_id) ON DELETE CASCADE,

    -- Session details
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255),

    -- Session metadata
    ip_address INET,
    user_agent TEXT,
    device_info JSONB DEFAULT '{}'::jsonb,

    -- Timing
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    logout_reason VARCHAR(50),

    CONSTRAINT chk_expires_at CHECK (expires_at > created_at)
);

CREATE INDEX idx_user_sessions_token ON user_management.user_sessions (token_hash);
CREATE INDEX idx_user_sessions_user ON user_management.user_sessions (user_id) WHERE is_active = true;
CREATE INDEX idx_user_sessions_expires ON user_management.user_sessions (expires_at);
CREATE INDEX idx_user_sessions_active ON user_management.user_sessions (is_active, last_accessed_at);

-- =============================================================================
-- AUDIT AND LOGGING TABLES
-- =============================================================================

-- System audit log
CREATE TABLE audit.audit_log (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- What happened
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    record_id UUID,

    -- When and who
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID REFERENCES user_management.users(user_id),
    session_id UUID REFERENCES user_management.user_sessions(session_id),

    -- How
    ip_address INET,
    user_agent TEXT,

    -- Data changes
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],

    -- Context
    business_context VARCHAR(100),
    additional_info JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT chk_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE', 'SELECT')),
    CONSTRAINT chk_table_name CHECK (LENGTH(table_name) > 0)
);

-- Partitioned by month for performance
CREATE INDEX idx_audit_log_occurred_at ON audit.audit_log (occurred_at);
CREATE INDEX idx_audit_log_table_operation ON audit.audit_log (table_name, operation);
CREATE INDEX idx_audit_log_user ON audit.audit_log (user_id);
CREATE INDEX idx_audit_log_record ON audit.audit_log (record_id);
CREATE INDEX idx_audit_log_context ON audit.audit_log (business_context);

-- =============================================================================
-- INITIAL DATA
-- =============================================================================

-- Insert default market
INSERT INTO reference.markets (market_code, market_name, country_code, currency, timezone) VALUES
('BIST', 'Borsa İstanbul', 'TUR', 'TRY', 'Europe/Istanbul'),
('NYSE', 'New York Stock Exchange', 'USA', 'USD', 'America/New_York'),
('NASDAQ', 'NASDAQ', 'USA', 'USD', 'America/New_York');

-- Insert basic sectors
INSERT INTO reference.sectors (sector_code, sector_name, description) VALUES
('BANK', 'Bankacılık', 'Bankacılık ve finansal hizmetler'),
('TECH', 'Teknoloji', 'Teknoloji ve yazılım şirketleri'),
('ENERGY', 'Enerji', 'Enerji ve petrol şirketleri'),
('TELECOM', 'Telekomünikasyon', 'Telekomünikasyon hizmetleri'),
('RETAIL', 'Perakende', 'Perakende ve tüketici ürünleri'),
('MANUF', 'İmalat', 'İmalat sanayi'),
('REAL_ESTATE', 'Gayrimenkul', 'Gayrimenkul yatırım ortaklıkları'),
('INSURANCE', 'Sigorta', 'Sigorta şirketleri'),
('TRANSPORT', 'Ulaştırma', 'Ulaştırma ve lojistik'),
('FOOD', 'Gıda', 'Gıda ve içecek sanayi');

-- Insert default roles
INSERT INTO user_management.roles (role_name, description, permissions) VALUES
('ADMIN', 'System Administrator', '["ADMIN", "USER_MANAGEMENT", "SYSTEM_CONFIG"]'::jsonb),
('TRADER', 'Active Trader', '["TRADING", "MARKET_DATA", "PORTFOLIO_VIEW"]'::jsonb),
('VIEWER', 'Market Data Viewer', '["MARKET_DATA", "PORTFOLIO_VIEW"]'::jsonb),
('SUPPORT', 'Customer Support', '["USER_SUPPORT", "VIEW_USERS"]'::jsonb);

-- =============================================================================
-- TRIGGERS FOR UPDATED_AT
-- =============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables with updated_at column
CREATE TRIGGER trigger_markets_updated_at BEFORE UPDATE ON reference.markets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_sectors_updated_at BEFORE UPDATE ON reference.sectors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_symbols_updated_at BEFORE UPDATE ON reference.symbols
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_roles_updated_at BEFORE UPDATE ON user_management.roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_users_updated_at BEFORE UPDATE ON user_management.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- COMMENTS ON TABLES AND COLUMNS
-- =============================================================================

COMMENT ON TABLE reference.markets IS 'Trading markets and exchanges';
COMMENT ON TABLE reference.sectors IS 'Business sectors classification';
COMMENT ON TABLE reference.symbols IS 'Trading symbols and instruments';
COMMENT ON TABLE user_management.roles IS 'User roles and permissions';
COMMENT ON TABLE user_management.users IS 'System users and traders';
COMMENT ON TABLE user_management.user_roles IS 'User role assignments';
COMMENT ON TABLE user_management.user_sessions IS 'Active user sessions';
COMMENT ON TABLE audit.audit_log IS 'System audit trail';

-- Column comments for complex fields
COMMENT ON COLUMN reference.symbols.tick_size IS 'Minimum price movement in trading currency';
COMMENT ON COLUMN reference.symbols.lot_size IS 'Minimum tradeable quantity';
COMMENT ON COLUMN user_management.users.tc_kimlik_no IS 'Turkish citizenship number (encrypted)';
COMMENT ON COLUMN user_management.users.risk_profile IS 'Trading risk assessment level';
COMMENT ON COLUMN audit.audit_log.changed_fields IS 'Array of field names that changed';

-- =============================================================================
-- SCHEMA MIGRATION COMPLETE
-- =============================================================================