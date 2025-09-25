package com.bisttrading.oms.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderCancelledEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private String reason;
    private LocalDateTime timestamp = LocalDateTime.now();
}