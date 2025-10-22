package com.bisttrading.telegram.handler;

import com.bisttrading.dto.LoginRequest;
import com.bisttrading.dto.JwtResponse;
import com.bisttrading.service.AuthenticationService;
import com.bisttrading.telegram.bot.BistTelegramBot;
import com.bisttrading.telegram.dto.ConversationState;
import com.bisttrading.telegram.dto.TelegramUserSession;
import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Handler for /login command.
 * Implements conversation flow for platform authentication.
 */
@Slf4j
@Component
public class LoginCommandHandler extends BaseCommandHandler {

    private final AuthenticationService authenticationService;

    public LoginCommandHandler(
            BistTelegramBot bot,
            TelegramSessionService sessionService,
            AuthenticationService authenticationService) {
        super(bot, sessionService);
        this.authenticationService = authenticationService;
    }

    @Override
    public String getCommand() {
        return "login";
    }

    @Override
    public String getDescription() {
        return "Platform'a giriş yap";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        // Check if already logged in
        if (isAuthenticated(userId)) {
            sendMessage(chatId,
                "✅ *Zaten giriş yapmışsınız!*\n\n" +
                "Çıkış yapmak için: /logout",
                KeyboardFactory.createMainMenuKeyboard(true));
            return;
        }

        // Check conversation state
        ConversationState state = sessionService.getConversationState(userId);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            // Check if this is the /login command itself
            if (text.startsWith("/login")) {
                startLoginFlow(chatId, userId);
            } else {
                // Handle conversation steps
                handleConversationInput(chatId, userId, text, state);
            }
        }
    }

    /**
     * Start login conversation flow
     */
    private void startLoginFlow(Long chatId, Long userId) throws TelegramApiException {
        sendMessage(chatId,
            "*🔐 Platform Girişi*\n\n" +
            "Lütfen kullanıcı adınızı girin:\n\n" +
            "_İpucu: Kayıt olduğunuz kullanıcı adı veya email_\n" +
            "_İptal etmek için:_ /cancel");

        sessionService.setConversationState(userId, ConversationState.WAITING_USERNAME);
        log.debug("Login flow started for user: {}", userId);
    }

    /**
     * Handle conversation input based on current state
     */
    private void handleConversationInput(Long chatId, Long userId, String text, ConversationState state)
            throws TelegramApiException {

        switch (state) {
            case WAITING_USERNAME -> handleUsernameInput(chatId, userId, text);
            case WAITING_PASSWORD -> handlePasswordInput(chatId, userId, text);
            default -> {
                // No active conversation, ignore
            }
        }
    }

    /**
     * Handle username input
     */
    private void handleUsernameInput(Long chatId, Long userId, String username) throws TelegramApiException {
        // Store username temporarily
        sessionService.setTempData(userId, "username", username);

        // Ask for password
        sendMessage(chatId,
            "*Kullanıcı adı:* `" + username + "`\n\n" +
            "Şimdi şifrenizi girin:\n\n" +
            "_Not: Şifreniz gizli tutulacaktır._\n" +
            "_İptal etmek için:_ /cancel");

        sessionService.setConversationState(userId, ConversationState.WAITING_PASSWORD);
        log.debug("Username received for user: {}", userId);
    }

    /**
     * Handle password input and attempt login
     */
    private void handlePasswordInput(Long chatId, Long userId, String password) throws TelegramApiException {
        String username = sessionService.getTempData(userId, "username");

        if (username == null) {
            sendMessage(chatId,
                "❌ Oturum süresi doldu. Lütfen tekrar başlayın.\n\n" +
                "/login");
            sessionService.clearConversationState(userId);
            return;
        }

        // Show loading message
        sendMessage(chatId, "🔄 Giriş yapılıyor...");

        try {
            // Call authentication service (existing service!)
            LoginRequest loginRequest = new LoginRequest(username, password);
            JwtResponse authResponse = authenticationService.authenticate(loginRequest);

            // Create Telegram user session in Redis
            TelegramUserSession session = TelegramUserSession.builder()
                .telegramUserId(userId)
                .platformUserId(authResponse.getUserId())
                .username(username)
                .jwtToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .algoLabAuthenticated(false)
                .build();

            sessionService.saveSession(session);

            // Clear conversation state and temp data
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);

            // Send success message
            sendMessage(chatId,
                "✅ *Giriş başarılı!*\n\n" +
                "Hoş geldiniz, *" + username + "*!\n\n" +
                "Artık tüm özelliklere erişebilirsiniz.",
                KeyboardFactory.createMainMenuKeyboard(true));

            log.info("Successful login for Telegram user {} as platform user {}", userId, username);

        } catch (Exception e) {
            log.error("Login failed for user: {}", userId, e);

            // Clear conversation
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);

            // Send error message
            sendMessage(chatId,
                "❌ *Giriş başarısız!*\n\n" +
                "Kullanıcı adı veya şifre hatalı.\n\n" +
                "Tekrar denemek için: /login");
        }
    }
}
