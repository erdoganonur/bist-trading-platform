package com.bisttrading.user.entity;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User entity for BIST Trading Platform
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

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

    @Column(name = "national_id", unique = true, length = 20)
    private String nationalId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "is_phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "phone_verification_code")
    private String phoneVerificationCode;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_attempts", nullable = false)
    private int loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType = UserType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    // Enums
    public enum UserType {
        INDIVIDUAL, CORPORATE, ADMIN
    }

    public enum UserStatus {
        PENDING, ACTIVE, SUSPENDED, DEACTIVATED
    }

    // Business methods
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