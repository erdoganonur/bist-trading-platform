package com.bisttrading.broker.algolab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Configuration for AlgoLab WebSocket client.
 *
 * Note: AlgoLabWebSocketClient is a @Component and creates its own
 * StandardWebSocketClient internally, so no additional beans are needed here.
 */
@Configuration
@EnableWebSocket
public class AlgoLabWebSocketConfig {
    // WebSocket configuration
    // AlgoLabWebSocketClient is auto-configured via @Component
}
