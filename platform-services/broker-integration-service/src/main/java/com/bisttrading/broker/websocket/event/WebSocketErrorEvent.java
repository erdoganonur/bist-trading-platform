package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class WebSocketErrorEvent {
    private String errorCode;
    private String errorMessage;
    private String channel;
    private LocalDateTime timestamp;
    private String userId;
}