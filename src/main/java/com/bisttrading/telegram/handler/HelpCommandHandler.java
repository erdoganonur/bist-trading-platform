package com.bisttrading.telegram.handler;

import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for /help command.
 * Shows available commands and usage instructions.
 */
@Slf4j
@Component
public class HelpCommandHandler extends BaseCommandHandler {

    public HelpCommandHandler(TelegramClient telegramClient, TelegramSessionService sessionService) {
        super(telegramClient, sessionService);
    }

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Komut listesi ve yardım";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        logCommand(update);

        boolean isLoggedIn = isAuthenticated(userId);

        String helpMessage = buildHelpMessage(isLoggedIn);
        sendMessage(chatId, helpMessage, KeyboardFactory.createMainMenuKeyboard(isLoggedIn));
    }

    /**
     * Build help message with available commands
     */
    private String buildHelpMessage(boolean isLoggedIn) {
        StringBuilder message = new StringBuilder();

        message.append("*📖 BIST Telegram Bot - Komutlar*\n\n");

        // Basic commands (always available)
        message.append("*🔹 Temel Komutlar*\n");
        message.append("/start - Bot'u başlat\n");
        message.append("/help - Bu yardım mesajı\n");
        message.append("/menu - Ana menüyü göster\n\n");

        if (!isLoggedIn) {
            // Not logged in
            message.append("*🔐 Giriş*\n");
            message.append("/login - Platform'a giriş yap\n\n");
            message.append("_Diğer komutları kullanmak için önce giriş yapmalısınız._\n");
        } else {
            // Logged in - show all commands
            message.append("*📊 Piyasa Verileri*\n");
            message.append("/quote SEMBOL - Hisse fiyat bilgisi\n");
            message.append("/search ARAMA - Sembol ara\n");
            message.append("/sectors - Sektör listesi\n\n");

            message.append("*💼 Broker İşlemleri*\n");
            message.append("/account - Hesap bilgileri\n");
            message.append("/positions - Açık pozisyonlar\n");
            message.append("/algolab - AlgoLab bağlantısı\n\n");

            message.append("*📋 Emir İşlemleri*\n");
            message.append("/order - Yeni emir gönder\n");
            message.append("/orders - Açık emirler\n");
            message.append("/history - Emir geçmişi\n\n");

            message.append("*⭐ Watchlist*\n");
            message.append("/watchlist - İzlenen hisseler\n");
            message.append("/watch SEMBOL - İzlemeye ekle\n");
            message.append("/unwatch SEMBOL - İzlemeden çıkar\n\n");

            message.append("*👤 Hesap*\n");
            message.append("/profile - Profil bilgileri\n");
            message.append("/settings - Bot ayarları\n");
            message.append("/logout - Çıkış yap\n\n");
        }

        message.append("*💡 İpuçları*\n");
        message.append("• Butonları kullanarak hızlıca gezinebilirsiniz\n");
        message.append("• Emir göndermeden önce AlgoLab'a bağlanın\n");
        message.append("• Watchlist ile hisselerinizi takip edin\n\n");

        message.append("_Sorularınız için: support@bisttrading.com_");

        return message.toString();
    }
}
