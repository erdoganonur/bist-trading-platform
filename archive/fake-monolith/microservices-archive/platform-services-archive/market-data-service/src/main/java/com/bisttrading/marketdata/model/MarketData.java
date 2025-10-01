package com.bisttrading.marketdata.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Market data for a symbol
 */
@Data
@Builder
public class MarketData {

    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal volume;
    private BigDecimal value;
    private BigDecimal change;
    private BigDecimal changePercent;
    private LocalDateTime timestamp;
    private String session;

    // Additional market data fields
    private BigDecimal averagePrice;
    private int transactionCount;
    private BigDecimal previousClose;
    private BigDecimal weightedAveragePrice;

    // Market status
    private String marketStatus;
    private boolean tradingHalted;

    // Calculated fields
    public BigDecimal getSpread() {
        if (askPrice != null && bidPrice != null) {
            return askPrice.subtract(bidPrice);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getSpreadPercentage() {
        if (askPrice != null && bidPrice != null && bidPrice.compareTo(BigDecimal.ZERO) > 0) {
            return getSpread().divide(bidPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    public boolean isValid() {
        return symbol != null && lastPrice != null && timestamp != null;
    }
}