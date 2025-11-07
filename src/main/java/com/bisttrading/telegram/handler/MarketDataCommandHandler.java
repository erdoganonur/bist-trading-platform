package com.bisttrading.telegram.handler;

import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for market data actions.
 * Handles symbol search, quotes, sectors, etc.
 */
@Slf4j
@Component
public class MarketDataCommandHandler extends BaseCommandHandler {

    public MarketDataCommandHandler(TelegramClient telegramClient, TelegramSessionService sessionService) {
        super(telegramClient, sessionService);
    }

    @Override
    public String getCommand() {
        return "market";
    }

    @Override
    public String getDescription() {
        return "Piyasa verileri";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        logCommand(update);

        // Parse callback data
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] parts = callbackData.split(":");

            if (parts.length > 1) {
                String action = parts[1];
                handleMarketAction(chatId, userId, action);
                return;
            }
        }

        // Default: show market data menu
        sendMessage(chatId,
            "*📊 Piyasa Verileri*\n\nHisse bilgilerini görüntülemek için aşağıdaki seçeneklerden birini seçin:",
            KeyboardFactory.createMarketDataKeyboard());
    }

    private void handleMarketAction(Long chatId, Long userId, String action) throws TelegramApiException {
        log.debug("Handling market action: {} for user: {}", action, userId);

        switch (action) {
            case "search" -> {
                sendMessage(chatId,
                    "*🔍 Sembol Arama*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:market"));
            }
            case "quote" -> {
                sendMessage(chatId,
                    "*📈 Hisse Fiyatı*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:market"));
            }
            case "sectors" -> {
                sendMessage(chatId,
                    "*📊 Sektörler*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:market"));
            }
            case "trending" -> {
                sendMessage(chatId,
                    "*🔥 Popüler Hisseler*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:market"));
            }
            default -> {
                log.warn("Unknown market action: {}", action);
                sendMessage(chatId,
                    "Bilinmeyen işlem.",
                    KeyboardFactory.createMarketDataKeyboard());
            }
        }
    }
}
