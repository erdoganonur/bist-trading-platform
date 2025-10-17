-- =============================================================================
-- BIST Trading Platform - Trading Domain Schema Rollback
-- Version: 2.0.0
-- Description: Rollback script for trading domain schema migration V002
-- =============================================================================

-- WARNING: This script will permanently delete all trading data!
-- Only run this in development/test environments or as part of a planned rollback.

-- =============================================================================
-- DROP VIEWS
-- =============================================================================

DROP VIEW IF EXISTS order_execution_summary CASCADE;
DROP VIEW IF EXISTS portfolio_summary CASCADE;
DROP VIEW IF EXISTS active_orders CASCADE;

-- =============================================================================
-- DROP ROW LEVEL SECURITY POLICIES
-- =============================================================================

-- Drop RLS policies for trading tables
DROP POLICY IF EXISTS broker_accounts_own_data_policy ON broker_accounts;
DROP POLICY IF EXISTS positions_own_data_policy ON positions;
DROP POLICY IF EXISTS order_executions_own_data_policy ON order_executions;
DROP POLICY IF EXISTS orders_own_data_policy ON orders;
DROP POLICY IF EXISTS symbols_public_policy ON symbols;

-- Disable RLS on trading tables
ALTER TABLE IF EXISTS broker_accounts DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS positions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS order_executions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS orders DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS symbols DISABLE ROW LEVEL SECURITY;

-- =============================================================================
-- DROP FUNCTIONS
-- =============================================================================

DROP FUNCTION IF EXISTS update_position_after_trade(UUID, VARCHAR(50), UUID, order_side, INTEGER, DECIMAL(20,6));
DROP FUNCTION IF EXISTS calculate_position_pnl(UUID, DECIMAL(20,6));

-- =============================================================================
-- DROP TRIGGERS
-- =============================================================================

DROP TRIGGER IF EXISTS trigger_broker_accounts_updated_at ON broker_accounts;
DROP TRIGGER IF EXISTS trigger_positions_updated_at ON positions;
DROP TRIGGER IF EXISTS trigger_orders_updated_at ON orders;
DROP TRIGGER IF EXISTS trigger_symbols_updated_at ON symbols;

-- =============================================================================
-- DROP INDEXES
-- =============================================================================

-- Broker accounts indexes
DROP INDEX IF EXISTS idx_broker_accounts_active;
DROP INDEX IF EXISTS idx_broker_accounts_broker;
DROP INDEX IF EXISTS idx_broker_accounts_user_id;

-- Positions table indexes
DROP INDEX IF EXISTS idx_positions_active;
DROP INDEX IF EXISTS idx_positions_side;
DROP INDEX IF EXISTS idx_positions_symbol_id;
DROP INDEX IF EXISTS idx_positions_account_id;
DROP INDEX IF EXISTS idx_positions_user_id;

-- Order executions indexes
DROP INDEX IF EXISTS idx_order_executions_execution_type;
DROP INDEX IF EXISTS idx_order_executions_execution_time;
DROP INDEX IF EXISTS idx_order_executions_trade_id;
DROP INDEX IF EXISTS idx_order_executions_order_id;

-- Orders table indexes
DROP INDEX IF EXISTS idx_orders_metadata_gin;
DROP INDEX IF EXISTS idx_orders_active;
DROP INDEX IF EXISTS idx_orders_account_status;
DROP INDEX IF EXISTS idx_orders_symbol_status;
DROP INDEX IF EXISTS idx_orders_user_status;
DROP INDEX IF EXISTS idx_orders_strategy_id;
DROP INDEX IF EXISTS idx_orders_parent_id;
DROP INDEX IF EXISTS idx_orders_broker_order_id;
DROP INDEX IF EXISTS idx_orders_client_order_id;
DROP INDEX IF EXISTS idx_orders_status;
DROP INDEX IF EXISTS idx_orders_symbol_id;
DROP INDEX IF EXISTS idx_orders_account_id;
DROP INDEX IF EXISTS idx_orders_user_id;

-- Symbols table indexes
DROP INDEX IF EXISTS idx_symbols_metadata_gin;
DROP INDEX IF EXISTS idx_symbols_index_memberships_gin;
DROP INDEX IF EXISTS idx_symbols_isin;
DROP INDEX IF EXISTS idx_symbols_sector;
DROP INDEX IF EXISTS idx_symbols_status;
DROP INDEX IF EXISTS idx_symbols_market_type;
DROP INDEX IF EXISTS idx_symbols_exchange;
DROP INDEX IF EXISTS idx_symbols_symbol;

-- =============================================================================
-- DROP FOREIGN KEY CONSTRAINTS
-- =============================================================================

-- Drop foreign key constraint for parent orders
ALTER TABLE IF EXISTS orders DROP CONSTRAINT IF EXISTS fk_orders_parent;

-- =============================================================================
-- DROP PARTITION TABLES
-- =============================================================================

-- Drop monthly partitions for orders table
DROP TABLE IF EXISTS orders_y2024m12 CASCADE;
DROP TABLE IF EXISTS orders_y2024m11 CASCADE;
DROP TABLE IF EXISTS orders_y2024m10 CASCADE;
DROP TABLE IF EXISTS orders_y2024m09 CASCADE;
DROP TABLE IF EXISTS orders_y2024m08 CASCADE;
DROP TABLE IF EXISTS orders_y2024m07 CASCADE;
DROP TABLE IF EXISTS orders_y2024m06 CASCADE;
DROP TABLE IF EXISTS orders_y2024m05 CASCADE;
DROP TABLE IF EXISTS orders_y2024m04 CASCADE;
DROP TABLE IF EXISTS orders_y2024m03 CASCADE;
DROP TABLE IF EXISTS orders_y2024m02 CASCADE;
DROP TABLE IF EXISTS orders_y2024m01 CASCADE;

-- =============================================================================
-- DROP TABLES (in reverse order due to foreign key dependencies)
-- =============================================================================

DROP TABLE IF EXISTS broker_accounts CASCADE;
DROP TABLE IF EXISTS positions CASCADE;
DROP TABLE IF EXISTS order_executions CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS symbols CASCADE;

-- =============================================================================
-- DROP ENUM TYPES
-- =============================================================================

DROP TYPE IF EXISTS execution_type CASCADE;
DROP TYPE IF EXISTS symbol_status CASCADE;
DROP TYPE IF EXISTS market_type CASCADE;
DROP TYPE IF EXISTS position_side CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS time_in_force CASCADE;
DROP TYPE IF EXISTS order_side CASCADE;
DROP TYPE IF EXISTS order_type CASCADE;

-- =============================================================================
-- ROLLBACK VERIFICATION
-- =============================================================================

-- Verify that all objects have been dropped
DO $$
DECLARE
    remaining_tables INTEGER;
    remaining_functions INTEGER;
    remaining_types INTEGER;
    remaining_views INTEGER;
    remaining_indexes INTEGER;
BEGIN
    -- Check for remaining tables
    SELECT COUNT(*)
    INTO remaining_tables
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_name IN ('symbols', 'orders', 'order_executions', 'positions', 'broker_accounts');

    -- Check for remaining functions
    SELECT COUNT(*)
    INTO remaining_functions
    FROM information_schema.routines
    WHERE routine_schema = 'public'
    AND routine_name IN ('calculate_position_pnl', 'update_position_after_trade');

    -- Check for remaining types
    SELECT COUNT(*)
    INTO remaining_types
    FROM pg_type
    WHERE typname IN (
        'order_type', 'order_side', 'time_in_force', 'order_status',
        'position_side', 'market_type', 'symbol_status', 'execution_type'
    );

    -- Check for remaining views
    SELECT COUNT(*)
    INTO remaining_views
    FROM information_schema.views
    WHERE table_schema = 'public'
    AND table_name IN ('active_orders', 'portfolio_summary', 'order_execution_summary');

    -- Check for remaining indexes starting with idx_
    SELECT COUNT(*)
    INTO remaining_indexes
    FROM pg_indexes
    WHERE schemaname = 'public'
    AND indexname LIKE 'idx_%'
    AND (
        indexname LIKE '%orders%' OR
        indexname LIKE '%symbols%' OR
        indexname LIKE '%positions%' OR
        indexname LIKE '%executions%' OR
        indexname LIKE '%broker_accounts%'
    );

    -- Report results
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Trading Domain Rollback Verification:';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Remaining tables: %', remaining_tables;
    RAISE NOTICE 'Remaining functions: %', remaining_functions;
    RAISE NOTICE 'Remaining types: %', remaining_types;
    RAISE NOTICE 'Remaining views: %', remaining_views;
    RAISE NOTICE 'Remaining indexes: %', remaining_indexes;

    IF remaining_tables = 0 AND remaining_functions = 0 AND remaining_types = 0
       AND remaining_views = 0 AND remaining_indexes = 0 THEN
        RAISE NOTICE '';
        RAISE NOTICE '✅ Trading domain rollback completed successfully - all objects removed';
        RAISE NOTICE '';
        RAISE NOTICE 'Removed components:';
        RAISE NOTICE '  - 5 main tables (symbols, orders, order_executions, positions, broker_accounts)';
        RAISE NOTICE '  - 12 monthly partition tables for orders';
        RAISE NOTICE '  - 8 enum types for trading domain';
        RAISE NOTICE '  - 2 business logic functions';
        RAISE NOTICE '  - 3 materialized views';
        RAISE NOTICE '  - 25+ performance indexes';
        RAISE NOTICE '  - 4 audit triggers';
        RAISE NOTICE '  - 5 RLS security policies';
        RAISE NOTICE '';
        RAISE NOTICE 'Database state: Reverted to user management schema only (V001)';
    ELSE
        RAISE WARNING '⚠️  Some trading domain objects may not have been removed completely';
        RAISE WARNING 'Manual cleanup may be required for complete rollback';

        IF remaining_tables > 0 THEN
            RAISE WARNING 'Remaining tables need manual review';
        END IF;

        IF remaining_functions > 0 THEN
            RAISE WARNING 'Remaining functions need manual cleanup';
        END IF;

        IF remaining_types > 0 THEN
            RAISE WARNING 'Remaining enum types need manual cleanup';
        END IF;

        IF remaining_views > 0 THEN
            RAISE WARNING 'Remaining views need manual cleanup';
        END IF;

        IF remaining_indexes > 0 THEN
            RAISE WARNING 'Remaining indexes need manual cleanup';
        END IF;
    END IF;

    RAISE NOTICE '=============================================================================';
END $$;

-- =============================================================================
-- POST-ROLLBACK CLEANUP COMMANDS
-- =============================================================================

/*
If the rollback verification shows remaining objects, run these commands manually:

-- Force drop any remaining partition tables
SELECT format('DROP TABLE IF EXISTS %I CASCADE;', tablename)
FROM pg_tables
WHERE schemaname = 'public'
AND tablename LIKE 'orders_y%';

-- Force drop any remaining trading-related indexes
SELECT format('DROP INDEX IF EXISTS %I;', indexname)
FROM pg_indexes
WHERE schemaname = 'public'
AND (indexname LIKE '%orders%'
     OR indexname LIKE '%symbols%'
     OR indexname LIKE '%positions%'
     OR indexname LIKE '%executions%'
     OR indexname LIKE '%broker_accounts%');

-- Verify database objects after cleanup
SELECT 'Tables:' as object_type, count(*) as count FROM information_schema.tables WHERE table_schema = 'public'
UNION ALL
SELECT 'Views:', count(*) FROM information_schema.views WHERE table_schema = 'public'
UNION ALL
SELECT 'Functions:', count(*) FROM information_schema.routines WHERE routine_schema = 'public'
UNION ALL
SELECT 'Types:', count(*) FROM pg_type WHERE typname NOT LIKE 'pg_%' AND typname NOT LIKE '\_%';
*/