package com.bisttrading.oms.event;

import com.bisttrading.oms.model.OMSOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private OMSOrder.OrderSide side;
    private OMSOrder.OrderType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime timestamp = LocalDateTime.now();
}