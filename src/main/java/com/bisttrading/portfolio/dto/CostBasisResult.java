package com.bisttrading.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result object for cost basis calculations.
 * Contains average cost, total cost, and fees.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostBasisResult {

    /**
     * Average cost per share
     */
    private BigDecimal averageCost;

    /**
     * Total cost (quantity * average price)
     */
    private BigDecimal totalCost;

    /**
     * Total commission paid
     */
    private BigDecimal totalCommission;

    /**
     * Total tax paid
     */
    private BigDecimal totalTax;

    /**
     * Create an empty cost basis result
     */
    public static CostBasisResult empty() {
        return CostBasisResult.builder()
                .averageCost(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .totalCommission(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .build();
    }

    /**
     * Calculate total cost including fees
     */
    public BigDecimal getTotalCostWithFees() {
        BigDecimal total = totalCost != null ? totalCost : BigDecimal.ZERO;
        if (totalCommission != null) total = total.add(totalCommission);
        if (totalTax != null) total = total.add(totalTax);
        return total;
    }
}
