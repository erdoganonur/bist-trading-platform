package com.bisttrading.broker.service.event;

import com.bisttrading.broker.algolab.model.Position;
import com.bisttrading.broker.service.PortfolioSynchronizationService.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public void publishPortfolioInitialized(PortfolioSnapshot snapshot) {
        PortfolioInitializedEvent event = PortfolioInitializedEvent.builder()
            .userId(snapshot.getUserId())
            .accountId(snapshot.getAccountId())
            .totalEquity(snapshot.getTotalEquity())
            .cashBalance(snapshot.getCashBalance())
            .portfolioValue(snapshot.getPortfolioValue())
            .positionCount(snapshot.getPositions().size())
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PortfolioInitializedEvent for user: {}", snapshot.getUserId());
    }

    @Async
    public void publishCashBalanceChanged(String userId, BigDecimal previousBalance, BigDecimal newBalance) {
        CashBalanceChangedEvent event = CashBalanceChangedEvent.builder()
            .userId(userId)
            .previousBalance(previousBalance)
            .newBalance(newBalance)
            .change(newBalance != null && previousBalance != null ? newBalance.subtract(previousBalance) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published CashBalanceChangedEvent for user: {} change: {}",
                 userId, event.getChange());
    }

    @Async
    public void publishPortfolioValueChanged(String userId, BigDecimal previousValue, BigDecimal newValue) {
        PortfolioValueChangedEvent event = PortfolioValueChangedEvent.builder()
            .userId(userId)
            .previousValue(previousValue)
            .newValue(newValue)
            .change(newValue != null && previousValue != null ? newValue.subtract(previousValue) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PortfolioValueChangedEvent for user: {} change: {}",
                 userId, event.getChange());
    }

    @Async
    public void publishUnrealizedPnlChanged(String userId, BigDecimal previousPnl, BigDecimal newPnl) {
        UnrealizedPnlChangedEvent event = UnrealizedPnlChangedEvent.builder()
            .userId(userId)
            .previousPnl(previousPnl)
            .newPnl(newPnl)
            .change(newPnl != null && previousPnl != null ? newPnl.subtract(previousPnl) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published UnrealizedPnlChangedEvent for user: {} change: {}",
                 userId, event.getChange());
    }

    @Async
    public void publishMarginUsageChanged(String userId, BigDecimal previousMargin, BigDecimal newMargin) {
        MarginUsageChangedEvent event = MarginUsageChangedEvent.builder()
            .userId(userId)
            .previousMargin(previousMargin)
            .newMargin(newMargin)
            .change(newMargin != null && previousMargin != null ? newMargin.subtract(previousMargin) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published MarginUsageChangedEvent for user: {} change: {}",
                 userId, event.getChange());
    }

    @Async
    public void publishPositionOpened(String userId, Position position) {
        PositionOpenedEvent event = PositionOpenedEvent.builder()
            .userId(userId)
            .symbol(position.getSymbol())
            .side(position.getSide())
            .quantity(position.getQuantity())
            .averageCost(position.getAverageCost())
            .marketValue(position.getMarketValue())
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionOpenedEvent for user: {} symbol: {} quantity: {}",
                 userId, position.getSymbol(), position.getQuantity());
    }

    @Async
    public void publishPositionClosed(String userId, Position position) {
        PositionClosedEvent event = PositionClosedEvent.builder()
            .userId(userId)
            .symbol(position.getSymbol())
            .side(position.getSide())
            .quantity(position.getQuantity())
            .averageCost(position.getAverageCost())
            .realizedPnl(position.getRealizedPnl())
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionClosedEvent for user: {} symbol: {} pnl: {}",
                 userId, position.getSymbol(), position.getRealizedPnl());
    }

    @Async
    public void publishPositionQuantityChanged(String userId, String symbol,
                                               Integer previousQuantity, Integer newQuantity) {
        PositionQuantityChangedEvent event = PositionQuantityChangedEvent.builder()
            .userId(userId)
            .symbol(symbol)
            .previousQuantity(previousQuantity)
            .newQuantity(newQuantity)
            .quantityChange(newQuantity - previousQuantity)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionQuantityChangedEvent for user: {} symbol: {} change: {}",
                 userId, symbol, event.getQuantityChange());
    }

    @Async
    public void publishPositionValueChanged(String userId, String symbol,
                                            BigDecimal previousValue, BigDecimal newValue) {
        PositionValueChangedEvent event = PositionValueChangedEvent.builder()
            .userId(userId)
            .symbol(symbol)
            .previousValue(previousValue)
            .newValue(newValue)
            .valueChange(newValue != null && previousValue != null ? newValue.subtract(previousValue) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionValueChangedEvent for user: {} symbol: {} change: {}",
                 userId, symbol, event.getValueChange());
    }

    @Async
    public void publishPositionPnlChanged(String userId, String symbol,
                                          BigDecimal previousPnl, BigDecimal newPnl) {
        PositionPnlChangedEvent event = PositionPnlChangedEvent.builder()
            .userId(userId)
            .symbol(symbol)
            .previousPnl(previousPnl)
            .newPnl(newPnl)
            .pnlChange(newPnl != null && previousPnl != null ? newPnl.subtract(previousPnl) : null)
            .timestamp(Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published PositionPnlChangedEvent for user: {} symbol: {} change: {}",
                 userId, symbol, event.getPnlChange());
    }
}