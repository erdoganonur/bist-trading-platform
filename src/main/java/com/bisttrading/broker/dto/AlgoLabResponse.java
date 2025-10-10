package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard response wrapper for AlgoLab broker operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlgoLabResponse<T> {
    private boolean success;
    private T content;
    private String message;
    private String error;
    private String details;
    private Instant timestamp;

    public static <T> AlgoLabResponse<T> success(T content, String message) {
        return AlgoLabResponse.<T>builder()
            .success(true)
            .content(content)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    public static <T> AlgoLabResponse<T> error(String error, String message) {
        return AlgoLabResponse.<T>builder()
            .success(false)
            .error(error)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
}