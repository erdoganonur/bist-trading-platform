-- Emirler tablosu - Aylık bölümleme ve durum takibi
-- Bu tablo trading emirlerini saklar ve performans için aylık bölümlere ayrılır

-- Ana orders tablosu (partitioned by month)
CREATE TABLE orders (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    broker_account_id VARCHAR(36) NOT NULL,
    organization_id VARCHAR(36),

    -- Emir detayları
    order_number VARCHAR(50) NOT NULL, -- Broker'dan gelen emir numarası
    client_order_id VARCHAR(50), -- Müşteri emir ID
    parent_order_id VARCHAR(36), -- Ana emir (iceberg, bracket orders için)

    -- Enstrüman bilgileri
    symbol VARCHAR(20) NOT NULL, -- GARAN, AKBNK vb.
    isin_code VARCHAR(12), -- ISIN kodu
    market VARCHAR(20) NOT NULL DEFAULT 'BIST', -- BIST, VIOP vb.
    market_segment VARCHAR(20), -- Ana, Gelişen, Kolektif vb.

    -- Emir tipi ve yönü
    order_side VARCHAR(5) NOT NULL CHECK (order_side IN ('BUY', 'SELL')),
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT', 'ICEBERG', 'BRACKET')),
    time_in_force VARCHAR(10) DEFAULT 'DAY' CHECK (time_in_force IN ('DAY', 'GTC', 'IOC', 'FOK')),

    -- Miktar ve fiyat bilgileri
    quantity DECIMAL(15,4) NOT NULL CHECK (quantity > 0),
    remaining_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    filled_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 CHECK (filled_quantity >= 0),

    price DECIMAL(12,4), -- Limit fiyatı (market emirler için NULL)
    stop_price DECIMAL(12,4), -- Stop fiyatı
    average_fill_price DECIMAL(12,4), -- Ortalama gerçekleşme fiyatı

    -- Para birimi ve değer hesaplamaları
    currency VARCHAR(3) DEFAULT 'TRY',
    order_value DECIMAL(19,4), -- Toplam emir değeri
    filled_value DECIMAL(19,4) DEFAULT 0, -- Gerçekleşen değer
    commission DECIMAL(12,4) DEFAULT 0, -- Komisyon
    commission_currency VARCHAR(3) DEFAULT 'TRY',

    -- Emir durumu ve zamanlaması
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        order_status IN ('PENDING', 'ACKNOWLEDGED', 'WORKING', 'PARTIALLY_FILLED',
                        'FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'SUSPENDED')
    ),

    -- Zaman damgaları
    order_date DATE NOT NULL, -- Partition key
    order_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    first_fill_at TIMESTAMP,
    last_fill_at TIMESTAMP,
    completed_at TIMESTAMP, -- Tamamlanma (filled/cancelled/rejected)
    expires_at TIMESTAMP,

    -- Durum değişiklik geçmişi
    status_history JSONB DEFAULT '[]'::jsonb,
    last_status_change TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Risk ve validasyon
    pre_trade_risk_check BOOLEAN DEFAULT FALSE,
    risk_check_details JSONB,
    margin_required DECIMAL(19,4),

    -- Algoritma ve strateji bilgileri
    strategy_id VARCHAR(36), -- Strateji referansı
    algorithm_name VARCHAR(100), -- Algoritma adı
    execution_algorithm VARCHAR(50), -- TWAP, VWAP vb.
    algo_params JSONB, -- Algoritma parametreleri

    -- Emir kaynağı
    order_source VARCHAR(20) DEFAULT 'WEB' CHECK (
        order_source IN ('WEB', 'MOBILE', 'API', 'FIX', 'ALGORITHM', 'BASKET', 'COPY_TRADING')
    ),
    trading_session VARCHAR(20) DEFAULT 'REGULAR' CHECK (
        trading_session IN ('PRE_MARKET', 'REGULAR', 'POST_MARKET', 'EXTENDED')
    ),

    -- Özel emir tipleri için parametreler
    iceberg_visible_qty DECIMAL(15,4), -- Iceberg görünür miktar
    bracket_params JSONB, -- Bracket order parametreleri

    -- Hata ve red nedenleri
    rejection_reason VARCHAR(500),
    error_code VARCHAR(20),
    error_message VARCHAR(1000),

    -- Audit ve metadata
    ip_address INET, -- Emir veren IP adresi
    user_agent TEXT, -- Browser/app bilgisi
    metadata JSONB, -- Ek veriler
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Primary key constraint
    CONSTRAINT pk_orders PRIMARY KEY (id, order_date)
) PARTITION BY RANGE (order_date);

-- Foreign key kısıtlamaları
-- Not: Partitioned tablolarda foreign key kısıtlamaları sınırlıdır
-- Bu kısıtlamalar application level'da enforce edilmelidir
CREATE INDEX idx_orders_user_id ON orders (user_id, order_date);
CREATE INDEX idx_orders_broker_account_id ON orders (broker_account_id, order_date);
CREATE INDEX idx_orders_organization_id ON orders (organization_id, order_date) WHERE organization_id IS NOT NULL;

-- Performans indexleri
CREATE INDEX idx_orders_symbol ON orders (symbol, order_date);
CREATE INDEX idx_orders_order_number ON orders (order_number, order_date);
CREATE INDEX idx_orders_client_order_id ON orders (client_order_id, order_date) WHERE client_order_id IS NOT NULL;
CREATE INDEX idx_orders_parent_order_id ON orders (parent_order_id, order_date) WHERE parent_order_id IS NOT NULL;
CREATE INDEX idx_orders_order_status ON orders (order_status, order_date);
CREATE INDEX idx_orders_order_side ON orders (order_side, order_date);
CREATE INDEX idx_orders_order_type ON orders (order_type, order_date);
CREATE INDEX idx_orders_order_time ON orders (order_time, order_date);
CREATE INDEX idx_orders_strategy_id ON orders (strategy_id, order_date) WHERE strategy_id IS NOT NULL;

-- Bileşik indexler
CREATE INDEX idx_orders_user_symbol_date ON orders (user_id, symbol, order_date);
CREATE INDEX idx_orders_user_status_date ON orders (user_id, order_status, order_date);
CREATE INDEX idx_orders_symbol_status_date ON orders (symbol, order_status, order_date);
CREATE INDEX idx_orders_broker_status_date ON orders (broker_account_id, order_status, order_date);

-- Aktif emirler için özel index
CREATE INDEX idx_orders_active ON orders (order_status, order_date)
    WHERE order_status IN ('PENDING', 'ACKNOWLEDGED', 'WORKING', 'PARTIALLY_FILLED');

-- Bugünün emirleri için index
CREATE INDEX idx_orders_today ON orders (user_id, order_time)
    WHERE order_date = CURRENT_DATE;

-- Aylık partition'lar oluşturmak için fonksiyon
CREATE OR REPLACE FUNCTION create_monthly_partition(
    table_name TEXT,
    start_date DATE
)
RETURNS TEXT AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';

    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %I
                    FOR VALUES FROM (%L) TO (%L)',
                   partition_name, table_name, start_date, end_date);

    RETURN partition_name;
END;
$$ LANGUAGE plpgsql;

-- Şu anki ay ve gelecek 12 ay için partition'lar oluştur
DO $$
DECLARE
    i INTEGER;
    partition_date DATE;
BEGIN
    FOR i IN 0..12 LOOP
        partition_date := date_trunc('month', CURRENT_DATE) + (i || ' months')::INTERVAL;
        PERFORM create_monthly_partition('orders', partition_date);
    END LOOP;
END $$;

-- Geçmiş 6 ay için partition'lar oluştur
DO $$
DECLARE
    i INTEGER;
    partition_date DATE;
BEGIN
    FOR i IN 1..6 LOOP
        partition_date := date_trunc('month', CURRENT_DATE) - (i || ' months')::INTERVAL;
        PERFORM create_monthly_partition('orders', partition_date);
    END LOOP;
END $$;

-- Emir durumu güncelleme fonksiyonu
CREATE OR REPLACE FUNCTION update_order_status(
    p_order_id VARCHAR(36),
    p_order_date DATE,
    p_new_status VARCHAR(20),
    p_filled_quantity DECIMAL(15,4) DEFAULT NULL,
    p_average_price DECIMAL(12,4) DEFAULT NULL,
    p_rejection_reason VARCHAR(500) DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    current_status VARCHAR(20);
    status_entry JSONB;
BEGIN
    -- Mevcut durumu al
    SELECT order_status INTO current_status
    FROM orders
    WHERE id = p_order_id AND order_date = p_order_date;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found: %', p_order_id;
    END IF;

    -- Durum geçmişi girişi oluştur
    status_entry := jsonb_build_object(
        'from_status', current_status,
        'to_status', p_new_status,
        'timestamp', CURRENT_TIMESTAMP,
        'reason', COALESCE(p_rejection_reason, '')
    );

    -- Emri güncelle
    UPDATE orders SET
        order_status = p_new_status,
        filled_quantity = COALESCE(p_filled_quantity, filled_quantity),
        remaining_quantity = quantity - COALESCE(p_filled_quantity, filled_quantity),
        average_fill_price = COALESCE(p_average_price, average_fill_price),
        rejection_reason = COALESCE(p_rejection_reason, rejection_reason),
        status_history = status_history || status_entry,
        last_status_change = CURRENT_TIMESTAMP,

        -- Özel timestamp güncellemeleri
        acknowledged_at = CASE WHEN p_new_status = 'ACKNOWLEDGED' THEN CURRENT_TIMESTAMP ELSE acknowledged_at END,
        first_fill_at = CASE
            WHEN p_new_status IN ('PARTIALLY_FILLED', 'FILLED') AND first_fill_at IS NULL
            THEN CURRENT_TIMESTAMP
            ELSE first_fill_at
        END,
        last_fill_at = CASE
            WHEN p_new_status IN ('PARTIALLY_FILLED', 'FILLED')
            THEN CURRENT_TIMESTAMP
            ELSE last_fill_at
        END,
        completed_at = CASE
            WHEN p_new_status IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')
            THEN CURRENT_TIMESTAMP
            ELSE completed_at
        END,

        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_order_id AND order_date = p_order_date;
END;
$$ LANGUAGE plpgsql;

-- Updated_at trigger function
CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Emir validasyon fonksiyonu
CREATE OR REPLACE FUNCTION validate_order_data()
RETURNS TRIGGER AS $$
BEGIN
    -- Order date kontrolü
    IF NEW.order_date != date(NEW.order_time) THEN
        RAISE EXCEPTION 'Order date must match order time date';
    END IF;

    -- Remaining quantity kontrolü
    IF NEW.filled_quantity > NEW.quantity THEN
        RAISE EXCEPTION 'Filled quantity cannot exceed total quantity';
    END IF;

    NEW.remaining_quantity = NEW.quantity - NEW.filled_quantity;

    -- Stop price kontrolü
    IF NEW.order_type IN ('STOP', 'STOP_LIMIT') AND NEW.stop_price IS NULL THEN
        RAISE EXCEPTION 'Stop price required for stop orders';
    END IF;

    -- Limit price kontrolü
    IF NEW.order_type IN ('LIMIT', 'STOP_LIMIT') AND NEW.price IS NULL THEN
        RAISE EXCEPTION 'Price required for limit orders';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_orders_validation
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION validate_order_data();

-- Günlük emir özeti view
CREATE VIEW daily_orders_summary AS
SELECT
    order_date,
    user_id,
    broker_account_id,
    symbol,
    order_side,
    COUNT(*) as total_orders,
    COUNT(*) FILTER (WHERE order_status = 'FILLED') as filled_orders,
    COUNT(*) FILTER (WHERE order_status = 'CANCELLED') as cancelled_orders,
    COUNT(*) FILTER (WHERE order_status = 'REJECTED') as rejected_orders,
    SUM(quantity) as total_quantity,
    SUM(filled_quantity) as total_filled_quantity,
    SUM(order_value) as total_order_value,
    SUM(filled_value) as total_filled_value,
    SUM(commission) as total_commission,
    AVG(average_fill_price) as avg_fill_price,
    MIN(order_time) as first_order_time,
    MAX(order_time) as last_order_time
FROM orders
WHERE deleted_at IS NULL
GROUP BY order_date, user_id, broker_account_id, symbol, order_side;

-- Aktif emirler view
CREATE VIEW active_orders_view AS
SELECT
    o.*,
    ba.broker_name,
    ba.account_number as broker_account_number,
    u.first_name || ' ' || u.last_name as user_full_name,

    -- Emir yaşı
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - o.order_time))/60 as order_age_minutes,

    -- Doluluk oranı
    CASE
        WHEN o.quantity > 0 THEN ROUND((o.filled_quantity / o.quantity * 100), 2)
        ELSE 0
    END as fill_percentage,

    -- Risk skoru
    CASE
        WHEN o.order_value > 100000 THEN 'HIGH'
        WHEN o.order_value > 10000 THEN 'MEDIUM'
        ELSE 'LOW'
    END as risk_level

FROM orders o
JOIN broker_accounts ba ON o.broker_account_id = ba.id
JOIN users u ON o.user_id = u.id
WHERE o.order_status IN ('PENDING', 'ACKNOWLEDGED', 'WORKING', 'PARTIALLY_FILLED')
  AND o.deleted_at IS NULL
  AND o.order_date >= CURRENT_DATE - INTERVAL '7 days'; -- Son 7 günün aktif emirleri

-- Yorumlar
COMMENT ON TABLE orders IS 'Trading emirleri - aylık bölümleme ile yüksek performans';
COMMENT ON COLUMN orders.order_date IS 'Partition key - emirlerin tarihi';
COMMENT ON COLUMN orders.order_number IS 'Broker sisteminden gelen benzersiz emir numarası';
COMMENT ON COLUMN orders.status_history IS 'Emir durum değişikliklerinin JSON geçmişi';
COMMENT ON COLUMN orders.pre_trade_risk_check IS 'Emir öncesi risk kontrolü yapıldı mı';
COMMENT ON COLUMN orders.iceberg_visible_qty IS 'Iceberg emirlerde görünür miktar';
COMMENT ON COLUMN orders.bracket_params IS 'Bracket order parametreleri (stop-loss, take-profit)';
COMMENT ON VIEW daily_orders_summary IS 'Günlük emir özet istatistikleri';
COMMENT ON VIEW active_orders_view IS 'Aktif emirlerin detaylı görünümü';