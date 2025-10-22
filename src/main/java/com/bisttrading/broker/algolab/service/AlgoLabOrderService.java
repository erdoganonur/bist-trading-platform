package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.dto.response.AlgoLabBaseResponse;
import com.bisttrading.broker.algolab.exception.AlgoLabApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AlgoLab order management service.
 */
@Service
@Slf4j
public class AlgoLabOrderService {

    private final AlgoLabRestClient restClient;

    public AlgoLabOrderService(AlgoLabRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends a new order to AlgoLab.
     *
     * @param symbol Sembol kodu (örn. "AKBNK")
     * @param direction "BUY" veya "SELL"
     * @param priceType "limit" veya "piyasa"
     * @param price Fiyat (limit emri için)
     * @param lot Lot miktarı (1 lot = 100 hisse)
     * @param sms SMS bildirimi
     * @param email Email bildirimi
     * @param subAccount Alt hesap (boş gönderilirse aktif hesap)
     * @return Response with order details
     */
    public Map<String, Object> sendOrder(
        String symbol,
        String direction,
        String priceType,
        BigDecimal price,
        Integer lot,
        Boolean sms,
        Boolean email,
        String subAccount
    ) {
        log.info("Sending order: {} {} lot={} price={}", direction, symbol, lot, price);

        // Convert direction: "0"/0/"BUY" -> "BUY", "1"/1/"SELL" -> "SELL"
        String algolabDirection = normalizeDirection(direction);

        // Convert priceType from "P"/"L" to AlgoLab format "piyasa"/"limit"
        String algolabPriceType = "P".equalsIgnoreCase(priceType) ? "piyasa" : "limit";

        // CRITICAL: Use LinkedHashMap to preserve insertion order for checker hash calculation
        // Python dict maintains order (Python 3.7+), so we must match the exact key order
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("symbol", symbol);
        payload.put("direction", algolabDirection);
        payload.put("pricetype", algolabPriceType);
        // For market orders, price can be empty string or "0"
        payload.put("price", price != null ? price.toString() : "");
        payload.put("lot", lot.toString());
        payload.put("sms", sms != null ? sms : false);
        payload.put("email", email != null ? email : false);
        // CRITICAL: Key name is "subAccount" (lowercase 's', uppercase 'A') - matches Python exactly
        payload.put("subAccount", (subAccount == null || "0".equals(subAccount)) ? "" : subAccount);

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/SendOrder",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from SendOrder", 500);
            }

            log.info("Order sent successfully: {}", body);
            return body;

        } catch (Exception e) {
            log.error("SendOrder failed for {} {}", direction, symbol, e);
            throw e;
        }
    }

    /**
     * Modifies an existing order.
     *
     * @param orderId Order ID
     * @param price New price
     * @param lot New lot (for VIOP, 0 for equity)
     * @param viop Is VIOP order?
     * @param subAccount Sub account
     * @return Response with modified order details
     */
    public Map<String, Object> modifyOrder(
        String orderId,
        BigDecimal price,
        Integer lot,
        Boolean viop,
        String subAccount
    ) {
        log.info("Modifying order: {} price={} lot={}", orderId, price, lot);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", orderId);
        // Allow null price when only modifying quantity
        payload.put("price", price != null ? price.toString() : "");
        payload.put("lot", lot != null ? lot.toString() : "0");
        payload.put("viop", viop != null ? viop : false);
        payload.put("Subaccount", subAccount != null ? subAccount : "");

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/ModifyOrder",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from ModifyOrder", 500);
            }

            log.info("Order modified successfully: {}", orderId);
            return body;

        } catch (Exception e) {
            log.error("ModifyOrder failed for {}", orderId, e);
            throw e;
        }
    }

    /**
     * Deletes (cancels) an order.
     *
     * @param orderId Order ID
     * @param subAccount Sub account
     * @return Response with cancellation details
     */
    public Map<String, Object> deleteOrder(String orderId, String subAccount) {
        log.info("Deleting order: {}", orderId);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", orderId);
        payload.put("Subaccount", subAccount != null ? subAccount : "");

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/DeleteOrder",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from DeleteOrder", 500);
            }

            log.info("Order deleted successfully: {}", orderId);
            return body;

        } catch (Exception e) {
            log.error("DeleteOrder failed for {}", orderId, e);
            throw e;
        }
    }

    /**
     * Gets instant positions (portfolio).
     *
     * @param subAccount Sub account
     * @return Response with positions
     */
    public Map<String, Object> getInstantPosition(String subAccount) {
        log.debug("Getting instant positions for subAccount: {}", subAccount);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("Subaccount", subAccount != null ? subAccount : "");

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/InstantPosition",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from InstantPosition", 500);
            }

            return body;

        } catch (Exception e) {
            log.error("GetInstantPosition failed", e);
            throw e;
        }
    }

    /**
     * Gets today's transactions.
     *
     * @param subAccount Sub account
     * @return Response with transactions
     */
    public Map<String, Object> getTodaysTransactions(String subAccount) {
        log.debug("Getting today's transactions for subAccount: {}", subAccount);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("Subaccount", subAccount != null ? subAccount : "");

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/TodaysTransaction",
                payload,
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from TodaysTransaction", 500);
            }

            // DEBUG: Log full response to understand structure
            log.info("🔍 TodaysTransaction FULL RESPONSE: {}", body);
            log.info("🔍 Response has 'content' key: {}", body.containsKey("content"));
            log.info("🔍 Response has 'success' key: {}", body.containsKey("success"));

            if (body.containsKey("content")) {
                Object content = body.get("content");
                log.info("🔍 Content type: {}", content != null ? content.getClass().getName() : "null");
                if (content instanceof List) {
                    log.info("🔍 Content list size: {}", ((List<?>) content).size());
                    if (!((List<?>) content).isEmpty()) {
                        log.info("🔍 First item in content: {}", ((List<?>) content).get(0));
                    }
                } else if (content instanceof Map) {
                    log.info("🔍 Content is a Map with keys: {}", ((Map<?, ?>) content).keySet());
                } else if (content == null) {
                    log.warn("⚠️ Content is NULL");
                } else {
                    log.info("🔍 Content value: {}", content);
                }
            } else {
                log.warn("⚠️ Response does NOT contain 'content' key. Available keys: {}", body.keySet());
            }

            return body;

        } catch (Exception e) {
            log.error("GetTodaysTransactions failed", e);
            throw e;
        }
    }

    /**
     * Gets sub accounts.
     *
     * @return Response with sub accounts
     */
    public Map<String, Object> getSubAccounts() {
        log.debug("Getting sub accounts");

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/GetSubAccounts",
                Map.of(),
                true,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AlgoLabApiException("Empty response from GetSubAccounts", 500);
            }

            return body;

        } catch (Exception e) {
            log.error("GetSubAccounts failed", e);
            throw e;
        }
    }

    /**
     * Gets pending/open orders from AlgoLab.
     * Uses TodaysTransaction API which includes all orders: pending, executed, cancelled, etc.
     * Then filters for only pending orders.
     *
     * @param subAccount Sub account
     * @return List of pending orders
     */
    public List<Map<String, Object>> getPendingOrders(String subAccount) {
        log.debug("Getting pending orders from AlgoLab for subAccount: {}", subAccount);

        try {
            // Get all today's transactions (includes pending, executed, cancelled)
            Map<String, Object> response = getTodaysTransactions(subAccount);

            // Extract orders from response
            // Response format: {"success": true, "content": [orders...]}
            if (response == null || !response.containsKey("content")) {
                log.warn("TodaysTransaction response is null or missing content");
                return List.of();
            }

            Object content = response.get("content");
            if (!(content instanceof List)) {
                log.warn("TodaysTransaction content is not a list");
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allOrders = (List<Map<String, Object>>) content;

            // DEBUG: Log first order to see structure
            if (!allOrders.isEmpty()) {
                log.info("Sample AlgoLab order structure: {}", allOrders.get(0));
            }

            // Filter for pending orders only
            // AlgoLab uses equityStatusDescription field with values: WAITING, DONE, DELETED, etc.
            List<Map<String, Object>> pendingOrders = allOrders.stream()
                    .filter(order -> {
                        // Check equityStatusDescription field (primary status indicator)
                        String equityStatus = (String) order.get("equityStatusDescription");

                        // Also check description field as fallback (Turkish text)
                        String description = (String) order.get("description");

                        // Filter for WAITING orders (pending, not executed, not cancelled)
                        boolean isWaiting = "WAITING".equals(equityStatus);

                        // Fallback: Check Turkish description for pending states
                        boolean isPendingByDescription = description != null && (
                            description.contains("İletildi") ||      // Sent/Submitted
                            description.contains("Bekle") ||         // Waiting
                            description.contains("Kısmi")            // Partially filled
                        );

                        return isWaiting || isPendingByDescription;
                    })
                    .toList();

            log.info("Found {} pending orders out of {} total orders", pendingOrders.size(), allOrders.size());
            return pendingOrders;

        } catch (Exception e) {
            log.error("Failed to get pending orders from AlgoLab", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Normalizes direction to AlgoLab API format.
     * Accepts: "0", "1", "BUY", "SELL" (case-insensitive)
     * Returns: "BUY" or "SELL"
     *
     * @param direction Input direction
     * @return Normalized direction ("BUY" or "SELL")
     * @throws AlgoLabApiException if direction is invalid
     */
    private String normalizeDirection(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            throw new AlgoLabApiException("Direction cannot be null or empty", 400);
        }

        String normalized = direction.trim().toUpperCase();

        // Map numeric values to direction names
        switch (normalized) {
            case "0":
            case "BUY":
                return "BUY";
            case "1":
            case "SELL":
                return "SELL";
            default:
                throw new AlgoLabApiException(
                    "Invalid direction: " + direction + ". Must be 0/BUY or 1/SELL",
                    400
                );
        }
    }
}
