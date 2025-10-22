package com.bisttrading.telegram.bot;

import com.bisttrading.telegram.config.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Initializes and registers the Telegram bot on application startup.
 * Only runs if telegram.bot.enabled=true
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class TelegramBotInitializer implements CommandLineRunner {

    private final TelegramBotProperties properties;
    private final BistTelegramBot bot;

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

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);

            log.info("✅ Telegram Bot started successfully!");
            log.info("   Bot Username: @{}", properties.getUsername());
            log.info("   Mode: Polling (Long Polling)");
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

        } catch (TelegramApiException e) {
            log.error("❌ Failed to start Telegram Bot", e);
            log.error("   Please check:");
            log.error("   - Bot token is valid");
            log.error("   - Bot is registered with BotFather");
            log.error("   - Internet connection is available");
            log.error("");
            log.error("   Application will continue without Telegram Bot functionality.");
        } catch (Exception e) {
            log.error("❌ Unexpected error while starting Telegram Bot", e);
        }
    }
}
