-- Insert sample trading data for BIST Trading Platform
-- This includes representative BIST symbols, trading specifications, and test data

-- Insert sample BIST symbols with realistic trading specifications
INSERT INTO symbols (
    symbol_code, symbol_name, isin_code, market_segment, lot_size, tick_size,
    sector_code, industry_code, company_name, index_memberships,
    min_price, max_price, circuit_breaker_percentage, is_tradeable
) VALUES
    -- BIST 30 Index Members
    ('THYAO', 'Türk Hava Yolları A.O.', 'TRAEREGL91I1', 'ANA PAZAR', 1, 0.01,
     'ULASTIRMA', 'HAVA_TASIMACILIGI', 'Türk Hava Yolları Anonim Ortaklığı',
     '["BIST100", "BIST30", "XULAS"]'::JSONB, 1.00, 500.00, 10.0, true),

    ('ISCTR', 'Türkiye İş Bankası A.Ş.', 'TRAEREGL91I2', 'ANA PAZAR', 1, 0.01,
     'MALI_KURULUSLAR', 'BANKALAR', 'Türkiye İş Bankası Anonim Şirketi',
     '["BIST100", "BIST30", "XBANK"]'::JSONB, 0.50, 50.00, 10.0, true),

    ('AKBNK', 'Akbank T.A.Ş.', 'TRAKATMM0049', 'ANA PAZAR', 1, 0.01,
     'MALI_KURULUSLAR', 'BANKALAR', 'Akbank Türk Anonim Şirketi',
     '["BIST100", "BIST30", "XBANK"]'::JSONB, 1.00, 100.00, 10.0, true),

    ('GARAN', 'Garanti BBVA', 'TRAGARAI0025', 'ANA PAZAR', 1, 0.01,
     'MALI_KURULUSLAR', 'BANKALAR', 'Garanti Bankası Anonim Şirketi',
     '["BIST100", "BIST30", "XBANK"]'::JSONB, 1.00, 150.00, 10.0, true),

    ('ASELS', 'Aselsan A.Ş.', 'TRAASELS0028', 'ANA PAZAR', 1, 0.01,
     'TEKNOLOJI', 'SAVUNMA_SANAYI', 'Aselsan Elektronik Sanayi ve Ticaret Anonim Şirketi',
     '["BIST100", "BIST30", "XILTM"]'::JSONB, 5.00, 500.00, 10.0, true),

    ('BIMAS', 'BİM Birleşik Mağazalar A.Ş.', 'TRABIMAS0024', 'ANA PAZAR', 1, 0.01,
     'PERAKENDE_TICARET', 'GIDA_PERAKENDE', 'BİM Birleşik Mağazalar Anonim Şirketi',
     '["BIST100", "BIST30", "XTRZM"]'::JSONB, 10.00, 1000.00, 10.0, true),

    -- Technology Stocks
    ('LOGO', 'Logo Yazılım Sanayi ve Ticaret A.Ş.', 'TRALOGOA0013', 'ANA PAZAR', 1, 0.01,
     'TEKNOLOJI', 'YAZILIM', 'Logo Yazılım Sanayi ve Ticaret Anonim Şirketi',
     '["BIST100", "XILTM"]'::JSONB, 5.00, 200.00, 10.0, true),

    ('NETAS', 'Netaş Telekomünikasyon A.Ş.', 'TRANETAS0018', 'ANA PAZAR', 1, 0.01,
     'TEKNOLOJI', 'TELEKOMÜNIKASYON', 'Netaş Telekomünikasyon Anonim Şirketi',
     '["BIST100", "XILTM"]'::JSONB, 1.00, 100.00, 10.0, true),

    -- Energy Sector
    ('TUPRS', 'Tüpraş-Türkiye Petrol Rafinerileri A.Ş.', 'TRATUPRS0027', 'ANA PAZAR', 1, 0.01,
     'ENERJI', 'PETROL_RAFINAJ', 'Tüpraş-Türkiye Petrol Rafinerileri Anonim Şirketi',
     '["BIST100", "BIST30", "XPETK"]'::JSONB, 10.00, 1000.00, 10.0, true),

    ('PETKM', 'Petkim Petrokimya Holding A.Ş.', 'TRAPETKM0024', 'ANA PAZAR', 1, 0.01,
     'ENERJI', 'PETROKIMYA', 'Petkim Petrokimya Holding Anonim Şirketi',
     '["BIST100", "XPETK"]'::JSONB, 1.00, 50.00, 10.0, true),

    -- Emerging Companies Market
    ('PAPIL', 'Papilion Teknoloji A.Ş.', 'TRAPAPIL0019', 'GELİŞEN İŞLETMELER PAZARI', 1, 0.01,
     'TEKNOLOJI', 'YAZILIM', 'Papilion Teknoloji Anonim Şirketi',
     '[]'::JSONB, 0.10, 20.00, 20.0, true),

    -- Star Market
    ('PGSUS', 'Pegasus Hava Yolları A.Ş.', 'TRAPGSUS0029', 'YILDIZ PAZAR', 1, 0.01,
     'ULASTIRMA', 'HAVA_TASIMACILIGI', 'Pegasus Hava Yolları Anonim Şirketi',
     '["BIST100", "XULAS"]'::JSONB, 5.00, 300.00, 10.0, true),

    -- Testing symbols with different specifications
    ('TEST1', 'Test Company 1', 'TRATEST10001', 'ANA PAZAR', 10, 0.05,
     'TEST', 'TEST_INDUSTRY', 'Test Company 1 Inc.',
     '[]'::JSONB, 1.00, 100.00, 15.0, true),

    ('TEST2', 'Test Company 2', 'TRATEST20001', 'ANA PAZAR', 100, 0.10,
     'TEST', 'TEST_INDUSTRY', 'Test Company 2 Inc.',
     '[]'::JSONB, 10.00, 1000.00, 20.0, false), -- Not tradeable for testing

    ('MICRO', 'Micro Cap Test', 'TRAMICRO001', 'GELİŞEN İŞLETMELER PAZARI', 1000, 0.001,
     'TEST', 'MICRO_CAP', 'Micro Cap Test Company',
     '[]'::JSONB, 0.001, 1.00, 30.0, true);

-- Insert sample strategy definitions
INSERT INTO strategies (
    strategy_name, strategy_code, strategy_type, strategy_category,
    description, strategy_parameters, applicable_symbols, risk_profile,
    max_position_size, allocated_capital, created_by, strategy_owner
) VALUES
    ('BIST Bank Momentum', 'BANK_MOM_001', 'ALGORITHMIC', 'MOMENTUM',
     'Momentum strategy focused on BIST banking sector stocks',
     '{"lookback_period": 20, "momentum_threshold": 0.05, "rsi_upper": 70, "rsi_lower": 30}'::JSONB,
     '["AKBNK", "GARAN", "ISCTR"]'::JSONB, 'MEDIUM',
     1000000.00, 5000000.00, 'strategy_dev', 'fund_manager_1'),

    ('Tech Stock Mean Reversion', 'TECH_MR_001', 'ALGORITHMIC', 'MEAN_REVERSION',
     'Mean reversion strategy for technology stocks',
     '{"sma_period": 50, "deviation_threshold": 2.0, "holding_period": 5}'::JSONB,
     '["LOGO", "NETAS", "ASELS"]'::JSONB, 'HIGH',
     500000.00, 2000000.00, 'strategy_dev', 'tech_trader'),

    ('Energy Sector Arbitrage', 'ENERGY_ARB_001', 'ALGORITHMIC', 'ARBITRAGE',
     'Pairs trading strategy for energy sector stocks',
     '{"pair_1": "TUPRS", "pair_2": "PETKM", "correlation_threshold": 0.8}'::JSONB,
     '["TUPRS", "PETKM"]'::JSONB, 'LOW',
     2000000.00, 10000000.00, 'quant_team', 'energy_specialist'),

    ('Manual High Frequency', 'MANUAL_HF_001', 'MANUAL', 'SCALPING',
     'Manual high-frequency trading strategy',
     '{"max_holding_time_minutes": 5, "profit_target_bps": 10}'::JSONB,
     '["THYAO", "BIMAS"]'::JSONB, 'EXTREME',
     100000.00, 500000.00, 'trader_1', 'day_trader'),

    ('Test Strategy', 'TEST_STRAT_001', 'ALGORITHMIC', 'MOMENTUM',
     'Strategy for testing and development purposes',
     '{"test_param": "test_value", "debug_mode": true}'::JSONB,
     '["TEST1", "TEST2"]'::JSONB, 'LOW',
     10000.00, 50000.00, 'developer', 'test_user');

-- Insert sample strategy execution records
INSERT INTO strategy_executions (
    strategy_id, execution_session_id, execution_type, started_at, ended_at,
    orders_generated, orders_executed, realized_pnl, commission_paid,
    total_volume_traded, execution_status, execution_notes
) VALUES
    (1, 'BANK_MOM_001_20241201_001', 'LIVE',
     '2024-12-01 09:30:00+03', '2024-12-01 18:00:00+03',
     25, 20, 15750.50, 125.30, 2500000.00, 'COMPLETED',
     'Successful execution during high volume trading session'),

    (1, 'BANK_MOM_001_20241202_001', 'LIVE',
     '2024-12-02 09:30:00+03', '2024-12-02 18:00:00+03',
     18, 15, -3250.75, 95.20, 1800000.00, 'COMPLETED',
     'Lower performance due to market volatility'),

    (2, 'TECH_MR_001_20241201_001', 'BACKTEST',
     '2024-12-01 00:00:00+03', '2024-12-01 23:59:59+03',
     45, 40, 8950.25, 200.15, 4500000.00, 'COMPLETED',
     'Backtest showing positive results in tech sector'),

    (3, 'ENERGY_ARB_001_20241201_001', 'PAPER',
     '2024-12-01 10:00:00+03', '2024-12-01 16:00:00+03',
     12, 12, 5420.80, 85.60, 6000000.00, 'COMPLETED',
     'Paper trading session with good arbitrage opportunities'),

    (5, 'TEST_STRAT_001_20241201_001', 'BACKTEST',
     '2024-12-01 12:00:00+03', '2024-12-01 14:00:00+03',
     5, 3, -150.00, 15.00, 50000.00, 'COMPLETED',
     'Test execution for development purposes');

-- Insert sample broker API log entries
INSERT INTO broker_api_logs (
    request_id, api_provider, endpoint_path, http_method,
    request_timestamp, response_timestamp, response_status_code,
    latency_ms, user_id, operation_type, affected_symbols,
    is_automated, request_source
) VALUES
    ('req_001_20241201_093001', 'ALGOLAB', '/api/v1/orders', 'POST',
     '2024-12-01 09:30:01+03', '2024-12-01 09:30:01.250+03', 200,
     250, 'trader_001', 'PLACE_ORDER', '["AKBNK"]'::JSONB,
     true, 'API_CLIENT'),

    ('req_002_20241201_093015', 'ALGOLAB', '/api/v1/positions', 'GET',
     '2024-12-01 09:30:15+03', '2024-12-01 09:30:15.150+03', 200,
     150, 'trader_001', 'GET_POSITIONS', '[]'::JSONB,
     true, 'WEB_UI'),

    ('req_003_20241201_093030', 'ALGOLAB', '/api/v1/orders/12345', 'DELETE',
     '2024-12-01 09:30:30+03', '2024-12-01 09:30:30.500+03', 404,
     500, 'trader_001', 'CANCEL_ORDER', '["GARAN"]'::JSONB,
     false, 'MOBILE_APP'),

    ('req_004_20241201_093045', 'ALGOLAB', '/api/v1/market-data/THYAO', 'GET',
     '2024-12-01 09:30:45+03', '2024-12-01 09:30:45.050+03', 200,
     50, 'system', 'GET_MARKET_DATA', '["THYAO"]'::JSONB,
     true, 'SCHEDULER'),

    ('req_005_20241201_100000', 'ALGOLAB', '/api/v1/orders', 'POST',
     '2024-12-01 10:00:00+03', '2024-12-01 10:00:05+03', 429,
     5000, 'trader_002', 'PLACE_ORDER', '["BIMAS"]'::JSONB,
     true, 'API_CLIENT'),

    -- Add some error scenarios
    ('req_error_001', 'ALGOLAB', '/api/v1/orders', 'POST',
     '2024-12-01 11:30:00+03', '2024-12-01 11:30:10+03', 500,
     10000, 'trader_003', 'PLACE_ORDER', '["TEST1"]'::JSONB,
     true, 'API_CLIENT'),

    ('req_error_002', 'ALGOLAB', '/api/v1/auth/login', 'POST',
     '2024-12-01 14:15:00+03', '2024-12-01 14:15:02+03', 401,
     2000, 'invalid_user', 'AUTHENTICATION', '[]'::JSONB,
     false, 'WEB_UI');

-- Update broker API logs with additional error details for error cases
UPDATE broker_api_logs
SET
    error_code = 'INTERNAL_SERVER_ERROR',
    error_message = 'Database connection timeout',
    error_type = 'SERVER_ERROR',
    rate_limit_exceeded = false
WHERE request_id = 'req_error_001';

UPDATE broker_api_logs
SET
    error_code = 'INVALID_CREDENTIALS',
    error_message = 'Invalid username or password',
    error_type = 'AUTHENTICATION',
    rate_limit_exceeded = false
WHERE request_id = 'req_error_002';

UPDATE broker_api_logs
SET
    error_code = 'RATE_LIMIT_EXCEEDED',
    error_message = 'Too many requests, please try again later',
    error_type = 'RATE_LIMIT',
    rate_limit_exceeded = true,
    rate_limit_remaining = 0,
    rate_limit_reset_timestamp = '2024-12-01 10:01:00+03'
WHERE request_id = 'req_005_20241201_100000';

-- Create some sample order execution records
-- First, let's insert some sample orders to reference
INSERT INTO orders (
    account_id, symbol_code, order_type, side, quantity, price,
    time_in_force, status, created_by, session_id
) VALUES
    (1, 'AKBNK', 'LIMIT', 'BUY', 1000, 45.50, 'DAY', 'PARTIALLY_FILLED', 'trader_001', 'session_001'),
    (1, 'GARAN', 'MARKET', 'SELL', 500, NULL, 'DAY', 'FILLED', 'trader_001', 'session_001'),
    (2, 'THYAO', 'LIMIT', 'BUY', 200, 125.75, 'GTC', 'FILLED', 'trader_002', 'session_002'),
    (2, 'BIMAS', 'STOP_LOSS', 'SELL', 100, 450.00, 'DAY', 'CANCELLED', 'trader_002', 'session_002');

-- Now insert corresponding order executions
INSERT INTO order_executions (
    order_id, execution_id, executed_quantity, execution_price,
    execution_time, trade_date, commission_amount, exchange_fee,
    execution_venue, liquidity_flag, is_partial_fill, remaining_quantity
) VALUES
    -- Partial fill for AKBNK order
    (1, 'EXEC_001_AKBNK_001', 600, 45.48, '2024-12-01 09:30:15+03', '2024-12-01',
     15.25, 2.50, 'BIST', 'TAKER', true, 400),

    -- Second fill for AKBNK order
    (1, 'EXEC_001_AKBNK_002', 400, 45.52, '2024-12-01 10:15:30+03', '2024-12-01',
     10.20, 1.75, 'BIST', 'MAKER', false, 0),

    -- Full fill for GARAN market order
    (2, 'EXEC_002_GARAN_001', 500, 78.25, '2024-12-01 09:32:00+03', '2024-12-01',
     19.55, 3.25, 'BIST', 'TAKER', false, 0),

    -- Full fill for THYAO order
    (3, 'EXEC_003_THYAO_001', 200, 125.75, '2024-12-01 11:45:00+03', '2024-12-01',
     12.58, 2.10, 'BIST', 'MAKER', false, 0);

-- Update order executions with additional context
UPDATE order_executions
SET
    counterparty_type = 'MARKET_MAKER',
    best_bid_price = 45.47,
    best_ask_price = 45.49,
    settlement_status = 'SETTLED',
    settlement_date = '2024-12-03'
WHERE execution_id = 'EXEC_001_AKBNK_001';

-- Add some performance summary comments
COMMENT ON TABLE symbols IS 'Sample data includes major BIST stocks with realistic trading specifications';
COMMENT ON TABLE strategies IS 'Sample strategies cover different trading approaches and risk profiles';
COMMENT ON TABLE strategy_executions IS 'Sample execution data shows both successful and failed strategy runs';
COMMENT ON TABLE broker_api_logs IS 'Sample API logs include normal operations, errors, and rate limiting scenarios';
COMMENT ON TABLE order_executions IS 'Sample executions demonstrate partial fills, different venues, and fee calculations';