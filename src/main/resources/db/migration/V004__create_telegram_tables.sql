-- V004: Create Telegram Bot Integration Tables
-- Creates tables for Telegram bot user management and notifications

-- ============================================================================
-- Telegram Users Table
-- Links Telegram users to platform users
-- ============================================================================
CREATE TABLE IF NOT EXISTS telegram_users (
    id                              BIGSERIAL PRIMARY KEY,
    telegram_user_id                BIGINT UNIQUE NOT NULL,
    platform_user_id                VARCHAR(255) REFERENCES user_entities(id) ON DELETE SET NULL,
    first_name                      VARCHAR(255),
    last_name                       VARCHAR(255),
    username                        VARCHAR(255),
    language_code                   VARCHAR(10),
    chat_id                         BIGINT,
    notifications_enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    price_alerts_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    order_notifications_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    last_interaction_at             TIMESTAMP,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for telegram_users
CREATE INDEX idx_telegram_user_id ON telegram_users(telegram_user_id);
CREATE INDEX idx_platform_user_id ON telegram_users(platform_user_id);
CREATE INDEX idx_last_interaction ON telegram_users(last_interaction_at);
CREATE INDEX idx_notifications_enabled ON telegram_users(notifications_enabled) WHERE notifications_enabled = true AND is_active = true;

-- ============================================================================
-- Telegram Notifications Table
-- Stores notification history sent via Telegram bot
-- ============================================================================
CREATE TABLE IF NOT EXISTS telegram_notifications (
    id                      BIGSERIAL PRIMARY KEY,
    telegram_user_id        BIGINT NOT NULL REFERENCES telegram_users(id) ON DELETE CASCADE,
    notification_type       VARCHAR(50) NOT NULL,
    title                   VARCHAR(255),
    message                 TEXT NOT NULL,
    sent_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at                 TIMESTAMP,
    data                    JSONB,
    CONSTRAINT fk_telegram_notifications_user FOREIGN KEY (telegram_user_id) REFERENCES telegram_users(id)
);

-- Indexes for telegram_notifications
CREATE INDEX idx_telegram_notifications_user ON telegram_notifications(telegram_user_id);
CREATE INDEX idx_telegram_notifications_type ON telegram_notifications(notification_type);
CREATE INDEX idx_telegram_notifications_sent ON telegram_notifications(sent_at DESC);
CREATE INDEX idx_telegram_notifications_unread ON telegram_notifications(telegram_user_id, read_at) WHERE read_at IS NULL;

-- ============================================================================
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE telegram_users IS 'Telegram bot users linked to platform accounts';
COMMENT ON COLUMN telegram_users.telegram_user_id IS 'Telegram user ID from Telegram API';
COMMENT ON COLUMN telegram_users.platform_user_id IS 'Link to platform user account';
COMMENT ON COLUMN telegram_users.chat_id IS 'Telegram chat ID for sending messages';
COMMENT ON COLUMN telegram_users.is_active IS 'User has not blocked the bot';
COMMENT ON COLUMN telegram_users.last_interaction_at IS 'Last time user interacted with bot';

COMMENT ON TABLE telegram_notifications IS 'History of notifications sent via Telegram bot';
COMMENT ON COLUMN telegram_notifications.notification_type IS 'Type of notification (PRICE_ALERT, ORDER_FILLED, etc.)';
COMMENT ON COLUMN telegram_notifications.data IS 'Additional JSON data for the notification';
