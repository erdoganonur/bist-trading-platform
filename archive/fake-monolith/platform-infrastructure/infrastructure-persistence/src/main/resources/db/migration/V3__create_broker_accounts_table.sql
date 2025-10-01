-- Broker hesapları tablosu - Şifreli kimlik bilgileri ve alt hesap desteği
-- Bu tablo broker hesap bilgilerini güvenli şekilde saklar

CREATE TABLE broker_accounts (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    organization_id VARCHAR(36),
    broker_name VARCHAR(100) NOT NULL,
    broker_code VARCHAR(20) NOT NULL,

    -- Hesap tipi ve durumu
    account_type VARCHAR(20) NOT NULL DEFAULT 'MAIN' CHECK (account_type IN ('MAIN', 'SUB_ACCOUNT', 'DEMO', 'PAPER_TRADING')),
    account_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'CLOSED')),

    -- Ana hesap referansı (alt hesaplar için)
    parent_account_id VARCHAR(36),
    sub_account_number VARCHAR(50),
    sub_account_name VARCHAR(255),

    -- Şifreli kimlik bilgileri (AES-256-GCM ile şifrelenir)
    account_number VARCHAR(500) NOT NULL, -- Şifreli
    customer_number VARCHAR(500), -- Şifreli
    username VARCHAR(500), -- Şifreli
    password_hash VARCHAR(500), -- Şifreli - broker şifresi hash'i
    api_key VARCHAR(500), -- Şifreli
    api_secret VARCHAR(500), -- Şifreli
    certificate_data TEXT, -- Şifreli - dijital sertifika bilgileri

    -- Bağlantı bilgileri
    trading_endpoint VARCHAR(500), -- Şifreli
    market_data_endpoint VARCHAR(500), -- Şifreli
    websocket_endpoint VARCHAR(500), -- Şifreli
    fix_session_config JSONB, -- FIX protokol ayarları

    -- Hesap özellikleri
    currency VARCHAR(3) DEFAULT 'TRY',
    account_balance DECIMAL(19,4) DEFAULT 0,
    available_balance DECIMAL(19,4) DEFAULT 0,
    blocked_amount DECIMAL(19,4) DEFAULT 0,
    margin_level DECIMAL(5,4), -- Kaldıraç oranı

    -- Risk yönetimi
    max_daily_loss DECIMAL(19,4),
    max_position_size DECIMAL(19,4),
    allowed_instruments TEXT[], -- İzin verilen enstrümanlar listesi
    trading_permissions JSONB, -- Trading izinleri

    -- Komisyon bilgileri
    commission_rate DECIMAL(8,6), -- Komisyon oranı
    minimum_commission DECIMAL(8,4), -- Minimum komisyon
    commission_currency VARCHAR(3) DEFAULT 'TRY',
    commission_settings JSONB, -- Detaylı komisyon ayarları

    -- Bağlantı durumu
    connection_status VARCHAR(20) DEFAULT 'DISCONNECTED' CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'CONNECTING', 'ERROR', 'MAINTENANCE')),
    last_connection_at TIMESTAMP,
    last_disconnection_at TIMESTAMP,
    connection_error_count INTEGER DEFAULT 0,
    last_error_message VARCHAR(1000),

    -- API limitleri
    api_rate_limit INTEGER DEFAULT 100, -- Dakikada istek sayısı
    current_api_calls INTEGER DEFAULT 0,
    api_calls_reset_at TIMESTAMP,

    -- Doğrulama ve onay
    verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED')),
    verified_at TIMESTAMP,
    verification_expires_at TIMESTAMP,

    -- Broker spesifik ayarlar
    broker_settings JSONB, -- Broker'a özgü konfigürasyon
    trading_hours JSONB, -- Trading saatleri
    market_holidays JSONB, -- Piyasa tatilleri

    -- Genel alanlar
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE, -- Varsayılan hesap
    notes TEXT,
    metadata JSONB, -- Ek meta veriler
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Foreign key kısıtlamaları
ALTER TABLE broker_accounts ADD CONSTRAINT fk_broker_accounts_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE broker_accounts ADD CONSTRAINT fk_broker_accounts_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;

ALTER TABLE broker_accounts ADD CONSTRAINT fk_broker_accounts_parent
    FOREIGN KEY (parent_account_id) REFERENCES broker_accounts(id) ON DELETE CASCADE;

-- Performans indexleri
CREATE INDEX idx_broker_accounts_user_id ON broker_accounts(user_id);
CREATE INDEX idx_broker_accounts_organization_id ON broker_accounts(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_broker_accounts_broker_code ON broker_accounts(broker_code);
CREATE INDEX idx_broker_accounts_account_type ON broker_accounts(account_type);
CREATE INDEX idx_broker_accounts_account_status ON broker_accounts(account_status);
CREATE INDEX idx_broker_accounts_parent_account ON broker_accounts(parent_account_id) WHERE parent_account_id IS NOT NULL;
CREATE INDEX idx_broker_accounts_connection_status ON broker_accounts(connection_status);
CREATE INDEX idx_broker_accounts_verification_status ON broker_accounts(verification_status);
CREATE INDEX idx_broker_accounts_is_active ON broker_accounts(is_active);
CREATE INDEX idx_broker_accounts_is_default ON broker_accounts(is_default) WHERE is_default = TRUE;
CREATE INDEX idx_broker_accounts_created_at ON broker_accounts(created_at);
CREATE INDEX idx_broker_accounts_deleted_at ON broker_accounts(deleted_at) WHERE deleted_at IS NOT NULL;

-- Bileşik indexler
CREATE INDEX idx_broker_accounts_user_active ON broker_accounts(user_id, is_active, deleted_at)
    WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_broker_accounts_user_default ON broker_accounts(user_id, is_default)
    WHERE is_default = TRUE;
CREATE INDEX idx_broker_accounts_broker_status ON broker_accounts(broker_code, account_status, is_active);
CREATE INDEX idx_broker_accounts_trading_ready ON broker_accounts(account_status, connection_status, verification_status)
    WHERE account_status = 'ACTIVE' AND connection_status = 'CONNECTED' AND verification_status = 'VERIFIED';

-- Kısmi indexler
CREATE INDEX idx_broker_accounts_sub_accounts ON broker_accounts(parent_account_id, account_type)
    WHERE account_type = 'SUB_ACCOUNT';
CREATE INDEX idx_broker_accounts_main_accounts ON broker_accounts(user_id, broker_code)
    WHERE account_type = 'MAIN' AND is_active = TRUE;
CREATE INDEX idx_broker_accounts_expired_verification ON broker_accounts(verification_expires_at)
    WHERE verification_expires_at < CURRENT_TIMESTAMP AND verification_status = 'VERIFIED';

-- Unique kısıtlamaları
CREATE UNIQUE INDEX idx_broker_accounts_user_broker_unique
    ON broker_accounts(user_id, broker_code, account_type)
    WHERE deleted_at IS NULL AND account_type = 'MAIN';

-- Varsayılan hesap benzersizlik kısıtlaması
CREATE UNIQUE INDEX idx_broker_accounts_user_default_unique
    ON broker_accounts(user_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

-- Trigger fonksiyonları
CREATE OR REPLACE FUNCTION validate_broker_account_constraints()
RETURNS TRIGGER AS $$
BEGIN
    -- Alt hesaplar için ana hesap kontrolü
    IF NEW.account_type = 'SUB_ACCOUNT' AND NEW.parent_account_id IS NULL THEN
        RAISE EXCEPTION 'Alt hesap için ana hesap ID gereklidir';
    END IF;

    -- Ana hesap kendi alt hesabı olamaz
    IF NEW.parent_account_id = NEW.id THEN
        RAISE EXCEPTION 'Hesap kendi alt hesabı olamaz';
    END IF;

    -- API çağrı sayacını sıfırla
    IF NEW.api_calls_reset_at IS NULL OR NEW.api_calls_reset_at < CURRENT_TIMESTAMP - INTERVAL '1 minute' THEN
        NEW.current_api_calls = 0;
        NEW.api_calls_reset_at = CURRENT_TIMESTAMP + INTERVAL '1 minute';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_broker_accounts_constraints
    BEFORE INSERT OR UPDATE ON broker_accounts
    FOR EACH ROW
    EXECUTE FUNCTION validate_broker_account_constraints();

-- Updated_at trigger
CREATE TRIGGER trigger_broker_accounts_updated_at
    BEFORE UPDATE ON broker_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Broker hesap bağlantı durumu güncelleme fonksiyonu
CREATE OR REPLACE FUNCTION update_connection_status(
    p_account_id VARCHAR(36),
    p_status VARCHAR(20),
    p_error_message VARCHAR(1000) DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    UPDATE broker_accounts
    SET
        connection_status = p_status,
        last_connection_at = CASE WHEN p_status = 'CONNECTED' THEN CURRENT_TIMESTAMP ELSE last_connection_at END,
        last_disconnection_at = CASE WHEN p_status = 'DISCONNECTED' THEN CURRENT_TIMESTAMP ELSE last_disconnection_at END,
        connection_error_count = CASE
            WHEN p_status = 'ERROR' THEN connection_error_count + 1
            WHEN p_status = 'CONNECTED' THEN 0
            ELSE connection_error_count
        END,
        last_error_message = COALESCE(p_error_message, last_error_message),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_account_id;
END;
$$ LANGUAGE plpgsql;

-- API rate limiting fonksiyonu
CREATE OR REPLACE FUNCTION check_api_rate_limit(
    p_account_id VARCHAR(36)
)
RETURNS BOOLEAN AS $$
DECLARE
    current_calls INTEGER;
    rate_limit INTEGER;
    reset_time TIMESTAMP;
BEGIN
    SELECT current_api_calls, api_rate_limit, api_calls_reset_at
    INTO current_calls, rate_limit, reset_time
    FROM broker_accounts
    WHERE id = p_account_id;

    -- Zaman aşımı kontrolü
    IF reset_time < CURRENT_TIMESTAMP THEN
        UPDATE broker_accounts
        SET
            current_api_calls = 1,
            api_calls_reset_at = CURRENT_TIMESTAMP + INTERVAL '1 minute'
        WHERE id = p_account_id;
        RETURN TRUE;
    END IF;

    -- Rate limit kontrolü
    IF current_calls >= rate_limit THEN
        RETURN FALSE;
    END IF;

    -- API çağrı sayısını artır
    UPDATE broker_accounts
    SET current_api_calls = current_api_calls + 1
    WHERE id = p_account_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Aktif broker hesapları view
CREATE VIEW active_broker_accounts_view AS
SELECT
    ba.*,
    u.first_name || ' ' || u.last_name as user_full_name,
    u.email as user_email,
    o.name as organization_name,

    -- Hesap durumu analizi
    CASE
        WHEN ba.account_status = 'ACTIVE' AND ba.connection_status = 'CONNECTED'
             AND ba.verification_status = 'VERIFIED' THEN 'READY_TO_TRADE'
        WHEN ba.account_status = 'ACTIVE' AND ba.connection_status = 'DISCONNECTED' THEN 'OFFLINE'
        WHEN ba.account_status = 'ACTIVE' AND ba.verification_status != 'VERIFIED' THEN 'VERIFICATION_REQUIRED'
        ELSE 'NOT_READY'
    END as trading_readiness,

    -- Bakiye durumu
    CASE
        WHEN ba.available_balance > 0 THEN 'SUFFICIENT'
        WHEN ba.account_balance > 0 THEN 'PARTIALLY_BLOCKED'
        ELSE 'INSUFFICIENT'
    END as balance_status,

    -- API kullanım oranı
    CASE
        WHEN ba.api_rate_limit > 0 THEN
            ROUND((ba.current_api_calls::DECIMAL / ba.api_rate_limit::DECIMAL) * 100, 2)
        ELSE 0
    END as api_usage_percentage,

    -- Alt hesap sayısı
    (SELECT COUNT(*) FROM broker_accounts sub WHERE sub.parent_account_id = ba.id) as sub_account_count

FROM broker_accounts ba
JOIN users u ON ba.user_id = u.id
LEFT JOIN organizations o ON ba.organization_id = o.id
WHERE ba.deleted_at IS NULL;

-- Yorumlar
COMMENT ON TABLE broker_accounts IS 'Broker hesap bilgileri - şifreli kimlik bilgileri ve alt hesap desteği';
COMMENT ON COLUMN broker_accounts.account_number IS 'Hesap numarası - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.username IS 'Broker kullanıcı adı - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.password_hash IS 'Broker şifre hash - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.api_key IS 'API anahtarı - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.api_secret IS 'API gizli anahtarı - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.certificate_data IS 'Dijital sertifika - AES-256-GCM ile şifreli';
COMMENT ON COLUMN broker_accounts.parent_account_id IS 'Ana hesap referansı (alt hesaplar için)';
COMMENT ON COLUMN broker_accounts.is_default IS 'Kullanıcının varsayılan broker hesabı';
COMMENT ON VIEW active_broker_accounts_view IS 'Aktif broker hesapları için analitik görünüm';