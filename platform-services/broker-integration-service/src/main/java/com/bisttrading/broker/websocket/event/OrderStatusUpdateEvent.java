package com.bisttrading.broker.websocket.event;

import com.bisttrading.broker.algolab.model.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderStatusUpdateEvent extends WebSocketEvent {
    private String userId;
    private String orderId;
    private String clientOrderId;
    private OrderStatus status;
    private String updateType;
}