package com.bisttrading.telegram.bot;

import com.bisttrading.telegram.config.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/**
 * Initializes and registers the Telegram bot on application startup.
 * Only runs if telegram.bot.enabled=true
 * Uses TelegramBots 7.x API (Spring Boot 3 compatible).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class TelegramBotInitializer implements CommandLineRunner {

    private final TelegramBotProperties properties;
    private final BistTelegramBot bot;

    private TelegramBotsLongPollingApplication botsApplication;

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            log.info("⏭️  Telegram Bot disabled (telegram.bot.enabled=false)");
            return;
        }

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("⚠️  Telegram Bot token not configured. Set TELEGRAM_BOT_TOKEN environment variable.");
            log.warn("   Bot will not start. To disable this warning, set telegram.bot.enabled=false");
            return;
        }

        try {
            log.info("🚀 Starting Telegram Bot...");

            // Initialize command handlers
            bot.init();

            // Register bot with TelegramBots API (v7.x)
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(bot.getBotToken(), bot);

            log.info("✅ Telegram Bot started successfully!");
            log.info("   Bot Username: @{}", properties.getUsername());
            log.info("   API Version: TelegramBots 7.x");
            log.info("   Mode: Long Polling");
            log.info("   Session Timeout: {} seconds", properties.getSession().getTimeoutSeconds());

            // Print feature flags
            if (properties.getFeatures().isNotifications()) {
                log.info("   ✓ Notifications: Enabled");
            }
            if (properties.getFeatures().isWatchlistUpdates()) {
                log.info("   ✓ Watchlist Updates: Enabled");
            }
            if (properties.getFeatures().isPriceAlerts()) {
                log.info("   ✓ Price Alerts: Enabled");
            }

            log.info("📱 Users can now interact with the bot via Telegram");

        } catch (Exception e) {
            log.error("❌ Failed to start Telegram Bot", e);
            log.error("   Please check:");
            log.error("   - Bot token is valid");
            log.error("   - Bot is registered with BotFather");
            log.error("   - Internet connection is available");
            log.error("");
            log.error("   Application will continue without Telegram Bot functionality.");
        }
    }

    /**
     * Shutdown hook to close bot gracefully
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        if (botsApplication != null) {
            try {
                log.info("🛑 Stopping Telegram Bot...");
                botsApplication.close();
                log.info("✅ Telegram Bot stopped successfully");
            } catch (Exception e) {
                log.error("Error while stopping Telegram Bot", e);
            }
        }
    }
}
