package com.bisttrading.telegram.handler;

import com.bisttrading.entity.trading.Order;
import com.bisttrading.entity.trading.enums.OrderSide;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.entity.trading.enums.OrderType;
import com.bisttrading.entity.trading.enums.TimeInForce;
import com.bisttrading.repository.trading.OrderRepository;
import com.bisttrading.symbol.dto.SymbolDto;
import com.bisttrading.symbol.service.SymbolService;
import com.bisttrading.telegram.dto.ConversationState;
import com.bisttrading.telegram.dto.TelegramUserSession;
import com.bisttrading.telegram.keyboard.KeyboardFactory;
import com.bisttrading.telegram.service.TelegramSessionService;
import com.bisttrading.trading.dto.CreateOrderRequest;
import com.bisttrading.trading.service.OrderManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for order-related actions.
 * Handles pending orders, order history, order actions, etc.
 */
@Slf4j
@Component
public class OrderCommandHandler extends BaseCommandHandler {

    private final OrderRepository orderRepository;
    private final OrderManagementService orderManagementService;
    private final SymbolService symbolService;
    private final com.bisttrading.broker.algolab.service.AlgoLabOrderService algoLabOrderService;
    private final com.bisttrading.broker.algolab.service.AlgoLabAuthService algoLabAuthService;
    private final com.bisttrading.telegram.service.MarketDataService marketDataService;
    private final com.bisttrading.telegram.service.OrderCalculationService orderCalculationService;

    public OrderCommandHandler(
            TelegramClient telegramClient,
            TelegramSessionService sessionService,
            OrderRepository orderRepository,
            OrderManagementService orderManagementService,
            SymbolService symbolService,
            com.bisttrading.broker.algolab.service.AlgoLabOrderService algoLabOrderService,
            com.bisttrading.broker.algolab.service.AlgoLabAuthService algoLabAuthService,
            com.bisttrading.telegram.service.MarketDataService marketDataService,
            com.bisttrading.telegram.service.OrderCalculationService orderCalculationService) {
        super(telegramClient, sessionService);
        this.orderRepository = orderRepository;
        this.orderManagementService = orderManagementService;
        this.symbolService = symbolService;
        this.algoLabOrderService = algoLabOrderService;
        this.algoLabAuthService = algoLabAuthService;
        this.marketDataService = marketDataService;
        this.orderCalculationService = orderCalculationService;
    }

    @Override
    public String getCommand() {
        return "orders";
    }

    @Override
    public String getDescription() {
        return "Emirler";
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

            // Handle order creation conversation states
            if (state == ConversationState.WAITING_ORDER_SYMBOL ||
                state == ConversationState.WAITING_ORDER_SIDE ||
                state == ConversationState.WAITING_ORDER_PRICE_TYPE ||
                state == ConversationState.WAITING_ORDER_QUANTITY ||
                state == ConversationState.WAITING_ORDER_PRICE) {
                handleOrderCreationInput(chatId, userId, text, state);
                return;
            }

            // Handle order modification conversation states
            if (state == ConversationState.WAITING_MODIFY_PRICE ||
                state == ConversationState.WAITING_MODIFY_QUANTITY) {
                handleOrderModificationInput(chatId, userId, text, state);
                return;
            }
        }

        // Parse callback data
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] parts = callbackData.split(":");

            if (parts.length > 1) {
                String action = parts[1];
                handleOrderAction(chatId, userId, action, parts);
                return;
            }
        }

        // Default: show orders menu
        sendMessage(chatId,
            "*📋 Emirler*\n\nEmir işlemleri için aşağıdaki seçenekleri kullanabilirsiniz:",
            KeyboardFactory.createBackButton("menu:main"));
    }

    private void handleOrderAction(Long chatId, Long userId, String action, String[] parts) throws TelegramApiException {
        log.debug("Handling order action: {} for user: {}", action, userId);

        switch (action) {
            case "pending" -> showPendingOrders(chatId, userId);
            case "create" -> startOrderCreation(chatId, userId);
            case "side" -> {
                // Handle order side selection from keyboard button
                if (parts.length > 2) {
                    String side = parts[2]; // "BUY" or "SELL"
                    handleOrderCreationInput(chatId, userId, side, ConversationState.WAITING_ORDER_SIDE);
                }
            }
            case "type" -> {
                // Handle order type selection from keyboard button
                if (parts.length > 2) {
                    String type = parts[2]; // "MARKET" or "LIMIT"
                    handleOrderCreationInput(chatId, userId, type, ConversationState.WAITING_ORDER_PRICE_TYPE);
                }
            }
            case "modify" -> {
                if (parts.length > 2) {
                    String orderId = parts[2];
                    startOrderModification(chatId, userId, orderId);
                }
            }
            case "cancel" -> {
                if (parts.length > 2) {
                    String orderId = parts[2];
                    showCancelConfirmation(chatId, userId, orderId);
                }
            }
            case "cancel_confirm" -> {
                if (parts.length > 2) {
                    String orderId = parts[2];
                    cancelOrder(chatId, userId, orderId);
                }
            }
            case "history" -> {
                sendMessage(chatId,
                    "*📜 Emir Geçmişi*\n\nBu özellik yakında eklenecek.",
                    KeyboardFactory.createBackButton("menu:main"));
            }
            default -> {
                log.warn("Unknown order action: {}", action);
                sendMessage(chatId,
                    "Bilinmeyen işlem.",
                    KeyboardFactory.createBackButton("menu:main"));
            }
        }
    }

    /**
     * Start order creation flow
     */
    private void startOrderCreation(Long chatId, Long userId) throws TelegramApiException {
        // Check if user is logged in
        TelegramUserSession session = sessionService.getSession(userId).orElse(null);
        if (session == null || !session.isLoggedIn()) {
            sendMessage(chatId,
                "❌ *Oturum Bulunamadı*\n\n" +
                "Önce platforma giriş yapmalısınız.",
                KeyboardFactory.createBackButton("menu:main"));
            return;
        }

        // Start conversation
        log.info("Starting order creation for user: {}", userId);
        sessionService.setConversationState(userId, ConversationState.WAITING_ORDER_SYMBOL);
        sessionService.clearAllTempData(userId); // Clear any previous order data

        sendMessage(chatId,
            "*📝 Yeni Emir Oluştur*\n\n" +
            "🏷️ *Sembol kodunu* girin (örnek: AKBNK, GARAN, THYAO):",
            null);
    }

    /**
     * Handle order creation conversation input
     */
    private void handleOrderCreationInput(Long chatId, Long userId, String text, ConversationState state)
            throws TelegramApiException {

        try {
            switch (state) {
                case WAITING_ORDER_SYMBOL -> {
                    log.debug("Received symbol from user: {}", userId);
                    String symbolCode = text.trim().toUpperCase();

                    // For AlgoLab integration, we'll validate symbol with AlgoLab API instead of local DB
                    // This allows any symbol that AlgoLab supports, even if not in our local database
                    log.info("Symbol {} will be validated by AlgoLab API during order submission", symbolCode);

                    // Save symbol and move to next step
                    sessionService.setTempData(userId, "order_symbol", symbolCode);
                    sessionService.setConversationState(userId, ConversationState.WAITING_ORDER_SIDE);

                    sendMessage(chatId,
                        "*📝 Yeni Emir Oluştur*\n\n" +
                        "Sembol: *" + symbolCode + "*\n\n" +
                        "📊 *İşlem yönünü* seçin:",
                        KeyboardFactory.createOrderSideKeyboard());
                }

                case WAITING_ORDER_SIDE -> {
                    log.debug("Received order side from user: {}", userId);
                    String sideText = text.trim().toUpperCase();

                    // Parse order side
                    OrderSide side;
                    if (sideText.equals("BUY") || sideText.equals("ALIS") || sideText.equals("AL")) {
                        side = OrderSide.BUY;
                    } else if (sideText.equals("SELL") || sideText.equals("SATIS") || sideText.equals("SAT")) {
                        side = OrderSide.SELL;
                    } else {
                        sendMessage(chatId,
                            "❌ *Geçersiz İşlem Yönü*\n\n" +
                            "Lütfen BUY (Alış) veya SELL (Satış) girin:",
                            null);
                        return;
                    }

                    // Save side and move to next step
                    sessionService.setTempData(userId, "order_side", side.name());
                    sessionService.setConversationState(userId, ConversationState.WAITING_ORDER_PRICE_TYPE);

                    String symbol = sessionService.getTempData(userId, "order_symbol");
                    String sideEmoji = side == OrderSide.BUY ? "🟢" : "🔴";

                    sendMessage(chatId,
                        "*📝 Yeni Emir Oluştur*\n\n" +
                        "Sembol: *" + symbol + "*\n" +
                        "İşlem: " + sideEmoji + " *" + side + "*\n\n" +
                        "💰 *Emir tipini* seçin:",
                        KeyboardFactory.createOrderTypeKeyboard());
                }

                case WAITING_ORDER_PRICE_TYPE -> {
                    log.debug("Received order type from user: {}", userId);
                    String typeText = text.trim().toUpperCase();

                    // Parse order type
                    OrderType orderType;
                    if (typeText.equals("MARKET") || typeText.equals("PIYASA")) {
                        orderType = OrderType.MARKET;
                    } else if (typeText.equals("LIMIT")) {
                        orderType = OrderType.LIMIT;
                    } else {
                        sendMessage(chatId,
                            "❌ *Geçersiz Emir Tipi*\n\n" +
                            "Lütfen MARKET veya LIMIT girin:",
                            null);
                        return;
                    }

                    // Save order type and move to next step
                    sessionService.setTempData(userId, "order_type", orderType.name());
                    sessionService.setConversationState(userId, ConversationState.WAITING_ORDER_QUANTITY);

                    // Check if this is a quick order (from position)
                    String quickSymbol = sessionService.getTempData(userId, "quick_order_symbol");
                    String quickSide = sessionService.getTempData(userId, "quick_order_side");

                    String symbol;
                    String sideStr;
                    if (quickSymbol != null && quickSide != null) {
                        // Quick order flow - use temp data and convert to order format
                        symbol = quickSymbol;
                        sideStr = "buy".equalsIgnoreCase(quickSide) ? "BUY" : "SELL";

                        // Save to standard order fields
                        sessionService.setTempData(userId, "order_symbol", symbol);
                        sessionService.setTempData(userId, "order_side", sideStr);

                        // Clear quick order temp data by setting to null
                        sessionService.setTempData(userId, "quick_order_symbol", null);
                        sessionService.setTempData(userId, "quick_order_side", null);
                    } else {
                        // Normal order flow
                        symbol = sessionService.getTempData(userId, "order_symbol");
                        sideStr = sessionService.getTempData(userId, "order_side");
                    }

                    String sideEmoji = sideStr.equals("BUY") ? "🟢" : "🔴";

                    sendMessage(chatId,
                        "*📝 Yeni Emir Oluştur*\n\n" +
                        "Sembol: *" + symbol + "*\n" +
                        "İşlem: " + sideEmoji + " *" + sideStr + "*\n" +
                        "Tip: *" + orderType + "*\n\n" +
                        "📦 *Kaç adet* almak/satmak istiyorsunuz?",
                        null);
                }

                case WAITING_ORDER_QUANTITY -> {
                    log.debug("Received quantity from user: {}", userId);

                    // Parse quantity
                    Integer quantity;
                    try {
                        quantity = Integer.parseInt(text.trim());
                        if (quantity <= 0) {
                            sendMessage(chatId,
                                "❌ *Geçersiz Miktar*\n\n" +
                                "Miktar pozitif bir sayı olmalıdır.\n\n" +
                                "Lütfen geçerli bir miktar girin:",
                                null);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId,
                            "❌ *Geçersiz Format*\n\n" +
                            "Lütfen sayısal bir değer girin (örnek: 1000):",
                            null);
                        return;
                    }

                    // Save quantity
                    sessionService.setTempData(userId, "order_quantity", quantity.toString());

                    String orderTypeStr = sessionService.getTempData(userId, "order_type");
                    OrderType orderType = OrderType.valueOf(orderTypeStr);

                    if (orderType == OrderType.LIMIT) {
                        // Ask for price
                        sessionService.setConversationState(userId, ConversationState.WAITING_ORDER_PRICE);

                        String symbol = sessionService.getTempData(userId, "order_symbol");
                        String sideStr = sessionService.getTempData(userId, "order_side");
                        String sideEmoji = sideStr.equals("BUY") ? "🟢" : "🔴";

                        sendMessage(chatId,
                            "*📝 Yeni Emir Oluştur*\n\n" +
                            "Sembol: *" + symbol + "*\n" +
                            "İşlem: " + sideEmoji + " *" + sideStr + "*\n" +
                            "Tip: *" + orderType + "*\n" +
                            "Miktar: *" + quantity + "*\n\n" +
                            "💵 *Limit fiyatını* girin (örnek: 15.75):",
                            null);
                    } else {
                        // MARKET order - submit immediately
                        submitOrder(chatId, userId, null);
                    }
                }

                case WAITING_ORDER_PRICE -> {
                    log.debug("Received price from user: {}", userId);

                    // Parse price
                    BigDecimal price;
                    try {
                        price = new BigDecimal(text.trim());
                        if (price.compareTo(BigDecimal.ZERO) <= 0) {
                            sendMessage(chatId,
                                "❌ *Geçersiz Fiyat*\n\n" +
                                "Fiyat pozitif bir sayı olmalıdır.\n\n" +
                                "Lütfen geçerli bir fiyat girin:",
                                null);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId,
                            "❌ *Geçersiz Format*\n\n" +
                            "Lütfen sayısal bir değer girin (örnek: 15.75):",
                            null);
                        return;
                    }

                    // Submit order with price
                    submitOrder(chatId, userId, price);
                }

                default -> {
                    log.warn("Unexpected conversation state: {}", state);
                    sessionService.clearConversationState(userId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling order creation for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Bir hata oluştu*\n\n" + e.getMessage(),
                KeyboardFactory.createBackButton("menu:main"));
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);
        }
    }

    /**
     * Submit order to AlgoLab API
     */
    private void submitOrder(Long chatId, Long userId, BigDecimal price) throws TelegramApiException {
        try {
            // Get session and check AlgoLab connection
            TelegramUserSession session = sessionService.getSession(userId).orElse(null);
            if (session == null || !session.isAlgoLabSessionValid()) {
                sendMessage(chatId,
                    "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                    "Önce AlgoLab'a bağlanmalısınız.\n\n" +
                    "Broker menüsünden AlgoLab'a bağlanabilirsiniz.",
                    KeyboardFactory.createBackButton("menu:broker"));
                sessionService.clearConversationState(userId);
                sessionService.clearAllTempData(userId);
                return;
            }

            // Get order data from temp storage
            String symbol = sessionService.getTempData(userId, "order_symbol");
            String sideStr = sessionService.getTempData(userId, "order_side");
            String orderTypeStr = sessionService.getTempData(userId, "order_type");
            String quantityStr = sessionService.getTempData(userId, "order_quantity");

            // Parse values
            OrderSide side = OrderSide.valueOf(sideStr);
            OrderType orderType = OrderType.valueOf(orderTypeStr);
            int quantity = Integer.parseInt(quantityStr); // User input is in adet (shares)

            // Convert to AlgoLab format
            String direction = side == OrderSide.BUY ? "BUY" : "SELL";
            String priceType = orderType == OrderType.MARKET ? "P" : "L"; // P=piyasa, L=limit

            // Submit order to AlgoLab
            sendMessage(chatId, "🔄 Emir AlgoLab'a gönderiliyor...", null);

            Map<String, Object> response = algoLabOrderService.sendOrder(
                symbol,
                direction,
                priceType,
                price,
                quantity,
                false, // sms
                false, // email
                ""     // subAccount (empty = use active account)
            );

            // Clear conversation state
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);

            // Extract order details from response
            String orderId = response.get("orderId") != null ? response.get("orderId").toString() : "N/A";
            boolean success = response.get("success") != null && (boolean) response.get("success");

            // Send confirmation
            String sideEmoji = side == OrderSide.BUY ? "🟢" : "🔴";
            StringBuilder confirmation = new StringBuilder();

            if (success) {
                confirmation.append("*✅ Emir Gönderildi*\n\n");
            } else {
                confirmation.append("*⚠️ Emir Durumu Belirsiz*\n\n");
            }

            confirmation.append(String.format("%s *%s %s*\n", sideEmoji, side, symbol));
            confirmation.append(String.format("Tip: %s\n", orderType));
            confirmation.append(String.format("Miktar: %d adet\n", quantity));
            if (price != null && orderType == OrderType.LIMIT) {
                confirmation.append(String.format("Fiyat: ₺%.2f\n", price));

                // Calculate and show order cost estimate
                try {
                    com.bisttrading.telegram.service.OrderCalculationService.OrderEstimate estimate =
                        orderCalculationService.calculateOrderCost(quantity, price, side == OrderSide.BUY);
                    confirmation.append("\n");
                    confirmation.append(orderCalculationService.formatEstimate(estimate));
                } catch (Exception e) {
                    log.warn("Failed to calculate order cost: {}", e.getMessage());
                }
            }
            if (!"N/A".equals(orderId)) {
                confirmation.append(String.format("\nEmir ID: `%s`", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
            }

            sendMessage(chatId, confirmation.toString(), KeyboardFactory.createBackButton("menu:orders"));

        } catch (Exception e) {
            log.error("Error submitting order to AlgoLab for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Emir Gönderilemedi*\n\n" +
                "Emir AlgoLab'a gönderilirken bir hata oluştu.\n\n" +
                "Detay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);
        }
    }

    /**
     * Show cancel confirmation for an AlgoLab order
     */
    private void showCancelConfirmation(Long chatId, Long userId, String orderId) throws TelegramApiException {
        try {
            // Check AlgoLab session
            TelegramUserSession session = sessionService.getSession(userId).orElse(null);
            if (session == null || !session.isAlgoLabSessionValid()) {
                sendMessage(chatId,
                    "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                    "Önce AlgoLab'a bağlanmalısınız.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            // Build confirmation message (generic, without fetching order details)
            StringBuilder message = new StringBuilder("*⚠️ Emir İptali Onayı*\n\n");
            message.append("Bu emri iptal etmek istediğinizden emin misiniz?\n\n");
            message.append(String.format("Emir ID: `%s`\n\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
            message.append("⚠️ *Dikkat:* Bu işlem geri alınamaz.");

            // Create confirmation keyboard
            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("✅ Evet, İptal Et")
                        .callbackData("orders:cancel_confirm:" + orderId)
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("❌ Hayır")
                        .callbackData("orders:pending")
                        .build()
                ))
                .build();

            sendMessage(chatId, message.toString(), keyboard);

        } catch (Exception e) {
            log.error("Error showing cancel confirmation for order {}: {}", orderId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Hata*\n\nİptal onayı gösterilemedi.\n\nDetay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
        }
    }

    /**
     * Cancel an order via AlgoLab API
     */
    private void cancelOrder(Long chatId, Long userId, String orderId) throws TelegramApiException {
        try {
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

            // Cancel order via AlgoLab
            sendMessage(chatId, "🔄 Emir AlgoLab'da iptal ediliyor...", null);

            Map<String, Object> response = algoLabOrderService.deleteOrder(
                orderId,
                "" // subAccount (empty = use active account)
            );

            // Extract response details
            boolean success = response.get("success") != null && (boolean) response.get("success");
            String message = response.get("message") != null ? response.get("message").toString() : "";

            // Send confirmation
            StringBuilder confirmation = new StringBuilder();

            if (success) {
                confirmation.append("*✅ Emir İptal Edildi*\n\n");
                confirmation.append(String.format("Emir ID: `%s`\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
                if (!message.isEmpty()) {
                    confirmation.append(String.format("\n%s", message));
                }
            } else {
                confirmation.append("*⚠️ Emir İptal Durumu Belirsiz*\n\n");
                confirmation.append(String.format("Emir ID: `%s`\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
                if (!message.isEmpty()) {
                    confirmation.append(String.format("\nDetay: %s", message));
                }
            }

            sendMessage(chatId, confirmation.toString(), KeyboardFactory.createBackButton("menu:orders"));

        } catch (Exception e) {
            log.error("Error cancelling order {} via AlgoLab for user {}: {}", orderId, userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Emir İptal Edilemedi*\n\n" +
                "Emir iptal edilirken bir hata oluştu.\n\n" +
                "Detay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
        }
    }

    /**
     * Start order modification flow for AlgoLab order
     */
    private void startOrderModification(Long chatId, Long userId, String orderId) throws TelegramApiException {
        try {
            // Check AlgoLab session
            TelegramUserSession session = sessionService.getSession(userId).orElse(null);
            if (session == null || !session.isAlgoLabSessionValid()) {
                sendMessage(chatId,
                    "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                    "Önce AlgoLab'a bağlanmalısınız.",
                    KeyboardFactory.createBackButton("menu:broker"));
                return;
            }

            // Start modification flow (without fetching order details from DB)
            log.info("Starting AlgoLab order modification for order: {} by user: {}", orderId, userId);
            sessionService.setTempData(userId, "modify_order_id", orderId);
            sessionService.setConversationState(userId, ConversationState.WAITING_MODIFY_PRICE);

            // Ask for new price (simplified - no current order details)
            StringBuilder message = new StringBuilder("*✏️ Emir Düzenle*\n\n");
            message.append(String.format("Emir ID: `%s`\n\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
            message.append("💵 *Yeni fiyat* girin (örnek: 15.75)\n\n");
            message.append("_Not: Fiyatı değiştirmek istemiyorsanız 'atla' yazın._");

            sendMessage(chatId, message.toString(), null);

        } catch (Exception e) {
            log.error("Error starting AlgoLab order modification for order {}: {}", orderId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Hata*\n\nEmir düzenleme başlatılamadı.\n\nDetay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
        }
    }

    /**
     * Handle AlgoLab order modification conversation input
     */
    private void handleOrderModificationInput(Long chatId, Long userId, String text, ConversationState state)
            throws TelegramApiException {

        try {
            String orderId = sessionService.getTempData(userId, "modify_order_id");
            if (orderId == null) {
                sendMessage(chatId,
                    "❌ *Hata*\n\nEmir bilgisi bulunamadı. Lütfen tekrar başlayın.",
                    KeyboardFactory.createBackButton("menu:orders"));
                sessionService.clearConversationState(userId);
                return;
            }

            switch (state) {
                case WAITING_MODIFY_PRICE -> {
                    log.debug("Received new price from user: {}", userId);

                    // Check if user wants to skip
                    if (text.trim().equalsIgnoreCase("atla") || text.trim().equalsIgnoreCase("skip")) {
                        // Skip price, move to quantity
                        sessionService.setConversationState(userId, ConversationState.WAITING_MODIFY_QUANTITY);

                        sendMessage(chatId,
                            "*✏️ Emir Düzenle*\n\n📦 *Yeni miktar* girin (adet cinsinden, örnek: 1000)\n\n" +
                                "_Not: Miktarı değiştirmek istemiyorsanız 'atla' yazın._",
                            null);
                        return;
                    }

                    // Parse price
                    BigDecimal newPrice;
                    try {
                        newPrice = new BigDecimal(text.trim());
                        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                            sendMessage(chatId,
                                "❌ *Geçersiz Fiyat*\n\nFiyat pozitif bir sayı olmalıdır.\n\n" +
                                "Lütfen geçerli bir fiyat girin veya 'atla' yazın:",
                                null);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId,
                            "❌ *Geçersiz Format*\n\nLütfen sayısal bir değer girin (örnek: 15.75) veya 'atla' yazın:",
                            null);
                        return;
                    }

                    // Save new price and move to quantity
                    sessionService.setTempData(userId, "modify_new_price", newPrice.toString());
                    sessionService.setConversationState(userId, ConversationState.WAITING_MODIFY_QUANTITY);

                    sendMessage(chatId,
                        "*✏️ Emir Düzenle*\n\n📦 *Yeni miktar* girin (adet cinsinden, örnek: 1000)\n\n" +
                            "_Not: Miktarı değiştirmek istemiyorsanız 'atla' yazın._",
                        null);
                }

                case WAITING_MODIFY_QUANTITY -> {
                    log.debug("Received new quantity from user: {}", userId);

                    Integer newQuantity = null;

                    // Check if user wants to skip
                    if (!text.trim().equalsIgnoreCase("atla") && !text.trim().equalsIgnoreCase("skip")) {
                        // Parse quantity
                        try {
                            newQuantity = Integer.parseInt(text.trim());
                            if (newQuantity <= 0) {
                                sendMessage(chatId,
                                    "❌ *Geçersiz Miktar*\n\nMiktar pozitif bir sayı olmalıdır.\n\n" +
                                    "Lütfen geçerli bir miktar girin veya 'atla' yazın:",
                                    null);
                                return;
                            }
                        } catch (NumberFormatException e) {
                            sendMessage(chatId,
                                "❌ *Geçersiz Format*\n\nLütfen sayısal bir değer girin (örnek: 1000) veya 'atla' yazın:",
                                null);
                            return;
                        }

                        sessionService.setTempData(userId, "modify_new_quantity", newQuantity.toString());
                    }

                    // Submit modification (pass null for originalOrder since we don't have it)
                    modifyOrderSubmit(chatId, userId, orderId, null);
                }

                default -> {
                    log.warn("Unexpected conversation state: {}", state);
                    sessionService.clearConversationState(userId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling AlgoLab order modification for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Bir hata oluştu*\n\n" + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);
        }
    }

    /**
     * Submit order modification to AlgoLab API
     */
    private void modifyOrderSubmit(Long chatId, Long userId, String orderId, Order originalOrder)
            throws TelegramApiException {
        try {
            // Check AlgoLab session
            TelegramUserSession session = sessionService.getSession(userId).orElse(null);
            if (session == null || !session.isAlgoLabSessionValid()) {
                sendMessage(chatId,
                    "❌ *AlgoLab Bağlantısı Yok*\n\n" +
                    "Önce AlgoLab'a bağlanmalısınız.",
                    KeyboardFactory.createBackButton("menu:broker"));
                sessionService.clearConversationState(userId);
                sessionService.clearAllTempData(userId);
                return;
            }

            // Get new values from temp storage
            String newPriceStr = sessionService.getTempData(userId, "modify_new_price");
            String newQuantityStr = sessionService.getTempData(userId, "modify_new_quantity");

            BigDecimal newPrice = newPriceStr != null ? new BigDecimal(newPriceStr) : null;
            Integer newQuantity = newQuantityStr != null ? Integer.parseInt(newQuantityStr) : null;

            // Check if anything changed
            if (newPrice == null && newQuantity == null) {
                sendMessage(chatId,
                    "⚠️ *Değişiklik Yok*\n\nHiçbir değer değiştirilmedi.",
                    KeyboardFactory.createBackButton("menu:orders"));
                sessionService.clearConversationState(userId);
                sessionService.clearAllTempData(userId);
                return;
            }

            // Use quantity directly (no conversion needed - adet = lot in equity market)
            Integer newQuantityForApi = newQuantity;

            // Modify order via AlgoLab
            sendMessage(chatId, "🔄 Emir AlgoLab'da güncelleniyor...", null);

            Map<String, Object> response = algoLabOrderService.modifyOrder(
                orderId,
                newPrice,
                newQuantityForApi,
                false, // viop
                ""     // subAccount (empty = use active account)
            );

            // Clear conversation state
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);

            // Extract response details
            boolean success = response.get("success") != null && (boolean) response.get("success");
            String message = response.get("message") != null ? response.get("message").toString() : "";

            // Send confirmation
            StringBuilder confirmation = new StringBuilder();

            if (success) {
                confirmation.append("*✅ Emir Güncellendi*\n\n");
            } else {
                confirmation.append("*⚠️ Emir Güncelleme Durumu Belirsiz*\n\n");
            }

            confirmation.append(String.format("Emir ID: `%s`\n\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));

            if (newPrice != null) {
                confirmation.append(String.format("Yeni Fiyat: ₺%.2f\n", newPrice));
            }
            if (newQuantity != null) {
                confirmation.append(String.format("Yeni Miktar: %d adet\n", newQuantity));
            }

            if (!message.isEmpty()) {
                confirmation.append(String.format("\n%s", message));
            }

            sendMessage(chatId, confirmation.toString(), KeyboardFactory.createBackButton("menu:orders"));

        } catch (Exception e) {
            log.error("Error modifying order {} via AlgoLab for user {}: {}", orderId, userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Emir Güncellenemedi*\n\n" +
                "Emir güncellenirken bir hata oluştu.\n\n" +
                "Detay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
            sessionService.clearConversationState(userId);
            sessionService.clearAllTempData(userId);
        }
    }

    /**
     * Show user's pending orders from AlgoLab
     */
    private void showPendingOrders(Long chatId, Long userId) throws TelegramApiException {
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

        log.debug("Fetching pending orders from AlgoLab for telegram user: {}", userId);

        try {
            // Get pending orders from AlgoLab API
            List<Map<String, Object>> pendingOrders = algoLabOrderService.getPendingOrders("");

            if (pendingOrders == null || pendingOrders.isEmpty()) {
                sendMessage(chatId,
                    "*📋 Bekleyen Emirler*\n\n" +
                    "Henüz bekleyen emriniz bulunmuyor.",
                    KeyboardFactory.createBackButton("menu:orders"));
                return;
            }

            // Build orders message
            StringBuilder message = new StringBuilder("*📋 Bekleyen Emirler*\n\n");
            message.append(String.format("Toplam: %d emir\n\n", pendingOrders.size()));

            for (int i = 0; i < pendingOrders.size(); i++) {
                Map<String, Object> order = pendingOrders.get(i);
                message.append(String.format("#%d\n", i + 1));
                message.append(formatAlgoLabOrder(order));
                message.append("\n");
            }

            message.append("\n💡 *İpucu:* Emir iptal/düzenleme için aşağıdaki butonları kullanın.");

            // Create keyboard with cancel/modify buttons - use AlgoLab order IDs
            sendMessage(chatId, message.toString(),
                createAlgoLabOrderListKeyboard(pendingOrders));

        } catch (Exception e) {
            log.error("Error fetching pending orders from AlgoLab for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId,
                "❌ *Hata*\n\n" +
                "Bekleyen emirler getirilirken bir hata oluştu.\n\n" +
                "Detay: " + e.getMessage(),
                KeyboardFactory.createBackButton("menu:orders"));
        }
    }

    /**
     * Format a single order for display (local database)
     */
    private String formatOrder(Order order) {
        StringBuilder sb = new StringBuilder();

        // Symbol and side
        String symbolCode = order.getSymbol() != null ? order.getSymbol().getSymbol() : "N/A";
        String sideEmoji = order.getOrderSide().name().equals("BUY") ? "🟢" : "🔴";
        sb.append(String.format("*%s %s %s*\n", sideEmoji, order.getOrderSide(), symbolCode));

        // Order type and status
        sb.append(String.format("Tip: %s | Durum: %s\n",
            order.getOrderType(),
            order.getOrderStatus().getTurkishDescription()));

        // Quantity information
        sb.append(String.format("Miktar: %d", order.getQuantity()));
        if (order.getFilledQuantity() != null && order.getFilledQuantity() > 0) {
            sb.append(String.format(" (Gerçekleşen: %d)", order.getFilledQuantity()));
        }
        if (order.getRemainingQuantity() != null && order.getRemainingQuantity() > 0) {
            sb.append(String.format(" (Kalan: %d)", order.getRemainingQuantity()));
        }
        sb.append("\n");

        // Price information
        if (order.getPrice() != null) {
            sb.append(String.format("Fiyat: ₺%.2f\n", order.getPrice()));
        }
        if (order.getStopPrice() != null) {
            sb.append(String.format("Stop Fiyat: ₺%.2f\n", order.getStopPrice()));
        }
        if (order.getAverageFillPrice() != null) {
            sb.append(String.format("Ortalama Gerçekleşme: ₺%.2f\n", order.getAverageFillPrice()));
        }

        // Order ID
        sb.append(String.format("ID: `%s`\n", order.getId().substring(0, Math.min(8, order.getId().length()))));

        sb.append("━━━━━━━━━━━━━━━━━\n");

        return sb.toString();
    }

    /**
     * Format a single AlgoLab order for display
     */
    private String formatAlgoLabOrder(Map<String, Object> order) {
        StringBuilder sb = new StringBuilder();

        // Extract data from AlgoLab TodaysTransaction response format
        String symbol = (String) order.getOrDefault("ticker", "N/A");
        String buySell = (String) order.getOrDefault("buysell", "N/A"); // "Alış" or "Satış"
        Object ordersizeObj = order.getOrDefault("ordersize", "0");
        Object waitingpriceObj = order.getOrDefault("waitingprice", "0");
        Object priceObj = order.getOrDefault("price", "0");
        String status = (String) order.getOrDefault("equityStatusDescription", "N/A");
        String orderId = (String) order.getOrDefault("transactionId",
                         (String) order.getOrDefault("atpref", "N/A"));

        // Convert Turkish buy/sell to English
        String direction = "Alış".equals(buySell) ? "ALIS" : "SATIŞ";
        String sideEmoji = "Alış".equals(buySell) ? "🟢" : "🔴";

        sb.append(String.format("*%s %s %s*\n", sideEmoji, direction, symbol));

        // Determine order type (if waitingprice is 0 or very close to 0, it's market order)
        double waitingPrice = 0;
        try {
            waitingPrice = Double.parseDouble(String.valueOf(waitingpriceObj));
        } catch (NumberFormatException e) {
            // ignore
        }

        String orderTypeDisplay = (waitingPrice > 0.01) ? "LIMIT" : "PIYASA";
        sb.append(String.format("Tip: %s | Durum: %s\n", orderTypeDisplay, status));

        // Quantity (ordersize is in adet/shares - display directly, no conversion needed)
        try {
            // Handle both Integer and Double from AlgoLab response
            int orderSize;
            if (ordersizeObj instanceof Integer) {
                orderSize = (Integer) ordersizeObj;
            } else if (ordersizeObj instanceof Double) {
                orderSize = ((Double) ordersizeObj).intValue();
            } else {
                orderSize = Integer.parseInt(String.valueOf(ordersizeObj));
            }

            sb.append(String.format("Miktar: %d adet\n", orderSize));
        } catch (NumberFormatException e) {
            sb.append(String.format("Miktar: %s adet\n", ordersizeObj));
        }

        // Price information (use waitingprice for limit orders)
        if (waitingPrice > 0.01) {
            sb.append(String.format("Fiyat: ₺%.2f\n", waitingPrice));
        } else {
            // For market orders, show executed price if available
            try {
                double executedPrice = Double.parseDouble(String.valueOf(priceObj));
                if (executedPrice > 0.01) {
                    sb.append(String.format("Gerçekleşen Fiyat: ₺%.2f\n", executedPrice));
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // Order ID
        sb.append(String.format("ID: `%s`\n", orderId.length() > 8 ? orderId.substring(0, 8) : orderId));

        sb.append("━━━━━━━━━━━━━━━━━\n");

        return sb.toString();
    }

    /**
     * Create inline keyboard for AlgoLab order list with cancel/modify buttons
     */
    private InlineKeyboardMarkup createAlgoLabOrderListKeyboard(List<Map<String, Object>> orders) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> order = orders.get(i);
            // Use transactionId or atpref as order ID
            String orderId = (String) order.getOrDefault("transactionId",
                             (String) order.getOrDefault("atpref", ""));

            if (orderId != null && !orderId.isEmpty()) {
                // Create two buttons per order: Modify and Cancel
                builder.keyboardRow(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text(String.format("✏️ Düzenle #%d", i + 1))
                        .callbackData("orders:modify:" + orderId)
                        .build(),
                    InlineKeyboardButton.builder()
                        .text(String.format("❌ İptal #%d", i + 1))
                        .callbackData("orders:cancel:" + orderId)
                        .build()
                ));
            }
        }

        // Add back button
        builder.keyboardRow(new InlineKeyboardRow(
            InlineKeyboardButton.builder()
                .text("🔙 Emirler Menüsü")
                .callbackData("menu:orders")
                .build()
        ));

        return builder.build();
    }
}
