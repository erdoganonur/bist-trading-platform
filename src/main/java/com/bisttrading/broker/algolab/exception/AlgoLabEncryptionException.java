package com.bisttrading.broker.algolab.exception;

/**
 * Exception thrown when encryption/decryption fails.
 */
public class AlgoLabEncryptionException extends AlgoLabException {
    public AlgoLabEncryptionException(String message) {
        super(message);
    }

    public AlgoLabEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
