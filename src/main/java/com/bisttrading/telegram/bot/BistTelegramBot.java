package com.bisttrading.telegram.bot;

import com.bisttrading.telegram.config.TelegramBotProperties;
import com.bisttrading.telegram.handler.CommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main Telegram Bot class for BIST Trading Platform.
 * Handles incoming updates and routes them to appropriate command handlers.
 */
@Slf4j
@Component
public class BistTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties properties;
    private final Map<String, CommandHandler> commandHandlers;

    public BistTelegramBot(
            TelegramBotProperties properties,
            List<CommandHandler> handlers) {
        super(properties.getToken());
        this.properties = properties;

        // Build command handler map
        this.commandHandlers = handlers.stream()
            .collect(Collectors.toMap(
                CommandHandler::getCommand,
                Function.identity()
            ));

        log.info("🤖 BIST Telegram Bot initialized");
        log.info("   Bot Username: @{}", properties.getUsername());
        log.info("   Registered Commands: {}", commandHandlers.keySet());
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API error while processing update", e);
        } catch (Exception e) {
            log.error("Unexpected error while processing update", e);

            // Try to send error message to user
            try {
                Long chatId = extractChatId(update);
                if (chatId != null) {
                    sendErrorMessage(chatId, "Bir hata oluştu. Lütfen tekrar deneyin.");
                }
            } catch (Exception ex) {
                log.error("Failed to send error message to user", ex);
            }
        }
    }

    /**
     * Handle text messages (commands and regular text)
     */
    private void handleTextMessage(Update update) throws TelegramApiException {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();

        log.debug("Received message from user {}: {}", userId, text);

        if (text.startsWith("/")) {
            // Handle command
            String[] parts = text.split(" ");
            String commandText = parts[0].substring(1); // Remove "/"

            // Remove bot username if present (e.g., "/start@BotUsername")
            if (commandText.contains("@")) {
                commandText = commandText.split("@")[0];
            }

            CommandHandler handler = commandHandlers.get(commandText);

            if (handler != null) {
                log.info("Executing command: {} for user: {}", commandText, userId);
                handler.handle(update);
            } else {
                sendErrorMessage(chatId,
                    "❌ Bilinmeyen komut: `" + commandText + "`\n\n" +
                    "Kullanılabilir komutları görmek için: /help");
            }
        } else {
            // Handle non-command text (conversation flows are handled by individual handlers)
            log.debug("Non-command text received from user {}: {}", userId, text);
        }
    }

    /**
     * Handle callback queries (inline keyboard button clicks)
     */
    private void handleCallbackQuery(Update update) throws TelegramApiException {
        var callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId();

        log.debug("Received callback query from user {}: {}", userId, data);

        // Answer callback query to remove loading state
        answerCallbackQuery(callbackQuery.getId());

        // Parse callback data (format: "command:action:param")
        String[] parts = data.split(":");
        if (parts.length == 0) {
            log.warn("Invalid callback data: {}", data);
            return;
        }

        String command = parts[0];
        CommandHandler handler = commandHandlers.get(command);

        if (handler != null) {
            log.info("Executing callback handler: {} for user: {}", command, userId);
            handler.handle(update);
        } else {
            log.warn("No handler found for callback: {}", command);
        }
    }

    /**
     * Answer callback query (removes loading indicator)
     */
    private void answerCallbackQuery(String callbackQueryId) {
        try {
            execute(new org.telegram.telegrambots.meta.api.methods.answercallbackquery.AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
    }

    /**
     * Send error message to user
     */
    private void sendErrorMessage(Long chatId, String errorText) throws TelegramApiException {
        var message = org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
            .chatId(chatId.toString())
            .text(errorText)
            .parseMode("Markdown")
            .build();

        execute(message);
    }

    /**
     * Extract chat ID from update
     */
    private Long extractChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }
}
