package com.bisttrading.telegram.service;

import com.bisttrading.broker.algolab.service.AlgoLabOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for fetching real-time market data from AlgoLab.
 * Used to show current prices when placing orders.
 */
@Slf4j
@Service
public class MarketDataService {

    private final AlgoLabOrderService algoLabOrderService;

    public MarketDataService(AlgoLabOrderService algoLabOrderService) {
        this.algoLabOrderService = algoLabOrderService;
    }

    /**
     * Get current price for a symbol from AlgoLab positions.
     * This fetches the user's positions and extracts the latest price for the given symbol.
     *
     * @param symbol Symbol code (e.g., "AKBNK")
     * @param subAccount Sub account (empty for default)
     * @return Current price if available
     */
    public Optional<BigDecimal> getCurrentPrice(String symbol, String subAccount) {
        try {
            log.debug("Fetching current price for symbol: {}", symbol);

            // Get positions from AlgoLab (this includes current prices)
            Map<String, Object> response = algoLabOrderService.getInstantPosition(subAccount);

            if (response == null || !response.containsKey("content")) {
                log.warn("No positions data available");
                return Optional.empty();
            }

            Object content = response.get("content");
            if (!(content instanceof List)) {
                log.warn("Positions content is not a list");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> positions = (List<Map<String, Object>>) content;

            // Find the position for this symbol
            for (Map<String, Object> position : positions) {
                String code = (String) position.get("code");
                if (symbol.equalsIgnoreCase(code)) {
                    // Found the position - extract current price
                    Object unitpriceObj = position.get("unitprice");
                    if (unitpriceObj != null) {
                        BigDecimal price = parseDecimal(unitpriceObj);
                        log.info("Found current price for {}: ₺{}", symbol, price);
                        return Optional.of(price);
                    }
                }
            }

            log.warn("No price found for symbol: {}", symbol);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error fetching current price for {}: {}", symbol, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Format price info message for user.
     *
     * @param symbol Symbol code
     * @param price Current price
     * @return Formatted message
     */
    public String formatPriceInfo(String symbol, BigDecimal price) {
        return String.format("💵 *%s Son Fiyat:* ₺%.2f", symbol, price);
    }

    /**
     * Helper to parse BigDecimal from various object types.
     */
    private BigDecimal parseDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        if (obj instanceof String) {
            try {
                return new BigDecimal((String) obj);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse decimal from string: {}", obj);
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}
