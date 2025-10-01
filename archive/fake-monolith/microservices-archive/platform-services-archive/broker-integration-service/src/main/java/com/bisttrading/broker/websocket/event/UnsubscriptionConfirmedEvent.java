package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class UnsubscriptionConfirmedEvent {
    private String channel;
    private String symbol;
    private String userId;
    private LocalDateTime timestamp;
    private boolean success;
}