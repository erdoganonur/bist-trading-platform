package com.bisttrading.infrastructure.persistence.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter for field-level encryption using AES-256-GCM.
 * Automatically encrypts/decrypts sensitive PII data fields.
 */
@Slf4j
@Component
@Converter
public class FieldEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public FieldEncryptionConverter(@Value("${bist.security.encryption.key:}") String encryptionKey) {
        this.secureRandom = new SecureRandom();

        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.warn("Veritabanı şifreleme anahtarı tanımlanmamış, yeni anahtar oluşturuluyor");
            this.secretKey = generateKey();
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                log.info("Veritabanı şifreleme anahtarı yüklendi");
            } catch (Exception e) {
                log.error("Şifreleme anahtarı yüklenemedi, yeni anahtar oluşturuluyor: {}", e.getMessage());
                this.secretKey = generateKey();
            }
        }
    }

    /**
     * Converts the entity attribute value to database column representation.
     * Encrypts the plain text value.
     *
     * @param attribute Entity attribute value (plain text)
     * @return Database column value (encrypted)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            return encrypt(attribute);
        } catch (Exception e) {
            log.error("Veritabanına kayıt sırasında şifreleme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Field encryption failed", e);
        }
    }

    /**
     * Converts the database column value to entity attribute representation.
     * Decrypts the encrypted value.
     *
     * @param dbData Database column value (encrypted)
     * @return Entity attribute value (plain text)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            return decrypt(dbData);
        } catch (Exception e) {
            log.error("Veritabanından okuma sırasında şifre çözme hatası: {}", e.getMessage(), e);
            // Return the original value if decryption fails (for data migration scenarios)
            return dbData;
        }
    }

    /**
     * Encrypts a plain text string using AES-256-GCM.
     *
     * @param plainText Text to encrypt
     * @return Base64 encoded encrypted data (IV + encrypted data + tag)
     */
    private String encrypt(String plainText) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        // Encrypt the data
        byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedData = cipher.doFinal(plainTextBytes);

        // Combine IV + encrypted data for storage
        byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

        // Return Base64 encoded result
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    /**
     * Decrypts an encrypted string using AES-256-GCM.
     *
     * @param encryptedText Base64 encoded encrypted data
     * @return Decrypted plain text
     */
    private String decrypt(String encryptedText) throws Exception {
        // Decode from Base64
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

        if (encryptedWithIv.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data format");
        }

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // Decrypt the data
        byte[] decryptedBytes = cipher.doFinal(encryptedData);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Generates a new AES-256 encryption key.
     *
     * @return Generated secret key
     */
    private static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Checks if a string appears to be encrypted.
     *
     * @param data String to check
     * @return true if data appears to be encrypted
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.length() < 20) {
            return false;
        }

        try {
            // Try to decode as Base64
            byte[] decoded = Base64.getDecoder().decode(data);
            // Check if it has minimum length for encrypted data
            return decoded.length >= GCM_IV_LENGTH + GCM_TAG_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }
}