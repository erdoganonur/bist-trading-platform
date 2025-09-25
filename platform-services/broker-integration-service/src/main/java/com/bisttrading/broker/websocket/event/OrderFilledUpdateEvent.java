package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class OrderFilledUpdateEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private String side;
    private BigDecimal filledQuantity;
    private BigDecimal filledPrice;
    private BigDecimal totalFilledValue;
    private LocalDateTime timestamp;
    private String fillId;
    private String status;
}