package com.bisttrading.infrastructure.persistence.repository;

import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserEntity operations.
 * Provides custom query methods for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Finds user by email address.
     *
     * @param email Email address
     * @return Optional UserEntity
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Finds user by username.
     *
     * @param username Username
     * @return Optional UserEntity
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Finds user by email or username.
     *
     * @param email Email address
     * @param username Username
     * @return Optional UserEntity
     */
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email OR u.username = :username")
    Optional<UserEntity> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);

    /**
     * Checks if user exists by email.
     *
     * @param email Email address
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if user exists by username.
     *
     * @param username Username
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks if user exists by TC Kimlik number (encrypted field).
     * Note: This requires the encrypted value for comparison.
     *
     * @param tcKimlikNo Encrypted TC Kimlik number
     * @return true if exists
     */
    boolean existsByTcKimlikNo(String tcKimlikNo);

    /**
     * Finds all users by organization ID.
     *
     * @param organizationId Organization ID
     * @return List of users
     */
    List<UserEntity> findByOrganizationId(String organizationId);

    /**
     * Finds all users by status.
     *
     * @param status User status
     * @return List of users
     */
    List<UserEntity> findByStatus(UserEntity.UserStatus status);

    /**
     * Finds all active users (not deleted).
     *
     * @return List of active users
     */
    @Query("SELECT u FROM UserEntity u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    List<UserEntity> findAllActive();

    /**
     * Finds all users with pending email verification.
     *
     * @return List of users with unverified emails
     */
    @Query("SELECT u FROM UserEntity u WHERE u.emailVerified = false AND u.status = 'PENDING'")
    List<UserEntity> findUsersWithPendingEmailVerification();

    /**
     * Finds all users with incomplete KYC.
     *
     * @return List of users with incomplete KYC
     */
    @Query("SELECT u FROM UserEntity u WHERE u.kycCompleted = false AND u.status = 'ACTIVE'")
    List<UserEntity> findUsersWithIncompleteKyc();

    /**
     * Finds users with expired passwords.
     *
     * @param now Current timestamp
     * @return List of users with expired passwords
     */
    @Query("SELECT u FROM UserEntity u WHERE u.passwordExpiresAt < :now AND u.status = 'ACTIVE'")
    List<UserEntity> findUsersWithExpiredPasswords(@Param("now") LocalDateTime now);

    /**
     * Finds users with locked accounts.
     *
     * @param now Current timestamp
     * @return List of users with locked accounts
     */
    @Query("SELECT u FROM UserEntity u WHERE u.accountLockedUntil > :now")
    List<UserEntity> findUsersWithLockedAccounts(@Param("now") LocalDateTime now);

    /**
     * Finds users who haven't logged in for specified days.
     *
     * @param cutoffDate Cutoff date for last login
     * @return List of inactive users
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastLoginAt < :cutoffDate OR u.lastLoginAt IS NULL")
    List<UserEntity> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds users by risk profile.
     *
     * @param riskProfile Risk profile
     * @return List of users with specified risk profile
     */
    List<UserEntity> findByRiskProfile(UserEntity.RiskProfile riskProfile);

    /**
     * Finds professional investors.
     *
     * @return List of professional investors
     */
    @Query("SELECT u FROM UserEntity u WHERE u.professionalInvestor = true AND u.status = 'ACTIVE'")
    List<UserEntity> findProfessionalInvestors();

    /**
     * Finds users by KYC level.
     *
     * @param kycLevel KYC level
     * @return List of users with specified KYC level
     */
    List<UserEntity> findByKycLevel(UserEntity.KycLevel kycLevel);

    /**
     * Finds users created within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of users created in date range
     */
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<UserEntity> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Finds users with two-factor authentication enabled.
     *
     * @return List of users with 2FA enabled
     */
    @Query("SELECT u FROM UserEntity u WHERE u.twoFactorEnabled = true")
    List<UserEntity> findUsersWithTwoFactorEnabled();

    /**
     * Counts users by organization.
     *
     * @param organizationId Organization ID
     * @return Number of users in organization
     */
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.organizationId = :organizationId AND u.deletedAt IS NULL")
    long countByOrganizationId(@Param("organizationId") String organizationId);

    /**
     * Counts active users.
     *
     * @return Number of active users
     */
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    long countActiveUsers();

    /**
     * Counts users by status.
     *
     * @param status User status
     * @return Number of users with specified status
     */
    long countByStatus(UserEntity.UserStatus status);

    /**
     * Finds users with failed login attempts above threshold.
     *
     * @param threshold Failed login attempts threshold
     * @return List of users with high failed login attempts
     */
    @Query("SELECT u FROM UserEntity u WHERE u.failedLoginAttempts >= :threshold")
    List<UserEntity> findUsersWithHighFailedLoginAttempts(@Param("threshold") int threshold);

    /**
     * Updates last login information for user.
     *
     * @param userId User ID
     * @param lastLoginAt Last login timestamp
     * @param lastLoginIp Last login IP address
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :lastLoginIp " +
           "WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") String userId,
                       @Param("lastLoginAt") LocalDateTime lastLoginAt,
                       @Param("lastLoginIp") String lastLoginIp);

    /**
     * Resets failed login attempts for user.
     *
     * @param userId User ID
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = 0, u.accountLockedUntil = NULL " +
           "WHERE u.id = :userId")
    int resetFailedLoginAttempts(@Param("userId") String userId);

    /**
     * Increments failed login attempts for user.
     *
     * @param userId User ID
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 " +
           "WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") String userId);

    /**
     * Locks user account until specified time.
     *
     * @param userId User ID
     * @param lockUntil Lock expiry time
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.accountLockedUntil = :lockUntil " +
           "WHERE u.id = :userId")
    int lockUserAccount(@Param("userId") String userId,
                       @Param("lockUntil") LocalDateTime lockUntil);

    /**
     * Updates user email verification status.
     *
     * @param userId User ID
     * @param verified Verification status
     * @param verifiedAt Verification timestamp
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.emailVerified = :verified, u.emailVerifiedAt = :verifiedAt " +
           "WHERE u.id = :userId")
    int updateEmailVerification(@Param("userId") String userId,
                               @Param("verified") boolean verified,
                               @Param("verifiedAt") LocalDateTime verifiedAt);

    /**
     * Updates user phone verification status.
     *
     * @param userId User ID
     * @param verified Verification status
     * @param verifiedAt Verification timestamp
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.phoneVerified = :verified, u.phoneVerifiedAt = :verifiedAt " +
           "WHERE u.id = :userId")
    int updatePhoneVerification(@Param("userId") String userId,
                               @Param("verified") boolean verified,
                               @Param("verifiedAt") LocalDateTime verifiedAt);

    /**
     * Updates user KYC status.
     *
     * @param userId User ID
     * @param completed KYC completion status
     * @param completedAt KYC completion timestamp
     * @param kycLevel KYC level achieved
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.kycCompleted = :completed, u.kycCompletedAt = :completedAt, " +
           "u.kycLevel = :kycLevel WHERE u.id = :userId")
    int updateKycStatus(@Param("userId") String userId,
                       @Param("completed") boolean completed,
                       @Param("completedAt") LocalDateTime completedAt,
                       @Param("kycLevel") UserEntity.KycLevel kycLevel);

    /**
     * Soft deletes user by setting deleted timestamp.
     *
     * @param userId User ID
     * @param deletedAt Deletion timestamp
     * @return Number of updated records
     */
    @Query("UPDATE UserEntity u SET u.deletedAt = :deletedAt, u.status = 'CLOSED' " +
           "WHERE u.id = :userId")
    int softDeleteUser(@Param("userId") String userId,
                      @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Searches users by partial name or email match.
     *
     * @param searchTerm Search term
     * @return List of matching users
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "u.deletedAt IS NULL")
    List<UserEntity> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Finds users requiring password change.
     *
     * @return List of users who must change password
     */
    @Query("SELECT u FROM UserEntity u WHERE u.mustChangePassword = true AND u.status = 'ACTIVE'")
    List<UserEntity> findUsersRequiringPasswordChange();

    /**
     * Finds users with marketing consent.
     *
     * @return List of users who consented to marketing
     */
    @Query("SELECT u FROM UserEntity u WHERE u.marketingConsent = true AND u.status = 'ACTIVE'")
    List<UserEntity> findUsersWithMarketingConsent();

    /**
     * Checks if phone number exists for a different user.
     *
     * @param phoneNumber Phone number
     * @param userId User ID to exclude
     * @return true if exists for another user
     */
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, String userId);
}