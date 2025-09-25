package com.bisttrading.broker.websocket;

import com.bisttrading.broker.websocket.handler.WebSocketMessageHandler;
import com.bisttrading.broker.websocket.model.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ClientEndpoint
public class AlgoLabWebSocketClient {

    private final ObjectMapper objectMapper;
    private final WebSocketMessageHandler messageHandler;
    private final ScheduledExecutorService scheduler;

    @Value("${algolab.websocket.url}")
    private String webSocketUrl;

    @Value("${algolab.websocket.reconnect.max-attempts:5}")
    private int maxReconnectAttempts;

    @Value("${algolab.websocket.reconnect.delay-ms:5000}")
    private long reconnectDelayMs;

    @Value("${algolab.websocket.heartbeat.interval-ms:30000}")
    private long heartbeatIntervalMs;

    private Session session;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> subscriptions = new ConcurrentHashMap<>();
    private CompletableFuture<Void> heartbeatTask;

    public AlgoLabWebSocketClient(ObjectMapper objectMapper, WebSocketMessageHandler messageHandler) {
        this.objectMapper = objectMapper;
        this.messageHandler = messageHandler;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public CompletableFuture<Boolean> connect(String authToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Connecting to AlgoLab WebSocket: {}", webSocketUrl);

                WebSocketContainer container = ContainerProvider.getWebSocketContainer();

                // Add auth header
                container.setDefaultMaxSessionIdleTimeout(0);

                URI uri = URI.create(webSocketUrl + "?token=" + authToken);
                session = container.connectToServer(this, uri);

                connected.set(true);
                reconnectAttempts.set(0);

                startHeartbeat();

                log.info("Connected to AlgoLab WebSocket successfully");
                return true;

            } catch (Exception e) {
                log.error("Failed to connect to WebSocket", e);
                connected.set(false);
                scheduleReconnect();
                return false;
            }
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket connection opened. Session ID: {}", session.getId());
        this.session = session;
        connected.set(true);

        // Resubscribe to previous subscriptions
        resubscribe();
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.debug("Received WebSocket message: {}", message);

            WebSocketMessage wsMessage = objectMapper.readValue(message, WebSocketMessage.class);
            messageHandler.handleMessage(wsMessage);

        } catch (Exception e) {
            log.error("Failed to process WebSocket message: {}", message, e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.warn("WebSocket connection closed. Reason: {}", closeReason.getReasonPhrase());
        connected.set(false);
        stopHeartbeat();

        if (closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            scheduleReconnect();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error occurred", throwable);
        connected.set(false);
        scheduleReconnect();
    }

    public boolean isConnected() {
        return connected.get() && session != null && session.isOpen();
    }

    public void disconnect() {
        connected.set(false);
        stopHeartbeat();

        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnect"));
            } catch (IOException e) {
                log.warn("Error closing WebSocket session", e);
            }
        }

        scheduler.shutdown();
    }

    // Subscription methods
    public CompletableFuture<Boolean> subscribeToMarketData(String symbol) {
        return subscribe("market_data", symbol, createMarketDataSubscription(symbol));
    }

    public CompletableFuture<Boolean> subscribeToOrderUpdates(String userId) {
        return subscribe("order_updates", userId, createOrderUpdatesSubscription(userId));
    }

    public CompletableFuture<Boolean> subscribeToPortfolioUpdates(String userId) {
        return subscribe("portfolio_updates", userId, createPortfolioUpdatesSubscription(userId));
    }

    public CompletableFuture<Boolean> subscribeToTrades(String symbol) {
        return subscribe("trades", symbol, createTradesSubscription(symbol));
    }

    public CompletableFuture<Boolean> unsubscribeFromMarketData(String symbol) {
        return unsubscribe("market_data", symbol);
    }

    public CompletableFuture<Boolean> unsubscribeFromOrderUpdates(String userId) {
        return unsubscribe("order_updates", userId);
    }

    public CompletableFuture<Boolean> unsubscribeFromPortfolioUpdates(String userId) {
        return unsubscribe("portfolio_updates", userId);
    }

    public CompletableFuture<Boolean> unsubscribeFromTrades(String symbol) {
        return unsubscribe("trades", symbol);
    }

    private CompletableFuture<Boolean> subscribe(String type, String key, String subscriptionMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                log.warn("Cannot subscribe to {} for {}: WebSocket not connected", type, key);
                return false;
            }

            try {
                session.getBasicRemote().sendText(subscriptionMessage);
                subscriptions.put(type + ":" + key, subscriptionMessage);
                log.info("Subscribed to {} for {}", type, key);
                return true;

            } catch (IOException e) {
                log.error("Failed to subscribe to {} for {}", type, key, e);
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> unsubscribe(String type, String key) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                log.warn("Cannot unsubscribe from {} for {}: WebSocket not connected", type, key);
                return false;
            }

            try {
                String unsubMessage = createUnsubscribeMessage(type, key);
                session.getBasicRemote().sendText(unsubMessage);
                subscriptions.remove(type + ":" + key);
                log.info("Unsubscribed from {} for {}", type, key);
                return true;

            } catch (IOException e) {
                log.error("Failed to unsubscribe from {} for {}", type, key, e);
                return false;
            }
        });
    }

    private void resubscribe() {
        if (!subscriptions.isEmpty()) {
            log.info("Resubscribing to {} subscriptions", subscriptions.size());

            subscriptions.values().forEach(subscriptionMessage -> {
                try {
                    session.getBasicRemote().sendText(subscriptionMessage);
                } catch (IOException e) {
                    log.error("Failed to resubscribe: {}", subscriptionMessage, e);
                }
            });
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts.get() >= maxReconnectAttempts) {
            log.error("Max reconnect attempts reached ({}). Giving up.", maxReconnectAttempts);
            return;
        }

        int attempt = reconnectAttempts.incrementAndGet();
        long delay = reconnectDelayMs * attempt; // Exponential backoff

        log.info("Scheduling WebSocket reconnect attempt {} in {} ms", attempt, delay);

        scheduler.schedule(() -> {
            if (!connected.get()) {
                log.info("Attempting WebSocket reconnect #{}", attempt);
                // In real implementation, you'd need to get a fresh auth token
                // connect(authToken);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void startHeartbeat() {
        stopHeartbeat();

        heartbeatTask = CompletableFuture.runAsync(() -> {
            while (isConnected() && !Thread.currentThread().isInterrupted()) {
                try {
                    sendHeartbeat();
                    Thread.sleep(heartbeatIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Heartbeat error", e);
                    break;
                }
            }
        });
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
    }

    private void sendHeartbeat() {
        if (isConnected()) {
            try {
                String heartbeat = createHeartbeatMessage();
                session.getBasicRemote().sendText(heartbeat);
                log.debug("Sent WebSocket heartbeat");
            } catch (IOException e) {
                log.warn("Failed to send heartbeat", e);
            }
        }
    }

    // Message creation methods
    private String createMarketDataSubscription(String symbol) {
        return String.format("""
            {
                "type": "subscribe",
                "channel": "market_data",
                "symbol": "%s",
                "timestamp": %d
            }
            """, symbol, Instant.now().toEpochMilli());
    }

    private String createOrderUpdatesSubscription(String userId) {
        return String.format("""
            {
                "type": "subscribe",
                "channel": "order_updates",
                "user_id": "%s",
                "timestamp": %d
            }
            """, userId, Instant.now().toEpochMilli());
    }

    private String createPortfolioUpdatesSubscription(String userId) {
        return String.format("""
            {
                "type": "subscribe",
                "channel": "portfolio_updates",
                "user_id": "%s",
                "timestamp": %d
            }
            """, userId, Instant.now().toEpochMilli());
    }

    private String createTradesSubscription(String symbol) {
        return String.format("""
            {
                "type": "subscribe",
                "channel": "trades",
                "symbol": "%s",
                "timestamp": %d
            }
            """, symbol, Instant.now().toEpochMilli());
    }

    private String createUnsubscribeMessage(String channel, String key) {
        return String.format("""
            {
                "type": "unsubscribe",
                "channel": "%s",
                "key": "%s",
                "timestamp": %d
            }
            """, channel, key, Instant.now().toEpochMilli());
    }

    private String createHeartbeatMessage() {
        return String.format("""
            {
                "type": "ping",
                "timestamp": %d
            }
            """, Instant.now().toEpochMilli());
    }
}