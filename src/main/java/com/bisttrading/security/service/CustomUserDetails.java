package com.bisttrading.security.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom implementation of Spring Security UserDetails for BIST Trading Platform.
 * Contains user information and security-related data.
 */
@Data
@Builder
@EqualsAndHashCode(of = "userId")
public class CustomUserDetails implements UserDetails {

    /**
     * Unique user identifier.
     */
    private String userId;

    /**
     * User's email address (used as username).
     */
    private String email;

    /**
     * User's username (alternative login).
     */
    private String username;

    /**
     * User's encrypted password.
     */
    @JsonIgnore
    private String password;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's Turkish identity number (TC Kimlik).
     */
    @JsonIgnore
    private String tcKimlik;

    /**
     * User's phone number.
     */
    private String phoneNumber;

    /**
     * User's birth date.
     */
    private LocalDateTime birthDate;

    /**
     * User's roles and authorities.
     */
    private Set<String> roles;

    /**
     * User's permissions.
     */
    private Set<String> permissions;

    /**
     * Whether the user account is active.
     */
    private boolean active;

    /**
     * Whether the user's email is verified.
     */
    private boolean emailVerified;

    /**
     * Whether the user's phone is verified.
     */
    private boolean phoneVerified;

    /**
     * Whether the user's KYC (Know Your Customer) is completed.
     */
    private boolean kycCompleted;

    /**
     * Whether two-factor authentication is enabled.
     */
    private boolean twoFactorEnabled;

    /**
     * User's last login date.
     */
    private LocalDateTime lastLoginDate;

    /**
     * Number of failed login attempts.
     */
    private int failedLoginAttempts;

    /**
     * Account lock expiry date (if locked).
     */
    private LocalDateTime accountLockExpiry;

    /**
     * User's preferred language.
     */
    private String preferredLanguage;

    /**
     * User's timezone.
     */
    private String timezone;

    /**
     * User creation date.
     */
    private LocalDateTime createdAt;

    /**
     * User last update date.
     */
    private LocalDateTime updatedAt;

    /**
     * Whether the user must change password on next login.
     */
    private boolean mustChangePassword;

    /**
     * Password expiry date.
     */
    private LocalDateTime passwordExpiryDate;

    /**
     * User's risk profile (CONSERVATIVE, MODERATE, AGGRESSIVE).
     */
    private String riskProfile;

    /**
     * Whether the user is a professional investor.
     */
    private boolean professionalInvestor;

    /**
     * User's investment experience level.
     */
    private String investmentExperience;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());

        // Add permissions as authorities
        if (permissions != null) {
            permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Use email as username if username is not set
        return username != null ? username : email;
    }

    @Override
    public boolean isAccountNonExpired() {
        // Account never expires for now
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Check if account is locked due to failed login attempts
        return accountLockExpiry == null || accountLockExpiry.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Check if password has expired
        return passwordExpiryDate == null || passwordExpiryDate.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return active;
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
     * Checks if user has a specific role.
     *
     * @param role Role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Checks if user has a specific permission.
     *
     * @param permission Permission to check
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Checks if user has any of the specified roles.
     *
     * @param rolesToCheck Roles to check
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        if (roles == null || rolesToCheck == null) {
            return false;
        }
        return List.of(rolesToCheck).stream().anyMatch(roles::contains);
    }

    /**
     * Checks if user has all of the specified roles.
     *
     * @param rolesToCheck Roles to check
     * @return true if user has all roles
     */
    public boolean hasAllRoles(String... rolesToCheck) {
        if (roles == null || rolesToCheck == null) {
            return false;
        }
        return roles.containsAll(List.of(rolesToCheck));
    }

    /**
     * Checks if user is an administrator.
     *
     * @return true if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    /**
     * Checks if user is a trader.
     *
     * @return true if user is trader
     */
    public boolean isTrader() {
        return hasRole("TRADER") || hasRole("PROFESSIONAL_TRADER");
    }

    /**
     * Checks if user is a customer.
     *
     * @return true if user is customer
     */
    public boolean isCustomer() {
        return hasRole("CUSTOMER") || hasRole("RETAIL_CUSTOMER");
    }

    /**
     * Checks if user account is fully verified.
     *
     * @return true if fully verified
     */
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified && kycCompleted;
    }

    /**
     * Checks if user can trade.
     *
     * @return true if user can trade
     */
    public boolean canTrade() {
        return isFullyVerified() && active && isAccountNonLocked() &&
               (isTrader() || isCustomer()) && !mustChangePassword;
    }

    /**
     * Checks if user needs to complete KYC.
     *
     * @return true if KYC is needed
     */
    public boolean needsKyc() {
        return !kycCompleted && active;
    }

    /**
     * Checks if user needs to verify email.
     *
     * @return true if email verification is needed
     */
    public boolean needsEmailVerification() {
        return !emailVerified && active;
    }

    /**
     * Checks if user needs to verify phone.
     *
     * @return true if phone verification is needed
     */
    public boolean needsPhoneVerification() {
        return !phoneVerified && active;
    }

    /**
     * Gets display name for UI.
     *
     * @return Display name
     */
    public String getDisplayName() {
        String fullName = getFullName();
        return !fullName.equals(email) ? fullName : username != null ? username : email;
    }

    /**
     * Gets user's initials.
     *
     * @return User initials
     */
    public String getInitials() {
        StringBuilder initials = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }

        if (initials.length() == 0) {
            String name = getDisplayName();
            if (name != null && !name.isEmpty()) {
                initials.append(name.charAt(0));
            }
        }

        return initials.toString().toUpperCase();
    }

    /**
     * Gets user's age.
     *
     * @return Age in years
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
     * Checks if user is senior (65+ years old).
     *
     * @return true if senior
     */
    public boolean isSenior() {
        Integer age = getAge();
        return age != null && age >= 65;
    }

    /**
     * Gets risk profile as enum-like string.
     *
     * @return Risk profile
     */
    public String getRiskProfileDisplay() {
        return switch (riskProfile != null ? riskProfile.toUpperCase() : "MODERATE") {
            case "CONSERVATIVE" -> "Muhafazakâr";
            case "MODERATE" -> "Dengeli";
            case "AGGRESSIVE" -> "Agresif";
            default -> "Dengeli";
        };
    }

    /**
     * Converts to a simplified DTO for token claims.
     *
     * @return Simplified user info
     */
    public UserTokenInfo toTokenInfo() {
        return UserTokenInfo.builder()
            .userId(userId)
            .email(email)
            .username(username)
            .fullName(getFullName())
            .roles(roles)
            .active(active)
            .build();
    }

    /**
     * Simplified user info for token claims.
     */
    @Data
    @Builder
    public static class UserTokenInfo {
        private String userId;
        private String email;
        private String username;
        private String fullName;
        private Set<String> roles;
        private boolean active;
    }
}