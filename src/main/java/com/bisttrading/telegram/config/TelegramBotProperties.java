package com.bisttrading.telegram.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Telegram Bot configuration properties.
 * Maps properties from application.yml (telegram.bot.*) to Java objects.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {

    /**
     * Enable or disable Telegram bot functionality
     */
    private boolean enabled = true;

    /**
     * Bot username (from BotFather)
     */
    private String username;

    /**
     * Bot API token (from BotFather)
     */
    private String token;

    /**
     * Webhook configuration
     */
    private Webhook webhook = new Webhook();

    /**
     * Bot options
     */
    private Options options = new Options();

    /**
     * Session configuration
     */
    private Session session = new Session();

    /**
     * Feature flags
     */
    private Features features = new Features();

    @Data
    public static class Webhook {
        private boolean enabled = false;
        private String url;
    }

    @Data
    public static class Options {
        private int maxThreads = 10;
        private int maxQueueSize = 100;
        private int timeout = 30;
    }

    @Data
    public static class Session {
        private long timeoutSeconds = 3600; // 1 hour
        private String redisKeyPrefix = "telegram:session:";
    }

    @Data
    public static class Features {
        private boolean notifications = true;
        private boolean watchlistUpdates = true;
        private boolean priceAlerts = true;
        private boolean autoLoginReminder = true;
    }
}
