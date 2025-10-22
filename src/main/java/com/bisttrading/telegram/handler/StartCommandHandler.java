package com.bisttrading.telegram.handler;

import com.bisttrading.telegram.bot.BistTelegramBot;
import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.model.TelegramUser;
import com.bisttrading.telegram.repository.TelegramUserRepository;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Handler for /start command.
 * Welcomes new users and creates Telegram user record.
 */
@Slf4j
@Component
public class StartCommandHandler extends BaseCommandHandler {

    private final TelegramUserRepository telegramUserRepository;

    public StartCommandHandler(
            BistTelegramBot bot,
            TelegramSessionService sessionService,
            TelegramUserRepository telegramUserRepository) {
        super(bot, sessionService);
        this.telegramUserRepository = telegramUserRepository;
    }

    @Override
    public String getCommand() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Bot'u başlat ve hoş geldin mesajı al";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        User telegramUser = update.getMessage().getFrom();
        Long telegramUserId = telegramUser.getId();

        logCommand(update);

        // Create or update Telegram user record
        createOrUpdateTelegramUser(telegramUser, chatId);

        // Check if user is already logged in
        boolean isLoggedIn = isAuthenticated(telegramUserId);

        // Build welcome message
        String welcomeMessage = buildWelcomeMessage(telegramUser.getFirstName(), isLoggedIn);

        // Send welcome message with main menu
        sendMessage(chatId, welcomeMessage, KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
    }

    /**
     * Create or update Telegram user record in database
     */
    private void createOrUpdateTelegramUser(User user, Long chatId) {
        try {
            var existingUser = telegramUserRepository.findByTelegramUserId(user.getId());

            if (existingUser.isEmpty()) {
                // Create new Telegram user
                TelegramUser newUser = TelegramUser.builder()
                    .telegramUserId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .username(user.getUserName())
                    .languageCode(user.getLanguageCode())
                    .chatId(chatId)
                    .isActive(true)
                    .build();

                newUser.updateLastInteraction();
                telegramUserRepository.save(newUser);

                log.info("Created new Telegram user: {} ({})", user.getFirstName(), user.getId());
            } else {
                // Update existing user
                TelegramUser telegramUser = existingUser.get();
                telegramUser.setFirstName(user.getFirstName());
                telegramUser.setLastName(user.getLastName());
                telegramUser.setUsername(user.getUserName());
                telegramUser.setLanguageCode(user.getLanguageCode());
                telegramUser.setChatId(chatId);
                telegramUser.setIsActive(true);
                telegramUser.updateLastInteraction();

                telegramUserRepository.save(telegramUser);

                log.debug("Updated Telegram user: {}", user.getId());
            }
        } catch (Exception e) {
            log.error("Failed to create/update Telegram user", e);
        }
    }

    /**
     * Build personalized welcome message
     */
    private String buildWelcomeMessage(String firstName, boolean isLoggedIn) {
        StringBuilder message = new StringBuilder();

        message.append("*🚀 BIST Trading Platform - Telegram Bot*\n\n");
        message.append("Merhaba ").append(firstName != null ? firstName : "").append("! 👋\n\n");

        if (isLoggedIn) {
            message.append("✅ Zaten giriş yapmışsınız.\n\n");
            message.append("*Ana Menü* butonlarını kullanarak işlemlerinize devam edebilirsiniz:\n\n");
            message.append("📊 *Piyasa Verileri* - Hisse fiyatları, sembol arama\n");
            message.append("💼 *Broker* - Hesap bilgileri, pozisyonlar\n");
            message.append("📋 *Emirler* - Emir gönderme, takip\n");
            message.append("⭐ *Watchlist* - Favori hisseleriniz\n");
        } else {
            message.append("Borsa İstanbul için interaktif trading botuna hoş geldiniz!\n\n");
            message.append("*Özellikler:*\n");
            message.append("📊 Gerçek zamanlı piyasa verileri\n");
            message.append("💼 Broker hesap yönetimi\n");
            message.append("📋 Emir gönderme ve takip\n");
            message.append("⭐ Watchlist ve fiyat alarmları\n");
            message.append("🔔 Anlık bildirimler\n\n");
            message.append("*Başlamak için giriş yapın:*\n");
            message.append("👇 Aşağıdaki *Giriş Yap* butonuna tıklayın\n");
        }

        message.append("\n_Komutları görmek için:_ /help");

        return message.toString();
    }
}
