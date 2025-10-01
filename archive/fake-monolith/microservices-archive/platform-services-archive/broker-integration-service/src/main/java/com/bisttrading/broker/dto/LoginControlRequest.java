package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LoginUserControl API request
 */
@Data
public class LoginControlRequest {

    @JsonProperty("token")
    private String token; // Şifreli token

    @JsonProperty("password")
    private String password; // Şifreli SMS kodu
}