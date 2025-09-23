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
 * Money value object representing an amount and currency.
 * Immutable and provides monetary arithmetic operations.
 */
@Getter
@EqualsAndHashCode
public final class Money {

    private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "Miktar boş olamaz");
        this.currency = Objects.requireNonNull(currency, "Para birimi boş olamaz");

        if (amount.scale() > currency.getDecimalPlaces()) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Miktar ondalık basamak sayısı para biriminin desteklediği sayıdan fazla olamaz");
        }
    }

    /**
     * Creates a Money instance.
     *
     * @param amount   The monetary amount
     * @param currency The currency
     * @return Money instance
     */
    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.INVALID_AMOUNT,
                "Miktar negatif olamaz");
        }

        // Round to currency's decimal places
        BigDecimal roundedAmount = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        return new Money(roundedAmount, currency);
    }

    /**
     * Creates a Money instance from double.
     *
     * @param amount   The monetary amount
     * @param currency The currency
     * @return Money instance
     */
    public static Money of(double amount, Currency currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Creates a Money instance from string.
     *
     * @param amount   The monetary amount as string
     * @param currency The currency
     * @return Money instance
     */
    public static Money of(String amount, Currency currency) {
        try {
            return of(new BigDecimal(amount), currency);
        } catch (NumberFormatException e) {
            throw new ValidationException(ErrorCodes.INVALID_NUMBER_FORMAT,
                "Geçersiz miktar formatı: " + amount);
        }
    }

    /**
     * Creates zero money for given currency.
     *
     * @param currency The currency
     * @return Zero Money instance
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO.setScale(currency.getDecimalPlaces()), currency);
    }

    /**
     * Adds another Money to this Money.
     * Both must have the same currency.
     *
     * @param other The Money to add
     * @return New Money instance with sum
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money from this Money.
     * Both must have the same currency.
     *
     * @param other The Money to subtract
     * @return New Money instance with difference
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.INSUFFICIENT_BALANCE,
                "İşlem sonucu negatif miktar oluşturamaz");
        }

        return new Money(result, this.currency);
    }

    /**
     * Multiplies this Money by a factor.
     *
     * @param factor The multiplication factor
     * @return New Money instance with product
     */
    public Money multiply(BigDecimal factor) {
        if (factor == null || factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Çarpan negatif olamaz");
        }

        BigDecimal result = this.amount.multiply(factor)
            .setScale(this.currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        return new Money(result, this.currency);
    }

    /**
     * Multiplies this Money by a factor.
     *
     * @param factor The multiplication factor
     * @return New Money instance with product
     */
    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * Divides this Money by a divisor.
     *
     * @param divisor The division divisor
     * @return New Money instance with quotient
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Bölen pozitif olmalıdır");
        }

        BigDecimal result = this.amount.divide(divisor, this.currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        return new Money(result, this.currency);
    }

    /**
     * Calculates percentage of this Money.
     *
     * @param percentage The percentage (e.g., 10 for 10%)
     * @return New Money instance with percentage amount
     */
    public Money percentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Yüzde oranı boş olamaz");
        }

        return multiply(percentage.divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    /**
     * Calculates percentage of this Money.
     *
     * @param percentage The percentage (e.g., 10 for 10%)
     * @return New Money instance with percentage amount
     */
    public Money percentage(double percentage) {
        return percentage(BigDecimal.valueOf(percentage));
    }

    /**
     * Checks if this Money is zero.
     *
     * @return true if amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this Money is positive.
     *
     * @return true if amount is greater than zero
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this Money is greater than another Money.
     *
     * @param other The Money to compare
     * @return true if this is greater
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this Money is greater than or equal to another Money.
     *
     * @param other The Money to compare
     * @return true if this is greater or equal
     */
    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Checks if this Money is less than another Money.
     *
     * @param other The Money to compare
     * @return true if this is less
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Formats this Money for display using Turkish locale.
     *
     * @return Formatted string
     */
    public String format() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(TURKISH_LOCALE);
        formatter.setMaximumFractionDigits(currency.getDecimalPlaces());
        formatter.setMinimumFractionDigits(currency.getDecimalPlaces());

        // For non-TRY currencies, use their symbol
        if (currency != Currency.TRY) {
            formatter = NumberFormat.getNumberInstance(TURKISH_LOCALE);
            formatter.setMaximumFractionDigits(currency.getDecimalPlaces());
            formatter.setMinimumFractionDigits(currency.getDecimalPlaces());
            return formatter.format(amount) + " " + currency.getSymbol();
        }

        return formatter.format(amount);
    }

    /**
     * Formats this Money for display with custom pattern.
     *
     * @param includeSymbol Whether to include currency symbol
     * @return Formatted string
     */
    public String format(boolean includeSymbol) {
        NumberFormat formatter = NumberFormat.getNumberInstance(TURKISH_LOCALE);
        formatter.setMaximumFractionDigits(currency.getDecimalPlaces());
        formatter.setMinimumFractionDigits(currency.getDecimalPlaces());

        String formatted = formatter.format(amount);

        if (includeSymbol) {
            return formatted + " " + currency.getSymbol();
        }

        return formatted;
    }

    /**
     * Gets the absolute value of this Money.
     *
     * @return New Money instance with absolute amount
     */
    public Money abs() {
        return new Money(amount.abs(), currency);
    }

    /**
     * Validates that two Money instances have the same currency.
     *
     * @param other The other Money instance
     */
    private void validateSameCurrency(Money other) {
        if (other == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Karşılaştırılan para objesi boş olamaz");
        }

        if (!this.currency.equals(other.currency)) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                String.format("Para birimleri farklı: %s ve %s",
                    this.currency.getCode(), other.currency.getCode()));
        }
    }

    @Override
    public String toString() {
        return format();
    }

    /**
     * Builder for Money instances.
     */
    public static class Builder {
        private BigDecimal amount;
        private Currency currency;

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = BigDecimal.valueOf(amount);
            return this;
        }

        public Builder amount(String amount) {
            this.amount = new BigDecimal(amount);
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder currency(String currencyCode) {
            this.currency = Currency.fromCode(currencyCode);
            return this;
        }

        public Money build() {
            return Money.of(amount, currency);
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