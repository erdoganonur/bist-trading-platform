package com.bisttrading.broker.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * AlgoLab API encryption utilities
 * Python karşılığı: encrypt() ve make_checker() metodları
 */
@Slf4j
@Component
public class AlgoLabEncryptionUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String HASH_ALGORITHM = "SHA-256";

    // Python'da iv = b'\0' * 16 kullanılıyor
    private static final byte[] ZERO_IV = new byte[16];

    /**
     * Text'i AES-256 ile şifreler (Python encrypt() metodunun karşılığı)
     *
     * @param text Şifrelenecek text
     * @param apiCode API code (base64 decode edilecek)
     * @return Base64 encoded şifreli text
     */
    public String encrypt(String text, String apiCode) {
        try {
            // Python: key = base64.b64decode(self.api_code.encode('utf-8'))
            byte[] keyBytes = Base64.getDecoder().decode(apiCode.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            // Python: iv = b'\0' * 16
            IvParameterSpec ivSpec = new IvParameterSpec(ZERO_IV);

            // Python: cipher = AES.new(key, AES.MODE_CBC, iv)
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Python: bytes = text.encode()
            // Python: padded_bytes = pad(bytes, 16)
            // Python: r = cipher.encrypt(padded_bytes)
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            // Python: return base64.b64encode(r).decode("utf-8")
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Şifreleme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Şifreleme işlemi başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Checker hash oluşturur (Python make_checker() metodunun karşılığı)
     *
     * @param apiKey API Key (örn: API-xyz123)
     * @param hostname API hostname (örn: https://www.algolab.com.tr)
     * @param endpoint API endpoint (örn: /api/LoginUser)
     * @param payload JSON payload (boşsa empty string)
     * @return SHA-256 hash
     */
    public String makeChecker(String apiKey, String hostname, String endpoint, String payload) {
        try {
            // Python: body = json.dumps(payload).replace(' ', '') if len(payload) > 0 else ""
            String body = (payload != null && !payload.isEmpty())
                ? payload.replaceAll("\\s", "")
                : "";

            // Python: data = self.api_key + self.api_hostname + endpoint + body
            String data = apiKey + hostname + endpoint + body;

            // Python: checker = hashlib.sha256(data.encode('utf-8')).hexdigest()
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Byte array'i hex string'e çevir
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algoritması bulunamadı: {}", e.getMessage(), e);
            throw new RuntimeException("Hash oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Text'in şifresini çözer (test amaçlı)
     *
     * @param encryptedText Base64 encoded şifreli text
     * @param apiCode API code
     * @return Şifresi çözülmüş text
     */
    public String decrypt(String encryptedText, String apiCode) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(apiCode.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            IvParameterSpec ivSpec = new IvParameterSpec(ZERO_IV);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Şifre çözme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Şifre çözme işlemi başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * API Key'i validate eder
     */
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        try {
            String apiCode = apiKey.startsWith("API-") ? apiKey.substring(4) : apiKey;
            // Base64 decode test et
            Base64.getDecoder().decode(apiCode.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            log.warn("Geçersiz API Key formatı: {}", apiKey);
            return false;
        }
    }
}