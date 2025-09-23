package com.bisttrading.core.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * User registration request DTO.
 * Contains all required information for new user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * User's email address (will be used as primary login).
     */
    @NotBlank(message = "Email adresi boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 100, message = "Email adresi en fazla 100 karakter olabilir")
    @JsonProperty("email")
    private String email;

    /**
     * User's preferred username (optional, email will be used if not provided).
     */
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arası olmalıdır")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Kullanıcı adı sadece harf, rakam, nokta, alt çizgi ve tire içerebilir")
    @JsonProperty("username")
    private String username;

    /**
     * User's password.
     */
    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, max = 128, message = "Şifre 8-128 karakter arası olmalıdır")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
             message = "Şifre en az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 özel karakter içermelidir")
    @JsonProperty("password")
    private String password;

    /**
     * Password confirmation.
     */
    @NotBlank(message = "Şifre tekrarı boş olamaz")
    @JsonProperty("confirmPassword")
    private String confirmPassword;

    /**
     * User's first name.
     */
    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arası olmalıdır")
    @Pattern(regexp = "^[a-zA-ZğĞıİöÖüÜşŞçÇ\\s]+$", message = "Ad sadece harf ve boşluk içerebilir")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name.
     */
    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arası olmalıdır")
    @Pattern(regexp = "^[a-zA-ZğĞıİöÖüÜşŞçÇ\\s]+$", message = "Soyad sadece harf ve boşluk içerebilir")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * User's Turkish identity number (TC Kimlik).
     */
    @NotBlank(message = "TC Kimlik numarası boş olamaz")
    @Pattern(regexp = "^[0-9]{11}$", message = "TC Kimlik numarası 11 haneli olmalıdır")
    @JsonProperty("tcKimlik")
    private String tcKimlik;

    /**
     * User's phone number.
     */
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^\\+90[0-9]{10}$", message = "Telefon numarası +90XXXXXXXXXX formatında olmalıdır")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    /**
     * User's birth date.
     */
    @NotNull(message = "Doğum tarihi boş olamaz")
    @Past(message = "Doğum tarihi geçmiş bir tarih olmalıdır")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("birthDate")
    private LocalDate birthDate;

    /**
     * User's preferred language (tr, en).
     */
    @Pattern(regexp = "^(tr|en)$", message = "Dil tercihi 'tr' veya 'en' olmalıdır")
    @JsonProperty("preferredLanguage")
    private String preferredLanguage = "tr";

    /**
     * User's timezone.
     */
    @JsonProperty("timezone")
    private String timezone = "Europe/Istanbul";

    /**
     * User's risk profile preference.
     */
    @Pattern(regexp = "^(CONSERVATIVE|MODERATE|AGGRESSIVE)$",
             message = "Risk profili CONSERVATIVE, MODERATE veya AGGRESSIVE olmalıdır")
    @JsonProperty("riskProfile")
    private String riskProfile = "MODERATE";

    /**
     * Whether user is a professional investor.
     */
    @JsonProperty("professionalInvestor")
    private boolean professionalInvestor = false;

    /**
     * User's investment experience level.
     */
    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED|EXPERT)$",
             message = "Yatırım deneyimi BEGINNER, INTERMEDIATE, ADVANCED veya EXPERT olmalıdır")
    @JsonProperty("investmentExperience")
    private String investmentExperience = "BEGINNER";

    /**
     * Terms and conditions acceptance.
     */
    @AssertTrue(message = "Kullanım şartları kabul edilmelidir")
    @JsonProperty("acceptTerms")
    private boolean acceptTerms;

    /**
     * Privacy policy acceptance.
     */
    @AssertTrue(message = "Gizlilik politikası kabul edilmelidir")
    @JsonProperty("acceptPrivacyPolicy")
    private boolean acceptPrivacyPolicy;

    /**
     * Marketing communications consent (optional).
     */
    @JsonProperty("acceptMarketing")
    private boolean acceptMarketing = false;

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
     * Referral code (optional).
     */
    @Size(max = 20, message = "Referans kodu en fazla 20 karakter olabilir")
    @JsonProperty("referralCode")
    private String referralCode;

    /**
     * Validates that passwords match.
     *
     * @return true if passwords match
     */
    public boolean doPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Validates TC Kimlik number using the official algorithm.
     *
     * @return true if TC Kimlik is valid
     */
    public boolean isTcKimlikValid() {
        if (tcKimlik == null || !tcKimlik.matches("^[0-9]{11}$")) {
            return false;
        }

        // TC Kimlik validation algorithm
        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = Character.getNumericValue(tcKimlik.charAt(i));
        }

        // First digit cannot be 0
        if (digits[0] == 0) {
            return false;
        }

        // Calculate checksum
        int evenSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int oddSum = digits[1] + digits[3] + digits[5] + digits[7];

        int check10 = ((evenSum * 7) - oddSum) % 10;
        int check11 = (evenSum + oddSum + digits[9]) % 10;

        return check10 == digits[9] && check11 == digits[10];
    }

    /**
     * Calculates user's age from birth date.
     *
     * @return Age in years
     */
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    /**
     * Checks if user is adult (18+ years old).
     *
     * @return true if adult
     */
    public boolean isAdult() {
        return getAge() >= 18;
    }

    /**
     * Gets clean email (trimmed and lowercase).
     *
     * @return Clean email
     */
    public String getCleanEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    /**
     * Gets clean phone number (removes spaces and dashes).
     *
     * @return Clean phone number
     */
    public String getCleanPhoneNumber() {
        return phoneNumber != null ? phoneNumber.replaceAll("[\\s-]", "") : null;
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
        return String.format("RegisterRequest{email='%s', username='%s', firstName='%s', lastName='%s', " +
                           "professionalInvestor=%b, riskProfile='%s', clientIp='%s'}",
            getCleanEmail(), username, firstName, lastName,
            professionalInvestor, riskProfile, clientIp);
    }
}