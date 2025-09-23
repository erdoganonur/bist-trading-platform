package com.bisttrading.core.security.util;

import com.bisttrading.core.security.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EncryptionUtil.
 * Tests AES-256-GCM encryption/decryption with Turkish character handling.
 */
@DisplayName("Encryption Util Tests")
class EncryptionUtilTest {

    private String testEncryptionKey;

    @BeforeEach
    void setUp() {
        testEncryptionKey = "dGVzdC1lbmNyeXB0aW9uLWtleS1mb3ItdW5pdC10ZXN0cw=="; // Base64 encoded
    }

    @Test
    @DisplayName("Should encrypt and decrypt regular text successfully")
    void shouldEncryptAndDecryptRegularTextSuccessfully() {
        // Given
        String plainText = "Sensitive Information";

        // When
        String encrypted = EncryptionUtil.encrypt(plainText, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(encrypted).isNotNull().isNotEmpty().isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt and decrypt Turkish characters successfully")
    void shouldEncryptAndDecryptTurkishCharactersSuccessfully() {
        // Given
        String turkishText = "Türkçe karakter içeren metin: ğĞıİöÖüÜşŞçÇ";

        // When
        String encrypted = EncryptionUtil.encrypt(turkishText, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(encrypted).isNotNull().isNotEmpty().isNotEqualTo(turkishText);
        assertThat(decrypted).isEqualTo(turkishText);
    }

    @ParameterizedTest
    @DisplayName("Should handle various Turkish names correctly")
    @ValueSource(strings = {
        "Çağlar",
        "Gülşah",
        "Ömer",
        "Şeyma",
        "İbrahim",
        "Ğülizar"
    })
    void shouldHandleVariousTurkishNamesCorrectly(String turkishName) {
        // When
        String encrypted = EncryptionUtil.encrypt(turkishName, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(decrypted).isEqualTo(turkishName);
    }

    @Test
    @DisplayName("Should encrypt TC Kimlik numbers securely")
    void shouldEncryptTcKimlikNumbersSecurely() {
        // Given
        String tcKimlik = "12345678901";

        // When
        String encrypted = EncryptionUtil.encrypt(tcKimlik, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(encrypted).isNotEqualTo(tcKimlik);
        assertThat(decrypted).isEqualTo(tcKimlik);
        assertThat(encrypted.length()).isGreaterThan(tcKimlik.length());
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$"); // Base64 format
    }

    @Test
    @DisplayName("Should encrypt Turkish phone numbers securely")
    void shouldEncryptTurkishPhoneNumbersSecurely() {
        // Given
        String phoneNumber = "+905551234567";

        // When
        String encrypted = EncryptionUtil.encrypt(phoneNumber, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(encrypted).isNotEqualTo(phoneNumber);
        assertThat(decrypted).isEqualTo(phoneNumber);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        // When & Then
        assertThat(EncryptionUtil.encrypt(null, testEncryptionKey)).isNull();
        assertThat(EncryptionUtil.decrypt(null, testEncryptionKey)).isNull();
    }

    @Test
    @DisplayName("Should handle empty input gracefully")
    void shouldHandleEmptyInputGracefully() {
        // When
        String encrypted = EncryptionUtil.encrypt("", testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt("", testEncryptionKey);

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
        String encrypted1 = EncryptionUtil.encrypt(plainText, testEncryptionKey);
        String encrypted2 = EncryptionUtil.encrypt(plainText, testEncryptionKey);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // Due to random IV
        assertThat(EncryptionUtil.decrypt(encrypted1, testEncryptionKey)).isEqualTo(plainText);
        assertThat(EncryptionUtil.decrypt(encrypted2, testEncryptionKey)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt long text successfully")
    void shouldEncryptLongTextSuccessfully() {
        // Given
        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100);

        // When
        String encrypted = EncryptionUtil.encrypt(longText, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

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
        String encrypted = EncryptionUtil.encrypt(specialChars, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    @DisplayName("Should encrypt numeric strings correctly")
    void shouldEncryptNumericStringsCorrectly() {
        // Given
        String numbers = "1234567890";

        // When
        String encrypted = EncryptionUtil.encrypt(numbers, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(numbers, testEncryptionKey);

        // Then
        assertThat(decrypted).isEqualTo(numbers);
    }

    @Test
    @DisplayName("Should handle whitespace and newlines correctly")
    void shouldHandleWhitespaceAndNewlinesCorrectly() {
        // Given
        String textWithWhitespace = "Line 1\nLine 2\tTabbed\r\nWindows line ending";

        // When
        String encrypted = EncryptionUtil.encrypt(textWithWhitespace, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(decrypted).isEqualTo(textWithWhitespace);
    }

    @Test
    @DisplayName("Should validate encryption format")
    void shouldValidateEncryptionFormat() {
        // Given
        String plainText = "Test validation";

        // When
        String encrypted = EncryptionUtil.encrypt(plainText, testEncryptionKey);

        // Then
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$"); // Base64 format
    }

    @Test
    @DisplayName("Should handle corrupted encrypted data gracefully")
    void shouldHandleCorruptedEncryptedDataGracefully() {
        // Given
        String plainText = "Test data";
        String encrypted = EncryptionUtil.encrypt(plainText, testEncryptionKey);
        String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        // When & Then
        assertThatThrownBy(() -> EncryptionUtil.decrypt(corrupted, testEncryptionKey))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle invalid Base64 data gracefully")
    void shouldHandleInvalidBase64DataGracefully() {
        // Given
        String invalidBase64 = "This is not base64 data!@#$%";

        // When & Then
        assertThatThrownBy(() -> EncryptionUtil.decrypt(invalidBase64, testEncryptionKey))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should encrypt JSON data correctly")
    void shouldEncryptJsonDataCorrectly() {
        // Given
        String jsonData = "{\"name\":\"John Doe\",\"age\":30,\"city\":\"İstanbul\"}";

        // When
        String encrypted = EncryptionUtil.encrypt(jsonData, testEncryptionKey);
        String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

        // Then
        assertThat(decrypted).isEqualTo(jsonData);
    }

    @Test
    @DisplayName("Should maintain data integrity for various test cases")
    void shouldMaintainDataIntegrityForVariousTestCases() {
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
            String encrypted = EncryptionUtil.encrypt(testCase, testEncryptionKey);
            String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);

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
            String encrypted = EncryptionUtil.encrypt(testData, testEncryptionKey);
            String decrypted = EncryptionUtil.decrypt(encrypted, testEncryptionKey);
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
        String plainText = "Concurrent test data";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                String encrypted = EncryptionUtil.encrypt(plainText, testEncryptionKey);
                return EncryptionUtil.decrypt(encrypted, testEncryptionKey);
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
    @DisplayName("Should have secure random IV")
    void shouldHaveSecureRandomIv() {
        // Given
        String plainText = "IV randomness test";

        // When
        String[] encryptions = new String[10];
        for (int i = 0; i < 10; i++) {
            encryptions[i] = EncryptionUtil.encrypt(plainText, testEncryptionKey);
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
            assertThat(EncryptionUtil.decrypt(encryption, testEncryptionKey)).isEqualTo(plainText);
        }
    }

    @Test
    @DisplayName("Should reject invalid encryption key")
    void shouldRejectInvalidEncryptionKey() {
        // Given
        String plainText = "Test data";
        String invalidKey = "invalid-key";

        // When & Then
        assertThatThrownBy(() -> EncryptionUtil.encrypt(plainText, invalidKey))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should reject null encryption key")
    void shouldRejectNullEncryptionKey() {
        // Given
        String plainText = "Test data";

        // When & Then
        assertThatThrownBy(() -> EncryptionUtil.encrypt(plainText, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}