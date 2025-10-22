package com.bisttrading.telegram.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Interface for Telegram command handlers.
 * Each command (e.g., /start, /login, /quote) implements this interface.
 */
public interface CommandHandler {

    /**
     * Get the command name (without slash)
     * e.g., "start", "login", "quote"
     */
    String getCommand();

    /**
     * Get command description for help text
     */
    String getDescription();

    /**
     * Handle the command
     * @param update Telegram update object
     * @throws TelegramApiException if Telegram API call fails
     */
    void handle(Update update) throws TelegramApiException;

    /**
     * Check if command requires authentication
     * @return true if user must be logged in
     */
    default boolean requiresAuth() {
        return false;
    }

    /**
     * Check if command requires AlgoLab authentication
     * @return true if AlgoLab session is required
     */
    default boolean requiresAlgoLabAuth() {
        return false;
    }
}
