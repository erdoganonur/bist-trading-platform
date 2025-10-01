package com.bisttrading.user.entity.trading;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import com.bisttrading.user.entity.trading.enums.MarketType;
import com.bisttrading.user.entity.trading.enums.SymbolStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a tradeable symbol (stock, bond, derivative, etc.)
 * Contains all trading specifications and market data for BIST symbols
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Entity
@Table(name = "symbols", indexes = {
    @Index(name = "idx_symbols_symbol", columnList = "symbol"),
    @Index(name = "idx_symbols_exchange", columnList = "exchange"),
    @Index(name = "idx_symbols_market_type", columnList = "marketType"),
    @Index(name = "idx_symbols_status", columnList = "symbolStatus"),
    @Index(name = "idx_symbols_sector", columnList = "sector"),
    @Index(name = "idx_symbols_isin", columnList = "isinCode")
})
@SQLDelete(sql = "UPDATE symbols SET deleted_at = CURRENT_TIMESTAMP WHERE symbol_id = ?")
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Symbol extends BaseEntity {

    /**
     * Symbol code (e.g., GARAN, THYAO, AKBNK)
     */
    @Column(name = "symbol", length = 20, nullable = false, unique = true)
    @NotBlank(message = "Symbol code is required")
    @Size(max = 20, message = "Symbol code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol must contain only uppercase letters and numbers")
    private String symbol;

    /**
     * Full name of the instrument/company
     */
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Symbol name is required")
    @Size(max = 255, message = "Symbol name must not exceed 255 characters")
    private String name;

    /**
     * Local name (Turkish name for BIST symbols)
     */
    @Column(name = "local_name")
    @Size(max = 255, message = "Local name must not exceed 255 characters")
    private String localName;

    /**
     * International Securities Identification Number
     */
    @Column(name = "isin_code", length = 12)
    @Size(min = 12, max = 12, message = "ISIN code must be exactly 12 characters")
    @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "Invalid ISIN format")
    private String isinCode;

    /**
     * Market type (EQUITY, BOND, DERIVATIVE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    @Builder.Default
    private MarketType marketType = MarketType.EQUITY;

    /**
     * Exchange where the symbol is traded
     */
    @Column(name = "exchange", length = 50, nullable = false)
    @NotBlank(message = "Exchange is required")
    @Builder.Default
    private String exchange = "BIST";

    /**
     * Business sector (Banking, Technology, etc.)
     */
    @Column(name = "sector", length = 100)
    @Size(max = 100, message = "Sector must not exceed 100 characters")
    private String sector;

    /**
     * Industry classification (more specific than sector)
     */
    @Column(name = "industry", length = 100)
    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    /**
     * Minimum trading unit (lot size)
     */
    @Column(name = "lot_size", nullable = false)
    @Positive(message = "Lot size must be positive")
    @Builder.Default
    private Integer lotSize = 1;

    /**
     * Minimum price increment
     */
    @Column(name = "tick_size", nullable = false, precision = 10, scale = 6)
    @Positive(message = "Tick size must be positive")
    @Digits(integer = 4, fraction = 6, message = "Invalid tick size format")
    @Builder.Default
    private BigDecimal tickSize = new BigDecimal("0.01");

    /**
     * Minimum order quantity
     */
    @Column(name = "min_order_quantity", nullable = false)
    @Positive(message = "Minimum order quantity must be positive")
    @Builder.Default
    private Integer minOrderQuantity = 1;

    /**
     * Maximum order quantity (if any)
     */
    @Column(name = "max_order_quantity")
    @Positive(message = "Maximum order quantity must be positive")
    private Integer maxOrderQuantity;

    /**
     * Daily upper price limit (BIST specific)
     */
    @Column(name = "ceiling_price", precision = 20, scale = 6)
    @Positive(message = "Ceiling price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid ceiling price format")
    private BigDecimal ceilingPrice;

    /**
     * Daily lower price limit (BIST specific)
     */
    @Column(name = "floor_price", precision = 20, scale = 6)
    @Positive(message = "Floor price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid floor price format")
    private BigDecimal floorPrice;

    /**
     * Reference price for limit calculations
     */
    @Column(name = "reference_price", precision = 20, scale = 6)
    @Positive(message = "Reference price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid reference price format")
    private BigDecimal referencePrice;

    /**
     * Currency code (TRY, USD, EUR)
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency code format")
    @Builder.Default
    private String currencyCode = "TRY";

    /**
     * Market capitalization
     */
    @Column(name = "market_cap", precision = 20, scale = 2)
    @PositiveOrZero(message = "Market cap must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid market cap format")
    private BigDecimal marketCap;

    /**
     * Free float ratio (0.0 to 1.0)
     */
    @Column(name = "free_float_ratio", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Free float ratio must be non-negative")
    @DecimalMax(value = "1.0", message = "Free float ratio must not exceed 1.0")
    private BigDecimal freeFloatRatio;

    /**
     * Index memberships as JSON array
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "index_memberships", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> indexMemberships = new ArrayList<>();

    /**
     * Annual dividend yield
     */
    @Column(name = "dividend_yield", precision = 8, scale = 4)
    @PositiveOrZero(message = "Dividend yield must be non-negative")
    @DecimalMax(value = "100.0", message = "Dividend yield cannot exceed 100%")
    private BigDecimal dividendYield;

    /**
     * Last dividend payment date
     */
    @Column(name = "last_dividend_date")
    private LocalDate lastDividendDate;

    /**
     * Stock split ratio
     */
    @Column(name = "split_ratio", precision = 10, scale = 6)
    @Positive(message = "Split ratio must be positive")
    private BigDecimal splitRatio;

    /**
     * Current trading status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "symbol_status", nullable = false)
    @Builder.Default
    private SymbolStatus symbolStatus = SymbolStatus.ACTIVE;

    /**
     * Daily trading start time
     */
    @Column(name = "trading_start_time")
    private LocalTime tradingStartTime;

    /**
     * Daily trading end time
     */
    @Column(name = "trading_end_time")
    private LocalTime tradingEndTime;

    /**
     * Additional metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode metadata = null;

    /**
     * One-to-many relationship with orders
     */
    @OneToMany(mappedBy = "symbol", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Order> orders = new ArrayList<>();

    /**
     * One-to-many relationship with positions
     */
    @OneToMany(mappedBy = "symbol", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Position> positions = new ArrayList<>();

    // Business methods

    /**
     * Check if the symbol is currently tradeable
     */
    public boolean isTradeable() {
        return symbolStatus == SymbolStatus.ACTIVE || symbolStatus == SymbolStatus.PRE_TRADING || symbolStatus == SymbolStatus.POST_TRADING;
    }

    /**
     * Check if price is within daily limits
     */
    public boolean isPriceWithinLimits(BigDecimal price) {
        if (price == null) return false;

        boolean aboveFloor = floorPrice == null || price.compareTo(floorPrice) >= 0;
        boolean belowCeiling = ceilingPrice == null || price.compareTo(ceilingPrice) <= 0;

        return aboveFloor && belowCeiling;
    }

    /**
     * Get the rounded price according to tick size
     */
    public BigDecimal roundToTickSize(BigDecimal price) {
        if (price == null || tickSize == null) return price;

        return price.divide(tickSize, 0, java.math.RoundingMode.HALF_UP)
                   .multiply(tickSize);
    }

    /**
     * Check if quantity meets minimum order requirements
     */
    public boolean isValidOrderQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) return false;

        // Check minimum quantity
        if (quantity < minOrderQuantity) return false;

        // Check maximum quantity if set
        if (maxOrderQuantity != null && quantity > maxOrderQuantity) return false;

        // Check lot size alignment
        return quantity % lotSize == 0;
    }

    /**
     * Check if symbol belongs to a specific index
     */
    public boolean isMemberOf(String indexCode) {
        return indexMemberships != null && indexMemberships.contains(indexCode);
    }

    /**
     * Get display name with fallback to symbol code
     */
    public String getDisplayName() {
        return localName != null ? localName : name;
    }

    /**
     * Check if symbol has dividend information
     */
    public boolean hasDividendInfo() {
        return dividendYield != null && dividendYield.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculate market value based on current price
     */
    public BigDecimal calculateMarketValue(BigDecimal currentPrice, Integer shares) {
        if (currentPrice == null || shares == null) return null;
        return currentPrice.multiply(new BigDecimal(shares));
    }

    // Validation methods

    @PrePersist
    @PreUpdate
    private void validate() {
        // Ensure max order quantity is greater than min order quantity
        if (maxOrderQuantity != null && maxOrderQuantity < minOrderQuantity) {
            throw new IllegalArgumentException("Maximum order quantity must be greater than minimum order quantity");
        }

        // Ensure ceiling price is greater than floor price
        if (ceilingPrice != null && floorPrice != null && ceilingPrice.compareTo(floorPrice) < 0) {
            throw new IllegalArgumentException("Ceiling price must be greater than floor price");
        }

        // Ensure trading times are valid
        if (tradingStartTime != null && tradingEndTime != null && tradingStartTime.isAfter(tradingEndTime)) {
            throw new IllegalArgumentException("Trading start time must be before end time");
        }
    }
}