package com.bisttrading.user.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysis {
    private String symbol;
    private String timeframe;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private String trendDirection; // UP, DOWN, SIDEWAYS
    private BigDecimal trendStrength; // 0-100
    private BigDecimal slopeAngle;
    private Integer dataPointCount;

    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private BigDecimal volumeTrend;
    private BigDecimal volatility;

    private BigDecimal rSquared; // Trend line fit quality
    private String confidence; // HIGH, MEDIUM, LOW
}