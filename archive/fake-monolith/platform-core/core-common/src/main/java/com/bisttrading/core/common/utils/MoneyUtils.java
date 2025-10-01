package com.bisttrading.core.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for money and currency operations with Turkish locale support.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoneyUtils {

    /**
     * Turkish locale for formatting.
     */
    public static final Locale TURKISH_LOCALE = new Locale("tr", "TR");

    /**
     * Default scale for monetary calculations.
     */
    public static final int DEFAULT_SCALE = 2;

    /**
     * Scale for high precision calculations.
     */
    public static final int HIGH_PRECISION_SCALE = 4;

    /**
     * Scale for percentage calculations.
     */
    public static final int PERCENTAGE_SCALE = 4;

    /**
     * Default rounding mode for monetary calculations.
     */
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Zero amount with default scale.
     */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);

    /**
     * One hundred for percentage calculations.
     */
    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Turkish Lira currency formatter.
     */
    private static final NumberFormat TRY_FORMATTER = NumberFormat.getCurrencyInstance(TURKISH_LOCALE);

    /**
     * Turkish number formatter.
     */
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getNumberInstance(TURKISH_LOCALE);

    static {
        TRY_FORMATTER.setMaximumFractionDigits(DEFAULT_SCALE);
        TRY_FORMATTER.setMinimumFractionDigits(DEFAULT_SCALE);
        NUMBER_FORMATTER.setMaximumFractionDigits(DEFAULT_SCALE);
        NUMBER_FORMATTER.setMinimumFractionDigits(DEFAULT_SCALE);
    }

    /**
     * Creates a BigDecimal with default scale and rounding.
     *
     * @param value The value
     * @return BigDecimal with default scale
     */
    public static BigDecimal of(String value) {
        return new BigDecimal(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Creates a BigDecimal with default scale and rounding.
     *
     * @param value The value
     * @return BigDecimal with default scale
     */
    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Creates a BigDecimal with default scale and rounding.
     *
     * @param value The value
     * @return BigDecimal with default scale
     */
    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Creates a BigDecimal with specified scale and default rounding.
     *
     * @param value The value
     * @param scale The scale
     * @return BigDecimal with specified scale
     */
    public static BigDecimal of(String value, int scale) {
        return new BigDecimal(value).setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Creates a BigDecimal with high precision scale.
     *
     * @param value The value
     * @return BigDecimal with high precision scale
     */
    public static BigDecimal ofHighPrecision(String value) {
        return new BigDecimal(value).setScale(HIGH_PRECISION_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Ensures the BigDecimal has the default scale.
     *
     * @param value The value to scale
     * @return BigDecimal with default scale
     */
    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Ensures the BigDecimal has the specified scale.
     *
     * @param value The value to scale
     * @param scale The scale
     * @return BigDecimal with specified scale
     */
    public static BigDecimal scale(BigDecimal value, int scale) {
        return value.setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Adds two monetary values.
     *
     * @param a First value
     * @param b Second value
     * @return Sum with default scale
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Subtracts two monetary values.
     *
     * @param a First value
     * @param b Second value
     * @return Difference with default scale
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Multiplies two monetary values.
     *
     * @param a First value
     * @param b Second value
     * @return Product with default scale
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Divides two monetary values.
     *
     * @param a First value
     * @param b Second value
     * @return Quotient with default scale
     * @throws ArithmeticException if b is zero
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (isZero(b)) {
            throw new ArithmeticException("Sıfıra bölme hatası");
        }
        return a.divide(b, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Divides two monetary values with high precision.
     *
     * @param a First value
     * @param b Second value
     * @return Quotient with high precision scale
     * @throws ArithmeticException if b is zero
     */
    public static BigDecimal divideHighPrecision(BigDecimal a, BigDecimal b) {
        if (isZero(b)) {
            throw new ArithmeticException("Sıfıra bölme hatası");
        }
        return a.divide(b, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Calculates percentage of a value.
     *
     * @param value      The base value
     * @param percentage The percentage
     * @return Percentage amount with default scale
     */
    public static BigDecimal percentage(BigDecimal value, BigDecimal percentage) {
        return multiply(value, divide(percentage, ONE_HUNDRED));
    }

    /**
     * Calculates percentage change between two values.
     *
     * @param oldValue The old value
     * @param newValue The new value
     * @return Percentage change with percentage scale
     * @throws ArithmeticException if oldValue is zero
     */
    public static BigDecimal percentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (isZero(oldValue)) {
            throw new ArithmeticException("Eski değer sıfır olamaz");
        }
        BigDecimal change = subtract(newValue, oldValue);
        return change.divide(oldValue, PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE)
                     .multiply(ONE_HUNDRED);
    }

    /**
     * Calculates the absolute value.
     *
     * @param value The value
     * @return Absolute value with same scale
     */
    public static BigDecimal abs(BigDecimal value) {
        return value.abs();
    }

    /**
     * Returns the maximum of two values.
     *
     * @param a First value
     * @param b Second value
     * @return Maximum value
     */
    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.max(b);
    }

    /**
     * Returns the minimum of two values.
     *
     * @param a First value
     * @param b Second value
     * @return Minimum value
     */
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.min(b);
    }

    /**
     * Checks if a value is zero.
     *
     * @param value The value to check
     * @return true if the value is zero
     */
    public static boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if a value is positive.
     *
     * @param value The value to check
     * @return true if the value is positive
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if a value is negative.
     *
     * @param value The value to check
     * @return true if the value is negative
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if a value is greater than another value.
     *
     * @param a First value
     * @param b Second value
     * @return true if a > b
     */
    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) > 0;
    }

    /**
     * Checks if a value is greater than or equal to another value.
     *
     * @param a First value
     * @param b Second value
     * @return true if a >= b
     */
    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) >= 0;
    }

    /**
     * Checks if a value is less than another value.
     *
     * @param a First value
     * @param b Second value
     * @return true if a < b
     */
    public static boolean isLessThan(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) < 0;
    }

    /**
     * Checks if a value is less than or equal to another value.
     *
     * @param a First value
     * @param b Second value
     * @return true if a <= b
     */
    public static boolean isLessThanOrEqual(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) <= 0;
    }

    /**
     * Checks if two values are equal.
     *
     * @param a First value
     * @param b Second value
     * @return true if a == b
     */
    public static boolean isEqual(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    /**
     * Formats a monetary value as Turkish Lira.
     *
     * @param value The value to format
     * @return Formatted string (e.g., "1.234,56 ₺")
     */
    public static String formatTRY(BigDecimal value) {
        if (value == null) {
            return "0,00 ₺";
        }
        return TRY_FORMATTER.format(value);
    }

    /**
     * Formats a monetary value as a number with Turkish locale.
     *
     * @param value The value to format
     * @return Formatted string (e.g., "1.234,56")
     */
    public static String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return NUMBER_FORMATTER.format(value);
    }

    /**
     * Formats a percentage value.
     *
     * @param value The percentage value
     * @return Formatted string (e.g., "%12,34")
     */
    public static String formatPercentage(BigDecimal value) {
        if (value == null) {
            return "%0,00";
        }
        return "%" + NUMBER_FORMATTER.format(value);
    }

    /**
     * Parses a Turkish formatted number string.
     *
     * @param value The formatted string (e.g., "1.234,56")
     * @return Parsed BigDecimal
     * @throws java.text.ParseException if the string cannot be parsed
     */
    public static BigDecimal parseNumber(String value) throws java.text.ParseException {
        if (StringUtils.isBlank(value)) {
            return ZERO;
        }
        Number number = NUMBER_FORMATTER.parse(value.trim());
        return of(number.toString());
    }

    /**
     * Safely parses a Turkish formatted number string.
     *
     * @param value        The formatted string
     * @param defaultValue Default value if parsing fails
     * @return Parsed BigDecimal or default value
     */
    public static BigDecimal parseNumberSafely(String value, BigDecimal defaultValue) {
        try {
            return parseNumber(value);
        } catch (Exception e) {
            return defaultValue != null ? defaultValue : ZERO;
        }
    }

    /**
     * Rounds a value to the nearest tick size.
     *
     * @param value    The value to round
     * @param tickSize The tick size
     * @return Rounded value
     */
    public static BigDecimal roundToTickSize(BigDecimal value, BigDecimal tickSize) {
        if (isZero(tickSize)) {
            return value;
        }

        BigDecimal divided = divideHighPrecision(value, tickSize);
        BigDecimal rounded = divided.setScale(0, DEFAULT_ROUNDING_MODE);
        return multiply(rounded, tickSize);
    }

    /**
     * Calculates compound interest.
     *
     * @param principal   The principal amount
     * @param rate        The annual interest rate (as percentage)
     * @param periods     The number of compounding periods
     * @param yearsPerPeriod Years per period (e.g., 1/12 for monthly)
     * @return Final amount after compound interest
     */
    public static BigDecimal compoundInterest(BigDecimal principal, BigDecimal rate,
                                            int periods, BigDecimal yearsPerPeriod) {
        if (periods == 0) {
            return principal;
        }

        BigDecimal ratePerPeriod = divide(rate, ONE_HUNDRED);
        BigDecimal periodRate = multiply(ratePerPeriod, yearsPerPeriod);
        BigDecimal onePlusRate = add(BigDecimal.ONE, periodRate);

        BigDecimal factor = BigDecimal.ONE;
        for (int i = 0; i < periods; i++) {
            factor = multiply(factor, onePlusRate);
        }

        return multiply(principal, factor);
    }

    /**
     * Validates that a value is within a specified range.
     *
     * @param value The value to validate
     * @param min   Minimum allowed value (inclusive)
     * @param max   Maximum allowed value (inclusive)
     * @return true if value is within range
     */
    public static boolean isWithinRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null &&
               isGreaterThanOrEqual(value, min) &&
               isLessThanOrEqual(value, max);
    }

    /**
     * Ensures a value is not null, returning zero if it is.
     *
     * @param value The value to check
     * @return The value or zero if null
     */
    public static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    /**
     * Returns zero if the value is null, otherwise returns the value with default scale.
     *
     * @param value The value to process
     * @return Scaled value or zero
     */
    public static BigDecimal scaleOrZero(BigDecimal value) {
        return value != null ? scale(value) : ZERO;
    }
}