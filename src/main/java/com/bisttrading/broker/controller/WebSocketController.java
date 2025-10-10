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
}
