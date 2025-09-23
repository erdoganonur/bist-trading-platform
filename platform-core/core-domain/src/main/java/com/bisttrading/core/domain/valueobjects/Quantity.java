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
 * Quantity value object representing stock quantity with lot size validation.
 * Immutable and provides quantity-related operations.
 */
@Getter
@EqualsAndHashCode
public final class Quantity {

    private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");
    private static final BigDecimal DEFAULT_LOT_SIZE = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_MIN_QUANTITY = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_MAX_QUANTITY = new BigDecimal("1000000");

    private final BigDecimal value;
    private final BigDecimal lotSize;
    private final BigDecimal minQuantity;
    private final BigDecimal maxQuantity;

    private Quantity(BigDecimal value, BigDecimal lotSize, BigDecimal minQuantity, BigDecimal maxQuantity) {
        this.value = Objects.requireNonNull(value, "Miktar değeri boş olamaz");
        this.lotSize = Objects.requireNonNull(lotSize, "Lot boyutu boş olamaz");
        this.minQuantity = Objects.requireNonNull(minQuantity, "Minimum miktar boş olamaz");
        this.maxQuantity = Objects.requireNonNull(maxQuantity, "Maksimum miktar boş olamaz");

        validateQuantity(value);
        validateLotSize(lotSize);
        validateMinMaxQuantity(minQuantity, maxQuantity);
        validateQuantityAgainstLotSize(value, lotSize);
        validateQuantityLimits(value, minQuantity, maxQuantity);
    }

    /**
     * Creates a Quantity instance.
     *
     * @param value       The quantity value
     * @param lotSize     The lot size for trading
     * @param minQuantity The minimum allowed quantity
     * @param maxQuantity The maximum allowed quantity
     * @return Quantity instance
     */
    public static Quantity of(BigDecimal value, BigDecimal lotSize, BigDecimal minQuantity, BigDecimal maxQuantity) {
        return new Quantity(value, lotSize, minQuantity, maxQuantity);
    }

    /**
     * Creates a Quantity instance with default lot size and limits.
     *
     * @param value The quantity value
     * @return Quantity instance
     */
    public static Quantity of(BigDecimal value) {
        return new Quantity(value, DEFAULT_LOT_SIZE, DEFAULT_MIN_QUANTITY, DEFAULT_MAX_QUANTITY);
    }

    /**
     * Creates a Quantity instance from long.
     *
     * @param value       The quantity value
     * @param lotSize     The lot size for trading
     * @param minQuantity The minimum allowed quantity
     * @param maxQuantity The maximum allowed quantity
     * @return Quantity instance
     */
    public static Quantity of(long value, long lotSize, long minQuantity, long maxQuantity) {
        return new Quantity(
            BigDecimal.valueOf(value),
            BigDecimal.valueOf(lotSize),
            BigDecimal.valueOf(minQuantity),
            BigDecimal.valueOf(maxQuantity)
        );
    }

    /**
     * Creates a Quantity instance from long with default parameters.
     *
     * @param value The quantity value
     * @return Quantity instance
     */
    public static Quantity of(long value) {
        return of(BigDecimal.valueOf(value));
    }

    /**
     * Creates a Quantity instance from string.
     *
     * @param value       The quantity value as string
     * @param lotSize     The lot size as string
     * @param minQuantity The minimum quantity as string
     * @param maxQuantity The maximum quantity as string
     * @return Quantity instance
     */
    public static Quantity of(String value, String lotSize, String minQuantity, String maxQuantity) {
        try {
            return new Quantity(
                new BigDecimal(value),
                new BigDecimal(lotSize),
                new BigDecimal(minQuantity),
                new BigDecimal(maxQuantity)
            );
        } catch (NumberFormatException e) {
            throw new ValidationException(ErrorCodes.INVALID_NUMBER_FORMAT,
                "Geçersiz miktar formatı: " + value);
        }
    }

    /**
     * Creates zero quantity.
     *
     * @return Zero Quantity instance
     */
    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO, DEFAULT_LOT_SIZE, BigDecimal.ZERO, DEFAULT_MAX_QUANTITY);
    }

    /**
     * Adds another quantity to this quantity.
     *
     * @param other The quantity to add
     * @return New Quantity instance with sum
     */
    public Quantity add(Quantity other) {
        validateCompatibleQuantity(other);
        BigDecimal newValue = this.value.add(other.value);
        return new Quantity(newValue, this.lotSize, this.minQuantity, this.maxQuantity);
    }

    /**
     * Subtracts another quantity from this quantity.
     *
     * @param other The quantity to subtract
     * @return New Quantity instance with difference
     */
    public Quantity subtract(Quantity other) {
        validateCompatibleQuantity(other);
        BigDecimal newValue = this.value.subtract(other.value);

        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                "İşlem sonucu negatif miktar oluşturamaz");
        }

        return new Quantity(newValue, this.lotSize, this.minQuantity, this.maxQuantity);
    }

    /**
     * Multiplies this quantity by a factor.
     *
     * @param factor The multiplication factor
     * @return New Quantity instance with product
     */
    public Quantity multiply(BigDecimal factor) {
        if (factor == null || factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Çarpan negatif olamaz");
        }

        BigDecimal newValue = this.value.multiply(factor).setScale(0, RoundingMode.DOWN);
        return new Quantity(newValue, this.lotSize, this.minQuantity, this.maxQuantity);
    }

    /**
     * Multiplies this quantity by a factor.
     *
     * @param factor The multiplication factor
     * @return New Quantity instance with product
     */
    public Quantity multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * Divides this quantity by a divisor.
     *
     * @param divisor The division divisor
     * @return New Quantity instance with quotient
     */
    public Quantity divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Bölen pozitif olmalıdır");
        }

        BigDecimal newValue = this.value.divide(divisor, 0, RoundingMode.DOWN);
        return new Quantity(newValue, this.lotSize, this.minQuantity, this.maxQuantity);
    }

    /**
     * Adds lots to this quantity.
     *
     * @param lots Number of lots to add
     * @return New Quantity instance
     */
    public Quantity addLots(int lots) {
        if (lots < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Lot sayısı negatif olamaz");
        }

        BigDecimal increment = lotSize.multiply(BigDecimal.valueOf(lots));
        BigDecimal newValue = value.add(increment);
        return new Quantity(newValue, lotSize, minQuantity, maxQuantity);
    }

    /**
     * Subtracts lots from this quantity.
     *
     * @param lots Number of lots to subtract
     * @return New Quantity instance
     */
    public Quantity subtractLots(int lots) {
        if (lots < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Lot sayısı negatif olamaz");
        }

        BigDecimal decrement = lotSize.multiply(BigDecimal.valueOf(lots));
        BigDecimal newValue = value.subtract(decrement);

        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                "Miktar negatif olamaz");
        }

        return new Quantity(newValue, lotSize, minQuantity, maxQuantity);
    }

    /**
     * Calculates the number of lots this quantity represents.
     *
     * @return Number of lots
     */
    public BigDecimal getLots() {
        return value.divide(lotSize, 0, RoundingMode.DOWN);
    }

    /**
     * Rounds this quantity to the nearest lot size.
     *
     * @return New Quantity instance rounded to lot size
     */
    public Quantity roundToLotSize() {
        BigDecimal lots = value.divide(lotSize, 0, RoundingMode.HALF_UP);
        BigDecimal roundedValue = lots.multiply(lotSize);
        return new Quantity(roundedValue, lotSize, minQuantity, maxQuantity);
    }

    /**
     * Checks if this quantity is zero.
     *
     * @return true if quantity is zero
     */
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this quantity is positive.
     *
     * @return true if quantity is greater than zero
     */
    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this quantity is valid for trading (within min/max limits and lot aligned).
     *
     * @return true if valid for trading
     */
    public boolean isValidForTrading() {
        try {
            validateQuantityLimits(value, minQuantity, maxQuantity);
            validateQuantityAgainstLotSize(value, lotSize);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    /**
     * Checks if this quantity is greater than another quantity.
     *
     * @param other The quantity to compare
     * @return true if this is greater
     */
    public boolean isGreaterThan(Quantity other) {
        return this.value.compareTo(other.value) > 0;
    }

    /**
     * Checks if this quantity is greater than or equal to another quantity.
     *
     * @param other The quantity to compare
     * @return true if this is greater or equal
     */
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) >= 0;
    }

    /**
     * Checks if this quantity is less than another quantity.
     *
     * @param other The quantity to compare
     * @return true if this is less
     */
    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }

    /**
     * Checks if this quantity is less than or equal to another quantity.
     *
     * @param other The quantity to compare
     * @return true if this is less or equal
     */
    public boolean isLessThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) <= 0;
    }

    /**
     * Calculates the percentage of another quantity this represents.
     *
     * @param total The total quantity
     * @return Percentage as BigDecimal
     */
    public BigDecimal percentageOf(Quantity total) {
        if (total.value.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Toplam miktar sıfır olamaz");
        }

        return this.value.divide(total.value, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Formats this quantity for display.
     *
     * @return Formatted string
     */
    public String format() {
        NumberFormat formatter = NumberFormat.getNumberInstance(TURKISH_LOCALE);
        formatter.setMaximumFractionDigits(0);
        formatter.setGroupingUsed(true);
        return formatter.format(value) + " adet";
    }

    /**
     * Formats this quantity with lot information.
     *
     * @return Formatted string with lot info
     */
    public String formatWithLots() {
        BigDecimal lots = getLots();
        if (lots.compareTo(BigDecimal.ZERO) > 0) {
            return format() + " (" + lots + " lot)";
        }
        return format();
    }

    /**
     * Converts this quantity to long value.
     *
     * @return Long value
     */
    public long longValue() {
        return value.longValue();
    }

    /**
     * Converts this quantity to int value.
     *
     * @return Int value
     */
    public int intValue() {
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Miktar integer sınırlarını aşıyor");
        }
        return value.intValue();
    }

    /**
     * Validates quantity value.
     */
    private void validateQuantity(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                "Miktar negatif olamaz");
        }

        if (quantity.scale() > 0) {
            throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                "Miktar tam sayı olmalıdır");
        }
    }

    /**
     * Validates lot size.
     */
    private void validateLotSize(BigDecimal lotSize) {
        if (lotSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Lot boyutu pozitif olmalıdır");
        }

        if (lotSize.scale() > 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Lot boyutu tam sayı olmalıdır");
        }
    }

    /**
     * Validates min/max quantity relationship.
     */
    private void validateMinMaxQuantity(BigDecimal minQuantity, BigDecimal maxQuantity) {
        if (minQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Minimum miktar negatif olamaz");
        }

        if (maxQuantity.compareTo(minQuantity) < 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Maksimum miktar minimum miktardan küçük olamaz");
        }
    }

    /**
     * Validates that quantity is aligned with lot size.
     */
    private void validateQuantityAgainstLotSize(BigDecimal quantity, BigDecimal lotSize) {
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainder = quantity.remainder(lotSize);
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                    "Miktar lot boyutunun katı olmalıdır");
            }
        }
    }

    /**
     * Validates quantity against min/max limits.
     */
    private void validateQuantityLimits(BigDecimal quantity, BigDecimal minQuantity, BigDecimal maxQuantity) {
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            if (quantity.compareTo(minQuantity) < 0) {
                throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                    "Miktar minimum miktardan küçük olamaz: " + minQuantity);
            }

            if (quantity.compareTo(maxQuantity) > 0) {
                throw new ValidationException(ErrorCodes.INVALID_ORDER_QUANTITY,
                    "Miktar maksimum miktarı aşamaz: " + maxQuantity);
            }
        }
    }

    /**
     * Validates that two quantities are compatible for operations.
     */
    private void validateCompatibleQuantity(Quantity other) {
        if (other == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Karşılaştırılan miktar objesi boş olamaz");
        }

        if (!this.lotSize.equals(other.lotSize)) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Farklı lot boyutuna sahip miktarlar işleme alınamaz");
        }
    }

    @Override
    public String toString() {
        return formatWithLots();
    }

    /**
     * Builder for Quantity instances.
     */
    public static class Builder {
        private BigDecimal value;
        private BigDecimal lotSize = DEFAULT_LOT_SIZE;
        private BigDecimal minQuantity = DEFAULT_MIN_QUANTITY;
        private BigDecimal maxQuantity = DEFAULT_MAX_QUANTITY;

        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder value(long value) {
            this.value = BigDecimal.valueOf(value);
            return this;
        }

        public Builder value(String value) {
            this.value = new BigDecimal(value);
            return this;
        }

        public Builder lotSize(BigDecimal lotSize) {
            this.lotSize = lotSize;
            return this;
        }

        public Builder lotSize(long lotSize) {
            this.lotSize = BigDecimal.valueOf(lotSize);
            return this;
        }

        public Builder minQuantity(BigDecimal minQuantity) {
            this.minQuantity = minQuantity;
            return this;
        }

        public Builder minQuantity(long minQuantity) {
            this.minQuantity = BigDecimal.valueOf(minQuantity);
            return this;
        }

        public Builder maxQuantity(BigDecimal maxQuantity) {
            this.maxQuantity = maxQuantity;
            return this;
        }

        public Builder maxQuantity(long maxQuantity) {
            this.maxQuantity = BigDecimal.valueOf(maxQuantity);
            return this;
        }

        public Quantity build() {
            return Quantity.of(value, lotSize, minQuantity, maxQuantity);
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