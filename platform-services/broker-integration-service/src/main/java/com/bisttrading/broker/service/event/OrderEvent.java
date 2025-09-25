package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
public abstract class OrderEvent {
    private String orderId;
    private String clientOrderId;
    private String symbol;
    private com.bisttrading.broker.algolab.model.OrderSide side;
    private Instant timestamp;
}