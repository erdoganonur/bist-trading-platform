package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.model.MarketData;
import com.bisttrading.marketdata.event.MarketDataEventPublisher;
import com.bisttrading.marketdata.model.TechnicalIndicators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Market Data Alert Service - Simplified Version
 * Monitors market data and generates alerts based on predefined conditions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataAlertService {

    private final MarketDataEventPublisher eventPublisher;

    /**
     * Check price alerts for market data
     */
    public void checkPriceAlerts(MarketData marketData) {
        try {
            // Sample price alert logic
            if (marketData.getChangePercent() != null) {
                BigDecimal changePercent = marketData.getChangePercent();

                if (changePercent.compareTo(BigDecimal.valueOf(5)) > 0) {
                    log.info("High price increase alert for {}: +{}%",
                             marketData.getSymbol(), changePercent);
                    eventPublisher.publishVolumeAlert(
                        marketData.getSymbol(),
                        "PRICE_INCREASE",
                        String.format("Price increased by %.2f%%", changePercent)
                    );
                } else if (changePercent.compareTo(BigDecimal.valueOf(-5)) < 0) {
                    log.info("High price decrease alert for {}: {}%",
                             marketData.getSymbol(), changePercent);
                    eventPublisher.publishVolumeAlert(
                        marketData.getSymbol(),
                        "PRICE_DECREASE",
                        String.format("Price decreased by %.2f%%", changePercent)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to check price alerts for symbol: {}", marketData.getSymbol(), e);
        }
    }

    /**
     * Check volume alerts for market data
     */
    public void checkVolumeAlerts(MarketData marketData) {
        try {
            // Sample volume alert logic
            if (marketData.getVolume() != null) {
                BigDecimal volume = marketData.getVolume();

                // Alert for high volume (simplified logic)
                if (volume.compareTo(BigDecimal.valueOf(1000000)) > 0) {
                    log.info("High volume alert for {}: {}",
                             marketData.getSymbol(), volume);
                    eventPublisher.publishVolumeAlert(
                        marketData.getSymbol(),
                        "HIGH_VOLUME",
                        String.format("High trading volume: %s", volume)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to check volume alerts for symbol: {}", marketData.getSymbol(), e);
        }
    }

    /**
     * Check technical indicators for alerts
     */
    public void checkTechnicalAlerts(TechnicalIndicators indicators) {
        try {
            // Sample technical indicator alerts
            if (indicators.getRsi() != null) {
                BigDecimal rsi = indicators.getRsi();

                if (rsi.compareTo(BigDecimal.valueOf(70)) > 0) {
                    log.info("Overbought alert for {}: RSI = {}",
                             indicators.getSymbol(), rsi);
                } else if (rsi.compareTo(BigDecimal.valueOf(30)) < 0) {
                    log.info("Oversold alert for {}: RSI = {}",
                             indicators.getSymbol(), rsi);
                }
            }

            // Overall signal alerts
            if ("BUY".equals(indicators.getOverallSignal())) {
                log.info("Buy signal alert for {}", indicators.getSymbol());
            } else if ("SELL".equals(indicators.getOverallSignal())) {
                log.info("Sell signal alert for {}", indicators.getSymbol());
            }

        } catch (Exception e) {
            log.error("Failed to check technical alerts for symbol: {}",
                     indicators.getSymbol(), e);
        }
    }

    /**
     * Process all alerts for market data
     */
    public void processAlerts(MarketData marketData) {
        checkPriceAlerts(marketData);
        checkVolumeAlerts(marketData);
    }

    /**
     * Process all technical alerts
     */
    public void processTechnicalAlerts(TechnicalIndicators indicators) {
        checkTechnicalAlerts(indicators);
    }
}