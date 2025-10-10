package com.bisttrading.symbol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Symbol information.
 * Combines database symbol data with real-time market data from AlgoLab.
 *
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Symbol information with real-time market data")
public class SymbolDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Basic Information =====

    @Schema(description = "Symbol code (e.g., AKBNK, GARAN)", example = "AKBNK", required = true)
    private String symbol;

    @Schema(description = "Full company name", example = "Akbank T.A.Ş.")
    private String name;

    @Schema(description = "Turkish company name", example = "Akbank T.A.Ş.")
    private String localName;

    @Schema(description = "Exchange where symbol is traded", example = "BIST", required = true)
    private String exchange;

    @Schema(description = "Business sector", example = "Banking")
    private String sector;

    @Schema(description = "Industry classification", example = "Commercial Banks")
    private String industry;

    @Schema(description = "Trading currency code", example = "TRY", required = true)
    private String currency;

    @Schema(description = "ISIN code", example = "TRAAKBNK91N6")
    private String isinCode;

    @Schema(description = "Market type", example = "EQUITY")
    private String marketType;

    // ===== Real-Time Trading Information =====

    @Schema(description = "Last traded price", example = "15.75")
    private BigDecimal lastPrice;

    @Schema(description = "Price change from previous close", example = "0.25")
    private BigDecimal change;

    @Schema(description = "Price change percentage", example = "1.61")
    private BigDecimal changePercent;

    // ===== Day's Trading Range =====

    @Schema(description = "Opening price of the day", example = "15.50")
    private BigDecimal dayOpen;

    @Schema(description = "Highest price of the day", example = "15.90")
    private BigDecimal dayHigh;

    @Schema(description = "Lowest price of the day", example = "15.45")
    private BigDecimal dayLow;

    @Schema(description = "Previous day's closing price", example = "15.50")
    private BigDecimal previousClose;

    // ===== Volume Information =====

    @Schema(description = "Number of shares traded today", example = "12500000")
    private Long volume;

    @Schema(description = "Total trading value in TRY", example = "196875000.00")
    private BigDecimal value;

    @Schema(description = "Average price weighted by volume", example = "15.75")
    private BigDecimal vwap;

    // ===== Price Limits (BIST Specific) =====

    @Schema(description = "Maximum allowed price for today (ceiling)", example = "17.05")
    private BigDecimal ceiling;

    @Schema(description = "Minimum allowed price for today (floor)", example = "13.95")
    private BigDecimal floor;

    @Schema(description = "Reference price for limit calculations", example = "15.50")
    private BigDecimal referencePrice;

    // ===== Order Rules =====

    @Schema(description = "Trading lot size", example = "1", required = true)
    private Integer lotSize;

    @Schema(description = "Minimum order quantity in lots", example = "1")
    private Integer minOrderQty;

    @Schema(description = "Maximum order quantity in lots", example = "1000000")
    private Integer maxOrderQty;

    @Schema(description = "Minimum price increment (tick size)", example = "0.01")
    private BigDecimal tickSize;

    // ===== Trading Status =====

    @Schema(description = "Current trading status", example = "ACTIVE", allowableValues = {"ACTIVE", "HALTED", "SUSPENDED", "CLOSED", "PRE_TRADING", "POST_TRADING"})
    private String tradingStatus;

    @Schema(description = "Is symbol currently tradeable", example = "true")
    private Boolean isTradeable;

    @Schema(description = "Is symbol part of BIST30 index", example = "true")
    private Boolean isBist30;

    // ===== Financial Metrics =====

    @Schema(description = "Market capitalization in millions", example = "45000000000.00")
    private BigDecimal marketCap;

    @Schema(description = "Free float ratio (0.0 to 1.0)", example = "0.35")
    private BigDecimal freeFloatRatio;

    @Schema(description = "Annual dividend yield percentage", example = "3.5")
    private BigDecimal dividendYield;

    @Schema(description = "Index memberships", example = "[\"BIST30\", \"BIST100\"]")
    private List<String> indexMemberships;

    // ===== Timestamps =====

    @Schema(description = "Last update timestamp", example = "2024-10-10T14:30:00")
    private LocalDateTime lastUpdated;

    @Schema(description = "Data source", example = "AlgoLab", allowableValues = {"DATABASE", "AlgoLab", "CACHE"})
    private String dataSource;

    // ===== Helper Methods =====

    /**
     * Calculate market value for a given quantity
     */
    public BigDecimal calculateMarketValue(Integer quantity) {
        if (lastPrice == null || quantity == null) {
            return null;
        }
        return lastPrice.multiply(new BigDecimal(quantity));
    }

    /**
     * Check if price is within daily limits
     */
    public boolean isPriceWithinLimits(BigDecimal price) {
        if (price == null) return false;

        boolean aboveFloor = floor == null || price.compareTo(floor) >= 0;
        boolean belowCeiling = ceiling == null || price.compareTo(ceiling) <= 0;

        return aboveFloor && belowCeiling;
    }

    /**
     * Get percentage to ceiling
     */
    public BigDecimal getPercentageToCeiling() {
        if (lastPrice == null || ceiling == null) {
            return null;
        }
        return ceiling.subtract(lastPrice)
                .divide(lastPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Get percentage to floor
     */
    public BigDecimal getPercentageToFloor() {
        if (lastPrice == null || floor == null) {
            return null;
        }
        return lastPrice.subtract(floor)
                .divide(lastPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
