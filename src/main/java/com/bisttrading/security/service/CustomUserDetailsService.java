package com.bisttrading.security.service;

import com.bisttrading.entity.UserEntity;
import com.bisttrading.repository.UserRepository;
import com.bisttrading.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Custom UserDetailsService implementation for BIST Trading Platform.
 * Loads user details for authentication and authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user by username (email or username).
     * This is used by Spring Security for authentication.
     *
     * @param username Username or email
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Kullanıcı yükleniyor - username: {}", username);

        // TODO: Replace with actual database lookup
        CustomUserDetails userDetails = this.findUserByUsername(username);

        if (userDetails == null) {
            log.warn("Kullanıcı bulunamadı - username: {}", username);
            throw new UsernameNotFoundException("Kullanıcı bulunamadı: " + username);
        }

        log.debug("Kullanıcı başarıyla yüklendi - userId: {}, active: {}",
            userDetails.getUserId(), userDetails.isActive());

        return userDetails;
    }

    /**
     * Loads user by user ID.
     * This is used by JWT authentication filter.
     *
     * @param userId User ID
     * @return CustomUserDetails
     * @throws UsernameNotFoundException if user not found
     */
    public CustomUserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
        log.debug("Kullanıcı ID ile yükleniyor - userId: {}", userId);

        // TODO: Replace with actual database lookup
        CustomUserDetails userDetails = findUserById(userId);

        if (userDetails == null) {
            log.warn("Kullanıcı bulunamadı - userId: {}", userId);
            throw new UsernameNotFoundException("Kullanıcı bulunamadı: " + userId);
        }

        log.debug("Kullanıcı başarıyla yüklendi - userId: {}, active: {}",
            userDetails.getUserId(), userDetails.isActive());

        return userDetails;
    }

    /**
     * Checks if user exists by username.
     *
     * @param username Username or email
     * @return true if user exists
     */
    public boolean existsByUsername(String username) {
        // TODO: Replace with actual database lookup
        return findUserByUsername(username) != null;
    }

    /**
     * Checks if user exists by email.
     *
     * @param email Email address
     * @return true if user exists
     */
    public boolean existsByEmail(String email) {
        // TODO: Replace with actual database lookup
        return findUserByEmail(email) != null;
    }

    /**
     * Checks if user exists by TC Kimlik number.
     *
     * @param tcKimlik TC Kimlik number
     * @return true if user exists
     */
    public boolean existsByTcKimlik(String tcKimlik) {
        // TODO: Replace with actual database lookup
        return findUserByTcKimlik(tcKimlik) != null;
    }

    /**
     * Updates user's last login date.
     *
     * @param userId User ID
     * @param loginDate Login date
     */
    public void updateLastLoginDate(String userId, LocalDateTime loginDate) {
        log.debug("Son login tarihi güncelleniyor - userId: {}, date: {}", userId, loginDate);
        // TODO: Implement database update
    }

    /**
     * Increments failed login attempts for user.
     *
     * @param username Username or email
     */
    public void incrementFailedLoginAttempts(String username) {
        log.debug("Başarısız login denemesi sayısı artırılıyor - username: {}", username);
        // TODO: Implement database update
    }

    /**
     * Resets failed login attempts for user.
     *
     * @param username Username or email
     */
    public void resetFailedLoginAttempts(String username) {
        log.debug("Başarısız login denemesi sayısı sıfırlanıyor - username: {}", username);
        // TODO: Implement database update
    }

    /**
     * Locks user account due to failed login attempts.
     *
     * @param username Username or email
     * @param lockUntil Lock expiry date
     */
    public void lockUserAccount(String username, LocalDateTime lockUntil) {
        log.warn("Kullanıcı hesabı kilitlendi - username: {}, lockUntil: {}", username, lockUntil);
        // TODO: Implement database update
    }

    /**
     * Unlocks user account.
     *
     * @param username Username or email
     */
    public void unlockUserAccount(String username) {
        log.info("Kullanıcı hesabı kilidi açıldı - username: {}", username);
        // TODO: Implement database update
    }

    // TODO: Replace these mock methods with actual database queries

    /**
     * Mock method to find user by username.
     * This should be replaced with actual database lookup.
     *
     * @param username Username or email
     * @return CustomUserDetails or null
     */
    private CustomUserDetails findUserByUsername(String username) {
        try {
            // Use repository to find user by email or username
            Optional<UserEntity> userEntityOpt = userRepository.findByEmail(username);

            if (userEntityOpt.isEmpty()) {
                log.debug("Kullanıcı bulunamadı - username/email: {}", username);
                return null;
            }

            UserEntity userEntity = userEntityOpt.get();

            // Check if user is active and not deleted
//            if (userEntity.getStatus() != UserEntity.UserStatus.ACTIVE || userEntity.getDeletedAt() != null) {
//                log.debug("Kullanıcı aktif değil veya silinmiş - userId: {}, status: {}",
//                    userEntity.getId(), userEntity.getStatus());
//                return null;
//            }

            // Check if account is locked
            if (isAccountLocked(userEntity)) {
                log.debug("Kullanıcı hesabı kilitli - userId: {}", userEntity.getId());
                return null;
            }

            return mapToCustomUserDetails(userEntity);

        } catch (Exception e) {
            log.error("Kullanıcı arama hatası - username: {}, error: {}", username, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Maps UserEntity to CustomUserDetails.
     *
     * @param userEntity UserEntity from database
     * @return CustomUserDetails
     */
    private CustomUserDetails mapToCustomUserDetails(UserEntity userEntity) {
        return CustomUserDetails.builder()
            .userId(userEntity.getId())
            .email(userEntity.getEmail())
            .username(userEntity.getUsername())
            .password(userEntity.getPasswordHash())
            .firstName(userEntity.getFirstName())
            .lastName(userEntity.getLastName())
            .tcKimlik(userEntity.getTcKimlikNo())
            .phoneNumber(userEntity.getPhoneNumber())
            .birthDate(userEntity.getBirthDate() != null ? userEntity.getBirthDate().atStartOfDay() : null)
            .roles(determineUserRoles(userEntity))
            .permissions(determineUserPermissions(userEntity))
            .active(userEntity.getStatus() == UserEntity.UserStatus.ACTIVE)
            .emailVerified(userEntity.isEmailVerified())
            .phoneVerified(userEntity.isPhoneVerified())
            .kycCompleted(userEntity.getKycCompleted() != null ? userEntity.getKycCompleted() : false)
            .twoFactorEnabled(userEntity.getTwoFactorEnabled() != null ? userEntity.getTwoFactorEnabled() : false)
            .lastLoginDate(userEntity.getLastLoginAt())
            .failedLoginAttempts(userEntity.getFailedLoginAttempts())
            .accountLockExpiry(userEntity.getAccountLockedUntil())
            .preferredLanguage(userEntity.getPreferredLanguage() != null ? userEntity.getPreferredLanguage() : "tr")
            .timezone(userEntity.getTimezone() != null ? userEntity.getTimezone() : "Europe/Istanbul")
            .createdAt(userEntity.getCreatedAt())
            .updatedAt(userEntity.getUpdatedAt())
            .mustChangePassword(userEntity.getMustChangePassword() != null ? userEntity.getMustChangePassword() : false)
            .passwordExpiryDate(userEntity.getPasswordExpiresAt())
            .riskProfile(userEntity.getRiskProfile() != null ? userEntity.getRiskProfile().toString() : "MODERATE")
            .professionalInvestor(userEntity.getProfessionalInvestor() != null ? userEntity.getProfessionalInvestor() : false)
            .investmentExperience(userEntity.getInvestmentExperience() != null ? userEntity.getInvestmentExperience().toString() : "BEGINNER")
            .build();
    }

    /**
     * Checks if user account is locked.
     *
     * @param userEntity UserEntity
     * @return true if account is locked
     */
    private boolean isAccountLocked(UserEntity userEntity) {
        if (userEntity.getAccountLockedUntil() == null) {
            return false;
        }
        return userEntity.getAccountLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Determines user roles based on user type and other factors.
     *
     * @param userEntity UserEntity
     * @return Set of roles
     */
    private Set<String> determineUserRoles(UserEntity userEntity) {
        Set<String> roles = Set.of();

        // UserEntity doesn't have userType, so we determine roles based on other attributes
        // For now, we'll assign roles based on professional investor status and other criteria

        // Check if this is an admin based on email domain or special attributes
        if (userEntity.getEmail() != null &&
            (userEntity.getEmail().contains("admin") || userEntity.getEmail().endsWith("@bist.com.tr"))) {
            roles = Set.of("ADMIN", "SUPER_ADMIN");
        }
        // Check if professional investor -> TRADER
        else if (userEntity.getProfessionalInvestor() != null && userEntity.getProfessionalInvestor()) {
            roles = Set.of("TRADER", "PROFESSIONAL_TRADER");
        }
        // Regular customer
        else {
            roles = Set.of("CUSTOMER", "RETAIL_CUSTOMER");
        }

        return roles;
    }

    /**
     * Determines user permissions based on roles and user type.
     *
     * @param userEntity UserEntity
     * @return Set of permissions
     */
    private Set<String> determineUserPermissions(UserEntity userEntity) {
        Set<String> permissions = Set.of();

        // Determine permissions based on email and professional investor status
        // Check if this is an admin based on email domain
        if (userEntity.getEmail() != null &&
            (userEntity.getEmail().contains("admin") || userEntity.getEmail().endsWith("@bist.com.tr"))) {
            permissions = Set.of("USER_MANAGEMENT", "SYSTEM_ADMIN", "TRADING_ADMIN", "MARKET_DATA", "PORTFOLIO_MANAGEMENT");
        }
        // Check if professional investor -> advanced permissions
        else if (userEntity.getProfessionalInvestor() != null && userEntity.getProfessionalInvestor()) {
            permissions = Set.of("TRADING", "PORTFOLIO_MANAGEMENT", "MARKET_DATA", "ADVANCED_TRADING");
        }
        // Regular customer permissions
        else {
            permissions = Set.of("TRADING", "PORTFOLIO_VIEW");
        }

        return permissions;
    }

    /**
     * Mock method to find user by ID.
     * This should be replaced with actual database lookup.
     *
     * @param userId User ID
     * @return CustomUserDetails or null
     */
    private CustomUserDetails findUserById(String userId) {
        // Mock implementation for demonstration
        return switch (userId) {
            case "admin-001" -> createMockAdminUser();
            case "trader-001" -> createMockTraderUser();
            case "customer-001" -> createMockCustomerUser();
            default -> null;
        };
    }

    /**
     * Mock method to find user by email.
     * This should be replaced with actual database lookup.
     *
     * @param email Email address
     * @return CustomUserDetails or null
     */
    private CustomUserDetails findUserByEmail(String email) {
        return findUserByUsername(email);
    }

    /**
     * Mock method to find user by TC Kimlik.
     * This should be replaced with actual database lookup.
     *
     * @param tcKimlik TC Kimlik number
     * @return CustomUserDetails or null
     */
    private CustomUserDetails findUserByTcKimlik(String tcKimlik) {
        // Mock implementation for demonstration
        if ("12345678901".equals(tcKimlik)) {
            return createMockAdminUser();
        } else if ("98765432109".equals(tcKimlik)) {
            return createMockTraderUser();
        } else if ("11223344556".equals(tcKimlik)) {
            return createMockCustomerUser();
        }
        return null;
    }

    /**
     * Creates a mock admin user for testing.
     *
     * @return Mock admin user
     */
    private CustomUserDetails createMockAdminUser() {
        return CustomUserDetails.builder()
            .userId("admin-001")
            .email("admin@bist.com.tr")
            .username("admin")
            .password("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi") // password: admin123
            .firstName("Sistem")
            .lastName("Yöneticisi")
            .tcKimlik("12345678901")
            .phoneNumber("+905551234567")
            .birthDate(LocalDateTime.of(1980, 1, 1, 0, 0))
            .roles(Set.of("ADMIN", "SUPER_ADMIN"))
            .permissions(Set.of("USER_MANAGEMENT", "SYSTEM_ADMIN", "TRADING_ADMIN"))
            .active(true)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .twoFactorEnabled(true)
            .lastLoginDate(LocalDateTime.now().minusHours(1))
            .failedLoginAttempts(0)
            .accountLockExpiry(null)
            .preferredLanguage("tr")
            .timezone("Europe/Istanbul")
            .createdAt(LocalDateTime.now().minusYears(1))
            .updatedAt(LocalDateTime.now())
            .mustChangePassword(false)
            .passwordExpiryDate(LocalDateTime.now().plusMonths(6))
            .riskProfile("MODERATE")
            .professionalInvestor(false)
            .investmentExperience("EXPERT")
            .build();
    }

    /**
     * Creates a mock trader user for testing.
     *
     * @return Mock trader user
     */
    private CustomUserDetails createMockTraderUser() {
        return CustomUserDetails.builder()
            .userId("trader-001")
            .email("trader@bist.com.tr")
            .username("trader")
            .password("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi") // password: trader123
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .tcKimlik("98765432109")
            .phoneNumber("+905559876543")
            .birthDate(LocalDateTime.of(1985, 5, 15, 0, 0))
            .roles(Set.of("TRADER", "PROFESSIONAL_TRADER"))
            .permissions(Set.of("TRADING", "PORTFOLIO_MANAGEMENT", "MARKET_DATA"))
            .active(true)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .twoFactorEnabled(true)
            .lastLoginDate(LocalDateTime.now().minusMinutes(30))
            .failedLoginAttempts(0)
            .accountLockExpiry(null)
            .preferredLanguage("tr")
            .timezone("Europe/Istanbul")
            .createdAt(LocalDateTime.now().minusMonths(6))
            .updatedAt(LocalDateTime.now())
            .mustChangePassword(false)
            .passwordExpiryDate(LocalDateTime.now().plusMonths(3))
            .riskProfile("AGGRESSIVE")
            .professionalInvestor(true)
            .investmentExperience("EXPERT")
            .build();
    }

    /**
     * Creates a mock customer user for testing.
     *
     * @return Mock customer user
     */
    private CustomUserDetails createMockCustomerUser() {
        return CustomUserDetails.builder()
            .userId("customer-001")
            .email("customer@bist.com.tr")
            .username("customer")
            .password("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfRNi9nOHn9OHwi") // password: customer123
            .firstName("Ayşe")
            .lastName("Demir")
            .tcKimlik("11223344556")
            .phoneNumber("+905551122334")
            .birthDate(LocalDateTime.of(1990, 8, 20, 0, 0))
            .roles(Set.of("CUSTOMER", "RETAIL_CUSTOMER"))
            .permissions(Set.of("TRADING", "PORTFOLIO_VIEW"))
            .active(true)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .twoFactorEnabled(false)
            .lastLoginDate(LocalDateTime.now().minusHours(2))
            .failedLoginAttempts(0)
            .accountLockExpiry(null)
            .preferredLanguage("tr")
            .timezone("Europe/Istanbul")
            .createdAt(LocalDateTime.now().minusMonths(3))
            .updatedAt(LocalDateTime.now())
            .mustChangePassword(false)
            .passwordExpiryDate(LocalDateTime.now().plusMonths(6))
            .riskProfile("CONSERVATIVE")
            .professionalInvestor(false)
            .investmentExperience("BEGINNER")
            .build();
    }
}