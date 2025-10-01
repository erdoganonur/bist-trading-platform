package com.bisttrading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for email verification requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "E-posta doğrulama isteği")
public class VerifyEmailRequest {

    @Schema(description = "E-posta doğrulama kodu", example = "123456", required = true)
    @NotBlank(message = "Doğrulama kodu boş olamaz")
    @Size(min = 6, max = 6, message = "Doğrulama kodu 6 haneli olmalıdır")
    @Pattern(regexp = "^\\d{6}$", message = "Doğrulama kodu sadece rakam içermelidir")
    private String verificationCode;
}