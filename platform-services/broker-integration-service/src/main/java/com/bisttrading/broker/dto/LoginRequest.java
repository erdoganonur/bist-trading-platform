package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LoginUser API request
 */
@Data
public class LoginRequest {

    @JsonProperty("username")
    private String username; // Şifreli TC Kimlik

    @JsonProperty("password")
    private String password; // Şifreli DenizBank şifresi
}