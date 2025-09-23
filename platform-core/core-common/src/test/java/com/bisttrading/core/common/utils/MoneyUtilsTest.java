package com.bisttrading.core.common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MoneyUtils.
 */
class MoneyUtilsTest {

    @Test
    void shouldCreateMoneyValues() {
        BigDecimal value1 = MoneyUtils.of("123.45");
        BigDecimal value2 = MoneyUtils.of(123.45);
        BigDecimal value3 = MoneyUtils.of(123L);

        assertThat(value1).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(value2).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(value3).isEqualByComparingTo(new BigDecimal("123.00"));

        assertThat(value1.scale()).isEqualTo(MoneyUtils.DEFAULT_SCALE);
        assertThat(value2.scale()).isEqualTo(MoneyUtils.DEFAULT_SCALE);
        assertThat(value3.scale()).isEqualTo(MoneyUtils.DEFAULT_SCALE);
    }

    @Test
    void shouldPerformBasicArithmetic() {
        BigDecimal a = MoneyUtils.of("100.50");
        BigDecimal b = MoneyUtils.of("50.25");

        BigDecimal sum = MoneyUtils.add(a, b);
        BigDecimal difference = MoneyUtils.subtract(a, b);
        BigDecimal product = MoneyUtils.multiply(a, b);
        BigDecimal quotient = MoneyUtils.divide(a, b);

        assertThat(sum).isEqualByComparingTo(MoneyUtils.of("150.75"));
        assertThat(difference).isEqualByComparingTo(MoneyUtils.of("50.25"));
        assertThat(product).isEqualByComparingTo(MoneyUtils.of("5050.13"));
        assertThat(quotient).isEqualByComparingTo(MoneyUtils.of("2.00"));
    }

    @Test
    void shouldThrowExceptionWhenDividingByZero() {
        BigDecimal a = MoneyUtils.of("100.00");
        BigDecimal zero = MoneyUtils.ZERO;

        assertThatThrownBy(() -> MoneyUtils.divide(a, zero))
                .isInstanceOf(ArithmeticException.class)
                .hasMessage("Sıfıra bölme hatası");
    }

    @Test
    void shouldCalculatePercentages() {
        BigDecimal value = MoneyUtils.of("1000.00");
        BigDecimal percentage = MoneyUtils.of("10.00");

        BigDecimal result = MoneyUtils.percentage(value, percentage);

        assertThat(result).isEqualByComparingTo(MoneyUtils.of("100.00"));
    }

    @Test
    void shouldCalculatePercentageChange() {
        BigDecimal oldValue = MoneyUtils.of("100.00");
        BigDecimal newValue = MoneyUtils.of("110.00");

        BigDecimal change = MoneyUtils.percentageChange(oldValue, newValue);

        assertThat(change).isEqualByComparingTo(MoneyUtils.of("10.0000"));
    }

    @Test
    void shouldThrowExceptionWhenCalculatingPercentageChangeWithZeroBase() {
        BigDecimal zero = MoneyUtils.ZERO;
        BigDecimal newValue = MoneyUtils.of("100.00");

        assertThatThrownBy(() -> MoneyUtils.percentageChange(zero, newValue))
                .isInstanceOf(ArithmeticException.class)
                .hasMessage("Eski değer sıfır olamaz");
    }

    @Test
    void shouldPerformComparisons() {
        BigDecimal small = MoneyUtils.of("50.00");
        BigDecimal large = MoneyUtils.of("100.00");
        BigDecimal zero = MoneyUtils.ZERO;

        assertThat(MoneyUtils.isZero(zero)).isTrue();
        assertThat(MoneyUtils.isPositive(large)).isTrue();
        assertThat(MoneyUtils.isNegative(small.negate())).isTrue();
        assertThat(MoneyUtils.isGreaterThan(large, small)).isTrue();
        assertThat(MoneyUtils.isLessThan(small, large)).isTrue();
        assertThat(MoneyUtils.isEqual(small, small)).isTrue();
    }

    @Test
    void shouldFormatTurkishCurrency() {
        BigDecimal value = MoneyUtils.of("1234.56");
        String formatted = MoneyUtils.formatTRY(value);

        // Turkish locale formatting
        assertThat(formatted).contains("1.234,56").contains("₺");
    }

    @Test
    void shouldFormatNumbers() {
        BigDecimal value = MoneyUtils.of("1234.56");
        String formatted = MoneyUtils.formatNumber(value);

        assertThat(formatted).isEqualTo("1.234,56");
    }

    @Test
    void shouldFormatPercentages() {
        BigDecimal value = MoneyUtils.of("12.34");
        String formatted = MoneyUtils.formatPercentage(value);

        assertThat(formatted).isEqualTo("%12,34");
    }

    @Test
    void shouldParseNumbers() throws Exception {
        BigDecimal parsed = MoneyUtils.parseNumber("1.234,56");

        assertThat(parsed).isEqualByComparingTo(MoneyUtils.of("1234.56"));
    }

    @Test
    void shouldParseSafely() {
        BigDecimal parsed = MoneyUtils.parseNumberSafely("invalid", MoneyUtils.of("100.00"));

        assertThat(parsed).isEqualByComparingTo(MoneyUtils.of("100.00"));
    }

    @Test
    void shouldRoundToTickSize() {
        BigDecimal value = MoneyUtils.of("123.456");
        BigDecimal tickSize = MoneyUtils.of("0.05");

        BigDecimal rounded = MoneyUtils.roundToTickSize(value, tickSize);

        assertThat(rounded).isEqualByComparingTo(MoneyUtils.of("123.45"));
    }

    @Test
    void shouldCalculateCompoundInterest() {
        BigDecimal principal = MoneyUtils.of("1000.00");
        BigDecimal rate = MoneyUtils.of("10.00"); // 10% annual
        int periods = 12; // 12 months
        BigDecimal yearsPerPeriod = MoneyUtils.of("0.0833"); // 1/12

        BigDecimal result = MoneyUtils.compoundInterest(principal, rate, periods, yearsPerPeriod);

        assertThat(result).isGreaterThan(principal);
    }

    @Test
    void shouldValidateRange() {
        BigDecimal value = MoneyUtils.of("50.00");
        BigDecimal min = MoneyUtils.of("0.00");
        BigDecimal max = MoneyUtils.of("100.00");

        assertThat(MoneyUtils.isWithinRange(value, min, max)).isTrue();
        assertThat(MoneyUtils.isWithinRange(MoneyUtils.of("150.00"), min, max)).isFalse();
    }

    @Test
    void shouldHandleNullValues() {
        assertThat(MoneyUtils.nullToZero(null)).isEqualByComparingTo(MoneyUtils.ZERO);
        assertThat(MoneyUtils.nullToZero(MoneyUtils.of("100.00"))).isEqualByComparingTo(MoneyUtils.of("100.00"));

        assertThat(MoneyUtils.scaleOrZero(null)).isEqualByComparingTo(MoneyUtils.ZERO);
        assertThat(MoneyUtils.scaleOrZero(new BigDecimal("100.123456")))
                .isEqualByComparingTo(MoneyUtils.of("100.12"));
    }

    @Test
    void shouldFindMinAndMax() {
        BigDecimal a = MoneyUtils.of("50.00");
        BigDecimal b = MoneyUtils.of("100.00");

        assertThat(MoneyUtils.max(a, b)).isEqualByComparingTo(b);
        assertThat(MoneyUtils.min(a, b)).isEqualByComparingTo(a);
    }

    @Test
    void shouldCalculateAbsoluteValue() {
        BigDecimal negative = MoneyUtils.of("-50.00");
        BigDecimal positive = MoneyUtils.of("50.00");

        assertThat(MoneyUtils.abs(negative)).isEqualByComparingTo(positive);
        assertThat(MoneyUtils.abs(positive)).isEqualByComparingTo(positive);
    }
}