package com.bisttrading.broker.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionConfirmationMessage extends WebSocketMessage {

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("status")
    private String status; // "confirmed" or "failed"

    @JsonProperty("message")
    private String message;
}