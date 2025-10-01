package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@Jacksonized
public class MarketData {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("last_price")
    private BigDecimal lastPrice;

    @JsonProperty("bid_price")
    private BigDecimal bidPrice;

    @JsonProperty("ask_price")
    private BigDecimal askPrice;

    @JsonProperty("bid_size")
    private Integer bidSize;

    @JsonProperty("ask_size")
    private Integer askSize;

    @JsonProperty("open_price")
    private BigDecimal openPrice;

    @JsonProperty("high_price")
    private BigDecimal highPrice;

    @JsonProperty("low_price")
    private BigDecimal lowPrice;

    @JsonProperty("close_price")
    private BigDecimal closePrice;

    @JsonProperty("volume")
    private Long volume;

    @JsonProperty("turnover")
    private BigDecimal turnover;

    @JsonProperty("change")
    private BigDecimal change;

    @JsonProperty("change_percent")
    private BigDecimal changePercent;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("market_status")
    private MarketStatus marketStatus;

    @JsonProperty("session")
    private TradingSession session;

    @JsonProperty("vwap")
    private BigDecimal vwap;

    @JsonProperty("previous_close")
    private BigDecimal previousClose;

    public BigDecimal getSpread() {
        if (askPrice != null && bidPrice != null) {
            return askPrice.subtract(bidPrice);
        }
        return null;
    }

    public BigDecimal getSpreadPercentage() {
        BigDecimal spread = getSpread();
        if (spread != null && lastPrice != null && lastPrice.compareTo(BigDecimal.ZERO) > 0) {
            return spread.divide(lastPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return null;
    }

    public BigDecimal getMidPrice() {
        if (bidPrice != null && askPrice != null) {
            return bidPrice.add(askPrice).divide(BigDecimal.valueOf(2), BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    public boolean isMarketOpen() {
        return marketStatus == MarketStatus.OPEN;
    }

    public boolean hasRecentData() {
        if (timestamp != null) {
            return Instant.now().minusSeconds(60).isBefore(timestamp);
        }
        return false;
    }
}