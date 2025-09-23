package com.bisttrading.core.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Custom UserDetailsService implementation for BIST Trading Platform.
 * Loads user details for authentication and authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // TODO: Inject actual user repository when user management module is implemented
    // private final UserRepository userRepository;

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
        CustomUserDetails userDetails = findUserByUsername(username);

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
        // Mock implementation for demonstration
        if ("admin@bist.com.tr".equals(username) || "admin".equals(username)) {
            return createMockAdminUser();
        } else if ("trader@bist.com.tr".equals(username) || "trader".equals(username)) {
            return createMockTraderUser();
        } else if ("customer@bist.com.tr".equals(username) || "customer".equals(username)) {
            return createMockCustomerUser();
        }
        return null;
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