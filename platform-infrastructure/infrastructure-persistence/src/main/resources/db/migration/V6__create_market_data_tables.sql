-- Piyasa verisi tabloları - TimescaleDB hypertables ile yüksek performans
-- Bu migration TimescaleDB uzantısını kullanarak zaman serisi tablolarını oluşturur

-- TimescaleDB uzantısını etkinleştir (varsa)
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- 1. Market Ticks Tablosu - En düşük seviye fiyat verileri
CREATE TABLE market_ticks (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(20) DEFAULT 'BIST',

    -- Fiyat bilgileri
    price DECIMAL(12,4) NOT NULL,
    volume DECIMAL(15,4) NOT NULL DEFAULT 0,

    -- Tick türü
    tick_type VARCHAR(10) CHECK (tick_type IN ('TRADE', 'BID', 'ASK', 'LAST')) DEFAULT 'TRADE',

    -- İşlem detayları
    trade_id VARCHAR(50),
    trade_condition VARCHAR(20), -- 'NORMAL', 'OPENING', 'CLOSING', 'BLOCK', 'CROSS'

    -- Market depth bilgileri
    bid_price DECIMAL(12,4),
    bid_volume DECIMAL(15,4),
    ask_price DECIMAL(12,4),
    ask_volume DECIMAL(15,4),

    -- Spread ve volatilite
    spread DECIMAL(12,6),
    tick_direction SMALLINT CHECK (tick_direction IN (-1, 0, 1)), -- -1:down, 0:unchanged, 1:up

    -- Metadata
    sequence_number BIGINT,
    market_session VARCHAR(20) DEFAULT 'REGULAR',
    data_quality SMALLINT DEFAULT 1 CHECK (data_quality BETWEEN 1 AND 5),

    -- Constraints
    CONSTRAINT pk_market_ticks PRIMARY KEY (time, symbol)
);

-- TimescaleDB hypertable oluştur
SELECT create_hypertable(
    'market_ticks',
    'time',
    chunk_time_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

-- 2. Market Candles Tablosu - OHLCV mum verileri
CREATE TABLE market_candles (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL CHECK (timeframe IN ('1m', '5m', '15m', '1h', '4h', '1d', '1w', '1M')),
    exchange VARCHAR(20) DEFAULT 'BIST',

    -- OHLCV verileri
    open_price DECIMAL(12,4) NOT NULL,
    high_price DECIMAL(12,4) NOT NULL,
    low_price DECIMAL(12,4) NOT NULL,
    close_price DECIMAL(12,4) NOT NULL,
    volume DECIMAL(15,4) NOT NULL DEFAULT 0,

    -- Ek istatistikler
    vwap DECIMAL(12,4), -- Volume Weighted Average Price
    trade_count INTEGER DEFAULT 0,
    turnover DECIMAL(19,4) DEFAULT 0, -- Ciro

    -- Teknik indikatörler (pre-calculated)
    sma_20 DECIMAL(12,4), -- 20 period Simple Moving Average
    ema_12 DECIMAL(12,4), -- 12 period Exponential Moving Average
    ema_26 DECIMAL(12,4), -- 26 period EMA
    rsi_14 DECIMAL(8,4), -- 14 period RSI

    -- Bollinger Bands
    bb_upper DECIMAL(12,4),
    bb_middle DECIMAL(12,4),
    bb_lower DECIMAL(12,4),

    -- MACD
    macd_line DECIMAL(12,6),
    macd_signal DECIMAL(12,6),
    macd_histogram DECIMAL(12,6),

    -- Volatilite ve momentum
    atr_14 DECIMAL(12,4), -- Average True Range
    volatility DECIMAL(8,6), -- Realized volatility

    -- İstatistiksel veriler
    tick_count INTEGER DEFAULT 0,
    gap_from_previous DECIMAL(12,6), -- Önceki kapanıştan fark

    -- Metadata
    is_complete BOOLEAN DEFAULT FALSE, -- Mum tamamlandı mı
    data_quality SMALLINT DEFAULT 1 CHECK (data_quality BETWEEN 1 AND 5),

    -- Constraints
    CONSTRAINT pk_market_candles PRIMARY KEY (time, symbol, timeframe),
    CONSTRAINT chk_ohlc_valid CHECK (
        high_price >= open_price AND high_price >= close_price AND
        low_price <= open_price AND low_price <= close_price AND
        high_price >= low_price
    )
);

-- TimescaleDB hypertable oluştur
SELECT create_hypertable(
    'market_candles',
    'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- 3. Order Book Tablosu - Emir defteri derinliği
CREATE TABLE order_book (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(20) DEFAULT 'BIST',

    -- Emir defteri seviyesi (1-20 arası derinlik)
    level_no SMALLINT NOT NULL CHECK (level_no BETWEEN 1 AND 20),

    -- Alış tarafı
    bid_price DECIMAL(12,4),
    bid_volume DECIMAL(15,4),
    bid_order_count INTEGER,

    -- Satış tarafı
    ask_price DECIMAL(12,4),
    ask_volume DECIMAL(15,4),
    ask_order_count INTEGER,

    -- Spread bilgileri
    spread_amount DECIMAL(12,6),
    spread_percent DECIMAL(8,6),

    -- Mid price
    mid_price DECIMAL(12,4),

    -- Likidite metrikleri
    total_bid_volume DECIMAL(15,4), -- Toplam alış hacmi
    total_ask_volume DECIMAL(15,4), -- Toplam satış hacmi
    imbalance_ratio DECIMAL(8,4), -- Dengesizlik oranı

    -- Metadata
    sequence_number BIGINT,
    update_type VARCHAR(10) CHECK (update_type IN ('FULL', 'INCREMENTAL')),
    data_quality SMALLINT DEFAULT 1 CHECK (data_quality BETWEEN 1 AND 5),

    -- Constraints
    CONSTRAINT pk_order_book PRIMARY KEY (time, symbol, level_no)
);

-- TimescaleDB hypertable oluştur
SELECT create_hypertable(
    'order_book',
    'time',
    chunk_time_interval => INTERVAL '30 minutes',
    if_not_exists => TRUE
);

-- 4. Market Statistics Tablosu - Günlük piyasa istatistikleri
CREATE TABLE market_statistics (
    date DATE NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(20) DEFAULT 'BIST',

    -- Günlük fiyat bilgileri
    open_price DECIMAL(12,4) NOT NULL,
    high_price DECIMAL(12,4) NOT NULL,
    low_price DECIMAL(12,4) NOT NULL,
    close_price DECIMAL(12,4) NOT NULL,
    previous_close DECIMAL(12,4),

    -- Hacim ve işlem
    volume DECIMAL(15,4) NOT NULL DEFAULT 0,
    turnover DECIMAL(19,4) NOT NULL DEFAULT 0,
    trade_count INTEGER DEFAULT 0,
    vwap DECIMAL(12,4),

    -- Değişim metrikleri
    change_amount DECIMAL(12,4),
    change_percent DECIMAL(8,4),

    -- Volatilite
    daily_volatility DECIMAL(8,6),
    intraday_volatility DECIMAL(8,6),

    -- Circuit breaker seviyeleri
    upper_limit DECIMAL(12,4), -- Üst fiyat bandı
    lower_limit DECIMAL(12,4), -- Alt fiyat bandı

    -- Piyasa değeri (hisseler için)
    market_cap DECIMAL(19,4),
    shares_outstanding BIGINT,

    -- Likidite metrikleri
    avg_bid_ask_spread DECIMAL(12,6),
    liquidity_score DECIMAL(8,4),

    -- Sektör ve endeks bilgileri
    sector VARCHAR(50),
    sub_sector VARCHAR(50),
    index_weight DECIMAL(8,6), -- BIST100 ağırlığı

    -- Trading durumu
    trading_status VARCHAR(20) DEFAULT 'TRADING',
    halt_reason VARCHAR(100),
    halt_start_time TIMESTAMPTZ,
    halt_end_time TIMESTAMPTZ,

    -- Metadata
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    data_quality SMALLINT DEFAULT 1 CHECK (data_quality BETWEEN 1 AND 5),

    -- Constraints
    CONSTRAINT pk_market_statistics PRIMARY KEY (date, symbol)
);

-- 5. Economic Events Tablosu - Ekonomik olaylar ve haberler
CREATE TABLE economic_events (
    id VARCHAR(36) PRIMARY KEY,
    event_time TIMESTAMPTZ NOT NULL,

    -- Olay detayları
    title VARCHAR(500) NOT NULL,
    description TEXT,
    event_type VARCHAR(50), -- 'EARNINGS', 'DIVIDEND', 'SPLIT', 'MERGER', 'NEWS', 'MACRO'
    importance VARCHAR(10) CHECK (importance IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

    -- Etkilenen enstrümanlar
    affected_symbols TEXT[], -- Array of symbols
    affected_sectors TEXT[], -- Array of sectors

    -- Ekonomik veriler
    actual_value DECIMAL(19,4),
    forecast_value DECIMAL(19,4),
    previous_value DECIMAL(19,4),
    unit VARCHAR(20), -- '%', 'M TRY', 'B USD' vb.

    -- Coğrafi kapsam
    country VARCHAR(3) DEFAULT 'TUR',
    region VARCHAR(50),

    -- Kaynak bilgileri
    source VARCHAR(100),
    source_url TEXT,
    reliability_score SMALLINT CHECK (reliability_score BETWEEN 1 AND 5),

    -- İşlem bilgileri
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Performans indexleri oluştur

-- Market Ticks indexleri
CREATE INDEX idx_market_ticks_symbol_time ON market_ticks (symbol, time DESC);
CREATE INDEX idx_market_ticks_exchange ON market_ticks (exchange, time DESC);
CREATE INDEX idx_market_ticks_volume ON market_ticks (volume DESC, time DESC) WHERE volume > 0;
CREATE INDEX idx_market_ticks_price_change ON market_ticks (symbol, tick_direction, time DESC);

-- Market Candles indexleri
CREATE INDEX idx_market_candles_symbol_timeframe ON market_candles (symbol, timeframe, time DESC);
CREATE INDEX idx_market_candles_volume ON market_candles (symbol, volume DESC, time DESC);
CREATE INDEX idx_market_candles_complete ON market_candles (symbol, timeframe, time DESC) WHERE is_complete = TRUE;
CREATE INDEX idx_market_candles_high_volume ON market_candles (symbol, time DESC) WHERE volume > 1000000;

-- Order Book indexleri
CREATE INDEX idx_order_book_symbol_time ON order_book (symbol, time DESC);
CREATE INDEX idx_order_book_level ON order_book (symbol, level_no, time DESC);
CREATE INDEX idx_order_book_spread ON order_book (symbol, spread_percent, time DESC) WHERE level_no = 1;

-- Market Statistics indexleri
CREATE INDEX idx_market_statistics_symbol ON market_statistics (symbol, date DESC);
CREATE INDEX idx_market_statistics_sector ON market_statistics (sector, date DESC);
CREATE INDEX idx_market_statistics_volume ON market_statistics (volume DESC, date DESC);
CREATE INDEX idx_market_statistics_change ON market_statistics (change_percent DESC, date DESC);

-- Economic Events indexleri
CREATE INDEX idx_economic_events_time ON economic_events (event_time DESC);
CREATE INDEX idx_economic_events_symbols ON economic_events USING GIN (affected_symbols);
CREATE INDEX idx_economic_events_type ON economic_events (event_type, event_time DESC);
CREATE INDEX idx_economic_events_importance ON economic_events (importance, event_time DESC);
CREATE INDEX idx_economic_events_country ON economic_events (country, event_time DESC);

-- Continuous aggregates (materialized views) oluştur
-- 1 dakikalık mumları 5 dakikalık mumlara dönüştür
CREATE MATERIALIZED VIEW market_candles_5m
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('5 minutes', time) AS time,
    symbol,
    exchange,
    '5m' as timeframe,
    (first(open_price, time))::DECIMAL(12,4) as open_price,
    max(high_price)::DECIMAL(12,4) as high_price,
    min(low_price)::DECIMAL(12,4) as low_price,
    (last(close_price, time))::DECIMAL(12,4) as close_price,
    sum(volume)::DECIMAL(15,4) as volume,
    (sum(close_price * volume) / NULLIF(sum(volume), 0))::DECIMAL(12,4) as vwap,
    sum(trade_count)::INTEGER as trade_count,
    sum(turnover)::DECIMAL(19,4) as turnover
FROM market_candles
WHERE timeframe = '1m'
GROUP BY time_bucket('5 minutes', time), symbol, exchange;

-- Refresh policy ayarla
SELECT add_continuous_aggregate_policy('market_candles_5m',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- Günlük hacim liderleri view
CREATE MATERIALIZED VIEW daily_volume_leaders
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', date) AS date,
    symbol,
    sum(volume) as total_volume,
    sum(turnover) as total_turnover,
    avg(change_percent) as avg_change_percent,
    last(close_price, date) as last_price
FROM market_statistics
GROUP BY time_bucket('1 day', date), symbol;

SELECT add_continuous_aggregate_policy('daily_volume_leaders',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 hour');

-- Data retention policy (eski verileri temizle)
-- Market ticks - 30 gün tut
SELECT add_retention_policy('market_ticks', INTERVAL '30 days');

-- Order book - 7 gün tut
SELECT add_retention_policy('order_book', INTERVAL '7 days');

-- Market candles - 2 yıl tut
SELECT add_retention_policy('market_candles', INTERVAL '2 years');

-- Compression policy (veri sıkıştırması)
ALTER TABLE market_ticks SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('market_ticks', INTERVAL '1 day');

ALTER TABLE market_candles SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol, timeframe',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('market_candles', INTERVAL '7 days');

-- Market data fonksiyonları

-- Son fiyat alma fonksiyonu
CREATE OR REPLACE FUNCTION get_last_price(p_symbol VARCHAR(20))
RETURNS DECIMAL(12,4) AS $$
DECLARE
    last_price DECIMAL(12,4);
BEGIN
    SELECT price INTO last_price
    FROM market_ticks
    WHERE symbol = p_symbol AND tick_type = 'TRADE'
    ORDER BY time DESC
    LIMIT 1;

    RETURN COALESCE(last_price, 0);
END;
$$ LANGUAGE plpgsql;

-- OHLCV verisi alma fonksiyonu
CREATE OR REPLACE FUNCTION get_ohlcv(
    p_symbol VARCHAR(20),
    p_timeframe VARCHAR(10),
    p_from_time TIMESTAMPTZ,
    p_to_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
)
RETURNS TABLE (
    time TIMESTAMPTZ,
    open_price DECIMAL(12,4),
    high_price DECIMAL(12,4),
    low_price DECIMAL(12,4),
    close_price DECIMAL(12,4),
    volume DECIMAL(15,4)
) AS $$
BEGIN
    RETURN QUERY
    SELECT mc.time, mc.open_price, mc.high_price, mc.low_price, mc.close_price, mc.volume
    FROM market_candles mc
    WHERE mc.symbol = p_symbol
      AND mc.timeframe = p_timeframe
      AND mc.time >= p_from_time
      AND mc.time <= p_to_time
      AND mc.is_complete = TRUE
    ORDER BY mc.time;
END;
$$ LANGUAGE plpgsql;

-- Volatilite hesaplama fonksiyonu
CREATE OR REPLACE FUNCTION calculate_volatility(
    p_symbol VARCHAR(20),
    p_periods INTEGER DEFAULT 20
)
RETURNS DECIMAL(8,6) AS $$
DECLARE
    volatility DECIMAL(8,6);
BEGIN
    WITH price_changes AS (
        SELECT
            close_price,
            LAG(close_price) OVER (ORDER BY time) as prev_close,
            LN(close_price / LAG(close_price) OVER (ORDER BY time)) as log_return
        FROM market_candles
        WHERE symbol = p_symbol
          AND timeframe = '1d'
          AND is_complete = TRUE
        ORDER BY time DESC
        LIMIT p_periods + 1
    )
    SELECT STDDEV(log_return) * SQRT(252) -- Annualized volatility
    INTO volatility
    FROM price_changes
    WHERE log_return IS NOT NULL;

    RETURN COALESCE(volatility, 0);
END;
$$ LANGUAGE plpgsql;

-- Real-time fiyat güncellemelerini trigger et
CREATE OR REPLACE FUNCTION notify_price_update()
RETURNS TRIGGER AS $$
BEGIN
    -- WebSocket ile real-time fiyat güncellemesi gönder
    PERFORM pg_notify('price_update',
        json_build_object(
            'symbol', NEW.symbol,
            'price', NEW.price,
            'volume', NEW.volume,
            'time', NEW.time,
            'change_percent',
                CASE
                    WHEN OLD.price IS NOT NULL AND OLD.price != 0 THEN
                        ROUND(((NEW.price - OLD.price) / OLD.price * 100)::NUMERIC, 4)
                    ELSE 0
                END
        )::text
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_market_ticks_notify
    AFTER INSERT ON market_ticks
    FOR EACH ROW
    EXECUTE FUNCTION notify_price_update();

-- Updated_at triggers
CREATE TRIGGER trigger_market_statistics_updated_at
    BEFORE UPDATE ON market_statistics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_economic_events_updated_at
    BEFORE UPDATE ON economic_events
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- En aktif hisseler view (gerçek zamanlı)
CREATE VIEW most_active_stocks AS
SELECT
    ms.symbol,
    ms.close_price,
    ms.change_percent,
    ms.volume,
    ms.turnover,
    ms.trade_count,
    -- Son 1 saatteki işlem hacmi
    (
        SELECT COALESCE(SUM(volume), 0)
        FROM market_ticks mt
        WHERE mt.symbol = ms.symbol
          AND mt.time > CURRENT_TIMESTAMP - INTERVAL '1 hour'
    ) as hourly_volume,
    -- Günlük volatilite
    calculate_volatility(ms.symbol, 20) as volatility_20d
FROM market_statistics ms
WHERE ms.date = CURRENT_DATE
  AND ms.volume > 0
ORDER BY ms.volume DESC, ms.turnover DESC
LIMIT 50;

-- Top gainers/losers view
CREATE VIEW top_movers AS
SELECT
    symbol,
    close_price,
    change_percent,
    volume,
    turnover,
    CASE
        WHEN change_percent > 0 THEN 'GAINER'
        WHEN change_percent < 0 THEN 'LOSER'
        ELSE 'UNCHANGED'
    END as movement_type,
    ABS(change_percent) as abs_change_percent
FROM market_statistics
WHERE date = CURRENT_DATE
  AND volume > 10000 -- Minimum likidite filtresi
ORDER BY ABS(change_percent) DESC
LIMIT 100;

-- Yorumlar
COMMENT ON TABLE market_ticks IS 'Gerçek zamanlı piyasa tick verileri - TimescaleDB hypertable';
COMMENT ON TABLE market_candles IS 'OHLCV mum verileri - çoklu timeframe desteği';
COMMENT ON TABLE order_book IS 'Emir defteri derinlik verileri - 20 seviye';
COMMENT ON TABLE market_statistics IS 'Günlük piyasa istatistikleri ve özet bilgiler';
COMMENT ON TABLE economic_events IS 'Ekonomik olaylar ve kurumsal haberler';
COMMENT ON VIEW most_active_stocks IS 'En aktif işlem gören hisseler (gerçek zamanlı)';
COMMENT ON VIEW top_movers IS 'Günün en çok yükselen/düşen hisseleri';