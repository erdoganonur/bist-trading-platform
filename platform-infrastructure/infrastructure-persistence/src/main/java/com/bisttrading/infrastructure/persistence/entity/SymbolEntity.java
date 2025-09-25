package com.bisttrading.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "symbols", indexes = {
    @Index(name = "idx_symbols_code", columnList = "symbolCode"),
    @Index(name = "idx_symbols_isin", columnList = "isinCode"),
    @Index(name = "idx_symbols_market", columnList = "marketSegment"),
    @Index(name = "idx_symbols_sector", columnList = "sectorCode"),
    @Index(name = "idx_symbols_tradeable", columnList = "isTradeable"),
    @Index(name = "idx_symbols_status", columnList = "tradingStatus")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class SymbolEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Symbol Information
    @Column(name = "symbol_code", nullable = false, unique = true, length = 20)
    private String symbolCode;

    @Column(name = "symbol_name", nullable = false)
    private String symbolName;

    @Column(name = "isin_code", unique = true, length = 12)
    private String isinCode;

    // Market Classification
    @Column(name = "exchange_code", length = 10, nullable = false)
    private String exchangeCode = "BIST";

    @Column(name = "market_segment", length = 50, nullable = false)
    private String marketSegment;

    @Column(name = "sub_market", length = 50)
    private String subMarket;

    // Trading Specifications
    @Column(name = "lot_size", nullable = false)
    private Integer lotSize = 1;

    @Column(name = "tick_size", nullable = false, precision = 10, scale = 6)
    private BigDecimal tickSize = BigDecimal.valueOf(0.01);

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "TRY";

    // Price Limits and Circuit Breakers
    @Column(name = "min_price", precision = 15, scale = 6)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 15, scale = 6)
    private BigDecimal maxPrice;

    @Column(name = "circuit_breaker_percentage", precision = 5, scale = 2)
    private BigDecimal circuitBreakerPercentage = BigDecimal.valueOf(10.0);

    // Trading Hours and Status
    @Column(name = "trading_start_time")
    private LocalTime tradingStartTime;

    @Column(name = "trading_end_time")
    private LocalTime tradingEndTime;

    @Column(name = "is_tradeable", nullable = false)
    private Boolean isTradeable = true;

    @Column(name = "trading_status", length = 20)
    private String tradingStatus = "ACTIVE";

    // Corporate Information
    @Column(name = "sector_code", length = 50)
    private String sectorCode;

    @Column(name = "industry_code", length = 50)
    private String industryCode;

    @Column(name = "company_name", length = 500)
    private String companyName;

    // Index Memberships (stored as JSON array)
    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "index_memberships", columnDefinition = "jsonb")
    private List<String> indexMemberships;

    // Market Making and Liquidity
    @Column(name = "market_maker_eligible")
    private Boolean marketMakerEligible = false;

    @Column(name = "continuous_trading")
    private Boolean continuousTrading = true;

    @Column(name = "call_auction_eligible")
    private Boolean callAuctionEligible = true;

    // Risk Parameters
    @Column(name = "position_limit")
    private Long positionLimit;

    @Column(name = "daily_volume_limit")
    private Long dailyVolumeLimit;

    // Data source tracking
    @Column(name = "data_source", length = 50)
    private String dataSource = "MANUAL";

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Convenience methods
    public boolean isActive() {
        return "ACTIVE".equals(tradingStatus) && Boolean.TRUE.equals(isTradeable);
    }

    public boolean isBistStock() {
        return "BIST".equals(exchangeCode);
    }

    public boolean isInIndex(String indexCode) {
        return indexMemberships != null && indexMemberships.contains(indexCode);
    }

    public boolean hasCircuitBreaker() {
        return circuitBreakerPercentage != null && circuitBreakerPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isWithinTradingHours(LocalTime currentTime) {
        if (tradingStartTime == null || tradingEndTime == null || currentTime == null) {
            return true; // Default to allow if times not set
        }
        return !currentTime.isBefore(tradingStartTime) && !currentTime.isAfter(tradingEndTime);
    }

    // Price validation methods
    public boolean isValidPrice(BigDecimal price) {
        if (price == null) return false;

        // Check minimum tick size
        if (tickSize != null && price.remainder(tickSize).compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }

        // Check price limits
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }

        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }

        return true;
    }

    public boolean isValidQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) return false;
        return lotSize == null || quantity % lotSize == 0;
    }
}