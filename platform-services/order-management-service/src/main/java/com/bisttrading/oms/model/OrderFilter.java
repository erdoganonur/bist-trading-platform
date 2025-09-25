package com.bisttrading.oms.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Filter criteria for querying orders
 */
@Data
@Builder
public class OrderFilter {

    private String symbol;
    private List<OMSOrder.OrderSide> sides;
    private List<OMSOrder.OrderType> types;
    private List<OMSOrder.OrderStatus> statuses;
    private List<OMSOrder.TimeInForce> timeInForceOptions;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String accountId;
    private String portfolioId;
    private String strategyId;

    private Boolean activeOnly;
    private Boolean filledOnly;

    private String clientOrderIdLike;
    private String externalOrderIdLike;

    // Helper methods
    public boolean hasSymbolFilter() {
        return symbol != null && !symbol.trim().isEmpty();
    }

    public boolean hasSideFilter() {
        return sides != null && !sides.isEmpty();
    }

    public boolean hasTypeFilter() {
        return types != null && !types.isEmpty();
    }

    public boolean hasStatusFilter() {
        return statuses != null && !statuses.isEmpty();
    }

    public boolean hasDateRangeFilter() {
        return startDate != null || endDate != null;
    }

    public boolean hasAccountFilter() {
        return accountId != null && !accountId.trim().isEmpty();
    }

    public boolean hasPortfolioFilter() {
        return portfolioId != null && !portfolioId.trim().isEmpty();
    }

    public boolean hasStrategyFilter() {
        return strategyId != null && !strategyId.trim().isEmpty();
    }

    public boolean hasActiveOnlyFilter() {
        return activeOnly != null && activeOnly;
    }

    public boolean hasFilledOnlyFilter() {
        return filledOnly != null && filledOnly;
    }
}