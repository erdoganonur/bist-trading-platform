package com.bisttrading.broker.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AlgoLabEncryptionUtil için test suite
 * Python karşılığından portlanan encryption metodlarını test eder
 */
@ExtendWith(MockitoExtension.class)
class AlgoLabEncryptionUtilTest {

    private AlgoLabEncryptionUtil encryptionUtil;
    private static final String TEST_SECRET_KEY = "test-secret-key-32-characters-long";

    @BeforeEach
    void setUp() {
        encryptionUtil = new AlgoLabEncryptionUtil(TEST_SECRET_KEY);
    }

    @Test
    void encrypt_Success() {
        // Arrange
        String plaintext = "test-password";

        // Act
        String encrypted = encryptionUtil.encrypt(plaintext, TEST_SECRET_KEY);

        // Assert
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintext);

        // Base64 encoded olduğunu kontrol et
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    void encrypt_EmptyPlaintext() {
        // Act
        String encrypted = encryptionUtil.encrypt("", TEST_SECRET_KEY);

        // Assert
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isEmpty();
    }

    @Test
    void encrypt_NullPlaintext() {
        // Act & Assert
        assertThatThrownBy(() -> encryptionUtil.encrypt(null, TEST_SECRET_KEY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plaintext null olamaz");
    }

    @Test
    void encrypt_NullSecretKey() {
        // Act & Assert
        assertThatThrownBy(() -> encryptionUtil.encrypt("test", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Secret key null olamaz");
    }

    @Test
    void encrypt_InvalidSecretKeyLength() {
        // Arrange
        String shortKey = "short";

        // Act & Assert
        assertThatThrownBy(() -> encryptionUtil.encrypt("test", shortKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Secret key en az 16 karakter olmalı");
    }

    @Test
    void encrypt_ConsistentResults() {
        // Arrange
        String plaintext = "consistent-test";

        // Act
        String encrypted1 = encryptionUtil.encrypt(plaintext, TEST_SECRET_KEY);
        String encrypted2 = encryptionUtil.encrypt(plaintext, TEST_SECRET_KEY);

        // Assert - Aynı input için aynı output üretmeli (IV sıfır olduğu için)
        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    @Test
    void makeChecker_Success() {
        // Arrange
        String apiKey = "test-api-key";
        String baseUrl = "https://api.test.com";
        String endpoint = "/api/test";
        String payload = "{\"test\": \"data\"}";

        // Act
        String checker = encryptionUtil.makeChecker(apiKey, baseUrl, endpoint, payload);

        // Assert
        assertThat(checker).isNotNull();
        assertThat(checker).isNotEmpty();

        // Checker consistent olmalı
        String checker2 = encryptionUtil.makeChecker(apiKey, baseUrl, endpoint, payload);
        assertThat(checker).isEqualTo(checker2);
    }

    @Test
    void makeChecker_DifferentInputs_DifferentOutputs() {
        // Arrange
        String baseInput = "test-api-key";
        String baseUrl = "https://api.test.com";
        String endpoint = "/api/test";
        String payload = "{\"test\": \"data\"}";

        // Act
        String checker1 = encryptionUtil.makeChecker(baseInput, baseUrl, endpoint, payload);
        String checker2 = encryptionUtil.makeChecker("different-key", baseUrl, endpoint, payload);
        String checker3 = encryptionUtil.makeChecker(baseInput, baseUrl, "/api/different", payload);

        // Assert
        assertThat(checker1).isNotEqualTo(checker2);
        assertThat(checker1).isNotEqualTo(checker3);
        assertThat(checker2).isNotEqualTo(checker3);
    }

    @Test
    void makeChecker_NullInputs() {
        // Act & Assert
        assertThatThrownBy(() ->
            encryptionUtil.makeChecker(null, "url", "endpoint", "payload"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            encryptionUtil.makeChecker("key", null, "endpoint", "payload"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            encryptionUtil.makeChecker("key", "url", null, "payload"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            encryptionUtil.makeChecker("key", "url", "endpoint", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void makeChecker_EmptyStrings() {
        // Arrange
        String apiKey = "";
        String baseUrl = "";
        String endpoint = "";
        String payload = "";

        // Act
        String checker = encryptionUtil.makeChecker(apiKey, baseUrl, endpoint, payload);

        // Assert - Empty stringlerle bile checker üretmeli
        assertThat(checker).isNotNull();
        assertThat(checker).isNotEmpty();
    }

    @Test
    void encrypt_SpecialCharacters() {
        // Arrange
        String plaintextWithSpecialChars = "şğüıöçĞÜİÖÇ@#$%^&*()";

        // Act
        String encrypted = encryptionUtil.encrypt(plaintextWithSpecialChars, TEST_SECRET_KEY);

        // Assert
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintextWithSpecialChars);
    }

    @Test
    void encrypt_LongText() {
        // Arrange
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a long text for testing purposes. ");
        }

        // Act
        String encrypted = encryptionUtil.encrypt(longText.toString(), TEST_SECRET_KEY);

        // Assert
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted.length()).isGreaterThan(100);
    }

    @Test
    void makeChecker_JsonPayload() {
        // Arrange
        String complexJson = "{"
            + "\"symbol\": \"THYAO\","
            + "\"direction\": \"BUY\","
            + "\"price\": \"15.50\","
            + "\"lot\": \"100\","
            + "\"nested\": {"
                + "\"key\": \"value\","
                + "\"array\": [1, 2, 3]"
            + "}"
            + "}";

        // Act
        String checker = encryptionUtil.makeChecker(
            "test-api-key",
            "https://api.test.com",
            "/api/SendOrder",
            complexJson
        );

        // Assert
        assertThat(checker).isNotNull();
        assertThat(checker).isNotEmpty();

        // Same input should produce same checker
        String checker2 = encryptionUtil.makeChecker(
            "test-api-key",
            "https://api.test.com",
            "/api/SendOrder",
            complexJson
        );
        assertThat(checker).isEqualTo(checker2);
    }
}