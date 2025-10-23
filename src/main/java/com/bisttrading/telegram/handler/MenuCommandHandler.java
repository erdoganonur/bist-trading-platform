package com.bisttrading.telegram.handler;

import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for /menu command and main menu callbacks.
 * Shows the main navigation menu.
 */
@Slf4j
@Component
public class MenuCommandHandler extends BaseCommandHandler {

    public MenuCommandHandler(TelegramClient telegramClient, TelegramSessionService sessionService) {
        super(telegramClient, sessionService);
    }

    @Override
    public String getCommand() {
        return "menu";
    }

    @Override
    public String getDescription() {
        return "Ana menüyü göster";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        logCommand(update);

        boolean isLoggedIn = isAuthenticated(userId);

        String menuMessage = buildMenuMessage(isLoggedIn);
        sendMessage(chatId, menuMessage, KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
    }

    /**
     * Build menu message based on login status
     */
    private String buildMenuMessage(boolean isLoggedIn) {
        if (isLoggedIn) {
            return "*📋 Ana Menü*\n\n" +
                   "Aşağıdaki seçeneklerden birini seçin:\n\n" +
                   "📊 *Piyasa Verileri* - Hisse fiyatları, sembol arama\n" +
                   "💼 *Broker* - Hesap ve pozisyon bilgileri\n" +
                   "📋 *Emirler* - Emir gönderme ve takip\n" +
                   "⭐ *Watchlist* - Favori hisseleriniz\n" +
                   "👤 *Profil* - Hesap bilgileriniz\n" +
                   "⚙️ *Ayarlar* - Bot ayarları";
        } else {
            return "*📋 Ana Menü*\n\n" +
                   "Özelliklere erişmek için giriş yapmanız gerekmektedir.\n\n" +
                   "👇 *Giriş Yap* butonuna tıklayarak başlayın.";
        }
    }
}
