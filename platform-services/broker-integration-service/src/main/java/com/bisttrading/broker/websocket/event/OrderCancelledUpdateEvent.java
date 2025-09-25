package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderCancelledUpdateEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private String reason;
    private LocalDateTime timestamp;
    private String status;
}