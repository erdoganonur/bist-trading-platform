package com.bisttrading.user.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrostructureAnalysis {
    private String symbol;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private BigDecimal averageSpread;
    private BigDecimal spreadStandardDeviation;
    private BigDecimal relativeSpread;
    private BigDecimal effectiveSpread;

    private BigDecimal orderBookThickness;
    private BigDecimal depthAtBestBid;
    private BigDecimal depthAtBestAsk;
    private BigDecimal orderImbalance;

    private BigDecimal tickBasedSpread;
    private BigDecimal quotedSpread;
    private BigDecimal realizedSpread;

    private Map<String, BigDecimal> spreadStatistics;
    private Map<String, Object> liquidityMetrics;
    private Map<String, Object> marketQualityIndicators;
}