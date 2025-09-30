package com.bisttrading.broker.websocket.handler;

import com.bisttrading.broker.service.event.OrderEventPublisher;
import com.bisttrading.broker.service.event.PortfolioEventPublisher;
import com.bisttrading.broker.websocket.event.WebSocketEventPublisher;
import com.bisttrading.broker.websocket.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageHandler {

    private final OrderEventPublisher orderEventPublisher;
    private final PortfolioEventPublisher portfolioEventPublisher;
    private final WebSocketEventPublisher webSocketEventPublisher;

    public void handleMessage(WebSocketMessage message) {
        try {
            log.debug("Processing WebSocket message type: {} channel: {}",
                     message.getType(), message.getChannel());

            switch (message) {
                case MarketDataMessage marketData -> handleMarketDataMessage(marketData);
                case OrderUpdateMessage orderUpdate -> handleOrderUpdateMessage(orderUpdate);
                case PortfolioUpdateMessage portfolioUpdate -> handlePortfolioUpdateMessage(portfolioUpdate);
                case TradeMessage trade -> handleTradeMessage(trade);
                case HeartbeatMessage heartbeat -> handleHeartbeatMessage(heartbeat);
                case ErrorMessage error -> handleErrorMessage(error);
                case SubscriptionConfirmationMessage confirmation -> handleSubscriptionConfirmation(confirmation);
                case UnsubscriptionConfirmationMessage unsubscription -> handleUnsubscriptionConfirmation(unsubscription);
                default -> log.warn("Unknown WebSocket message type: {}", message.getType());
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", message.getType(), e);
        }
    }

    private void handleMarketDataMessage(MarketDataMessage message) {
        log.debug("Processing market data for symbol: {}", message.getSymbol());

        // Publish market data update event
        webSocketEventPublisher.publishMarketDataUpdate(
            message.getSymbol(),
            message.getData()
        );

        // Additional processing for market data
        if (message.getData() != null) {
            // Cache the latest market data
            // Update related calculations
            // Trigger alerts if needed
            log.debug("Market data updated for {}: price={}, volume={}",
                     message.getSymbol(),
                     message.getData().getLastPrice(),
                     message.getData().getVolume());
        }
    }

    private void handleOrderUpdateMessage(OrderUpdateMessage message) {
        log.info("Processing order update for user: {} order: {} type: {}",
                message.getUserId(), message.getOrder().getOrderId(), message.getUpdateType());

        // Publish order update event based on type
        switch (message.getUpdateType().toLowerCase()) {
            case "status_change" -> {
                if (message.getOrder().getStatus().isFinal()) {
                    if (message.getOrder().isFilled()) {
                        webSocketEventPublisher.publishOrderFilledUpdate(message);
                    } else if (message.getOrder().isCancelled()) {
                        webSocketEventPublisher.publishOrderCancelledUpdate(message);
                    } else if (message.getOrder().isRejected()) {
                        webSocketEventPublisher.publishOrderRejectedUpdate(message);
                    }
                }
            }
            case "fill", "partial_fill" -> {
                webSocketEventPublisher.publishOrderFillUpdate(message);
            }
            case "cancel" -> {
                webSocketEventPublisher.publishOrderCancelledUpdate(message);
            }
            default -> {
                webSocketEventPublisher.publishOrderStatusUpdate(message);
            }
        }
    }

    private void handlePortfolioUpdateMessage(PortfolioUpdateMessage message) {
        log.info("Processing portfolio update for user: {} type: {}",
                message.getUserId(), message.getUpdateType());

        // Publish portfolio update events
        switch (message.getUpdateType().toLowerCase()) {
            case "balance_change" -> {
                if (message.getAccountInfo() != null) {
                    webSocketEventPublisher.publishCashBalanceUpdate(
                        message.getUserId(),
                        message.getAccountInfo().getCashBalance()
                    );
                }
            }
            case "position_change" -> {
                if (message.getPositions() != null) {
                    webSocketEventPublisher.publishPositionUpdate(
                        message.getUserId(),
                        message.getPositions()
                    );
                }
            }
            case "pnl_change" -> {
                if (message.getAccountInfo() != null) {
                    webSocketEventPublisher.publishPnlUpdate(
                        message.getUserId(),
                        message.getAccountInfo().getUnrealizedPnl(),
                        message.getAccountInfo().getRealizedPnl()
                    );
                }
            }
            default -> {
                webSocketEventPublisher.publishPortfolioUpdate(message);
            }
        }
    }

    private void handleTradeMessage(TradeMessage message) {
        log.debug("Processing trade for symbol: {} price: {} quantity: {}",
                 message.getSymbol(), message.getPrice(), message.getQuantity());

        // Publish trade event
        webSocketEventPublisher.publishTradeUpdate(message);

        // Update market statistics
        // Trigger trade-based alerts
        // Update volume-weighted average price (VWAP)
    }

    private void handleHeartbeatMessage(HeartbeatMessage message) {
        log.debug("Received WebSocket heartbeat");
        // Connection is alive - no action needed
        webSocketEventPublisher.publishHeartbeatReceived();
    }

    private void handleErrorMessage(ErrorMessage message) {
        log.error("WebSocket error received: {} - {} ({})",
                 message.getErrorCode(), message.getMessage(), message.getDetails());

        // Publish error event for monitoring and alerting
        webSocketEventPublisher.publishWebSocketError(
            message.getErrorCode(),
            message.getMessage(),
            message.getDetails()
        );

        // Handle specific error types
        switch (message.getErrorCode()) {
            case "AUTH_ERROR" -> {
                log.warn("Authentication error on WebSocket - may need to reconnect");
                // Trigger reconnection with fresh token
            }
            case "SUBSCRIPTION_ERROR" -> {
                log.warn("Subscription error - invalid subscription parameters");
                // Handle subscription failures
            }
            case "RATE_LIMIT" -> {
                log.warn("Rate limit exceeded on WebSocket");
                // Implement backoff strategy
            }
            default -> {
                log.warn("Unknown WebSocket error type: {}", message.getErrorCode());
            }
        }
    }

    private void handleSubscriptionConfirmation(SubscriptionConfirmationMessage message) {
        if ("confirmed".equals(message.getStatus())) {
            log.info("WebSocket subscription confirmed: {} channel: {}",
                    message.getSubscriptionId(), message.getChannel());
        } else {
            log.error("WebSocket subscription failed: {} - {}",
                     message.getSubscriptionId(), message.getMessage());
        }

        webSocketEventPublisher.publishSubscriptionConfirmed(
            message.getSubscriptionId(),
            message.getChannel(),
            "confirmed".equals(message.getStatus())
        );
    }

    private void handleUnsubscriptionConfirmation(UnsubscriptionConfirmationMessage message) {
        if ("confirmed".equals(message.getStatus())) {
            log.info("WebSocket unsubscription confirmed: {} channel: {}",
                    message.getSubscriptionId(), message.getChannel());
        } else {
            log.error("WebSocket unsubscription failed: {} - {}",
                     message.getSubscriptionId(), message.getMessage());
        }

        webSocketEventPublisher.publishUnsubscriptionConfirmed(
            message.getSubscriptionId(),
            message.getChannel(),
            "confirmed".equals(message.getStatus())
        );
    }
}