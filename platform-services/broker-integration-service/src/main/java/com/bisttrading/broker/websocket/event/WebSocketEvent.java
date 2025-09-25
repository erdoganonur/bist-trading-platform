package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
public abstract class WebSocketEvent {
    private Instant timestamp;
}