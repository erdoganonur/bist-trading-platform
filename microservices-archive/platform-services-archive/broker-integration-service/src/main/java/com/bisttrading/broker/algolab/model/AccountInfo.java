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
public class AccountInfo {

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("account_type")
    private AccountType accountType;

    @JsonProperty("status")
    private AccountStatus status;

    @JsonProperty("cash_balance")
    private BigDecimal cashBalance;

    @JsonProperty("available_cash")
    private BigDecimal availableCash;

    @JsonProperty("blocked_cash")
    private BigDecimal blockedCash;

    @JsonProperty("portfolio_value")
    private BigDecimal portfolioValue;

    @JsonProperty("total_equity")
    private BigDecimal totalEquity;

    @JsonProperty("margin_balance")
    private BigDecimal marginBalance;

    @JsonProperty("margin_used")
    private BigDecimal marginUsed;

    @JsonProperty("margin_available")
    private BigDecimal marginAvailable;

    @JsonProperty("margin_ratio")
    private BigDecimal marginRatio;

    @JsonProperty("buying_power")
    private BigDecimal buyingPower;

    @JsonProperty("day_trading_buying_power")
    private BigDecimal dayTradingBuyingPower;

    @JsonProperty("unrealized_pnl")
    private BigDecimal unrealizedPnl;

    @JsonProperty("realized_pnl")
    private BigDecimal realizedPnl;

    @JsonProperty("total_pnl")
    private BigDecimal totalPnl;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("last_updated")
    private Instant lastUpdated;

    @JsonProperty("maintenance_margin")
    private BigDecimal maintenanceMargin;

    @JsonProperty("initial_margin")
    private BigDecimal initialMargin;

    public BigDecimal getTotalValue() {
        if (cashBalance != null && portfolioValue != null) {
            return cashBalance.add(portfolioValue);
        }
        return cashBalance != null ? cashBalance : portfolioValue;
    }

    public boolean hasMarginCall() {
        if (marginRatio != null && marginRatio.compareTo(BigDecimal.ZERO) > 0) {
            return marginRatio.compareTo(BigDecimal.valueOf(0.25)) < 0;
        }
        return false;
    }

    public boolean canTrade() {
        return status == AccountStatus.ACTIVE &&
               (availableCash != null && availableCash.compareTo(BigDecimal.ZERO) > 0);
    }

    public BigDecimal getNetLiquidity() {
        BigDecimal totalValue = getTotalValue();
        if (totalValue != null && marginUsed != null) {
            return totalValue.subtract(marginUsed);
        }
        return totalValue;
    }

    public BigDecimal getPnlPercentage() {
        BigDecimal totalValue = getTotalValue();
        if (totalPnl != null && totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
            return totalPnl.divide(totalValue, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}