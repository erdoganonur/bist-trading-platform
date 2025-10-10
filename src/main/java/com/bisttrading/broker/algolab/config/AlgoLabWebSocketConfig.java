package com.bisttrading.broker.algolab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Configuration for AlgoLab WebSocket client.
 */
@Configuration
@EnableWebSocket
public class AlgoLabWebSocketConfig {

    /**
     * Creates a StandardWebSocketClient bean for AlgoLab WebSocket connections.
     */
    @Bean
    public StandardWebSocketClient algoLabWebSocketClient() {
        return new StandardWebSocketClient();
    }
}
