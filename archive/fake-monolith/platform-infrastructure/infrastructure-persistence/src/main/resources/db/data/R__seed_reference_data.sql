-- =============================================================================
-- BIST Trading Platform - Reference Data Seeding
-- Repeatable migration for reference data that can be updated
-- =============================================================================

-- =============================================================================
-- TURKISH MARKET SYMBOLS (BIST)
-- =============================================================================

-- Get BIST market ID
DO $$
DECLARE
    bist_market_id UUID;
    banking_sector_id UUID;
    tech_sector_id UUID;
    energy_sector_id UUID;
    telecom_sector_id UUID;
    retail_sector_id UUID;
    manuf_sector_id UUID;
    real_estate_sector_id UUID;
    insurance_sector_id UUID;
    transport_sector_id UUID;
    food_sector_id UUID;
BEGIN
    -- Get market and sector IDs
    SELECT market_id INTO bist_market_id FROM reference.markets WHERE market_code = 'BIST';
    SELECT sector_id INTO banking_sector_id FROM reference.sectors WHERE sector_code = 'BANK';
    SELECT sector_id INTO tech_sector_id FROM reference.sectors WHERE sector_code = 'TECH';
    SELECT sector_id INTO energy_sector_id FROM reference.sectors WHERE sector_code = 'ENERGY';
    SELECT sector_id INTO telecom_sector_id FROM reference.sectors WHERE sector_code = 'TELECOM';
    SELECT sector_id INTO retail_sector_id FROM reference.sectors WHERE sector_code = 'RETAIL';
    SELECT sector_id INTO manuf_sector_id FROM reference.sectors WHERE sector_code = 'MANUF';
    SELECT sector_id INTO real_estate_sector_id FROM reference.sectors WHERE sector_code = 'REAL_ESTATE';
    SELECT sector_id INTO insurance_sector_id FROM reference.sectors WHERE sector_code = 'INSURANCE';
    SELECT sector_id INTO transport_sector_id FROM reference.sectors WHERE sector_code = 'TRANSPORT';
    SELECT sector_id INTO food_sector_id FROM reference.sectors WHERE sector_code = 'FOOD';

    -- Banking sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('AKBNK', bist_market_id, banking_sector_id, 'Akbank T.A.Ş.', 'AKBANK', 'TRAAKBNK91E2', 1, 0.01, 1, 999999999, 2, 0, '1990-01-02'),
        ('GARAN', bist_market_id, banking_sector_id, 'Türkiye Garanti Bankası A.Ş.', 'GARANTI', 'TREGARN91EA6', 1, 0.01, 1, 999999999, 2, 0, '1990-01-02'),
        ('ISCTR', bist_market_id, banking_sector_id, 'Türkiye İş Bankası A.Ş.', 'İŞ BANKASI', 'TREISCTR91N1', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02'),
        ('YKBNK', bist_market_id, banking_sector_id, 'Yapı ve Kredi Bankası A.Ş.', 'YAPI KREDİ', 'TREYKBNK91N2', 1, 0.01, 1, 999999999, 2, 0, '1987-01-02'),
        ('HALKB', bist_market_id, banking_sector_id, 'Türkiye Halk Bankası A.Ş.', 'HALKBANK', 'TREHALKB91W5', 1, 0.01, 1, 999999999, 2, 0, '1988-06-15'),
        ('VAKBN', bist_market_id, banking_sector_id, 'Türkiye Vakıflar Bankası T.A.O.', 'VakıfBank', 'TREVAKBN91P9', 1, 0.01, 1, 999999999, 2, 0, '2005-04-22')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Technology sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('ASELS', bist_market_id, tech_sector_id, 'Aselsan Elektronik Sanayi ve Ticaret A.Ş.', 'ASELSAN', 'TREASELS91AP', 1, 0.01, 1, 999999999, 2, 0, '1991-02-04'),
        ('LOGO', bist_market_id, tech_sector_id, 'Logo Yazılım Sanayi ve Ticaret A.Ş.', 'LOGO', 'TRELOGO091R4', 1, 0.01, 1, 999999999, 2, 0, '2000-07-28'),
        ('NETAS', bist_market_id, tech_sector_id, 'Netaş Telekomünikasyon A.Ş.', 'NETAŞ', 'TRENETAS91R8', 1, 0.01, 1, 999999999, 2, 0, '1992-01-06'),
        ('ARMDA', bist_market_id, tech_sector_id, 'Armada Bilgisayar Sistemleri Sanayi ve Ticaret A.Ş.', 'ARMADA', 'TREARMDA91R2', 1, 0.01, 1, 999999999, 2, 0, '2000-11-27')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Energy sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('TUPRS', bist_market_id, energy_sector_id, 'Türkiye Petrol Rafinerileri A.Ş.', 'TÜPRAŞ', 'TRETUPRS91T1', 1, 0.01, 1, 999999999, 2, 0, '1991-07-11'),
        ('PETKM', bist_market_id, energy_sector_id, 'Petkim Petrokimya Holding A.Ş.', 'PETKİM', 'TREPETKM91N6', 1, 0.01, 1, 999999999, 2, 0, '1990-01-02'),
        ('AKSEN', bist_market_id, energy_sector_id, 'Aksa Enerji Üretim A.Ş.', 'AKSA ENERJİ', 'TREAKSEN91E8', 1, 0.01, 1, 999999999, 2, 0, '2007-02-16'),
        ('ZOREN', bist_market_id, energy_sector_id, 'Zorlu Enerji Elektrik Üretim A.Ş.', 'ZORLU ENERJİ', 'TREZOREN91E4', 1, 0.01, 1, 999999999, 2, 0, '2007-07-06')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Telecom sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('TTKOM', bist_market_id, telecom_sector_id, 'Türk Telekomünikasyon A.Ş.', 'TÜRK TELEKOM', 'TRETTKOM00E8', 1, 0.01, 1, 999999999, 2, 0, '2008-05-15'),
        ('THYAO', bist_market_id, transport_sector_id, 'Türk Hava Yolları A.O.', 'THY', 'TRETHYAO91N4', 1, 0.01, 1, 999999999, 2, 0, '1990-01-02')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Retail and consumer sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('BIMAS', bist_market_id, retail_sector_id, 'BİM Birleşik Mağazalar A.Ş.', 'BİM', 'TREBIMAS91R7', 1, 0.01, 1, 999999999, 2, 0, '1995-06-20'),
        ('MGROS', bist_market_id, retail_sector_id, 'Migros Ticaret A.Ş.', 'MİGROS', 'TREMGROS91R3', 1, 0.01, 1, 999999999, 2, 0, '1991-01-28'),
        ('SOKM', bist_market_id, retail_sector_id, 'Şok Marketler Ticaret A.Ş.', 'ŞOK', 'TRESOKM091E5', 1, 0.01, 1, 999999999, 2, 0, '2018-06-20'),
        ('CARSI', bist_market_id, retail_sector_id, 'Carsi Ticaret A.Ş.', 'CARSI', 'TRECARSI91E9', 1, 0.01, 1, 999999999, 2, 0, '2013-07-19')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Manufacturing sector symbols
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('ARCLK', bist_market_id, manuf_sector_id, 'Arçelik A.Ş.', 'ARÇELİK', 'TREARCLK91E1', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02'),
        ('KCHOL', bist_market_id, manuf_sector_id, 'Koç Holding A.Ş.', 'KOÇ HOLDİNG', 'TREKCHOL91E3', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02'),
        ('SAHOL', bist_market_id, manuf_sector_id, 'Sabancı Holding A.Ş.', 'SABANCI HOLDİNG', 'TRESAHOL91E7', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02'),
        ('EREGL', bist_market_id, manuf_sector_id, 'Ereğli Demir ve Çelik Fabrikaları T.A.Ş.', 'EREĞLİ DEMİR ÇELİK', 'TREEREGL91E9', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Real Estate Investment Trusts
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('EKGYO', real_estate_sector_id, bist_market_id, 'Emlak Konut Gayrimenkul Yatırım Ortaklığı A.Ş.', 'EMLAK KONUT GYO', 'TREEKGYO91E7', 1, 0.01, 1, 999999999, 2, 0, '2010-12-10'),
        ('ISGYO', real_estate_sector_id, bist_market_id, 'İş Gayrimenkul Yatırım Ortaklığı A.Ş.', 'İŞ GYO', 'TREISGYO91E1', 1, 0.01, 1, 999999999, 2, 0, '2007-04-18'),
        ('TRGYO', real_estate_sector_id, bist_market_id, 'Torunlar Gayrimenkul Yatırım Ortaklığı A.Ş.', 'TORUNLAR GYO', 'TRETRGYO91E5', 1, 0.01, 1, 999999999, 2, 0, '2010-10-25')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

    -- Food and beverage sector
    INSERT INTO reference.symbols (
        symbol, market_id, sector_id, full_name, short_name, isin_code,
        lot_size, tick_size, min_order_quantity, max_order_quantity,
        price_precision, quantity_precision, listing_date
    ) VALUES
        ('ULKER', bist_market_id, food_sector_id, 'Ülker Bisküvi Sanayi A.Ş.', 'ÜLKER', 'TREULKER91N0', 1, 0.01, 1, 999999999, 2, 0, '1993-01-25'),
        ('FROTO', bist_market_id, food_sector_id, 'Ford Otomotiv Sanayi A.Ş.', 'FORD OTO', 'TREFROTO91E2', 1, 0.01, 1, 999999999, 2, 0, '2004-06-28'),
        ('CCOLA', bist_market_id, food_sector_id, 'Coca-Cola İçecek A.Ş.', 'COCA-COLA İÇECEK', 'TRECCOLA91E6', 1, 0.01, 1, 999999999, 2, 0, '2006-09-29'),
        ('AEFES', bist_market_id, food_sector_id, 'Anadolu Efes Biracılık ve Malt Sanayii A.Ş.', 'ANADOLU EFES', 'TREAEFES91E0', 1, 0.01, 1, 999999999, 2, 0, '1986-01-02')
    ON CONFLICT (symbol, market_id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        short_name = EXCLUDED.short_name,
        updated_at = NOW();

END $$;

-- =============================================================================
-- TRADING HOURS AND MARKET SESSIONS
-- =============================================================================

-- Create trading sessions table if not exists
CREATE TABLE IF NOT EXISTS reference.trading_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    market_id UUID NOT NULL REFERENCES reference.markets(market_id),
    session_name VARCHAR(50) NOT NULL,
    session_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    days_of_week INTEGER[] NOT NULL DEFAULT '{1,2,3,4,5}', -- Monday to Friday
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_session_type CHECK (session_type IN ('PRE_MARKET', 'REGULAR', 'POST_MARKET', 'EXTENDED')),
    CONSTRAINT chk_days_of_week CHECK (array_length(days_of_week, 1) > 0),
    CONSTRAINT chk_time_order CHECK (start_time < end_time),
    CONSTRAINT uk_market_session_name UNIQUE (market_id, session_name)
);

-- Insert BIST trading sessions
DO $$
DECLARE
    bist_market_id UUID;
BEGIN
    SELECT market_id INTO bist_market_id FROM reference.markets WHERE market_code = 'BIST';

    INSERT INTO reference.trading_sessions (
        market_id, session_name, session_type, start_time, end_time, days_of_week, timezone
    ) VALUES
        (bist_market_id, 'Pre-Market', 'PRE_MARKET', '09:00:00', '09:30:00', '{1,2,3,4,5}', 'Europe/Istanbul'),
        (bist_market_id, 'Morning Session', 'REGULAR', '09:30:00', '12:30:00', '{1,2,3,4,5}', 'Europe/Istanbul'),
        (bist_market_id, 'Afternoon Session', 'REGULAR', '14:00:00', '18:00:00', '{1,2,3,4,5}', 'Europe/Istanbul'),
        (bist_market_id, 'Post-Market', 'POST_MARKET', '18:00:00', '18:30:00', '{1,2,3,4,5}', 'Europe/Istanbul')
    ON CONFLICT (market_id, session_name) DO UPDATE SET
        start_time = EXCLUDED.start_time,
        end_time = EXCLUDED.end_time,
        updated_at = NOW();
END $$;

-- =============================================================================
-- MARKET HOLIDAYS
-- =============================================================================

-- Create market holidays table if not exists
CREATE TABLE IF NOT EXISTS reference.market_holidays (
    holiday_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    market_id UUID NOT NULL REFERENCES reference.markets(market_id),
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(200) NOT NULL,
    holiday_type VARCHAR(50) NOT NULL DEFAULT 'NATIONAL',
    is_full_day BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_holiday_type CHECK (holiday_type IN ('NATIONAL', 'RELIGIOUS', 'MARKET_SPECIFIC', 'EMERGENCY')),
    CONSTRAINT uk_market_holiday_date UNIQUE (market_id, holiday_date)
);

-- Insert Turkish market holidays for current year
DO $$
DECLARE
    bist_market_id UUID;
    current_year INTEGER := EXTRACT(YEAR FROM CURRENT_DATE);
BEGIN
    SELECT market_id INTO bist_market_id FROM reference.markets WHERE market_code = 'BIST';

    INSERT INTO reference.market_holidays (
        market_id, holiday_date, holiday_name, holiday_type, description
    ) VALUES
        -- National holidays
        (bist_market_id, (current_year || '-01-01')::DATE, 'Yılbaşı', 'NATIONAL', 'New Year''s Day'),
        (bist_market_id, (current_year || '-04-23')::DATE, 'Ulusal Egemenlik ve Çocuk Bayramı', 'NATIONAL', 'National Sovereignty and Children''s Day'),
        (bist_market_id, (current_year || '-05-01')::DATE, 'İşçi Bayramı', 'NATIONAL', 'Labour Day'),
        (bist_market_id, (current_year || '-05-19')::DATE, 'Atatürk''ü Anma, Gençlik ve Spor Bayramı', 'NATIONAL', 'Commemoration of Atatürk, Youth and Sports Day'),
        (bist_market_id, (current_year || '-07-15')::DATE, '15 Temmuz Demokrasi ve Milli Birlik Günü', 'NATIONAL', 'Democracy and National Unity Day'),
        (bist_market_id, (current_year || '-08-30')::DATE, 'Zafer Bayramı', 'NATIONAL', 'Victory Day'),
        (bist_market_id, (current_year || '-10-29')::DATE, 'Cumhuriyet Bayramı', 'NATIONAL', 'Republic Day')

        -- Religious holidays (dates vary each year - these are placeholders)
        -- In real implementation, these would be calculated based on lunar calendar
        -- (bist_market_id, (current_year || '-04-10')::DATE, 'Ramazan Bayramı 1. Gün', 'RELIGIOUS', 'Eid al-Fitr Day 1'),
        -- (bist_market_id, (current_year || '-04-11')::DATE, 'Ramazan Bayramı 2. Gün', 'RELIGIOUS', 'Eid al-Fitr Day 2'),
        -- (bist_market_id, (current_year || '-04-12')::DATE, 'Ramazan Bayramı 3. Gün', 'RELIGIOUS', 'Eid al-Fitr Day 3')

    ON CONFLICT (market_id, holiday_date) DO UPDATE SET
        holiday_name = EXCLUDED.holiday_name,
        description = EXCLUDED.description;
END $$;

-- =============================================================================
-- CURRENCY EXCHANGE RATES (Sample data)
-- =============================================================================

-- Create exchange rates table if not exists
CREATE TABLE IF NOT EXISTS reference.exchange_rates (
    rate_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(20,8) NOT NULL,
    rate_date DATE NOT NULL DEFAULT CURRENT_DATE,
    rate_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rate_type VARCHAR(20) NOT NULL DEFAULT 'SPOT',
    data_source VARCHAR(50) NOT NULL DEFAULT 'TCMB',
    is_active BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_currencies CHECK (LENGTH(from_currency) = 3 AND LENGTH(to_currency) = 3),
    CONSTRAINT chk_rate CHECK (rate > 0),
    CONSTRAINT chk_rate_type CHECK (rate_type IN ('SPOT', 'FORWARD', 'SWAP')),
    CONSTRAINT chk_different_currencies CHECK (from_currency != to_currency),
    CONSTRAINT uk_currency_pair_date UNIQUE (from_currency, to_currency, rate_date, rate_type)
);

-- Insert sample exchange rates
INSERT INTO reference.exchange_rates (
    from_currency, to_currency, rate, rate_date, rate_type, data_source
) VALUES
    ('USD', 'TRY', 29.50, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('EUR', 'TRY', 31.75, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('GBP', 'TRY', 36.80, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('CHF', 'TRY', 32.20, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('JPY', 'TRY', 0.197, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('TRY', 'USD', 0.0339, CURRENT_DATE, 'SPOT', 'TCMB'),
    ('TRY', 'EUR', 0.0315, CURRENT_DATE, 'SPOT', 'TCMB')
ON CONFLICT (from_currency, to_currency, rate_date, rate_type) DO UPDATE SET
    rate = EXCLUDED.rate,
    rate_time = EXCLUDED.rate_time,
    is_active = EXCLUDED.is_active;

-- =============================================================================
-- TRADING PARAMETERS AND LIMITS
-- =============================================================================

-- Create trading parameters table if not exists
CREATE TABLE IF NOT EXISTS reference.trading_parameters (
    param_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    market_id UUID REFERENCES reference.markets(market_id),
    symbol_id UUID REFERENCES reference.symbols(symbol_id),
    param_name VARCHAR(100) NOT NULL,
    param_value TEXT NOT NULL,
    param_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_param_type CHECK (param_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON')),
    CONSTRAINT chk_effective_dates CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT uk_param_effective UNIQUE (market_id, symbol_id, param_name, effective_from)
);

-- Insert BIST trading parameters
DO $$
DECLARE
    bist_market_id UUID;
BEGIN
    SELECT market_id INTO bist_market_id FROM reference.markets WHERE market_code = 'BIST';

    INSERT INTO reference.trading_parameters (
        market_id, param_name, param_value, param_type, description
    ) VALUES
        (bist_market_id, 'MAX_DAILY_PRICE_CHANGE_PERCENT', '10.0', 'NUMBER', 'Maximum daily price change percentage'),
        (bist_market_id, 'MIN_ORDER_VALUE', '100.00', 'NUMBER', 'Minimum order value in TRY'),
        (bist_market_id, 'COMMISSION_RATE', '0.00188', 'NUMBER', 'Default commission rate'),
        (bist_market_id, 'SETTLEMENT_DAYS', '2', 'NUMBER', 'Trade settlement period in days'),
        (bist_market_id, 'MARGIN_REQUIREMENT', '0.20', 'NUMBER', 'Minimum margin requirement (20%)'),
        (bist_market_id, 'MAX_LEVERAGE', '5.0', 'NUMBER', 'Maximum allowed leverage'),
        (bist_market_id, 'PRICE_BAND_ENABLED', 'true', 'BOOLEAN', 'Whether price bands are enabled'),
        (bist_market_id, 'CIRCUIT_BREAKER_ENABLED', 'true', 'BOOLEAN', 'Whether circuit breakers are enabled')
    ON CONFLICT (market_id, symbol_id, param_name, effective_from) DO UPDATE SET
        param_value = EXCLUDED.param_value,
        updated_at = NOW();
END $$;

-- =============================================================================
-- INDICES AND PERFORMANCE OPTIMIZATION
-- =============================================================================

-- Create indexes for new tables
CREATE INDEX IF NOT EXISTS idx_trading_sessions_market ON reference.trading_sessions (market_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_market_holidays_market_date ON reference.market_holidays (market_id, holiday_date);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_currencies_date ON reference.exchange_rates (from_currency, to_currency, rate_date) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_trading_parameters_market ON reference.trading_parameters (market_id, effective_from, effective_to);

-- =============================================================================
-- UPDATE STATISTICS
-- =============================================================================

-- Update table statistics for better query performance
ANALYZE reference.markets;
ANALYZE reference.sectors;
ANALYZE reference.symbols;
ANALYZE reference.trading_sessions;
ANALYZE reference.market_holidays;
ANALYZE reference.exchange_rates;
ANALYZE reference.trading_parameters;

-- =============================================================================
-- REFERENCE DATA SEEDING COMPLETE
-- =============================================================================