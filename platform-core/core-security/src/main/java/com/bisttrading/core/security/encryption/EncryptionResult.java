package com.bisttrading.core.security.encryption;

import lombok.Builder;
import lombok.Data;

/**
 * Result of encryption operation containing encrypted data and metadata
 */
@Data
@Builder
public class EncryptionResult {

    private String encryptedData;
    private String algorithm;
    private String keyId;
    private String initializationVector;
    private long timestamp;
    private int keySize;

    /**
     * Check if encryption was successful
     */
    public boolean isSuccess() {
        return encryptedData != null && !encryptedData.isEmpty();
    }

    /**
     * Get encryption metadata as string
     */
    public String getMetadata() {
        return String.format("Algorithm: %s, KeySize: %d, KeyId: %s", algorithm, keySize, keyId);
    }
}