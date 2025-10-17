package com.bisttrading.broker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AlgoLab OTP verification (Step 2).
 * Verifies SMS code received from AlgoLab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AlgoLab OTP verification request")
public class VerifyOtpRequest {

    @NotBlank(message = "OTP code is required")
    @Size(min = 4, max = 8, message = "OTP code must be between 4 and 8 digits")
    @Pattern(regexp = "^[0-9]+$", message = "OTP code must contain only digits")
    @Schema(
        description = "SMS OTP code received from AlgoLab",
        example = "123456",
        required = true,
        minLength = 4,
        maxLength = 8
    )
    private String otpCode;
}
