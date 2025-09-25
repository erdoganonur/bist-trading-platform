package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class HeartbeatReceivedEvent {
    private LocalDateTime timestamp;
    private String connectionId;
    private long latency;
}