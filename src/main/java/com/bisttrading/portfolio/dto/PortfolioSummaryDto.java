package com.bisttrading.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive portfolio summary with P&L and performance metrics.
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
@Schema(description = "Portfolio summary with P&L calculations")
public class PortfolioSummaryDto {

    // ===== Positions =====

    /**
     * List of all positions
     */
    @Schema(description = "All positions with details")
    private List<EnrichedPositionDto> positions;

    /**
     * Number of positions
     */
    @Schema(description = "Number of positions", example = "12")
    private Integer positionCount;

    // ===== Portfolio Value =====

    /**
     * Total current market value
     */
    @Schema(description = "Total market value", example = "485230.00", required = true)
    private BigDecimal totalValue;

    /**
     * Total cost basis
     */
    @Schema(description = "Total cost", example = "425000.00", required = true)
    private BigDecimal totalCost;

    /**
     * Available cash (if applicable)
     */
    @Schema(description = "Available cash", example = "25000.00")
    private BigDecimal availableCash;

    // ===== Total P&L =====

    /**
     * Total unrealized P&L in TRY
     */
    @Schema(description = "Total unrealized P&L", example = "60230.00", required = true)
    private BigDecimal totalPnl;

    /**
     * Total unrealized P&L percentage
     */
    @Schema(description = "Total unrealized P&L %", example = "14.17", required = true)
    private BigDecimal totalPnlPercent;

    // ===== Day Performance =====

    /**
     * Today's P&L in TRY
     */
    @Schema(description = "Today's P&L", example = "12450.00")
    private BigDecimal dayPnl;

    /**
     * Today's P&L percentage
     */
    @Schema(description = "Today's P&L %", example = "2.63")
    private BigDecimal dayPnlPercent;

    // ===== Performance Metrics =====

    /**
     * Top gaining positions
     */
    @Schema(description = "Top 3 gaining positions")
    private List<PositionPerformance> topGainers;

    /**
     * Top losing positions
     */
    @Schema(description = "Top 3 losing positions")
    private List<PositionPerformance> topLosers;

    /**
     * Best performing position today
     */
    @Schema(description = "Best performer today")
    private PositionPerformance bestToday;

    /**
     * Worst performing position today
     */
    @Schema(description = "Worst performer today")
    private PositionPerformance worstToday;

    // ===== Allocation =====

    /**
     * Sector allocation breakdown
     */
    @Schema(description = "Portfolio allocation by sector")
    private Map<String, SectorAllocation> sectorAllocation;

    /**
     * Concentration risk (largest position %)
     */
    @Schema(description = "Largest position percentage", example = "18.5")
    private BigDecimal largestPositionPercent;

    // ===== Additional Metrics =====

    /**
     * Total fees paid
     */
    @Schema(description = "Total fees paid", example = "2125.50")
    private BigDecimal totalFees;

    /**
     * Total commission paid
     */
    @Schema(description = "Total commission", example = "1062.75")
    private BigDecimal totalCommission;

    /**
     * Total tax paid
     */
    @Schema(description = "Total tax", example = "1062.75")
    private BigDecimal totalTax;

    /**
     * Number of winning positions
     */
    @Schema(description = "Winning positions count", example = "8")
    private Integer winningPositions;

    /**
     * Number of losing positions
     */
    @Schema(description = "Losing positions count", example = "4")
    private Integer losingPositions;

    /**
     * Win rate percentage
     */
    @Schema(description = "Win rate", example = "66.67")
    private BigDecimal winRate;

    // ===== Metadata =====

    /**
     * Last update timestamp
     */
    @Schema(description = "Last updated", example = "2024-09-24T14:30:00Z")
    private Instant lastUpdated;

    /**
     * Data source
     */
    @Schema(description = "Data source", example = "AlgoLab")
    private String dataSource;

    // ===== Computed Fields =====

    /**
     * Calculate total value including cash
     */
    public BigDecimal getTotalValueWithCash() {
        BigDecimal value = totalValue != null ? totalValue : BigDecimal.ZERO;
        if (availableCash != null) {
            value = value.add(availableCash);
        }
        return value;
    }

    /**
     * Check if portfolio is profitable
     */
    public boolean isProfitable() {
        return totalPnl != null && totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if portfolio gained today
     */
    public boolean isGainingToday() {
        return dayPnl != null && dayPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get diversity score (number of sectors / number of positions)
     */
    public BigDecimal getDiversityScore() {
        if (positionCount == null || positionCount == 0) {
            return BigDecimal.ZERO;
        }
        if (sectorAllocation == null || sectorAllocation.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(sectorAllocation.size())
                .divide(BigDecimal.valueOf(positionCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
