-- =============================================================================
-- BIST Trading Platform - Test User Data Seeding
-- Version: V4 - Development and test users with portfolios
-- =============================================================================

-- =============================================================================
-- TEST USER CREATION
-- =============================================================================

-- Insert test users with different risk profiles and roles
INSERT INTO user_management.users (
    email, phone, password_hash, password_salt,
    first_name, last_name, tc_kimlik_no, birth_date,
    address, city, postal_code, country,
    trading_account_no, risk_profile,
    is_active, is_email_verified, is_phone_verified, is_kyc_completed, is_trading_enabled,
    preferred_language, timezone, notification_preferences
) VALUES
    -- Admin user
    (
        'admin@bist-trading.com',
        '+905551234567',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- AdminPassword123!
        'admin_salt_123',
        'Admin',
        'User',
        '12345678901',
        '1980-01-01',
        'Levent Mahallesi, İş Kuleleri No:1',
        'İstanbul',
        '34330',
        'Türkiye',
        'ADM001',
        'MODERATE',
        true, true, true, true, true,
        'tr',
        'Europe/Istanbul',
        '{"email": true, "sms": true, "push": true}'::jsonb
    ),

    -- Regular trader - Conservative
    (
        'trader1@test.com',
        '+905551234568',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- TraderPass123!
        'trader1_salt_456',
        'Ahmet',
        'Yılmaz',
        '98765432101',
        '1985-03-15',
        'Bağdat Caddesi No:123',
        'İstanbul',
        '34740',
        'Türkiye',
        'TRD001',
        'CONSERVATIVE',
        true, true, true, true, true,
        'tr',
        'Europe/Istanbul',
        '{"email": true, "sms": false, "push": true}'::jsonb
    ),

    -- Active trader - Aggressive
    (
        'trader2@test.com',
        '+905551234569',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- TraderPass123!
        'trader2_salt_789',
        'Fatma',
        'Demir',
        '11223344556',
        '1990-07-22',
        'Nişantaşı Mahallesi No:45',
        'İstanbul',
        '34367',
        'Türkiye',
        'TRD002',
        'AGGRESSIVE',
        true, true, true, true, true,
        'tr',
        'Europe/Istanbul',
        '{"email": true, "sms": true, "push": true}'::jsonb
    ),

    -- Moderate trader
    (
        'trader3@test.com',
        '+905551234570',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- TraderPass123!
        'trader3_salt_012',
        'Mehmet',
        'Özkan',
        '55667788990',
        '1988-11-10',
        'Kadıköy Moda Caddesi No:78',
        'İstanbul',
        '34710',
        'Türkiye',
        'TRD003',
        'MODERATE',
        true, true, true, true, true,
        'tr',
        'Europe/Istanbul',
        '{"email": true, "sms": false, "push": false}'::jsonb
    ),

    -- Viewer only user
    (
        'viewer@test.com',
        '+905551234571',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- ViewerPass123!
        'viewer_salt_345',
        'Ayşe',
        'Kara',
        '99887766554',
        '1995-12-05',
        'Beyoğlu İstiklal Caddesi No:200',
        'İstanbul',
        '34433',
        'Türkiye',
        'VWR001',
        'CONSERVATIVE',
        true, true, true, true, false,
        'tr',
        'Europe/Istanbul',
        '{"email": false, "sms": false, "push": true}'::jsonb
    ),

    -- Support user
    (
        'support@bist-trading.com',
        '+905551234572',
        '$2a$12$rQZ0ZvCx9Q6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6xQ6x', -- SupportPass123!
        'support_salt_678',
        'Support',
        'Team',
        '44556677889',
        '1987-06-18',
        'Maslak Mahallesi No:5',
        'İstanbul',
        '34485',
        'Türkiye',
        'SUP001',
        'MODERATE',
        true, true, true, true, false,
        'tr',
        'Europe/Istanbul',
        '{"email": true, "sms": true, "push": false}'::jsonb
    )
ON CONFLICT (email) DO NOTHING;

-- =============================================================================
-- USER ROLE ASSIGNMENTS
-- =============================================================================

-- Assign roles to test users
INSERT INTO user_management.user_roles (user_id, role_id, assigned_by)
SELECT
    u.user_id,
    r.role_id,
    (SELECT user_id FROM user_management.users WHERE email = 'admin@bist-trading.com')
FROM user_management.users u
CROSS JOIN user_management.roles r
WHERE
    (u.email = 'admin@bist-trading.com' AND r.role_name = 'ADMIN') OR
    (u.email = 'trader1@test.com' AND r.role_name = 'TRADER') OR
    (u.email = 'trader2@test.com' AND r.role_name = 'TRADER') OR
    (u.email = 'trader3@test.com' AND r.role_name = 'TRADER') OR
    (u.email = 'viewer@test.com' AND r.role_name = 'VIEWER') OR
    (u.email = 'support@bist-trading.com' AND r.role_name = 'SUPPORT')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =============================================================================
-- TEST PORTFOLIOS
-- =============================================================================

-- Create portfolios for trader users
INSERT INTO trading.portfolios (
    user_id, portfolio_name, portfolio_type, base_currency,
    is_margin_enabled, max_leverage, max_daily_loss, max_position_size,
    risk_tolerance, initial_balance, current_value
)
SELECT
    u.user_id,
    CASE u.email
        WHEN 'trader1@test.com' THEN 'Conservative Portfolio'
        WHEN 'trader2@test.com' THEN 'Aggressive Trading'
        WHEN 'trader3@test.com' THEN 'Balanced Portfolio'
        ELSE 'Default Portfolio'
    END,
    CASE u.email
        WHEN 'trader2@test.com' THEN 'DAY_TRADING'
        ELSE 'STANDARD'
    END,
    'TRY',
    CASE u.email
        WHEN 'trader2@test.com' THEN true
        ELSE false
    END,
    CASE u.email
        WHEN 'trader1@test.com' THEN 1.0
        WHEN 'trader2@test.com' THEN 5.0
        WHEN 'trader3@test.com' THEN 2.0
        ELSE 1.0
    END,
    CASE u.email
        WHEN 'trader1@test.com' THEN 5000.00
        WHEN 'trader2@test.com' THEN 20000.00
        WHEN 'trader3@test.com' THEN 10000.00
        ELSE 1000.00
    END,
    CASE u.email
        WHEN 'trader1@test.com' THEN 50000.00
        WHEN 'trader2@test.com' THEN 100000.00
        WHEN 'trader3@test.com' THEN 75000.00
        ELSE 10000.00
    END,
    u.risk_profile,
    CASE u.email
        WHEN 'trader1@test.com' THEN 100000.00
        WHEN 'trader2@test.com' THEN 250000.00
        WHEN 'trader3@test.com' THEN 150000.00
        ELSE 50000.00
    END,
    CASE u.email
        WHEN 'trader1@test.com' THEN 105000.00
        WHEN 'trader2@test.com' THEN 245000.00
        WHEN 'trader3@test.com' THEN 148000.00
        ELSE 52000.00
    END
FROM user_management.users u
WHERE u.email IN ('trader1@test.com', 'trader2@test.com', 'trader3@test.com', 'admin@bist-trading.com')
ON CONFLICT (user_id, portfolio_name) DO NOTHING;

-- =============================================================================
-- CASH BALANCES
-- =============================================================================

-- Initialize cash balances for portfolios
INSERT INTO trading.cash_balances (
    portfolio_id, currency, available_balance, reserved_balance, credit_limit
)
SELECT
    p.portfolio_id,
    'TRY',
    CASE
        WHEN u.email = 'trader1@test.com' THEN 50000.00
        WHEN u.email = 'trader2@test.com' THEN 75000.00
        WHEN u.email = 'trader3@test.com' THEN 60000.00
        ELSE 25000.00
    END,
    CASE
        WHEN u.email = 'trader2@test.com' THEN 5000.00
        ELSE 0.00
    END,
    CASE
        WHEN u.email = 'trader2@test.com' THEN 50000.00
        ELSE 0.00
    END
FROM trading.portfolios p
JOIN user_management.users u ON p.user_id = u.user_id
WHERE u.email IN ('trader1@test.com', 'trader2@test.com', 'trader3@test.com', 'admin@bist-trading.com')
ON CONFLICT (portfolio_id, currency) DO NOTHING;

-- Add USD balances for some portfolios
INSERT INTO trading.cash_balances (
    portfolio_id, currency, available_balance, reserved_balance, credit_limit
)
SELECT
    p.portfolio_id,
    'USD',
    CASE
        WHEN u.email = 'trader2@test.com' THEN 2000.00
        WHEN u.email = 'trader3@test.com' THEN 1000.00
        ELSE 0.00
    END,
    0.00,
    0.00
FROM trading.portfolios p
JOIN user_management.users u ON p.user_id = u.user_id
WHERE u.email IN ('trader2@test.com', 'trader3@test.com')
ON CONFLICT (portfolio_id, currency) DO NOTHING;

-- =============================================================================
-- SAMPLE POSITIONS
-- =============================================================================

-- Create some sample positions for testing
INSERT INTO trading.positions (
    portfolio_id, symbol_id, position_type, quantity, average_price, current_price
)
SELECT
    p.portfolio_id,
    s.symbol_id,
    'LONG',
    pos.quantity,
    pos.avg_price,
    pos.current_price
FROM trading.portfolios p
JOIN user_management.users u ON p.user_id = u.user_id
CROSS JOIN reference.symbols s
CROSS JOIN (VALUES
    -- Conservative trader positions
    ('trader1@test.com', 'AKBNK', 1000, 15.50, 15.75),
    ('trader1@test.com', 'GARAN', 500, 22.30, 22.10),
    ('trader1@test.com', 'THYAO', 200, 85.40, 87.20),

    -- Aggressive trader positions
    ('trader2@test.com', 'TUPRS', 300, 145.80, 148.50),
    ('trader2@test.com', 'ASELS', 150, 68.90, 71.20),
    ('trader2@test.com', 'KCHOL', 800, 25.15, 24.80),
    ('trader2@test.com', 'ARCLK', 400, 42.60, 43.10),

    -- Moderate trader positions
    ('trader3@test.com', 'BIMAS', 100, 165.20, 167.80),
    ('trader3@test.com', 'ISCTR', 600, 18.75, 18.90),
    ('trader3@test.com', 'SAHOL', 350, 35.40, 35.10)
) AS pos(email, symbol, quantity, avg_price, current_price)
WHERE u.email = pos.email AND s.symbol = pos.symbol
ON CONFLICT (portfolio_id, symbol_id, is_active) DO NOTHING;

-- =============================================================================
-- SAMPLE TRANSACTIONS
-- =============================================================================

-- Create sample transactions for the positions
INSERT INTO trading.transactions (
    portfolio_id, transaction_type, symbol_id, quantity, price,
    amount, currency, cash_impact, status,
    transaction_date, settlement_date, value_date,
    description
)
SELECT
    pos.portfolio_id,
    'BUY',
    pos.symbol_id,
    pos.quantity,
    pos.average_price,
    pos.quantity * pos.average_price * -1, -- Negative for buy
    'TRY',
    pos.quantity * pos.average_price * -1,
    'SETTLED',
    CURRENT_DATE - INTERVAL '7 days',
    CURRENT_DATE - INTERVAL '5 days',
    CURRENT_DATE - INTERVAL '5 days',
    'Initial position purchase'
FROM trading.positions pos
WHERE pos.is_active = true;

-- =============================================================================
-- SAMPLE PORTFOLIO SNAPSHOTS
-- =============================================================================

-- Create daily snapshots for the last 30 days
INSERT INTO trading.portfolio_snapshots (
    portfolio_id, snapshot_date, total_value, cash_value, positions_value,
    daily_pnl, daily_pnl_percent, total_pnl, total_pnl_percent,
    trades_count, volume_traded, fees_paid
)
SELECT
    p.portfolio_id,
    d.snapshot_date,
    CASE
        WHEN u.email = 'trader1@test.com' THEN 100000.00 + (random() * 10000 - 5000)
        WHEN u.email = 'trader2@test.com' THEN 250000.00 + (random() * 25000 - 12500)
        WHEN u.email = 'trader3@test.com' THEN 150000.00 + (random() * 15000 - 7500)
        ELSE 50000.00 + (random() * 5000 - 2500)
    END,
    CASE
        WHEN u.email = 'trader1@test.com' THEN 50000.00
        WHEN u.email = 'trader2@test.com' THEN 75000.00
        WHEN u.email = 'trader3@test.com' THEN 60000.00
        ELSE 25000.00
    END,
    CASE
        WHEN u.email = 'trader1@test.com' THEN 50000.00 + (random() * 10000 - 5000)
        WHEN u.email = 'trader2@test.com' THEN 175000.00 + (random() * 25000 - 12500)
        WHEN u.email = 'trader3@test.com' THEN 90000.00 + (random() * 15000 - 7500)
        ELSE 25000.00 + (random() * 5000 - 2500)
    END,
    random() * 2000 - 1000, -- Daily PnL
    (random() * 4 - 2), -- Daily PnL percent
    CASE
        WHEN u.email = 'trader1@test.com' THEN random() * 10000 - 2000
        WHEN u.email = 'trader2@test.com' THEN random() * 20000 - 5000
        WHEN u.email = 'trader3@test.com' THEN random() * 5000 - 2000
        ELSE random() * 2000 - 500
    END,
    random() * 10 - 2, -- Total PnL percent
    FLOOR(random() * 10)::INTEGER, -- Trades count
    random() * 50000, -- Volume traded
    random() * 100 -- Fees paid
FROM trading.portfolios p
JOIN user_management.users u ON p.user_id = u.user_id
CROSS JOIN (
    SELECT CURRENT_DATE - INTERVAL '1 day' * generate_series(0, 29) AS snapshot_date
) d
WHERE u.email IN ('trader1@test.com', 'trader2@test.com', 'trader3@test.com', 'admin@bist-trading.com')
ON CONFLICT (portfolio_id, snapshot_date) DO NOTHING;

-- =============================================================================
-- UPDATE STATISTICS AND MAINTENANCE
-- =============================================================================

-- Update statistics for better query performance
ANALYZE user_management.users;
ANALYZE user_management.user_roles;
ANALYZE trading.portfolios;
ANALYZE trading.positions;
ANALYZE trading.cash_balances;
ANALYZE trading.transactions;
ANALYZE trading.portfolio_snapshots;

-- =============================================================================
-- TEST USER DATA SEEDING COMPLETE
-- =============================================================================

-- Summary of created test data:
-- - 6 test users with different roles and risk profiles
-- - 4 portfolios with different strategies
-- - Cash balances in TRY and USD
-- - Sample stock positions across major BIST symbols
-- - Historical transactions and portfolio snapshots
-- - 30 days of portfolio performance history