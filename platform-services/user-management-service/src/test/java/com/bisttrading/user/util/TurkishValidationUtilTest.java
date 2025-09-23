package com.bisttrading.user.util;

import com.bisttrading.user.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for TurkishValidationUtil.
 * Tests Turkish-specific validation including TC Kimlik, phone numbers, IBAN, etc.
 */
@DisplayName("Turkish Validation Util Tests")
class TurkishValidationUtilTest {

    private TurkishValidationUtil validationUtil;

    @BeforeEach
    void setUp() {
        validationUtil = new TurkishValidationUtil();
    }

    @Test
    @DisplayName("Should validate correct TC Kimlik numbers")
    void shouldValidateCorrectTcKimlik() {
        // Given - Valid TC Kimlik numbers (using test data)
        String[] validTcKimliks = TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS;

        // When & Then
        for (String tcKimlik : validTcKimliks) {
            assertThat(validationUtil.isValidTcKimlik(tcKimlik))
                .withFailMessage("TC Kimlik should be valid: %s", tcKimlik)
                .isTrue();
        }
    }

    @Test
    void shouldRejectInvalidTcKimlik() {
        // Null and empty
        assertThat(validationUtil.isValidTcKimlik(null)).isFalse();
        assertThat(validationUtil.isValidTcKimlik("")).isFalse();
        assertThat(validationUtil.isValidTcKimlik("   ")).isFalse();

        // Wrong length
        assertThat(validationUtil.isValidTcKimlik("123456789")).isFalse();
        assertThat(validationUtil.isValidTcKimlik("123456789012")).isFalse();

        // Starts with 0
        assertThat(validationUtil.isValidTcKimlik("01234567890")).isFalse();

        // Contains non-digits
        assertThat(validationUtil.isValidTcKimlik("1234567890a")).isFalse();
        assertThat(validationUtil.isValidTcKimlik("12345-67890")).isFalse();

        // Invalid checksum
        assertThat(validationUtil.isValidTcKimlik("12345678900")).isFalse();
        assertThat(validationUtil.isValidTcKimlik("11111111111")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+905551234567",
        "05551234567",
        "5551234567",
        "+90 555 123 45 67",
        "0555 123 45 67",
        "555-123-45-67"
    })
    void shouldValidateCorrectTurkishPhoneNumbers(String phoneNumber) {
        assertThat(validationUtil.isValidTurkishPhoneNumber(phoneNumber)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "123456789",
        "+1234567890",
        "+905441234567", // 544 is not a valid mobile prefix
        "05341234567",   // 534 is not mobile
        "+905551234",    // Too short
        "+9055512345678", // Too long
        "abc5551234567",  // Contains letters
        "+90-555-123-45-67-89" // Too long
    })
    void shouldRejectInvalidTurkishPhoneNumbers(String phoneNumber) {
        assertThat(validationUtil.isValidTurkishPhoneNumber(phoneNumber)).isFalse();
    }

    @Test
    void shouldNormalizePhoneNumbers() {
        assertThat(validationUtil.normalizePhoneNumber("+905551234567")).isEqualTo("+905551234567");
        assertThat(validationUtil.normalizePhoneNumber("905551234567")).isEqualTo("+905551234567");
        assertThat(validationUtil.normalizePhoneNumber("05551234567")).isEqualTo("+905551234567");
        assertThat(validationUtil.normalizePhoneNumber("5551234567")).isEqualTo("+905551234567");
        assertThat(validationUtil.normalizePhoneNumber("0555 123 45 67")).isEqualTo("+905551234567");
        assertThat(validationUtil.normalizePhoneNumber("555-123-45-67")).isEqualTo("+905551234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test@example.com",
        "user.name@domain.co.uk",
        "firstname+lastname@example.com",
        "email@123.123.123.123", // IP address
        "1234567890@example.com",
        "email@example-one.com",
        "_______@example.com",
        "email@example.name"
    })
    void shouldValidateCorrectEmailAddresses(String email) {
        assertThat(validationUtil.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "plainaddress",
        "@missingusername.com",
        "username@.com",
        "username@com",
        "username..double.dot@example.com",
        "username@-example.com",
        "username@example-.com"
    })
    void shouldRejectInvalidEmailAddresses(String email) {
        assertThat(validationUtil.isValidEmail(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "StrongPass123",
        "MyPassword1",
        "SecureP@ss1",
        "TürkçeŞifre123", // Turkish characters
        "UPPERCASE123abc",
        "lowercase123ABC"
    })
    void shouldValidateStrongPasswords(String password) {
        assertThat(validationUtil.isValidPassword(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "weak",
        "12345678", // Only digits
        "password", // Only lowercase
        "PASSWORD", // Only uppercase
        "Password", // Missing digit
        "password123", // Missing uppercase
        "PASSWORD123", // Missing lowercase
        "Pass1" // Too short
    })
    void shouldRejectWeakPasswords(String password) {
        assertThat(validationUtil.isValidPassword(password)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "01000",
        "34000",
        "06000",
        "99999"
    })
    void shouldValidateCorrectPostalCodes(String postalCode) {
        assertThat(validationUtil.isValidTurkishPostalCode(postalCode)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "123",
        "123456",
        "abcde",
        "1234a"
    })
    void shouldRejectInvalidPostalCodes(String postalCode) {
        assertThat(validationUtil.isValidTurkishPostalCode(postalCode)).isFalse();
    }

    @Test
    void shouldValidateCorrectTurkishIban() {
        // Valid Turkish IBAN (using real format but test numbers)
        String validIban = "TR330006100519786457841326";
        assertThat(validationUtil.isValidTurkishIban(validIban)).isTrue();

        // Should also work with spaces and dashes
        assertThat(validationUtil.isValidTurkishIban("TR33 0006 1005 1978 6457 8413 26")).isTrue();
        assertThat(validationUtil.isValidTurkishIban("TR33-0006-1005-1978-6457-8413-26")).isTrue();
    }

    @Test
    void shouldRejectInvalidTurkishIban() {
        assertThat(validationUtil.isValidTurkishIban(null)).isFalse();
        assertThat(validationUtil.isValidTurkishIban("")).isFalse();
        assertThat(validationUtil.isValidTurkishIban("TR")).isFalse();
        assertThat(validationUtil.isValidTurkishIban("TR33000610051978645784132")).isFalse(); // Too short
        assertThat(validationUtil.isValidTurkishIban("US330006100519786457841326")).isFalse(); // Wrong country
        assertThat(validationUtil.isValidTurkishIban("TR330006100519786457841325")).isFalse(); // Wrong checksum
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Ahmet",
        "Mehmet Ali",
        "Ayşe Gül",
        "Ömer-Faruk",
        "Ali'nin",
        "Çağlar Şıktırık"
    })
    void shouldValidateCorrectTurkishNames(String name) {
        assertThat(validationUtil.isValidTurkishName(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "A", // Too short
        "123Ahmet", // Contains numbers
        "@Ahmet", // Contains special chars
        "A".repeat(51) // Too long
    })
    void shouldRejectInvalidTurkishNames(String name) {
        assertThat(validationUtil.isValidTurkishName(name)).isFalse();
    }

    @Test
    void shouldFormatTcKimlik() {
        String tcKimlik = "12345678901";
        String formatted = validationUtil.formatTcKimlik(tcKimlik);
        assertThat(formatted).isEqualTo("123 45 678 90 1");

        // Should return original if invalid
        String invalid = "123";
        assertThat(validationUtil.formatTcKimlik(invalid)).isEqualTo(invalid);
    }

    @Test
    void shouldFormatPhoneNumber() {
        String phoneNumber = "+905551234567";
        String formatted = validationUtil.formatPhoneNumber(phoneNumber);
        assertThat(formatted).isEqualTo("+90 555 123 45 67");

        // Should work with different input formats
        assertThat(validationUtil.formatPhoneNumber("05551234567")).isEqualTo("+90 555 123 45 67");
        assertThat(validationUtil.formatPhoneNumber("5551234567")).isEqualTo("+90 555 123 45 67");

        // Should return original if invalid
        String invalid = "123";
        assertThat(validationUtil.formatPhoneNumber(invalid)).isEqualTo(invalid);
    }

    @Test
    @DisplayName("Should handle concurrent validation operations")
    void shouldHandleConcurrentValidationOperations() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                String tcKimlik = TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS[index % TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS.length];
                String phone = "+90555123456" + (index % 10);
                String email = "user" + index + "@example.com";
                String password = "Password" + index;

                return validationUtil.isValidTcKimlik(tcKimlik) &&
                       validationUtil.isValidTurkishPhoneNumber(phone) &&
                       validationUtil.isValidEmail(email) &&
                       validationUtil.isValidPassword(password);
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(5, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should validate Turkish characters in names and emails")
    void shouldValidateTurkishCharactersInNamesAndEmails() {
        // Given
        String[] turkishNames = TestDataBuilder.TurkishCharacters.TURKISH_NAMES;
        String[] turkishEmails = TestDataBuilder.TurkishCharacters.TURKISH_EMAILS;

        // When & Then
        for (String name : turkishNames) {
            assertThat(validationUtil.isValidTurkishName(name))
                .withFailMessage("Turkish name should be valid: %s", name)
                .isTrue();
        }

        for (String email : turkishEmails) {
            assertThat(validationUtil.isValidEmail(email))
                .withFailMessage("Turkish email should be valid: %s", email)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should perform validation operations efficiently")
    void shouldPerformValidationOperationsEfficiently() {
        // Given
        String tcKimlik = TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS[0];
        String phone = "+905551234567";
        String email = "test@example.com";
        String password = "Password123";
        String iban = TestDataBuilder.FinancialData.VALID_TURKISH_IBANS[0];

        long startTime = System.currentTimeMillis();

        // When - Perform 1000 validation operations
        for (int i = 0; i < 1000; i++) {
            validationUtil.isValidTcKimlik(tcKimlik);
            validationUtil.isValidTurkishPhoneNumber(phone);
            validationUtil.isValidEmail(email);
            validationUtil.isValidPassword(password);
            validationUtil.isValidTurkishIban(iban);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then - Should complete within reasonable time
        assertThat(duration).isLessThan(2000); // Less than 2 seconds
    }

    @Test
    @DisplayName("Should handle edge cases in validation methods")
    void shouldHandleEdgeCasesInValidationMethods() {
        // When & Then - Test null inputs
        assertThat(validationUtil.isValidTcKimlik(null)).isFalse();
        assertThat(validationUtil.isValidTurkishPhoneNumber(null)).isFalse();
        assertThat(validationUtil.isValidEmail(null)).isFalse();
        assertThat(validationUtil.isValidPassword(null)).isFalse();
        assertThat(validationUtil.isValidTurkishPostalCode(null)).isFalse();
        assertThat(validationUtil.isValidTurkishIban(null)).isFalse();
        assertThat(validationUtil.isValidTurkishTaxNumber(null)).isFalse();
        assertThat(validationUtil.isValidTurkishName(null)).isFalse();

        // Test empty strings
        assertThat(validationUtil.isValidTcKimlik("")).isFalse();
        assertThat(validationUtil.isValidTurkishPhoneNumber("")).isFalse();
        assertThat(validationUtil.isValidEmail("")).isFalse();
        assertThat(validationUtil.isValidPassword("")).isFalse();
        assertThat(validationUtil.isValidTurkishPostalCode("")).isFalse();
        assertThat(validationUtil.isValidTurkishIban("")).isFalse();
        assertThat(validationUtil.isValidTurkishTaxNumber("")).isFalse();
        assertThat(validationUtil.isValidTurkishName("")).isFalse();

        // Test whitespace-only strings
        assertThat(validationUtil.isValidTcKimlik("   ")).isFalse();
        assertThat(validationUtil.isValidTurkishPhoneNumber("   ")).isFalse();
        assertThat(validationUtil.isValidEmail("   ")).isFalse();
        assertThat(validationUtil.isValidPassword("   ")).isFalse();
        assertThat(validationUtil.isValidTurkishPostalCode("   ")).isFalse();
        assertThat(validationUtil.isValidTurkishIban("   ")).isFalse();
        assertThat(validationUtil.isValidTurkishTaxNumber("   ")).isFalse();
        assertThat(validationUtil.isValidTurkishName("   ")).isFalse();
    }

    @Test
    @DisplayName("Should validate IBAN with various Turkish banks")
    void shouldValidateIbanWithVariousTurkishBanks() {
        // Given - Valid Turkish IBANs for different banks from test data
        String[] validIbans = TestDataBuilder.FinancialData.VALID_TURKISH_IBANS;

        // When & Then
        for (String iban : validIbans) {
            assertThat(validationUtil.isValidTurkishIban(iban))
                .withFailMessage("IBAN should be valid: %s", iban)
                .isTrue();
        }
    }

    @ParameterizedTest
    @DisplayName("Should validate various Turkish phone operator prefixes")
    @ValueSource(strings = {
        "+905321234567", // Turkcell
        "+905331234567", // Turkcell
        "+905341234567", // Turkcell
        "+905351234567", // Turkcell
        "+905361234567", // Turkcell
        "+905371234567", // Turkcell
        "+905381234567", // Turkcell
        "+905391234567", // Turkcell
        "+905501234567", // Vodafone
        "+905511234567", // Vodafone
        "+905521234567", // Vodafone
        "+905531234567", // Vodafone
        "+905541234567", // Vodafone
        "+905551234567", // Vodafone
        "+905561234567", // Türk Telekom
        "+905571234567"  // Türk Telekom
    })
    void shouldValidateVariousTurkishPhoneOperatorPrefixes(String phoneNumber) {
        // When & Then
        assertThat(validationUtil.isValidTurkishPhoneNumber(phoneNumber)).isTrue();
    }

    @Test
    @DisplayName("Should validate TC Kimlik with proper algorithm")
    void shouldValidateTcKimlikWithProperAlgorithm() {
        // Given - Test specific TC Kimlik that follows the algorithm
        String validTcKimlik = TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS[0];

        // When
        boolean isValid = validationUtil.isValidTcKimlik(validTcKimlik);

        // Then
        assertThat(isValid).isTrue();

        // Verify formatting also works
        String formatted = validationUtil.formatTcKimlik(validTcKimlik);
        assertThat(formatted).contains(" ");
        assertThat(formatted.replace(" ", "")).isEqualTo(validTcKimlik);
    }
}