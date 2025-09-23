package com.bisttrading.infrastructure.persistence.entity;

import com.bisttrading.infrastructure.persistence.converter.FieldEncryptionConverter;
import com.bisttrading.infrastructure.persistence.converter.JsonbConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * User entity for BIST Trading Platform.
 * Contains all user information with encrypted PII fields and audit support.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_organization_id", columnList = "organization_id"),
    @Index(name = "idx_users_status", columnList = "status"),
    @Index(name = "idx_users_created_at", columnList = "created_at"),
    @Index(name = "idx_users_last_login_at", columnList = "last_login_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserEntity {

    /**
     * Primary key - UUID.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    /**
     * Organization reference.
     */
    @Column(name = "organization_id", length = 36)
    private String organizationId;

    /**
     * User's email address (unique).
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Geçerli bir email adresi giriniz")
    @NotBlank(message = "Email adresi boş olamaz")
    private String email;

    /**
     * User's username (unique, optional).
     */
    @Column(name = "username", unique = true, length = 50)
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arası olmalıdır")
    private String username;

    /**
     * User's encrypted password hash.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "Şifre hash'i boş olamaz")
    private String passwordHash;

    /**
     * User's first name.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "Ad boş olamaz")
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Soyad boş olamaz")
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
    private String lastName;

    /**
     * User's Turkish identity number (TC Kimlik) - ENCRYPTED.
     */
    @Column(name = "tc_kimlik_no", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String tcKimlikNo;

    /**
     * User's phone number - ENCRYPTED.
     */
    @Column(name = "phone_number", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String phoneNumber;

    /**
     * User's birth date.
     */
    @Column(name = "birth_date")
    private LocalDateTime birthDate;

    /**
     * User's gender (M/F/O).
     */
    @Column(name = "gender", length = 1)
    @Pattern(regexp = "^[MFO]$", message = "Cinsiyet M, F veya O olmalıdır")
    private String gender;

    /**
     * User's nationality.
     */
    @Column(name = "nationality", length = 3)
    @Size(max = 3, message = "Uyruk en fazla 3 karakter olabilir")
    private String nationality;

    /**
     * User's preferred language.
     */
    @Column(name = "preferred_language", length = 5)
    @Pattern(regexp = "^(tr|en)(-[A-Z]{2})?$", message = "Geçerli dil kodu giriniz")
    private String preferredLanguage;

    /**
     * User's timezone.
     */
    @Column(name = "timezone", length = 50)
    @Size(max = 50, message = "Zaman dilimi en fazla 50 karakter olabilir")
    private String timezone;

    /**
     * User account status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Kullanıcı durumu boş olamaz")
    private UserStatus status;

    /**
     * Whether email is verified.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Email verification date.
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * Whether phone is verified.
     */
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    /**
     * Phone verification date.
     */
    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    /**
     * Whether KYC is completed.
     */
    @Column(name = "kyc_completed", nullable = false)
    @Builder.Default
    private Boolean kycCompleted = false;

    /**
     * KYC completion date.
     */
    @Column(name = "kyc_completed_at")
    private LocalDateTime kycCompletedAt;

    /**
     * KYC level (BASIC, INTERMEDIATE, ADVANCED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", length = 20)
    private KycLevel kycLevel;

    /**
     * Whether two-factor authentication is enabled.
     */
    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    /**
     * Two-factor authentication secret.
     */
    @Column(name = "two_factor_secret", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String twoFactorSecret;

    /**
     * User's risk profile.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", length = 20)
    private RiskProfile riskProfile;

    /**
     * Whether user is a professional investor.
     */
    @Column(name = "professional_investor", nullable = false)
    @Builder.Default
    private Boolean professionalInvestor = false;

    /**
     * User's investment experience level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "investment_experience", length = 20)
    private InvestmentExperience investmentExperience;

    /**
     * User's annual income range.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "annual_income", length = 20)
    private IncomeRange annualIncome;

    /**
     * User's net worth range.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "net_worth", length = 20)
    private IncomeRange netWorth;

    /**
     * User's employment status.
     */
    @Column(name = "employment_status", length = 50)
    @Size(max = 50, message = "İstihdam durumu en fazla 50 karakter olabilir")
    private String employmentStatus;

    /**
     * User's occupation.
     */
    @Column(name = "occupation", length = 100)
    @Size(max = 100, message = "Meslek en fazla 100 karakter olabilir")
    private String occupation;

    /**
     * Number of failed login attempts.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Account lock expiry date (if locked).
     */
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    /**
     * Whether user must change password on next login.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    /**
     * Password expiry date.
     */
    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    /**
     * Last successful login date.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Last login IP address.
     */
    @Column(name = "last_login_ip", length = 45)
    @Size(max = 45, message = "IP adresi en fazla 45 karakter olabilir")
    private String lastLoginIp;

    /**
     * Terms and conditions acceptance date.
     */
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    /**
     * Privacy policy acceptance date.
     */
    @Column(name = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;

    /**
     * Marketing communications consent.
     */
    @Column(name = "marketing_consent", nullable = false)
    @Builder.Default
    private Boolean marketingConsent = false;

    /**
     * Marketing consent date.
     */
    @Column(name = "marketing_consent_at")
    private LocalDateTime marketingConsentAt;

    /**
     * Additional user preferences and settings (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private Map<String, Object> preferences;

    /**
     * Additional metadata (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Record creation timestamp.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last modification timestamp.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Record deletion timestamp (soft delete).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User status enumeration.
     */
    public enum UserStatus {
        PENDING,     // Waiting for email verification
        ACTIVE,      // Active user
        SUSPENDED,   // Temporarily suspended
        INACTIVE,    // Deactivated by user
        BANNED,      // Permanently banned
        CLOSED       // Account closed
    }

    /**
     * KYC level enumeration.
     */
    public enum KycLevel {
        NONE,        // No KYC completed
        BASIC,       // Basic identity verification
        INTERMEDIATE,// Additional document verification
        ADVANCED     // Full KYC with video call
    }

    /**
     * Risk profile enumeration.
     */
    public enum RiskProfile {
        CONSERVATIVE,  // Low risk tolerance
        MODERATE,      // Medium risk tolerance
        AGGRESSIVE     // High risk tolerance
    }

    /**
     * Investment experience enumeration.
     */
    public enum InvestmentExperience {
        BEGINNER,      // 0-1 years
        INTERMEDIATE,  // 1-5 years
        ADVANCED,      // 5-10 years
        EXPERT         // 10+ years
    }

    /**
     * Income range enumeration.
     */
    public enum IncomeRange {
        RANGE_0_50K,      // 0-50,000 TL
        RANGE_50K_100K,   // 50,000-100,000 TL
        RANGE_100K_250K,  // 100,000-250,000 TL
        RANGE_250K_500K,  // 250,000-500,000 TL
        RANGE_500K_1M,    // 500,000-1,000,000 TL
        RANGE_1M_PLUS     // 1,000,000+ TL
    }

    /**
     * Gets user's full name.
     *
     * @return Full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return email;
        }
    }

    /**
     * Checks if user account is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status) && deletedAt == null;
    }

    /**
     * Checks if user account is locked.
     *
     * @return true if locked
     */
    public boolean isLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Checks if user is fully verified.
     *
     * @return true if email, phone and KYC are completed
     */
    public boolean isFullyVerified() {
        return Boolean.TRUE.equals(emailVerified) &&
               Boolean.TRUE.equals(phoneVerified) &&
               Boolean.TRUE.equals(kycCompleted);
    }

    /**
     * Checks if user can trade.
     *
     * @return true if user can perform trading operations
     */
    public boolean canTrade() {
        return isActive() && isFullyVerified() && !isLocked() &&
               !Boolean.TRUE.equals(mustChangePassword);
    }

    /**
     * Gets user's age from birth date.
     *
     * @return Age in years, null if birth date not set
     */
    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        return LocalDateTime.now().getYear() - birthDate.getYear();
    }

    /**
     * Checks if user is adult (18+ years old).
     *
     * @return true if adult
     */
    public boolean isAdult() {
        Integer age = getAge();
        return age != null && age >= 18;
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void prePersist() {
        if (status == null) {
            status = UserStatus.PENDING;
        }
        if (preferredLanguage == null) {
            preferredLanguage = "tr";
        }
        if (timezone == null) {
            timezone = "Europe/Istanbul";
        }
        if (riskProfile == null) {
            riskProfile = RiskProfile.MODERATE;
        }
        if (investmentExperience == null) {
            investmentExperience = InvestmentExperience.BEGINNER;
        }
    }
}