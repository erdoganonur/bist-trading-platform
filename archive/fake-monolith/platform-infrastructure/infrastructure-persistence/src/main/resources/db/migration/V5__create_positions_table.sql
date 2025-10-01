-- Pozisyonlar tablosu - Gerçek zamanlı P&L takibi
-- Bu tablo açık pozisyonları ve kar/zarar hesaplamalarını saklar

CREATE TABLE positions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    broker_account_id VARCHAR(36) NOT NULL,
    organization_id VARCHAR(36),

    -- Pozisyon tanımlayıcıları
    position_id VARCHAR(50) NOT NULL, -- Broker pozisyon ID
    symbol VARCHAR(20) NOT NULL,
    isin_code VARCHAR(12),
    market VARCHAR(20) NOT NULL DEFAULT 'BIST',

    -- Pozisyon detayları
    position_side VARCHAR(5) NOT NULL CHECK (position_side IN ('LONG', 'SHORT')),
    quantity DECIMAL(15,4) NOT NULL CHECK (quantity != 0),
    available_quantity DECIMAL(15,4) NOT NULL DEFAULT 0, -- Satılabilir miktar

    -- Maliyet bilgileri
    average_cost DECIMAL(12,4) NOT NULL, -- Ortalama maliyet fiyatı
    total_cost DECIMAL(19,4) NOT NULL, -- Toplam maliyet (komisyon dahil)
    commission_paid DECIMAL(12,4) NOT NULL DEFAULT 0,

    -- Güncel piyasa değerleri
    current_price DECIMAL(12,4), -- Güncel piyasa fiyatı
    last_price_update TIMESTAMP, -- Son fiyat güncellemesi
    market_value DECIMAL(19,4), -- Güncel piyasa değeri

    -- Kar/Zarar hesaplamaları
    unrealized_pnl DECIMAL(19,4) DEFAULT 0, -- Gerçekleşmemiş K/Z
    unrealized_pnl_percent DECIMAL(8,4) DEFAULT 0, -- Gerçekleşmemiş K/Z yüzdesi
    realized_pnl DECIMAL(19,4) DEFAULT 0, -- Gerçekleşmiş K/Z
    total_pnl DECIMAL(19,4) DEFAULT 0, -- Toplam K/Z

    -- Günlük P&L
    daily_pnl DECIMAL(19,4) DEFAULT 0,
    daily_pnl_percent DECIMAL(8,4) DEFAULT 0,
    previous_close_price DECIMAL(12,4),

    -- Risk metrikleri
    var_1day DECIMAL(19,4), -- 1 günlük Value at Risk
    var_5day DECIMAL(19,4), -- 5 günlük Value at Risk
    beta DECIMAL(8,6), -- Beta katsayısı (piyasaya göre)
    volatility DECIMAL(8,6), -- 30 günlük oynaklık

    -- Margin bilgileri (kaldıraçlı işlemler için)
    margin_required DECIMAL(19,4),
    margin_available DECIMAL(19,4),
    margin_call_level DECIMAL(8,4), -- Margin call seviyesi
    liquidation_price DECIMAL(12,4), -- Tasfiye fiyatı

    -- Pozisyon açılış/kapanış
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    position_status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (
        position_status IN ('OPEN', 'CLOSING', 'CLOSED', 'LIQUIDATED')
    ),

    -- Stop loss ve take profit seviyeleri
    stop_loss_price DECIMAL(12,4),
    take_profit_price DECIMAL(12,4),
    trailing_stop_distance DECIMAL(12,4),

    -- İstatistikler
    max_quantity DECIMAL(15,4), -- Maksimum ulaşılan miktar
    min_quantity DECIMAL(15,4), -- Minimum miktar
    max_profit DECIMAL(19,4), -- Maksimum kar
    max_loss DECIMAL(19,4), -- Maksimum zarar
    days_held INTEGER DEFAULT 0, -- Pozisyon tutma günü

    -- Strateji ve algoritma bilgileri
    strategy_id VARCHAR(36),
    algorithm_name VARCHAR(100),
    entry_signal VARCHAR(50), -- Giriş sinyali
    exit_signal VARCHAR(50), -- Çıkış sinyali

    -- Pozisyon kaynağı ve tipi
    position_source VARCHAR(20) DEFAULT 'MANUAL' CHECK (
        position_source IN ('MANUAL', 'ALGORITHM', 'COPY_TRADING', 'BASKET', 'SYSTEMATIC')
    ),
    position_type VARCHAR(20) DEFAULT 'NORMAL' CHECK (
        position_type IN ('NORMAL', 'HEDGE', 'ARBITRAGE', 'COVERED_CALL', 'PROTECTIVE_PUT')
    ),

    -- Metadata ve audit
    metadata JSONB,
    price_alerts JSONB, -- Fiyat alarm seviyeleri
    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Foreign key kısıtlamaları
ALTER TABLE positions ADD CONSTRAINT fk_positions_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE positions ADD CONSTRAINT fk_positions_broker_account
    FOREIGN KEY (broker_account_id) REFERENCES broker_accounts(id) ON DELETE CASCADE;

ALTER TABLE positions ADD CONSTRAINT fk_positions_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;

-- Performans indexleri
CREATE INDEX idx_positions_user_id ON positions(user_id);
CREATE INDEX idx_positions_broker_account_id ON positions(broker_account_id);
CREATE INDEX idx_positions_organization_id ON positions(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_positions_position_id ON positions(position_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);
CREATE INDEX idx_positions_position_side ON positions(position_side);
CREATE INDEX idx_positions_position_status ON positions(position_status);
CREATE INDEX idx_positions_opened_at ON positions(opened_at);
CREATE INDEX idx_positions_closed_at ON positions(closed_at) WHERE closed_at IS NOT NULL;
CREATE INDEX idx_positions_strategy_id ON positions(strategy_id) WHERE strategy_id IS NOT NULL;
CREATE INDEX idx_positions_last_price_update ON positions(last_price_update) WHERE last_price_update IS NOT NULL;

-- Bileşik indexler
CREATE INDEX idx_positions_user_symbol ON positions(user_id, symbol, position_status);
CREATE INDEX idx_positions_user_status ON positions(user_id, position_status);
CREATE INDEX idx_positions_broker_symbol ON positions(broker_account_id, symbol);
CREATE INDEX idx_positions_symbol_status ON positions(symbol, position_status);

-- Açık pozisyonlar için özel indexler
CREATE INDEX idx_positions_open ON positions(position_status, symbol) WHERE position_status = 'OPEN';
CREATE INDEX idx_positions_user_open ON positions(user_id, position_status) WHERE position_status = 'OPEN';

-- Risk metrikleri için indexler
CREATE INDEX idx_positions_high_risk ON positions(position_status, unrealized_pnl_percent)
    WHERE position_status = 'OPEN' AND ABS(unrealized_pnl_percent) > 10;

-- Unique kısıtlamaları
CREATE UNIQUE INDEX idx_positions_broker_unique
    ON positions(broker_account_id, position_id, symbol)
    WHERE position_status = 'OPEN' AND deleted_at IS NULL;

-- P&L hesaplama fonksiyonu
CREATE OR REPLACE FUNCTION calculate_position_pnl(
    p_position_id VARCHAR(36),
    p_current_price DECIMAL(12,4)
)
RETURNS VOID AS $$
DECLARE
    pos_record RECORD;
    new_market_value DECIMAL(19,4);
    new_unrealized_pnl DECIMAL(19,4);
    new_unrealized_pnl_percent DECIMAL(8,4);
    new_daily_pnl DECIMAL(19,4);
    new_daily_pnl_percent DECIMAL(8,4);
BEGIN
    -- Pozisyon bilgilerini al
    SELECT * INTO pos_record FROM positions WHERE id = p_position_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Position not found: %', p_position_id;
    END IF;

    -- Piyasa değerini hesapla
    new_market_value := pos_record.quantity * p_current_price;

    -- Gerçekleşmemiş K/Z hesapla
    IF pos_record.position_side = 'LONG' THEN
        new_unrealized_pnl := new_market_value - pos_record.total_cost;
    ELSE -- SHORT
        new_unrealized_pnl := pos_record.total_cost - new_market_value;
    END IF;

    -- Gerçekleşmemiş K/Z yüzdesini hesapla
    IF pos_record.total_cost != 0 THEN
        new_unrealized_pnl_percent := (new_unrealized_pnl / ABS(pos_record.total_cost)) * 100;
    ELSE
        new_unrealized_pnl_percent := 0;
    END IF;

    -- Günlük P&L hesapla (eğer önceki kapanış fiyatı varsa)
    IF pos_record.previous_close_price IS NOT NULL THEN
        IF pos_record.position_side = 'LONG' THEN
            new_daily_pnl := pos_record.quantity * (p_current_price - pos_record.previous_close_price);
        ELSE -- SHORT
            new_daily_pnl := pos_record.quantity * (pos_record.previous_close_price - p_current_price);
        END IF;

        IF pos_record.previous_close_price != 0 THEN
            new_daily_pnl_percent := ((p_current_price - pos_record.previous_close_price) / pos_record.previous_close_price) * 100;
            IF pos_record.position_side = 'SHORT' THEN
                new_daily_pnl_percent := -new_daily_pnl_percent;
            END IF;
        END IF;
    ELSE
        new_daily_pnl := 0;
        new_daily_pnl_percent := 0;
    END IF;

    -- Pozisyonu güncelle
    UPDATE positions SET
        current_price = p_current_price,
        last_price_update = CURRENT_TIMESTAMP,
        market_value = new_market_value,
        unrealized_pnl = new_unrealized_pnl,
        unrealized_pnl_percent = new_unrealized_pnl_percent,
        total_pnl = realized_pnl + new_unrealized_pnl,
        daily_pnl = new_daily_pnl,
        daily_pnl_percent = new_daily_pnl_percent,
        updated_at = CURRENT_TIMESTAMP,

        -- Min/max takibi
        max_profit = GREATEST(COALESCE(max_profit, 0), new_unrealized_pnl),
        max_loss = LEAST(COALESCE(max_loss, 0), new_unrealized_pnl),

        -- Günleri güncelle
        days_held = EXTRACT(DAY FROM (CURRENT_TIMESTAMP - opened_at))

    WHERE id = p_position_id;

END;
$$ LANGUAGE plpgsql;

-- Tüm açık pozisyonların P&L'ini güncelleme fonksiyonu
CREATE OR REPLACE FUNCTION update_all_positions_pnl()
RETURNS INTEGER AS $$
DECLARE
    position_count INTEGER := 0;
    pos_record RECORD;
BEGIN
    FOR pos_record IN
        SELECT id, symbol FROM positions WHERE position_status = 'OPEN' AND deleted_at IS NULL
    LOOP
        -- Bu gerçek uygulamada market data servisinden fiyat alınacak
        -- Şimdilik mevcut fiyatı kullan
        PERFORM calculate_position_pnl(pos_record.id,
            (SELECT current_price FROM positions WHERE id = pos_record.id));

        position_count := position_count + 1;
    END LOOP;

    RETURN position_count;
END;
$$ LANGUAGE plpgsql;

-- Pozisyon kapama fonksiyonu
CREATE OR REPLACE FUNCTION close_position(
    p_position_id VARCHAR(36),
    p_close_price DECIMAL(12,4),
    p_close_quantity DECIMAL(15,4),
    p_commission DECIMAL(12,4) DEFAULT 0,
    p_exit_signal VARCHAR(50) DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    pos_record RECORD;
    close_proceeds DECIMAL(19,4);
    realized_pnl DECIMAL(19,4);
BEGIN
    -- Pozisyon bilgilerini al
    SELECT * INTO pos_record FROM positions WHERE id = p_position_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Position not found: %', p_position_id;
    END IF;

    IF pos_record.position_status != 'OPEN' THEN
        RAISE EXCEPTION 'Position is not open: %', p_position_id;
    END IF;

    -- Kapanış hasılatını hesapla
    close_proceeds := p_close_quantity * p_close_price - p_commission;

    -- Gerçekleşen K/Z hesapla
    IF pos_record.position_side = 'LONG' THEN
        realized_pnl := close_proceeds - (pos_record.average_cost * p_close_quantity);
    ELSE -- SHORT
        realized_pnl := (pos_record.average_cost * p_close_quantity) - close_proceeds;
    END IF;

    -- Tam kapanış mı yoksa kısmi kapanış mı?
    IF p_close_quantity >= pos_record.quantity THEN
        -- Tam kapanış
        UPDATE positions SET
            quantity = 0,
            available_quantity = 0,
            position_status = 'CLOSED',
            closed_at = CURRENT_TIMESTAMP,
            realized_pnl = realized_pnl,
            total_pnl = realized_pnl,
            unrealized_pnl = 0,
            exit_signal = COALESCE(p_exit_signal, exit_signal),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = p_position_id;
    ELSE
        -- Kısmi kapanış
        UPDATE positions SET
            quantity = quantity - p_close_quantity,
            available_quantity = GREATEST(0, available_quantity - p_close_quantity),
            realized_pnl = realized_pnl,
            commission_paid = commission_paid + p_commission,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = p_position_id;

        -- P&L'i yeniden hesapla
        PERFORM calculate_position_pnl(p_position_id, p_close_price);
    END IF;

END;
$$ LANGUAGE plpgsql;

-- Günlük kapanış fiyatlarını güncelleme fonksiyonu
CREATE OR REPLACE FUNCTION update_previous_close_prices()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER := 0;
BEGIN
    -- Bu fonksiyon günlük piyasa kapanışında çalıştırılır
    -- Gerçek uygulamada market data servisinden kapanış fiyatları alınır

    UPDATE positions SET
        previous_close_price = current_price,
        daily_pnl = 0,
        daily_pnl_percent = 0,
        updated_at = CURRENT_TIMESTAMP
    WHERE position_status = 'OPEN' AND deleted_at IS NULL;

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Updated_at trigger
CREATE TRIGGER trigger_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Pozisyon risk kontrol fonksiyonu
CREATE OR REPLACE FUNCTION check_position_risk()
RETURNS TRIGGER AS $$
BEGIN
    -- Stop loss kontrolü
    IF NEW.stop_loss_price IS NOT NULL AND NEW.current_price IS NOT NULL THEN
        IF (NEW.position_side = 'LONG' AND NEW.current_price <= NEW.stop_loss_price) OR
           (NEW.position_side = 'SHORT' AND NEW.current_price >= NEW.stop_loss_price) THEN
            -- Stop loss tetiklendi - bu normalde trading engine tarafından işlenir
            NEW.position_status = 'CLOSING';
        END IF;
    END IF;

    -- Take profit kontrolü
    IF NEW.take_profit_price IS NOT NULL AND NEW.current_price IS NOT NULL THEN
        IF (NEW.position_side = 'LONG' AND NEW.current_price >= NEW.take_profit_price) OR
           (NEW.position_side = 'SHORT' AND NEW.current_price <= NEW.take_profit_price) THEN
            -- Take profit tetiklendi
            NEW.position_status = 'CLOSING';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_positions_risk_check
    BEFORE UPDATE OF current_price ON positions
    FOR EACH ROW
    EXECUTE FUNCTION check_position_risk();

-- Portföy özeti view
CREATE VIEW portfolio_summary AS
SELECT
    user_id,
    broker_account_id,
    COUNT(*) as total_positions,
    COUNT(*) FILTER (WHERE position_status = 'OPEN') as open_positions,
    COUNT(*) FILTER (WHERE position_side = 'LONG') as long_positions,
    COUNT(*) FILTER (WHERE position_side = 'SHORT') as short_positions,

    -- Değer toplamları
    SUM(market_value) FILTER (WHERE position_status = 'OPEN') as total_market_value,
    SUM(total_cost) FILTER (WHERE position_status = 'OPEN') as total_cost,

    -- P&L toplamları
    SUM(unrealized_pnl) FILTER (WHERE position_status = 'OPEN') as total_unrealized_pnl,
    SUM(realized_pnl) as total_realized_pnl,
    SUM(daily_pnl) FILTER (WHERE position_status = 'OPEN') as total_daily_pnl,

    -- Risk metrikleri
    SUM(var_1day) FILTER (WHERE position_status = 'OPEN') as portfolio_var_1day,
    AVG(beta) FILTER (WHERE position_status = 'OPEN') as avg_beta,

    -- En iyi ve en kötü performans
    MAX(unrealized_pnl_percent) FILTER (WHERE position_status = 'OPEN') as best_performer,
    MIN(unrealized_pnl_percent) FILTER (WHERE position_status = 'OPEN') as worst_performer

FROM positions
WHERE deleted_at IS NULL
GROUP BY user_id, broker_account_id;

-- Risk uyarıları view
CREATE VIEW position_risk_alerts AS
SELECT
    p.*,
    u.first_name || ' ' || u.last_name as user_name,
    u.email as user_email,

    CASE
        WHEN p.unrealized_pnl_percent < -20 THEN 'CRITICAL_LOSS'
        WHEN p.unrealized_pnl_percent < -10 THEN 'HIGH_LOSS'
        WHEN p.unrealized_pnl_percent > 50 THEN 'HIGH_PROFIT_LOCK'
        WHEN p.margin_call_level IS NOT NULL AND p.margin_available < p.margin_required * 1.2 THEN 'MARGIN_WARNING'
        WHEN EXTRACT(DAY FROM (CURRENT_TIMESTAMP - p.opened_at)) > 365 THEN 'LONG_HELD'
        ELSE 'NORMAL'
    END as alert_type,

    ABS(p.unrealized_pnl_percent) as risk_magnitude

FROM positions p
JOIN users u ON p.user_id = u.id
WHERE p.position_status = 'OPEN'
  AND p.deleted_at IS NULL
  AND (p.unrealized_pnl_percent < -10 OR
       p.unrealized_pnl_percent > 50 OR
       (p.margin_call_level IS NOT NULL AND p.margin_available < p.margin_required * 1.2) OR
       EXTRACT(DAY FROM (CURRENT_TIMESTAMP - p.opened_at)) > 365);

-- Yorumlar
COMMENT ON TABLE positions IS 'Açık pozisyonlar ve gerçek zamanlı P&L takibi';
COMMENT ON COLUMN positions.position_id IS 'Broker sistemindeki pozisyon ID';
COMMENT ON COLUMN positions.unrealized_pnl IS 'Gerçekleşmemiş kar/zarar (TRY)';
COMMENT ON COLUMN positions.unrealized_pnl_percent IS 'Gerçekleşmemiş kar/zarar yüzdesi';
COMMENT ON COLUMN positions.daily_pnl IS 'Günlük kar/zarar değişimi';
COMMENT ON COLUMN positions.var_1day IS '1 günlük Value at Risk';
COMMENT ON COLUMN positions.margin_call_level IS 'Margin call tetiklenme seviyesi';
COMMENT ON COLUMN positions.liquidation_price IS 'Otomatik tasfiye fiyatı';
COMMENT ON VIEW portfolio_summary IS 'Kullanıcı portföy özet bilgileri';
COMMENT ON VIEW position_risk_alerts IS 'Risk uyarıları gerektiren pozisyonlar';