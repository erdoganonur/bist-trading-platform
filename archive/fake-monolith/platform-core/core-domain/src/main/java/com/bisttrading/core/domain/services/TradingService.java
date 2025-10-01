package com.bisttrading.core.domain.services;

import com.bisttrading.core.domain.valueobjects.*;
import com.bisttrading.core.domain.events.OrderCreatedEvent;
import com.bisttrading.core.domain.events.PositionOpenedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain service interface for trading operations.
 * Encapsulates business logic related to trading activities.
 */
public interface TradingService {

    /**
     * Validates if an order can be placed for the given parameters.
     *
     * @param symbol          The trading symbol
     * @param orderType       The type of order
     * @param orderSide       Buy or sell
     * @param quantity        Order quantity
     * @param price           Order price (null for market orders)
     * @param availableBalance Available user balance
     * @return OrderValidationResult
     */
    OrderValidationResult validateOrder(Symbol symbol, OrderCreatedEvent.OrderType orderType,
                                       OrderCreatedEvent.OrderSide orderSide, Quantity quantity,
                                       Price price, Money availableBalance);

    /**
     * Calculates the total order value including commission and fees.
     *
     * @param symbol    The trading symbol
     * @param quantity  Order quantity
     * @param price     Order price
     * @param orderSide Buy or sell
     * @return OrderValue containing breakdown of costs
     */
    OrderValue calculateOrderValue(Symbol symbol, Quantity quantity, Price price,
                                 OrderCreatedEvent.OrderSide orderSide);

    /**
     * Calculates commission for an order.
     *
     * @param orderValue The order value
     * @param symbol     The trading symbol
     * @param userId     The user ID (for tier-based commission)
     * @return Commission amount
     */
    Money calculateCommission(Money orderValue, Symbol symbol, String userId);

    /**
     * Checks if trading is allowed for a symbol at the given time.
     *
     * @param symbol      The trading symbol
     * @param currentTime The current time
     * @return true if trading is allowed
     */
    boolean isTradingAllowed(Symbol symbol, LocalDateTime currentTime);

    /**
     * Gets the current market status for a symbol.
     *
     * @param symbol The trading symbol
     * @return Market status
     */
    MarketStatus getMarketStatus(Symbol symbol);

    /**
     * Calculates position size recommendation based on risk parameters.
     *
     * @param symbol            The trading symbol
     * @param accountValue      Total account value
     * @param riskPercentage    Risk percentage per trade
     * @param stopLossPrice     Stop loss price
     * @param entryPrice        Entry price
     * @return Recommended position size
     */
    Quantity calculatePositionSize(Symbol symbol, Money accountValue, BigDecimal riskPercentage,
                                 Price stopLossPrice, Price entryPrice);

    /**
     * Validates if a position can be opened with given parameters.
     *
     * @param symbol           The trading symbol
     * @param positionSide     Long or short
     * @param quantity         Position quantity
     * @param accountValue     Account value
     * @param existingPositions Existing positions for risk calculation
     * @return PositionValidationResult
     */
    PositionValidationResult validatePosition(Symbol symbol, PositionOpenedEvent.PositionSide positionSide,
                                            Quantity quantity, Money accountValue,
                                            List<ExistingPosition> existingPositions);

    /**
     * Order validation result.
     */
    record OrderValidationResult(
        boolean isValid,
        String errorMessage,
        List<String> warnings,
        Money requiredBalance,
        Money marginRequirement
    ) {
        public static OrderValidationResult valid(Money requiredBalance, Money marginRequirement) {
            return new OrderValidationResult(true, null, List.of(), requiredBalance, marginRequirement);
        }

        public static OrderValidationResult validWithWarnings(Money requiredBalance, Money marginRequirement,
                                                            List<String> warnings) {
            return new OrderValidationResult(true, null, warnings, requiredBalance, marginRequirement);
        }

        public static OrderValidationResult invalid(String errorMessage) {
            return new OrderValidationResult(false, errorMessage, List.of(), null, null);
        }
    }

    /**
     * Order value breakdown.
     */
    record OrderValue(
        Money baseValue,
        Money commission,
        Money fees,
        Money totalValue,
        BigDecimal commissionRate,
        String breakdown
    ) {}

    /**
     * Market status enumeration.
     */
    enum MarketStatus {
        OPEN("OPEN", "Açık"),
        CLOSED("CLOSED", "Kapalı"),
        PRE_MARKET("PRE_MARKET", "Açılış Öncesi"),
        AFTER_HOURS("AFTER_HOURS", "Mesai Sonrası"),
        SUSPENDED("SUSPENDED", "Askıda"),
        HALT("HALT", "Durduruldu");

        private final String code;
        private final String description;

        MarketStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public boolean isTradingAllowed() {
            return this == OPEN || this == PRE_MARKET || this == AFTER_HOURS;
        }
    }

    /**
     * Position validation result.
     */
    record PositionValidationResult(
        boolean isValid,
        String errorMessage,
        List<String> warnings,
        Money marginRequirement,
        BigDecimal riskPercentage,
        String riskCategory
    ) {
        public static PositionValidationResult valid(Money marginRequirement, BigDecimal riskPercentage,
                                                   String riskCategory) {
            return new PositionValidationResult(true, null, List.of(), marginRequirement,
                riskPercentage, riskCategory);
        }

        public static PositionValidationResult validWithWarnings(Money marginRequirement, BigDecimal riskPercentage,
                                                               String riskCategory, List<String> warnings) {
            return new PositionValidationResult(true, null, warnings, marginRequirement,
                riskPercentage, riskCategory);
        }

        public static PositionValidationResult invalid(String errorMessage) {
            return new PositionValidationResult(false, errorMessage, List.of(), null, null, null);
        }
    }

    /**
     * Existing position data for risk calculations.
     */
    record ExistingPosition(
        String positionId,
        Symbol symbol,
        PositionOpenedEvent.PositionSide side,
        Quantity quantity,
        Money currentValue,
        Money unrealizedPnL
    ) {}
}