package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AlgoLab session data (token and hash).
 * Used for both REST API and WebSocket authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgoLabSession {
    /**
     * Authentication token from LoginUser
     */
    private String token;

    /**
     * Authorization hash from LoginUserControl
     */
    private String hash;

    /**
     * Last update timestamp
     */
    private LocalDateTime lastUpdate;

    /**
     * WebSocket connection status
     */
    private boolean websocketConnected;

    /**
     * WebSocket last connection time
     */
    private LocalDateTime websocketLastConnected;
}
