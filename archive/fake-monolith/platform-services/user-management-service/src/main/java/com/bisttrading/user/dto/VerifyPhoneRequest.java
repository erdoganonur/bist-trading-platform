package com.bisttrading.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for phone verification requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Telefon doğrulama isteği")
public class VerifyPhoneRequest {

    @Schema(description = "SMS doğrulama kodu", example = "123456", required = true)
    @NotBlank(message = "SMS kodu boş olamaz")
    @Size(min = 6, max = 6, message = "SMS kodu 6 haneli olmalıdır")
    @Pattern(regexp = "^\\d{6}$", message = "SMS kodu sadece rakam içermelidir")
    private String verificationCode;
}