package com.bisttrading.telegram.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for sending notifications to Telegram users.
 * Handles order execution notifications, alerts, and system messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final OkHttpTelegramClient telegramClient;
    private final TelegramSessionService sessionService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /**
     * Send order completed notification
     */
    public void sendOrderCompletedNotification(Long telegramUserId, OrderNotification order) {
        sessionService.getChatIdForUser(telegramUserId).ifPresentOrElse(
            chatId -> {
                StringBuilder message = new StringBuilder();
                message.append("🟢 *Emir Gerçekleşti!*\n\n");
                message.append(String.format("%s - %s\n", order.getSymbol(), order.getSide()));
                message.append(String.format("%d adet @ ₺%.2f\n", order.getQuantity(), order.getPrice()));
                message.append(String.format("Toplam: ₺%,.2f\n\n", order.getTotalAmount()));
                message.append(String.format("Emir ID: %s\n", order.getOrderId()));
                message.append("Durum: TAMAMLANDI ✅\n");
                message.append(String.format("Tarih: %s\n", formatDateTime(order.getExecutionTime())));

                InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        createButton("📊 Pozisyonları Gör", "broker:positions"),
                        createButton("➕ Yeni Emir", "orders:create")
                    ))
                    .build();

                sendMessage(chatId, message.toString(), keyboard);
            },
            () -> log.warn("Cannot send notification - chat ID not found for user: {}", telegramUserId)
        );
    }

    /**
     * Send partial fill notification
     */
    public void sendPartialFillNotification(Long telegramUserId, OrderNotification order) {
        sessionService.getChatIdForUser(telegramUserId).ifPresentOrElse(
            chatId -> {
                int filled = order.getFilledQuantity();
                int remaining = order.getQuantity() - filled;

                StringBuilder message = new StringBuilder();
                message.append("⚠️ *Emir Kısmen Gerçekleşti*\n\n");
                message.append(String.format("%s - %s\n", order.getSymbol(), order.getSide()));
                message.append(String.format("Gerçekleşen: %d / %d adet\n", filled, order.getQuantity()));
                message.append(String.format("Kalan: %d adet\n\n", remaining));
                message.append(String.format("%d adet @ ₺%.2f\n", filled, order.getPrice()));
                message.append(String.format("Tutar: ₺%,.2f\n", order.getTotalAmount()));

                InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        createButton("📋 Bekleyen Emirler", "orders:pending"),
                        createButton("❌ Kalan İptal", "orders:cancel:" + order.getOrderId())
                    ))
                    .build();

                sendMessage(chatId, message.toString(), keyboard);
            },
            () -> log.warn("Cannot send notification - chat ID not found for user: {}", telegramUserId)
        );
    }

    /**
     * Send order cancelled notification
     */
    public void sendOrderCancelledNotification(Long telegramUserId, OrderNotification order) {
        sessionService.getChatIdForUser(telegramUserId).ifPresentOrElse(
            chatId -> {
                StringBuilder message = new StringBuilder();
                message.append("❌ *Emir İptal Edildi*\n\n");
                message.append(String.format("%s - %s\n", order.getSymbol(), order.getSide()));
                message.append(String.format("%d adet @ ₺%.2f\n\n", order.getQuantity(), order.getPrice()));

                if (order.getCancelReason() != null && !order.getCancelReason().isEmpty()) {
                    message.append(String.format("Sebep: %s\n", order.getCancelReason()));
                }

                message.append(String.format("Tarih: %s\n", formatDateTime(order.getExecutionTime())));

                InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        createButton("📋 Bekleyen Emirler", "orders:pending"),
                        createButton("🔄 Yeniden Gönder", "orders:resubmit:" + order.getSymbol())
                    ))
                    .build();

                sendMessage(chatId, message.toString(), keyboard);
            },
            () -> log.warn("Cannot send notification - chat ID not found for user: {}", telegramUserId)
        );
    }

    /**
     * Send order status notification (for monitoring)
     */
    public void sendOrderStatusNotification(Long telegramUserId, Map<String, Object> orderData) {
        sessionService.getChatIdForUser(telegramUserId).ifPresentOrElse(
            chatId -> {
                String symbol = (String) orderData.getOrDefault("symbol", "N/A");
                String side = (String) orderData.getOrDefault("direction", "N/A");
                String status = (String) orderData.getOrDefault("status", "BEKLEMEDE");

                StringBuilder message = new StringBuilder();
                message.append("📊 *Emir Durumu*\n\n");
                message.append(String.format("%s - %s\n", symbol, side));
                message.append(String.format("Durum: %s\n", status));

                InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        createButton("📋 Bekleyen Emirler", "orders:pending"),
                        createButton("🔙 Ana Menü", "menu:main")
                    ))
                    .build();

                sendMessage(chatId, message.toString(), keyboard);
            },
            () -> log.warn("Cannot send notification - chat ID not found for user: {}", telegramUserId)
        );
    }

    /**
     * Send a simple text notification
     */
    public void sendTextNotification(Long telegramUserId, String text) {
        sessionService.getChatIdForUser(telegramUserId).ifPresentOrElse(
            chatId -> sendMessage(chatId, text, null),
            () -> log.warn("Cannot send notification - chat ID not found for user: {}", telegramUserId)
        );
    }

    /**
     * Helper method to send a message
     */
    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();

            if (keyboard != null) {
                message.setReplyMarkup(keyboard);
            }

            telegramClient.execute(message);
            log.debug("Notification sent to chat: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Helper to create inline keyboard button
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
            .text(text)
            .callbackData(callbackData)
            .build();
    }

    /**
     * Format date time for display
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Order notification data class
     */
    @Data
    public static class OrderNotification {
        private String orderId;
        private String symbol;
        private String side;
        private int quantity;
        private int filledQuantity;
        private BigDecimal price;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime executionTime;
        private String cancelReason;

        public static OrderNotification fromAlgoLabOrder(Map<String, Object> order) {
            OrderNotification notification = new OrderNotification();
            notification.setOrderId((String) order.get("transactionId"));
            notification.setSymbol((String) order.get("symbol"));
            notification.setSide((String) order.get("direction"));

            // Parse quantity
            Object quantityObj = order.get("quantity");
            if (quantityObj instanceof Integer) {
                notification.setQuantity((Integer) quantityObj);
            } else if (quantityObj != null) {
                notification.setQuantity(Integer.parseInt(quantityObj.toString()));
            }

            // Parse filled quantity
            Object filledObj = order.get("filled");
            if (filledObj instanceof Integer) {
                notification.setFilledQuantity((Integer) filledObj);
            } else if (filledObj != null) {
                notification.setFilledQuantity(Integer.parseInt(filledObj.toString()));
            }

            // Parse price
            Object priceObj = order.get("price");
            if (priceObj instanceof BigDecimal) {
                notification.setPrice((BigDecimal) priceObj);
            } else if (priceObj instanceof Double) {
                notification.setPrice(BigDecimal.valueOf((Double) priceObj));
            } else if (priceObj != null) {
                notification.setPrice(new BigDecimal(priceObj.toString()));
            }

            // Calculate total amount
            if (notification.getPrice() != null && notification.getFilledQuantity() > 0) {
                notification.setTotalAmount(
                    notification.getPrice().multiply(BigDecimal.valueOf(notification.getFilledQuantity()))
                );
            } else if (notification.getPrice() != null) {
                notification.setTotalAmount(
                    notification.getPrice().multiply(BigDecimal.valueOf(notification.getQuantity()))
                );
            }

            notification.setStatus((String) order.get("status"));
            notification.setExecutionTime(LocalDateTime.now());
            notification.setCancelReason((String) order.get("cancelReason"));

            return notification;
        }
    }
}
