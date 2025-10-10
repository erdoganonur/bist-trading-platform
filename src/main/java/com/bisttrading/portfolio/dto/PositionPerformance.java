package com.bisttrading.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Position performance summary.
 * Used for top gainers/losers.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Position performance summary")
public class PositionPerformance {

    /**
     * Symbol code
     */
    @Schema(description = "Symbol code", example = "AKBNK")
    private String symbol;

    /**
     * Symbol name
     */
    @Schema(description = "Symbol name", example = "Akbank T.A.Ş.")
    private String symbolName;

    /**
     * Unrealized P&L
     */
    @Schema(description = "Unrealized P&L", example = "2500.00")
    private BigDecimal unrealizedPnl;

    /**
     * Unrealized P&L percentage
     */
    @Schema(description = "Unrealized P&L percentage", example = "15.9")
    private BigDecimal unrealizedPnlPercent;

    /**
     * Market value
     */
    @Schema(description = "Market value", example = "18230.00")
    private BigDecimal marketValue;

    /**
     * Day change percentage
     */
    @Schema(description = "Day change percentage", example = "2.3")
    private BigDecimal dayChangePercent;
}
