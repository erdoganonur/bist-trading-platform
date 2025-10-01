package com.bisttrading.user.entity.trading;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import com.bisttrading.user.entity.User;
import com.bisttrading.user.entity.Organization;
import com.bisttrading.user.entity.trading.enums.PositionSide;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Entity representing a trading position with P&L calculations
 * Tracks real-time position data and risk metrics
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Entity
@Table(name = "positions",
    indexes = {
        @Index(name = "idx_positions_user_id", columnList = "user_id"),
        @Index(name = "idx_positions_account_id", columnList = "account_id"),
        @Index(name = "idx_positions_symbol_id", columnList = "symbol_id"),
        @Index(name = "idx_positions_side", columnList = "position_side"),
        @Index(name = "idx_positions_active", columnList = "user_id, account_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "positions_unique_account_symbol",
                         columnNames = {"user_id", "account_id", "symbol_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Position extends BaseEntity {

    /**
     * User who owns this position
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @ToString.Exclude
    private User user;

    /**
     * Organization (if position is held by organization)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @ToString.Exclude
    private Organization organization;

    /**
     * Broker account ID
     */
    @Column(name = "account_id", length = 50, nullable = false)
    @NotBlank(message = "Account ID is required")
    @Size(max = 50, message = "Account ID must not exceed 50 characters")
    private String accountId;

    /**
     * Sub-account ID (if applicable)
     */
    @Column(name = "sub_account_id", length = 50)
    @Size(max = 50, message = "Sub-account ID must not exceed 50 characters")
    private String subAccountId;

    /**
     * Symbol for this position
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    @NotNull(message = "Symbol is required")
    @ToString.Exclude
    private Symbol symbol;

    /**
     * Position side (LONG, SHORT, FLAT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "position_side", nullable = false)
    @Builder.Default
    private PositionSide positionSide = PositionSide.FLAT;

    /**
     * Current position quantity (positive for long, negative for short)
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /**
     * Quantity available for trading (not blocked by orders)
     */
    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    /**
     * Quantity blocked by pending orders
     */
    @Column(name = "blocked_quantity", nullable = false)
    @Builder.Default
    private Integer blockedQuantity = 0;

    /**
     * Average cost per share (cost basis)
     */
    @Column(name = "average_cost", precision = 20, scale = 6)
    @Positive(message = "Average cost must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid average cost format")
    private BigDecimal averageCost;

    /**
     * Total cost of position (quantity * average_cost)
     */
    @Column(name = "total_cost", precision = 20, scale = 6)
    @Digits(integer = 14, fraction = 6, message = "Invalid total cost format")
    private BigDecimal totalCost;

    /**
     * Realized P&L from closed positions
     */
    @Column(name = "realized_pnl", precision = 20, scale = 6, nullable = false)
    @Digits(integer = 14, fraction = 6, message = "Invalid realized P&L format")
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    /**
     * Unrealized P&L from current position
     */
    @Column(name = "unrealized_pnl", precision = 20, scale = 6)
    @Digits(integer = 14, fraction = 6, message = "Invalid unrealized P&L format")
    private BigDecimal unrealizedPnl;

    /**
     * Current market price of the symbol
     */
    @Column(name = "market_price", precision = 20, scale = 6)
    @Positive(message = "Market price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid market price format")
    private BigDecimal marketPrice;

    /**
     * Current market value of position (quantity * market_price)
     */
    @Column(name = "market_value", precision = 20, scale = 6)
    @Digits(integer = 14, fraction = 6, message = "Invalid market value format")
    private BigDecimal marketValue;

    /**
     * Day change in position value
     */
    @Column(name = "day_change", precision = 20, scale = 6)
    @Digits(integer = 14, fraction = 6, message = "Invalid day change format")
    private BigDecimal dayChange;

    /**
     * Day change as percentage
     */
    @Column(name = "day_change_percent", precision = 8, scale = 4)
    @DecimalMin(value = "-100.0", message = "Day change percentage cannot be less than -100%")
    @Digits(integer = 4, fraction = 4, message = "Invalid day change percentage format")
    private BigDecimal dayChangePercent;

    /**
     * Last trade price
     */
    @Column(name = "last_trade_price", precision = 20, scale = 6)
    @Positive(message = "Last trade price must be positive")
    @Digits(integer = 14, fraction = 6, message = "Invalid last trade price format")
    private BigDecimal lastTradePrice;

    /**
     * Timestamp of last trade
     */
    @Column(name = "last_trade_time")
    private ZonedDateTime lastTradeTime;

    /**
     * When price was last updated
     */
    @Column(name = "price_updated_at")
    private ZonedDateTime priceUpdatedAt;

    /**
     * Value at Risk (VaR) calculation
     */
    @Column(name = "value_at_risk", precision = 20, scale = 6)
    @PositiveOrZero(message = "Value at risk must be non-negative")
    @Digits(integer = 14, fraction = 6, message = "Invalid VaR format")
    private BigDecimal valueAtRisk;

    /**
     * Maximum drawdown since position opened
     */
    @Column(name = "maximum_drawdown", precision = 20, scale = 6)
    @PositiveOrZero(message = "Maximum drawdown must be non-negative")
    @Digits(integer = 14, fraction = 6, message = "Invalid maximum drawdown format")
    private BigDecimal maximumDrawdown;

    /**
     * Additional metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode metadata = null;

    // Business methods

    /**
     * Check if position is flat (no exposure)
     */
    public boolean isFlat() {
        return quantity == null || quantity == 0 || positionSide == PositionSide.FLAT;
    }

    /**
     * Check if position is long
     */
    public boolean isLong() {
        return positionSide == PositionSide.LONG && quantity != null && quantity > 0;
    }

    /**
     * Check if position is short
     */
    public boolean isShort() {
        return positionSide == PositionSide.SHORT && quantity != null && quantity < 0;
    }

    /**
     * Get absolute quantity (always positive)
     */
    public Integer getAbsoluteQuantity() {
        return quantity == null ? 0 : Math.abs(quantity);
    }

    /**
     * Update position with new trade
     */
    public void updateWithTrade(com.bisttrading.user.entity.trading.enums.OrderSide side,
                               Integer tradeQuantity, BigDecimal tradePrice) {
        if (tradeQuantity == null || tradeQuantity <= 0 || tradePrice == null) {
            throw new IllegalArgumentException("Invalid trade parameters");
        }

        int signedQuantity = side == com.bisttrading.user.entity.trading.enums.OrderSide.BUY ?
                           tradeQuantity : -tradeQuantity;

        updatePosition(signedQuantity, tradePrice);
    }

    /**
     * Internal method to update position calculations
     */
    private void updatePosition(int quantityChange, BigDecimal tradePrice) {
        int newQuantity = (quantity == null ? 0 : quantity) + quantityChange;

        if (quantity == null || quantity == 0) {
            // New position
            quantity = newQuantity;
            averageCost = tradePrice;
            totalCost = tradePrice.multiply(BigDecimal.valueOf(Math.abs(newQuantity)));
        } else if (Integer.signum(quantity) == Integer.signum(quantityChange)) {
            // Adding to existing position
            BigDecimal currentValue = averageCost.multiply(BigDecimal.valueOf(Math.abs(quantity)));
            BigDecimal addValue = tradePrice.multiply(BigDecimal.valueOf(Math.abs(quantityChange)));
            totalCost = currentValue.add(addValue);
            quantity = newQuantity;
            averageCost = totalCost.divide(BigDecimal.valueOf(Math.abs(quantity)), 6, java.math.RoundingMode.HALF_UP);
        } else {
            // Reducing or closing position
            int absCurrentQty = Math.abs(quantity);
            int absTradeQty = Math.abs(quantityChange);

            if (absTradeQty >= absCurrentQty) {
                // Closing or reversing position
                // Calculate realized P&L
                BigDecimal closedPnl = calculateRealizedPnl(absCurrentQty, tradePrice);
                realizedPnl = realizedPnl.add(closedPnl);

                int remainingQty = absTradeQty - absCurrentQty;
                if (remainingQty > 0) {
                    // Reversing position
                    quantity = Integer.signum(quantityChange) * remainingQty;
                    averageCost = tradePrice;
                    totalCost = tradePrice.multiply(BigDecimal.valueOf(remainingQty));
                } else {
                    // Closing position
                    quantity = 0;
                    averageCost = null;
                    totalCost = BigDecimal.ZERO;
                }
            } else {
                // Partially reducing position
                BigDecimal closedPnl = calculateRealizedPnl(absTradeQty, tradePrice);
                realizedPnl = realizedPnl.add(closedPnl);

                quantity = newQuantity;
                totalCost = averageCost.multiply(BigDecimal.valueOf(Math.abs(quantity)));
            }
        }

        // Update position side
        positionSide = PositionSide.fromQuantity(quantity);
        availableQuantity = quantity;

        // Update trade tracking
        lastTradePrice = tradePrice;
        lastTradeTime = ZonedDateTime.now();
    }

    /**
     * Calculate realized P&L for a partial or full close
     */
    private BigDecimal calculateRealizedPnl(int closedQuantity, BigDecimal closePrice) {
        if (averageCost == null || quantity == null) return BigDecimal.ZERO;

        BigDecimal costBasis = averageCost.multiply(BigDecimal.valueOf(closedQuantity));
        BigDecimal saleValue = closePrice.multiply(BigDecimal.valueOf(closedQuantity));

        if (quantity > 0) {
            // Long position - profit when sale > cost
            return saleValue.subtract(costBasis);
        } else {
            // Short position - profit when cost > sale
            return costBasis.subtract(saleValue);
        }
    }

    /**
     * Update market price and recalculate P&L
     */
    public void updateMarketPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Market price must be positive");
        }

        this.marketPrice = newPrice;
        this.priceUpdatedAt = ZonedDateTime.now();

        // Recalculate market value and unrealized P&L
        if (quantity != null && quantity != 0) {
            marketValue = newPrice.multiply(BigDecimal.valueOf(Math.abs(quantity)));

            if (averageCost != null) {
                BigDecimal costValue = averageCost.multiply(BigDecimal.valueOf(Math.abs(quantity)));

                if (quantity > 0) {
                    // Long position
                    unrealizedPnl = marketValue.subtract(costValue);
                } else {
                    // Short position
                    unrealizedPnl = costValue.subtract(marketValue);
                }
            }

            // Calculate day change if we have previous price
            if (lastTradePrice != null) {
                BigDecimal priceChange = newPrice.subtract(lastTradePrice);
                dayChange = priceChange.multiply(BigDecimal.valueOf(Math.abs(quantity)));
                dayChangePercent = priceChange.divide(lastTradePrice, 4, java.math.RoundingMode.HALF_UP)
                                             .multiply(BigDecimal.valueOf(100));
            }
        } else {
            marketValue = BigDecimal.ZERO;
            unrealizedPnl = BigDecimal.ZERO;
            dayChange = BigDecimal.ZERO;
            dayChangePercent = BigDecimal.ZERO;
        }
    }

    /**
     * Get total P&L (realized + unrealized)
     */
    public BigDecimal getTotalPnl() {
        BigDecimal total = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        if (unrealizedPnl != null) {
            total = total.add(unrealizedPnl);
        }
        return total;
    }

    /**
     * Get P&L percentage based on cost basis
     */
    public BigDecimal getPnlPercentage() {
        if (totalCost == null || totalCost.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        return getTotalPnl().divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Block quantity for pending orders
     */
    public void blockQuantity(Integer quantityToBlock) {
        if (quantityToBlock == null || quantityToBlock <= 0) {
            throw new IllegalArgumentException("Quantity to block must be positive");
        }

        if (availableQuantity < quantityToBlock) {
            throw new IllegalArgumentException("Insufficient available quantity to block");
        }

        availableQuantity -= quantityToBlock;
        blockedQuantity += quantityToBlock;
    }

    /**
     * Release blocked quantity when order is completed/cancelled
     */
    public void releaseQuantity(Integer quantityToRelease) {
        if (quantityToRelease == null || quantityToRelease <= 0) {
            throw new IllegalArgumentException("Quantity to release must be positive");
        }

        if (blockedQuantity < quantityToRelease) {
            throw new IllegalArgumentException("Cannot release more quantity than blocked");
        }

        blockedQuantity -= quantityToRelease;
        availableQuantity += quantityToRelease;
    }

    /**
     * Get position exposure in base currency
     */
    public BigDecimal getExposure() {
        if (marketValue == null) return BigDecimal.ZERO;
        return marketValue.abs();
    }

    /**
     * Check if position has significant exposure (> threshold)
     */
    public boolean hasSignificantExposure(BigDecimal threshold) {
        if (threshold == null) return !isFlat();
        return getExposure().compareTo(threshold) > 0;
    }

    /**
     * Get leverage ratio (market value / margin used)
     */
    public BigDecimal getLeverage(BigDecimal marginUsed) {
        if (marginUsed == null || marginUsed.equals(BigDecimal.ZERO) || marketValue == null) {
            return BigDecimal.ZERO;
        }
        return marketValue.divide(marginUsed, 2, java.math.RoundingMode.HALF_UP);
    }

    // Validation methods

    @PrePersist
    @PreUpdate
    private void validate() {
        // Validate quantity relationships
        if (quantity != null && availableQuantity != null && blockedQuantity != null) {
            if (Math.abs(quantity) != availableQuantity + blockedQuantity) {
                throw new IllegalArgumentException("Quantity must equal available + blocked quantity");
            }
        }

        // Validate position side consistency
        if (quantity != null) {
            PositionSide calculatedSide = PositionSide.fromQuantity(quantity);
            if (positionSide != calculatedSide) {
                positionSide = calculatedSide;
            }
        }

        // Validate cost basis for open positions
        if (quantity != null && quantity != 0) {
            if (averageCost == null || averageCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Non-zero positions must have positive average cost");
            }
        }

        // Set default values
        if (quantity == null) quantity = 0;
        if (availableQuantity == null) availableQuantity = quantity;
        if (blockedQuantity == null) blockedQuantity = 0;
        if (realizedPnl == null) realizedPnl = BigDecimal.ZERO;

        // Update position side
        positionSide = PositionSide.fromQuantity(quantity);
    }
}