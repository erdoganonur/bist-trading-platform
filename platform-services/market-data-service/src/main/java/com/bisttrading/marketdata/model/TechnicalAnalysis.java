package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Technical analysis data for a symbol
 */
@Data
@Builder
public class TechnicalAnalysis {

    private String symbol;
    private String timeframe;
    private LocalDateTime timestamp;

    // Moving averages
    private BigDecimal sma20;
    private BigDecimal sma50;
    private BigDecimal sma200;
    private BigDecimal ema12;
    private BigDecimal ema26;

    // MACD
    private BigDecimal macdLine;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;

    // RSI
    private BigDecimal rsi;

    // Bollinger Bands
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;

    // Stochastic
    private BigDecimal stochasticK;
    private BigDecimal stochasticD;

    // Volume indicators
    private BigDecimal volumeMA;
    private BigDecimal obv; // On Balance Volume

    // Support and resistance levels
    private BigDecimal supportLevel1;
    private BigDecimal supportLevel2;
    private BigDecimal resistanceLevel1;
    private BigDecimal resistanceLevel2;

    // Trend analysis
    private String trendDirection; // UP, DOWN, SIDEWAYS
    private String trendStrength;  // STRONG, MODERATE, WEAK

    // Custom indicators (extensible)
    private Map<String, BigDecimal> customIndicators;

    // Analysis summary
    private String overallSignal; // BUY, SELL, HOLD
    private int confidenceScore;   // 0-100
    private String analysis;       // Text analysis

    // Helper methods
    public boolean isBullish() {
        return "BUY".equals(overallSignal) || "UP".equals(trendDirection);
    }

    public boolean isBearish() {
        return "SELL".equals(overallSignal) || "DOWN".equals(trendDirection);
    }

    public boolean isHighConfidence() {
        return confidenceScore >= 70;
    }

    public boolean isValid() {
        return symbol != null && timestamp != null && overallSignal != null;
    }
}