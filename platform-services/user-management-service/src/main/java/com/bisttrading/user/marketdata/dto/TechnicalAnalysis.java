package com.bisttrading.user.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysis {
    private String symbol;
    private String timeframe;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    // Moving Averages
    private BigDecimal simpleMovingAverage20;
    private BigDecimal simpleMovingAverage50;
    private BigDecimal exponentialMovingAverage12;
    private BigDecimal exponentialMovingAverage26;

    // RSI
    private BigDecimal relativeStrengthIndex;

    // MACD
    private BigDecimal macdLine;
    private BigDecimal signalLine;
    private BigDecimal histogram;

    // Bollinger Bands
    private BigDecimal upperBollingerBand;
    private BigDecimal middleBollingerBand;
    private BigDecimal lowerBollingerBand;

    // Support/Resistance
    private List<BigDecimal> supportLevels;
    private List<BigDecimal> resistanceLevels;

    // Volume indicators
    private BigDecimal volumeWeightedAveragePrice;
    private BigDecimal onBalanceVolume;

    // Pattern recognition
    private List<String> detectedPatterns;
    private String overallTrend;
    private String momentum;

    // Additional metrics
    private BigDecimal volatility;
    private Map<String, Object> additionalIndicators;
}