package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Authentication request for AlgoLab API
 */
@Data
@Builder
@Jacksonized
public class AuthenticationRequest {

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("platform")
    private String platform;

    public static AuthenticationRequest of(String username, String password) {
        return AuthenticationRequest.builder()
            .username(username)
            .password(password)
            .deviceId("BIST_TRADING_PLATFORM")
            .appVersion("1.0.0")
            .platform("JAVA")
            .build();
    }
}