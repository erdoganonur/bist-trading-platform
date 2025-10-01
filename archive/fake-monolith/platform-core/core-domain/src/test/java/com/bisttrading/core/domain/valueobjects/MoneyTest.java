package com.bisttrading.core.domain.valueobjects;

import com.bisttrading.core.common.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Nested
    @DisplayName("Money Creation")
    class MoneyCreation {

        @Test
        @DisplayName("Should create Money with valid BigDecimal amount")
        void shouldCreateMoneyWithValidBigDecimalAmount() {
            // Given
            BigDecimal amount = new BigDecimal("100.50");
            Currency currency = Currency.TRY;

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.50"));
            assertThat(money.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should create Money with valid double amount")
        void shouldCreateMoneyWithValidDoubleAmount() {
            // Given
            double amount = 100.50;
            Currency currency = Currency.USD;

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.50"));
            assertThat(money.getCurrency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("Should create Money with valid string amount")
        void shouldCreateMoneyWithValidStringAmount() {
            // Given
            String amount = "100.50";
            Currency currency = Currency.EUR;

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.50"));
            assertThat(money.getCurrency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("Should create zero Money")
        void shouldCreateZeroMoney() {
            // When
            Money money = Money.zero(Currency.TRY);

            // Then
            assertThat(money.getAmount()).isEqualTo(BigDecimal.ZERO.setScale(2));
            assertThat(money.getCurrency()).isEqualTo(Currency.TRY);
            assertThat(money.isZero()).isTrue();
        }

        @Test
        @DisplayName("Should create Money using builder")
        void shouldCreateMoneyUsingBuilder() {
            // When
            Money money = Money.builder()
                .amount("150.75")
                .currency(Currency.TRY)
                .build();

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("150.75"));
            assertThat(money.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            // When & Then
            assertThatThrownBy(() -> Money.of((BigDecimal) null, Currency.TRY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Miktar boş olamaz");
        }

        @Test
        @DisplayName("Should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            // When & Then
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Para birimi boş olamaz");
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            // When & Then
            assertThatThrownBy(() -> Money.of(new BigDecimal("-10"), Currency.TRY))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Miktar negatif olamaz");
        }

        @Test
        @DisplayName("Should throw exception for invalid string amount")
        void shouldThrowExceptionForInvalidStringAmount() {
            // When & Then
            assertThatThrownBy(() -> Money.of("invalid", Currency.TRY))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Geçersiz miktar formatı");
        }

        @Test
        @DisplayName("Should round amount to currency decimal places")
        void shouldRoundAmountToCurrencyDecimalPlaces() {
            // Given
            BigDecimal amount = new BigDecimal("100.567");

            // When
            Money money = Money.of(amount, Currency.TRY);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.57"));
        }
    }

    @Nested
    @DisplayName("Money Arithmetic Operations")
    class MoneyArithmeticOperations {

        @Test
        @DisplayName("Should add two Money amounts with same currency")
        void shouldAddTwoMoneyAmountsWithSameCurrency() {
            // Given
            Money money1 = Money.of("100.50", Currency.TRY);
            Money money2 = Money.of("50.25", Currency.TRY);

            // When
            Money result = money1.add(money2);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.75"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should subtract two Money amounts with same currency")
        void shouldSubtractTwoMoneyAmountsWithSameCurrency() {
            // Given
            Money money1 = Money.of("100.50", Currency.TRY);
            Money money2 = Money.of("50.25", Currency.TRY);

            // When
            Money result = money1.subtract(money2);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.25"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should multiply Money by BigDecimal factor")
        void shouldMultiplyMoneyByBigDecimalFactor() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);
            BigDecimal factor = new BigDecimal("1.5");

            // When
            Money result = money.multiply(factor);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should multiply Money by double factor")
        void shouldMultiplyMoneyByDoubleFactor() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);
            double factor = 2.5;

            // When
            Money result = money.multiply(factor);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("250.00"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should divide Money by BigDecimal divisor")
        void shouldDivideMoneyByBigDecimalDivisor() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);
            BigDecimal divisor = new BigDecimal("4");

            // When
            Money result = money.divide(divisor);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("25.00"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should calculate percentage of Money")
        void shouldCalculatePercentageOfMoney() {
            // Given
            Money money = Money.of("1000.00", Currency.TRY);
            BigDecimal percentage = new BigDecimal("15");

            // When
            Money result = money.percentage(percentage);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should calculate percentage with double")
        void shouldCalculatePercentageWithDouble() {
            // Given
            Money money = Money.of("1000.00", Currency.TRY);
            double percentage = 10.0;

            // When
            Money result = money.percentage(percentage);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
            assertThat(result.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            // Given
            Money moneyTry = Money.of("100.00", Currency.TRY);
            Money moneyUsd = Money.of("100.00", Currency.USD);

            // When & Then
            assertThatThrownBy(() -> moneyTry.add(moneyUsd))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Para birimleri farklı");
        }

        @Test
        @DisplayName("Should throw exception when subtracting would result in negative")
        void shouldThrowExceptionWhenSubtractingWouldResultInNegative() {
            // Given
            Money money1 = Money.of("50.00", Currency.TRY);
            Money money2 = Money.of("100.00", Currency.TRY);

            // When & Then
            assertThatThrownBy(() -> money1.subtract(money2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("İşlem sonucu negatif miktar oluşturamaz");
        }

        @Test
        @DisplayName("Should throw exception for negative multiplication factor")
        void shouldThrowExceptionForNegativeMultiplicationFactor() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);
            BigDecimal factor = new BigDecimal("-1");

            // When & Then
            assertThatThrownBy(() -> money.multiply(factor))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Çarpan negatif olamaz");
        }

        @Test
        @DisplayName("Should throw exception for zero or negative divisor")
        void shouldThrowExceptionForZeroOrNegativeDivisor() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);
            BigDecimal divisor = BigDecimal.ZERO;

            // When & Then
            assertThatThrownBy(() -> money.divide(divisor))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Bölen pozitif olmalıdır");
        }
    }

    @Nested
    @DisplayName("Money Comparison Operations")
    class MoneyComparisonOperations {

        @Test
        @DisplayName("Should correctly identify greater than")
        void shouldCorrectlyIdentifyGreaterThan() {
            // Given
            Money money1 = Money.of("100.00", Currency.TRY);
            Money money2 = Money.of("50.00", Currency.TRY);

            // When & Then
            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isGreaterThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify greater than or equal")
        void shouldCorrectlyIdentifyGreaterThanOrEqual() {
            // Given
            Money money1 = Money.of("100.00", Currency.TRY);
            Money money2 = Money.of("100.00", Currency.TRY);
            Money money3 = Money.of("50.00", Currency.TRY);

            // When & Then
            assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
            assertThat(money1.isGreaterThanOrEqual(money3)).isTrue();
            assertThat(money3.isGreaterThanOrEqual(money1)).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify less than")
        void shouldCorrectlyIdentifyLessThan() {
            // Given
            Money money1 = Money.of("50.00", Currency.TRY);
            Money money2 = Money.of("100.00", Currency.TRY);

            // When & Then
            assertThat(money1.isLessThan(money2)).isTrue();
            assertThat(money2.isLessThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify zero amount")
        void shouldCorrectlyIdentifyZeroAmount() {
            // Given
            Money zeroMoney = Money.zero(Currency.TRY);
            Money nonZeroMoney = Money.of("100.00", Currency.TRY);

            // When & Then
            assertThat(zeroMoney.isZero()).isTrue();
            assertThat(nonZeroMoney.isZero()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify positive amount")
        void shouldCorrectlyIdentifyPositiveAmount() {
            // Given
            Money zeroMoney = Money.zero(Currency.TRY);
            Money positiveMoney = Money.of("100.00", Currency.TRY);

            // When & Then
            assertThat(positiveMoney.isPositive()).isTrue();
            assertThat(zeroMoney.isPositive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Money Formatting")
    class MoneyFormatting {

        @Test
        @DisplayName("Should format TRY currency with Turkish locale")
        void shouldFormatTryCurrencyWithTurkishLocale() {
            // Given
            Money money = Money.of("1234.56", Currency.TRY);

            // When
            String formatted = money.format();

            // Then
            assertThat(formatted).contains("1.234,56");
            assertThat(formatted).contains("₺");
        }

        @Test
        @DisplayName("Should format foreign currency with symbol")
        void shouldFormatForeignCurrencyWithSymbol() {
            // Given
            Money money = Money.of("1234.56", Currency.USD);

            // When
            String formatted = money.format();

            // Then
            assertThat(formatted).contains("1.234,56");
            assertThat(formatted).contains("$");
        }

        @Test
        @DisplayName("Should format with custom symbol option")
        void shouldFormatWithCustomSymbolOption() {
            // Given
            Money money = Money.of("1234.56", Currency.USD);

            // When
            String withSymbol = money.format(true);
            String withoutSymbol = money.format(false);

            // Then
            assertThat(withSymbol).contains("$");
            assertThat(withoutSymbol).doesNotContain("$");
        }

        @Test
        @DisplayName("Should have meaningful toString representation")
        void shouldHaveMeaningfulToStringRepresentation() {
            // Given
            Money money = Money.of("100.50", Currency.TRY);

            // When
            String toString = money.toString();

            // Then
            assertThat(toString).contains("100");
            assertThat(toString).contains("50");
        }
    }

    @Nested
    @DisplayName("Money Utility Operations")
    class MoneyUtilityOperations {

        @Test
        @DisplayName("Should return absolute value")
        void shouldReturnAbsoluteValue() {
            // Given - create a Money with zero amount first, then subtract to get negative
            Money zeroMoney = Money.zero(Currency.TRY);
            Money positiveMoney = Money.of("100.00", Currency.TRY);

            // This creates a scenario where we test abs() conceptually
            // Since Money doesn't allow negative creation, we test with positive value
            Money money = Money.of("100.50", Currency.TRY);

            // When
            Money abs = money.abs();

            // Then
            assertThat(abs.getAmount()).isEqualTo(new BigDecimal("100.50"));
            assertThat(abs.getCurrency()).isEqualTo(Currency.TRY);
        }

        @Test
        @DisplayName("Should validate currency compatibility")
        void shouldValidateCurrencyCompatibility() {
            // Given
            Money moneyTry = Money.of("100.00", Currency.TRY);
            Money moneyUsd = Money.of("100.00", Currency.USD);

            // When & Then
            assertThatThrownBy(() -> moneyTry.add(moneyUsd))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Para birimleri farklı");
        }

        @Test
        @DisplayName("Should handle null comparison gracefully")
        void shouldHandleNullComparisonGracefully() {
            // Given
            Money money = Money.of("100.00", Currency.TRY);

            // When & Then
            assertThatThrownBy(() -> money.isGreaterThan(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Karşılaştırılan para objesi boş olamaz");
        }
    }

    @Nested
    @DisplayName("Money Equality and Hash Code")
    class MoneyEqualityAndHashCode {

        @Test
        @DisplayName("Should be equal for same amount and currency")
        void shouldBeEqualForSameAmountAndCurrency() {
            // Given
            Money money1 = Money.of("100.50", Currency.TRY);
            Money money2 = Money.of("100.50", Currency.TRY);

            // When & Then
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal for different amounts")
        void shouldNotBeEqualForDifferentAmounts() {
            // Given
            Money money1 = Money.of("100.50", Currency.TRY);
            Money money2 = Money.of("100.51", Currency.TRY);

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should not be equal for different currencies")
        void shouldNotBeEqualForDifferentCurrencies() {
            // Given
            Money money1 = Money.of("100.50", Currency.TRY);
            Money money2 = Money.of("100.50", Currency.USD);

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            Money money = Money.of("100.50", Currency.TRY);

            // When & Then
            assertThat(money).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            Money money = Money.of("100.50", Currency.TRY);
            String notMoney = "100.50 TRY";

            // When & Then
            assertThat(money).isNotEqualTo(notMoney);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "100", "100.50", "1000000.99"})
    @DisplayName("Should handle various valid amount formats")
    void shouldHandleVariousValidAmountFormats(String amount) {
        // When & Then
        assertThatCode(() -> Money.of(amount, Currency.TRY))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "100,50", "100.50.50"})
    @DisplayName("Should reject invalid amount formats")
    void shouldRejectInvalidAmountFormats(String amount) {
        // When & Then
        assertThatThrownBy(() -> Money.of(amount, Currency.TRY))
            .isInstanceOf(ValidationException.class);
    }
}