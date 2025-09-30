package com.bisttrading.broker.websocket.event;

import com.bisttrading.broker.algolab.model.MarketData;
import com.bisttrading.broker.algolab.model.Position;
import com.bisttrading.broker.websocket.model.OrderUpdateMessage;
import com.bisttrading.broker.websocket.model.PortfolioUpdateMessage;
import com.bisttrading.broker.websocket.model.TradeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public void publishMarketDataUpdate(String symbol, MarketData marketData) {
        MarketDataUpdateEvent event = MarketDataUpdateEvent.builder()
            .symbol(symbol)
            .marketData(marketData)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published MarketDataUpdateEvent for symbol: {}", symbol);
    }

    @Async
    public void publishOrderStatusUpdate(OrderUpdateMessage orderUpdate) {
        OrderStatusUpdateEvent event = OrderStatusUpdateEvent.builder()
            .userId(orderUpdate.getUserId())
            .orderId(orderUpdate.getOrder().getOrderId())
            .clientOrderId(orderUpdate.getOrder().getClientOrderId())
            .status(orderUpdate.getOrder().getStatus())
            .updateType(orderUpdate.getUpdateType())
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderStatusUpdateEvent for order: {}", orderUpdate.getOrder().getOrderId());
    }

    @Async
    public void publishOrderFilledUpdate(OrderUpdateMessage orderUpdate) {
        OrderFilledUpdateEvent event = OrderFilledUpdateEvent.builder()
            .userId(orderUpdate.getUserId())
            .orderId(orderUpdate.getOrder().getOrderId())
            .symbol(orderUpdate.getOrder().getSymbol())
            .side(orderUpdate.getOrder().getSide().name())
            .filledQuantity(orderUpdate.getOrder().getFilledQuantity() != null ?
                BigDecimal.valueOf(orderUpdate.getOrder().getFilledQuantity()) : BigDecimal.ZERO)
            .filledPrice(orderUpdate.getOrder().getAveragePrice())
            .totalFilledValue(orderUpdate.getOrder().getTotalValue())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .status(orderUpdate.getOrder().getStatus().name())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderFilledUpdateEvent for order: {}", orderUpdate.getOrder().getOrderId());
    }

    @Async
    public void publishOrderFillUpdate(OrderUpdateMessage orderUpdate) {
        OrderFillUpdateEvent event = OrderFillUpdateEvent.builder()
            .userId(orderUpdate.getUserId())
            .orderId(orderUpdate.getOrder().getOrderId())
            .symbol(orderUpdate.getOrder().getSymbol())
            .quantity(orderUpdate.getOrder().getQuantity() != null ?
                BigDecimal.valueOf(orderUpdate.getOrder().getQuantity()) : BigDecimal.ZERO)
            .filledQuantity(orderUpdate.getOrder().getFilledQuantity() != null ?
                BigDecimal.valueOf(orderUpdate.getOrder().getFilledQuantity()) : BigDecimal.ZERO)
            .price(orderUpdate.getOrder().getAveragePrice())
            .side(orderUpdate.getOrder().getSide().name())
            .fillType(orderUpdate.getUpdateType())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderFillUpdateEvent for order: {}", orderUpdate.getOrder().getOrderId());
    }

    @Async
    public void publishOrderCancelledUpdate(OrderUpdateMessage orderUpdate) {
        OrderCancelledUpdateEvent event = OrderCancelledUpdateEvent.builder()
            .userId(orderUpdate.getUserId())
            .orderId(orderUpdate.getOrder().getOrderId())
            .symbol(orderUpdate.getOrder().getSymbol())
            .reason("Order cancelled by user")
            .status(orderUpdate.getOrder().getStatus().name())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderCancelledUpdateEvent for order: {}", orderUpdate.getOrder().getOrderId());
    }

    @Async
    public void publishOrderRejectedUpdate(OrderUpdateMessage orderUpdate) {
        OrderRejectedUpdateEvent event = OrderRejectedUpdateEvent.builder()
            .userId(orderUpdate.getUserId())
            .orderId(orderUpdate.getOrder().getOrderId())
            .symbol(orderUpdate.getOrder().getSymbol())
            .reason(orderUpdate.getOrder().getRejectReason())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderRejectedUpdateEvent for order: {}", orderUpdate.getOrder().getOrderId());
    }

    @Async
    public void publishCashBalanceUpdate(String userId, BigDecimal newBalance) {
        CashBalanceUpdateEvent event = CashBalanceUpdateEvent.builder()
            .userId(userId)
            .balance(newBalance)
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published CashBalanceUpdateEvent for user: {}", userId);
    }

    @Async
    public void publishPositionUpdate(String userId, List<Position> positions) {
        PositionUpdateEvent event = PositionUpdateEvent.builder()
            .userId(userId)
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionUpdateEvent for user: {} positions: {}", userId, positions.size());
    }

    @Async
    public void publishPnlUpdate(String userId, BigDecimal unrealizedPnl, BigDecimal realizedPnl) {
        PnlUpdateEvent event = PnlUpdateEvent.builder()
            .userId(userId)
            .unrealizedPnl(unrealizedPnl)
            .realizedPnl(realizedPnl)
            .totalPnl(unrealizedPnl.add(realizedPnl))
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PnlUpdateEvent for user: {}", userId);
    }

    @Async
    public void publishPortfolioUpdate(PortfolioUpdateMessage portfolioUpdate) {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
            .userId(portfolioUpdate.getUserId())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PortfolioUpdateEvent for user: {}", portfolioUpdate.getUserId());
    }

    @Async
    public void publishTradeUpdate(TradeMessage tradeMessage) {
        TradeUpdateEvent event = TradeUpdateEvent.builder()
            .symbol(tradeMessage.getSymbol())
            .tradeId(tradeMessage.getTradeId())
            .price(tradeMessage.getPrice())
            .quantity(tradeMessage.getQuantity() != null ?
                BigDecimal.valueOf(tradeMessage.getQuantity()) : BigDecimal.ZERO)
            .side(tradeMessage.getSide())
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published TradeUpdateEvent for symbol: {} trade: {}", tradeMessage.getSymbol(), tradeMessage.getTradeId());
    }

    @Async
    public void publishWebSocketError(String errorCode, String message, String details) {
        WebSocketErrorEvent event = WebSocketErrorEvent.builder()
            .errorCode(errorCode)
            .errorMessage(message)
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published WebSocketErrorEvent: {}", errorCode);
    }

    @Async
    public void publishHeartbeatReceived() {
        HeartbeatReceivedEvent event = HeartbeatReceivedEvent.builder()
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.trace("Published HeartbeatReceivedEvent");
    }

    @Async
    public void publishSubscriptionConfirmed(String subscriptionId, String channel, boolean success) {
        SubscriptionConfirmedEvent event = SubscriptionConfirmedEvent.builder()
            .channel(channel)
            .success(success)
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published SubscriptionConfirmedEvent: {} channel: {} success: {}", subscriptionId, channel, success);
    }

    @Async
    public void publishUnsubscriptionConfirmed(String subscriptionId, String channel, boolean success) {
        UnsubscriptionConfirmedEvent event = UnsubscriptionConfirmedEvent.builder()
            .channel(channel)
            .success(success)
            .timestamp(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published UnsubscriptionConfirmedEvent: {} channel: {} success: {}", subscriptionId, channel, success);
    }
}