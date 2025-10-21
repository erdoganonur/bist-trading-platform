package com.bisttrading.broker.algolab.websocket;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.dto.websocket.WebSocketMessage;
import com.bisttrading.broker.algolab.dto.websocket.TickData;
import com.bisttrading.broker.algolab.dto.websocket.OrderBookData;
import com.bisttrading.broker.algolab.dto.websocket.TradeData;
import com.bisttrading.broker.algolab.exception.AlgoLabException;
import com.bisttrading.broker.algolab.service.WebSocketMessageBuffer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket client for AlgoLab API.
 * Handles connection, reconnection, message parsing, and heartbeat.
 */
@Component
@Slf4j
public class AlgoLabWebSocketClient extends TextWebSocketHandler {

    private final AlgoLabProperties properties;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient webSocketClient;
    private final ScheduledExecutorService scheduler;
    private final WebSocketMessageBuffer messageBuffer;

    private volatile WebSocketSession session;
    private volatile String authToken;
    private volatile String authHash;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> reconnectTask;

    // Message handlers
    private final Map<String, MessageHandler<?>> messageHandlers = new ConcurrentHashMap<>();

    // Track subscribed symbols per channel (Type)
    // Key: Type (T/D/O), Value: Set of symbols
    private final Map<String, Set<String>> subscribedSymbols = new ConcurrentHashMap<>();

    public AlgoLabWebSocketClient(
            AlgoLabProperties properties,
            ObjectMapper objectMapper,
            WebSocketMessageBuffer messageBuffer) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.messageBuffer = messageBuffer;
        this.webSocketClient = new StandardWebSocketClient();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "algolab-ws-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Connects to AlgoLab WebSocket server.
     */
    public CompletableFuture<Void> connect(String token, String hash) {
        if (connected.get()) {
            log.warn("Already connected to WebSocket");
            return CompletableFuture.completedFuture(null);
        }

        this.authToken = token;
        this.authHash = hash;

        log.info("Connecting to AlgoLab WebSocket: {}", properties.getWebsocket().getUrl());

        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            URI uri = new URI(properties.getWebsocket().getUrl());
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

            // Add AlgoLab authentication headers (from Python example)
            String apiKey = properties.getApi().getKey();
            String hostname = properties.getApi().getHostname();

            // Calculate Checker: SHA-256(apiKey + hostname + "/ws")
            String checkerData = apiKey + hostname + "/ws";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(checkerData.getBytes(StandardCharsets.UTF_8));
            StringBuilder checker = new StringBuilder();
            for (byte b : hashBytes) {
                checker.append(String.format("%02x", b));
            }

            headers.add("APIKEY", apiKey);
            headers.add("Authorization", hash);
            headers.add("Checker", checker.toString());

            log.debug("Added AlgoLab WebSocket headers - APIKEY, Authorization, Checker");

            webSocketClient.doHandshake(this, headers, uri).addCallback(
                result -> {
                    if (result != null) {
                        this.session = result;
                        connected.set(true);
                        reconnectAttempts.set(0);
                        log.info("WebSocket connected successfully");

                        // Don't send initial auth message - token is sent with subscription
                        // Python example only sends token when subscribing to channels
                        authenticated.set(true);

                        future.complete(null);
                    }
                },
                ex -> {
                    log.error("Failed to connect to WebSocket", ex);
                    connected.set(false);
                    scheduleReconnect();
                    future.completeExceptionally(ex);
                }
            );

        } catch (Exception e) {
            log.error("Error initiating WebSocket connection", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Disconnects from WebSocket server.
     */
    public void disconnect() {
        log.info("Disconnecting from AlgoLab WebSocket");

        stopHeartbeat();
        cancelReconnect();

        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.error("Error closing WebSocket session", e);
            }
        }

        connected.set(false);
        authenticated.set(false);
        session = null;
    }

    /**
     * Sends authentication message to server.
     */
    private void sendAuthMessage() {
        try {
            WebSocketMessage<Map<String, String>> authMsg = WebSocketMessage.<Map<String, String>>builder()
                .type(WebSocketMessage.Type.AUTH)
                .data(Map.of(
                    "token", authToken,
                    "hash", authHash
                ))
                .build();

            sendMessage(authMsg);
            log.debug("Authentication message sent");

        } catch (Exception e) {
            log.error("Failed to send authentication message", e);
        }
    }

    /**
     * Sends a message to the WebSocket server.
     */
    public <T> void sendMessage(WebSocketMessage<T> message) throws IOException {
        if (session == null || !session.isOpen()) {
            throw new AlgoLabException("WebSocket session is not open");
        }

        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
        log.debug("Sent WebSocket message: type={}, channel={}", message.getType(), message.getChannel());
    }

    /**
     * Subscribes to a channel.
     * AlgoLab format: {"token": "hash", "Type": "T|D|O", "Symbols": ["SYMBOL1", "SYMBOL2"]}
     * Type: T=Tick, D=Depth (OrderBook), O=Order Status
     *
     * IMPORTANT: AlgoLab replaces subscriptions on each call, so we accumulate all symbols
     * and send them together in one subscription message.
     *
     * If "ALL" is already subscribed, no need to subscribe to individual symbols
     * as "ALL" provides real-time data for all market symbols.
     */
    public void subscribe(String channel, String symbol) throws IOException {
        if (session == null || !session.isOpen()) {
            throw new AlgoLabException("WebSocket session is not open");
        }

        // Map channel to AlgoLab type
        String type = switch (channel.toLowerCase()) {
            case "tick" -> "T";
            case "orderbook", "depth" -> "D";
            case "order", "orders" -> "O";
            default -> "T"; // Default to tick
        };

        // Check if "ALL" is already subscribed for this type
        Set<String> currentSymbols = subscribedSymbols.get(type);
        if (currentSymbols != null && currentSymbols.contains("ALL")) {
            log.info("✅ Already subscribed to ALL symbols (Type={}). Symbol {} is already included.", type, symbol);
            return; // No need to re-subscribe
        }

        // Add symbol to the subscribed set for this type
        subscribedSymbols.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(symbol);

        // Get all symbols for this type
        Set<String> allSymbols = subscribedSymbols.get(type);

        // AlgoLab format: {"token": hash, "Type": "T", "Symbols": ["SYM1", "SYM2", ...]}
        // CRITICAL: Send ALL subscribed symbols, not just the new one!
        Map<String, Object> subscribeMsg = Map.of(
            "token", authHash,
            "Type", type,
            "Symbols", allSymbols.toArray(new String[0])
        );

        String json = objectMapper.writeValueAsString(subscribeMsg);
        session.sendMessage(new TextMessage(json));

        log.info("✅ Subscribed to AlgoLab channel: Type={}, Symbols={} (total: {})",
            type, symbol, allSymbols.size());
    }

    /**
     * Unsubscribes from a channel.
     */
    public void unsubscribe(String channel, String symbol) throws IOException {
        WebSocketMessage<Map<String, String>> msg = WebSocketMessage.<Map<String, String>>builder()
            .type(WebSocketMessage.Type.UNSUBSCRIBE)
            .channel(channel)
            .data(Map.of("symbol", symbol))
            .build();

        sendMessage(msg);
        log.info("Unsubscribed from channel: {} for symbol: {}", channel, symbol);
    }

    /**
     * Registers a message handler for a specific message type.
     */
    public <T> void registerHandler(String messageType, MessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
        log.debug("Registered handler for message type: {}", messageType);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established");
        this.session = session;
        connected.set(true);
        reconnectAttempts.set(0);

        // Start heartbeat
        startHeartbeat();

        // CRITICAL: Send subscription immediately (like Python example)
        // AlgoLab closes connection if no subscription is sent quickly
        try {
            log.info("Sending IMMEDIATE subscription to prevent connection close");

            // Map channel to AlgoLab type
            String type = "T"; // Tick data

            // Track "ALL" as subscribed symbol
            subscribedSymbols.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add("ALL");

            // AlgoLab format: {"token": hash, "Type": "T", "Symbols": ["ALL"]}
            // Using "ALL" like Python example for instant data flow
            Map<String, Object> subscribeMsg = Map.of(
                "token", authHash,
                "Type", type,
                "Symbols", new String[]{"ALL"}
            );

            String json = objectMapper.writeValueAsString(subscribeMsg);
            session.sendMessage(new TextMessage(json));

            log.info("✅ IMMEDIATE subscription sent: Type={}, Symbols=ALL (all market data)", type);

        } catch (Exception e) {
            log.error("❌ Failed to send immediate subscription", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("Received WebSocket message: {}", payload);

            // AlgoLab uses different format: {"Type":"T","Content":{...}}
            // Parse as Map first to check format
            Map<String, Object> rawMessage = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});

            // Check if this is AlgoLab format (has "Type" and "Content")
            if (rawMessage.containsKey("Type") && rawMessage.containsKey("Content")) {
                handleAlgoLabMessage(rawMessage);
            } else {
                // Generic WebSocket format (our format)
                WebSocketMessage<?> wsMessage = objectMapper.readValue(payload, new TypeReference<WebSocketMessage<Object>>() {});
                handleMessage(wsMessage, payload);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    /**
     * Handles AlgoLab-specific message format.
     * Format: {"Type":"T|D|O","Content":{...}}
     */
    private void handleAlgoLabMessage(Map<String, Object> message) {
        String type = (String) message.get("Type");
        Object content = message.get("Content");

        try {
            switch (type) {
                case "T": // Tick data
                    TickData tickData = objectMapper.convertValue(content, TickData.class);
                    log.info("✅ Received TICK data: Symbol={}, Price={}", tickData.getSymbol(), tickData.getLastPrice());

                    // Buffer the message for HTTP polling
                    messageBuffer.addTick(tickData.getSymbol(), tickData);

                    // Notify registered handlers
                    notifyHandler(WebSocketMessage.Type.TICK, tickData);
                    break;

                case "D": // Depth/OrderBook data
                    OrderBookData orderBookData = objectMapper.convertValue(content, OrderBookData.class);
                    log.info("✅ Received DEPTH data: Symbol={}", orderBookData.getSymbol());

                    // Buffer the message for HTTP polling
                    messageBuffer.addOrderBook(orderBookData.getSymbol(), orderBookData);

                    // Notify registered handlers
                    notifyHandler(WebSocketMessage.Type.ORDER_BOOK, orderBookData);
                    break;

                case "O": // Order status / Trade data
                    log.info("✅ Received ORDER status data");

                    // Try to parse as trade data
                    try {
                        TradeData tradeData = objectMapper.convertValue(content, TradeData.class);
                        messageBuffer.addTrade(tradeData.getSymbol(), tradeData);
                    } catch (Exception e) {
                        log.debug("Could not parse order status as trade data: {}", e.getMessage());
                    }

                    // Notify registered handlers
                    notifyHandler(WebSocketMessage.Type.TRADE, content);
                    break;

                default:
                    log.debug("Unhandled AlgoLab message type: {}", type);
            }

        } catch (Exception e) {
            log.error("Error processing AlgoLab message type: {}", type, e);
        }
    }

    /**
     * Handles incoming WebSocket message based on type.
     */
    private void handleMessage(WebSocketMessage<?> message, String rawPayload) {
        String type = message.getType();

        try {
            switch (type) {
                case WebSocketMessage.Type.AUTH_SUCCESS:
                    authenticated.set(true);
                    log.info("WebSocket authentication successful");
                    break;

                case WebSocketMessage.Type.AUTH_FAILURE:
                    authenticated.set(false);
                    log.error("WebSocket authentication failed: {}", message.getError());
                    disconnect();
                    break;

                case WebSocketMessage.Type.PONG:
                    log.debug("Received PONG from server");
                    break;

                case WebSocketMessage.Type.ERROR:
                    log.error("Received error from server: {}", message.getError());
                    break;

                case WebSocketMessage.Type.TICK:
                    TickData tickData = objectMapper.convertValue(message.getData(), TickData.class);
                    notifyHandler(type, tickData);
                    break;

                case WebSocketMessage.Type.ORDER_BOOK:
                    OrderBookData orderBookData = objectMapper.convertValue(message.getData(), OrderBookData.class);
                    notifyHandler(type, orderBookData);
                    break;

                case WebSocketMessage.Type.TRADE:
                    TradeData tradeData = objectMapper.convertValue(message.getData(), TradeData.class);
                    notifyHandler(type, tradeData);
                    break;

                default:
                    log.debug("Unhandled message type: {}", type);
                    notifyHandler(type, message.getData());
            }

        } catch (Exception e) {
            log.error("Error processing message type: {}", type, e);
        }
    }

    /**
     * Notifies registered handler for a message type.
     */
    @SuppressWarnings("unchecked")
    private <T> void notifyHandler(String messageType, T data) {
        MessageHandler<T> handler = (MessageHandler<T>) messageHandlers.get(messageType);
        if (handler != null) {
            try {
                handler.handle(data);
            } catch (Exception e) {
                log.error("Error in message handler for type: {}", messageType, e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error", exception);
        connected.set(false);
        authenticated.set(false);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("WebSocket connection closed: {}", status);
        connected.set(false);
        authenticated.set(false);
        stopHeartbeat();

        // Schedule reconnect if enabled
        if (properties.getWebsocket().getReconnect().isEnabled()) {
            scheduleReconnect();
        }
    }

    /**
     * Starts heartbeat/ping mechanism.
     */
    private void startHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        long interval = properties.getWebsocket().getHeartbeatInterval();

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session != null && session.isOpen()) {
                    WebSocketMessage<?> pingMsg = WebSocketMessage.builder()
                        .type(WebSocketMessage.Type.PING)
                        .build();
                    sendMessage(pingMsg);
                    log.debug("Sent PING to server");
                }
            } catch (Exception e) {
                log.error("Error sending heartbeat", e);
            }
        }, interval, interval, TimeUnit.MILLISECONDS);

        log.debug("Heartbeat started with interval: {}ms", interval);
    }

    /**
     * Stops heartbeat mechanism.
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
            log.debug("Heartbeat stopped");
        }
    }

    /**
     * Schedules reconnection with exponential backoff.
     */
    private void scheduleReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            log.debug("Reconnect already scheduled");
            return;
        }

        var reconnectConfig = properties.getWebsocket().getReconnect();
        int attempts = reconnectAttempts.incrementAndGet();

        // Check max attempts
        if (reconnectConfig.getMaxAttempts() > 0 && attempts > reconnectConfig.getMaxAttempts()) {
            log.error("Max reconnect attempts ({}) reached. Giving up.", reconnectConfig.getMaxAttempts());
            return;
        }

        // Calculate delay with exponential backoff
        long delay = (long) Math.min(
            reconnectConfig.getInitialDelay() * Math.pow(reconnectConfig.getMultiplier(), attempts - 1),
            reconnectConfig.getMaxDelay()
        );

        log.info("Scheduling reconnect attempt {} in {}ms", attempts, delay);

        reconnectTask = scheduler.schedule(() -> {
            log.info("Attempting to reconnect (attempt {})", attempts);
            connect(authToken, authHash);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels scheduled reconnect.
     */
    private void cancelReconnect() {
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    /**
     * Checks if WebSocket is connected.
     */
    public boolean isConnected() {
        return connected.get() && session != null && session.isOpen();
    }

    /**
     * Checks if WebSocket is authenticated.
     */
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    /**
     * Gets WebSocket URL.
     */
    public String getWebSocketUrl() {
        return properties.getWebsocket().getUrl();
    }

    /**
     * Gets last heartbeat timestamp.
     */
    public String getLastHeartbeat() {
        // TODO: Track last heartbeat time
        return connected.get() ? java.time.Instant.now().toString() : null;
    }

    /**
     * Gets message count.
     */
    public long getMessageCount() {
        // TODO: Track message count
        return 0;
    }

    /**
     * Gets last error.
     */
    public String getLastError() {
        // TODO: Track last error
        return null;
    }

    /**
     * Cleanup on bean destruction.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up WebSocket client");
        disconnect();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Functional interface for message handlers.
     */
    @FunctionalInterface
    public interface MessageHandler<T> {
        void handle(T data);
    }
}
