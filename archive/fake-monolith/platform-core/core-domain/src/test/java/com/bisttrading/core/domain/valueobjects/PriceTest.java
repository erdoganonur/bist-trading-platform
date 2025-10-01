package com.bisttrading.core.domain.valueobjects;

import com.bisttrading.core.common.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Price Value Object Tests")
class PriceTest {

    private static final String VALID_SYMBOL = "GARAN";
    private static final BigDecimal VALID_PRICE = new BigDecimal("10.50");
    private static final BigDecimal VALID_TICK_SIZE = new BigDecimal("0.01");

    @Nested
    @DisplayName("Price Creation")
    class PriceCreation {

        @Test
        @DisplayName("Should create Price with valid parameters")
        void shouldCreatePriceWithValidParameters() {
            // When
            Price price = Price.of(VALID_PRICE, VALID_SYMBOL, VALID_TICK_SIZE);

            // Then
            assertThat(price.getValue()).isEqualTo(VALID_PRICE);
            assertThat(price.getSymbol()).isEqualTo(VALID_SYMBOL);
            assertThat(price.getTickSize()).isEqualTo(VALID_TICK_SIZE);
        }

        @Test
        @DisplayName("Should create Price with default tick size")
        void shouldCreatePriceWithDefaultTickSize() {
            // When
            Price price = Price.of(VALID_PRICE, VALID_SYMBOL);

            // Then
            assertThat(price.getValue()).isEqualTo(VALID_PRICE);
            assertThat(price.getSymbol()).isEqualTo(VALID_SYMBOL);
            assertThat(price.getTickSize()).isEqualTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("Should create Price from double values")
        void shouldCreatePriceFromDoubleValues() {
            // When
            Price price = Price.of(10.50, VALID_SYMBOL, 0.01);

            // Then
            assertThat(price.getValue()).isEqualTo(new BigDecimal("10.5000"));
            assertThat(price.getSymbol()).isEqualTo(VALID_SYMBOL);
        }

        @Test
        @DisplayName("Should create Price from string values")
        void shouldCreatePriceFromStringValues() {
            // When
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");

            // Then
            assertThat(price.getValue()).isEqualTo(new BigDecimal("10.5000"));
            assertThat(price.getSymbol()).isEqualTo(VALID_SYMBOL);
        }

        @Test
        @DisplayName("Should create Price using builder")
        void shouldCreatePriceUsingBuilder() {
            // When
            Price price = Price.builder()
                .value("10.50")
                .symbol(VALID_SYMBOL)
                .tickSize(0.01)
                .build();

            // Then
            assertThat(price.getValue()).isEqualTo(new BigDecimal("10.5000"));
            assertThat(price.getSymbol()).isEqualTo(VALID_SYMBOL);
            assertThat(price.getTickSize()).isEqualTo(new BigDecimal("0.0100"));
        }

        @Test
        @DisplayName("Should convert symbol to uppercase")
        void shouldConvertSymbolToUppercase() {
            // When
            Price price = Price.of(VALID_PRICE, "garan", VALID_TICK_SIZE);

            // Then
            assertThat(price.getSymbol()).isEqualTo("GARAN");
        }

        @Test
        @DisplayName("Should throw exception for null values")
        void shouldThrowExceptionForNullValues() {
            // When & Then
            assertThatThrownBy(() -> Price.of(null, VALID_SYMBOL, VALID_TICK_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Fiyat değeri boş olamaz");

            assertThatThrownBy(() -> Price.of(VALID_PRICE, null, VALID_TICK_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Sembol boş olamaz");

            assertThatThrownBy(() -> Price.of(VALID_PRICE, VALID_SYMBOL, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Tick boyutu boş olamaz");
        }

        @Test
        @DisplayName("Should throw exception for invalid price")
        void shouldThrowExceptionForInvalidPrice() {
            // When & Then
            assertThatThrownBy(() -> Price.of(BigDecimal.ZERO, VALID_SYMBOL, VALID_TICK_SIZE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Fiyat pozitif olmalıdır");

            assertThatThrownBy(() -> Price.of(new BigDecimal("-10"), VALID_SYMBOL, VALID_TICK_SIZE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Fiyat pozitif olmalıdır");
        }

        @Test
        @DisplayName("Should throw exception for invalid symbol")
        void shouldThrowExceptionForInvalidSymbol() {
            // When & Then
            assertThatThrownBy(() -> Price.of(VALID_PRICE, "", VALID_TICK_SIZE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Sembol 1-10 karakter arasında olmalıdır");

            assertThatThrownBy(() -> Price.of(VALID_PRICE, "VERY_LONG_SYMBOL", VALID_TICK_SIZE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Sembol 1-10 karakter arasında olmalıdır");

            assertThatThrownBy(() -> Price.of(VALID_PRICE, "GAR@N", VALID_TICK_SIZE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Sembol sadece büyük harf ve rakam içerebilir");
        }

        @Test
        @DisplayName("Should throw exception for invalid tick size")
        void shouldThrowExceptionForInvalidTickSize() {
            // When & Then
            assertThatThrownBy(() -> Price.of(VALID_PRICE, VALID_SYMBOL, BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tick boyutu pozitif olmalıdır");

            assertThatThrownBy(() -> Price.of(VALID_PRICE, VALID_SYMBOL, new BigDecimal("-0.01")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tick boyutu pozitif olmalıdır");
        }

        @Test
        @DisplayName("Should throw exception for price not aligned with tick size")
        void shouldThrowExceptionForPriceNotAlignedWithTickSize() {
            // Given
            BigDecimal price = new BigDecimal("10.557"); // Not divisible by 0.01
            BigDecimal tickSize = new BigDecimal("0.01");

            // When & Then
            assertThatThrownBy(() -> Price.of(price, VALID_SYMBOL, tickSize))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Fiyat tick boyutunun katı olmalıdır");
        }
    }

    @Nested
    @DisplayName("Price Operations")
    class PriceOperations {

        @Test
        @DisplayName("Should add ticks to price")
        void shouldAddTicksToPrice() {
            // Given
            Price price = Price.of("10.00", VALID_SYMBOL, "0.01");

            // When
            Price result = price.addTicks(5);

            // Then
            assertThat(result.getValue()).isEqualTo(new BigDecimal("10.0500"));
            assertThat(result.getSymbol()).isEqualTo(VALID_SYMBOL);
        }

        @Test
        @DisplayName("Should subtract ticks from price")
        void shouldSubtractTicksFromPrice() {
            // Given
            Price price = Price.of("10.00", VALID_SYMBOL, "0.01");

            // When
            Price result = price.subtractTicks(5);

            // Then
            assertThat(result.getValue()).isEqualTo(new BigDecimal("9.9500"));
            assertThat(result.getSymbol()).isEqualTo(VALID_SYMBOL);
        }

        @Test
        @DisplayName("Should throw exception when subtracting ticks results in zero or negative")
        void shouldThrowExceptionWhenSubtractingTicksResultsInZeroOrNegative() {
            // Given
            Price price = Price.of("0.05", VALID_SYMBOL, "0.01");

            // When & Then
            assertThatThrownBy(() -> price.subtractTicks(10))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Fiyat sıfır veya negatif olamaz");
        }

        @Test
        @DisplayName("Should calculate percentage change from another price")
        void shouldCalculatePercentageChangeFromAnotherPrice() {
            // Given
            Price basePrice = Price.of("10.00", VALID_SYMBOL, "0.01");
            Price currentPrice = Price.of("11.00", VALID_SYMBOL, "0.01");

            // When
            BigDecimal percentageChange = currentPrice.percentageChangeFrom(basePrice);

            // Then
            assertThat(percentageChange).isEqualTo(new BigDecimal("10.0000"));
        }

        @Test
        @DisplayName("Should calculate price difference")
        void shouldCalculatePriceDifference() {
            // Given
            Price price1 = Price.of("11.00", VALID_SYMBOL, "0.01");
            Price price2 = Price.of("10.00", VALID_SYMBOL, "0.01");

            // When
            BigDecimal difference = price1.differenceFrom(price2);

            // Then
            assertThat(difference).isEqualTo(new BigDecimal("1.0000"));
        }

        @Test
        @DisplayName("Should round price to tick size")
        void shouldRoundPriceToTickSize() {
            // Given - create a price that's exactly aligned first
            Price alignedPrice = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When - test the rounding functionality
            Price rounded = alignedPrice.roundToTickSize();

            // Then
            assertThat(rounded.getValue()).isEqualTo(new BigDecimal("10.5000"));
        }

        @Test
        @DisplayName("Should validate price within limits")
        void shouldValidatePriceWithinLimits() {
            // Given
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");
            Price floor = Price.of("10.00", VALID_SYMBOL, "0.01");
            Price ceiling = Price.of("11.00", VALID_SYMBOL, "0.01");

            // When
            boolean withinLimits = price.isWithinLimits(floor, ceiling);

            // Then
            assertThat(withinLimits).isTrue();
        }

        @Test
        @DisplayName("Should validate price within daily limits")
        void shouldValidatePriceWithinDailyLimits() {
            // Given
            Price currentPrice = Price.of("10.50", VALID_SYMBOL, "0.01");
            Price referencePrice = Price.of("10.00", VALID_SYMBOL, "0.01");
            BigDecimal limitPercentage = new BigDecimal("10"); // 10%

            // When
            boolean withinLimits = currentPrice.isWithinDailyLimits(referencePrice, limitPercentage);

            // Then
            assertThat(withinLimits).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for operations with different symbols")
        void shouldThrowExceptionForOperationsWithDifferentSymbols() {
            // Given
            Price priceGaran = Price.of("10.00", "GARAN", "0.01");
            Price priceAkbnk = Price.of("10.00", "AKBNK", "0.01");

            // When & Then
            assertThatThrownBy(() -> priceGaran.percentageChangeFrom(priceAkbnk))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Fiyat sembolleri farklı");
        }
    }

    @Nested
    @DisplayName("Price Comparison")
    class PriceComparison {

        @Test
        @DisplayName("Should correctly compare prices")
        void shouldCorrectlyComparePrices() {
            // Given
            Price price1 = Price.of("10.00", VALID_SYMBOL, "0.01");
            Price price2 = Price.of("11.00", VALID_SYMBOL, "0.01");
            Price price3 = Price.of("10.00", VALID_SYMBOL, "0.01");

            // When & Then
            assertThat(price2.isGreaterThan(price1)).isTrue();
            assertThat(price1.isLessThan(price2)).isTrue();
            assertThat(price1.isEqualTo(price3)).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for null comparison")
        void shouldThrowExceptionForNullComparison() {
            // Given
            Price price = Price.of("10.00", VALID_SYMBOL, "0.01");

            // When & Then
            assertThatThrownBy(() -> price.isGreaterThan(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Karşılaştırılan fiyat objesi boş olamaz");
        }
    }

    @Nested
    @DisplayName("Price Conversion")
    class PriceConversion {

        @Test
        @DisplayName("Should convert to Money with default currency")
        void shouldConvertToMoneyWithDefaultCurrency() {
            // Given
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When
            Money money = price.toMoney();

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("10.50"));
            assertThat(money.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should convert to Money with specified currency")
        void shouldConvertToMoneyWithSpecifiedCurrency() {
            // Given
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When
            Money money = price.toMoney(Currency.USD);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("10.50"));
            assertThat(money.getCurrency()).isEqualTo(Currency.USD);
        }
    }

    @Nested
    @DisplayName("Price Formatting")
    class PriceFormatting {

        @Test
        @DisplayName("Should format price correctly")
        void shouldFormatPriceCorrectly() {
            // Given
            Price price = Price.of("1234.56", VALID_SYMBOL, "0.01");

            // When
            String formatted = price.format();

            // Then
            assertThat(formatted).contains("1.234,56");
            assertThat(formatted).contains("₺");
        }

        @Test
        @DisplayName("Should format price with symbol")
        void shouldFormatPriceWithSymbol() {
            // Given
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When
            String formatted = price.formatWithSymbol();

            // Then
            assertThat(formatted).startsWith(VALID_SYMBOL + ":");
            assertThat(formatted).contains("10");
            assertThat(formatted).contains("50");
        }

        @Test
        @DisplayName("Should have meaningful toString representation")
        void shouldHaveMeaningfulToStringRepresentation() {
            // Given
            Price price = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When
            String toString = price.toString();

            // Then
            assertThat(toString).contains(VALID_SYMBOL);
            assertThat(toString).contains("10");
            assertThat(toString).contains("50");
        }
    }

    @Nested
    @DisplayName("Price Equality and Hash Code")
    class PriceEqualityAndHashCode {

        @Test
        @DisplayName("Should be equal for same values")
        void shouldBeEqualForSameValues() {
            // Given
            Price price1 = Price.of("10.50", VALID_SYMBOL, "0.01");
            Price price2 = Price.of("10.50", VALID_SYMBOL, "0.01");

            // When & Then
            assertThat(price1).isEqualTo(price2);
            assertThat(price1.hashCode()).isEqualTo(price2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal for different values")
        void shouldNotBeEqualForDifferentValues() {
            // Given
            Price price1 = Price.of("10.50", VALID_SYMBOL, "0.01");
            Price price2 = Price.of("10.51", VALID_SYMBOL, "0.01");
            Price price3 = Price.of("10.50", "AKBNK", "0.01");

            // When & Then
            assertThat(price1).isNotEqualTo(price2);
            assertThat(price1).isNotEqualTo(price3);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GARAN", "AKBNK", "THYAO", "ISCTR", "KCHOL"})
    @DisplayName("Should handle various valid BIST symbols")
    void shouldHandleVariousValidBistSymbols(String symbol) {
        // When & Then
        assertThatCode(() -> Price.of("10.50", symbol, "0.01"))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "VERY_LONG_SYMBOL_NAME", "GAR@N"})
    @DisplayName("Should reject invalid symbols")
    void shouldRejectInvalidSymbols(String symbol) {
        // When & Then
        assertThatThrownBy(() -> Price.of("10.50", symbol, "0.01"))
            .isInstanceOf(ValidationException.class);
    }
}