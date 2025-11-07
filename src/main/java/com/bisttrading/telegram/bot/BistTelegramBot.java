package com.bisttrading.telegram.bot;

import com.bisttrading.telegram.config.TelegramBotProperties;
import com.bisttrading.telegram.dto.ConversationState;
import com.bisttrading.telegram.handler.CommandHandler;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main Telegram Bot class for BIST Trading Platform.
 * Handles incoming updates and routes them to appropriate command handlers.
 * Uses TelegramBots 7.x API (Spring Boot 3 compatible).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BistTelegramBot implements LongPollingSingleThreadUpdateConsumer {

    @Getter
    private final TelegramClient telegramClient;
    private final TelegramBotProperties properties;
    private final List<CommandHandler> handlers;
    private final TelegramSessionService sessionService;

    private Map<String, CommandHandler> commandHandlers;

    /**
     * Initialize command handlers map (called after bean creation)
     */
    public void init() {
        this.commandHandlers = handlers.stream()
            .collect(Collectors.toMap(
                CommandHandler::getCommand,
                Function.identity()
            ));

        log.info("🤖 BIST Telegram Bot initialized");
        log.info("   Bot Username: @{}", properties.getUsername());
        log.info("   Registered Commands: {}", commandHandlers.keySet());
    }

    public String getBotUsername() {
        return properties.getUsername();
    }

    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public void consume(Update update) {
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
            // Handle non-command text (conversation flows)
            log.debug("Non-command text received from user {}: {}", userId, text);

            // Get user's conversation state to route to correct handler
            ConversationState state = sessionService.getConversationState(userId);

            // Route based on conversation state
            CommandHandler targetHandler = null;

            if (state == ConversationState.WAITING_ALGOLAB_USERNAME ||
                state == ConversationState.WAITING_ALGOLAB_PASSWORD ||
                state == ConversationState.WAITING_ALGOLAB_OTP) {
                targetHandler = commandHandlers.get("broker");
            } else if (state == ConversationState.WAITING_ORDER_SYMBOL ||
                       state == ConversationState.WAITING_ORDER_SIDE ||
                       state == ConversationState.WAITING_ORDER_PRICE_TYPE ||
                       state == ConversationState.WAITING_ORDER_QUANTITY ||
                       state == ConversationState.WAITING_ORDER_PRICE ||
                       state == ConversationState.WAITING_MODIFY_PRICE ||
                       state == ConversationState.WAITING_MODIFY_QUANTITY) {
                targetHandler = commandHandlers.get("orders");
            } else if (state == ConversationState.WAITING_USERNAME ||
                       state == ConversationState.WAITING_PASSWORD) {
                targetHandler = commandHandlers.get("login");
            } else if (state == ConversationState.WAITING_SEARCH_QUERY) {
                targetHandler = commandHandlers.get("market");
            } else {
                // No active conversation state - ignore or send help
                log.debug("No active conversation state for user {}, ignoring text: {}", userId, text);
                return;
            }

            if (targetHandler != null) {
                log.info("Routing conversation message to handler: {}", targetHandler.getCommand());
                targetHandler.handle(update);
            }
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
            telegramClient.execute(
                org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .build()
            );
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
    }

    /**
     * Send error message to user
     */
    private void sendErrorMessage(Long chatId, String errorText) throws TelegramApiException {
        var message = SendMessage.builder()
            .chatId(chatId.toString())
            .text(errorText)
            .parseMode("Markdown")
            .build();

        telegramClient.execute(message);
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

    /**
     * Send a message (used by handlers)
     */
    public void execute(SendMessage message) throws TelegramApiException {
        telegramClient.execute(message);
    }

    /**
     * Edit a message (used by handlers)
     */
    public void execute(EditMessageText message) throws TelegramApiException {
        telegramClient.execute(message);
    }
}
