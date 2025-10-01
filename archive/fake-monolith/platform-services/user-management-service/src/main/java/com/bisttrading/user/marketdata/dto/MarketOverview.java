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
public class MarketOverview {
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private Integer totalActiveSymbols;
    private Long totalVolume;
    private BigDecimal totalValue;
    private BigDecimal averageVolume;

    private List<Map<String, Object>> topVolumeSymbols;
    private List<Map<String, Object>> topPerformers;
    private List<Map<String, Object>> bottomPerformers;

    private Map<String, Object> sectorDistribution;
    private Map<String, BigDecimal> marketStatistics;
    private Map<String, Object> databaseMetrics;

    private BigDecimal marketVolatility;
    private String marketTrend;
    private BigDecimal advancedDeclineRatio;
}