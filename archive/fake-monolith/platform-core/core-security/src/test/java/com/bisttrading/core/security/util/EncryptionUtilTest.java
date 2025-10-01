package com.bisttrading.core.security.util;

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
    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        testEncryptionKey = "dGVzdC1lbmNyeXB0aW9uLWtleS1mb3ItdW5pdC10ZXN0cw=="; // Base64 encoded
        encryptionUtil = new EncryptionUtil(testEncryptionKey);
    }

    @Test
    @DisplayName("Should encrypt and decrypt regular text successfully")
    void shouldEncryptAndDecryptRegularTextSuccessfully() {
        // Given
        String plainText = "Sensitive Information";

        // When
        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

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
        String encrypted = encryptionUtil.encrypt(turkishText);
        String decrypted = encryptionUtil.decrypt(encrypted);

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
        String encrypted = encryptionUtil.encrypt(turkishName);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(turkishName);
    }

    @Test
    @DisplayName("Should encrypt TC Kimlik numbers securely")
    void shouldEncryptTcKimlikNumbersSecurely() {
        // Given
        String tcKimlik = "12345678901";

        // When
        String encrypted = encryptionUtil.encrypt(tcKimlik);
        String decrypted = encryptionUtil.decrypt(encrypted);

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
        String encrypted = encryptionUtil.encrypt(phoneNumber);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(phoneNumber);
        assertThat(decrypted).isEqualTo(phoneNumber);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        // When & Then
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("Should handle empty input gracefully")
    void shouldHandleEmptyInputGracefully() {
        // When
        String encrypted = encryptionUtil.encrypt("");
        String decrypted = encryptionUtil.decrypt("");

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
        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // Due to random IV
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should encrypt long text successfully")
    void shouldEncryptLongTextSuccessfully() {
        // Given
        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100);

        // When
        String encrypted = encryptionUtil.encrypt(longText);
        String decrypted = encryptionUtil.decrypt(encrypted);

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
        String encrypted = encryptionUtil.encrypt(specialChars);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    @DisplayName("Should encrypt numeric strings correctly")
    void shouldEncryptNumericStringsCorrectly() {
        // Given
        String numbers = "1234567890";

        // When
        String encrypted = encryptionUtil.encrypt(numbers);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(numbers);
    }

    @Test
    @DisplayName("Should handle whitespace and newlines correctly")
    void shouldHandleWhitespaceAndNewlinesCorrectly() {
        // Given
        String textWithWhitespace = "Line 1\nLine 2\tTabbed\r\nWindows line ending";

        // When
        String encrypted = encryptionUtil.encrypt(textWithWhitespace);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(textWithWhitespace);
    }

    @Test
    @DisplayName("Should validate encryption format")
    void shouldValidateEncryptionFormat() {
        // Given
        String plainText = "Test validation";

        // When
        String encrypted = encryptionUtil.encrypt(plainText);

        // Then
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$"); // Base64 format
    }

    @Test
    @DisplayName("Should handle corrupted encrypted data gracefully")
    void shouldHandleCorruptedEncryptedDataGracefully() {
        // Given
        String plainText = "Test data";
        String encrypted = encryptionUtil.encrypt(plainText);
        String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        // When & Then
        assertThatThrownBy(() -> encryptionUtil.decrypt(corrupted))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle invalid Base64 data gracefully")
    void shouldHandleInvalidBase64DataGracefully() {
        // Given
        String invalidBase64 = "This is not base64 data!@#$%";

        // When & Then
        assertThatThrownBy(() -> encryptionUtil.decrypt(invalidBase64))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should encrypt JSON data correctly")
    void shouldEncryptJsonDataCorrectly() {
        // Given
        String jsonData = "{\"name\":\"John Doe\",\"age\":30,\"city\":\"İstanbul\"}";

        // When
        String encrypted = encryptionUtil.encrypt(jsonData);
        String decrypted = encryptionUtil.decrypt(encrypted);

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
            String encrypted = encryptionUtil.encrypt(testCase);
            String decrypted = encryptionUtil.decrypt(encrypted);

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
            String encrypted = encryptionUtil.encrypt(testData);
            String decrypted = encryptionUtil.decrypt(encrypted);
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
                String encrypted = encryptionUtil.encrypt(plainText);
                return encryptionUtil.decrypt(encrypted);
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
            encryptions[i] = encryptionUtil.encrypt(plainText);
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
            assertThat(encryptionUtil.decrypt(encryption)).isEqualTo(plainText);
        }
    }
}