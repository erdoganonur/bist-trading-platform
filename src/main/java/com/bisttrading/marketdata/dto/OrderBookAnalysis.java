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
public class OrderBookAnalysis {
    private String symbol;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private BigDecimal bidAskSpread;
    private BigDecimal spreadPercentage;
    private BigDecimal averageSpread;

    private BigDecimal marketDepth;
    private BigDecimal liquidityImbalance; // Positive means more buy pressure
    private BigDecimal orderBookThickness;

    private List<Map<String, Object>> supportLevels;
    private List<Map<String, Object>> resistanceLevels;

    private BigDecimal priceImpactAnalysis;
    private Map<String, Object> volumeDistribution;
    
    private Integer totalLevels;
    private BigDecimal totalBidVolume;
    private BigDecimal totalAskVolume;
}