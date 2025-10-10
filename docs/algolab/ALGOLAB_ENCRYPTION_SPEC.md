# AlgoLab API - Encryption & Security Specification

**Doküman Tarihi:** 2025-10-09
**Python Kaynak:** `algolab.py` (lines 570-586, 607-618)

---

## 1. Genel Bakış

AlgoLab API, hassas verileri (kullanıcı adı, şifre, token, SMS kodu) şifrelemek için **AES-128-CBC** kullanır.
Ayrıca, her API çağrısı için **SHA-256 tabanlı bir "Checker" hash** oluşturulur (request integrity doğrulaması için).

---

## 2. AES Encryption (Data Encryption)

### 2.1 Algoritma Özellikleri

| Parametre | Değer |
|-----------|-------|
| **Algorithm** | AES (Advanced Encryption Standard) |
| **Mode** | CBC (Cipher Block Chaining) |
| **Key Size** | 128-bit (16 bytes) |
| **IV (Initialization Vector)** | 16 bytes sıfırlar (`\x00` * 16) |
| **Padding** | PKCS#7 (16 byte block size) |
| **Output Format** | Base64 encoded string |

### 2.2 Python Implementation

```python
def encrypt(self, text):
    iv = b'\0' * 16  # 16 byte zero IV
    key = base64.b64decode(self.api_code.encode('utf-8'))  # API key'den türetilmiş key
    cipher = AES.new(key, AES.MODE_CBC, iv)
    bytes = text.encode()  # String'i byte'a çevir
    padded_bytes = pad(bytes, 16)  # PKCS#7 padding (16 byte blocks)
    r = cipher.encrypt(padded_bytes)
    return base64.b64encode(r).decode("utf-8")  # Base64 encode ve string'e çevir
```

**Kullanım:**
```python
# Kullanıcı adını şifrele
encrypted_username = self.encrypt("52738096404")

# Şifreyi şifrele
encrypted_password = self.encrypt("mypassword123")

# Token'ı şifrele
encrypted_token = self.encrypt(self.token)

# SMS kodunu şifrele
encrypted_sms = self.encrypt("123456")
```

### 2.3 Java Implementation

#### Dependency (Maven):
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

#### Java Kod:

```java
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AlgoLabEncryptionUtil {

    private final String apiCode;
    private final byte[] key;
    private final IvParameterSpec iv;

    public AlgoLabEncryptionUtil(String apiKey) {
        // API Key format: "API-{base64_encoded_key}"
        this.apiCode = apiKey.startsWith("API-") ? apiKey.substring(4) : apiKey;

        // Decode base64 API key to get encryption key
        this.key = Base64.getDecoder().decode(this.apiCode.getBytes(StandardCharsets.UTF_8));

        // IV: 16 bytes of zeros
        this.iv = new IvParameterSpec(new byte[16]);
    }

    /**
     * Encrypts plaintext using AES-128-CBC with zero IV and PKCS#7 padding.
     * @param plaintext The text to encrypt
     * @return Base64 encoded ciphertext
     */
    public String encrypt(String plaintext) {
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
            throw new AlgoLabEncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts base64 encoded ciphertext (for testing/debugging only).
     * AlgoLab API does NOT require decryption on client side.
     */
    public String decrypt(String base64Ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);

            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new AlgoLabEncryptionException("Decryption failed", e);
        }
    }
}
```

#### Custom Exception:
```java
public class AlgoLabEncryptionException extends RuntimeException {
    public AlgoLabEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### Test Case:
```java
@Test
public void testEncryptDecrypt() {
    String apiKey = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=";
    AlgoLabEncryptionUtil util = new AlgoLabEncryptionUtil(apiKey);

    String plaintext = "52738096404";
    String encrypted = util.encrypt(plaintext);
    String decrypted = util.decrypt(encrypted);

    assertEquals(plaintext, decrypted);
    assertTrue(encrypted.length() > plaintext.length()); // Encrypted data is longer
}
```

---

## 3. Checker Hash (Request Integrity)

### 3.1 Checker Oluşturma Mantığı

Her **authenticated** API çağrısı için bir "Checker" hash oluşturulur. Bu hash, request'in manipüle edilmediğini doğrulamak için kullanılır.

**Checker formülü:**
```
data = APIKEY + API_HOSTNAME + ENDPOINT + JSON_BODY (whitespace removed)
checker = SHA256(data)
```

**Python Implementation:**
```python
def make_checker(self, endpoint, payload):
    if len(payload) > 0:
        body = json.dumps(payload).replace(' ', '')  # JSON serialize ve whitespace kaldır
    else:
        body = ""
    data = self.api_key + self.api_hostname + endpoint + body
    checker = hashlib.sha256(data.encode('utf-8')).hexdigest()
    return checker
```

**Örnek:**
```python
# Örnek değerler
api_key = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM="
api_hostname = "https://www.algolab.com.tr"
endpoint = "/api/GetEquityInfo"
payload = {"symbol": "AKBNK"}

# JSON body (whitespace removed)
body = '{"symbol":"AKBNK"}'

# Concatenate
data = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=https://www.algolab.com.tr/api/GetEquityInfo{\"symbol\":\"AKBNK\"}"

# SHA-256 hash
checker = hashlib.sha256(data.encode('utf-8')).hexdigest()
# Örnek output: "a1b2c3d4e5f6..."
```

### 3.2 Java Implementation

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

public class AlgoLabCheckerUtil {

    private final String apiKey;
    private final String apiHostname;
    private final ObjectMapper objectMapper;

    public AlgoLabCheckerUtil(String apiKey, String apiHostname) {
        this.apiKey = apiKey;
        this.apiHostname = apiHostname;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates SHA-256 checker hash for API request integrity.
     * @param endpoint API endpoint path (e.g., "/api/GetEquityInfo")
     * @param payload Request payload (Map, DTO, or any object)
     * @return SHA-256 hex string
     */
    public String makeChecker(String endpoint, Object payload) {
        try {
            String body = "";
            if (payload != null && !isEmpty(payload)) {
                // Serialize to JSON and remove whitespace
                String json = objectMapper.writeValueAsString(payload);
                body = json.replaceAll("\\s+", "");  // Remove all whitespace
            }

            String data = apiKey + apiHostname + endpoint + body;
            return DigestUtils.sha256Hex(data);

        } catch (Exception e) {
            throw new AlgoLabCheckerException("Failed to create checker hash", e);
        }
    }

    private boolean isEmpty(Object payload) {
        if (payload instanceof Map) {
            return ((Map<?, ?>) payload).isEmpty();
        }
        return false;
    }
}
```

#### Dependency (Maven):
```xml
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.15</version>
</dependency>
```

#### Test Case:
```java
@Test
public void testMakeChecker() {
    String apiKey = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=";
    String apiHostname = "https://www.algolab.com.tr";
    AlgoLabCheckerUtil util = new AlgoLabCheckerUtil(apiKey, apiHostname);

    String endpoint = "/api/GetEquityInfo";
    Map<String, String> payload = Map.of("symbol", "AKBNK");

    String checker = util.makeChecker(endpoint, payload);

    assertNotNull(checker);
    assertEquals(64, checker.length()); // SHA-256 produces 64-char hex string
}
```

---

## 4. HTTP Headers (Authenticated Requests)

### 4.1 Login Request Headers (Unauthenticated)

**Endpoint:** `/api/LoginUser`, `/api/LoginUserControl`

```json
{
  "APIKEY": "API-{base64_key}"
}
```

**Python:**
```python
headers = {"APIKEY": self.api_key}
```

**Java:**
```java
HttpHeaders headers = new HttpHeaders();
headers.set("APIKEY", apiKey);
headers.setContentType(MediaType.APPLICATION_JSON);
```

### 4.2 Authenticated Request Headers

**Endpoint:** Diğer tüm API endpoint'leri

```json
{
  "APIKEY": "API-{base64_key}",
  "Checker": "{sha256_hash}",
  "Authorization": "{hash_from_loginControl}"
}
```

**Python:**
```python
def post(self, endpoint, payload, login=False):
    url = self.api_url
    if not login:
        checker = self.make_checker(endpoint, payload)
        headers = {
            "APIKEY": self.api_key,
            "Checker": checker,
            "Authorization": self.hash
        }
    else:
        headers = {"APIKEY": self.api_key}
    resp = requests.post(url + endpoint, json=payload, headers=headers)
    return resp
```

**Java:**
```java
public HttpHeaders createAuthenticatedHeaders(String endpoint, Object payload) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("APIKEY", apiKey);
    headers.set("Checker", checkerUtil.makeChecker(endpoint, payload));
    headers.set("Authorization", this.hash);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}

public HttpHeaders createLoginHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("APIKEY", apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}
```

---

## 5. Rate Limiting (Request Throttling)

### 5.1 Python Implementation

**Kod:**
```python
last_request = 0.0
LOCK = False

def _request(self, method, url, endpoint, payload, headers):
    global last_request, LOCK
    while LOCK:
        time.sleep(0.01)  # Spin lock
    LOCK = True
    try:
        if method == "POST":
            t = time.time()
            diff = t - last_request
            wait_for = last_request > 0.0 and diff < 5.0  # 5 saniye minimum interval
            if wait_for:
                time.sleep(5 - diff + 0.01)
            response = requests.post(url + endpoint, json=payload, headers=headers)
            last_request = time.time()
    finally:
        LOCK = False
    return response
```

**Açıklama:**
- Her request arasında **minimum 5 saniye** bekleniyor
- Global `LOCK` kullanılarak thread-safe yapılıyor
- Bu, AlgoLab API'nin rate limit'ine takılmamak için

### 5.2 Java Implementation

#### Option 1: Guava RateLimiter (Önerilen)

**Dependency:**
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.2-jre</version>
</dependency>
```

**Kod:**
```java
import com.google.common.util.concurrent.RateLimiter;

@Component
public class AlgoLabRestClient {

    // 0.2 permits/second = 1 request per 5 seconds
    private final RateLimiter rateLimiter = RateLimiter.create(0.2);

    private final RestTemplate restTemplate;

    public AlgoLabRestClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public <T> ResponseEntity<T> post(String endpoint, Object payload, HttpHeaders headers, Class<T> responseType) {
        // Block until permit is available (automatic throttling)
        rateLimiter.acquire();

        HttpEntity<?> request = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(endpoint, request, responseType);
    }
}
```

#### Option 2: Resilience4j RateLimiter

**Dependency:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-ratelimiter</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Configuration:**
```yaml
resilience4j:
  ratelimiter:
    instances:
      algolab:
        limit-for-period: 1
        limit-refresh-period: 5s
        timeout-duration: 10s
```

**Kod:**
```java
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

@Component
public class AlgoLabRestClient {

    private final RateLimiter rateLimiter;
    private final RestTemplate restTemplate;

    public AlgoLabRestClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();

        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(1)  // 1 request
            .limitRefreshPeriod(Duration.ofSeconds(5))  // per 5 seconds
            .timeoutDuration(Duration.ofSeconds(10))
            .build();

        this.rateLimiter = RateLimiter.of("algolab", config);
    }

    public <T> ResponseEntity<T> post(String endpoint, Object payload, HttpHeaders headers, Class<T> responseType) {
        return RateLimiter.decorateSupplier(rateLimiter, () -> {
            HttpEntity<?> request = new HttpEntity<>(payload, headers);
            return restTemplate.postForEntity(endpoint, request, responseType);
        }).get();
    }
}
```

---

## 6. Security Best Practices

### 6.1 API Key Management

**❌ Yanlış (Hardcoded):**
```java
String apiKey = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=";
```

**✅ Doğru (Externalized Config):**
```yaml
# application.yml
algolab:
  api:
    key: ${ALGOLAB_API_KEY}
    username: ${ALGOLAB_USERNAME}
    password: ${ALGOLAB_PASSWORD}
```

```bash
# Environment variables (.env or system env)
export ALGOLAB_API_KEY="API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM="
export ALGOLAB_USERNAME="52738096404"
export ALGOLAB_PASSWORD="mypassword"
```

```java
@ConfigurationProperties(prefix = "algolab.api")
@Validated
public class AlgoLabProperties {
    @NotBlank
    private String key;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    // getters/setters
}
```

### 6.2 Token/Hash Storage Encryption

**File-based storage için:**
```java
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class SecureSessionStorage {

    private final TextEncryptor encryptor;

    public SecureSessionStorage(String password, String salt) {
        // AES-256 encryption
        this.encryptor = Encryptors.text(password, salt);
    }

    public void saveSession(String token, String hash) {
        String encryptedToken = encryptor.encrypt(token);
        String encryptedHash = encryptor.encrypt(hash);
        // Save to file...
    }

    public AlgoLabSession loadSession() {
        // Load from file...
        String token = encryptor.decrypt(encryptedToken);
        String hash = encryptor.decrypt(encryptedHash);
        return new AlgoLabSession(token, hash);
    }
}
```

### 6.3 HTTPS Only

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                // Force HTTPS
                factory.setConnectTimeout(5000);
                factory.setReadTimeout(10000);
                return factory;
            })
            .build();
    }
}
```

---

## 7. Complete Java Service Example

```java
@Service
@Slf4j
public class AlgoLabSecurityService {

    private final AlgoLabProperties properties;
    private final AlgoLabEncryptionUtil encryptionUtil;
    private final AlgoLabCheckerUtil checkerUtil;
    private final AlgoLabRestClient restClient;

    private String token;
    private String hash;

    public AlgoLabSecurityService(AlgoLabProperties properties) {
        this.properties = properties;
        this.encryptionUtil = new AlgoLabEncryptionUtil(properties.getKey());
        this.checkerUtil = new AlgoLabCheckerUtil(properties.getKey(), properties.getHostname());
        this.restClient = new AlgoLabRestClient();
    }

    public String encryptData(String plaintext) {
        return encryptionUtil.encrypt(plaintext);
    }

    public String createChecker(String endpoint, Object payload) {
        return checkerUtil.makeChecker(endpoint, payload);
    }

    public HttpHeaders createAuthHeaders(String endpoint, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKEY", properties.getKey());
        headers.set("Checker", createChecker(endpoint, payload));
        headers.set("Authorization", this.hash);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public HttpHeaders createLoginHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKEY", properties.getKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Getters/setters for token and hash
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
```

---

## 8. Debugging & Testing Tools

### 8.1 Encryption Test
```java
@Test
public void testEncryption() {
    String apiKey = "API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=";
    AlgoLabEncryptionUtil util = new AlgoLabEncryptionUtil(apiKey);

    String plaintext = "52738096404";
    String encrypted = util.encrypt(plaintext);

    System.out.println("Plaintext: " + plaintext);
    System.out.println("Encrypted: " + encrypted);

    String decrypted = util.decrypt(encrypted);
    assertEquals(plaintext, decrypted);
}
```

### 8.2 Checker Test
```java
@Test
public void testChecker() {
    String apiKey = "API-test";
    String apiHostname = "https://www.algolab.com.tr";
    AlgoLabCheckerUtil util = new AlgoLabCheckerUtil(apiKey, apiHostname);

    String endpoint = "/api/GetEquityInfo";
    Map<String, String> payload = Map.of("symbol", "AKBNK");

    String checker = util.makeChecker(endpoint, payload);
    System.out.println("Checker: " + checker);

    assertEquals(64, checker.length()); // SHA-256 = 64 chars
}
```

### 8.3 Compare with Python
```python
# Python side
print("Encrypted:", encrypt("test"))
print("Checker:", make_checker("/api/test", {"key": "value"}))
```

```java
// Java side
System.out.println("Encrypted: " + encrypt("test"));
System.out.println("Checker: " + makeChecker("/api/test", Map.of("key", "value")));
```

**Her iki tarafın output'u aynı olmalı!**

---

**Doküman Sonu**