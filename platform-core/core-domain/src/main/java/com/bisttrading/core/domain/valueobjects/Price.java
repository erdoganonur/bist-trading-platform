package com.bisttrading.core.domain.valueobjects;

import com.bisttrading.core.common.exceptions.ValidationException;
import com.bisttrading.core.common.constants.ErrorCodes;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Price value object representing a stock price with symbol and tick size validation.
 * Immutable and provides price-related operations.
 */
@Getter
@EqualsAndHashCode
public final class Price {

    private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");
    private static final BigDecimal DEFAULT_TICK_SIZE = new BigDecimal("0.01");
    private static final int PRICE_SCALE = 4; // 4 decimal places for prices

    private final BigDecimal value;
    private final String symbol;
    private final BigDecimal tickSize;

    private Price(BigDecimal value, String symbol, BigDecimal tickSize) {
        this.value = Objects.requireNonNull(value, "Fiyat değeri boş olamaz");
        this.symbol = Objects.requireNonNull(symbol, "Sembol boş olamaz").trim().toUpperCase();
        this.tickSize = Objects.requireNonNull(tickSize, "Tick boyutu boş olamaz");

        validatePrice(value);
        validateTickSize(tickSize);
        validateSymbol(this.symbol);
        validatePriceAgainstTickSize(value, tickSize);
    }

    /**
     * Creates a Price instance.
     *
     * @param value    The price value
     * @param symbol   The stock symbol
     * @param tickSize The minimum price increment
     * @return Price instance
     */
    public static Price of(BigDecimal value, String symbol, BigDecimal tickSize) {
        return new Price(value, symbol, tickSize);
    }

    /**
     * Creates a Price instance with default tick size.
     *
     * @param value  The price value
     * @param symbol The stock symbol
     * @return Price instance
     */
    public static Price of(BigDecimal value, String symbol) {
        return new Price(value, symbol, DEFAULT_TICK_SIZE);
    }

    /**
     * Creates a Price instance from double.
     *
     * @param value    The price value
     * @param symbol   The stock symbol
     * @param tickSize The minimum price increment
     * @return Price instance
     */
    public static Price of(double value, String symbol, double tickSize) {
        return new Price(
            BigDecimal.valueOf(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
            symbol,
            BigDecimal.valueOf(tickSize).setScale(PRICE_SCALE, RoundingMode.HALF_UP)
        );
    }

    /**
     * Creates a Price instance from double with default tick size.
     *
     * @param value  The price value
     * @param symbol The stock symbol
     * @return Price instance
     */
    public static Price of(double value, String symbol) {
        return of(BigDecimal.valueOf(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP), symbol);
    }

    /**
     * Creates a Price instance from string.
     *
     * @param value    The price value as string
     * @param symbol   The stock symbol
     * @param tickSize The minimum price increment as string
     * @return Price instance
     */
    public static Price of(String value, String symbol, String tickSize) {
        try {
            return new Price(
                new BigDecimal(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                symbol,
                new BigDecimal(tickSize).setScale(PRICE_SCALE, RoundingMode.HALF_UP)
            );
        } catch (NumberFormatException e) {
            throw new ValidationException(ErrorCodes.INVALID_NUMBER_FORMAT,
                "Geçersiz fiyat formatı: " + value);
        }
    }

    /**
     * Adds a price increment (ticks) to this price.
     *
     * @param ticks Number of ticks to add
     * @return New Price instance
     */
    public Price addTicks(int ticks) {
        if (ticks < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Tick sayısı negatif olamaz");
        }

        BigDecimal increment = tickSize.multiply(BigDecimal.valueOf(ticks));
        BigDecimal newValue = value.add(increment);
        return new Price(newValue, symbol, tickSize);
    }

    /**
     * Subtracts price increments (ticks) from this price.
     *
     * @param ticks Number of ticks to subtract
     * @return New Price instance
     */
    public Price subtractTicks(int ticks) {
        if (ticks < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Tick sayısı negatif olamaz");
        }

        BigDecimal decrement = tickSize.multiply(BigDecimal.valueOf(ticks));
        BigDecimal newValue = value.subtract(decrement);

        if (newValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_PRICE,
                "Fiyat sıfır veya negatif olamaz");
        }

        return new Price(newValue, symbol, tickSize);
    }

    /**
     * Calculates percentage change from another price.
     *
     * @param otherPrice The base price for comparison
     * @return Percentage change as BigDecimal
     */
    public BigDecimal percentageChangeFrom(Price otherPrice) {
        validateSameSymbol(otherPrice);

        if (otherPrice.value.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Baz fiyat sıfır olamaz");
        }

        BigDecimal difference = this.value.subtract(otherPrice.value);
        return difference.divide(otherPrice.value, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Calculates price difference from another price.
     *
     * @param otherPrice The price to compare
     * @return Price difference
     */
    public BigDecimal differenceFrom(Price otherPrice) {
        validateSameSymbol(otherPrice);
        return this.value.subtract(otherPrice.value);
    }

    /**
     * Checks if this price is within price ceiling/floor limits.
     *
     * @param floor   The minimum allowed price
     * @param ceiling The maximum allowed price
     * @return true if within limits
     */
    public boolean isWithinLimits(Price floor, Price ceiling) {
        validateSameSymbol(floor);
        validateSameSymbol(ceiling);

        return this.value.compareTo(floor.value) >= 0 &&
               this.value.compareTo(ceiling.value) <= 0;
    }

    /**
     * Validates if this price is within daily limits based on reference price.
     *
     * @param referencePrice The reference price (usually previous close)
     * @param limitPercentage The daily limit percentage (e.g., 10 for ±10%)
     * @return true if within daily limits
     */
    public boolean isWithinDailyLimits(Price referencePrice, BigDecimal limitPercentage) {
        validateSameSymbol(referencePrice);

        BigDecimal limitAmount = referencePrice.value
            .multiply(limitPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        BigDecimal upperLimit = referencePrice.value.add(limitAmount);
        BigDecimal lowerLimit = referencePrice.value.subtract(limitAmount);

        return this.value.compareTo(lowerLimit) >= 0 &&
               this.value.compareTo(upperLimit) <= 0;
    }

    /**
     * Rounds this price to the nearest tick size.
     *
     * @return New Price instance rounded to tick size
     */
    public Price roundToTickSize() {
        BigDecimal remainder = value.remainder(tickSize);
        BigDecimal halfTick = tickSize.divide(new BigDecimal("2"), PRICE_SCALE, RoundingMode.HALF_UP);

        BigDecimal roundedValue;
        if (remainder.compareTo(halfTick) >= 0) {
            // Round up
            roundedValue = value.subtract(remainder).add(tickSize);
        } else {
            // Round down
            roundedValue = value.subtract(remainder);
        }

        return new Price(roundedValue, symbol, tickSize);
    }

    /**
     * Checks if this price is greater than another price.
     *
     * @param other The price to compare
     * @return true if this is greater
     */
    public boolean isGreaterThan(Price other) {
        validateSameSymbol(other);
        return this.value.compareTo(other.value) > 0;
    }

    /**
     * Checks if this price is less than another price.
     *
     * @param other The price to compare
     * @return true if this is less
     */
    public boolean isLessThan(Price other) {
        validateSameSymbol(other);
        return this.value.compareTo(other.value) < 0;
    }

    /**
     * Checks if this price equals another price.
     *
     * @param other The price to compare
     * @return true if prices are equal
     */
    public boolean isEqualTo(Price other) {
        validateSameSymbol(other);
        return this.value.compareTo(other.value) == 0;
    }

    /**
     * Converts this price to Money using TRY currency.
     *
     * @return Money instance in TRY
     */
    public Money toMoney() {
        return Money.of(value, Currency.TRY);
    }

    /**
     * Converts this price to Money using specified currency.
     *
     * @param currency The target currency
     * @return Money instance in specified currency
     */
    public Money toMoney(Currency currency) {
        return Money.of(value, currency);
    }

    /**
     * Formats this price for display.
     *
     * @return Formatted string
     */
    public String format() {
        NumberFormat formatter = NumberFormat.getNumberInstance(TURKISH_LOCALE);
        formatter.setMaximumFractionDigits(PRICE_SCALE);
        formatter.setMinimumFractionDigits(2);
        return formatter.format(value) + " ₺";
    }

    /**
     * Formats this price with symbol.
     *
     * @return Formatted string with symbol
     */
    public String formatWithSymbol() {
        return symbol + ": " + format();
    }

    /**
     * Validates price value.
     */
    private void validatePrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_PRICE,
                "Fiyat pozitif olmalıdır");
        }

        if (price.scale() > PRICE_SCALE) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_PRICE,
                "Fiyat en fazla " + PRICE_SCALE + " ondalık basamağa sahip olabilir");
        }
    }

    /**
     * Validates tick size.
     */
    private void validateTickSize(BigDecimal tickSize) {
        if (tickSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Tick boyutu pozitif olmalıdır");
        }

        if (tickSize.scale() > PRICE_SCALE) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Tick boyutu en fazla " + PRICE_SCALE + " ondalık basamağa sahip olabilir");
        }
    }

    /**
     * Validates symbol format.
     */
    private void validateSymbol(String symbol) {
        if (symbol.isEmpty() || symbol.length() > 10) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Sembol 1-10 karakter arasında olmalıdır");
        }

        if (!symbol.matches("^[A-Z0-9]+$")) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Sembol sadece büyük harf ve rakam içerebilir");
        }
    }

    /**
     * Validates that price is aligned with tick size.
     */
    private void validatePriceAgainstTickSize(BigDecimal price, BigDecimal tickSize) {
        BigDecimal remainder = price.remainder(tickSize);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_PRICE,
                "Fiyat tick boyutunun katı olmalıdır");
        }
    }

    /**
     * Validates that two prices have the same symbol.
     */
    private void validateSameSymbol(Price other) {
        if (other == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Karşılaştırılan fiyat objesi boş olamaz");
        }

        if (!this.symbol.equals(other.symbol)) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                String.format("Fiyat sembolleri farklı: %s ve %s", this.symbol, other.symbol));
        }
    }

    @Override
    public String toString() {
        return formatWithSymbol();
    }

    /**
     * Builder for Price instances.
     */
    public static class Builder {
        private BigDecimal value;
        private String symbol;
        private BigDecimal tickSize = DEFAULT_TICK_SIZE;

        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder value(double value) {
            this.value = BigDecimal.valueOf(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            return this;
        }

        public Builder value(String value) {
            this.value = new BigDecimal(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            return this;
        }

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder tickSize(BigDecimal tickSize) {
            this.tickSize = tickSize;
            return this;
        }

        public Builder tickSize(double tickSize) {
            this.tickSize = BigDecimal.valueOf(tickSize).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            return this;
        }

        public Price build() {
            return Price.of(value, symbol, tickSize);
        }
    }

    /**
     * Creates a new Builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}