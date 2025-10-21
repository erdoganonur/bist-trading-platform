package com.bisttrading.broker.algolab.event;

import lombok.Getter;

import java.time.Instant;

/**
 * Event fired when AlgoLab authentication is completed successfully.
 * This event triggers WebSocket connection initialization.
 */
@Getter
public class AuthenticationCompletedEvent {

    private final String token;
    private final String hash;
    private final Instant timestamp;

    public AuthenticationCompletedEvent(String token, String hash) {
        this.token = token;
        this.hash = hash;
        this.timestamp = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("AuthenticationCompletedEvent{timestamp=%s}", timestamp);
    }
}
