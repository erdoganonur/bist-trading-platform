package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LoginUser API response content
 */
@Data
public class LoginResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("expires_at")
    private String expiresAt;

    @JsonProperty("user_info")
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("username")
        private String username;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("account_type")
        private String accountType;
    }
}