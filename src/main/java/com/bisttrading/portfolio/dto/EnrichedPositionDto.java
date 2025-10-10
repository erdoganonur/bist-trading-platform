package com.bisttrading.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Enriched position with P&L calculations and cost basis.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Position with P&L calculations")
public class EnrichedPositionDto {

    // ===== Basic Information =====

    /**
     * Symbol code
     */
    @Schema(description = "Symbol code", example = "AKBNK", required = true)
    private String symbol;

    /**
     * Symbol name
     */
    @Schema(description = "Symbol name", example = "Akbank T.A.Ş.")
    private String symbolName;

    /**
     * Sector
     */
    @Schema(description = "Sector", example = "Banking")
    private String sector;

    // ===== Quantity and Cost =====

    /**
     * Current quantity held
     */
    @Schema(description = "Quantity held", example = "1000", required = true)
    private BigDecimal quantity;

    /**
     * Average cost per share (including fees)
     */
    @Schema(description = "Average cost per share", example = "15.73")
    private BigDecimal averageCost;

    /**
     * Total cost (quantity * average cost)
     */
    @Schema(description = "Total cost", example = "15730.00")
    private BigDecimal totalCost;

    // ===== Current Market Data =====

    /**
     * Current market price
     */
    @Schema(description = "Current price", example = "17.25", required = true)
    private BigDecimal currentPrice;

    /**
     * Current market value
     */
    @Schema(description = "Market value", example = "17250.00")
    private BigDecimal marketValue;

    // ===== P&L Calculations =====

    /**
     * Unrealized P&L in TRY
     */
    @Schema(description = "Unrealized P&L", example = "1520.00")
    private BigDecimal unrealizedPnl;

    /**
     * Unrealized P&L percentage
     */
    @Schema(description = "Unrealized P&L %", example = "9.66")
    private BigDecimal unrealizedPnlPercent;

    /**
     * Today's change in TRY
     */
    @Schema(description = "Day change", example = "250.00")
    private BigDecimal dayChange;

    /**
     * Today's change percentage
     */
    @Schema(description = "Day change %", example = "1.47")
    private BigDecimal dayChangePercent;

    // ===== Fees =====

    /**
     * Total commission paid
     */
    @Schema(description = "Total commission", example = "78.65")
    private BigDecimal totalCommission;

    /**
     * Total tax paid
     */
    @Schema(description = "Total tax", example = "31.46")
    private BigDecimal totalTax;

    // ===== Metadata =====

    /**
     * Last update timestamp
     */
    @Schema(description = "Last updated", example = "2024-09-24T14:30:00Z")
    private Instant lastUpdated;

    // ===== Computed Fields =====

    /**
     * Calculate total fees paid
     */
    public BigDecimal getTotalFees() {
        BigDecimal fees = BigDecimal.ZERO;
        if (totalCommission != null) fees = fees.add(totalCommission);
        if (totalTax != null) fees = fees.add(totalTax);
        return fees;
    }

    /**
     * Calculate percentage of portfolio
     */
    public BigDecimal getPortfolioPercentage(BigDecimal totalPortfolioValue) {
        if (totalPortfolioValue == null || totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (marketValue == null) {
            return BigDecimal.ZERO;
        }
        return marketValue
                .divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return unrealizedPnl != null && unrealizedPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if position gained today
     */
    public boolean isGainingToday() {
        return dayChange != null && dayChange.compareTo(BigDecimal.ZERO) > 0;
    }
}
