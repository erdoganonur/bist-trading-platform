package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MacdIndicator {
    private BigDecimal macdLine;
    private BigDecimal signalLine;
    private BigDecimal histogram;

    public boolean isBullishCrossover() {
        if (macdLine == null || signalLine == null) {
            return false;
        }
        return macdLine.compareTo(signalLine) > 0;
    }

    public boolean isBearishCrossover() {
        if (macdLine == null || signalLine == null) {
            return false;
        }
        return macdLine.compareTo(signalLine) < 0;
    }

    public String getSignal() {
        if (isBullishCrossover()) {
            return "BULLISH_CROSSOVER";
        } else if (isBearishCrossover()) {
            return "BEARISH_CROSSOVER";
        } else {
            return "NO_SIGNAL";
        }
    }
}