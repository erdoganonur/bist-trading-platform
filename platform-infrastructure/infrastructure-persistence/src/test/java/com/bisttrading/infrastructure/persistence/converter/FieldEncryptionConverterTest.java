package com.bisttrading.infrastructure.persistence.converter;

import com.bisttrading.infrastructure.persistence.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Enhanced unit tests for FieldEncryptionConverter.
 * Tests field-level encryption with Turkish character handling and performance.
 */
@ActiveProfiles("test")
@DisplayName("Field Encryption Converter Tests")
class FieldEncryptionConverterTest {

    private FieldEncryptionConverter converter;
    private String testEncryptionKey;

    @BeforeEach
    void setUp() {
        testEncryptionKey = "dGVzdC1lbmNyeXB0aW9uLWtleS1mb3ItdW5pdC10ZXN0cw==";
        converter = new FieldEncryptionConverter(testEncryptionKey);
    }

    @Test
    @DisplayName("Should encrypt and decrypt string successfully")
    void shouldEncryptAndDecryptStringSuccessfully() {
        // Given
        String plainText = "Sensitive Information";

        // When
        String encrypted = converter.convertToDatabaseColumn(plainText);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(plainText).isNotEmpty();
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt Turkish characters correctly")
    void shouldEncryptTurkishCharactersCorrectly() {
        // Given
        String turkishText = "Türkçe karakter içeren metin: ğĞıİöÖüÜşŞçÇ";

        // When
        String encrypted = converter.convertToDatabaseColumn(turkishText);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(turkishText);
    }

    @ParameterizedTest
    @DisplayName("Should handle various Turkish names")
    @ValueSource(strings = {
        "Çağlar Şıktırıkoğlu",
        "Gülşah Çelik",
        "Ömer Güneş",
        "Şeyma İnanç",
        "İbrahim Ğökçe"
    })
    void shouldHandleVariousTurkishNames(String turkishName) {
        // When
        String encrypted = converter.convertToDatabaseColumn(turkishName);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(turkishName);
    }

    @Test
    @DisplayName("Should encrypt TC Kimlik number securely")
    void shouldEncryptTcKimlikNumberSecurely() {
        // Given
        String tcKimlik = "12345678901";

        // When
        String encrypted = converter.convertToDatabaseColumn(tcKimlik);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(tcKimlik);
        assertThat(decrypted).isEqualTo(tcKimlik);
        assertThat(encrypted.length()).isGreaterThan(tcKimlik.length());
    }

    @Test
    @DisplayName("Should encrypt Turkish phone numbers")
    void shouldEncryptTurkishPhoneNumbers() {
        // Given
        String phoneNumber = "+905551234567";

        // When
        String encrypted = converter.convertToDatabaseColumn(phoneNumber);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(phoneNumber);
        assertThat(decrypted).isEqualTo(phoneNumber);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        // When
        String encrypted = converter.convertToDatabaseColumn(null);
        String decrypted = converter.convertToEntityAttribute(null);

        // Then
        assertThat(encrypted).isNull();
        assertThat(decrypted).isNull();
    }

    @Test
    @DisplayName("Should handle empty input gracefully")
    void shouldHandleEmptyInputGracefully() {
        // When
        String encrypted = converter.convertToDatabaseColumn("");
        String decrypted = converter.convertToEntityAttribute("");

        // Then
        assertThat(encrypted).isEmpty();
        assertThat(decrypted).isEmpty();
    }

    @Test
    @DisplayName("Should produce different encryption for same input")
    void shouldProduceDifferentEncryptionForSameInput() {
        // Given
        String plainText = "Same input text";

        // When
        String encrypted1 = converter.convertToDatabaseColumn(plainText);
        String encrypted2 = converter.convertToDatabaseColumn(plainText);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // Due to random IV
        assertThat(converter.convertToEntityAttribute(encrypted1)).isEqualTo(plainText);
        assertThat(converter.convertToEntityAttribute(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt long text successfully")
    void shouldEncryptLongTextSuccessfully() {
        // Given
        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100);

        // When
        String encrypted = converter.convertToDatabaseColumn(longText);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(longText);
        assertThat(encrypted.length()).isGreaterThan(longText.length());
    }

    @Test
    @DisplayName("Should encrypt special characters correctly")
    void shouldEncryptSpecialCharactersCorrectly() {
        // Given
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // When
        String encrypted = converter.convertToDatabaseColumn(specialChars);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    @DisplayName("Should encrypt numeric strings correctly")
    void shouldEncryptNumericStringsCorrectly() {
        // Given
        String numbers = "1234567890";

        // When
        String encrypted = converter.convertToDatabaseColumn(numbers);
        String decrypted = converter.convertToEntityAttribute(numbers);

        // Then
        assertThat(decrypted).isEqualTo(numbers);
    }

    @Test
    @DisplayName("Should handle whitespace and newlines")
    void shouldHandleWhitespaceAndNewlines() {
        // Given
        String textWithWhitespace = "Line 1\nLine 2\tTabbed\r\nWindows line ending";

        // When
        String encrypted = converter.convertToDatabaseColumn(textWithWhitespace);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(textWithWhitespace);
    }

    @Test
    @DisplayName("Should validate encryption format")
    void shouldValidateEncryptionFormat() {
        // Given
        String plainText = "Test validation";

        // When
        String encrypted = converter.convertToDatabaseColumn(plainText);

        // Then
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$"); // Base64 format
        assertThat(converter.isEncrypted(encrypted)).isTrue();
        assertThat(converter.isEncrypted(plainText)).isFalse();
        assertThat(converter.isEncrypted("")).isFalse();
        assertThat(converter.isEncrypted(null)).isFalse();
    }

    @Test
    @DisplayName("Should handle corrupted encrypted data gracefully")
    void shouldHandleCorruptedEncryptedDataGracefully() {
        // Given
        String plainText = "Test data";
        String encrypted = converter.convertToDatabaseColumn(plainText);
        String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        // When
        String result = converter.convertToEntityAttribute(corrupted);

        // Then
        assertThat(result).isEqualTo(corrupted); // Returns original corrupted data
    }

    @Test
    @DisplayName("Should handle invalid Base64 data gracefully")
    void shouldHandleInvalidBase64DataGracefully() {
        // Given
        String invalidBase64 = "This is not base64 data!@#$%";

        // When
        String result = converter.convertToEntityAttribute(invalidBase64);

        // Then
        assertThat(result).isEqualTo(invalidBase64); // Returns original for invalid base64
    }

    @Test
    @DisplayName("Should encrypt JSON data correctly")
    void shouldEncryptJsonDataCorrectly() {
        // Given
        String jsonData = "{\"name\":\"John Doe\",\"age\":30,\"city\":\"İstanbul\"}";

        // When
        String encrypted = converter.convertToDatabaseColumn(jsonData);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(jsonData);
    }

    @Test
    @DisplayName("Should maintain data integrity for various cases")
    void shouldMaintainDataIntegrityForVariousCases() {
        // Given
        String[] testCases = {
            "Simple text",
            "Türkçe metin",
            "12345678901",
            "+905551234567",
            "user@example.com",
            "Complex data: {\"key\": \"value\", \"number\": 123}",
            "",
            " ",
            "\n\t\r",
            "Very long text that exceeds normal field lengths and contains various characters including unicode: αβγδε"
        };

        // When & Then
        for (String testCase : testCases) {
            String encrypted = converter.convertToDatabaseColumn(testCase);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            assertThat(decrypted)
                .withFailMessage("Data integrity failed for: '%s'", testCase)
                .isEqualTo(testCase);
        }
    }

    @Test
    @DisplayName("Should perform reasonably fast")
    void shouldPerformReasonablyFast() {
        // Given
        String testData = "Performance test data with reasonable length";
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < 1000; i++) {
            String encrypted = converter.convertToDatabaseColumn(testData);
            String decrypted = converter.convertToEntityAttribute(encrypted);
            assertThat(decrypted).isEqualTo(testData);
        }

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(5000); // Should complete in less than 5 seconds
    }

    @Test
    @DisplayName("Should handle concurrent encryption/decryption")
    void shouldHandleConcurrentEncryptionDecryption() throws Exception {
        // Given
        String plainText = "Concurrent encryption test";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                String encrypted = converter.convertToDatabaseColumn(plainText);
                return converter.convertToEntityAttribute(encrypted);
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(5, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<String> future : futures) {
            assertThat(future.get()).isEqualTo(plainText);
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should have secure random IV for each encryption")
    void shouldHaveSecureRandomIvForEachEncryption() {
        // Given
        String plainText = "IV randomness test";

        // When
        String[] encryptions = new String[10];
        for (int i = 0; i < 10; i++) {
            encryptions[i] = converter.convertToDatabaseColumn(plainText);
        }

        // Then
        // All encryptions should be different due to random IV
        for (int i = 0; i < encryptions.length; i++) {
            for (int j = i + 1; j < encryptions.length; j++) {
                assertThat(encryptions[i])
                    .withFailMessage("Encryptions should be different due to random IV")
                    .isNotEqualTo(encryptions[j]);
            }
        }

        // But all should decrypt to same plaintext
        for (String encryption : encryptions) {
            assertThat(converter.convertToEntityAttribute(encryption)).isEqualTo(plainText);
        }
    }

    @Test
    @DisplayName("Should encrypt Turkish addresses correctly")
    void shouldEncryptTurkishAddressesCorrectly() {
        // Given
        String[] turkishAddresses = {
            "Çamlıca Mahallesi, Gülşah Sokağı No:12",
            "Şişli Mahallesi, İnönü Caddesi No:45",
            "Çankaya Mahallesi, Atatürk Bulvarı No:78"
        };

        // When & Then
        for (String address : turkishAddresses) {
            String encrypted = converter.convertToDatabaseColumn(address);
            String decrypted = converter.convertToEntityAttribute(encrypted);
            assertThat(decrypted).isEqualTo(address);
        }
    }

    @Test
    @DisplayName("Should handle edge case inputs")
    void shouldHandleEdgeCaseInputs() {
        // Given
        String[] edgeCases = {
            "a", // Single character
            "ç", // Single Turkish character
            "A".repeat(1000), // Very long string
            "123", // Only numbers
            "   ", // Only spaces
            "\t\n\r", // Only whitespace characters
            "çğıöşüÇĞIİÖŞÜ", // Only Turkish characters
            "🇹🇷", // Emoji
            "αβγδε", // Greek characters
        };

        // When & Then
        for (String edgeCase : edgeCases) {
            String encrypted = converter.convertToDatabaseColumn(edgeCase);
            String decrypted = converter.convertToEntityAttribute(encrypted);
            assertThat(decrypted).isEqualTo(edgeCase);
        }
    }

    @Test
    @DisplayName("Should detect encrypted vs non-encrypted data")
    void shouldDetectEncryptedVsNonEncryptedData() {
        // Given
        String plainText = "This is plain text";
        String encrypted = converter.convertToDatabaseColumn(plainText);

        // When & Then
        assertThat(converter.isEncrypted(plainText)).isFalse();
        assertThat(converter.isEncrypted(encrypted)).isTrue();
        assertThat(converter.isEncrypted("not-base64-data!@#")).isFalse();
        assertThat(converter.isEncrypted("SGVsbG8gV29ybGQ=")).isFalse(); // Valid base64 but not our format
    }

    @Test
    @DisplayName("Should encrypt PII data according to Turkish regulations")
    void shouldEncryptPiiDataAccordingToTurkishRegulations() {
        // Given - Turkish PII data types
        String tcKimlik = "12345678901";
        String phoneNumber = "+905551234567";
        String address = "Çamlıca Mahallesi, Gülşah Sokağı No:12, İstanbul";
        String email = "çağlar@example.com";

        // When
        String encryptedTc = converter.convertToDatabaseColumn(tcKimlik);
        String encryptedPhone = converter.convertToDatabaseColumn(phoneNumber);
        String encryptedAddress = converter.convertToDatabaseColumn(address);
        String encryptedEmail = converter.convertToDatabaseColumn(email);

        // Then
        assertThat(converter.convertToEntityAttribute(encryptedTc)).isEqualTo(tcKimlik);
        assertThat(converter.convertToEntityAttribute(encryptedPhone)).isEqualTo(phoneNumber);
        assertThat(converter.convertToEntityAttribute(encryptedAddress)).isEqualTo(address);
        assertThat(converter.convertToEntityAttribute(encryptedEmail)).isEqualTo(email);

        // Verify all are properly encrypted (not equal to original)
        assertThat(encryptedTc).isNotEqualTo(tcKimlik);
        assertThat(encryptedPhone).isNotEqualTo(phoneNumber);
        assertThat(encryptedAddress).isNotEqualTo(address);
        assertThat(encryptedEmail).isNotEqualTo(email);
    }
}