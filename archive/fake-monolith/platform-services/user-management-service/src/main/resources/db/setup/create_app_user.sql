-- =============================================================================
-- BIST Trading Platform - Database User Setup
-- Description: Create application database user with proper privileges
-- =============================================================================

-- This script should be run by a PostgreSQL superuser (e.g., postgres)
-- to create the application database user and set up proper permissions.

-- =============================================================================
-- CREATE DATABASE AND APPLICATION USER
-- =============================================================================

-- Create the database if it doesn't exist
-- Note: This command needs to be run outside a transaction block
-- You may need to run this manually: CREATE DATABASE bist_trading;

-- Connect to the target database
\c bist_trading;

-- Create application user with secure password
-- IMPORTANT: Change this password in production!
CREATE USER app_user WITH
    PASSWORD 'SecureAppPassword123!'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    CONNECTION LIMIT 50;  -- Limit concurrent connections

-- Create read-only user for reporting/analytics
CREATE USER app_readonly WITH
    PASSWORD 'SecureReadOnlyPassword123!'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    CONNECTION LIMIT 10;

-- Create backup user for maintenance operations
CREATE USER app_backup WITH
    PASSWORD 'SecureBackupPassword123!'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    CONNECTION LIMIT 5;

-- =============================================================================
-- GRANT BASIC PERMISSIONS
-- =============================================================================

-- Grant connection to database
GRANT CONNECT ON DATABASE bist_trading TO app_user;
GRANT CONNECT ON DATABASE bist_trading TO app_readonly;
GRANT CONNECT ON DATABASE bist_trading TO app_backup;

-- Grant usage on public schema
GRANT USAGE ON SCHEMA public TO app_user;
GRANT USAGE ON SCHEMA public TO app_readonly;
GRANT USAGE ON SCHEMA public TO app_backup;

-- =============================================================================
-- APPLICATION USER PERMISSIONS (READ/WRITE)
-- =============================================================================

-- Grant table permissions for application user
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Grant permissions on future tables and sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- Grant execute permissions on functions
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS TO app_user;

-- =============================================================================
-- READ-ONLY USER PERMISSIONS
-- =============================================================================

-- Grant select permissions for read-only user
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_readonly;

-- Grant permissions on future tables and sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO app_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_readonly;

-- Grant execute permissions on read-only functions only
GRANT EXECUTE ON FUNCTION decrypt_sensitive_data(TEXT) TO app_readonly;
GRANT EXECUTE ON FUNCTION decrypt_sensitive_data(TEXT, TEXT) TO app_readonly;

-- =============================================================================
-- BACKUP USER PERMISSIONS
-- =============================================================================

-- Grant necessary permissions for backup operations
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_backup;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_backup;
GRANT USAGE ON SCHEMA information_schema TO app_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA information_schema TO app_backup;

-- Grant permissions on future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO app_backup;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_backup;

-- =============================================================================
-- SECURITY CONFIGURATION
-- =============================================================================

-- Set up row level security policies for application user
-- Note: The main migration script enables RLS and creates policies

-- Create a role for RLS context setting
CREATE ROLE app_rls_user;
GRANT app_rls_user TO app_user;

-- Configure application settings for encryption
-- These settings should be set per session by the application
ALTER DATABASE bist_trading SET app.encryption_key TO '';  -- Will be set by application

-- =============================================================================
-- CONNECTION AND PERFORMANCE SETTINGS
-- =============================================================================

-- Configure connection settings for application user
ALTER ROLE app_user SET
    statement_timeout = '30s',
    idle_in_transaction_session_timeout = '60s',
    lock_timeout = '10s',
    log_statement = 'mod',  -- Log all modifications
    log_min_duration_statement = '1s';  -- Log slow queries

-- Configure settings for read-only user
ALTER ROLE app_readonly SET
    default_transaction_isolation = 'repeatable read',
    statement_timeout = '60s',
    idle_in_transaction_session_timeout = '300s',
    log_min_duration_statement = '5s';

-- Configure settings for backup user
ALTER ROLE app_backup SET
    statement_timeout = '0',  -- No timeout for backup operations
    lock_timeout = '0',
    maintenance_work_mem = '1GB',
    work_mem = '256MB';

-- =============================================================================
-- MONITORING SETUP
-- =============================================================================

-- Grant permissions to monitor database statistics
GRANT SELECT ON pg_stat_user_tables TO app_user, app_readonly;
GRANT SELECT ON pg_stat_user_indexes TO app_user, app_readonly;
GRANT SELECT ON pg_stat_activity TO app_user, app_readonly;
GRANT SELECT ON pg_stat_statements TO app_user, app_readonly;

-- =============================================================================
-- CREATE UTILITY FUNCTIONS FOR APPLICATION
-- =============================================================================

-- Function to set current user context for RLS
CREATE OR REPLACE FUNCTION set_current_user_id(user_uuid UUID)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_user_id', user_uuid::TEXT, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission to application user
GRANT EXECUTE ON FUNCTION set_current_user_id(UUID) TO app_user;

-- Function to get current user context
CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_user_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission to application user
GRANT EXECUTE ON FUNCTION get_current_user_id() TO app_user;

-- Function to safely set encryption key (should only be called by application startup)
CREATE OR REPLACE FUNCTION set_encryption_key(key_hex TEXT)
RETURNS VOID AS $$
BEGIN
    -- Validate key format (should be 64 hex characters for 256-bit key)
    IF length(key_hex) != 64 OR key_hex !~ '^[0-9A-Fa-f]+$' THEN
        RAISE EXCEPTION 'Invalid encryption key format. Expected 64 hexadecimal characters.';
    END IF;

    PERFORM set_config('app.encryption_key', key_hex, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Only application user can set encryption key
GRANT EXECUTE ON FUNCTION set_encryption_key(TEXT) TO app_user;

-- =============================================================================
-- AUDIT SETUP
-- =============================================================================

-- Create audit log table for security events
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    session_id UUID,
    action VARCHAR(50) NOT NULL,
    table_name VARCHAR(50),
    record_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Grant permissions on audit table
GRANT INSERT ON audit_log TO app_user;
GRANT SELECT ON audit_log TO app_user, app_readonly;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);

-- =============================================================================
-- SUMMARY
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Database setup completed successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Created users:';
    RAISE NOTICE '  - app_user: Full read/write access for application';
    RAISE NOTICE '  - app_readonly: Read-only access for reporting';
    RAISE NOTICE '  - app_backup: Backup and maintenance operations';
    RAISE NOTICE '';
    RAISE NOTICE 'Security features enabled:';
    RAISE NOTICE '  - Row Level Security (RLS) policies';
    RAISE NOTICE '  - Connection limits per user role';
    RAISE NOTICE '  - Statement timeouts and logging';
    RAISE NOTICE '  - Audit logging infrastructure';
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANT SECURITY NOTES:';
    RAISE NOTICE '  1. Change all default passwords immediately!';
    RAISE NOTICE '  2. Configure app.encryption_key in your application';
    RAISE NOTICE '  3. Set up proper network security (pg_hba.conf)';
    RAISE NOTICE '  4. Enable SSL/TLS for database connections';
    RAISE NOTICE '  5. Regularly rotate passwords and encryption keys';
    RAISE NOTICE '=============================================================================';
END $$;