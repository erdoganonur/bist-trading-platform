# Python to Java Code Mapping - AlgoLab API Client

**Doküman Tarihi:** 2025-10-09
**Amaç:** Python AlgoLab client kodunu Java Spring Boot'a taşımak için kod karşılıkları

---

## 1. Dependencies & Libraries

### Python Dependencies
```python
import requests          # HTTP client
import hashlib           # SHA-256 for checker
import json              # JSON serialization
import base64            # Base64 encoding/decoding
import inspect           # Function name inspection
import time              # Sleep, timestamps
from Crypto.Cipher import AES           # AES encryption
from Crypto.Util.Padding import pad     # PKCS#7 padding
from threading import Thread            # Background threads
from datetime import datetime           # Date/time handling
```

### Java Dependencies (Maven)
```xml
<!-- Spring Boot Starter Web (includes RestTemplate) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter WebSocket (for future WebSocket support) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Jackson for JSON (included in spring-boot-starter-web) -->

<!-- BouncyCastle for AES encryption -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>

<!-- Apache Commons Codec for SHA-256 -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.15</version>
</dependency>

<!-- Guava for RateLimiter -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.2-jre</version>
</dependency>

<!-- Lombok (optional, for cleaner code) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## 2. Class Structure Mapping

### Python Class
```python
class API():
    def __init__(self, api_key, username, password, auto_login=True, keep_alive=False, verbose=True):
        self.api_key = api_key
        self.username = username
        self.password = password
        self.token = ""
        self.hash = ""
        self.auto_login = auto_login
        self.keep_alive = keep_alive
        self.verbose = verbose
```

### Java Class
```java
@Service
@Slf4j
public class AlgoLabApiClient {

    private final AlgoLabProperties properties;
    private final AlgoLabEncryptionUtil encryptionUtil;
    private final AlgoLabCheckerUtil checkerUtil;
    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;

    private String token;
    private String hash;
    private boolean autoLogin;
    private boolean keepAlive;

    @Autowired
    public AlgoLabApiClient(
        AlgoLabProperties properties,
        RestTemplateBuilder restTemplateBuilder
    ) {
        this.properties = properties;
        this.autoLogin = properties.isAutoLogin();
        this.keepAlive = properties.isKeepAlive();

        this.encryptionUtil = new AlgoLabEncryptionUtil(properties.getApiKey());
        this.checkerUtil = new AlgoLabCheckerUtil(properties.getApiKey(), properties.getHostname());
        this.restTemplate = restTemplateBuilder.build();
        this.rateLimiter = RateLimiter.create(0.2); // 5 seconds interval

        if (autoLogin) {
            start();
        }
    }

    // ... methods
}
```

---

## 3. Initialization & Auto-Login

### Python
```python
def start(self):
    if self.auto_login:
        s = self.load_settings()
        if not s or not self.is_alive:
            if self.verbose:
                print("Login zaman aşimina uğradi. Yeniden giriş yapiliyor...")
            if self.LoginUser():
                self.LoginUserControl()
        else:
            if self.verbose:
                print("Otomatik login başarili...")
    if self.keep_alive:
        self.thread_keepalive.start()
```

### Java
```java
@PostConstruct
public void start() {
    if (autoLogin) {
        boolean loaded = loadSettings();
        if (!loaded || !isAlive()) {
            log.info("Session expired or not found. Starting login flow...");
            String token = loginUser();
            String smsCode = requestSmsCodeFromUser(); // Console input or callback
            loginUserControl(token, smsCode);
        } else {
            log.info("Auto-login successful using saved session");
        }
    }

    if (keepAlive) {
        // Scheduled task will handle this
    }
}

@Scheduled(fixedDelay = 300000) // 5 minutes
public void sessionRefreshTask() {
    if (keepAlive) {
        try {
            sessionRefresh();
        } catch (Exception e) {
            log.error("Session refresh failed", e);
        }
    }
}
```

---

## 4. Encryption

### Python
```python
def encrypt(self, text):
    iv = b'\0' * 16
    key = base64.b64decode(self.api_code.encode('utf-8'))
    cipher = AES.new(key, AES.MODE_CBC, iv)
    bytes = text.encode()
    padded_bytes = pad(bytes, 16)
    r = cipher.encrypt(padded_bytes)
    return base64.b64encode(r).decode("utf-8")
```

### Java
```java
public String encrypt(String plaintext) {
    try {
        // Zero IV
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        // Decode base64 API key
        byte[] key = Base64.getDecoder().decode(apiCode.getBytes(StandardCharsets.UTF_8));

        // Create cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);

        // Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Return base64
        return Base64.getEncoder().encodeToString(ciphertext);

    } catch (Exception e) {
        throw new AlgoLabEncryptionException("Encryption failed", e);
    }
}
```

---

## 5. Checker Hash

### Python
```python
def make_checker(self, endpoint, payload):
    if len(payload) > 0:
        body = json.dumps(payload).replace(' ', '')
    else:
        body = ""
    data = self.api_key + self.api_hostname + endpoint + body
    checker = hashlib.sha256(data.encode('utf-8')).hexdigest()
    return checker
```

### Java
```java
public String makeChecker(String endpoint, Object payload) {
    try {
        String body = "";
        if (payload != null && !isEmpty(payload)) {
            String json = objectMapper.writeValueAsString(payload);
            body = json.replaceAll("\\s+", ""); // Remove whitespace
        }

        String data = apiKey + apiHostname + endpoint + body;
        return DigestUtils.sha256Hex(data);

    } catch (Exception e) {
        throw new AlgoLabCheckerException("Failed to create checker", e);
    }
}

private boolean isEmpty(Object payload) {
    if (payload instanceof Map) {
        return ((Map<?, ?>) payload).isEmpty();
    }
    return false;
}
```

---

## 6. HTTP POST Request

### Python
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

### Java
```java
public <T> ResponseEntity<T> post(String endpoint, Object payload, boolean login, Class<T> responseType) {
    // Rate limiting
    rateLimiter.acquire();

    // Build URL
    String url = properties.getApiUrl() + endpoint;

    // Build headers
    HttpHeaders headers = login ? createLoginHeaders() : createAuthHeaders(endpoint, payload);

    // Build request
    HttpEntity<Object> request = new HttpEntity<>(payload, headers);

    // Execute request
    return restTemplate.postForEntity(url, request, responseType);
}

private HttpHeaders createLoginHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("APIKEY", properties.getApiKey());
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}

private HttpHeaders createAuthHeaders(String endpoint, Object payload) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("APIKEY", properties.getApiKey());
    headers.set("Checker", checkerUtil.makeChecker(endpoint, payload));
    headers.set("Authorization", this.hash);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}
```

---

## 7. Rate Limiting

### Python (Global Lock + Sleep)
```python
last_request = 0.0
LOCK = False

def _request(self, method, url, endpoint, payload, headers):
    global last_request, LOCK
    while LOCK:
        time.sleep(0.01)
    LOCK = True
    try:
        if method == "POST":
            t = time.time()
            diff = t - last_request
            wait_for = last_request > 0.0 and diff < 5.0
            if wait_for:
                time.sleep(5 - diff + 0.01)
            response = requests.post(url + endpoint, json=payload, headers=headers)
            last_request = time.time()
    finally:
        LOCK = False
    return response
```

### Java (Guava RateLimiter)
```java
import com.google.common.util.concurrent.RateLimiter;

// In constructor
this.rateLimiter = RateLimiter.create(0.2); // 0.2 permits/sec = 5 sec interval

// In request method
public <T> ResponseEntity<T> post(...) {
    rateLimiter.acquire(); // Blocks until permit available
    // ... rest of request
}
```

---

## 8. Error Handling

### Python
```python
def error_check(self, resp, f, silent=False):
    try:
        if resp.status_code == 200:
            data = resp.json()
            return data
        else:
            if not silent:
                print(f"Error kodu: {resp.status_code}")
                print(resp.text)
            return False
    except:
        if not silent:
            print(f"{f}() fonksiyonunda veri tipi hatasi")
            print(resp.text)
        return False
```

### Java
```java
public <T> T errorCheck(ResponseEntity<String> response, Class<T> responseClass) {
    HttpStatus status = response.getStatusCode();

    if (status == HttpStatus.OK) {
        try {
            return objectMapper.readValue(response.getBody(), responseClass);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response: {}", response.getBody());
            throw new AlgoLabResponseParseException("Invalid JSON response", e);
        }
    } else if (status == HttpStatus.UNAUTHORIZED) {
        log.warn("Unauthorized - session expired");
        this.token = null;
        this.hash = null;
        throw new AlgoLabAuthenticationException("Session expired, re-login required");
    } else {
        log.error("API error: {} - {}", status, response.getBody());
        throw new AlgoLabApiException("API request failed: " + status);
    }
}
```

---

## 9. Session Persistence

### Python
```python
def save_settings(self):
    data = {
        "date": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "token": self.token,
        "hash": self.hash
    }
    with open("./data.json", "w") as f:
        json.dump(data, f)

def load_settings(self):
    try:
        with open("./data.json", "r") as f:
            data = json.load(f)
            self.token = data["token"]
            self.hash = data["hash"]
            return True
    except:
        return False
```

### Java (File-based)
```java
@Data
public class AlgoLabSession {
    private String date;
    private String token;
    private String hash;
}

public void saveSettings() {
    AlgoLabSession session = new AlgoLabSession();
    session.setDate(LocalDateTime.now().toString());
    session.setToken(this.token);
    session.setHash(this.hash);

    try {
        objectMapper.writeValue(new File(properties.getSessionFilePath()), session);
        log.debug("Session saved successfully");
    } catch (IOException e) {
        log.error("Failed to save session", e);
    }
}

public boolean loadSettings() {
    try {
        File file = new File(properties.getSessionFilePath());
        if (!file.exists()) {
            return false;
        }

        AlgoLabSession session = objectMapper.readValue(file, AlgoLabSession.class);
        this.token = session.getToken();
        this.hash = session.getHash();
        log.debug("Session loaded successfully");
        return true;

    } catch (IOException e) {
        log.warn("Failed to load session", e);
        return false;
    }
}
```

### Java (Database-based - Recommended)
```java
@Entity
@Table(name = "algolab_sessions")
@Data
public class AlgoLabSessionEntity {
    @Id
    private String userId;
    private String token;
    private String hash;
    private LocalDateTime lastUpdate;
}

@Repository
public interface AlgoLabSessionRepository extends JpaRepository<AlgoLabSessionEntity, String> {
}

@Service
public class AlgoLabSessionManager {

    @Autowired
    private AlgoLabSessionRepository repository;

    public void saveSession(String userId, String token, String hash) {
        AlgoLabSessionEntity entity = new AlgoLabSessionEntity();
        entity.setUserId(userId);
        entity.setToken(token);
        entity.setHash(hash);
        entity.setLastUpdate(LocalDateTime.now());
        repository.save(entity);
    }

    public Optional<AlgoLabSessionEntity> loadSession(String userId) {
        return repository.findById(userId);
    }
}
```

---

## 10. API Method Examples

### 10.1 LoginUser

**Python:**
```python
def LoginUser(self):
    f = inspect.stack()[0][3]
    u = self.encrypt(self.username)
    p = self.encrypt(self.password)
    payload = {"username": u, "password": p}
    endpoint = URL_LOGIN_USER
    resp = self.post(endpoint=endpoint, payload=payload, login=True)
    login_user = self.error_check(resp, f)
    if not login_user:
        return False
    login_user = resp.json()
    succ = login_user["success"]
    if succ:
        self.token = login_user["content"]["token"]
        return True
```

**Java:**
```java
public String loginUser() {
    String encryptedUsername = encryptionUtil.encrypt(properties.getUsername());
    String encryptedPassword = encryptionUtil.encrypt(properties.getPassword());

    Map<String, String> payload = Map.of(
        "username", encryptedUsername,
        "password", encryptedPassword
    );

    ResponseEntity<LoginUserResponse> response = post(
        "/api/LoginUser",
        payload,
        true, // login=true (no auth headers)
        LoginUserResponse.class
    );

    LoginUserResponse body = errorCheck(response, LoginUserResponse.class);
    if (body.isSuccess()) {
        this.token = body.getContent().getToken();
        log.info("Login successful");
        return token;
    } else {
        throw new AlgoLabAuthenticationException("Login failed: " + body.getMessage());
    }
}

@Data
public class LoginUserResponse {
    private boolean success;
    private String message;
    private LoginUserContent content;

    @Data
    public static class LoginUserContent {
        private String token;
    }
}
```

---

### 10.2 GetCandleData

**Python:**
```python
def GetCandleData(self, symbol, period):
    f = inspect.stack()[0][3]
    end_point = URL_GETCANDLEDATA
    payload = {
        'symbol': symbol,
        'period': period
    }
    resp = self.post(end_point, payload)
    return self.error_check(resp, f)
```

**Java:**
```java
public List<OHLCVData> getCandleData(String symbol, int periodMinutes) {
    Map<String, Object> payload = Map.of(
        "symbol", symbol,
        "period", String.valueOf(periodMinutes)
    );

    ResponseEntity<CandleDataResponse> response = post(
        "/api/GetCandleData",
        payload,
        false, // authenticated request
        CandleDataResponse.class
    );

    CandleDataResponse body = errorCheck(response, CandleDataResponse.class);
    if (body.isSuccess()) {
        return body.getContent().getCandles();
    } else {
        throw new AlgoLabApiException("Failed to get candle data: " + body.getMessage());
    }
}

@Data
public class CandleDataResponse {
    private boolean success;
    private String message;
    private CandleDataContent content;

    @Data
    public static class CandleDataContent {
        private List<OHLCVData> candles;
    }
}

@Data
public class OHLCVData {
    private String timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
}
```

---

### 10.3 SendOrder

**Python:**
```python
def SendOrder(self, symbol, direction, pricetype, price, lot, sms, email, subAccount):
    end_point = URL_SENDORDER
    payload = {
        "symbol": symbol,
        "direction": direction,
        "pricetype": pricetype,
        "price": price,
        "lot": lot,
        "sms": sms,
        "email": email,
        "subAccount": subAccount
    }
    resp = self.post(end_point, payload)
    try:
        data = resp.json()
        return data
    except:
        f = inspect.stack()[0][3]
        print(f"{f}() fonksiyonunda veri tipi hatasi")
        print(resp.text)
```

**Java:**
```java
public SendOrderResponse sendOrder(SendOrderRequest request) {
    Map<String, Object> payload = Map.of(
        "symbol", request.getSymbol(),
        "direction", request.getDirection(),
        "pricetype", request.getPriceType(),
        "price", request.getPrice().toString(),
        "lot", request.getLot().toString(),
        "sms", request.isSms(),
        "email", request.isEmail(),
        "subAccount", request.getSubAccount() != null ? request.getSubAccount() : ""
    );

    ResponseEntity<SendOrderResponse> response = post(
        "/api/SendOrder",
        payload,
        false,
        SendOrderResponse.class
    );

    SendOrderResponse body = errorCheck(response, SendOrderResponse.class);
    if (body.isSuccess()) {
        log.info("Order sent successfully: {}", body.getContent().getOrderId());
        return body;
    } else {
        throw new AlgoLabApiException("Failed to send order: " + body.getMessage());
    }
}

@Data
public class SendOrderRequest {
    private String symbol;
    private String direction; // BUY or SELL
    private String priceType; // limit or piyasa
    private BigDecimal price;
    private Integer lot;
    private boolean sms = false;
    private boolean email = false;
    private String subAccount;
}

@Data
public class SendOrderResponse {
    private boolean success;
    private String message;
    private SendOrderContent content;

    @Data
    public static class SendOrderContent {
        private String orderId;
        private String brokerOrderId;
        private String status;
    }
}
```

---

## 11. Threading & Background Tasks

### Python (Thread for Keep-Alive)
```python
from threading import Thread

def __init__(..., keep_alive=False):
    self.keep_alive = keep_alive
    self.thread_keepalive = Thread(target=self.ping)

def ping(self):
    while self.keep_alive:
        p = self.SessionRefresh(silent=True)
        time.sleep(60 * 5)

def start(self):
    if self.keep_alive:
        self.thread_keepalive.start()
```

### Java (Spring @Scheduled)
```java
@EnableScheduling
@Configuration
public class SchedulingConfig {
}

@Service
public class AlgoLabApiClient {

    private boolean keepAlive;

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void sessionRefreshTask() {
        if (keepAlive) {
            try {
                sessionRefresh();
                log.debug("Session refreshed successfully");
            } catch (Exception e) {
                log.error("Session refresh failed", e);
            }
        }
    }

    public void sessionRefresh() {
        Map<String, Object> emptyPayload = Map.of();
        ResponseEntity<Map> response = post(
            "/api/SessionRefresh",
            emptyPayload,
            false,
            Map.class
        );

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("Session expired during refresh");
            this.token = null;
            this.hash = null;
        }
    }
}
```

---

## 12. Properties Configuration

### Python (config.py)
```python
MY_API_KEY = 'API-...'
MY_USERNAME = "52738096404"
MY_PASSWORD = "141589"
api_hostname = "https://www.algolab.com.tr"
api_url = api_hostname + "/api"
```

### Java (application.yml)
```yaml
algolab:
  api:
    key: ${ALGOLAB_API_KEY}
    hostname: https://www.algolab.com.tr
    url: ${algolab.api.hostname}/api
  auth:
    username: ${ALGOLAB_USERNAME}
    password: ${ALGOLAB_PASSWORD}
    auto-login: true
    keep-alive: true
  session:
    storage: file  # or database
    file-path: ./algolab-session.json
```

### Java Configuration Class
```java
@ConfigurationProperties(prefix = "algolab")
@Validated
@Data
public class AlgoLabProperties {

    @NotNull
    private ApiConfig api;

    @NotNull
    private AuthConfig auth;

    @NotNull
    private SessionConfig session;

    @Data
    public static class ApiConfig {
        @NotBlank
        private String key;

        @NotBlank
        private String hostname;

        @NotBlank
        private String url;
    }

    @Data
    public static class AuthConfig {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        private boolean autoLogin = true;
        private boolean keepAlive = true;
    }

    @Data
    public static class SessionConfig {
        private String storage = "file"; // file or database
        private String filePath = "./algolab-session.json";
    }
}
```

---

## 13. Complete Java Service Structure

```
src/main/java/com/bisttrading/broker/algolab/
├── config/
│   ├── AlgoLabProperties.java
│   └── AlgoLabRestTemplateConfig.java
├── service/
│   ├── AlgoLabApiClient.java                 # Main API client
│   ├── AlgoLabAuthService.java               # Authentication logic
│   ├── AlgoLabOrderService.java              # Order management
│   ├── AlgoLabMarketDataService.java         # Market data
│   └── AlgoLabSessionManager.java            # Session persistence
├── util/
│   ├── AlgoLabEncryptionUtil.java            # AES encryption
│   ├── AlgoLabCheckerUtil.java               # SHA-256 checker
│   └── AlgoLabRateLimiter.java               # Rate limiting
├── dto/
│   ├── request/
│   │   ├── SendOrderRequest.java
│   │   ├── ModifyOrderRequest.java
│   │   └── LoginUserRequest.java
│   └── response/
│       ├── SendOrderResponse.java
│       ├── LoginUserResponse.java
│       ├── CandleDataResponse.java
│       └── InstantPositionResponse.java
├── exception/
│   ├── AlgoLabApiException.java
│   ├── AlgoLabAuthenticationException.java
│   ├── AlgoLabEncryptionException.java
│   └── AlgoLabResponseParseException.java
└── model/
    ├── AlgoLabSession.java
    └── OHLCVData.java
```

---

## 14. Testing Comparison

### Python Test
```python
# test.py
from algolab import API

api = API(
    api_key="API-test",
    username="12345678901",
    password="mypassword",
    auto_login=False,
    verbose=True
)

# Test encryption
encrypted = api.encrypt("test")
print(f"Encrypted: {encrypted}")

# Test checker
checker = api.make_checker("/api/test", {"key": "value"})
print(f"Checker: {checker}")
```

### Java Test
```java
@SpringBootTest
public class AlgoLabApiClientTest {

    @Autowired
    private AlgoLabEncryptionUtil encryptionUtil;

    @Autowired
    private AlgoLabCheckerUtil checkerUtil;

    @Test
    public void testEncryption() {
        String plaintext = "test";
        String encrypted = encryptionUtil.encrypt(plaintext);
        System.out.println("Encrypted: " + encrypted);

        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testChecker() {
        String endpoint = "/api/test";
        Map<String, String> payload = Map.of("key", "value");
        String checker = checkerUtil.makeChecker(endpoint, payload);
        System.out.println("Checker: " + checker);
        assertEquals(64, checker.length());
    }
}
```

---

## 15. Key Differences & Gotchas

| Aspect | Python | Java | Notlar |
|--------|--------|------|--------|
| **Type System** | Dynamic | Static | Java'da her DTO için class tanımlamak gerekir |
| **JSON Handling** | `json.dumps()`, `json.load()` | Jackson `ObjectMapper` | Java otomatik serialize/deserialize yapar |
| **Threading** | `Thread(target=func)` | `@Scheduled` annotation | Java daha kolay, declarative |
| **Error Handling** | `try/except`, return `False` | Custom exceptions | Java'da proper exception hierarchy kullan |
| **Rate Limiting** | Global lock + sleep | Guava RateLimiter | Java çözümü daha robust |
| **Property Inspection** | `inspect.stack()` for function name | SLF4J logger otomatik yapar | Java'da gerek yok |
| **Null Handling** | `None`, manual check | `Optional<T>`, `@Nullable` | Java daha type-safe |
| **String Formatting** | f-strings `f"Hello {name}"` | String.format() or `+` | Java 17+ text blocks kullanılabilir |

---

## 16. Migration Checklist

- [ ] Tüm Python dependencies'i Java karşılıklarına çevir
- [ ] DTO class'larını oluştur (request/response için)
- [ ] Encryption ve Checker util'leri implement et
- [ ] RestTemplate configuration ve rate limiting ekle
- [ ] Authentication flow'u implement et (LoginUser, LoginUserControl)
- [ ] Session management (file veya database-based)
- [ ] Scheduled task (SessionRefresh) ekle
- [ ] Tüm API endpoint'leri için service method'ları yaz
- [ ] Custom exception class'ları oluştur
- [ ] Unit test'ler yaz (encryption, checker, API calls)
- [ ] Integration test'ler yaz (mock server ile)
- [ ] Logging ekle (SLF4J)
- [ ] Configuration (application.yml, environment variables)
- [ ] Documentation (JavaDoc, README)

---

**Doküman Sonu**
