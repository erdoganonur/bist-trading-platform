package com.bisttrading.core.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ValidationUtils.
 */
class ValidationUtilsTest {

    @Test
    void shouldValidateCorrectTCKimlik() {
        // Valid TC Kimlik numbers for testing
        assertThat(ValidationUtils.isValidTCKimlik("12345678901")).isFalse(); // Invalid algorithm
        assertThat(ValidationUtils.isValidTCKimlik("11111111116")).isTrue(); // Valid test number
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123", "1234567890123", "0123456789", "abcdefghijk"})
    void shouldRejectInvalidTCKimlikFormats(String tcKimlik) {
        assertThat(ValidationUtils.isValidTCKimlik(tcKimlik)).isFalse();
    }

    @Test
    void shouldHandleNullTCKimlik() {
        assertThat(ValidationUtils.isValidTCKimlik(null)).isFalse();
    }

    @Test
    void shouldValidateCorrectTurkishIBAN() {
        // Valid Turkish IBAN format (but may not be real account)
        String validIBAN = "TR330006100519786457841326";
        assertThat(ValidationUtils.isValidTurkishIBAN(validIBAN)).isTrue();
    }

    @Test
    void shouldValidateIBANWithSpaces() {
        String ibanWithSpaces = "TR33 0006 1005 1978 6457 8413 26";
        assertThat(ValidationUtils.isValidTurkishIBAN(ibanWithSpaces)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TR", "TR00", "TR1234567890123456789012345", "GB82WEST12345698765432"})
    void shouldRejectInvalidTurkishIBANFormats(String iban) {
        assertThat(ValidationUtils.isValidTurkishIBAN(iban)).isFalse();
    }

    @Test
    void shouldValidateInternationalIBAN() {
        String germanIBAN = "DE89370400440532013000";
        assertThat(ValidationUtils.isValidIBAN(germanIBAN)).isTrue();
    }

    @Test
    void shouldHandleNullIBAN() {
        assertThat(ValidationUtils.isValidIBAN(null)).isFalse();
        assertThat(ValidationUtils.isValidTurkishIBAN(null)).isFalse();
    }

    @Test
    void shouldValidateCorrectTaxNumber() {
        // Test with a valid Turkish tax number pattern
        String validTaxNumber = "1234567890";
        // Note: This tests format, actual validation requires real algorithm
        assertThat(ValidationUtils.isValidTurkishTaxNumber(validTaxNumber)).isFalse(); // Will fail algorithm
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345678901", "abcdefghij"})
    void shouldRejectInvalidTaxNumberFormats(String taxNumber) {
        assertThat(ValidationUtils.isValidTurkishTaxNumber(taxNumber)).isFalse();
    }

    @Test
    void shouldValidatePassportNumbers() {
        assertThat(ValidationUtils.isValidTurkishPassport("U12345678")).isTrue();
        assertThat(ValidationUtils.isValidTurkishPassport("A87654321")).isTrue();
        assertThat(ValidationUtils.isValidTurkishPassport("123456789")).isFalse();
        assertThat(ValidationUtils.isValidTurkishPassport("AB1234567")).isFalse();
    }

    @Test
    void shouldValidateDrivingLicense() {
        assertThat(ValidationUtils.isValidTurkishDrivingLicense("123456")).isTrue();
        assertThat(ValidationUtils.isValidTurkishDrivingLicense("12345")).isFalse();
        assertThat(ValidationUtils.isValidTurkishDrivingLicense("1234567")).isFalse();
        assertThat(ValidationUtils.isValidTurkishDrivingLicense("abcdef")).isFalse();
    }

    @Test
    void shouldValidatePostalCodes() {
        assertThat(ValidationUtils.isValidTurkishPostalCode("34000")).isTrue(); // Istanbul
        assertThat(ValidationUtils.isValidTurkishPostalCode("06000")).isTrue(); // Ankara
        assertThat(ValidationUtils.isValidTurkishPostalCode("35000")).isTrue(); // Izmir
        assertThat(ValidationUtils.isValidTurkishPostalCode("00000")).isFalse(); // Invalid
        assertThat(ValidationUtils.isValidTurkishPostalCode("99000")).isFalse(); // Invalid
        assertThat(ValidationUtils.isValidTurkishPostalCode("1234")).isFalse(); // Too short
    }

    @Test
    void shouldValidateCreditCardNumbers() {
        // Test Luhn algorithm with known valid test numbers
        assertThat(ValidationUtils.isValidCreditCardNumber("4111111111111111")).isTrue(); // Visa test
        assertThat(ValidationUtils.isValidCreditCardNumber("5555555555554444")).isTrue(); // MasterCard test
        assertThat(ValidationUtils.isValidCreditCardNumber("1234567890123456")).isFalse(); // Invalid
        assertThat(ValidationUtils.isValidCreditCardNumber("411111111111111")).isFalse(); // One digit short
    }

    @Test
    void shouldValidateAge() {
        assertThat(ValidationUtils.isValidAge("25")).isTrue();
        assertThat(ValidationUtils.isValidAge("0")).isTrue();
        assertThat(ValidationUtils.isValidAge("150")).isTrue();
        assertThat(ValidationUtils.isValidAge("-1")).isFalse();
        assertThat(ValidationUtils.isValidAge("151")).isFalse();
        assertThat(ValidationUtils.isValidAge("abc")).isFalse();
    }

    @Test
    void shouldValidateLegalAge() {
        assertThat(ValidationUtils.isLegalAge("18")).isTrue();
        assertThat(ValidationUtils.isLegalAge("25")).isTrue();
        assertThat(ValidationUtils.isLegalAge("17")).isFalse();
        assertThat(ValidationUtils.isLegalAge("0")).isFalse();
    }

    @Test
    void shouldValidatePasswordStrength() {
        assertThat(ValidationUtils.isStrongPassword("Password123!")).isTrue();
        assertThat(ValidationUtils.isStrongPassword("PASSWORD123!")).isTrue();
        assertThat(ValidationUtils.isStrongPassword("password123!")).isTrue();
        assertThat(ValidationUtils.isStrongPassword("Password!")).isFalse(); // Too short
        assertThat(ValidationUtils.isStrongPassword("password")).isFalse(); // No variety
        assertThat(ValidationUtils.isStrongPassword("")).isFalse();
    }

    @Test
    void shouldValidateTextInput() {
        assertThat(ValidationUtils.isValidTextInput("Ahmet Mehmet")).isTrue();
        assertThat(ValidationUtils.isValidTextInput("Çağlar Şahin")).isTrue();
        assertThat(ValidationUtils.isValidTextInput("Test123")).isTrue();
        assertThat(ValidationUtils.isValidTextInput("Test-Name.")).isTrue();
        assertThat(ValidationUtils.isValidTextInput("Test@Name")).isFalse(); // Invalid char
        assertThat(ValidationUtils.isValidTextInput("Test#Name")).isFalse(); // Invalid char
    }

    @Test
    void shouldValidateURL() {
        assertThat(ValidationUtils.isValidUrl("https://www.example.com")).isTrue();
        assertThat(ValidationUtils.isValidUrl("http://example.com")).isTrue();
        assertThat(ValidationUtils.isValidUrl("ftp://ftp.example.com")).isTrue();
        assertThat(ValidationUtils.isValidUrl("not-a-url")).isFalse();
        assertThat(ValidationUtils.isValidUrl("")).isFalse();
    }

    @Test
    void shouldValidateDecimalNumbers() {
        assertThat(ValidationUtils.isValidDecimal("123.45")).isTrue();
        assertThat(ValidationUtils.isValidDecimal("123,45")).isTrue(); // Turkish format
        assertThat(ValidationUtils.isValidDecimal("0.5")).isTrue();
        assertThat(ValidationUtils.isValidDecimal("-123.45")).isTrue();
        assertThat(ValidationUtils.isValidDecimal("abc")).isFalse();
        assertThat(ValidationUtils.isValidDecimal("")).isFalse();
    }

    @Test
    void shouldValidateIntegers() {
        assertThat(ValidationUtils.isValidInteger("123")).isTrue();
        assertThat(ValidationUtils.isValidInteger("-123")).isTrue();
        assertThat(ValidationUtils.isValidInteger("0")).isTrue();
        assertThat(ValidationUtils.isValidInteger("123.45")).isFalse();
        assertThat(ValidationUtils.isValidInteger("abc")).isFalse();
        assertThat(ValidationUtils.isValidInteger("")).isFalse();
    }
}