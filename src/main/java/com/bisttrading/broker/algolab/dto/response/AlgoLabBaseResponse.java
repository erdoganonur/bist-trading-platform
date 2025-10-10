package com.bisttrading.broker.algolab.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Base response structure for AlgoLab API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgoLabBaseResponse<T> {
    private boolean success;
    private String message;
    private T content;
    private String error;
    private Instant timestamp;
}
