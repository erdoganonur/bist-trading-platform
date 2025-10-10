package com.bisttrading.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sector allocation information.
 * Shows portfolio distribution across sectors.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sector allocation in portfolio")
public class SectorAllocation {

    /**
     * Sector name
     */
    @Schema(description = "Sector name", example = "Banking")
    private String sector;

    /**
     * Total value in this sector
     */
    @Schema(description = "Total value in sector", example = "157300.00")
    private BigDecimal value;

    /**
     * Percentage of portfolio
     */
    @Schema(description = "Percentage of portfolio", example = "35.5")
    private BigDecimal percentage;

    /**
     * Number of positions in this sector
     */
    @Schema(description = "Number of positions", example = "3")
    private Integer positionCount;
}
