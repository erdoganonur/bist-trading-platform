package com.bisttrading.core.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * AES Encryption Service for secure data encryption and decryption
 */
@Slf4j
@Service
public class AESEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;

    private final Map<String, SecretKey> keyStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new AES key
     */
    public String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();

        String keyId = "key_" + System.currentTimeMillis();
        keyStore.put(keyId, secretKey);

        log.debug("Generated new AES key with ID: {}", keyId);
        return keyId;
    }

    /**
     * Store a key with given ID
     */
    public void storeKey(String keyId, byte[] keyBytes) {
        SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        keyStore.put(keyId, secretKey);
        log.debug("Stored AES key with ID: {}", keyId);
    }

    /**
     * Encrypt data using AES
     */
    public EncryptionResult encrypt(String data, String keyId) throws Exception {
        SecretKey secretKey = keyStore.get(keyId);
        if (secretKey == null) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());

        String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
        String ivString = Base64.getEncoder().encodeToString(iv);

        EncryptionResult result = EncryptionResult.builder()
            .encryptedData(encryptedData)
            .algorithm(ALGORITHM)
            .keyId(keyId)
            .initializationVector(ivString)
            .timestamp(System.currentTimeMillis())
            .keySize(KEY_SIZE)
            .build();

        log.debug("Data encrypted successfully with key: {}", keyId);
        return result;
    }

    /**
     * Decrypt data using AES
     */
    public String decrypt(String encryptedData, String keyId, String ivString) throws Exception {
        SecretKey secretKey = keyStore.get(keyId);
        if (secretKey == null) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = Base64.getDecoder().decode(ivString);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        String decryptedData = new String(decryptedBytes);
        log.debug("Data decrypted successfully with key: {}", keyId);
        return decryptedData;
    }

    /**
     * Decrypt using EncryptionResult
     */
    public String decrypt(EncryptionResult encryptionResult) throws Exception {
        return decrypt(
            encryptionResult.getEncryptedData(),
            encryptionResult.getKeyId(),
            encryptionResult.getInitializationVector()
        );
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String keyId) {
        return keyStore.containsKey(keyId);
    }

    /**
     * Remove key from store
     */
    public boolean removeKey(String keyId) {
        SecretKey removed = keyStore.remove(keyId);
        if (removed != null) {
            log.debug("Removed AES key with ID: {}", keyId);
            return true;
        }
        return false;
    }

    /**
     * Get number of stored keys
     */
    public int getKeyCount() {
        return keyStore.size();
    }
}