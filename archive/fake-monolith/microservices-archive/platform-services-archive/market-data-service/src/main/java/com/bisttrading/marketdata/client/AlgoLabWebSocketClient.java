package com.bisttrading.marketdata.client;

import com.bisttrading.marketdata.config.AlgoLabWebSocketProperties;
import com.bisttrading.marketdata.config.WebSocketConfig;
import com.bisttrading.marketdata.dto.MarketMessage;
import com.bisttrading.marketdata.exception.AlgoLabWebSocketException;
import com.bisttrading.marketdata.handler.MarketDataHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AlgoLab WebSocket client - Python AlgoLabSocket sınıfının Java karşılığı
 *
 * Bu sınıf Python ws.py dosyasındaki AlgoLabSocket sınıfını portlar:
 * - SSL bağlantı kurma
 * - Header-based authentication (APIKEY, Authorization, Checker)
 * - Heartbeat mekanizması
 * - Mesaj gönderme ve alma
 * - Otomatik yeniden bağlanma
 */
@Slf4j
@Component
public class AlgoLabWebSocketClient {

    private final AlgoLabWebSocketProperties properties;
    private final MarketDataHandler marketDataHandler;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;
    private final WebSocketConfig.WebSocketMetrics metrics;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicReference<String> authHash = new AtomicReference<>();
    private final AtomicReference<WebSocketClient> webSocketClient = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> heartbeatTask = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> reconnectTask = new AtomicReference<>();

    public AlgoLabWebSocketClient(AlgoLabWebSocketProperties properties,
                                MarketDataHandler marketDataHandler,
                                ObjectMapper objectMapper,
                                TaskScheduler taskScheduler,
                                WebSocketConfig.WebSocketMetrics metrics) {
        this.properties = properties;
        this.marketDataHandler = marketDataHandler;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
        this.metrics = metrics;
    }

    @PostConstruct
    public void initialize() {
        log.info("AlgoLab WebSocket client başlatılıyor...");
    }

    /**
     * WebSocket bağlantısını başlatır (Python connect() metodunun karşılığı)
     */
    @CircuitBreaker(name = "algolab-websocket")
    @Retry(name = "algolab-websocket")
    public boolean connect(String hash) {
        if (connected.get()) {
            log.warn("Zaten bağlı durumda");
            return true;
        }

        if (connecting.getAndSet(true)) {
            log.warn("Bağlantı zaten kuruluyor");
            return false;
        }

        try {
            authHash.set(hash);

            log.info("AlgoLab WebSocket bağlantısı kuruluyor: {}", properties.getWebsocket().getUrl());

            URI serverUri = URI.create(properties.getWebsocket().getUrl());
            Map<String, String> headers = createHeaders(hash);

            WebSocketClient client = new WebSocketClient(serverUri, headers) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    handleConnectionOpen(handshake);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    handleConnectionClose(code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    handleError(ex);
                }
            };

            // Connection timeout ayarla
            client.setConnectionLostTimeout((int) properties.getWebsocket().getConnection().getTimeout().toSeconds());

            webSocketClient.set(client);

            // Bağlantıyı başlat
            boolean connectResult = client.connectBlocking(
                properties.getWebsocket().getConnection().getTimeout().toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            );

            if (connectResult) {
                connected.set(true);
                reconnectAttempts.set(0);
                startHeartbeat();
                log.info("AlgoLab WebSocket bağlantısı başarılı");
                metrics.incrementConnections();
                return true;
            } else {
                log.error("AlgoLab WebSocket bağlantısı kurulamadı");
                return false;
            }

        } catch (Exception e) {
            log.error("WebSocket bağlantı hatası: {}", e.getMessage(), e);
            metrics.recordError("connection");
            throw new AlgoLabWebSocketException("Bağlantı kurulamadı: " + e.getMessage(), e);
        } finally {
            connecting.set(false);
        }
    }

    /**
     * Bağlantı headers oluşturur (Python headers mantığının karşılığı)
     */
    private Map<String, String> createHeaders(String hash) {
        try {
            String apiKey = properties.getFormattedApiKey();
            String hostname = properties.getWebsocket().getHostname();

            // Python: self.data = self.api_key + api_hostname + "/ws"
            // Python: self.checker = hashlib.sha256(self.data.encode('utf-8')).hexdigest()
            String checkerData = apiKey + "https://" + hostname + "/ws";
            String checker = createSha256Hash(checkerData);

            return Map.of(
                "APIKEY", apiKey,
                "Authorization", hash,
                "Checker", checker
            );
        } catch (Exception e) {
            throw new AlgoLabWebSocketException("Header oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * SHA256 hash oluşturur (Python hashlib.sha256 karşılığı)
     */
    private String createSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new AlgoLabWebSocketException("SHA256 hash oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Mesaj gönderir (Python send() metodunun karşılığı)
     */
    public boolean send(Map<String, Object> data) {
        WebSocketClient client = webSocketClient.get();
        if (client == null || !connected.get()) {
            log.warn("WebSocket bağlı değil, mesaj gönderilemedi");
            return false;
        }

        try {
            // Python: data = {"token": self.hash}
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("token", authHash.get());
            message.putAll(data);

            String jsonMessage = objectMapper.writeValueAsString(message);

            client.send(jsonMessage);
            metrics.recordMessageSent();

            log.debug("Mesaj gönderildi: {}", jsonMessage);
            return true;

        } catch (Exception e) {
            log.error("Mesaj gönderme hatası: {}", e.getMessage(), e);
            metrics.recordError("send");
            return false;
        }
    }

    /**
     * Sembol subscription yapar (Python örneğindeki gibi)
     */
    public boolean subscribeToSymbols(String type, String[] symbols) {
        Map<String, Object> subscriptionData = Map.of(
            "Type", type,
            "Symbols", symbols
        );

        log.info("Sembol subscription: Type={}, Symbols={}", type, java.util.Arrays.toString(symbols));
        return send(subscriptionData);
    }

    /**
     * Tüm sembollere subscription yapar
     */
    public boolean subscribeToAll(String type) {
        return subscribeToSymbols(type, new String[]{"ALL"});
    }

    /**
     * Bağlantı açıldığında çalışır
     */
    private void handleConnectionOpen(ServerHandshake handshake) {
        log.info("WebSocket bağlantısı açıldı. Status: {}", handshake.getHttpStatus());
        connected.set(true);
        reconnectAttempts.set(0);

        // Otomatik subscription başlat
        startAutoSubscription();
    }

    /**
     * Mesaj geldiğinde çalışır (Python recv() metodunun karşılığı)
     */
    private void handleMessage(String message) {
        try {
            metrics.recordMessageReceived();
            log.debug("Mesaj alındı: {}", message);

            // JSON parse et
            MarketMessage marketMessage = objectMapper.readValue(message, MarketMessage.class);
            marketMessage.setTimestamp(LocalDateTime.now());

            // Message handler'a ilet
            marketDataHandler.handleMessage(marketMessage);

        } catch (Exception e) {
            log.error("Mesaj işleme hatası: {}", e.getMessage(), e);
            metrics.recordError("message_processing");
        }
    }

    /**
     * Bağlantı kapandığında çalışır
     */
    private void handleConnectionClose(int code, String reason, boolean remote) {
        log.warn("WebSocket bağlantısı kapandı. Code: {}, Reason: {}, Remote: {}", code, reason, remote);

        connected.set(false);
        metrics.decrementConnections();
        stopHeartbeat();

        // Otomatik yeniden bağlanma
        if (properties.getWebsocket().getReconnection().isEnabled()) {
            scheduleReconnection();
        }
    }

    /**
     * Hata durumunda çalışır
     */
    private void handleError(Exception ex) {
        log.error("WebSocket hatası: {}", ex.getMessage(), ex);
        metrics.recordError("websocket");

        connected.set(false);

        if (properties.getWebsocket().getReconnection().isEnabled()) {
            scheduleReconnection();
        }
    }

    /**
     * Heartbeat mekanizmasını başlatır (Python'da ping mantığı)
     */
    private void startHeartbeat() {
        if (!properties.getWebsocket().getHeartbeat().isEnabled()) {
            return;
        }

        ScheduledFuture<?> currentTask = heartbeatTask.get();
        if (currentTask != null) {
            currentTask.cancel(false);
        }

        Duration interval = properties.getWebsocket().getHeartbeat().getInterval();
        ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
            this::sendHeartbeat,
            interval
        );

        heartbeatTask.set(task);
        log.debug("Heartbeat başlatıldı, interval: {}", interval);
    }

    /**
     * Heartbeat mesajı gönderir
     */
    private void sendHeartbeat() {
        if (!connected.get()) {
            return;
        }

        Map<String, Object> heartbeatData = Map.of(
            "Type", "H",
            "timestamp", LocalDateTime.now().toString()
        );

        boolean sent = send(heartbeatData);
        log.debug("Heartbeat gönderildi: {}", sent);
    }

    /**
     * Heartbeat'i durdurur
     */
    private void stopHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask.getAndSet(null);
        if (task != null) {
            task.cancel(false);
            log.debug("Heartbeat durduruldu");
        }
    }

    /**
     * Otomatik subscription başlatır
     */
    private void startAutoSubscription() {
        try {
            // Tick data subscription
            if (properties.getSubscription().getTypes().contains("T")) {
                subscribeToAll("T");
            }

            // Depth data subscription
            if (properties.getSubscription().getTypes().contains("D")) {
                subscribeToAll("D");
            }

            // Order status subscription
            if (properties.getSubscription().getTypes().contains("O")) {
                subscribeToAll("O");
            }

        } catch (Exception e) {
            log.error("Otomatik subscription hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Yeniden bağlanma zamanlaması yapar
     */
    private void scheduleReconnection() {
        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = properties.getWebsocket().getReconnection().getMaxAttempts();

        if (maxAttempts > 0 && attempts > maxAttempts) {
            log.error("Maksimum yeniden bağlanma denemesi aşıldı: {}", attempts);
            return;
        }

        // Exponential backoff hesapla
        Duration initialDelay = properties.getWebsocket().getReconnection().getInitialDelay();
        double multiplier = properties.getWebsocket().getReconnection().getMultiplier();
        Duration maxDelay = properties.getWebsocket().getReconnection().getMaxDelay();

        long delayMs = Math.min(
            (long) (initialDelay.toMillis() * Math.pow(multiplier, attempts - 1)),
            maxDelay.toMillis()
        );

        Duration reconnectDelay = Duration.ofMillis(delayMs);

        log.info("Yeniden bağlanma zamanlandı. Deneme: {}, Gecikme: {}", attempts, reconnectDelay);

        ScheduledFuture<?> task = taskScheduler.schedule(
            this::attemptReconnection,
            java.time.Instant.now().plus(reconnectDelay)
        );

        reconnectTask.set(task);
        metrics.recordReconnection();
    }

    /**
     * Yeniden bağlanma denemesi yapar
     */
    private void attemptReconnection() {
        String hash = authHash.get();
        if (hash == null) {
            log.error("Auth hash bulunamadı, yeniden bağlanma yapılamıyor");
            return;
        }

        try {
            log.info("Yeniden bağlanma deneniyor...");
            connect(hash);
        } catch (Exception e) {
            log.error("Yeniden bağlanma başarısız: {}", e.getMessage(), e);
            scheduleReconnection();
        }
    }

    /**
     * Bağlantıyı kapatır (Python close() metodunun karşılığı)
     */
    public void close() {
        log.info("WebSocket bağlantısı kapatılıyor...");

        connected.set(false);
        stopHeartbeat();

        // Reconnect task'ını iptal et
        ScheduledFuture<?> reconnectTaskRef = reconnectTask.getAndSet(null);
        if (reconnectTaskRef != null) {
            reconnectTaskRef.cancel(false);
        }

        WebSocketClient client = webSocketClient.getAndSet(null);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("WebSocket kapatma hatası: {}", e.getMessage());
            }
        }

        authHash.set(null);
        log.info("WebSocket bağlantısı kapatıldı");
    }

    /**
     * Bağlantı durumunu döndürür
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Reconnect attempt sayısını döndürür
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    @PreDestroy
    public void destroy() {
        close();
    }
}