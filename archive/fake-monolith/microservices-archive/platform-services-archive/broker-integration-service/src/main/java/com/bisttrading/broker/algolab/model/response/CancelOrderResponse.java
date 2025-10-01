package com.bisttrading.broker.algolab.model.response;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Data
@Builder
public class CancelOrderResponse {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("cancelled_at")
    private LocalDateTime cancelledAt;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;
}