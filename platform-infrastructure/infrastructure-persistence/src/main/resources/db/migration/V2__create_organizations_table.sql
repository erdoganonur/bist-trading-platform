-- Create organizations table
-- This table stores organization information for trading entities

CREATE TABLE organizations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    organization_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    organization_type VARCHAR(20) NOT NULL CHECK (organization_type IN ('BROKER', 'BANK', 'INVESTMENT_FIRM', 'ASSET_MANAGER', 'FUND_MANAGER', 'FINTECH', 'TRADING_FIRM', 'MARKET_MAKER', 'CUSTODIAN', 'OTHER')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'CLOSED')),
    tax_id VARCHAR(500), -- Encrypted field
    trade_registry_no VARCHAR(500), -- Encrypted field
    website_url VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(500), -- Encrypted field
    fax_number VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(3) DEFAULT 'TUR',
    license_number VARCHAR(100),
    license_authority VARCHAR(100),
    license_issued_at TIMESTAMP,
    license_expires_at TIMESTAMP,
    trading_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_users INTEGER CHECK (max_users > 0),
    current_users INTEGER NOT NULL DEFAULT 0 CHECK (current_users >= 0),
    contact_person VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(500), -- Encrypted field
    settings JSONB, -- Organization settings
    metadata JSONB, -- Additional metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_organizations_code ON organizations(organization_code);
CREATE INDEX idx_organizations_name ON organizations(name);
CREATE INDEX idx_organizations_type ON organizations(organization_type);
CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_created_at ON organizations(created_at);
CREATE INDEX idx_organizations_country ON organizations(country);
CREATE INDEX idx_organizations_city ON organizations(city) WHERE city IS NOT NULL;
CREATE INDEX idx_organizations_trading_enabled ON organizations(trading_enabled);
CREATE INDEX idx_organizations_deleted_at ON organizations(deleted_at) WHERE deleted_at IS NOT NULL;

-- Create compound indexes for common query patterns
CREATE INDEX idx_organizations_status_active ON organizations(status, deleted_at) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_organizations_trading_status ON organizations(status, trading_enabled) WHERE status = 'ACTIVE' AND trading_enabled = TRUE;
CREATE INDEX idx_organizations_license_expiry ON organizations(license_expires_at) WHERE license_expires_at IS NOT NULL;

-- Create partial indexes for specific use cases
CREATE INDEX idx_organizations_valid_license ON organizations(license_expires_at) WHERE license_expires_at > CURRENT_TIMESTAMP AND license_number IS NOT NULL;
CREATE INDEX idx_organizations_near_capacity ON organizations(current_users, max_users) WHERE max_users IS NOT NULL;

-- Add foreign key constraint to users table
ALTER TABLE users ADD CONSTRAINT fk_users_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Create index on the foreign key for better performance
CREATE INDEX idx_users_organization_fk ON users(organization_id) WHERE organization_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE organizations IS 'Organizations and trading entities in BIST Trading Platform';
COMMENT ON COLUMN organizations.id IS 'Primary key - UUID format';
COMMENT ON COLUMN organizations.organization_code IS 'Unique organization code identifier';
COMMENT ON COLUMN organizations.tax_id IS 'Tax identification number - encrypted with AES-256-GCM';
COMMENT ON COLUMN organizations.trade_registry_no IS 'Trade registry number - encrypted with AES-256-GCM';
COMMENT ON COLUMN organizations.phone_number IS 'Organization phone - encrypted with AES-256-GCM';
COMMENT ON COLUMN organizations.contact_phone IS 'Contact person phone - encrypted with AES-256-GCM';
COMMENT ON COLUMN organizations.current_users IS 'Current number of active users in organization';
COMMENT ON COLUMN organizations.max_users IS 'Maximum allowed users (NULL = unlimited)';
COMMENT ON COLUMN organizations.settings IS 'Organization configuration stored as JSONB';
COMMENT ON COLUMN organizations.metadata IS 'Additional organization metadata stored as JSONB';
COMMENT ON COLUMN organizations.deleted_at IS 'Soft delete timestamp - NULL means not deleted';

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER trigger_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to validate user capacity
CREATE OR REPLACE FUNCTION check_organization_user_capacity()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if organization has reached maximum user capacity
    IF NEW.current_users IS NOT NULL THEN
        DECLARE
            org_max_users INTEGER;
        BEGIN
            SELECT max_users INTO org_max_users FROM organizations WHERE id = NEW.organization_id;

            IF org_max_users IS NOT NULL AND NEW.current_users > org_max_users THEN
                RAISE EXCEPTION 'Organization has reached maximum user capacity of %', org_max_users;
            END IF;
        END;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to enforce user capacity limits
-- This will be activated when organizations are updated with user counts
CREATE TRIGGER trigger_organization_capacity_check
    BEFORE UPDATE OF current_users ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION check_organization_user_capacity();

-- Create view for active organizations with license status
CREATE VIEW active_organizations_view AS
SELECT
    o.*,
    CASE
        WHEN o.license_expires_at IS NULL THEN 'NO_LICENSE'
        WHEN o.license_expires_at > CURRENT_TIMESTAMP THEN 'VALID'
        WHEN o.license_expires_at > CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 'EXPIRED'
        ELSE 'EXPIRED_LONG'
    END as license_status,
    CASE
        WHEN o.max_users IS NULL THEN NULL
        ELSE (o.max_users - o.current_users)
    END as available_user_slots,
    CASE
        WHEN o.max_users IS NULL THEN 0
        ELSE ROUND((o.current_users::DECIMAL / o.max_users::DECIMAL) * 100, 2)
    END as capacity_percentage
FROM organizations o
WHERE o.status = 'ACTIVE' AND o.deleted_at IS NULL;

COMMENT ON VIEW active_organizations_view IS 'View of active organizations with calculated license and capacity status';