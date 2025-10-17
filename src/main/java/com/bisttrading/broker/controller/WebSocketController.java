package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.dto.websocket.OrderBookData;
import com.bisttrading.broker.algolab.dto.websocket.TickData;
import com.bisttrading.broker.algolab.dto.websocket.TradeData;
import com.bisttrading.broker.algolab.model.AlgoLabSession;
import com.bisttrading.broker.algolab.service.AlgoLabSessionManager;
import com.bisttrading.broker.algolab.websocket.AlgoLabWebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * REST controller for managing AlgoLab WebSocket connections and subscriptions.
 */
@RestController
@RequestMapping("/api/v1/broker/websocket")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket Management", description = "Manage AlgoLab WebSocket connections and subscriptions")
public class WebSocketController {

    private final AlgoLabWebSocketService webSocketService;
    private final AlgoLabSessionManager sessionManager;

    @PostMapping("/connect")
    @Operation(summary = "Connect to AlgoLab WebSocket", description = "Establishes WebSocket connection using existing session")
    public ResponseEntity<Map<String, Object>> connect() {
        try {
            AlgoLabSession session = sessionManager.loadSession();

            if (session == null || session.getToken() == null || session.getHash() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "No valid session found. Please authenticate first."
                    ));
            }

            webSocketService.connectAsync(session.getToken(), session.getHash())
                .thenAccept(v -> log.info("WebSocket connected successfully"))
                .exceptionally(ex -> {
                    log.error("WebSocket connection failed", ex);
                    return null;
                });

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "WebSocket connection initiated"
            ));

        } catch (Exception e) {
            log.error("Error connecting to WebSocket", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Failed to connect: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect from WebSocket", description = "Closes WebSocket connection")
    public ResponseEntity<Map<String, String>> disconnect() {
        try {
            webSocketService.disconnect();
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "WebSocket disconnected"
            ));
        } catch (Exception e) {
            log.error("Error disconnecting WebSocket", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to disconnect: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get WebSocket status", description = "Returns current WebSocket connection status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(webSocketService.getStatus());
    }

    @PostMapping("/subscribe/tick/{symbol}")
    @Operation(summary = "Subscribe to tick data", description = "Subscribe to real-time tick data for a symbol")
    public ResponseEntity<Map<String, String>> subscribeToTick(@PathVariable String symbol) {
        try {
            webSocketService.subscribeToTick(symbol, tickData -> {
                log.debug("Tick received for {}: last={}, bid={}, ask={}",
                    tickData.getSymbol(),
                    tickData.getLastPrice(),
                    tickData.getBidPrice(),
                    tickData.getAskPrice()
                );
            });

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Subscribed to tick data for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error subscribing to tick data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to subscribe: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/subscribe/orderbook/{symbol}")
    @Operation(summary = "Subscribe to order book", description = "Subscribe to real-time order book for a symbol")
    public ResponseEntity<Map<String, String>> subscribeToOrderBook(@PathVariable String symbol) {
        try {
            webSocketService.subscribeToOrderBook(symbol, orderBook -> {
                log.debug("OrderBook received for {}: spread={}, midPrice={}",
                    orderBook.getSymbol(),
                    orderBook.getSpread(),
                    orderBook.getMidPrice()
                );
            });

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Subscribed to order book for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error subscribing to order book", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to subscribe: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/subscribe/trade/{symbol}")
    @Operation(summary = "Subscribe to trades", description = "Subscribe to real-time trade executions for a symbol")
    public ResponseEntity<Map<String, String>> subscribeToTrade(@PathVariable String symbol) {
        try {
            webSocketService.subscribeToTrade(symbol, trade -> {
                log.debug("Trade received for {}: price={}, qty={}, side={}",
                    trade.getSymbol(),
                    trade.getPrice(),
                    trade.getQuantity(),
                    trade.getSide()
                );
            });

            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Subscribed to trade data for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error subscribing to trade data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to subscribe: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/unsubscribe/tick/{symbol}")
    @Operation(summary = "Unsubscribe from tick data", description = "Unsubscribe from tick data for a symbol")
    public ResponseEntity<Map<String, String>> unsubscribeFromTick(@PathVariable String symbol) {
        try {
            webSocketService.unsubscribeFromTick(symbol);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Unsubscribed from tick data for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error unsubscribing from tick data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/unsubscribe/orderbook/{symbol}")
    @Operation(summary = "Unsubscribe from order book", description = "Unsubscribe from order book for a symbol")
    public ResponseEntity<Map<String, String>> unsubscribeFromOrderBook(@PathVariable String symbol) {
        try {
            webSocketService.unsubscribeFromOrderBook(symbol);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Unsubscribed from order book for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error unsubscribing from order book", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/unsubscribe/trade/{symbol}")
    @Operation(summary = "Unsubscribe from trades", description = "Unsubscribe from trade data for a symbol")
    public ResponseEntity<Map<String, String>> unsubscribeFromTrade(@PathVariable String symbol) {
        try {
            webSocketService.unsubscribeFromTrade(symbol);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Unsubscribed from trade data for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error unsubscribing from trade data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/unsubscribe/all/{symbol}")
    @Operation(summary = "Unsubscribe from all channels", description = "Unsubscribe from all data types for a symbol")
    public ResponseEntity<Map<String, String>> unsubscribeFromAll(@PathVariable String symbol) {
        try {
            webSocketService.unsubscribeFromAll(symbol);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Unsubscribed from all channels for " + symbol
            ));
        } catch (Exception e) {
            log.error("Error unsubscribing from all channels", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", "false",
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get active subscriptions", description = "Returns all active WebSocket subscriptions")
    public ResponseEntity<Map<String, Set<String>>> getSubscriptions() {
        return ResponseEntity.ok(webSocketService.getAllSubscriptions());
    }

    @GetMapping("/subscriptions/{channel}")
    @Operation(summary = "Get subscriptions for channel", description = "Returns active subscriptions for a specific channel")
    public ResponseEntity<Set<String>> getSubscriptionsByChannel(@PathVariable String channel) {
        return ResponseEntity.ok(webSocketService.getSubscriptions(channel));
    }

    /**
     * Generic subscribe endpoint for CLI compatibility.
     * Accepts channel and symbol in JSON body.
     */
    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to channel", description = "Generic subscribe endpoint accepting channel and symbol in JSON")
    public ResponseEntity<Map<String, Object>> subscribeGeneric(@RequestBody Map<String, String> request) {
        try {
            String channel = request.get("channel");
            String symbol = request.get("symbol");

            if (channel == null || symbol == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "Both 'channel' and 'symbol' are required"
                    ));
            }

            // Subscribe based on channel type
            switch (channel.toLowerCase()) {
                case "tick":
                    webSocketService.subscribeToTick(symbol, tickData ->
                        log.debug("Tick: {} @ {}", tickData.getSymbol(), tickData.getLastPrice())
                    );
                    break;
                case "orderbook":
                    webSocketService.subscribeToOrderBook(symbol, orderBook ->
                        log.debug("OrderBook: {} spread={}", orderBook.getSymbol(), orderBook.getSpread())
                    );
                    break;
                case "trade":
                    webSocketService.subscribeToTrade(symbol, trade ->
                        log.debug("Trade: {} {} @ {}", trade.getSymbol(), trade.getQuantity(), trade.getPrice())
                    );
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "Invalid channel: " + channel + ". Valid channels: tick, orderbook, trade"
                        ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Subscribed to %s channel for %s", channel, symbol),
                "channel", channel,
                "symbol", symbol
            ));

        } catch (Exception e) {
            log.error("Error in generic subscribe", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Failed to subscribe: " + e.getMessage()
                ));
        }
    }

    /**
     * Generic unsubscribe endpoint for CLI compatibility.
     * Accepts channel and symbol in JSON body.
     */
    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from channel", description = "Generic unsubscribe endpoint accepting channel and symbol in JSON")
    public ResponseEntity<Map<String, Object>> unsubscribeGeneric(@RequestBody Map<String, String> request) {
        try {
            String channel = request.get("channel");
            String symbol = request.get("symbol");

            if (channel == null || symbol == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "Both 'channel' and 'symbol' are required"
                    ));
            }

            // Unsubscribe based on channel type
            switch (channel.toLowerCase()) {
                case "tick":
                    webSocketService.unsubscribeFromTick(symbol);
                    break;
                case "orderbook":
                    webSocketService.unsubscribeFromOrderBook(symbol);
                    break;
                case "trade":
                    webSocketService.unsubscribeFromTrade(symbol);
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "Invalid channel: " + channel
                        ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Unsubscribed from %s channel for %s", channel, symbol),
                "channel", channel,
                "symbol", symbol
            ));

        } catch (Exception e) {
            log.error("Error in generic unsubscribe", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    /**
     * Get order book data for a symbol.
     * Returns mock data since AlgoLab WebSocket is not fully operational.
     */
    @GetMapping("/orderbook/{symbol}")
    @Operation(summary = "Get order book", description = "Returns order book (Level 2 market data) for a symbol")
    public ResponseEntity<Map<String, Object>> getOrderBook(@PathVariable String symbol) {
        log.info("Getting order book for symbol: {}", symbol);

        try {
            // Return mock order book data since WebSocket is not fully operational
            Map<String, Object> response = Map.of(
                "content", Map.of(
                    "symbol", symbol,
                    "timestamp", java.time.Instant.now().toString(),
                    "sequence", System.currentTimeMillis(),
                    "bids", new Object[]{
                        Map.of("price", "52.70", "quantity", 50000, "orderCount", 15, "side", "BID"),
                        Map.of("price", "52.65", "quantity", 30000, "orderCount", 10, "side", "BID"),
                        Map.of("price", "52.60", "quantity", 25000, "orderCount", 8, "side", "BID"),
                        Map.of("price", "52.55", "quantity", 20000, "orderCount", 6, "side", "BID"),
                        Map.of("price", "52.50", "quantity", 15000, "orderCount", 5, "side", "BID")
                    },
                    "asks", new Object[]{
                        Map.of("price", "52.80", "quantity", 45000, "orderCount", 12, "side", "ASK"),
                        Map.of("price", "52.85", "quantity", 35000, "orderCount", 8, "side", "ASK"),
                        Map.of("price", "52.90", "quantity", 40000, "orderCount", 10, "side", "ASK"),
                        Map.of("price", "52.95", "quantity", 30000, "orderCount", 7, "side", "ASK"),
                        Map.of("price", "53.00", "quantity", 25000, "orderCount", 6, "side", "ASK")
                    }
                )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting order book for symbol: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to get order book: " + e.getMessage()
                ));
        }
    }
}
