-- =============================================================================
-- BIST Trading Platform - User Management Schema Rollback
-- Version: 1.0.0
-- Description: Rollback script for user management schema migration V001
-- =============================================================================

-- WARNING: This script will permanently delete all user management data!
-- Only run this in development/test environments or as part of a planned rollback.

-- =============================================================================
-- DROP ROW LEVEL SECURITY POLICIES
-- =============================================================================

-- Drop RLS policies
DROP POLICY IF EXISTS users_own_data_policy ON users;
DROP POLICY IF EXISTS user_sessions_own_data_policy ON user_sessions;
DROP POLICY IF EXISTS organizations_member_policy ON organizations;
DROP POLICY IF EXISTS organization_members_same_org_policy ON organization_members;

-- Disable RLS
ALTER TABLE IF EXISTS users DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS user_sessions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS organizations DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS organization_members DISABLE ROW LEVEL SECURITY;

-- =============================================================================
-- DROP TRIGGERS
-- =============================================================================

DROP TRIGGER IF EXISTS trigger_users_updated_at ON users;
DROP TRIGGER IF EXISTS trigger_organizations_updated_at ON organizations;
DROP TRIGGER IF EXISTS trigger_organization_members_updated_at ON organization_members;

-- =============================================================================
-- DROP INDEXES
-- =============================================================================

-- Organization members indexes
DROP INDEX IF EXISTS idx_organization_members_org_id;
DROP INDEX IF EXISTS idx_organization_members_user_id;
DROP INDEX IF EXISTS idx_organization_members_role;
DROP INDEX IF EXISTS idx_organization_members_active;

-- Organizations indexes
DROP INDEX IF EXISTS idx_organizations_name;
DROP INDEX IF EXISTS idx_organizations_status;
DROP INDEX IF EXISTS idx_organizations_verified;
DROP INDEX IF EXISTS idx_organizations_tax_number;

-- User sessions indexes
DROP INDEX IF EXISTS idx_user_sessions_user_id;
DROP INDEX IF EXISTS idx_user_sessions_token_hash;
DROP INDEX IF EXISTS idx_user_sessions_refresh_hash;
DROP INDEX IF EXISTS idx_user_sessions_expires_at;
DROP INDEX IF EXISTS idx_user_sessions_last_activity;
DROP INDEX IF EXISTS idx_user_sessions_active;
DROP INDEX IF EXISTS idx_user_sessions_device_gin;

-- Users table indexes
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_status;
DROP INDEX IF EXISTS idx_users_kyc_status;
DROP INDEX IF EXISTS idx_users_created_at;
DROP INDEX IF EXISTS idx_users_last_login;
DROP INDEX IF EXISTS idx_users_locked_until;
DROP INDEX IF EXISTS idx_users_soft_delete;
DROP INDEX IF EXISTS idx_users_active;
DROP INDEX IF EXISTS idx_users_preferences_gin;
DROP INDEX IF EXISTS idx_users_kyc_data_gin;

-- =============================================================================
-- DROP TABLES (in reverse order due to foreign key dependencies)
-- =============================================================================

DROP TABLE IF EXISTS organization_members CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
DROP TABLE IF EXISTS user_sessions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =============================================================================
-- DROP FUNCTIONS
-- =============================================================================

DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS verify_password(TEXT, TEXT);
DROP FUNCTION IF EXISTS hash_password(TEXT);
DROP FUNCTION IF EXISTS decrypt_sensitive_data(TEXT, TEXT);
DROP FUNCTION IF EXISTS decrypt_sensitive_data(TEXT);
DROP FUNCTION IF EXISTS encrypt_sensitive_data(TEXT, TEXT);
DROP FUNCTION IF EXISTS encrypt_sensitive_data(TEXT);

-- =============================================================================
-- DROP ENUM TYPES
-- =============================================================================

DROP TYPE IF EXISTS mfa_method CASCADE;
DROP TYPE IF EXISTS session_type CASCADE;
DROP TYPE IF EXISTS organization_role CASCADE;
DROP TYPE IF EXISTS kyc_status CASCADE;
DROP TYPE IF EXISTS account_type CASCADE;
DROP TYPE IF EXISTS user_status CASCADE;

-- =============================================================================
-- NOTE ON EXTENSIONS
-- =============================================================================

/*
The following extensions are NOT dropped as they may be used by other parts of the system:
- uuid-ossp
- pgcrypto
- pg_stat_statements

If you need to remove these extensions entirely, run manually:

DROP EXTENSION IF EXISTS "pg_stat_statements";
DROP EXTENSION IF EXISTS "pgcrypto";
DROP EXTENSION IF EXISTS "uuid-ossp";

WARNING: Only drop extensions if you're sure no other parts of your system use them!
*/

-- =============================================================================
-- ROLLBACK VERIFICATION
-- =============================================================================

-- Verify that all objects have been dropped
DO $$
DECLARE
    remaining_tables INTEGER;
    remaining_functions INTEGER;
    remaining_types INTEGER;
BEGIN
    -- Check for remaining tables
    SELECT COUNT(*)
    INTO remaining_tables
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_name IN ('users', 'user_sessions', 'organizations', 'organization_members');

    -- Check for remaining functions
    SELECT COUNT(*)
    INTO remaining_functions
    FROM information_schema.routines
    WHERE routine_schema = 'public'
    AND routine_name IN (
        'encrypt_sensitive_data',
        'decrypt_sensitive_data',
        'hash_password',
        'verify_password',
        'update_updated_at_column'
    );

    -- Check for remaining types
    SELECT COUNT(*)
    INTO remaining_types
    FROM information_schema.data_type_privileges
    WHERE object_name IN (
        'user_status',
        'account_type',
        'kyc_status',
        'organization_role',
        'session_type',
        'mfa_method'
    );

    -- Report results
    RAISE NOTICE 'Rollback verification:';
    RAISE NOTICE 'Remaining tables: %', remaining_tables;
    RAISE NOTICE 'Remaining functions: %', remaining_functions;
    RAISE NOTICE 'Remaining types: %', remaining_types;

    IF remaining_tables = 0 AND remaining_functions = 0 AND remaining_types = 0 THEN
        RAISE NOTICE '✅ Rollback completed successfully - all objects removed';
    ELSE
        RAISE WARNING '⚠️  Some objects may not have been removed completely';
    END IF;
END $$;