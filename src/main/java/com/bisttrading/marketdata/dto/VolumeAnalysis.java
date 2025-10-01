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
public class VolumeAnalysis {
    private String symbol;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private Long totalVolume;
    private BigDecimal volumeWeightedAveragePrice; // VWAP
    private BigDecimal averageVolumePerPeriod;
    private BigDecimal volumeStandardDeviation;

    private List<Map<String, Object>> highVolumePeriodsWithPriceImpact;
    private Map<String, BigDecimal> volumeDistributionByTimeframe;

    private Long peakVolumeValue;
    private OffsetDateTime peakVolumeTime;
    private BigDecimal priceImpactOfPeakVolume;
}