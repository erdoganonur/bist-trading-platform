package com.bisttrading.broker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for AlgoLab authentication operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AlgoLab authentication response")
public class BrokerAuthResponse {

    @Schema(description = "Success flag", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "SMS code sent to your phone")
    private String message;

    @Schema(description = "Whether SMS was sent (for login step)", example = "true")
    private Boolean smsSent;

    @Schema(description = "Whether user is authenticated (for verify step)", example = "true")
    private Boolean authenticated;

    @Schema(description = "Session expiration time (ISO 8601)", example = "2025-10-16T12:00:00Z")
    private Instant sessionExpiresAt;

    @Schema(description = "Response timestamp", example = "2025-10-15T10:30:00Z")
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Creates a successful login response (SMS sent).
     */
    public static BrokerAuthResponse loginSuccess() {
        return BrokerAuthResponse.builder()
            .success(true)
            .message("SMS kodu telefonunuza gönderildi. Lütfen kodu girerek doğrulama yapın.")
            .smsSent(true)
            .authenticated(false)
            .build();
    }

    /**
     * Creates a successful OTP verification response.
     */
    public static BrokerAuthResponse verifySuccess(Instant expiresAt) {
        return BrokerAuthResponse.builder()
            .success(true)
            .message("Doğrulama başarılı. AlgoLab'a başarıyla bağlandınız.")
            .authenticated(true)
            .sessionExpiresAt(expiresAt)
            .build();
    }

    /**
     * Creates a logout success response.
     */
    public static BrokerAuthResponse logoutSuccess() {
        return BrokerAuthResponse.builder()
            .success(true)
            .message("AlgoLab oturumu başarıyla sonlandırıldı.")
            .authenticated(false)
            .build();
    }

    /**
     * Creates an error response.
     */
    public static BrokerAuthResponse error(String message) {
        return BrokerAuthResponse.builder()
            .success(false)
            .message(message)
            .authenticated(false)
            .build();
    }
}
