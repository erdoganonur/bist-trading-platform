package com.bisttrading.core.common.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorCodes enum.
 */
class ErrorCodesTest {

    @Test
    void shouldHaveValidCodeAndMessage() {
        ErrorCodes errorCode = ErrorCodes.INTERNAL_SERVER_ERROR;

        assertThat(errorCode.getCode()).isEqualTo("BIST_1000");
        assertThat(errorCode.getMessage()).isEqualTo("Sistem hatası oluştu. Lütfen daha sonra tekrar deneyiniz.");
    }

    @Test
    void shouldFormatMessageWithParameters() {
        ErrorCodes errorCode = ErrorCodes.INSUFFICIENT_BALANCE;
        String formattedMessage = errorCode.getFormattedMessage("1000 TL", "1500 TL");

        assertThat(formattedMessage).contains("1000 TL").contains("1500 TL");
    }

    @Test
    void shouldFindErrorCodeByCode() {
        ErrorCodes found = ErrorCodes.findByCode("BIST_1000");

        assertThat(found).isEqualTo(ErrorCodes.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnNullForInvalidCode() {
        ErrorCodes found = ErrorCodes.findByCode("INVALID_CODE");

        assertThat(found).isNull();
    }

    @ParameterizedTest
    @EnumSource(ErrorCodes.class)
    void shouldHaveValidCodeFormat(ErrorCodes errorCode) {
        assertThat(errorCode.getCode())
                .isNotBlank()
                .startsWith("BIST_")
                .matches("BIST_\\d{4}");
    }

    @ParameterizedTest
    @EnumSource(ErrorCodes.class)
    void shouldHaveNonEmptyMessage(ErrorCodes errorCode) {
        assertThat(errorCode.getMessage())
                .isNotBlank()
                .hasSizeGreaterThan(5);
    }

    @Test
    void shouldHaveUniqueErrorCodes() {
        ErrorCodes[] values = ErrorCodes.values();
        long uniqueCodesCount = java.util.Arrays.stream(values)
                .map(ErrorCodes::getCode)
                .distinct()
                .count();

        assertThat(uniqueCodesCount).isEqualTo(values.length);
    }

    @Test
    void shouldCategorizeErrorCodesByRange() {
        // System errors (1000-1999)
        assertThat(ErrorCodes.INTERNAL_SERVER_ERROR.getCode()).startsWith("BIST_1");
        assertThat(ErrorCodes.VALIDATION_ERROR.getCode()).startsWith("BIST_1");

        // Auth errors (2000-2999)
        assertThat(ErrorCodes.INVALID_CREDENTIALS.getCode()).startsWith("BIST_2");
        assertThat(ErrorCodes.TOKEN_EXPIRED.getCode()).startsWith("BIST_2");

        // User errors (3000-3999)
        assertThat(ErrorCodes.USER_NOT_FOUND.getCode()).startsWith("BIST_3");
        assertThat(ErrorCodes.USER_ALREADY_EXISTS.getCode()).startsWith("BIST_3");

        // Trading errors (4000-4999)
        assertThat(ErrorCodes.INSUFFICIENT_BALANCE.getCode()).startsWith("BIST_4");
        assertThat(ErrorCodes.ORDER_NOT_FOUND.getCode()).startsWith("BIST_4");
    }
}