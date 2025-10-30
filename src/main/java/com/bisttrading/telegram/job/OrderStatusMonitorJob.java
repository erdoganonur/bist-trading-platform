package com.bisttrading.telegram.job;

import com.bisttrading.broker.algolab.service.AlgoLabOrderService;
import com.bisttrading.telegram.dto.TelegramUserSession;
import com.bisttrading.telegram.service.TelegramNotificationService;
import com.bisttrading.telegram.service.TelegramSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Background job that monitors order status for all active Telegram users.
 * Checks every minute for order completions and sends notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true")
public class OrderStatusMonitorJob {

    private final TelegramSessionService sessionService;
    private final AlgoLabOrderService algoLabOrderService;
    private final TelegramNotificationService notificationService;

    /**
     * Check order status for all active users every minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Every 1 minute
    public void monitorOrderStatus() {
        log.debug("Starting order status monitoring job...");

        try {
            // Get all active sessions
            List<TelegramUserSession> activeSessions = sessionService.getAllActiveSessions();

            if (activeSessions.isEmpty()) {
                log.debug("No active sessions to monitor");
                return;
            }

            log.debug("Monitoring {} active sessions", activeSessions.size());

            // Check each user's orders
            for (TelegramUserSession session : activeSessions) {
                try {
                    checkUserOrders(session);
                } catch (Exception e) {
                    log.error("Error checking orders for user {}: {}",
                        session.getTelegramUserId(), e.getMessage(), e);
                }
            }

            log.debug("Order status monitoring job completed");
        } catch (Exception e) {
            log.error("Error in order status monitoring job: {}", e.getMessage(), e);
        }
    }

    /**
     * Check orders for a specific user
     */
    private void checkUserOrders(TelegramUserSession session) {
        Long telegramUserId = session.getTelegramUserId();

        // Only check if user has valid AlgoLab session
        if (!session.isAlgoLabSessionValid()) {
            log.debug("User {} does not have valid AlgoLab session, skipping", telegramUserId);
            return;
        }

        try {
            // Get current pending orders from AlgoLab
            List<Map<String, Object>> currentOrders = algoLabOrderService.getPendingOrders("");

            if (currentOrders == null) {
                log.warn("Failed to fetch pending orders for user {}", telegramUserId);
                return;
            }

            // Extract current order IDs
            Set<String> currentOrderIds = currentOrders.stream()
                .map(order -> (String) order.get("transactionId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Get previously tracked order IDs from Redis
            Set<String> previousOrderIds = sessionService.getTrackedOrderIds(telegramUserId);

            // If this is the first check, just store current orders
            if (previousOrderIds.isEmpty()) {
                log.debug("First check for user {}, tracking {} orders",
                    telegramUserId, currentOrderIds.size());
                sessionService.setTrackedOrderIds(telegramUserId, currentOrderIds);
                return;
            }

            // Find completed orders (were in previous list, not in current list)
            Set<String> completedOrderIds = new HashSet<>(previousOrderIds);
            completedOrderIds.removeAll(currentOrderIds);

            if (!completedOrderIds.isEmpty()) {
                log.info("User {} has {} completed orders",
                    telegramUserId, completedOrderIds.size());

                // Send notifications for completed orders
                for (String completedOrderId : completedOrderIds) {
                    sendCompletionNotification(telegramUserId, completedOrderId, currentOrders);
                }
            }

            // Update tracked orders
            sessionService.setTrackedOrderIds(telegramUserId, currentOrderIds);

            log.debug("Order check completed for user {}: {} current, {} completed",
                telegramUserId, currentOrderIds.size(), completedOrderIds.size());

        } catch (Exception e) {
            log.error("Error processing orders for user {}: {}",
                telegramUserId, e.getMessage(), e);
        }
    }

    /**
     * Send completion notification for an order
     */
    private void sendCompletionNotification(
            Long telegramUserId,
            String orderId,
            List<Map<String, Object>> currentOrders) {

        try {
            // Try to get order details from today's transactions
            Map<String, Object> orderDetails = getOrderDetails(orderId);

            if (orderDetails == null) {
                log.warn("Could not find details for completed order: {}", orderId);
                // Send generic completion notification
                notificationService.sendTextNotification(
                    telegramUserId,
                    String.format("🟢 Emir gerçekleşti! (ID: %s)", orderId)
                );
                return;
            }

            // Create notification from order details
            TelegramNotificationService.OrderNotification notification =
                TelegramNotificationService.OrderNotification.fromAlgoLabOrder(orderDetails);

            // Send appropriate notification based on order status
            String status = (String) orderDetails.get("status");
            if (status != null) {
                if (status.contains("CANCELLED") || status.contains("CANCELED")) {
                    notificationService.sendOrderCancelledNotification(telegramUserId, notification);
                } else if (status.contains("PARTIAL")) {
                    notificationService.sendPartialFillNotification(telegramUserId, notification);
                } else {
                    notificationService.sendOrderCompletedNotification(telegramUserId, notification);
                }
            } else {
                // Default to completed notification
                notificationService.sendOrderCompletedNotification(telegramUserId, notification);
            }

            log.info("Sent completion notification for order {} to user {}",
                orderId, telegramUserId);

        } catch (Exception e) {
            log.error("Error sending completion notification for order {}: {}",
                orderId, e.getMessage(), e);
        }
    }

    /**
     * Get order details from AlgoLab (would typically fetch from transaction history)
     */
    private Map<String, Object> getOrderDetails(String orderId) {
        try {
            // In a real implementation, this would fetch from AlgoLab's transaction history API
            // For now, return a minimal order info
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("transactionId", orderId);
            orderInfo.put("status", "COMPLETED");
            return orderInfo;
        } catch (Exception e) {
            log.error("Error fetching order details for {}: {}", orderId, e.getMessage(), e);
            return null;
        }
    }
}
