package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderRejectedUpdateEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private String reason;
    private String errorCode;
    private LocalDateTime timestamp;
}