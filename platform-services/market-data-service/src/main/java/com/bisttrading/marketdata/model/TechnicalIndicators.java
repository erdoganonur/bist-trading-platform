package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Technical indicators for a symbol
 */
@Data
@Builder
public class TechnicalIndicators {

    private String symbol;
    private String timeframe;
    private LocalDateTime timestamp;

    // Moving averages
    private BigDecimal sma5;
    private BigDecimal sma10;
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
    private BigDecimal rsi14;

    // Bollinger Bands
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;
    private BigDecimal bollingerWidth;

    // Stochastic
    private BigDecimal stochasticK;
    private BigDecimal stochasticD;

    // Volume indicators
    private BigDecimal volumeMA;
    private BigDecimal obv; // On Balance Volume
    private BigDecimal vwap; // Volume Weighted Average Price

    // Momentum indicators
    private BigDecimal momentum;
    private BigDecimal roc; // Rate of Change
    private BigDecimal williams;

    // Volatility indicators
    private BigDecimal atr; // Average True Range
    private BigDecimal volatility;

    // Custom indicators (extensible)
    private Map<String, BigDecimal> customIndicators;

    // Signals
    private String macdSignal_text;
    private String rsiSignal;
    private String bollingerSignal;
    private String overallSignal; // BUY, SELL, HOLD

    // Helper methods
    public boolean isBullishMACD() {
        return macdLine != null && macdSignal != null &&
               macdLine.compareTo(macdSignal) > 0;
    }

    public boolean isBullishRSI() {
        return rsi != null && rsi.compareTo(BigDecimal.valueOf(30)) > 0 &&
               rsi.compareTo(BigDecimal.valueOf(70)) < 0;
    }

    public boolean isOverbought() {
        return rsi != null && rsi.compareTo(BigDecimal.valueOf(70)) > 0;
    }

    public boolean isOversold() {
        return rsi != null && rsi.compareTo(BigDecimal.valueOf(30)) < 0;
    }

    public boolean isValid() {
        return symbol != null && timestamp != null && timeframe != null;
    }
}