package com.bisttrading.broker.websocket;

import com.bisttrading.broker.algolab.session.SessionManager;
import com.bisttrading.broker.websocket.event.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceManager {

    private final AlgoLabWebSocketClient webSocketClient;
    private final SessionManager sessionManager;
    private final WebSocketEventPublisher eventPublisher;

    @Value("${algolab.websocket.auto-connect:true}")
    private boolean autoConnect;

    @Value("${algolab.websocket.auto-subscribe.market-data:}")
    private List<String> autoSubscribeSymbols;

    // Track active subscriptions
    private final Set<String> marketDataSubscriptions = ConcurrentHashMap.newKeySet();
    private final Set<String> orderUpdateSubscriptions = ConcurrentHashMap.newKeySet();
    private final Set<String> portfolioUpdateSubscriptions = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initialize() {
        if (autoConnect) {
            log.info("Auto-connecting WebSocket service");
            connectWithCurrentSession();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket service");
        webSocketClient.disconnect();
    }

    public CompletableFuture<Boolean> connectWithCurrentSession() {
        if (!sessionManager.isAuthenticated()) {
            log.warn("Cannot connect WebSocket: No active session");
            return CompletableFuture.completedFuture(false);
        }

        String token = sessionManager.getCurrentToken();
        return webSocketClient.connect(token)
            .thenCompose(connected -> {
                if (connected && autoSubscribeSymbols != null && !autoSubscribeSymbols.isEmpty()) {
                    return autoSubscribeToMarketData();
                }
                return CompletableFuture.completedFuture(connected);
            });
    }

    private CompletableFuture<Boolean> autoSubscribeToMarketData() {
        log.info("Auto-subscribing to market data for {} symbols", autoSubscribeSymbols.size());

        List<CompletableFuture<Boolean>> subscriptionFutures = autoSubscribeSymbols.stream()
            .map(this::subscribeToMarketData)
            .toList();

        return CompletableFuture.allOf(subscriptionFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> subscriptionFutures.stream()
                .mapToInt(future -> future.join() ? 1 : 0)
                .sum() > 0);
    }

    // Market Data Subscriptions
    public CompletableFuture<Boolean> subscribeToMarketData(String symbol) {
        if (marketDataSubscriptions.contains(symbol)) {
            log.debug("Already subscribed to market data for symbol: {}", symbol);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.subscribeToMarketData(symbol)
            .thenApply(success -> {
                if (success) {
                    marketDataSubscriptions.add(symbol);
                    log.info("Successfully subscribed to market data for symbol: {}", symbol);
                } else {
                    log.error("Failed to subscribe to market data for symbol: {}", symbol);
                }
                return success;
            });
    }

    public CompletableFuture<Boolean> unsubscribeFromMarketData(String symbol) {
        if (!marketDataSubscriptions.contains(symbol)) {
            log.debug("Not subscribed to market data for symbol: {}", symbol);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.unsubscribeFromMarketData(symbol)
            .thenApply(success -> {
                if (success) {
                    marketDataSubscriptions.remove(symbol);
                    log.info("Successfully unsubscribed from market data for symbol: {}", symbol);
                } else {
                    log.error("Failed to unsubscribe from market data for symbol: {}", symbol);
                }
                return success;
            });
    }

    public CompletableFuture<Boolean> subscribeToMultipleSymbols(List<String> symbols) {
        List<CompletableFuture<Boolean>> futures = symbols.stream()
            .map(this::subscribeToMarketData)
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                long successCount = futures.stream()
                    .mapToLong(future -> future.join() ? 1 : 0)
                    .sum();

                log.info("Subscribed to market data for {}/{} symbols", successCount, symbols.size());
                return successCount > 0;
            });
    }

    // Order Update Subscriptions
    public CompletableFuture<Boolean> subscribeToOrderUpdates(String userId) {
        if (orderUpdateSubscriptions.contains(userId)) {
            log.debug("Already subscribed to order updates for user: {}", userId);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.subscribeToOrderUpdates(userId)
            .thenApply(success -> {
                if (success) {
                    orderUpdateSubscriptions.add(userId);
                    log.info("Successfully subscribed to order updates for user: {}", userId);
                } else {
                    log.error("Failed to subscribe to order updates for user: {}", userId);
                }
                return success;
            });
    }

    public CompletableFuture<Boolean> unsubscribeFromOrderUpdates(String userId) {
        if (!orderUpdateSubscriptions.contains(userId)) {
            log.debug("Not subscribed to order updates for user: {}", userId);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.unsubscribeFromOrderUpdates(userId)
            .thenApply(success -> {
                if (success) {
                    orderUpdateSubscriptions.remove(userId);
                    log.info("Successfully unsubscribed from order updates for user: {}", userId);
                } else {
                    log.error("Failed to unsubscribe from order updates for user: {}", userId);
                }
                return success;
            });
    }

    // Portfolio Update Subscriptions
    public CompletableFuture<Boolean> subscribeToPortfolioUpdates(String userId) {
        if (portfolioUpdateSubscriptions.contains(userId)) {
            log.debug("Already subscribed to portfolio updates for user: {}", userId);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.subscribeToPortfolioUpdates(userId)
            .thenApply(success -> {
                if (success) {
                    portfolioUpdateSubscriptions.add(userId);
                    log.info("Successfully subscribed to portfolio updates for user: {}", userId);
                } else {
                    log.error("Failed to subscribe to portfolio updates for user: {}", userId);
                }
                return success;
            });
    }

    public CompletableFuture<Boolean> unsubscribeFromPortfolioUpdates(String userId) {
        if (!portfolioUpdateSubscriptions.contains(userId)) {
            log.debug("Not subscribed to portfolio updates for user: {}", userId);
            return CompletableFuture.completedFuture(true);
        }

        return webSocketClient.unsubscribeFromPortfolioUpdates(userId)
            .thenApply(success -> {
                if (success) {
                    portfolioUpdateSubscriptions.remove(userId);
                    log.info("Successfully unsubscribed from portfolio updates for user: {}", userId);
                } else {
                    log.error("Failed to unsubscribe from portfolio updates for user: {}", userId);
                }
                return success;
            });
    }

    // Trade Subscriptions
    public CompletableFuture<Boolean> subscribeToTrades(String symbol) {
        return webSocketClient.subscribeToTrades(symbol)
            .thenApply(success -> {
                if (success) {
                    log.info("Successfully subscribed to trades for symbol: {}", symbol);
                } else {
                    log.error("Failed to subscribe to trades for symbol: {}", symbol);
                }
                return success;
            });
    }

    public CompletableFuture<Boolean> unsubscribeFromTrades(String symbol) {
        return webSocketClient.unsubscribeFromTrades(symbol)
            .thenApply(success -> {
                if (success) {
                    log.info("Successfully unsubscribed from trades for symbol: {}", symbol);
                } else {
                    log.error("Failed to unsubscribe from trades for symbol: {}", symbol);
                }
                return success;
            });
    }

    // User session management
    public CompletableFuture<Boolean> subscribeUserToAllUpdates(String userId) {
        log.info("Subscribing user to all updates: {}", userId);

        CompletableFuture<Boolean> orderUpdates = subscribeToOrderUpdates(userId);
        CompletableFuture<Boolean> portfolioUpdates = subscribeToPortfolioUpdates(userId);

        return CompletableFuture.allOf(orderUpdates, portfolioUpdates)
            .thenApply(v -> {
                boolean orderSuccess = orderUpdates.join();
                boolean portfolioSuccess = portfolioUpdates.join();

                log.info("User subscription results for {}: orders={} portfolio={}",
                        userId, orderSuccess, portfolioSuccess);

                return orderSuccess && portfolioSuccess;
            });
    }

    public CompletableFuture<Boolean> unsubscribeUserFromAllUpdates(String userId) {
        log.info("Unsubscribing user from all updates: {}", userId);

        CompletableFuture<Boolean> orderUpdates = unsubscribeFromOrderUpdates(userId);
        CompletableFuture<Boolean> portfolioUpdates = unsubscribeFromPortfolioUpdates(userId);

        return CompletableFuture.allOf(orderUpdates, portfolioUpdates)
            .thenApply(v -> {
                boolean orderSuccess = orderUpdates.join();
                boolean portfolioSuccess = portfolioUpdates.join();

                log.info("User unsubscription results for {}: orders={} portfolio={}",
                        userId, orderSuccess, portfolioSuccess);

                return orderSuccess || portfolioSuccess;
            });
    }

    // Status methods
    public boolean isConnected() {
        return webSocketClient.isConnected();
    }

    public Set<String> getMarketDataSubscriptions() {
        return Set.copyOf(marketDataSubscriptions);
    }

    public Set<String> getOrderUpdateSubscriptions() {
        return Set.copyOf(orderUpdateSubscriptions);
    }

    public Set<String> getPortfolioUpdateSubscriptions() {
        return Set.copyOf(portfolioUpdateSubscriptions);
    }

    public WebSocketConnectionStatus getConnectionStatus() {
        return WebSocketConnectionStatus.builder()
            .connected(isConnected())
            .marketDataSubscriptions(marketDataSubscriptions.size())
            .orderUpdateSubscriptions(orderUpdateSubscriptions.size())
            .portfolioUpdateSubscriptions(portfolioUpdateSubscriptions.size())
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class WebSocketConnectionStatus {
        private boolean connected;
        private int marketDataSubscriptions;
        private int orderUpdateSubscriptions;
        private int portfolioUpdateSubscriptions;
    }
}