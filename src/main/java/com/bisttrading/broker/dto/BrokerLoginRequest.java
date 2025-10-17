package com.bisttrading.broker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AlgoLab broker login (Step 1).
 * Sends username/password to AlgoLab, which triggers SMS OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AlgoLab broker login request")
public class BrokerLoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(
        description = "AlgoLab broker username",
        example = "demo_user",
        required = true
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(
        description = "AlgoLab broker password",
        example = "demo_password",
        required = true
    )
    private String password;
}
