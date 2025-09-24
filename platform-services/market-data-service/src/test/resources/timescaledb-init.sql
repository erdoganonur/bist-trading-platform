-- TimescaleDB Extension kurulumu ve test tabloları
-- Bu script TestContainers tarafından PostgreSQL container'ında çalıştırılır

-- TimescaleDB extension'ını etkinleştir (eğer mevcut değilse atlama)
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Market ticks tablosu oluştur
CREATE TABLE IF NOT EXISTS market_ticks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    volume BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    bid_price DECIMAL(19,4),
    ask_price DECIMAL(19,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TimescaleDB hypertable'a dönüştür (timestamp sütunu üzerinden)
-- Eğer TimescaleDB extension mevcutsa hypertable oluştur
DO $$
BEGIN
    -- TimescaleDB fonksiyonu mevcut mu kontrol et
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
        -- Hypertable oluştur
        PERFORM create_hypertable('market_ticks', 'timestamp', if_not_exists => TRUE);

        -- Compression policy ekle (1 günden eski veriler için)
        PERFORM add_compression_policy('market_ticks', INTERVAL '1 day', if_not_exists => TRUE);

        -- Retention policy ekle (30 günden eski verileri sil)
        PERFORM add_retention_policy('market_ticks', INTERVAL '30 days', if_not_exists => TRUE);
    END IF;
END $$;

-- Performans için indexler
CREATE INDEX IF NOT EXISTS idx_market_ticks_symbol_timestamp ON market_ticks(symbol, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_market_ticks_timestamp ON market_ticks(timestamp DESC);

-- Test için örnek fonksiyon - market data istatistikleri
CREATE OR REPLACE FUNCTION get_market_stats(p_symbol VARCHAR(10), p_hours INTEGER DEFAULT 24)
RETURNS TABLE(
    symbol VARCHAR(10),
    avg_price DECIMAL(19,4),
    min_price DECIMAL(19,4),
    max_price DECIMAL(19,4),
    total_volume BIGINT,
    tick_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        mt.symbol,
        AVG(mt.price)::DECIMAL(19,4) as avg_price,
        MIN(mt.price)::DECIMAL(19,4) as min_price,
        MAX(mt.price)::DECIMAL(19,4) as max_price,
        SUM(mt.volume) as total_volume,
        COUNT(*) as tick_count
    FROM market_ticks mt
    WHERE mt.symbol = p_symbol
      AND mt.timestamp >= NOW() - INTERVAL '1 hour' * p_hours
    GROUP BY mt.symbol;
END;
$$ LANGUAGE plpgsql;

-- Test verisi için temizlik fonksiyonu
CREATE OR REPLACE FUNCTION cleanup_test_data()
RETURNS void AS $$
BEGIN
    DELETE FROM market_ticks WHERE symbol LIKE 'TEST%' OR symbol LIKE 'BATCH%' OR symbol LIKE 'THREAD%';
END;
$$ LANGUAGE plpgsql;