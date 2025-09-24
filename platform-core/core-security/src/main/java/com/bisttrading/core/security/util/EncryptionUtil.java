package com.bisttrading.core.security.util;

import com.bisttrading.core.common.exceptions.TechnicalException;
import com.bisttrading.core.common.constants.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for PII data protection.
 * Provides field-level encryption for sensitive user information.
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Constructor that initializes the encryption key.
     *
     * @param encryptionKey Base64 encoded encryption key
     */
    public EncryptionUtil(@Value("${bist.security.encryption.key:}") String encryptionKey) {
        this.secureRandom = new SecureRandom();

        SecretKey tempKey;
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            log.warn("Şifreleme anahtarı tanımlanmamış, yeni anahtar oluşturuluyor");
            tempKey = generateKey();
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                tempKey = new SecretKeySpec(keyBytes, ALGORITHM);
                log.info("Şifreleme anahtarı başarıyla yüklendi");
            } catch (Exception e) {
                log.error("Şifreleme anahtarı yüklenemedi, yeni anahtar oluşturuluyor: {}", e.getMessage());
                tempKey = generateKey();
            }
        }
        this.secretKey = tempKey;
    }

    /**
     * Encrypts a plain text string using AES-256-GCM.
     *
     * @param plainText Text to encrypt
     * @return Base64 encoded encrypted data (IV + encrypted data + tag)
     * @throws TechnicalException if encryption fails
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
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

        } catch (Exception e) {
            log.error("Şifreleme hatası: {}", e.getMessage(), e);
            throw new TechnicalException(ErrorCodes.ENCRYPTION_ERROR,
                "Veri şifrelenirken hata oluştu", e);
        }
    }

    /**
     * Decrypts an encrypted string using AES-256-GCM.
     *
     * @param encryptedText Base64 encoded encrypted data
     * @return Decrypted plain text
     * @throws TechnicalException if decryption fails
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            if (encryptedWithIv.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Geçersiz şifreli veri formatı");
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

        } catch (Exception e) {
            log.error("Şifre çözme hatası: {}", e.getMessage(), e);
            throw new TechnicalException(ErrorCodes.DECRYPTION_ERROR,
                "Veri şifresi çözülürken hata oluştu", e);
        }
    }

    /**
     * Encrypts sensitive PII data (TC Kimlik, phone numbers, etc.).
     *
     * @param piiData PII data to encrypt
     * @return Encrypted PII data
     */
    public String encryptPII(String piiData) {
        if (piiData == null || piiData.trim().isEmpty()) {
            return piiData;
        }

        log.debug("PII verisi şifreleniyor");
        return encrypt(piiData.trim());
    }

    /**
     * Decrypts sensitive PII data.
     *
     * @param encryptedPII Encrypted PII data
     * @return Decrypted PII data
     */
    public String decryptPII(String encryptedPII) {
        if (encryptedPII == null || encryptedPII.trim().isEmpty()) {
            return encryptedPII;
        }

        log.debug("PII verisi şifresi çözülüyor");
        return decrypt(encryptedPII);
    }

    /**
     * Encrypts a TC Kimlik number.
     *
     * @param tcKimlik TC Kimlik number
     * @return Encrypted TC Kimlik
     */
    public String encryptTcKimlik(String tcKimlik) {
        if (tcKimlik == null || !tcKimlik.matches("^[0-9]{11}$")) {
            return tcKimlik;
        }

        log.debug("TC Kimlik şifreleniyor");
        return encryptPII(tcKimlik);
    }

    /**
     * Decrypts a TC Kimlik number.
     *
     * @param encryptedTcKimlik Encrypted TC Kimlik
     * @return Decrypted TC Kimlik
     */
    public String decryptTcKimlik(String encryptedTcKimlik) {
        log.debug("TC Kimlik şifresi çözülüyor");
        return decryptPII(encryptedTcKimlik);
    }

    /**
     * Encrypts a phone number.
     *
     * @param phoneNumber Phone number
     * @return Encrypted phone number
     */
    public String encryptPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        log.debug("Telefon numarası şifreleniyor");
        return encryptPII(phoneNumber.trim());
    }

    /**
     * Decrypts a phone number.
     *
     * @param encryptedPhoneNumber Encrypted phone number
     * @return Decrypted phone number
     */
    public String decryptPhoneNumber(String encryptedPhoneNumber) {
        log.debug("Telefon numarası şifresi çözülüyor");
        return decryptPII(encryptedPhoneNumber);
    }

    /**
     * Checks if a string is encrypted (basic heuristic check).
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

    /**
     * Generates a new AES-256 encryption key.
     *
     * @return Generated secret key
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            log.info("Yeni AES-256 şifreleme anahtarı oluşturuldu");
            return key;
        } catch (Exception e) {
            throw new TechnicalException(ErrorCodes.ENCRYPTION_ERROR,
                "Şifreleme anahtarı oluşturulamadı", e);
        }
    }

    /**
     * Generates a new encryption key and returns it as Base64 encoded string.
     *
     * @return Base64 encoded encryption key
     */
    public static String generateKeyAsBase64() {
        SecretKey key = generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Validates if the encryption key is properly configured.
     *
     * @return true if key is valid
     */
    public boolean isKeyValid() {
        try {
            // Test encryption/decryption
            String testData = "test-encryption-key-validation";
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            return testData.equals(decrypted);
        } catch (Exception e) {
            log.error("Şifreleme anahtarı doğrulama hatası: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the algorithm information.
     *
     * @return Algorithm details
     */
    public String getAlgorithmInfo() {
        return String.format("Algorithm: %s, Transformation: %s, Key Length: %d bits",
            ALGORITHM, TRANSFORMATION, KEY_LENGTH);
    }

    /**
     * Wipes sensitive data from memory (best effort).
     *
     * @param sensitiveData Character array to wipe
     */
    public static void wipeSensitiveData(char[] sensitiveData) {
        if (sensitiveData != null) {
            for (int i = 0; i < sensitiveData.length; i++) {
                sensitiveData[i] = '\0';
            }
        }
    }

    /**
     * Wipes sensitive data from memory (best effort).
     *
     * @param sensitiveData Byte array to wipe
     */
    public static void wipeSensitiveData(byte[] sensitiveData) {
        if (sensitiveData != null) {
            for (int i = 0; i < sensitiveData.length; i++) {
                sensitiveData[i] = 0;
            }
        }
    }
}