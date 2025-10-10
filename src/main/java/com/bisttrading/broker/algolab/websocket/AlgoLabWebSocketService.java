package com.bisttrading.broker.algolab.websocket;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.dto.websocket.*;
import com.bisttrading.broker.algolab.exception.AlgoLabException;
import com.bisttrading.broker.algolab.model.AlgoLabSession;
import com.bisttrading.broker.algolab.service.AlgoLabSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * High-level WebSocket service for AlgoLab API.
 * Manages subscriptions, data streams, and message handlers.
 */
@Service
@Slf4j
public class AlgoLabWebSocketService {

    private final AlgoLabProperties properties;
    private final AlgoLabWebSocketClient client;
    private final AlgoLabSessionManager sessionManager;

    // Active subscriptions
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    // Data handlers
    private final Map<String, Consumer<TickData>> tickHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<OrderBookData>> orderBookHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<TradeData>> tradeHandlers = new ConcurrentHashMap<>();

    public AlgoLabWebSocketService(
            AlgoLabProperties properties,
            AlgoLabWebSocketClient client,
            AlgoLabSessionManager sessionManager) {
        this.properties = properties;
        this.client = client;
        this.sessionManager = sessionManager;

        // Register message handlers
        setupMessageHandlers();
    }

    /**
     * Auto-connect on startup if enabled.
     */
    @PostConstruct
    public void init() {
        if (!properties.getWebsocket().isEnabled()) {
            log.info("WebSocket is disabled in configuration");
            return;
        }

        if (properties.getWebsocket().isAutoConnect()) {
            log.info("Auto-connect enabled. Attempting to connect WebSocket...");

            // Load session
            AlgoLabSession session = sessionManager.loadSession();
            if (session != null && session.getToken() != null && session.getHash() != null) {
                connectAsync(session.getToken(), session.getHash())
                    .thenAccept(v -> log.info("WebSocket auto-connect successful"))
                    .exceptionally(ex -> {
                        log.error("WebSocket auto-connect failed", ex);
                        return null;
                    });
            } else {
                log.warn("No saved session found. WebSocket auto-connect skipped.");
            }
        }
    }

    /**
     * Connects to WebSocket asynchronously.
     */
    public CompletableFuture<Void> connectAsync(String token, String hash) {
        return client.connect(token, hash);
    }

    /**
     * Disconnects from WebSocket.
     */
    public void disconnect() {
        log.info("Disconnecting WebSocket service");
        client.disconnect();
        clearAllSubscriptions();
    }

    /**
     * Subscribes to tick data for a symbol.
     *
     * @param symbol Symbol to subscribe to
     * @param handler Handler for incoming tick data
     */
    public void subscribeToTick(String symbol, Consumer<TickData> handler) {
        try {
            client.subscribe(WebSocketMessage.Channel.TICK, symbol);
            tickHandlers.put(symbol, handler);
            addSubscription(WebSocketMessage.Channel.TICK, symbol);
            log.info("Subscribed to tick data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to subscribe to tick data for symbol: {}", symbol, e);
            throw new AlgoLabException("Failed to subscribe to tick data", e);
        }
    }

    /**
     * Unsubscribes from tick data for a symbol.
     */
    public void unsubscribeFromTick(String symbol) {
        try {
            client.unsubscribe(WebSocketMessage.Channel.TICK, symbol);
            tickHandlers.remove(symbol);
            removeSubscription(WebSocketMessage.Channel.TICK, symbol);
            log.info("Unsubscribed from tick data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to unsubscribe from tick data for symbol: {}", symbol, e);
        }
    }

    /**
     * Subscribes to order book data for a symbol.
     *
     * @param symbol Symbol to subscribe to
     * @param handler Handler for incoming order book data
     */
    public void subscribeToOrderBook(String symbol, Consumer<OrderBookData> handler) {
        try {
            client.subscribe(WebSocketMessage.Channel.ORDER_BOOK, symbol);
            orderBookHandlers.put(symbol, handler);
            addSubscription(WebSocketMessage.Channel.ORDER_BOOK, symbol);
            log.info("Subscribed to order book data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to subscribe to order book data for symbol: {}", symbol, e);
            throw new AlgoLabException("Failed to subscribe to order book", e);
        }
    }

    /**
     * Unsubscribes from order book data for a symbol.
     */
    public void unsubscribeFromOrderBook(String symbol) {
        try {
            client.unsubscribe(WebSocketMessage.Channel.ORDER_BOOK, symbol);
            orderBookHandlers.remove(symbol);
            removeSubscription(WebSocketMessage.Channel.ORDER_BOOK, symbol);
            log.info("Unsubscribed from order book data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to unsubscribe from order book data for symbol: {}", symbol, e);
        }
    }

    /**
     * Subscribes to trade data for a symbol.
     *
     * @param symbol Symbol to subscribe to
     * @param handler Handler for incoming trade data
     */
    public void subscribeToTrade(String symbol, Consumer<TradeData> handler) {
        try {
            client.subscribe(WebSocketMessage.Channel.TRADE, symbol);
            tradeHandlers.put(symbol, handler);
            addSubscription(WebSocketMessage.Channel.TRADE, symbol);
            log.info("Subscribed to trade data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to subscribe to trade data for symbol: {}", symbol, e);
            throw new AlgoLabException("Failed to subscribe to trade data", e);
        }
    }

    /**
     * Unsubscribes from trade data for a symbol.
     */
    public void unsubscribeFromTrade(String symbol) {
        try {
            client.unsubscribe(WebSocketMessage.Channel.TRADE, symbol);
            tradeHandlers.remove(symbol);
            removeSubscription(WebSocketMessage.Channel.TRADE, symbol);
            log.info("Unsubscribed from trade data for symbol: {}", symbol);

        } catch (IOException e) {
            log.error("Failed to unsubscribe from trade data for symbol: {}", symbol, e);
        }
    }

    /**
     * Subscribes to all data types for a symbol.
     */
    public void subscribeToAll(
            String symbol,
            Consumer<TickData> tickHandler,
            Consumer<OrderBookData> orderBookHandler,
            Consumer<TradeData> tradeHandler) {

        if (tickHandler != null) {
            subscribeToTick(symbol, tickHandler);
        }
        if (orderBookHandler != null) {
            subscribeToOrderBook(symbol, orderBookHandler);
        }
        if (tradeHandler != null) {
            subscribeToTrade(symbol, tradeHandler);
        }

        log.info("Subscribed to all data types for symbol: {}", symbol);
    }

    /**
     * Unsubscribes from all data types for a symbol.
     */
    public void unsubscribeFromAll(String symbol) {
        unsubscribeFromTick(symbol);
        unsubscribeFromOrderBook(symbol);
        unsubscribeFromTrade(symbol);
        log.info("Unsubscribed from all data types for symbol: {}", symbol);
    }

    /**
     * Gets active subscriptions for a channel.
     */
    public Set<String> getSubscriptions(String channel) {
        return subscriptions.getOrDefault(channel, Set.of());
    }

    /**
     * Gets all active subscriptions.
     */
    public Map<String, Set<String>> getAllSubscriptions() {
        return Map.copyOf(subscriptions);
    }

    /**
     * Clears all subscriptions.
     */
    private void clearAllSubscriptions() {
        subscriptions.clear();
        tickHandlers.clear();
        orderBookHandlers.clear();
        tradeHandlers.clear();
        log.debug("All subscriptions cleared");
    }

    /**
     * Adds a subscription to tracking.
     */
    private void addSubscription(String channel, String symbol) {
        subscriptions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(symbol);
    }

    /**
     * Removes a subscription from tracking.
     */
    private void removeSubscription(String channel, String symbol) {
        Set<String> symbols = subscriptions.get(channel);
        if (symbols != null) {
            symbols.remove(symbol);
            if (symbols.isEmpty()) {
                subscriptions.remove(channel);
            }
        }
    }

    /**
     * Sets up message handlers for WebSocket client.
     */
    private void setupMessageHandlers() {
        // Tick data handler
        client.registerHandler(WebSocketMessage.Type.TICK, (TickData data) -> {
            String symbol = data.getSymbol();
            Consumer<TickData> handler = tickHandlers.get(symbol);
            if (handler != null) {
                try {
                    handler.accept(data);
                } catch (Exception e) {
                    log.error("Error in tick data handler for symbol: {}", symbol, e);
                }
            }
        });

        // Order book handler
        client.registerHandler(WebSocketMessage.Type.ORDER_BOOK, (OrderBookData data) -> {
            String symbol = data.getSymbol();
            Consumer<OrderBookData> handler = orderBookHandlers.get(symbol);
            if (handler != null) {
                try {
                    handler.accept(data);
                } catch (Exception e) {
                    log.error("Error in order book handler for symbol: {}", symbol, e);
                }
            }
        });

        // Trade data handler
        client.registerHandler(WebSocketMessage.Type.TRADE, (TradeData data) -> {
            String symbol = data.getSymbol();
            Consumer<TradeData> handler = tradeHandlers.get(symbol);
            if (handler != null) {
                try {
                    handler.accept(data);
                } catch (Exception e) {
                    log.error("Error in trade data handler for symbol: {}", symbol, e);
                }
            }
        });

        log.debug("WebSocket message handlers configured");
    }

    /**
     * Checks if WebSocket is connected.
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Checks if WebSocket is authenticated.
     */
    public boolean isAuthenticated() {
        return client.isAuthenticated();
    }

    /**
     * Gets WebSocket connection status.
     */
    public Map<String, Object> getStatus() {
        return Map.of(
            "connected", isConnected(),
            "authenticated", isAuthenticated(),
            "enabled", properties.getWebsocket().isEnabled(),
            "autoConnect", properties.getWebsocket().isAutoConnect(),
            "activeSubscriptions", subscriptions.size()
        );
    }

    /**
     * Cleanup on bean destruction.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up WebSocket service");
        disconnect();
    }
}
