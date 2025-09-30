package com.bisttrading.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * AlgoLab WebSocket konfigürasyon özellikleri
 */
@Data
@Component
@ConfigurationProperties(prefix = "algolab")
public class AlgoLabWebSocketProperties {

    private WebSocket websocket = new WebSocket();
    private Subscription subscription = new Subscription();

    @Data
    public static class WebSocket {
        private String url = "wss://www.algolab.com.tr/api/ws";
        private String hostname = "www.algolab.com.tr";
        private String apiKey;
        private Connection connection = new Connection();
        private Reconnection reconnection = new Reconnection();
        private Heartbeat heartbeat = new Heartbeat();
    }

    @Data
    public static class Connection {
        private Duration timeout = Duration.ofSeconds(30);
        private Duration pingInterval = Duration.ofSeconds(30);
        private int maxFrameSize = 65536;
    }

    @Data
    public static class Reconnection {
        private boolean enabled = true;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double multiplier = 2.0;
        private int maxAttempts = -1; // Unlimited
    }

    @Data
    public static class Heartbeat {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(30);
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Subscription {
        private List<String> symbols = List.of("ALL");
        private List<String> types = List.of("T", "D", "O");
    }

    /**
     * Formatted API key (prefix kontrolü ile)
     */
    public String getFormattedApiKey() {
        String key = websocket.getApiKey();
        if (key != null && !key.startsWith("API-")) {
            return "API-" + key;
        }
        return key;
    }
}