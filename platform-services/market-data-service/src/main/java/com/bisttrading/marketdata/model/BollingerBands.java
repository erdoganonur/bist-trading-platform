package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BollingerBands {
    private BigDecimal upperBand;
    private BigDecimal middleBand; // 20-period SMA
    private BigDecimal lowerBand;

    public BigDecimal getBandwidth() {
        if (upperBand == null || lowerBand == null || middleBand == null) {
            return null;
        }
        return upperBand.subtract(lowerBand).divide(middleBand, BigDecimal.ROUND_HALF_UP);
    }

    public String getSignal(BigDecimal currentPrice) {
        if (currentPrice == null || upperBand == null || lowerBand == null) {
            return "UNKNOWN";
        }

        if (currentPrice.compareTo(upperBand) >= 0) {
            return "OVERBOUGHT";
        } else if (currentPrice.compareTo(lowerBand) <= 0) {
            return "OVERSOLD";
        } else {
            return "NORMAL";
        }
    }

    public BigDecimal getPercentB(BigDecimal currentPrice) {
        if (currentPrice == null || upperBand == null || lowerBand == null) {
            return null;
        }

        BigDecimal bandWidth = upperBand.subtract(lowerBand);
        if (bandWidth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(0.5);
        }

        return currentPrice.subtract(lowerBand).divide(bandWidth, BigDecimal.ROUND_HALF_UP);
    }
}