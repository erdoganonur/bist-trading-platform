package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Jacksonized
public class ErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("details")
    private List<String> details;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("path")
    private String path;

    @JsonProperty("request_id")
    private String requestId;

    public boolean hasDetails() {
        return details != null && !details.isEmpty();
    }

    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();

        if (message != null) {
            sb.append(message);
        }

        if (hasDetails()) {
            sb.append(" Details: ");
            sb.append(String.join(", ", details));
        }

        return sb.toString();
    }
}