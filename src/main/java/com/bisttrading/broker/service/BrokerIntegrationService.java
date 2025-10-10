package com.bisttrading.broker.service;

import com.bisttrading.broker.algolab.service.AlgoLabAuthService;
import com.bisttrading.broker.algolab.service.AlgoLabOrderService;
import com.bisttrading.broker.dto.AlgoLabResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Production-grade AlgoLab broker integration service.
 *
 * Provides comprehensive trading operations through AlgoLab API integration:
 * - Order management (send, modify, cancel)
 * - Portfolio and position tracking
 * - Transaction history
 * - Real-time market data access
 * - Session and authentication management
 *
 * This service acts as a facade for AlgoLab API operations, providing
 * a clean interface for broker operations within the BIST Trading Platform.
 *
 * @since 2.0.0
 * @see AlgoLabOrderService
 * @see AlgoLabAuthService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerIntegrationService {

    private final AlgoLabOrderService algoLabOrderService;
    private final AlgoLabAuthService algoLabAuthService;

    /**
     * Places a new order via AlgoLab API.
     *
     * @param symbol Stock symbol (e.g., "AKBNK", "GARAN")
     * @param direction Order direction ("BUY" or "SELL")
     * @param priceType Price type ("LIMIT", "MARKET", etc.)
     * @param price Order price (for limit orders)
     * @param lot Number of lots to trade
     * @param sms Enable SMS notification
     * @param email Enable email notification
     * @param subAccount Sub-account identifier (optional)
     * @return Response containing order confirmation or error details
     */
    public AlgoLabResponse<Object> sendOrder(
            String symbol,
            String direction,
            String priceType,
            BigDecimal price,
            Integer lot,
            Boolean sms,
            Boolean email,
            String subAccount) {

        log.info("Sending order to AlgoLab - symbol: {}, direction: {}, price: {}, lot: {}",
            symbol, direction, price, lot);

        try {
            Map<String, Object> response = algoLabOrderService.sendOrder(
                symbol, direction, priceType, price, lot, sms, email, subAccount
            );

            boolean success = (boolean) response.getOrDefault("success", false);
            String message = (String) response.getOrDefault("message", "");
            Object content = response.get("content");

            if (success) {
                log.info("Order placed successfully via AlgoLab: {}", content);
                return AlgoLabResponse.success(content, message);
            } else {
                log.warn("Order placement failed: {}", message);
                return AlgoLabResponse.error("ORDER_FAILED", message);
            }

        } catch (Exception e) {
            log.error("Failed to send order to AlgoLab", e);
            return AlgoLabResponse.error("API_ERROR", "AlgoLab API çağrısı başarısız: " + e.getMessage());
        }
    }

    /**
     * Modifies an existing order via AlgoLab API.
     *
     * @param orderId ID of the order to modify
     * @param price New price (for limit orders)
     * @param lot New lot quantity
     * @param viop VIOP flag ("true" or "false")
     * @param subAccount Sub-account identifier (optional)
     * @return Response containing modification confirmation or error details
     */
    public AlgoLabResponse<Object> modifyOrder(
            String orderId,
            BigDecimal price,
            Integer lot,
            String viop,
            String subAccount) {

        log.info("Modifying order via AlgoLab - orderId: {}, price: {}, lot: {}", orderId, price, lot);

        try {
            Boolean isViop = viop != null && viop.equalsIgnoreCase("true");
            Map<String, Object> response = algoLabOrderService.modifyOrder(
                orderId, price, lot, isViop, subAccount
            );

            boolean success = (boolean) response.getOrDefault("success", false);
            String message = (String) response.getOrDefault("message", "");
            Object content = response.get("content");

            if (success) {
                log.info("Order modified successfully via AlgoLab: {}", orderId);
                return AlgoLabResponse.success(content, message);
            } else {
                log.warn("Order modification failed: {}", message);
                return AlgoLabResponse.error("MODIFY_FAILED", message);
            }

        } catch (Exception e) {
            log.error("Failed to modify order via AlgoLab", e);
            return AlgoLabResponse.error("API_ERROR", "AlgoLab API çağrısı başarısız: " + e.getMessage());
        }
    }

    /**
     * Cancels an order via AlgoLab API.
     *
     * @param orderId ID of the order to cancel
     * @param subAccount Sub-account identifier (optional)
     * @return Response containing cancellation confirmation or error details
     */
    public AlgoLabResponse<Object> deleteOrder(String orderId, String subAccount) {
        log.info("Cancelling order via AlgoLab - orderId: {}", orderId);

        try {
            Map<String, Object> response = algoLabOrderService.deleteOrder(orderId, subAccount);

            boolean success = (boolean) response.getOrDefault("success", false);
            String message = (String) response.getOrDefault("message", "");
            Object content = response.get("content");

            if (success) {
                log.info("Order cancelled successfully via AlgoLab: {}", orderId);
                return AlgoLabResponse.success(content, message);
            } else {
                log.warn("Order cancellation failed: {}", message);
                return AlgoLabResponse.error("CANCEL_FAILED", message);
            }

        } catch (Exception e) {
            log.error("Failed to cancel order via AlgoLab", e);
            return AlgoLabResponse.error("API_ERROR", "AlgoLab API çağrısı başarısız: " + e.getMessage());
        }
    }

    /**
     * Gets user's portfolio positions via AlgoLab API.
     *
     * @return List of position objects containing symbol, quantity, value, etc.
     */
    public List<Map<String, Object>> getPositions() {
        log.debug("Getting portfolio positions from AlgoLab");

        try {
            Map<String, Object> response = algoLabOrderService.getInstantPosition(null);
            boolean success = (boolean) response.getOrDefault("success", false);

            if (success && response.containsKey("content")) {
                Map<String, Object> content = (Map<String, Object>) response.get("content");
                if (content.containsKey("positions")) {
                    return (List<Map<String, Object>>) content.get("positions");
                }
            }

            log.warn("No positions found in AlgoLab response");
            return List.of();

        } catch (Exception e) {
            log.error("Failed to get positions from AlgoLab", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Gets transaction history via AlgoLab API.
     *
     * @param subAccount Sub-account identifier (optional)
     * @return Response containing transaction history or error details
     */
    public AlgoLabResponse<Object> getTodaysTransactions(String subAccount) {
        log.debug("Getting today's transactions from AlgoLab");

        try {
            Map<String, Object> response = algoLabOrderService.getTodaysTransactions(subAccount);

            boolean success = (boolean) response.getOrDefault("success", false);
            String message = (String) response.getOrDefault("message", "");
            Object content = response.get("content");

            if (success) {
                return AlgoLabResponse.success(content, message);
            } else {
                log.warn("Failed to get transactions: {}", message);
                return AlgoLabResponse.error("GET_TRANSACTIONS_FAILED", message);
            }

        } catch (Exception e) {
            log.error("Failed to get transactions from AlgoLab", e);
            return AlgoLabResponse.error("API_ERROR", "AlgoLab API çağrısı başarısız: " + e.getMessage());
        }
    }

    /**
     * Gets instant positions via AlgoLab API.
     * Provides real-time portfolio snapshot with current prices and P&L.
     *
     * @param subAccount Sub-account identifier (optional)
     * @return Response containing instant positions or error details
     */
    public AlgoLabResponse<Object> getInstantPosition(String subAccount) {
        log.debug("Getting instant positions from AlgoLab");

        try {
            Map<String, Object> response = algoLabOrderService.getInstantPosition(subAccount);

            boolean success = (boolean) response.getOrDefault("success", false);
            String message = (String) response.getOrDefault("message", "");
            Object content = response.get("content");

            if (success) {
                return AlgoLabResponse.success(content, message);
            } else {
                log.warn("Failed to get instant positions: {}", message);
                return AlgoLabResponse.error("GET_POSITIONS_FAILED", message);
            }

        } catch (Exception e) {
            log.error("Failed to get instant positions from AlgoLab", e);
            return AlgoLabResponse.error("API_ERROR", "AlgoLab API çağrısı başarısız: " + e.getMessage());
        }
    }

    /**
     * Checks if broker service is authenticated with AlgoLab.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return algoLabAuthService.isAuthenticated();
    }

    /**
     * Checks if broker connection is alive and session is valid.
     *
     * @return true if connected and session is active, false otherwise
     */
    public boolean isConnected() {
        return algoLabAuthService.isAlive();
    }
}
