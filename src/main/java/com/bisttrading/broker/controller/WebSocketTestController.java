package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.service.AlgoLabSessionManager;
import com.bisttrading.broker.algolab.model.AlgoLabSession;
import com.bisttrading.broker.algolab.websocket.AlgoLabWebSocketClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test endpoint for WebSocket V2 client.
 */
@RestController
@RequestMapping("/api/test/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final AlgoLabWebSocketClientV2 webSocketClientV2;
    private final AlgoLabSessionManager sessionManager;

    /**
     * Test WebSocket V2 connection.
     */
    @PostMapping("/connect-v2")
    public ResponseEntity<Map<String, Object>> testConnectV2() {
        log.info("Testing WebSocket V2 connection...");

        try {
            // Load session
            AlgoLabSession session = sessionManager.loadSession();
            if (session == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No session found. Please login first."
                ));
            }

            // Connect using V2 client
            boolean connected = webSocketClientV2.connect(session.getToken(), session.getHash());

            if (connected) {
                log.info("✅ WebSocket V2 connected successfully!");

                // Try to subscribe
                log.info("📤 Sending subscription...");
                webSocketClientV2.subscribe("T", "GARAN");

                // Wait a bit to see if messages arrive
                Thread.sleep(3000);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "WebSocket V2 connected and subscription sent!",
                        "connected", webSocketClientV2.isConnected()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Failed to connect with V2 client"
                ));
            }

        } catch (Exception e) {
            log.error("Error testing WebSocket V2", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Disconnect WebSocket V2.
     */
    @PostMapping("/disconnect-v2")
    public ResponseEntity<Map<String, Object>> disconnectV2() {
        log.info("Disconnecting WebSocket V2...");

        try {
            webSocketClientV2.disconnect();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "WebSocket V2 disconnected"
            ));

        } catch (Exception e) {
            log.error("Error disconnecting WebSocket V2", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Check WebSocket V2 status.
     */
    @GetMapping("/status-v2")
    public ResponseEntity<Map<String, Object>> statusV2() {
        return ResponseEntity.ok(Map.of(
                "connected", webSocketClientV2.isConnected()
        ));
    }
}
