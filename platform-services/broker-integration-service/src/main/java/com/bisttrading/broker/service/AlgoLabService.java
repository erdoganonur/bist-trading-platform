package com.bisttrading.broker.service;

import com.bisttrading.broker.adapter.BrokerAdapter;
import com.bisttrading.broker.config.AlgoLabProperties;
import com.bisttrading.broker.dto.*;
import com.bisttrading.broker.exception.AlgoLabException;
import com.bisttrading.broker.util.AlgoLabEncryptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AlgoLab API Client Service
 * Python API class'ının Java Spring Boot karşılığı
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgoLabService implements BrokerAdapter {

    private final AlgoLabProperties properties;
    private final AlgoLabEncryptionUtil encryptionUtil;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // Session durumu
    private volatile String token;
    private volatile String hash;
    private volatile boolean isAuthenticated = false;
    private volatile LocalDateTime lastRequestTime = LocalDateTime.now();

    // Rate limiting için - Python'daki global last_request ve LOCK karşılığı
    private final AtomicLong lastRequestMillis = new AtomicLong(0);

    // Keep-alive için
    private ScheduledExecutorService keepAliveExecutor;

    private WebClient webClient;

    @PostConstruct
    public void initialize() {
        this.webClient = webClientBuilder
            .baseUrl(properties.getApiUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        if (properties.getSession().isAutoLogin()) {
            // Session bilgilerini yükle ve otomatik login başlat
            loadSessionData();
            if (!isAlive() || !isAuthenticated) {
                log.info("Otomatik login başlatılıyor...");
                authenticateAsync();
            } else {
                log.info("Önceki session bilgileri ile giriş yapıldı");
            }
        }

        if (properties.getSession().isKeepAlive()) {
            startKeepAlive();
        }
    }

    /**
     * Authentication işlemi (Python start() metodunun karşılığı)
     */
    public CompletableFuture<Void> authenticateAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (loginUser()) {
                    log.info("Login başarılı, SMS kontrolü bekleniyor...");
                    // SMS kontrolü manuel olarak çağrılmalı
                } else {
                    throw new AlgoLabException("Login işlemi başarısız");
                }
            } catch (Exception e) {
                log.error("Authentication hatası: {}", e.getMessage(), e);
                throw new AlgoLabException("Authentication başarısız: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Kullanıcı girişi (Python LoginUser() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public boolean loginUser() {
        log.info("Login işlemi yapılıyor...");

        try {
            // Python: u = self.encrypt(self.username)
            String encryptedUsername = encryptionUtil.encrypt(properties.getUsername(), properties.getApiCode());
            // Python: p = self.encrypt(self.password)
            String encryptedPassword = encryptionUtil.encrypt(properties.getPassword(), properties.getApiCode());

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(encryptedUsername);
            loginRequest.setPassword(encryptedPassword);

            String payload = objectMapper.writeValueAsString(loginRequest);

            AlgoLabResponse<LoginResponse> response = makeRequest(
                AlgoLabEndpoints.LOGIN_USER,
                payload,
                true, // login request
                LoginResponse.class
            ).block(properties.getTimeout().getConnect());

            if (response != null && response.isSuccessful()) {
                this.token = response.getContent().getToken();
                log.info("Login başarılı");
                return true;
            } else {
                String errorMsg = response != null ? response.getErrorMessage() : "Bilinmeyen hata";
                log.error("Login başarısız: {}", errorMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("LoginUser() fonksiyonunda hata: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * SMS kontrolü (Python LoginUserControl() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public boolean loginUserControl(String smsCode) {
        log.info("Login kontrolü yapılıyor...");

        try {
            if (token == null) {
                log.error("Token bulunamadı. Önce loginUser() çağrılmalı");
                return false;
            }

            // Python: t = self.encrypt(self.token)
            String encryptedToken = encryptionUtil.encrypt(token, properties.getApiCode());
            // Python: s = self.encrypt(self.sms_code)
            String encryptedSmsCode = encryptionUtil.encrypt(smsCode, properties.getApiCode());

            LoginControlRequest controlRequest = new LoginControlRequest();
            controlRequest.setToken(encryptedToken);
            controlRequest.setPassword(encryptedSmsCode);

            String payload = objectMapper.writeValueAsString(controlRequest);

            AlgoLabResponse<LoginControlResponse> response = makeRequest(
                AlgoLabEndpoints.LOGIN_USER_CONTROL,
                payload,
                true, // login request
                LoginControlResponse.class
            ).block(properties.getTimeout().getConnect());

            if (response != null && response.isSuccessful()) {
                this.hash = response.getContent().getHash();
                this.isAuthenticated = true;

                // Session bilgilerini kaydet
                saveSessionData();

                log.info("Login kontrolü başarılı");
                return true;
            } else {
                String errorMsg = response != null ? response.getErrorMessage() : "Bilinmeyen hata";
                log.error("Login kontrolü başarısız: {}", errorMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("LoginUserControl() fonksiyonunda hata: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Session yenileme (Python SessionRefresh() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    public boolean sessionRefresh() {
        return sessionRefresh(false);
    }

    /**
     * Session yenileme (sessiz mod destekli)
     */
    public boolean sessionRefresh(boolean silent) {
        try {
            if (!isAuthenticated) {
                if (!silent) {
                    log.warn("Authentication yapılmamış, session refresh yapılamaz");
                }
                return false;
            }

            AlgoLabResponse<Object> response = makeRequest(
                AlgoLabEndpoints.SESSION_REFRESH,
                "{}",
                false, // normal request (with auth headers)
                Object.class
            ).block(properties.getTimeout().getConnect());

            if (response != null && response.isSuccessful()) {
                if (!silent) {
                    log.info("Session başarıyla yenilendi");
                }
                return true;
            } else {
                if (!silent) {
                    String errorMsg = response != null ? response.getErrorMessage() : "Bilinmeyen hata";
                    log.error("Session refresh başarısız: {}", errorMsg);
                }
                return false;
            }

        } catch (Exception e) {
            if (!silent) {
                log.error("SessionRefresh() fonksiyonunda hata: {}", e.getMessage(), e);
            }
            return false;
        }
    }

    /**
     * Session durumunu kontrol eder (Python is_alive property'sinin karşılığı)
     */
    public boolean isAlive() {
        try {
            return sessionRefresh(true);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * HTTP request yapar (Python post() metodunun karşılığı)
     */
    private <T> Mono<AlgoLabResponse<T>> makeRequest(String endpoint, String payload, boolean isLoginRequest, Class<T> responseType) {
        return Mono.fromCallable(() -> {
            // Rate limiting - Python'daki global lock mantığı
            long currentTime = System.currentTimeMillis();
            long lastRequest = lastRequestMillis.get();
            long timeDiff = currentTime - lastRequest;

            // Python: 5 saniye bekleme
            if (lastRequest > 0 && timeDiff < properties.getRateLimit().getRequestDelay().toMillis()) {
                long waitTime = properties.getRateLimit().getRequestDelay().toMillis() - timeDiff;
                Thread.sleep(waitTime);
            }

            return null;
        })
        .then(
            webClient
                .post()
                .uri(endpoint)
                .headers(headers -> buildHeaders(headers, endpoint, payload, isLoginRequest))
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    lastRequestMillis.set(System.currentTimeMillis());
                    return parseResponse(responseBody, responseType);
                })
                .onErrorMap(WebClientResponseException.class, this::handleWebClientException)
        );
    }

    /**
     * HTTP headers oluşturur
     */
    private void buildHeaders(HttpHeaders headers, String endpoint, String payload, boolean isLoginRequest) {
        // Her request için API Key header'ı
        headers.set("APIKEY", properties.getFormattedApiKey());

        if (!isLoginRequest && isAuthenticated) {
            // Normal request için checker ve authorization headers
            String checker = encryptionUtil.makeChecker(
                properties.getFormattedApiKey(),
                properties.getBaseUrl(),
                endpoint,
                payload
            );
            headers.set("Checker", checker);
            headers.set("Authorization", hash);
        }
    }

    /**
     * Response'u parse eder
     */
    private <T> AlgoLabResponse<T> parseResponse(String responseBody, Class<T> contentType) {
        try {
            // Generic type handling için JavaType kullanılıyor
            var responseType = objectMapper.getTypeFactory()
                .constructParametricType(AlgoLabResponse.class, contentType);
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException e) {
            log.error("Response parse hatası: {}", e.getMessage(), e);
            throw new AlgoLabException("Response parse edilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * WebClient exception'ları handle eder
     */
    private AlgoLabException handleWebClientException(WebClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String responseBody = ex.getResponseBodyAsString();

        log.error("HTTP Hatası - Status: {}, Body: {}", status, responseBody);

        String errorMessage = switch (status.value()) {
            case 401 -> "Kimlik doğrulama hatası. Token geçersiz veya süresi dolmuş.";
            case 403 -> "Erişim izni yok. API anahtarı kontrol edilmeli.";
            case 429 -> "Çok fazla istek. Rate limit aşıldı.";
            case 500 -> "AlgoLab sunucu hatası.";
            case 503 -> "AlgoLab servisi geçici olarak kullanılamıyor.";
            default -> "HTTP hatası: " + status.getReasonPhrase();
        };

        return new AlgoLabException(errorMessage + " (HTTP " + status.value() + ")", ex);
    }

    /**
     * Keep-alive thread başlatır (Python ping() metodunun karşılığı)
     */
    private void startKeepAlive() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdown();
        }

        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AlgoLab-KeepAlive");
            thread.setDaemon(true);
            return thread;
        });

        long intervalMinutes = properties.getSession().getKeepAliveInterval().toMinutes();

        keepAliveExecutor.scheduleAtFixedRate(
            () -> {
                if (properties.getSession().isKeepAlive() && isAuthenticated) {
                    log.debug("Keep-alive session refresh çalışıyor...");
                    sessionRefresh(true);
                }
            },
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );

        log.info("Keep-alive thread başlatıldı, interval: {} dakika", intervalMinutes);
    }

    /**
     * Session verilerini dosyaya kaydeder (Python save_settings() metodunun karşılığı)
     */
    private void saveSessionData() {
        try {
            if (!isAuthenticated || token == null) {
                log.debug("Session bilgisi yok, kayıt atlanıyor");
                return;
            }

            // Session data object
            SessionData sessionData = new SessionData(
                token,
                hash,
                isAuthenticated,
                System.currentTimeMillis(),
                properties.getUsername()
            );

            // JSON dosyasına kaydet
            Path sessionDir = getSessionDirectory();
            Files.createDirectories(sessionDir);

            Path sessionFile = sessionDir.resolve("session.json");
            String json = objectMapper.writeValueAsString(sessionData);
            Files.writeString(sessionFile, json);

            log.debug("Session bilgileri kaydedildi: {}", sessionFile);
        } catch (Exception e) {
            log.warn("Session kayıt hatası: {}", e.getMessage());
        }
    }

    /**
     * Session verilerini dosyadan yükler (Python load_settings() metodunun karşılığı)
     */
    private void loadSessionData() {
        try {
            Path sessionFile = getSessionDirectory().resolve("session.json");

            if (!Files.exists(sessionFile)) {
                log.debug("Session dosyası bulunamadı: {}", sessionFile);
                return;
            }

            String json = Files.readString(sessionFile);
            SessionData sessionData = objectMapper.readValue(json, SessionData.class);

            // Session süresini kontrol et (24 saat TTL)
            long elapsed = System.currentTimeMillis() - sessionData.timestamp;
            if (elapsed > Duration.ofHours(24).toMillis()) {
                log.info("Session süresi dolmuş, dosya siliniyor");
                Files.deleteIfExists(sessionFile);
                return;
            }

            // Session bilgilerini yükle
            this.token = sessionData.token;
            this.hash = sessionData.hash;
            this.isAuthenticated = sessionData.authenticated;

            log.info("Session bilgileri yüklendi: {} saat önce kaydedilmiş",
                elapsed / (1000 * 60 * 60));

        } catch (Exception e) {
            log.warn("Session yükleme hatası: {}", e.getMessage());
        }
    }

    /**
     * Service'i temizle
     */
    public void destroy() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdown();
            try {
                if (!keepAliveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    keepAliveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                keepAliveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ===========================================
    // TRADING METHODS - Python'daki ORDERS bölümü
    // ===========================================

    /**
     * Emir gönderme (Python SendOrder() metodunun karşılığı) - stub
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> sendOrderLegacy(String symbol, String direction, String priceType,
                                           String price, String lot, Boolean sms, Boolean email, String subAccount) {

        log.info("sendOrderLegacy çağrıldı: {} {} {} @ {} - henüz implement edilmedi", symbol, direction, priceType, price);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    /**
     * Emir değiştirme (Python ModifyOrder() metodunun karşılığı) - stub
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> modifyOrder(String id, String price, String lot, Boolean viop, String subAccount) {

        log.info("modifyOrder çağrıldı: {} fiyat: {} lot: {} - henüz implement edilmedi", id, price, lot);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    /**
     * Emir iptal etme (Python DeleteOrder() metodunun karşılığı) - stub
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> deleteOrder(String id, String subAccount) {

        log.info("deleteOrder çağrıldı: {} - henüz implement edilmedi", id);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    /**
     * VIOP emir iptal etme (Python DeleteOrderViop() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> deleteOrderViop(String id, String amount, String subAccount) {

        log.info("VIOP emir iptal ediliyor: {} adet: {}", id, amount);

        try {
            DeleteOrderRequest deleteRequest = new DeleteOrderRequest();
            deleteRequest.setId(id);
            deleteRequest.setAmount(amount);
            deleteRequest.setSubAccount(subAccount);

            String payload = objectMapper.writeValueAsString(deleteRequest);

            return makeRequest(
                AlgoLabEndpoints.DELETE_ORDER_VIOP,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("DeleteOrderViop() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("VIOP emir iptal edilemedi: " + e.getMessage(), e);
        }
    }

    // ===========================================
    // MARKET DATA METHODS - Python'daki market data metodları
    // ===========================================

    /**
     * Sembol bilgisi getir (Python GetEquityInfo() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getEquityInfo(String symbol) {

        log.debug("Sembol bilgisi getiriliyor: {}", symbol);

        try {
            var request = objectMapper.createObjectNode();
            request.put("symbol", symbol);

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.GET_EQUITY_INFO,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetEquityInfo() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("Sembol bilgisi getirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Mum verisi getir (Python GetCandleData() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getCandleData(String symbol, String period) {

        log.debug("Mum verisi getiriliyor: {} period: {}", symbol, period);

        try {
            var request = objectMapper.createObjectNode();
            request.put("symbol", symbol);
            request.put("period", period);

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.GET_CANDLE_DATA,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetCandleData() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("Mum verisi getirilemedi: " + e.getMessage(), e);
        }
    }

    // ===========================================
    // ACCOUNT DATA METHODS - Python'daki hesap metodları
    // ===========================================

    /**
     * Alt hesapları getir (Python GetSubAccounts() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getSubAccounts() {

        log.debug("Alt hesaplar getiriliyor");

        try {
            return makeRequest(
                AlgoLabEndpoints.GET_SUB_ACCOUNTS,
                "{}",
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetSubAccounts() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("Alt hesaplar getirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Anlık pozisyonları getir (Python GetInstantPosition() metodunun karşılığı) - stub
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getInstantPosition(String subAccount) {

        log.info("getInstantPosition çağrıldı: {} - henüz implement edilmedi", subAccount);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    /**
     * Günlük işlemleri getir (Python GetTodaysTransaction() metodunun karşılığı) - stub
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getTodaysTransactions(String subAccount) {

        log.info("getTodaysTransactions çağrıldı: {} - henüz implement edilmedi", subAccount);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    // ===========================================
    // VIOP METHODS - Python'daki VIOP metodları
    // ===========================================

    /**
     * VIOP müşteri genel durumu (Python GetViopCustomerOverall() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getViopCustomerOverall(String subAccount) {

        log.debug("VIOP müşteri genel durumu getiriliyor, subAccount: {}", subAccount);

        try {
            var request = objectMapper.createObjectNode();
            request.put("Subaccount", subAccount != null ? subAccount : "");

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.VIOP_CUSTOMER_OVERALL,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetViopCustomerOverall() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("VIOP müşteri genel durumu getirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * VIOP müşteri işlemleri (Python GetViopCustomerTransactions() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getViopCustomerTransactions(String subAccount) {

        log.debug("VIOP müşteri işlemleri getiriliyor, subAccount: {}", subAccount);

        try {
            var request = objectMapper.createObjectNode();
            request.put("Subaccount", subAccount != null ? subAccount : "");

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.VIOP_CUSTOMER_TRANSACTIONS,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetViopCustomerTransactions() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("VIOP müşteri işlemleri getirilemedi: " + e.getMessage(), e);
        }
    }

    // ===========================================
    // ORDER HISTORY METHODS - Python'daki emir geçmişi metodları
    // ===========================================

    /**
     * Hisse senedi emir geçmişi (Python GetEquityOrderHistory() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getEquityOrderHistory(String id, String subAccount) {

        log.debug("Hisse senedi emir geçmişi getiriliyor, id: {}, subAccount: {}", id, subAccount);

        try {
            var request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("subAccount", subAccount != null ? subAccount : "");

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.GET_EQUITY_ORDER_HISTORY,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetEquityOrderHistory() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("Hisse senedi emir geçmişi getirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * VIOP emir geçmişi (Python GetViopOrderHistory() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-api")
    @Retry(name = "algolab-api")
    public AlgoLabResponse<Object> getViopOrderHistory(String id, String subAccount) {

        log.debug("VIOP emir geçmişi getiriliyor, id: {}, subAccount: {}", id, subAccount);

        try {
            var request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("subAccount", subAccount != null ? subAccount : "");

            String payload = objectMapper.writeValueAsString(request);

            return makeRequest(
                AlgoLabEndpoints.GET_VIOP_ORDER_HISTORY,
                payload,
                false,
                Object.class
            ).block(properties.getTimeout().getRead());

        } catch (Exception e) {
            log.error("GetViopOrderHistory() fonksiyonunda hata: {}", e.getMessage(), e);
            throw new AlgoLabException("VIOP emir geçmişi getirilemedi: " + e.getMessage(), e);
        }
    }

    // ===============================
    // BrokerAdapter Implementation
    // ===============================

    @Override
    public AuthenticationResult authenticate(Map<String, String> credentials) {
        try {
            // Mevcut authenticate metodunu kullan
            authenticateAsync().get();

            return new AuthenticationResult(
                isAuthenticated,
                token != null ? token : "",
                token,
                System.currentTimeMillis() + Duration.ofHours(24).toMillis(),
                isAuthenticated ? null : "Authentication başarısız"
            );
        } catch (Exception e) {
            log.error("BrokerAdapter authentication hatası: {}", e.getMessage(), e);
            return new AuthenticationResult(
                false,
                "",
                "",
                0,
                e.getMessage()
            );
        }
    }

    @Override
    public OrderResult sendOrder(Map<String, Object> orderData) {
        log.info("sendOrder çağrıldı - henüz implement edilmedi");
        return new OrderResult(
            false,
            orderData.get("id") != null ? orderData.get("id").toString() : "",
            "",
            OrderStatus.REJECTED,
            "Not yet implemented"
        );
    }

    /**
     * Emir gönderme (AlgoLab API implementasyonu) - stub
     */
    public AlgoLabResponse<Object> sendOrder(String symbol, String direction, String priceType,
                                           String price, String lot, Boolean sms, Boolean email, String subAccount) {
        log.info("sendOrder(AlgoLab) çağrıldı: {} {} {} @ {} - henüz implement edilmedi", symbol, direction, priceType, price);

        AlgoLabResponse<Object> response = new AlgoLabResponse<>();
        response.setSuccess(false);
        response.setMessage("Not yet implemented");
        return response;
    }

    @Override
    public CancelOrderResult cancelOrder(String orderId) {
        try {
            // AlgoLab order iptal mantığı
            log.info("Emir iptal ediliyor: {}", orderId);

            return new CancelOrderResult(
                true,
                orderId,
                OrderStatus.CANCELLED,
                null
            );
        } catch (Exception e) {
            log.error("Emir iptal hatası: {}", e.getMessage(), e);
            return new CancelOrderResult(
                false,
                orderId,
                OrderStatus.PENDING,
                e.getMessage()
            );
        }
    }

    @Override
    public MarketDataResult getMarketData(String symbol) {
        try {
            // AlgoLab market data alma mantığı
            log.info("Market data alınıyor: {}", symbol);

            // Mock data
            return new MarketDataResult(
                symbol,
                100.0 + Math.random() * 10, // lastPrice
                99.5 + Math.random() * 10,  // bidPrice
                100.5 + Math.random() * 10, // askPrice
                1000 + (long)(Math.random() * 10000), // volume
                System.currentTimeMillis(),
                true,
                null
            );
        } catch (Exception e) {
            log.error("Market data alma hatası: {}", e.getMessage(), e);
            return new MarketDataResult(
                symbol, 0, 0, 0, 0, 0, false, e.getMessage()
            );
        }
    }

    @Override
    public List<Map<String, Object>> getPositions() {
        try {
            // AlgoLab pozisyon alma mantığı
            log.info("Pozisyonlar alınıyor...");

            // Şimdilik boş liste döndür
            return List.of();
        } catch (Exception e) {
            log.error("Pozisyon alma hatası: {}", e.getMessage(), e);
            throw new BrokerPositionException("Pozisyonlar alınamadı: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return isAuthenticated && isAlive();
    }

    @Override
    public void disconnect() {
        try {
            isAuthenticated = false;
            token = null;
            hash = null;

            if (keepAliveExecutor != null) {
                keepAliveExecutor.shutdown();
            }

            // Session bilgilerini temizle
            clearSessionData();

            log.info("AlgoLab bağlantısı kapatıldı");
        } catch (Exception e) {
            log.error("Disconnect hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Session bilgilerini temizle
     */
    public void clearSessionData() {
        try {
            Path sessionFile = getSessionDirectory().resolve("session.json");
            Files.deleteIfExists(sessionFile);
            log.debug("Session dosyası silindi");
        } catch (Exception e) {
            log.warn("Session temizleme hatası: {}", e.getMessage());
        }
    }

    /**
     * Session dizinini al/oluştur
     */
    private Path getSessionDirectory() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".bist-trading");
    }

    /**
     * Session data sınıfı
     */
    private static class SessionData {
        public String token;
        public String hash;
        public boolean authenticated;
        public long timestamp;
        public String username;

        public SessionData() {}

        public SessionData(String token, String hash, boolean authenticated, long timestamp, String username) {
            this.token = token;
            this.hash = hash;
            this.authenticated = authenticated;
            this.timestamp = timestamp;
            this.username = username;
        }
    }

    // Getter methods
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getToken() {
        return token;
    }

    public String getHash() {
        return hash;
    }
}