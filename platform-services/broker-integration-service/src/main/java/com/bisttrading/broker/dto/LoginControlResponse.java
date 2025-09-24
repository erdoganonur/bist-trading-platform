package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LoginUserControl API response content
 */
@Data
public class LoginControlResponse {

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("session_expires_at")
    private String sessionExpiresAt;

    @JsonProperty("permissions")
    private String[] permissions;

    @JsonProperty("account_status")
    private String accountStatus;
}