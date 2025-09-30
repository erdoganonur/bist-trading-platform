package com.bisttrading.marketdata.event;

import com.bisttrading.marketdata.model.MarketData;
import com.bisttrading.marketdata.model.OrderBook;
import com.bisttrading.marketdata.model.TechnicalIndicators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Market Data Event Publisher - Simplified Version
 * Publishes market data related events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish market data update event
     */
    public void publishMarketDataUpdate(MarketData marketData) {
        try {
            MarketDataUpdateEvent event = MarketDataUpdateEvent.builder()
                .marketData(marketData)
                .build();

            applicationEventPublisher.publishEvent(event);
            log.debug("Published MarketDataUpdate event for symbol: {}", marketData.getSymbol());
        } catch (Exception e) {
            log.error("Failed to publish MarketDataUpdate event for symbol: {}",
                     marketData.getSymbol(), e);
        }
    }

    /**
     * Publish technical indicators update event
     */
    public void publishTechnicalIndicatorsUpdate(TechnicalIndicators indicators) {
        try {
            log.debug("Technical indicators update published for symbol: {}", indicators.getSymbol());
            // Event publishing logic would go here
        } catch (Exception e) {
            log.error("Failed to publish TechnicalIndicatorsUpdate event for symbol: {}",
                     indicators.getSymbol(), e);
        }
    }

    /**
     * Publish volume alert event
     */
    public void publishVolumeAlert(String symbol, String alertType, String message) {
        try {
            log.info("Volume alert published for symbol: {} type: {} message: {}",
                    symbol, alertType, message);
            // Event publishing logic would go here
        } catch (Exception e) {
            log.error("Failed to publish VolumeAlert event for symbol: {}", symbol, e);
        }
    }

    /**
     * Publish market status change event
     */
    public void publishMarketStatusChange(String status, String message) {
        try {
            log.info("Market status change published: {} - {}", status, message);
            // Event publishing logic would go here
        } catch (Exception e) {
            log.error("Failed to publish MarketStatusChange event", e);
        }
    }

    /**
     * Publish error event
     */
    public void publishError(String source, String message, Exception exception) {
        try {
            log.error("Market data error published from {}: {}", source, message, exception);
            // Event publishing logic would go here
        } catch (Exception e) {
            log.error("Failed to publish MarketDataError event", e);
        }
    }
}