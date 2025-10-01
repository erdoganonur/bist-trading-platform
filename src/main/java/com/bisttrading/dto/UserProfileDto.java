package com.bisttrading.dto;

import com.bisttrading.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User profile DTO for API responses.
 * Contains user profile information without sensitive data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kullanıcı profil bilgileri")
public class UserProfileDto {

    @Schema(description = "Kullanıcı benzersiz kimliği", example = "user-123-456")
    private String id;

    @Schema(description = "E-posta adresi", example = "kullanici@example.com")
    private String email;

    @Schema(description = "Kullanıcı adı", example = "kullanici123")
    private String username;

    @Schema(description = "Ad", example = "Ahmet")
    private String firstName;

    @Schema(description = "Soyad", example = "Yılmaz")
    private String lastName;

    @Schema(description = "Telefon numarası", example = "+905551234567")
    private String phoneNumber;

    @Schema(description = "TC Kimlik No", example = "12345678901")
    private String tcKimlik;

    @Schema(description = "Doğum tarihi", example = "1990-01-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Schema(description = "Adres")
    private String address;

    @Schema(description = "Şehir", example = "İstanbul")
    private String city;

    @Schema(description = "Posta kodu", example = "34000")
    private String postalCode;

    @Schema(description = "Ülke", example = "Türkiye")
    private String country;

    @Schema(description = "Hesap durumu", example = "ACTIVE")
    private UserEntity.UserStatus status;

    @Schema(description = "Hesap aktif mi?", example = "true")
    private boolean active;

    @Schema(description = "E-posta doğrulanmış mı?", example = "true")
    private Boolean emailVerified;

    @Schema(description = "E-posta doğrulama tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime emailVerifiedAt;

    @Schema(description = "Telefon doğrulanmış mı?", example = "true")
    private Boolean phoneVerified;

    @Schema(description = "Telefon doğrulama tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime phoneVerifiedAt;

    @Schema(description = "KYC tamamlanmış mı?", example = "true")
    private Boolean kycCompleted;

    @Schema(description = "KYC tamamlanma tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime kycCompletedAt;

    @Schema(description = "KYC seviyesi", example = "BASIC")
    private UserEntity.KycLevel kycLevel;

    @Schema(description = "Profesyonel yatırımcı mı?", example = "false")
    private Boolean professionalInvestor;

    @Schema(description = "Risk profili", example = "MODERATE")
    private UserEntity.RiskProfile riskProfile;

    @Schema(description = "Yatırım deneyimi", example = "BEGINNER")
    private UserEntity.InvestmentExperience investmentExperience;

    @Schema(description = "Hesap oluşturma tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Son güncelleme tarihi", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Son giriş tarihi", example = "2024-01-15T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Son giriş IP adresi", example = "192.168.1.100")
    private String lastLoginIp;

    @Schema(description = "Başarısız giriş denemesi sayısı", example = "0")
    private Integer failedLoginAttempts;

    @Schema(description = "Hesap kilitlenme tarihi", example = "2024-01-15T15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime accountLockedUntil;

    @Schema(description = "Tam ad", example = "Ahmet Yılmaz")
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    @Schema(description = "Hesap aktif mi?", example = "true")
    public boolean isActive() {
        return status != null && status == UserEntity.UserStatus.ACTIVE;
    }

    @Schema(description = "Tam doğrulanmış mı?", example = "true")
    public boolean isFullyVerified() {
        return Boolean.TRUE.equals(emailVerified) &&
               Boolean.TRUE.equals(phoneVerified) &&
               Boolean.TRUE.equals(kycCompleted);
    }

    @Schema(description = "İşlem yapabilir mi?", example = "true")
    public boolean canTrade() {
        return isActive() && isFullyVerified();
    }
}