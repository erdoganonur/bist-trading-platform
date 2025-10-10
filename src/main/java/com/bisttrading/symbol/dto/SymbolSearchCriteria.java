package com.bisttrading.symbol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for filtering symbols.
 * Used for advanced symbol queries with multiple filters.
 *
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Symbol search and filter criteria")
public class SymbolSearchCriteria {

    /**
     * Filter by exchange code (e.g., "BIST")
     */
    @Schema(description = "Exchange code filter", example = "BIST")
    private String exchange;

    /**
     * Filter by business sector
     */
    @Schema(description = "Sector filter", example = "Banking")
    private String sector;

    /**
     * Filter by industry
     */
    @Schema(description = "Industry filter", example = "Commercial Banks")
    private String industry;

    /**
     * Search term for symbol code, name, or local name
     */
    @Schema(description = "Search term (searches symbol, name, localName)", example = "Akbank")
    private String searchTerm;

    /**
     * Filter by currency code
     */
    @Schema(description = "Currency filter", example = "TRY")
    private String currency;

    /**
     * Filter by market type
     */
    @Schema(description = "Market type filter", example = "EQUITY", allowableValues = {"EQUITY", "BOND", "DERIVATIVE", "COMMODITY"})
    private String marketType;

    /**
     * Only return active/tradeable symbols
     */
    @Schema(description = "Return only active symbols", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean activeOnly = true;

    /**
     * Only return symbols that are members of a specific index
     */
    @Schema(description = "Index membership filter", example = "BIST30")
    private String indexMembership;

    /**
     * Minimum market capitalization filter
     */
    @Schema(description = "Minimum market cap", example = "1000000000")
    private Long minMarketCap;

    /**
     * Maximum market capitalization filter
     */
    @Schema(description = "Maximum market cap", example = "100000000000")
    private Long maxMarketCap;

    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return exchange != null
            || sector != null
            || industry != null
            || searchTerm != null
            || currency != null
            || marketType != null
            || indexMembership != null
            || minMarketCap != null
            || maxMarketCap != null;
    }

    /**
     * Check if search term is provided
     */
    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }
}
