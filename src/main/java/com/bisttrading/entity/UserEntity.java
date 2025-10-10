package com.bisttrading.entity;

import com.bisttrading.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Id
    private String id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "tc_kimlik_no", unique = true, length = 11)
    private String tcKimlikNo;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "authorities")
    private String authorities;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "is_phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_attempts", nullable = false)
    private int loginAttempts = 0;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "organization_id", insertable = false, updatable = false)
    private String organizationId;

    @Column(name = "kyc_completed", nullable = false)
    private Boolean kycCompleted = false;

    @Column(name = "kyc_completed_at")
    private LocalDateTime kycCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level")
    private KycLevel kycLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile")
    private RiskProfile riskProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_experience")
    private InvestmentExperience investmentExperience;

    @Column(name = "professional_investor", nullable = false)
    private Boolean professionalInvestor = false;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "tr";

    @Column(name = "timezone", length = 50)
    private String timezone = "Europe/Istanbul";

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType = UserType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    public enum UserType {
        INDIVIDUAL, CORPORATE, ADMIN
    }

    public enum UserStatus {
        PENDING, ACTIVE, SUSPENDED, DEACTIVATED, INACTIVE, CLOSED
    }

    public enum KycLevel {
        BASIC, STANDARD, PREMIUM
    }

    public enum RiskProfile {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }

    public enum InvestmentExperience {
        BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isVerified() {
        return emailVerified && phoneVerified;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void incrementLoginAttempts() {
        this.loginAttempts++;
        if (this.loginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lockedUntil = null;
    }
}