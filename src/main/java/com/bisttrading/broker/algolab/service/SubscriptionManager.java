package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.websocket.AlgoLabWebSocketClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages WebSocket subscriptions and handles reconnection scenarios.
 * Automatically restores subscriptions after reconnect.
 */
@Service
@Slf4j
public class SubscriptionManager {

    private final Set<Subscription> activeSubscriptions = ConcurrentHashMap.newKeySet();
    private final AlgoLabWebSocketClient webSocketClient;

    public SubscriptionManager(AlgoLabWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        log.info("Subscription Manager initialized");
    }

    /**
     * Subscribe to a symbol on a specific channel.
     */
    public void subscribe(String symbol, String channel) {
        Subscription sub = new Subscription(symbol, channel, Instant.now());

        if (activeSubscriptions.add(sub)) {
            log.info("New subscription registered: {} on channel {}", symbol, channel);

            // Send subscription to WebSocket
            try {
                webSocketClient.subscribe(channel, symbol);
            } catch (Exception e) {
                log.error("Failed to send subscription for {}", symbol, e);
                activeSubscriptions.remove(sub);
                throw new RuntimeException("Failed to subscribe", e);
            }
        } else {
            log.debug("Subscription already exists: {} on channel {}", symbol, channel);
        }
    }

    /**
     * Unsubscribe from a symbol.
     */
    public void unsubscribe(String symbol, String channel) {
        Subscription sub = new Subscription(symbol, channel, null);

        if (activeSubscriptions.remove(sub)) {
            log.info("Subscription removed: {} from channel {}", symbol, channel);

            try {
                webSocketClient.unsubscribe(channel, symbol);
            } catch (Exception e) {
                log.error("Failed to send unsubscribe for {}", symbol, e);
            }
        }
    }

    /**
     * Get all active subscriptions.
     */
    public Set<Subscription> getActiveSubscriptions() {
        return Set.copyOf(activeSubscriptions);
    }

    /**
     * Get active symbols (unique list).
     */
    public Set<String> getActiveSymbols() {
        return activeSubscriptions.stream()
            .map(Subscription::getSymbol)
            .collect(Collectors.toSet());
    }

    /**
     * Clear all subscriptions (without unsubscribing from WebSocket).
     */
    public void clearAll() {
        int count = activeSubscriptions.size();
        activeSubscriptions.clear();
        log.info("Cleared all {} subscriptions", count);
    }

    /**
     * Restore all subscriptions after reconnect.
     * Called automatically by WebSocket lifecycle events.
     */
    public void restoreSubscriptions() {
        if (activeSubscriptions.isEmpty()) {
            log.info("No subscriptions to restore");
            return;
        }

        log.info("Restoring {} subscriptions after reconnect", activeSubscriptions.size());

        int successCount = 0;
        int failCount = 0;

        for (Subscription sub : activeSubscriptions) {
            try {
                webSocketClient.subscribe(sub.getChannel(), sub.getSymbol());
                successCount++;
                log.info("✅ Restored subscription: {} on channel {}", sub.getSymbol(), sub.getChannel());
            } catch (Exception e) {
                failCount++;
                log.error("❌ Failed to restore subscription: {} on channel {}", sub.getSymbol(), sub.getChannel(), e);
            }
        }

        log.info("Subscription restore complete: {} success, {} failed", successCount, failCount);
    }

    /**
     * Subscribe to ALL symbols (AlgoLab special feature).
     */
    public void subscribeToAll(String channel) {
        Subscription allSub = new Subscription("ALL", channel, Instant.now());

        if (activeSubscriptions.add(allSub)) {
            log.info("Subscribing to ALL symbols on channel {}", channel);

            try {
                webSocketClient.subscribe(channel, "ALL");
                log.info("✅ Successfully subscribed to ALL symbols");
            } catch (Exception e) {
                log.error("❌ Failed to subscribe to ALL symbols", e);
                activeSubscriptions.remove(allSub);
                throw new RuntimeException("Failed to subscribe to ALL", e);
            }
        } else {
            log.debug("ALL subscription already exists on channel {}", channel);
        }
    }

    /**
     * Check if subscribed to ALL symbols.
     */
    public boolean isSubscribedToAll() {
        return activeSubscriptions.stream()
            .anyMatch(sub -> "ALL".equalsIgnoreCase(sub.getSymbol()));
    }

    /**
     * Get subscription count.
     */
    public int getSubscriptionCount() {
        return activeSubscriptions.size();
    }

    /**
     * Event listener for WebSocket reconnection.
     * Automatically restores subscriptions.
     */
    @EventListener
    public void onWebSocketReconnected(WebSocketReconnectedEvent event) {
        log.info("Received WebSocket reconnected event. Triggering subscription restore...");
        restoreSubscriptions();
    }

    /**
     * Subscription value object.
     */
    @Data
    public static class Subscription {
        private final String symbol;
        private final String channel;
        private final Instant subscribedAt;

        public Subscription(String symbol, String channel, Instant subscribedAt) {
            this.symbol = symbol;
            this.channel = channel;
            this.subscribedAt = subscribedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Subscription that = (Subscription) o;
            return symbol.equals(that.symbol) && channel.equals(that.channel);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(symbol, channel);
        }

        @Override
        public String toString() {
            return String.format("%s:%s", channel, symbol);
        }
    }

    /**
     * Event fired when WebSocket reconnects.
     */
    public static class WebSocketReconnectedEvent {
        private final Instant timestamp;

        public WebSocketReconnectedEvent() {
            this.timestamp = Instant.now();
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
