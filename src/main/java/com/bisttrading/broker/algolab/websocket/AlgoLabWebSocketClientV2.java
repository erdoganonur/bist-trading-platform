package com.bisttrading.broker.algolab.websocket;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Alternative WebSocket client using Java-WebSocket library (similar to Python's websocket-client).
 * This implementation matches Python's behavior more closely with custom SSL context.
 */
@Component
@Slf4j
public class AlgoLabWebSocketClientV2 {

    private final AlgoLabProperties properties;
    private final ObjectMapper objectMapper;

    private WebSocketClient webSocketClient;
    private volatile boolean connected = false;
    private volatile String authToken;
    private volatile String authHash;

    public AlgoLabWebSocketClientV2(
            AlgoLabProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Connects to AlgoLab WebSocket with custom SSL context (like Python).
     */
    public boolean connect(String token, String hash) {
        try {
            this.authToken = token;
            this.authHash = hash;

            String url = properties.getWebsocket().getUrl();
            URI uri = new URI(url);

            log.info("Connecting to AlgoLab WebSocket: {}", url);

            // Prepare headers (like Python)
            String apiKey = properties.getApi().getKey();
            String hostname = properties.getApi().getHostname();

            // Calculate Checker: SHA-256(apiKey + hostname + "/ws")
            String checkerData = apiKey + hostname + "/ws";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(checkerData.getBytes(StandardCharsets.UTF_8));
            StringBuilder checker = new StringBuilder();
            for (byte b : hashBytes) {
                checker.append(String.format("%02x", b));
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("APIKEY", apiKey);
            headers.put("Authorization", hash);
            headers.put("Checker", checker.toString());

            log.debug("WebSocket headers: APIKEY={}, Authorization={}..., Checker={}",
                    apiKey, hash.substring(0, 20), checker.toString().substring(0, 20));

            // Create WebSocket client
            CountDownLatch latch = new CountDownLatch(1);

            webSocketClient = new WebSocketClient(uri, headers) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected = true;
                    log.info("✅ WebSocket connection opened!");
                    log.debug("Server handshake: HTTP {} {}", handshake.getHttpStatus(), handshake.getHttpStatusMessage());
                    latch.countDown();
                }

                @Override
                public void onMessage(String message) {
                    log.info("✅ Received message: {}", message.length() > 100 ? message.substring(0, 100) + "..." : message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    log.warn("⚠️  WebSocket connection closed: code={}, reason={}, remote={}", code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("❌ WebSocket error", ex);
                }
            };

            // Set up SSL socket factory (like Python's SSL context)
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, new SecureRandom());
                SSLSocketFactory factory = sslContext.getSocketFactory();
                webSocketClient.setSocketFactory(factory);
                log.debug("SSL context configured");
            } catch (Exception e) {
                log.error("Failed to configure SSL context", e);
            }

            // Connect with timeout
            log.debug("Initiating WebSocket connection...");
            boolean connectResult = webSocketClient.connectBlocking(10, TimeUnit.SECONDS);

            if (connectResult) {
                // Wait for onOpen callback
                boolean openSuccess = latch.await(5, TimeUnit.SECONDS);
                if (openSuccess) {
                    log.info("🎉 WebSocket connected successfully!");
                    return true;
                } else {
                    log.error("Timeout waiting for WebSocket to open");
                    return false;
                }
            } else {
                log.error("Failed to connect to WebSocket");
                return false;
            }

        } catch (Exception e) {
            log.error("Error connecting to WebSocket", e);
            return false;
        }
    }

    /**
     * Subscribes to AlgoLab channel.
     * Format: {"token": "hash", "Type": "T|D|O", "Symbols": ["SYMBOL"]}
     */
    public void subscribe(String type, String... symbols) {
        if (webSocketClient == null || !connected) {
            log.error("Cannot subscribe: WebSocket not connected");
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("token", authHash);
            message.put("Type", type);
            message.put("Symbols", symbols);

            String json = objectMapper.writeValueAsString(message);
            webSocketClient.send(json);

            log.info("📤 Sent subscription: Type={}, Symbols={}", type, String.join(",", symbols));

        } catch (Exception e) {
            log.error("Error sending subscription", e);
        }
    }

    /**
     * Handles incoming WebSocket message.
     */
    private void handleMessage(String message) {
        try {
            // Parse and handle message
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            log.debug("Parsed message: {}", data);

            // TODO: Add message type handling

        } catch (Exception e) {
            log.error("Error parsing message: {}", message, e);
        }
    }

    /**
     * Disconnects from WebSocket.
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            connected = false;
            log.info("Disconnected from WebSocket");
        }
    }

    /**
     * Checks if connected.
     */
    public boolean isConnected() {
        return connected && webSocketClient != null && !webSocketClient.isClosed();
    }
}
