package com.bisttrading.user.dto;

import com.bisttrading.user.entity.UserSessionEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user session information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kullanıcı oturum bilgileri")
public class UserSessionDto {

    @Schema(description = "Oturum ID'si", example = "session-123-456")
    private String id;

    @Schema(description = "IP adresi", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "User Agent bilgisi", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    private String userAgent;

    @Schema(description = "Cihaz tipi", example = "DESKTOP")
    private UserSessionEntity.DeviceType deviceType;

    @Schema(description = "Konum bilgisi", example = "İstanbul, Türkiye")
    private String location;

    @Schema(description = "Güvenlik seviyesi", example = "STANDARD")
    private UserSessionEntity.SecurityLevel securityLevel;

    @Schema(description = "Oturum durumu", example = "ACTIVE")
    private UserSessionEntity.SessionStatus status;

    @Schema(description = "Oturum başlangıç tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Son aktivite tarihi", example = "2024-01-15T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivityAt;

    @Schema(description = "Oturum bitiş tarihi", example = "2024-01-15T16:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    @Schema(description = "Mevcut oturum mu?", example = "true")
    private Boolean isCurrent;

    @Schema(description = "Cihaz açıklaması", example = "Chrome 120 on Windows 10")
    public String getDeviceDescription() {
        if (userAgent == null) {
            return "Bilinmeyen cihaz";
        }

        String browser = extractBrowser(userAgent);
        String os = extractOperatingSystem(userAgent);

        return String.format("%s - %s", browser, os);
    }

    @Schema(description = "Oturum süresi (dakika)", example = "240")
    public Long getSessionDurationMinutes() {
        if (createdAt == null) {
            return null;
        }

        LocalDateTime endTime = endedAt != null ? endedAt :
                               (lastActivityAt != null ? lastActivityAt : LocalDateTime.now());

        return java.time.Duration.between(createdAt, endTime).toMinutes();
    }

    @Schema(description = "Oturum aktif mi?", example = "true")
    public boolean isActive() {
        return status == UserSessionEntity.SessionStatus.ACTIVE;
    }

    private String extractBrowser(String userAgent) {
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Edge";
        } else if (userAgent.contains("Opera")) {
            return "Opera";
        } else {
            return "Bilinmeyen tarayıcı";
        }
    }

    private String extractOperatingSystem(String userAgent) {
        if (userAgent.contains("Windows NT 10.0")) {
            return "Windows 10";
        } else if (userAgent.contains("Windows NT 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else {
            return "Bilinmeyen işletim sistemi";
        }
    }
}