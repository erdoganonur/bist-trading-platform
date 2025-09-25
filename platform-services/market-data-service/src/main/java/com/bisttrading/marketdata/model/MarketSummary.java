package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class MarketSummary {
    private int totalSymbols;
    private int activeSymbols;
    private int advancingCount;
    private int decliningCount;
    private int unchangedCount;
    private long totalVolume;
    private BigDecimal totalTurnover;
    private Instant timestamp;

    public double getAdvanceDeclineRatio() {
        if (decliningCount == 0) {
            return advancingCount > 0 ? Double.MAX_VALUE : 0.0;
        }
        return (double) advancingCount / decliningCount;
    }

    public double getAdvancingPercentage() {
        if (activeSymbols == 0) {
            return 0.0;
        }
        return (double) advancingCount / activeSymbols * 100.0;
    }

    public double getDecliningPercentage() {
        if (activeSymbols == 0) {
            return 0.0;
        }
        return (double) decliningCount / activeSymbols * 100.0;
    }

    public String getMarketSentiment() {
        double advancingPct = getAdvancingPercentage();

        if (advancingPct >= 70) {
            return "VERY_BULLISH";
        } else if (advancingPct >= 60) {
            return "BULLISH";
        } else if (advancingPct >= 40) {
            return "NEUTRAL";
        } else if (advancingPct >= 30) {
            return "BEARISH";
        } else {
            return "VERY_BEARISH";
        }
    }
}