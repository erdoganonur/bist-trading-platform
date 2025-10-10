package com.bisttrading.broker.algolab.util;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.exception.AlgoLabEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-128-CBC encryption utility for AlgoLab API.
 *
 * Algorithm: AES-128-CBC
 * IV: 16 bytes of zeros
 * Padding: PKCS5/PKCS7
 * Output: Base64 encoded
 */
@Component
@Slf4j
public class AlgoLabEncryptionUtil {

    private final byte[] key;
    private final IvParameterSpec iv;

    public AlgoLabEncryptionUtil(AlgoLabProperties properties) {
        String apiCode = properties.getApiCode();

        // Decode base64 API key to get encryption key
        this.key = Base64.getDecoder().decode(apiCode.getBytes(StandardCharsets.UTF_8));

        // IV: 16 bytes of zeros
        this.iv = new IvParameterSpec(new byte[16]);

        log.debug("AlgoLab encryption utility initialized with key length: {} bytes", key.length);
    }

    /**
     * Encrypts plaintext using AES-128-CBC.
     *
     * @param plaintext The text to encrypt
     * @return Base64 encoded ciphertext
     * @throws AlgoLabEncryptionException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new AlgoLabEncryptionException("Plaintext cannot be null or empty");
        }

        try {
            // Create AES cipher in CBC mode with PKCS5Padding (equivalent to PKCS#7 for AES)
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Create secret key spec from decoded API key
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Initialize cipher with key and IV
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);

            // Encrypt plaintext
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Return base64 encoded result
            return Base64.getEncoder().encodeToString(ciphertext);

        } catch (Exception e) {
            log.error("Encryption failed for plaintext length: {}", plaintext.length(), e);
            throw new AlgoLabEncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts base64 encoded ciphertext (for testing/debugging only).
     * AlgoLab API does NOT require decryption on client side.
     *
     * @param base64Ciphertext Base64 encoded ciphertext
     * @return Decrypted plaintext
     * @throws AlgoLabEncryptionException if decryption fails
     */
    public String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isEmpty()) {
            throw new AlgoLabEncryptionException("Ciphertext cannot be null or empty");
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);

            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new AlgoLabEncryptionException("Decryption failed", e);
        }
    }
}
