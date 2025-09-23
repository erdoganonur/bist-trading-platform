package com.bisttrading.core.domain.services;

import com.bisttrading.core.domain.valueobjects.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain service interface for price-related operations.
 * Encapsulates business logic related to pricing, quotes, and market data.
 */
public interface PriceService {

    /**
     * Gets the current market price for a symbol.
     *
     * @param symbol The trading symbol
     * @return Current market price or empty if not available
     */
    Optional<Price> getCurrentPrice(Symbol symbol);

    /**
     * Gets the current bid/ask quote for a symbol.
     *
     * @param symbol The trading symbol
     * @return Current quote or empty if not available
     */
    Optional<Quote> getCurrentQuote(Symbol symbol);

    /**
     * Validates if a price is within daily limits for a symbol.
     *
     * @param symbol    The trading symbol
     * @param price     The price to validate
     * @param reference The reference price (usually previous close)
     * @return Price validation result
     */
    PriceValidationResult validatePrice(Symbol symbol, Price price, Price reference);

    /**
     * Calculates the theoretical fair value for a symbol.
     *
     * @param symbol     The trading symbol
     * @param parameters Valuation parameters
     * @return Theoretical fair value
     */
    Money calculateFairValue(Symbol symbol, ValuationParameters parameters);

    /**
     * Gets historical prices for a symbol within a date range.
     *
     * @param symbol    The trading symbol
     * @param startDate Start date
     * @param endDate   End date
     * @return List of historical prices
     */
    List<HistoricalPrice> getHistoricalPrices(Symbol symbol, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Calculates price volatility for a symbol over a period.
     *
     * @param symbol    The trading symbol
     * @param periodDays Period in days
     * @return Volatility as percentage
     */
    BigDecimal calculateVolatility(Symbol symbol, int periodDays);

    /**
     * Calculates moving average for a symbol.
     *
     * @param symbol     The trading symbol
     * @param periodDays Period for moving average
     * @param type       Type of moving average
     * @return Moving average price
     */
    Price calculateMovingAverage(Symbol symbol, int periodDays, MovingAverageType type);

    /**
     * Gets price levels (support/resistance) for a symbol.
     *
     * @param symbol The trading symbol
     * @return Price levels
     */
    PriceLevels getPriceLevels(Symbol symbol);

    /**
     * Calculates price change percentage from previous close.
     *
     * @param symbol       The trading symbol
     * @param currentPrice Current price
     * @return Price change data
     */
    PriceChange calculatePriceChange(Symbol symbol, Price currentPrice);

    /**
     * Validates if a price matches the symbol's tick size rules.
     *
     * @param symbol The trading symbol
     * @param price  The price to validate
     * @return true if price is valid for the symbol
     */
    boolean isValidTickSize(Symbol symbol, Price price);

    /**
     * Rounds a price to the nearest valid tick for a symbol.
     *
     * @param symbol The trading symbol
     * @param price  The price to round
     * @return Rounded price
     */
    Price roundToValidTick(Symbol symbol, Price price);

    /**
     * Gets the appropriate tick size for a symbol based on price level.
     *
     * @param symbol The trading symbol
     * @param price  The price level
     * @return Tick size for the price level
     */
    BigDecimal getTickSizeForPrice(Symbol symbol, Price price);

    /**
     * Bid/Ask quote.
     */
    record Quote(
        Symbol symbol,
        Price bidPrice,
        Quantity bidQuantity,
        Price askPrice,
        Quantity askQuantity,
        Price lastPrice,
        LocalDateTime timestamp,
        BigDecimal spread,
        BigDecimal spreadPercentage
    ) {
        public Price getMidPrice() {
            BigDecimal mid = bidPrice.getValue().add(askPrice.getValue())
                .divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
            return Price.of(mid, symbol.getCode(), bidPrice.getTickSize());
        }

        public boolean isCrossed() {
            return bidPrice.isGreaterThan(askPrice);
        }

        public boolean isLocked() {
            return bidPrice.isEqualTo(askPrice);
        }
    }

    /**
     * Price validation result.
     */
    record PriceValidationResult(
        boolean isValid,
        String errorMessage,
        Price upperLimit,
        Price lowerLimit,
        BigDecimal changePercentage,
        boolean isAtLimit
    ) {
        public static PriceValidationResult valid(Price upperLimit, Price lowerLimit,
                                                BigDecimal changePercentage) {
            return new PriceValidationResult(true, null, upperLimit, lowerLimit,
                changePercentage, false);
        }

        public static PriceValidationResult atLimit(Price upperLimit, Price lowerLimit,
                                                  BigDecimal changePercentage, String limitType) {
            return new PriceValidationResult(true, "Fiyat " + limitType + " limitinde", upperLimit,
                lowerLimit, changePercentage, true);
        }

        public static PriceValidationResult invalid(String errorMessage) {
            return new PriceValidationResult(false, errorMessage, null, null, null, false);
        }
    }

    /**
     * Valuation parameters for fair value calculation.
     */
    record ValuationParameters(
        BigDecimal discountRate,
        BigDecimal growthRate,
        int projectionYears,
        BigDecimal marketMultiple,
        Money bookValue,
        Money earnings
    ) {}

    /**
     * Historical price data.
     */
    record HistoricalPrice(
        Symbol symbol,
        LocalDateTime timestamp,
        Price openPrice,
        Price highPrice,
        Price lowPrice,
        Price closePrice,
        Quantity volume,
        Money turnover
    ) {}

    /**
     * Moving average types.
     */
    enum MovingAverageType {
        SIMPLE("SMA", "Basit Hareketli Ortalama"),
        EXPONENTIAL("EMA", "Üstel Hareketli Ortalama"),
        WEIGHTED("WMA", "Ağırlıklı Hareketli Ortalama");

        private final String code;
        private final String description;

        MovingAverageType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * Price levels (support/resistance).
     */
    record PriceLevels(
        Symbol symbol,
        List<PriceLevel> supportLevels,
        List<PriceLevel> resistanceLevels,
        LocalDateTime calculatedAt
    ) {}

    /**
     * Individual price level.
     */
    record PriceLevel(
        Price price,
        BigDecimal strength,
        int touchCount,
        LocalDateTime lastTouch,
        String levelType
    ) {}

    /**
     * Price change information.
     */
    record PriceChange(
        Symbol symbol,
        Price currentPrice,
        Price previousClose,
        Money absoluteChange,
        BigDecimal percentageChange,
        Price dayHigh,
        Price dayLow,
        Quantity volume,
        String trend
    ) {
        public boolean isPositive() {
            return percentageChange.compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean isNegative() {
            return percentageChange.compareTo(BigDecimal.ZERO) < 0;
        }

        public boolean isUnchanged() {
            return percentageChange.compareTo(BigDecimal.ZERO) == 0;
        }
    }
}