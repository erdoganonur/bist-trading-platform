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
 * DTO for password change requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Şifre değiştirme isteği")
public class ChangePasswordRequest {

    @Schema(description = "Mevcut şifre", example = "MevcutSifre123", required = true)
    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String currentPassword;

    @Schema(description = "Yeni şifre", example = "YeniSifre123", required = true)
    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 8, max = 128, message = "Şifre 8-128 karakter arasında olmalıdır")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&çğıöşüÇĞIİÖŞÜ]{8,}$",
        message = "Şifre en az 8 karakter olmalı ve büyük harf, küçük harf, rakam içermelidir"
    )
    private String newPassword;

    @Schema(description = "Yeni şifre tekrarı", example = "YeniSifre123", required = true)
    @NotBlank(message = "Şifre tekrarı boş olamaz")
    private String confirmPassword;

    /**
     * Validates that new password and confirmation match.
     */
    public boolean isNewPasswordValid() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}