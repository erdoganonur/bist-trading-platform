package com.bisttrading.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO for all API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hata yanıtı")
public class ErrorResponse {

    @Schema(description = "Hata kodu", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Hata mesajı", example = "Geçersiz istek verisi")
    private String message;

    @Schema(description = "Detaylı hata bilgileri")
    private Map<String, Object> details;

    @Schema(description = "Hata zamanı", example = "2024-01-15T10:30:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;

    @Schema(description = "İstek yolu", example = "/api/v1/auth/login")
    private String path;

    @Schema(description = "HTTP durum kodu", example = "400")
    private Integer status;
}