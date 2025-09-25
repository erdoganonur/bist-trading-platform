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
public class Position {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("available_quantity")
    private Integer availableQuantity;

    @JsonProperty("blocked_quantity")
    private Integer blockedQuantity;

    @JsonProperty("average_cost")
    private BigDecimal averageCost;

    @JsonProperty("market_value")
    private BigDecimal marketValue;

    @JsonProperty("current_price")
    private BigDecimal currentPrice;

    @JsonProperty("unrealized_pnl")
    private BigDecimal unrealizedPnl;

    @JsonProperty("realized_pnl")
    private BigDecimal realizedPnl;

    @JsonProperty("total_pnl")
    private BigDecimal totalPnl;

    @JsonProperty("side")
    private PositionSide side;

    @JsonProperty("last_updated")
    private Instant lastUpdated;

    @JsonProperty("margin_requirement")
    private BigDecimal marginRequirement;

    @JsonProperty("maintenance_margin")
    private BigDecimal maintenanceMargin;

    public boolean isLong() {
        return side == PositionSide.LONG;
    }

    public boolean isShort() {
        return side == PositionSide.SHORT;
    }

    public boolean isEmpty() {
        return quantity == null || quantity == 0;
    }

    public BigDecimal getCostBasis() {
        if (averageCost != null && quantity != null && quantity > 0) {
            return averageCost.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getPnlPercentage() {
        BigDecimal costBasis = getCostBasis();
        if (costBasis.compareTo(BigDecimal.ZERO) > 0 && totalPnl != null) {
            return totalPnl.divide(costBasis, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    public boolean requiresMarginCall() {
        if (marginRequirement != null && maintenanceMargin != null) {
            return marginRequirement.compareTo(maintenanceMargin) < 0;
        }
        return false;
    }
}