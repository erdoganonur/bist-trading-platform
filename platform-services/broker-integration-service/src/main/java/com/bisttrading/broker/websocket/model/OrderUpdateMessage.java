package com.bisttrading.broker.websocket.model;

import com.bisttrading.broker.algolab.model.OrderResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderUpdateMessage extends WebSocketMessage {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("order")
    private OrderResponse order;

    @JsonProperty("update_type")
    private String updateType; // "status_change", "fill", "cancel", etc.
}