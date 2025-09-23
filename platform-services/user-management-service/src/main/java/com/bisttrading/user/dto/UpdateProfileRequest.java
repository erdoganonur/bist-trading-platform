package com.bisttrading.user.dto;

import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for user profile update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kullanıcı profil güncelleme isteği")
public class UpdateProfileRequest {

    @Schema(description = "Ad", example = "Ahmet", required = true)
    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s'-]+$",
             message = "Ad sadece harf, boşluk, apostrof ve tire içerebilir")
    private String firstName;

    @Schema(description = "Soyad", example = "Yılmaz", required = true)
    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s'-]+$",
             message = "Soyad sadece harf, boşluk, apostrof ve tire içerebilir")
    private String lastName;

    @Schema(description = "Telefon numarası", example = "+905551234567")
    @Pattern(regexp = "^(\\+90|0)?\\s?5\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}$",
             message = "Geçersiz Türk telefon numarası formatı")
    private String phoneNumber;

    @Schema(description = "TC Kimlik No (değiştirilemez)", example = "12345678901")
    @Pattern(regexp = "^\\d{11}$", message = "TC Kimlik No 11 haneli olmalıdır")
    private String tcKimlik;

    @Schema(description = "Doğum tarihi", example = "1990-01-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "Doğum tarihi geçmişte olmalıdır")
    private LocalDate dateOfBirth;

    @Schema(description = "Adres")
    @Size(max = 255, message = "Adres 255 karakterden fazla olamaz")
    private String address;

    @Schema(description = "Şehir", example = "İstanbul")
    @Size(max = 100, message = "Şehir adı 100 karakterden fazla olamaz")
    @Pattern(regexp = "^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s'-]*$",
             message = "Şehir adı sadece harf, boşluk, apostrof ve tire içerebilir")
    private String city;

    @Schema(description = "Posta kodu", example = "34000")
    @Pattern(regexp = "^\\d{5}$", message = "Posta kodu 5 haneli olmalıdır")
    private String postalCode;

    @Schema(description = "Ülke", example = "Türkiye")
    @Size(max = 100, message = "Ülke adı 100 karakterden fazla olamaz")
    @Pattern(regexp = "^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s'-]*$",
             message = "Ülke adı sadece harf, boşluk, apostrof ve tire içerebilir")
    private String country;

    @Schema(description = "Risk profili", example = "MODERATE")
    private UserEntity.RiskProfile riskProfile;

    @Schema(description = "Yatırım deneyimi", example = "BEGINNER")
    private UserEntity.InvestmentExperience investmentExperience;
}