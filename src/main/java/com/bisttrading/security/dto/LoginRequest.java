package com.bisttrading.core.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Login request DTO for user authentication.
 * Supports login with email, username, or TC Kimlik number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Username, email, or TC Kimlik number for authentication.
     */
    @NotBlank(message = "Kullanıcı adı, email veya TC Kimlik boş olamaz")
    @Size(min = 3, max = 100, message = "Kullanıcı bilgisi 3-100 karakter arası olmalıdır")
    @JsonProperty("username")
    private String username;

    /**
     * User password.
     */
    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, max = 128, message = "Şifre 8-128 karakter arası olmalıdır")
    @JsonProperty("password")
    private String password;

    /**
     * Remember me option for extended session.
     */
    @JsonProperty("rememberMe")
    private boolean rememberMe = false;

    /**
     * Two-factor authentication code (if enabled).
     */
    @Size(max = 10, message = "2FA kodu en fazla 10 karakter olabilir")
    @Pattern(regexp = "^[0-9]*$", message = "2FA kodu sadece rakam içerebilir")
    @JsonProperty("twoFactorCode")
    private String twoFactorCode;

    /**
     * Client IP address for security logging.
     */
    @JsonProperty("clientIp")
    private String clientIp;

    /**
     * User agent for security logging.
     */
    @JsonProperty("userAgent")
    private String userAgent;

    /**
     * Device fingerprint for security.
     */
    @JsonProperty("deviceFingerprint")
    private String deviceFingerprint;

    /**
     * Checks if the username is an email address.
     *
     * @return true if email format
     */
    public boolean isEmailLogin() {
        return username != null && username.contains("@");
    }

    /**
     * Checks if the username is a TC Kimlik number.
     *
     * @return true if TC Kimlik format
     */
    public boolean isTcKimlikLogin() {
        return username != null && username.matches("^[0-9]{11}$");
    }

    /**
     * Checks if two-factor authentication is provided.
     *
     * @return true if 2FA code is provided
     */
    public boolean hasTwoFactorCode() {
        return twoFactorCode != null && !twoFactorCode.trim().isEmpty();
    }

    /**
     * Gets the clean username (trimmed and lowercase for email).
     *
     * @return Clean username
     */
    public String getCleanUsername() {
        if (username == null) {
            return null;
        }

        String clean = username.trim();

        // Convert email to lowercase for consistency
        if (isEmailLogin()) {
            clean = clean.toLowerCase();
        }

        return clean;
    }

    /**
     * Validation helper to check password complexity.
     *
     * @return true if password meets complexity requirements
     */
    public boolean isPasswordComplex() {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Masks the password for logging purposes.
     *
     * @return Masked password
     */
    public String getMaskedPassword() {
        if (password == null) {
            return null;
        }
        return "*".repeat(password.length());
    }

    /**
     * Creates a string representation for logging (without sensitive data).
     *
     * @return Log-safe string
     */
    public String toLogString() {
        return String.format("LoginRequest{username='%s', rememberMe=%b, has2FA=%b, clientIp='%s'}",
            getCleanUsername(), rememberMe, hasTwoFactorCode(), clientIp);
    }
}