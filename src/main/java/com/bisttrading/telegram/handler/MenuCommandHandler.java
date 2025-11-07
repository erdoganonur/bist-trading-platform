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

        // Handle callback query (button clicks)
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] parts = callbackData.split(":");

            if (parts.length > 1) {
                String action = parts[1];
                handleMenuAction(chatId, userId, action, isLoggedIn);
                return;
            }
        }

        // Show main menu (from /menu command)
        String menuMessage = buildMenuMessage(isLoggedIn);
        sendMessage(chatId, menuMessage, KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
    }

    /**
     * Handle menu action from callback query
     */
    private void handleMenuAction(Long chatId, Long userId, String action, boolean isLoggedIn)
            throws TelegramApiException {

        log.debug("Handling menu action: {} for user: {}", action, userId);

        switch (action) {
            case "main" -> {
                // Show main menu
                String menuMessage = buildMenuMessage(isLoggedIn);
                sendMessage(chatId, menuMessage, KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
            }
            case "market" -> {
                // Show market data menu
                sendMessage(chatId,
                    "*📊 Piyasa Verileri*\n\nHisse bilgilerini görüntülemek için aşağıdaki seçeneklerden birini seçin:",
                    KeyboardFactory.createMarketDataKeyboard());
            }
            case "broker" -> {
                // Show broker menu
                boolean algoLabConnected = sessionService.getSession(userId)
                    .map(session -> session.isAlgoLabSessionValid())
                    .orElse(false);

                sendMessage(chatId,
                    "*💼 Broker*\n\nBroker işlemleriniz için aşağıdaki seçenekleri kullanabilirsiniz:",
                    KeyboardFactory.createBrokerKeyboard(algoLabConnected));
            }
            case "logout" -> {
                // Handle logout
                sessionService.logout(userId);
                sendMessage(chatId,
                    "*🚪 Çıkış Yapıldı*\n\nBaşarıyla çıkış yaptınız.\n\nTekrar giriş yapmak için /login komutunu kullanabilirsiniz.",
                    KeyboardFactory.createMainMenuKeyboard(false));
            }
            case "orders" -> {
                // Show orders menu with pending orders option
                sendMessage(chatId,
                    "*📋 Emirler*\n\nEmir işlemleri için aşağıdaki seçenekleri kullanabilirsiniz:",
                    KeyboardFactory.createOrdersMenuKeyboard());
            }
            case "watchlist", "profile", "settings" -> {
                // Not implemented yet
                sendMessage(chatId,
                    "*⚠️ Geliştirme Aşamasında*\n\nBu özellik henüz geliştirilmektedir. Lütfen daha sonra tekrar deneyin.",
                    KeyboardFactory.createBackButton("menu:main"));
            }
            default -> {
                log.warn("Unknown menu action: {}", action);
                sendMessage(chatId, "Bilinmeyen işlem. Ana menüye dönülüyor...",
                    KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
            }
        }
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
