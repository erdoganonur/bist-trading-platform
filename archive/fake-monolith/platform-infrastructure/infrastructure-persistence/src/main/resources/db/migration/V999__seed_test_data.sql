-- BIST Trading Platform test verileri
-- Bu migration geliştirme ve test için başlangıç verilerini ekler
-- İçerik: AlgoLab, DenizBank, Garanti brokers ve BIST30 sembolleri

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

-- BIST30 endeksi sembollerini ve güncel piyasa verilerini ekle
INSERT INTO market_statistics (
    date, symbol, exchange, open_price, high_price, low_price, close_price,
    previous_close, volume, turnover, trade_count, vwap,
    change_amount, change_percent, shares_outstanding, market_cap,
    sector, sub_sector, trading_status, last_updated
) VALUES
-- BIST30 sembolleri (2024 başı fiyatları temel alınarak)
(CURRENT_DATE, 'AKBNK', 'BIST', 32.50, 33.20, 32.10, 32.80, 32.50, 15000000, 495600000, 12500, 32.75, 0.30, 0.92, 6600000000, 216480000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'ARCLK', 'BIST', 125.50, 128.00, 124.00, 126.75, 125.50, 2500000, 316875000, 8200, 126.75, 1.25, 1.00, 520000000, 65910000000, 'Teknoloji', 'Ev Aletleri', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'ASELS', 'BIST', 89.40, 91.20, 88.50, 90.10, 89.40, 3200000, 288320000, 9500, 90.10, 0.70, 0.78, 1100000000, 99110000000, 'Teknoloji', 'Savunma', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'BIMAS', 'BIST', 198.50, 202.00, 196.00, 200.25, 198.50, 1800000, 360450000, 6800, 200.25, 1.75, 0.88, 250000000, 50062500000, 'Mali', 'Sigorta', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'EREGL', 'BIST', 41.20, 42.10, 40.80, 41.65, 41.20, 8500000, 354025000, 15200, 41.65, 0.45, 1.09, 3500000000, 145775000000, 'Sanayi', 'Demir Çelik', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'FROTO', 'BIST', 285.00, 290.50, 283.00, 287.75, 285.00, 950000, 273362500, 4200, 287.75, 2.75, 0.96, 180000000, 51795000000, 'Otomotiv', 'Otomobil', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'GARAN', 'BIST', 28.75, 29.40, 28.50, 29.15, 28.75, 18500000, 539275000, 18700, 29.15, 0.40, 1.39, 4200000000, 122430000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'HALKB', 'BIST', 7.85, 8.02, 7.78, 7.95, 7.85, 35000000, 278250000, 25600, 7.95, 0.10, 1.27, 6800000000, 54060000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'ISCTR', 'BIST', 16.45, 16.85, 16.30, 16.70, 16.45, 22000000, 367400000, 19800, 16.70, 0.25, 1.52, 8250000000, 137775000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'KCHOL', 'BIST', 52.25, 53.50, 51.80, 52.90, 52.25, 4200000, 222180000, 11200, 52.90, 0.65, 1.24, 2500000000, 132250000000, 'Holding', 'Holding', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'KOZAA', 'BIST', 165.50, 168.00, 164.00, 166.75, 165.50, 1200000, 200100000, 5400, 166.75, 1.25, 0.76, 142000000, 23678500000, 'Sanayi', 'Madencilik', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'KOZAL', 'BIST', 98.20, 100.10, 97.50, 99.35, 98.20, 2100000, 208635000, 7800, 99.35, 1.15, 1.17, 550000000, 54642500000, 'Sanayi', 'Madencilik', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'KRDMD', 'BIST', 145.25, 148.50, 144.00, 146.90, 145.25, 850000, 124865000, 3600, 146.90, 1.65, 1.14, 120000000, 17628000000, 'Sanayi', 'Gıda', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'OTKAR', 'BIST', 410.00, 420.50, 405.00, 415.25, 410.00, 320000, 132880000, 1800, 415.25, 5.25, 1.28, 40000000, 16610000000, 'Otomotiv', 'Otobüs Kamyon', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'PETKM', 'BIST', 88.50, 90.25, 87.80, 89.60, 88.50, 4500000, 403200000, 12800, 89.60, 1.10, 1.24, 1300000000, 116480000000, 'Kimya', 'Petrokimya', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'PGSUS', 'BIST', 185.00, 189.50, 183.50, 187.25, 185.00, 1100000, 205975000, 4900, 187.25, 2.25, 1.22, 200000000, 37450000000, 'Teknoloji', 'Yazılım', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'SAHOL', 'BIST', 24.80, 25.35, 24.60, 25.10, 24.80, 12500000, 313750000, 16800, 25.10, 0.30, 1.21, 3250000000, 81575000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'SASA', 'BIST', 75.50, 77.20, 74.80, 76.35, 75.50, 2800000, 213780000, 8500, 76.35, 0.85, 1.13, 475000000, 36266250000, 'Sanayi', 'Tekstil', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'SISE', 'BIST', 35.25, 36.10, 34.90, 35.70, 35.25, 6200000, 221340000, 13400, 35.70, 0.45, 1.28, 1275000000, 45517500000, 'Sanayi', 'Cam', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'TAVHL', 'BIST', 58.75, 60.20, 58.25, 59.50, 58.75, 2900000, 172550000, 9200, 59.50, 0.75, 1.28, 550000000, 32725000000, 'Turizm', 'Otel', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'TCELL', 'BIST', 42.50, 43.25, 42.10, 42.90, 42.50, 5800000, 248820000, 14600, 42.90, 0.40, 0.94, 2200000000, 94380000000, 'Teknoloji', 'Telekomünikasyon', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'THYAO', 'BIST', 265.50, 272.00, 262.00, 268.75, 265.50, 1650000, 443437500, 6800, 268.75, 3.25, 1.22, 800000000, 215000000000, 'Ulaştırma', 'Havayolu', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'TKFEN', 'BIST', 125.50, 128.75, 124.00, 127.25, 125.50, 850000, 108162500, 3200, 127.25, 1.75, 1.39, 135000000, 17178750000, 'İnşaat', 'İnşaat', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'TOASO', 'BIST', 88.25, 90.50, 87.50, 89.75, 88.25, 1900000, 170525000, 6400, 89.75, 1.50, 1.70, 300000000, 26925000000, 'Otomotiv', 'Lastik', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'TUPRS', 'BIST', 445.00, 455.50, 440.00, 450.25, 445.00, 720000, 324180000, 2900, 450.25, 5.25, 1.18, 220000000, 99055000000, 'Sanayi', 'Petrokimya', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'ULKER', 'BIST', 72.50, 74.25, 71.80, 73.60, 72.50, 1450000, 106720000, 4800, 73.60, 1.10, 1.52, 325000000, 23920000000, 'Sanayi', 'Gıda', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'VAKBN', 'BIST', 12.85, 13.15, 12.70, 13.02, 12.85, 28500000, 371070000, 22100, 13.02, 0.17, 1.32, 6200000000, 80724000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'VESTL', 'BIST', 48.25, 49.50, 47.80, 48.95, 48.25, 3200000, 156640000, 9800, 48.95, 0.70, 1.45, 650000000, 31817500000, 'Sanayi', 'Demir Çelik', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'YKBNK', 'BIST', 25.40, 26.10, 25.15, 25.75, 25.40, 16800000, 432600000, 19500, 25.75, 0.35, 1.38, 2500000000, 64375000000, 'Mali', 'Bankacılık', 'TRADING', CURRENT_TIMESTAMP),
(CURRENT_DATE, 'ZOREN', 'BIST', 68.50, 70.25, 67.80, 69.40, 68.50, 1850000, 128390000, 5600, 69.40, 0.90, 1.31, 290000000, 20126000000, 'Sanayi', 'Otomotiv Yan Sanayi', 'TRADING', CURRENT_TIMESTAMP);

-- Broker hesaplarını ve test pozisyonlarını ekle
INSERT INTO broker_accounts (
    id, user_id, organization_id, broker_name, broker_code, account_type,
    account_status, account_number, username, password_hash,
    currency, account_balance, available_balance, commission_rate,
    minimum_commission, connection_status, last_connection_at,
    verification_status, verified_at, is_active, is_default,
    created_at, updated_at
) VALUES
-- Admin hesabı için AlgoLab broker hesabı
('ba-admin-algolab-001', 'user-admin-001', 'org-bist-001', 'AlgoLab Trading API', 'ALGOLAB', 'MAIN',
 'ACTIVE', 'encrypted_account_12345', 'encrypted_username_admin', 'encrypted_password_hash',
 'TRY', 250000.00, 200000.00, 0.0015, 2.50,
 'CONNECTED', CURRENT_TIMESTAMP - INTERVAL '1 hour', 'VERIFIED', CURRENT_TIMESTAMP - INTERVAL '20 days',
 TRUE, TRUE, CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP),

-- Trader hesabı için birden fazla broker
('ba-trader-algolab-001', 'user-trader-001', 'org-algolab-001', 'AlgoLab Trading API', 'ALGOLAB', 'MAIN',
 'ACTIVE', 'encrypted_account_67890', 'encrypted_username_trader', 'encrypted_password_hash',
 'TRY', 500000.00, 350000.00, 0.0012, 2.00,
 'CONNECTED', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'VERIFIED', CURRENT_TIMESTAMP - INTERVAL '15 days',
 TRUE, TRUE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP),

('ba-trader-garanti-001', 'user-trader-001', 'org-algolab-001', 'Garanti BBVA Investment API', 'GARANTI', 'MAIN',
 'ACTIVE', 'encrypted_account_54321', 'encrypted_username_trader_grt', 'encrypted_password_hash',
 'TRY', 300000.00, 250000.00, 0.0020, 3.00,
 'CONNECTED', CURRENT_TIMESTAMP - INTERVAL '45 minutes', 'VERIFIED', CURRENT_TIMESTAMP - INTERVAL '12 days',
 TRUE, FALSE, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP),

-- Customer hesabı için DenizBank
('ba-customer-deniz-001', 'user-customer-001', 'org-denizbank-001', 'DenizBank Investment API', 'DENIZBANK', 'MAIN',
 'ACTIVE', 'encrypted_account_98765', 'encrypted_username_customer', 'encrypted_password_hash',
 'TRY', 150000.00, 120000.00, 0.0025, 5.00,
 'CONNECTED', CURRENT_TIMESTAMP - INTERVAL '15 minutes', 'VERIFIED', CURRENT_TIMESTAMP - INTERVAL '8 days',
 TRUE, TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP);

-- Örnek açık pozisyonlar ekle
INSERT INTO positions (
    id, user_id, broker_account_id, organization_id, position_id,
    symbol, position_side, quantity, available_quantity,
    average_cost, total_cost, commission_paid, current_price,
    last_price_update, market_value, unrealized_pnl, unrealized_pnl_percent,
    daily_pnl, daily_pnl_percent, previous_close_price,
    position_status, opened_at, strategy_id, position_source,
    created_at, updated_at
) VALUES
-- Admin GARAN pozisyonu
('pos-admin-garan-001', 'user-admin-001', 'ba-admin-algolab-001', 'org-bist-001', 'POS_GARAN_001',
 'GARAN', 'LONG', 10000.00, 10000.00, 28.50, 285000.00, 427.50, 29.15,
 CURRENT_TIMESTAMP - INTERVAL '5 minutes', 291500.00, 6072.50, 2.13,
 65.00, 0.22, 29.15, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, 'MANUAL',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '5 minutes'),

-- Trader AKBNK pozisyonu
('pos-trader-akbnk-001', 'user-trader-001', 'ba-trader-algolab-001', 'org-algolab-001', 'POS_AKBNK_001',
 'AKBNK', 'LONG', 15000.00, 15000.00, 32.00, 480000.00, 576.00, 32.80,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 492000.00, 11424.00, 2.38,
 450.00, 0.92, 32.50, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '5 days', 'strategy-momentum-001', 'ALGORITHM',
 CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),

-- Trader THYAO pozisyonu (kısa vadeli)
('pos-trader-thyao-001', 'user-trader-001', 'ba-trader-garanti-001', 'org-algolab-001', 'POS_THYAO_001',
 'THYAO', 'LONG', 2000.00, 2000.00, 260.00, 520000.00, 1040.00, 268.75,
 CURRENT_TIMESTAMP - INTERVAL '1 minute', 537500.00, 16460.00, 3.17,
 6500.00, 1.22, 265.50, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '2 days', 'strategy-airline-recovery', 'MANUAL',
 CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 minute'),

-- Customer ISCTR pozisyonu (muhafazakar)
('pos-customer-isctr-001', 'user-customer-001', 'ba-customer-deniz-001', 'org-denizbank-001', 'POS_ISCTR_001',
 'ISCTR', 'LONG', 5000.00, 5000.00, 16.20, 81000.00, 202.50, 16.70,
 CURRENT_TIMESTAMP - INTERVAL '3 minutes', 83500.00, 2297.50, 2.84,
 125.00, 1.52, 16.45, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'MANUAL',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '3 minutes');

-- Örnek geçmiş emirler ekle (orders tablosuna)
INSERT INTO orders (
    id, user_id, broker_account_id, organization_id, order_number,
    symbol, order_side, order_type, quantity, filled_quantity,
    price, average_fill_price, order_value, filled_value,
    commission, order_status, order_date, order_time,
    acknowledged_at, first_fill_at, completed_at, order_source,
    created_at, updated_at
) VALUES
-- Tamamlanmış alım emri
('order-001', 'user-trader-001', 'ba-trader-algolab-001', 'org-algolab-001', 'ORD_20240115_001',
 'AKBNK', 'BUY', 'LIMIT', 15000.00, 15000.00, 32.00, 31.98, 480000.00, 479700.00,
 575.64, 'FILLED', CURRENT_DATE - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days',
 CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '2 minutes',
 CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '5 minutes',
 CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '8 minutes', 'API',
 CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '8 minutes'),

-- Aktif limit emri
('order-002', 'user-customer-001', 'ba-customer-deniz-001', 'org-denizbank-001', 'ORD_20240120_002',
 'TUPRS', 'BUY', 'LIMIT', 1000.00, 0.00, 440.00, NULL, 440000.00, 0.00,
 0.00, 'WORKING', CURRENT_DATE, CURRENT_TIMESTAMP - INTERVAL '2 hours',
 CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '1 minute', NULL, NULL, 'WEB',
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '1 minute');

-- Test için market ticks verisi ekle (son 1 saatlik)
INSERT INTO market_ticks (
    time, symbol, exchange, price, volume, tick_type,
    bid_price, bid_volume, ask_price, ask_volume, tick_direction
)
SELECT
    CURRENT_TIMESTAMP - (random() * INTERVAL '1 hour'),
    symbols.symbol,
    'BIST',
    (stats.close_price + (random() - 0.5) * stats.close_price * 0.02)::DECIMAL(12,4),
    (random() * 10000 + 100)::DECIMAL(15,4),
    'TRADE',
    (stats.close_price * 0.998)::DECIMAL(12,4),
    (random() * 50000 + 1000)::DECIMAL(15,4),
    (stats.close_price * 1.002)::DECIMAL(12,4),
    (random() * 50000 + 1000)::DECIMAL(15,4),
    (CASE WHEN random() < 0.33 THEN -1 WHEN random() < 0.66 THEN 0 ELSE 1 END)
FROM
    (VALUES ('GARAN'), ('AKBNK'), ('ISCTR'), ('THYAO'), ('TUPRS')) AS symbols(symbol)
CROSS JOIN
    market_statistics stats
WHERE stats.symbol = symbols.symbol AND stats.date = CURRENT_DATE
AND generate_series(1, 50) IS NOT NULL; -- Her sembol için 50 tick

-- Test verileri özeti göster
DO $$
BEGIN
    RAISE NOTICE 'BIST Trading Platform test verilerinin eklenmesi tamamlandı:';
    RAISE NOTICE '- Organizasyonlar: % satır', (SELECT COUNT(*) FROM organizations);
    RAISE NOTICE '- Kullanıcılar: % satır', (SELECT COUNT(*) FROM users);
    RAISE NOTICE '- Broker hesapları: % satır', (SELECT COUNT(*) FROM broker_accounts);
    RAISE NOTICE '- BIST30 piyasa verileri: % sembol', (SELECT COUNT(*) FROM market_statistics WHERE date = CURRENT_DATE);
    RAISE NOTICE '- Açık pozisyonlar: % pozisyon', (SELECT COUNT(*) FROM positions WHERE position_status = ''OPEN'');
    RAISE NOTICE '- Emirler: % emir', (SELECT COUNT(*) FROM orders);
    RAISE NOTICE '- Market tick verileri: % tick', (SELECT COUNT(*) FROM market_ticks);
    RAISE NOTICE '- Oturumlar: % oturum', (SELECT COUNT(*) FROM user_sessions);
    RAISE NOTICE '';
    RAISE NOTICE 'Hazır brokerlar: AlgoLab, DenizBank, Garanti BBVA';
    RAISE NOTICE 'BIST30 sembolleri: GARAN, AKBNK, ISCTR, THYAO, TUPRS, ARCLK, vb.';
    RAISE NOTICE 'Test kullanıcıları: admin@bist.com.tr (admin123), trader@algolab.com.tr (trader123)';
END
$$;