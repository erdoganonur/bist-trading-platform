package com.bisttrading.broker.websocket.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeartbeatMessage extends WebSocketMessage {
    // Empty - just used for connection keep-alive
}