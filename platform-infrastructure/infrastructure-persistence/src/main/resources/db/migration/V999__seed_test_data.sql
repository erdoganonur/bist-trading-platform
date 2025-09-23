-- Seed test data for BIST Trading Platform
-- This migration inserts initial test data for development and testing

-- Insert test organizations
INSERT INTO organizations (
    id, organization_code, name, display_name, organization_type, status,
    email, website_url, country, license_number, license_authority,
    license_issued_at, license_expires_at, trading_enabled, max_users,
    contact_person, contact_email, created_at, updated_at
) VALUES
-- BIST organization (primary)
(
    'org-bist-001', 'BIST', 'Borsa İstanbul A.Ş.', 'Borsa İstanbul',
    'MARKET_MAKER', 'ACTIVE', 'info@bist.com.tr', 'https://www.bist.com.tr',
    'TUR', 'BIST-LIC-001', 'SPK', CURRENT_TIMESTAMP - INTERVAL '2 years',
    CURRENT_TIMESTAMP + INTERVAL '3 years', TRUE, NULL,
    'BIST Yönetimi', 'contact@bist.com.tr', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- AlgoLab organization
(
    'org-algolab-001', 'ALGOLAB', 'AlgoLab Teknoloji A.Ş.', 'AlgoLab',
    'FINTECH', 'ACTIVE', 'info@algolab.com.tr', 'https://www.algolab.com.tr',
    'TUR', 'ALGO-LIC-001', 'SPK', CURRENT_TIMESTAMP - INTERVAL '1 year',
    CURRENT_TIMESTAMP + INTERVAL '4 years', TRUE, 1000,
    'AlgoLab Teknik Ekip', 'teknik@algolab.com.tr', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- DenizBank organization
(
    'org-denizbank-001', 'DENIZBANK', 'DenizBank A.Ş.', 'DenizBank',
    'BANK', 'ACTIVE', 'yatirim@denizbank.com', 'https://www.denizbank.com',
    'TUR', 'DENIZ-LIC-001', 'BDDK', CURRENT_TIMESTAMP - INTERVAL '5 years',
    CURRENT_TIMESTAMP + INTERVAL '2 years', TRUE, 5000,
    'DenizBank Yatırım', 'yatirim.destek@denizbank.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- Garanti BBVA organization
(
    'org-garanti-001', 'GARANTI', 'Garanti BBVA Yatırım', 'Garanti BBVA',
    'BANK', 'ACTIVE', 'yatirim@garantibbva.com.tr', 'https://www.garantibbva.com.tr',
    'TUR', 'GARANTI-LIC-001', 'BDDK', CURRENT_TIMESTAMP - INTERVAL '10 years',
    CURRENT_TIMESTAMP + INTERVAL '1 year', TRUE, 10000,
    'Garanti BBVA Yatırım Ekibi', 'yatirim.musteri@garantibbva.com.tr', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Insert test brokers (pre-populate as requested)
INSERT INTO brokers (
    id, broker_code, name, display_name, description, broker_type, status,
    website_url, api_endpoint, api_version, ssl_enabled,
    connection_timeout, read_timeout, max_connections, rate_limit_per_minute,
    commission_rate, minimum_commission, maximum_commission, commission_currency,
    supports_realtime_data, supports_historical_data, supports_order_management, supports_portfolio_tracking,
    supported_order_types, supported_markets, supported_instruments,
    contact_email, support_email, trading_enabled, priority_order,
    configuration, created_at, updated_at
) VALUES
-- AlgoLab Broker
(
    'broker-algolab-001', 'ALGOLAB', 'AlgoLab Trading API', 'AlgoLab Broker',
    'Algoritmik trading ve veri sağlayıcı hizmetleri', 'FINTECH', 'ACTIVE',
    'https://api.algolab.com.tr', 'https://api.algolab.com.tr/v2', 'v2.0', TRUE,
    15000, 30000, 20, 100,
    0.15, 2.50, 50.00, 'TRY',
    TRUE, TRUE, TRUE, TRUE,
    '["MARKET", "LIMIT", "STOP", "STOP_LIMIT", "ICEBERG"]'::jsonb,
    '["BIST_EQUITY", "BIST_FUTURES", "BIST_OPTIONS"]'::jsonb,
    '["STOCKS", "FUTURES", "OPTIONS", "BONDS"]'::jsonb,
    'integration@algolab.com.tr', 'support@algolab.com.tr', TRUE, 1,
    '{"api_rate_limit": 1000, "sandbox_mode": false, "webhook_support": true}'::jsonb,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- DenizBank Broker
(
    'broker-denizbank-001', 'DENIZBANK', 'DenizBank Investment API', 'DenizBank Yatırım',
    'DenizBank yatırım ürünleri ve trading hizmetleri', 'BANK', 'ACTIVE',
    'https://yatirim.denizbank.com', 'https://api.denizbank.com/investment/v1', 'v1.0', TRUE,
    20000, 45000, 15, 60,
    0.25, 5.00, 100.00, 'TRY',
    TRUE, TRUE, TRUE, TRUE,
    '["MARKET", "LIMIT", "STOP"]'::jsonb,
    '["BIST_EQUITY", "BIST_BONDS"]'::jsonb,
    '["STOCKS", "BONDS", "FUNDS"]'::jsonb,
    'api@denizbank.com', 'yatirim.destek@denizbank.com', TRUE, 2,
    '{"api_rate_limit": 500, "sandbox_mode": true, "requires_approval": true}'::jsonb,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- Garanti BBVA Broker
(
    'broker-garanti-001', 'GARANTI', 'Garanti BBVA Investment API', 'Garanti BBVA Yatırım',
    'Garanti BBVA yatırım platformu ve trading API''si', 'BANK', 'ACTIVE',
    'https://yatirim.garantibbva.com.tr', 'https://api.garantibbva.com.tr/trading/v1', 'v1.2', TRUE,
    25000, 60000, 25, 80,
    0.20, 3.00, 75.00, 'TRY',
    TRUE, TRUE, TRUE, TRUE,
    '["MARKET", "LIMIT", "STOP", "STOP_LIMIT"]'::jsonb,
    '["BIST_EQUITY", "BIST_FUTURES", "VIOP"]'::jsonb,
    '["STOCKS", "FUTURES", "OPTIONS", "BONDS", "FUNDS"]'::jsonb,
    'api.integration@garantibbva.com.tr', 'yatirim.api@garantibbva.com.tr', TRUE, 3,
    '{"api_rate_limit": 750, "sandbox_mode": true, "enterprise_features": true}'::jsonb,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Insert test users
INSERT INTO users (
    id, organization_id, email, username, password_hash,
    first_name, last_name, birth_date, gender, nationality,
    preferred_language, timezone, status,
    email_verified, email_verified_at, phone_verified, phone_verified_at,
    kyc_completed, kyc_completed_at, kyc_level, two_factor_enabled,
    risk_profile, professional_investor, investment_experience,
    annual_income, employment_status, occupation,
    terms_accepted_at, privacy_accepted_at, marketing_consent,
    preferences, created_at, updated_at
) VALUES
-- System Administrator
(
    'user-admin-001', 'org-bist-001', 'admin@bist.com.tr', 'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi', -- password: admin123
    'Sistem', 'Yöneticisi', '1980-01-01 00:00:00', 'M', 'TUR',
    'tr', 'Europe/Istanbul', 'ACTIVE',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '30 days', TRUE, CURRENT_TIMESTAMP - INTERVAL '30 days',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '30 days', 'ADVANCED', TRUE,
    'MODERATE', FALSE, 'EXPERT',
    'RANGE_100K_250K', 'EMPLOYED', 'Sistem Yöneticisi',
    CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days', FALSE,
    '{"theme": "dark", "notifications": {"email": true, "sms": false}, "language": "tr"}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP
),
-- Professional Trader
(
    'user-trader-001', 'org-algolab-001', 'trader@algolab.com.tr', 'trader',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi', -- password: trader123
    'Ahmet', 'Yılmaz', '1985-05-15 00:00:00', 'M', 'TUR',
    'tr', 'Europe/Istanbul', 'ACTIVE',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '20 days', TRUE, CURRENT_TIMESTAMP - INTERVAL '20 days',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '15 days', 'ADVANCED', TRUE,
    'AGGRESSIVE', TRUE, 'EXPERT',
    'RANGE_250K_500K', 'EMPLOYED', 'Profesyonel Trader',
    CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days', TRUE,
    '{"theme": "light", "charts": {"default_timeframe": "1H", "indicators": ["RSI", "MACD"]}, "trading": {"auto_confirm": false}}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP
),
-- Retail Customer (DenizBank)
(
    'user-customer-001', 'org-denizbank-001', 'customer@denizbank.com', 'customer',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi', -- password: customer123
    'Ayşe', 'Demir', '1990-08-20 00:00:00', 'F', 'TUR',
    'tr', 'Europe/Istanbul', 'ACTIVE',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days', TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '5 days', 'BASIC', FALSE,
    'CONSERVATIVE', FALSE, 'BEGINNER',
    'RANGE_50K_100K', 'EMPLOYED', 'Mühendis',
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', TRUE,
    '{"theme": "light", "notifications": {"email": true, "sms": true}, "investment_alerts": true}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP
),
-- Test User (Garanti BBVA)
(
    'user-test-001', 'org-garanti-001', 'test@garantibbva.com.tr', 'testuser',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi', -- password: test123
    'Test', 'Kullanıcı', '1995-12-10 00:00:00', 'O', 'TUR',
    'en', 'Europe/Istanbul', 'ACTIVE',
    TRUE, CURRENT_TIMESTAMP - INTERVAL '5 days', FALSE, NULL,
    FALSE, NULL, 'BASIC', FALSE,
    'MODERATE', FALSE, 'INTERMEDIATE',
    'RANGE_100K_250K', 'EMPLOYED', 'Yazılım Geliştirici',
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', FALSE,
    '{"theme": "auto", "language": "en", "testing_mode": true}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP
);

-- Insert test user sessions
INSERT INTO user_sessions (
    id, user_id, session_token, refresh_token, status,
    ip_address, user_agent, device_type, operating_system, browser,
    location_country, location_city, started_at, expires_at, last_activity_at,
    is_remembered, is_trusted_device, two_factor_verified, security_level,
    metadata, created_at, updated_at
) VALUES
-- Admin active session
(
    'session-admin-001', 'user-admin-001',
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.session.token',
    'refresh.token.admin.001', 'ACTIVE',
    '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'DESKTOP', 'Windows 10', 'Chrome 120',
    'TUR', 'Istanbul', CURRENT_TIMESTAMP - INTERVAL '2 hours',
    CURRENT_TIMESTAMP + INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '10 minutes',
    FALSE, TRUE, TRUE, 'HIGH',
    '{"login_method": "2FA", "session_type": "admin_panel"}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '10 minutes'
),
-- Trader active session
(
    'session-trader-001', 'user-trader-001',
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.trader.session.token',
    'refresh.token.trader.001', 'ACTIVE',
    '10.0.0.50', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15',
    'DESKTOP', 'macOS', 'Safari 17',
    'TUR', 'Istanbul', CURRENT_TIMESTAMP - INTERVAL '1 hour',
    CURRENT_TIMESTAMP + INTERVAL '7 hours', CURRENT_TIMESTAMP - INTERVAL '5 minutes',
    TRUE, TRUE, TRUE, 'HIGH',
    '{"login_method": "2FA", "trading_session": true, "api_access": true}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '5 minutes'
),
-- Customer mobile session
(
    'session-customer-001', 'user-customer-001',
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.customer.session.token',
    'refresh.token.customer.001', 'ACTIVE',
    '172.16.0.25', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15',
    'MOBILE', 'iOS 17', 'Safari Mobile',
    'TUR', 'Ankara', CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    CURRENT_TIMESTAMP + INTERVAL '7 hours 30 minutes', CURRENT_TIMESTAMP - INTERVAL '2 minutes',
    TRUE, FALSE, FALSE, 'MEDIUM',
    '{"login_method": "PASSWORD", "mobile_app": true, "app_version": "2.1.0"}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '2 minutes'
);

-- Update organization current user counts
UPDATE organizations SET current_users = (
    SELECT COUNT(*) FROM users u
    WHERE u.organization_id = organizations.id
    AND u.status = 'ACTIVE'
    AND u.deleted_at IS NULL
);

-- Update broker connection statistics with test data
UPDATE brokers SET
    last_connection_at = CURRENT_TIMESTAMP - INTERVAL '1 hour',
    connection_success_rate = CASE
        WHEN broker_code = 'ALGOLAB' THEN 98.5
        WHEN broker_code = 'DENIZBANK' THEN 95.2
        WHEN broker_code = 'GARANTI' THEN 97.8
    END,
    average_response_time = CASE
        WHEN broker_code = 'ALGOLAB' THEN 150
        WHEN broker_code = 'DENIZBANK' THEN 280
        WHEN broker_code = 'GARANTI' THEN 220
    END
WHERE broker_code IN ('ALGOLAB', 'DENIZBANK', 'GARANTI');

-- Add some sample JSONB data to preferences and metadata
UPDATE users SET
    preferences = preferences || '{"dashboard_layout": "advanced", "default_market": "BIST_100"}'::jsonb,
    metadata = '{"registration_source": "web", "referral_code": null, "test_account": true}'::jsonb
WHERE id LIKE 'user-%';

UPDATE organizations SET
    settings = '{"trading_hours": {"start": "09:30", "end": "18:00"}, "commission_sharing": 0.1, "api_access": true}'::jsonb,
    metadata = '{"integration_type": "api", "certification_level": "production", "test_data": true}'::jsonb
WHERE id LIKE 'org-%';

UPDATE brokers SET
    configuration = configuration || '{"retry_attempts": 3, "timeout_multiplier": 1.5, "health_check_interval": 300}'::jsonb,
    metadata = '{"integration_status": "active", "last_update": "2024-01-15", "test_broker": true}'::jsonb
WHERE id LIKE 'broker-%';

-- Insert some audit trail comments
INSERT INTO user_sessions (
    id, user_id, session_token, refresh_token, status,
    ip_address, device_type, started_at, expires_at, ended_at,
    logout_reason, security_level, metadata, created_at, updated_at
) VALUES
-- Expired session example
(
    'session-expired-001', 'user-test-001',
    'expired.session.token.001', NULL, 'EXPIRED',
    '203.0.113.10', 'MOBILE', CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day',
    'SESSION_TIMEOUT', 'MEDIUM',
    '{"session_duration_minutes": 480, "auto_expired": true}'::jsonb,
    CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
);

-- Add comments to indicate this is test data
COMMENT ON TABLE users IS 'User accounts for BIST Trading Platform with encrypted PII fields (includes test data)';
COMMENT ON TABLE organizations IS 'Organizations and trading entities in BIST Trading Platform (includes test data)';
COMMENT ON TABLE brokers IS 'External brokers and trading service providers (includes test data for AlgoLab, DenizBank, Garanti)';
COMMENT ON TABLE user_sessions IS 'User session tracking for security and audit purposes (includes test data)';

-- Show summary of inserted test data
DO $$
BEGIN
    RAISE NOTICE 'Test data seeding completed:';
    RAISE NOTICE '- Organizations: % rows', (SELECT COUNT(*) FROM organizations);
    RAISE NOTICE '- Users: % rows', (SELECT COUNT(*) FROM users);
    RAISE NOTICE '- Brokers: % rows', (SELECT COUNT(*) FROM brokers);
    RAISE NOTICE '- Sessions: % rows', (SELECT COUNT(*) FROM user_sessions);
    RAISE NOTICE 'Pre-populated brokers: AlgoLab, DenizBank, Garanti BBVA';
END
$$;