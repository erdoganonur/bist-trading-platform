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

        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("direction", direction);
        payload.put("pricetype", priceType);
        payload.put("price", price.toString());
        payload.put("lot", lot.toString());
        payload.put("sms", sms != null ? sms : false);
        payload.put("email", email != null ? email : false);
        payload.put("subAccount", subAccount != null ? subAccount : "");

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", orderId);
        payload.put("price", price.toString());
        payload.put("lot", lot != null ? lot.toString() : "0");
        payload.put("viop", viop != null ? viop : false);
        payload.put("subAccount", subAccount != null ? subAccount : "");

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", orderId);
        payload.put("subAccount", subAccount != null ? subAccount : "");

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

        Map<String, Object> payload = new HashMap<>();
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

        Map<String, Object> payload = new HashMap<>();
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
}
