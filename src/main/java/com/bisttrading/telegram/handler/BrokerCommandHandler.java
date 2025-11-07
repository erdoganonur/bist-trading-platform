package com.bisttrading.telegram.handler;

import com.bisttrading.broker.algolab.exception.AlgoLabAuthenticationException;
import com.bisttrading.broker.algolab.service.AlgoLabAuthService;
import com.bisttrading.broker.algolab.service.AlgoLabOrderService;
import com.bisttrading.telegram.dto.ConversationState;
import com.bisttrading.telegram.dto.TelegramUserSession;
import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handler for broker-related actions.
 * Handles AlgoLab connection, account info, positions, etc.
 */
@Slf4j
@Component
public class BrokerCommandHandler extends BaseCommandHandler {

    private final AlgoLabAuthService algoLabAuthService;
    private final AlgoLabOrderService algoLabOrderService;

    public BrokerCommandHandler(
            TelegramClient telegramClient,
            TelegramSessionService sessionService,
            AlgoLabAuthService algoLabAuthService,
            AlgoLabOrderService algoLabOrderService) {
        super(telegramClient, sessionService);
        this.algoLabAuthService = algoLabAuthService;
        this.algoLabOrderService = algoLabOrderService;
    }

    @Override
    public String getCommand() {
        return "broker";
    }

    @Override
    public String getDescription() {
        return "Broker işlemleri";
    }

    @Override
    public void handle(Update update) throws TelegramApiException {
        Long chatId = getChatId(update);
        Long userId = getTelegramUserId(update);

        logCommand(update);

        // Check conversation state
        ConversationState state = sessionService.getConversationState(userId);

        // Handle text messages (conversation flow)
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            // Handle AlgoLab conversation states
            if (state == ConversationState.WAITING_ALGOLAB_USERNAME ||
                state == ConversationState.WAITING_ALGOLAB_PASSWORD ||
                state == ConversationState.WAITING_ALGOLAB_OTP) {
                handleAlgoLabConversationInput(chatId, userId, text, state);
                return;
            }
        }

        // Parse callback data
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] parts = callbackData.split(":");

            if (parts.length > 1) {
                String action = parts[1];

                // Handle position quick actions (buy/sell from position)
                if (("buy".equals(action) || "sell".equals(action)) && parts.length > 2) {
                    String symbol = parts[2];
                    handlePositionAction(chatId, userId, action, symbol);
                    return;
                }

                handleBrokerAction(chatId, userId, action);
                return;
            }
        }

        // Default: show broker menu (check actual AlgoLab connection status)
        boolean isAlgoLabConnected = sessionService.getSession(userId)
            .map(TelegramUserSession::isAlgoLabSessionValid)
            .orElse(false);

        sendMessage(chatId,
            "*💼 Broker*\n\nBroker işlemleriniz için aşağıdaki seçenekleri kullanabilirsiniz:",
            KeyboardFactory.createBrokerKeyboard(isAlgoLabConnected));
    }

    private void handleBrokerAction(Long chatId, Long userId, String action) throws TelegramApiException {
        log.debug("Handling broker action: {} for user: {}", action, userId);

        switch (action) {
            case "connect" -> startAlgoLabConnection(chatId, userId);
            case "account" -> {
                sendMessage(chatId,
                    "*💰 Hesap Bilgileri*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:broker"));
            }
            case "positions" -> showPositions(chatId, userId);
            case "status" -> showAlgoLabStatus(chatId, userId);
            default -> {
                log.warn("Unknown broker action: {}", action);
                sendMessage(chatId,
                    "Bilinmeyen işlem.",
                    KeyboardFactory.createBrokerKeyboard(false));
            }
        }
    }

    /**
     * Handle position quick action (buy/sell from position)
     * Delegates to OrderCommandHandler for quick order flow
     */
    public void handlePositionAction(Long chatId, Long userId, String action, String symbol) throws TelegramApiException {
        log.info("Handling position action: {} for symbol: {} by user: {}", action, symbol, userId);

        // Check if user is logged in and has AlgoLab session
        TelegramUserSession session = sessionService.getSession(userId).orElse(null);
        if (session == null || !session.isAlgoLabSessionValid()) {
            sendMessage(chatId,
                "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                "Önce AlgoLab'a bağlanmalısınız.",
                KeyboardFactory.createBackButton("menu:broker"));
            return;
        }

        // Save symbol and side to temp data for quick order flow
        sessionService.setTempData(userId, "quick_order_symbol", symbol);
        sessionService.setTempData(userId, "quick_order_side", action); // "buy" or "sell"
        sessionService.setConversationState(userId, com.bisttrading.telegram.dto.ConversationState.WAITING_ORDER_PRICE_TYPE);

        String sideEmoji = "buy".equalsIgnoreCase(action) ? "🟢" : "🔴";
        String sideText = "buy".equalsIgnoreCase(action) ? "ALIS" : "SATIŞ";

        sendMessage(chatId,
            String.format("*📝 Hızlı Emir: %s %s*\n\n", sideEmoji, symbol) +
            String.format("İşlem: %s %s\n\n", sideEmoji, sideText) +
            "💰 *Emir tipini* seçin:",
            KeyboardFactory.createOrderTypeKeyboard());
    }

    /**
     * Start AlgoLab connection flow
     */
    private void startAlgoLabConnection(Long chatId, Long userId) throws TelegramApiException {
        // Check if already connected
        TelegramUserSession session = sessionService.getSession(userId).orElse(null);
        if (session != null && session.isAlgoLabSessionValid()) {
            sendMessage(chatId,
                "*✅ AlgoLab Bağlantısı Aktif*\n\n" +
                "AlgoLab hesabınız zaten bağlı.\n\n" +
                "Token: `" + maskToken(session.getAlgoLabToken()) + "`\n" +
                "Geçerlilik: " + formatDateTime(session.getAlgoLabSessionExpires()),
                KeyboardFactory.createBackButton("menu:broker"));
            return;
        }

        // Start connection flow
        log.info("Starting AlgoLab connection for user: {}", userId);
        sessionService.setConversationState(userId, ConversationState.WAITING_ALGOLAB_USERNAME);
        sendMessage(chatId,
            "*🔗 AlgoLab Bağlantısı*\n\n" +
            "AlgoLab hesabınızı bağlamak için aşağıdaki bilgileri girin:\n\n" +
            "📧 *AlgoLab kullanıcı adınızı* girin:",
            null);
    }

    /**
     * Show AlgoLab connection status
     */
    private void showAlgoLabStatus(Long chatId, Long userId) throws TelegramApiException {
        TelegramUserSession session = sessionService.getSession(userId).orElse(null);

        if (session != null && session.isAlgoLabSessionValid()) {
            sendMessage(chatId,
                "*✅ AlgoLab Durumu: Bağlı*\n\n" +
                "Token: `" + maskToken(session.getAlgoLabToken()) + "`\n" +
                "Hash: `" + maskToken(session.getAlgoLabHash()) + "`\n" +
                "Geçerlilik: " + formatDateTime(session.getAlgoLabSessionExpires()) + "\n\n" +
                "Bağlantı aktif ve kullanılabilir.",
                KeyboardFactory.createBackButton("menu:broker"));
        } else {
            sendMessage(chatId,
                "*❌ AlgoLab Durumu: Bağlı Değil*\n\n" +
                "AlgoLab hesabınız bağlı değil.\n\n" +
                "Bağlantı kurmak için 'AlgoLab Bağlantısı' butonuna tıklayın.",
                KeyboardFactory.createBackButton("menu:broker"));
        }
    }

    /**
     * Show user's open positions from AlgoLab
     */
    private void showPositions(Long chatId, Long userId) throws TelegramApiException {
        // Check AlgoLab session
        TelegramUserSession session = sessionService.getSession(userId).orElse(null);
        if (session == null || !session.isAlgoLabSessionValid()) {
            sendMessage(chatId,
                "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                "Önce AlgoLab'a bağlanmalısınız.\n\n" +
                "Broker menüsünden AlgoLab'a bağlanabilirsiniz.",
                KeyboardFactory.createBackButton("menu:broker"));
            return;
        }

        log.debug("Fetching positions from AlgoLab for telegram user: {}", userId);

        try {
            // Get positions from AlgoLab
            Map<String, Object> response = algoLabOrderService.getInstantPosition("");

            // Parse response
            if (response == null || !response.containsKey("content")) {
                sendMessage(chatId,
                    "*📊 Açık Pozisyonlar*\n\n" +
                    "Henüz açık pozisyonunuz bulunmuyor.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            Object content = response.get("content");
            if (!(content instanceof List)) {
                sendMessage(chatId,
                    "*📊 Açık Pozisyonlar*\n\n" +
                    "Henüz açık pozisyonunuz bulunmuyor.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> positions = (List<Map<String, Object>>) content;

            if (positions.isEmpty()) {
                sendMessage(chatId,
                    "*📊 Açık Pozisyonlar*\n\n" +
                    "Henüz açık pozisyonunuz bulunmuyor.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            // Filter out summary row (code="-") and cash positions (TRY)
            log.info("🔍 Total positions received: {}", positions.size());

            List<Map<String, Object>> actualPositions = positions.stream()
                    .filter(pos -> {
                        String code = (String) pos.get("code");
                        String type = (String) pos.get("type");

                        // DEBUG: Log each position to understand filtering
                        log.debug("Position: code={}, type={}", code, type);

                        // Filter out:
                        // 1. Summary row (code="-")
                        // 2. Cash positions (type="CA" or code="TRY")
                        boolean isSummary = "-".equals(code);
                        boolean isCash = "CA".equals(type) || "TRY".equals(code);

                        boolean include = !isSummary && !isCash;

                        if (!include) {
                            log.debug("  -> Filtering out: code={}, type={}", code, type);
                        } else {
                            log.debug("  -> Including: code={}", code);
                        }

                        return include;
                    })
                    .toList();

            log.info("🔍 Filtered positions count: {}", actualPositions.size());

            if (actualPositions.isEmpty()) {
                sendMessage(chatId,
                    "*📊 Açık Pozisyonlar*\n\n" +
                    "Henüz açık pozisyonunuz bulunmuyor.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            // Group positions by symbol (to handle duplicates from different trades)
            Map<String, Map<String, Object>> groupedPositions = new java.util.LinkedHashMap<>();
            for (Map<String, Object> position : actualPositions) {
                String code = (String) position.get("code");

                if (groupedPositions.containsKey(code)) {
                    // Merge with existing position
                    Map<String, Object> existing = groupedPositions.get(code);
                    double existingStock = parseDouble(existing.get("totalstock"));
                    double existingProfit = parseDouble(existing.get("profit"));
                    double existingCost = parseDouble(existing.get("cost"));
                    double existingTotalCost = existingStock * existingCost;

                    double newStock = parseDouble(position.get("totalstock"));
                    double newProfit = parseDouble(position.get("profit"));
                    double newCost = parseDouble(position.get("cost"));
                    double newTotalCost = newStock * newCost;

                    // Sum totalstock and profit
                    double combinedStock = existingStock + newStock;
                    double combinedProfit = existingProfit + newProfit;

                    // Calculate weighted average cost
                    double combinedCost = combinedStock > 0
                        ? (existingTotalCost + newTotalCost) / combinedStock
                        : 0.0;

                    existing.put("totalstock", String.valueOf(combinedStock));
                    existing.put("profit", String.valueOf(combinedProfit));
                    existing.put("cost", String.valueOf(combinedCost));
                    // Keep the latest unitprice (current price)
                    existing.put("unitprice", position.get("unitprice"));
                } else {
                    // First occurrence of this symbol
                    groupedPositions.put(code, new java.util.HashMap<>(position));
                }
            }

            log.info("🔍 Grouped positions count: {}", groupedPositions.size());

            // Build positions message and collect symbols for action buttons
            StringBuilder message = new StringBuilder("*📊 Açık Pozisyonlar*\n\n");
            message.append(String.format("Toplam: %d sembol\n\n", groupedPositions.size()));

            List<String> symbols = new java.util.ArrayList<>();
            for (Map<String, Object> position : groupedPositions.values()) {
                String symbol = (String) position.get("code");
                symbols.add(symbol);
                message.append(formatAlgoLabPosition(position));
                message.append("\n");
            }

            message.append("💡 *İpucu:* Aşağıdaki butonlardan hızlıca alım/satım yapabilirsiniz!");

            // Create keyboard with buy/sell buttons for each position
            sendMessage(chatId, message.toString(), KeyboardFactory.createPositionsKeyboard(symbols));

        } catch (Exception e) {
            log.error("Error fetching positions from AlgoLab for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Hata*\n\n" +
                "Pozisyonlar getirilirken bir hata oluştu.\n\n" +
                "Detay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:broker"));
        }
    }

    /**
     * Format a single AlgoLab position for display
     */
    private String formatAlgoLabPosition(Map<String, Object> position) {
        StringBuilder sb = new StringBuilder();

        // Extract position data using AlgoLab API field names
        String symbol = (String) position.getOrDefault("code", "N/A");
        double totalstock = parseDouble(position.get("totalstock"));
        double cost = parseDouble(position.get("cost"));
        double unitprice = parseDouble(position.get("unitprice"));
        double profit = parseDouble(position.get("profit"));

        // Calculate profit percentage
        double profitPercent = 0.0;
        if (cost > 0 && totalstock > 0) {
            double totalCost = cost * totalstock;
            profitPercent = (profit / totalCost) * 100.0;
        }

        // Format: Symbol | Miktar | Ort. Fiyat | Son Fiyat | Kar/Zarar | Kar/Zarar %
        sb.append(String.format("*%s*\n", symbol));
        sb.append(String.format("Miktar: %d adet\n", (int) totalstock));
        sb.append(String.format("Ort. Fiyat: ₺%.2f\n", cost));
        sb.append(String.format("Son Fiyat: ₺%.2f\n", unitprice));

        String emoji = profit >= 0 ? "🟢" : "🔴";
        String plFormatted = profit >= 0 ? String.format("+₺%.2f", profit) : String.format("₺%.2f", profit);
        String percentFormatted = profit >= 0 ? String.format("+%.2f%%", profitPercent) : String.format("%.2f%%", profitPercent);

        sb.append(String.format("Kar/Zarar: %s %s (%s)\n", emoji, plFormatted, percentFormatted));
        sb.append("━━━━━━━━━━━━━━━━━\n");

        return sb.toString();
    }

    /**
     * Helper method to safely parse double from Object (handles both Number and String)
     */
    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                String str = ((String) obj).trim();
                if (str.isEmpty()) return 0.0;
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse double from string: {}", obj);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Format P&L with color
     */
    private String formatPnl(BigDecimal pnl) {
        if (pnl == null) {
            return "₺0.00";
        }

        String sign = pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return String.format("%s₺%.2f", sign, pnl);
    }

    /**
     * Handle AlgoLab conversation input
     */
    private void handleAlgoLabConversationInput(Long chatId, Long userId, String text, ConversationState state)
            throws TelegramApiException {

        try {
            switch (state) {
                case WAITING_ALGOLAB_USERNAME -> {
                    log.debug("Received AlgoLab username from user: {}", userId);
                    sessionService.setTempData(userId, "algolab_username", text);
                    sessionService.setConversationState(userId, ConversationState.WAITING_ALGOLAB_PASSWORD);
                    sendMessage(chatId,
                        "*🔑 AlgoLab Şifrenizi* girin:\n\n" +
                        "⚠️ Şifreniz güvenli bir şekilde işlenecektir.",
                        null);
                }

                case WAITING_ALGOLAB_PASSWORD -> {
                    log.debug("Received AlgoLab password from user: {}", userId);
                    String username = sessionService.getTempData(userId, "algolab_username");

                    if (username == null) {
                        sendMessage(chatId, "❌ Kullanıcı adı bulunamadı. Lütfen tekrar başlayın.", null);
                        sessionService.clearConversationState(userId);
                        sessionService.clearAllTempData(userId);
                        return;
                    }

                    // Call AlgoLabAuthService.loginUser()
                    sendMessage(chatId, "🔄 AlgoLab'a bağlanılıyor...", null);

                    try {
                        String token = algoLabAuthService.loginUser(username, text);
                        sessionService.setTempData(userId, "algolab_token", token);
                        sessionService.setConversationState(userId, ConversationState.WAITING_ALGOLAB_OTP);

                        sendMessage(chatId,
                            "*✅ SMS Kodu Gönderildi*\n\n" +
                            "Telefonunuza gelen *SMS kodunu* girin:",
                            null);

                    } catch (AlgoLabAuthenticationException e) {
                        log.error("AlgoLab login failed for user {}: {}", userId, e.getMessage());
                        sendMessage(chatId,
                            "❌ *Bağlantı Hatası*\n\n" +
                            "Kullanıcı adı veya şifre hatalı.\n\n" +
                            "Detay: " + e.getMessage(),
                            KeyboardFactory.createBackButton("menu:broker"));
                        sessionService.clearConversationState(userId);
                        sessionService.clearAllTempData(userId);
                    }
                }

                case WAITING_ALGOLAB_OTP -> {
                    log.debug("Received AlgoLab OTP from user: {}", userId);
                    String smsCode = text.trim();

                    // Call AlgoLabAuthService.loginUserControl()
                    sendMessage(chatId, "🔄 SMS kodu doğrulanıyor...", null);

                    try {
                        String hash = algoLabAuthService.loginUserControl(smsCode);
                        String token = sessionService.getTempData(userId, "algolab_token");

                        if (token == null) {
                            sendMessage(chatId, "❌ Token bulunamadı. Lütfen tekrar başlayın.", null);
                            sessionService.clearConversationState(userId);
                            sessionService.clearAllTempData(userId);
                            return;
                        }

                        // Save to session
                        TelegramUserSession session = sessionService.getSession(userId).orElse(null);
                        if (session != null) {
                            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
                            session.setAlgoLabSession(token, hash, expiresAt);
                            sessionService.saveSession(session);
                        }

                        // Clear conversation state
                        sessionService.clearConversationState(userId);
                        sessionService.clearAllTempData(userId);

                        sendMessage(chatId,
                            "*✅ AlgoLab Bağlantısı Başarılı*\n\n" +
                            "Hesabınız başarıyla bağlandı.\n\n" +
                            "Token: `" + maskToken(token) + "`\n" +
                            "Geçerlilik: 24 saat",
                            KeyboardFactory.createBackButton("menu:broker"));

                    } catch (AlgoLabAuthenticationException e) {
                        log.error("AlgoLab OTP verification failed for user {}: {}", userId, e.getMessage());
                        sendMessage(chatId,
                            "❌ *Doğrulama Hatası*\n\n" +
                            "SMS kodu hatalı veya süresi dolmuş.\n\n" +
                            "Detay: " + e.getMessage(),
                            KeyboardFactory.createBackButton("menu:broker"));
                        sessionService.clearConversationState(userId);
                        sessionService.clearAllTempData(userId);
                    }
                }

                default -> {
                    log.warn("Unexpected conversation state: {}", state);
                    sessionService.clearConversationState(userId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling AlgoLab conversation for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Bir hata oluştu*\n\n" + e.getMessage(),
                KeyboardFactory.createBackButton("menu:broker"));
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);
        }
    }

    /**
     * Mask sensitive token for display
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    /**
     * Format LocalDateTime for display
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
