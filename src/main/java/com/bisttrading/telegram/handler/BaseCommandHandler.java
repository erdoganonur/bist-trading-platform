package com.bisttrading.telegram.handler;

import com.bisttrading.telegram.bot.BistTelegramBot;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Base class for command handlers.
 * Provides common utility methods for sending messages.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseCommandHandler implements CommandHandler {

    @Getter
    protected final BistTelegramBot bot;
    protected final TelegramSessionService sessionService;

    /**
     * Send a text message to user
     */
    protected void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .build();

        bot.execute(message);
    }

    /**
     * Send a message with inline keyboard
     */
    protected void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();

        bot.execute(message);
    }

    /**
     * Extract chat ID from update
     */
    protected Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        throw new IllegalArgumentException("No chat ID found in update");
    }

    /**
     * Extract Telegram user ID from update
     */
    protected Long getTelegramUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }
        throw new IllegalArgumentException("No user ID found in update");
    }

    /**
     * Check if user is authenticated
     */
    protected boolean isAuthenticated(Long telegramUserId) {
        return sessionService.isLoggedIn(telegramUserId);
    }

    /**
     * Handle authentication check
     * Sends error message if not authenticated
     */
    protected boolean checkAuthentication(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        if (!isAuthenticated(userId)) {
            sendMessage(chatId,
                "❌ *Bu komutu kullanmak için giriş yapmalısınız.*\n\n" +
                "Giriş yapmak için: /login");
            return false;
        }

        return true;
    }

    /**
     * Log command execution
     */
    protected void logCommand(Update update) {
        Long userId = getTelegramUserId(update);
        String command = getCommand();
        log.info("Command '{}' executed by Telegram user: {}", command, userId);
    }
}
