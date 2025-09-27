package com.bisttrading.infrastructure.persistence.repository;

import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Enhanced integration tests for UserRepository using TestContainers.
 * Tests repository methods with real PostgreSQL database, Turkish character handling,
 * and concurrent access scenarios.
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("User Repository Integration Tests")
class UserRepositoryTest {

    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("bist_trading_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private UserEntity turkishUser;
    private UserEntity inactiveUser;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test users
        testUser = TestDataBuilder.validUser().build();
        turkishUser = TestDataBuilder.turkishUser().build();
        inactiveUser = TestDataBuilder.inactiveUser().build();

        entityManager.persist(testUser);
        entityManager.persist(turkishUser);
        entityManager.persist(inactiveUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by email successfully")
    void shouldFindUserByEmailSuccessfully() {
        // When
        Optional<UserEntity> found = userRepository.findByEmail(testUser.getEmail());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(found.get().getFirstName()).isEqualTo(testUser.getFirstName());
    }

    @Test
    @DisplayName("Should find Turkish user by email with special characters")
    void shouldFindTurkishUserByEmailWithSpecialCharacters() {
        // When
        Optional<UserEntity> found = userRepository.findByEmail(turkishUser.getEmail());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(turkishUser.getEmail());
        assertThat(found.get().getFirstName()).isEqualTo(turkishUser.getFirstName());
        assertThat(found.get().getLastName()).isEqualTo(turkishUser.getLastName());
    }

    @ParameterizedTest
    @DisplayName("Should find users with various Turkish names")
    @ValueSource(strings = {
        "Çağlar",
        "Gülşah",
        "Ömer",
        "Şeyma",
        "İbrahim"
    })
    void shouldFindUsersWithVariousTurkishNames(String firstName) {
        // Given
        UserEntity turkishNameUser = TestDataBuilder.validUser()
            .firstName(firstName)
            .build();
        entityManager.persist(turkishNameUser);
        entityManager.flush();

        // When
        List<UserEntity> searchResults = userRepository.searchUsers(firstName);

        // Then
        assertThat(searchResults).isNotEmpty();
        assertThat(searchResults).anyMatch(user -> user.getFirstName().equals(firstName));
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void shouldFindUserByUsernameSuccessfully() {
        // When
        Optional<UserEntity> found = userRepository.findByUsername(testUser.getUsername());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(testUser.getUsername());
        assertThat(found.get().getLastName()).isEqualTo(testUser.getLastName());
    }

    @Test
    @DisplayName("Should find user by email or username")
    void shouldFindUserByEmailOrUsername() {
        // When
        Optional<UserEntity> foundByEmail = userRepository.findByEmailOrUsername(
            testUser.getEmail(), "nonexistent"
        );
        Optional<UserEntity> foundByUsername = userRepository.findByEmailOrUsername(
            "nonexistent@example.com", testUser.getUsername()
        );

        // Then
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getEmail()).isEqualTo(testUser.getEmail());

        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfUserExistsByEmail() {
        // When
        boolean exists = userRepository.existsByEmail(testUser.getEmail());
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if Turkish user exists by email")
    void shouldCheckIfTurkishUserExistsByEmail() {
        // When
        boolean exists = userRepository.existsByEmail(turkishUser.getEmail());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should check if user exists by username")
    void shouldCheckIfUserExistsByUsername() {
        // When
        boolean exists = userRepository.existsByUsername(testUser.getUsername());
        boolean notExists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find users by status")
    void shouldFindUsersByStatus() {
        // When
        List<UserEntity> activeUsers = userRepository.findByStatus(UserEntity.UserStatus.ACTIVE);
        List<UserEntity> inactiveUsers = userRepository.findByStatus(UserEntity.UserStatus.INACTIVE);

        // Then
        assertThat(activeUsers).hasSize(2); // testUser and turkishUser
        assertThat(inactiveUsers).hasSize(1); // inactiveUser

        assertThat(activeUsers).extracting(UserEntity::getStatus)
            .containsOnly(UserEntity.UserStatus.ACTIVE);
        assertThat(inactiveUsers).extracting(UserEntity::getStatus)
            .containsOnly(UserEntity.UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should find all active users")
    void shouldFindAllActiveUsers() {
        // When
        List<UserEntity> activeUsers = userRepository.findAllActive();

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).allMatch(UserEntity::isActive);
    }

    @Test
    @DisplayName("Should find users with pending email verification")
    void shouldFindUsersWithPendingEmailVerification() {
        // When
        List<UserEntity> pendingVerification = userRepository.findUsersWithPendingEmailVerification();

        // Then
        assertThat(pendingVerification).hasSize(1); // inactiveUser
        assertThat(pendingVerification).allMatch(user -> !user.getEmailVerified());
        assertThat(pendingVerification).allMatch(user ->
            user.getStatus() != UserEntity.UserStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("Should find users with incomplete KYC")
    void shouldFindUsersWithIncompleteKyc() {
        // When
        List<UserEntity> incompleteKyc = userRepository.findUsersWithIncompleteKyc();

        // Then
        assertThat(incompleteKyc).isEmpty(); // All active users have completed KYC
    }

    @Test
    @DisplayName("Should find professional investors")
    void shouldFindProfessionalInvestors() {
        // Given - Add a professional investor
        UserEntity professionalInvestor = TestDataBuilder.professionalInvestor().build();
        entityManager.persist(professionalInvestor);
        entityManager.flush();

        // When
        List<UserEntity> professionals = userRepository.findProfessionalInvestors();

        // Then
        assertThat(professionals).hasSize(1);
        assertThat(professionals.get(0).isProfessionalInvestor()).isTrue();
        assertThat(professionals.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("Should find users by risk profile")
    void shouldFindUsersByRiskProfile() {
        // When
        List<UserEntity> moderateRisk = userRepository.findByRiskProfile(UserEntity.RiskProfile.MODERATE);
        List<UserEntity> conservativeRisk = userRepository.findByRiskProfile(UserEntity.RiskProfile.CONSERVATIVE);

        // Then
        assertThat(moderateRisk).hasSize(2); // testUser and turkishUser
        assertThat(conservativeRisk).hasSize(1); // inactiveUser
    }

    @Test
    @DisplayName("Should count active users")
    void shouldCountActiveUsers() {
        // When
        long activeCount = userRepository.countActiveUsers();

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count users by status")
    void shouldCountUsersByStatus() {
        // When
        long activeCount = userRepository.countByStatus(UserEntity.UserStatus.ACTIVE);
        long inactiveCount = userRepository.countByStatus(UserEntity.UserStatus.INACTIVE);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search users by name and email")
    void shouldSearchUsersByNameAndEmail() {
        // When
        List<UserEntity> searchResults = userRepository.searchUsers("Ahmet");
        List<UserEntity> emailSearch = userRepository.searchUsers("test@example");

        // Then
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getFirstName()).contains("Ahmet");

        assertThat(emailSearch).hasSize(1);
        assertThat(emailSearch.get(0).getEmail()).contains("test@example");
    }

    @Test
    @DisplayName("Should search Turkish users with special characters")
    void shouldSearchTurkishUsersWithSpecialCharacters() {
        // When
        List<UserEntity> searchResults = userRepository.searchUsers("Çağlar");

        // Then
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getFirstName()).isEqualTo("Çağlar");
    }

    @Test
    @DisplayName("Should update email verification")
    void shouldUpdateEmailVerification() {
        // Given
        LocalDateTime verifiedAt = LocalDateTime.now(ISTANBUL_ZONE);

        // When
        int updated = userRepository.updateEmailVerification(inactiveUser.getId(), true, verifiedAt);

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmailVerified()).isTrue();
        assertThat(updatedUser.getEmailVerifiedAt()).isEqualToIgnoringNanos(verifiedAt);
    }

    @Test
    @DisplayName("Should update phone verification")
    void shouldUpdatePhoneVerification() {
        // Given
        LocalDateTime verifiedAt = LocalDateTime.now(ISTANBUL_ZONE);

        // When
        int updated = userRepository.updatePhoneVerification(inactiveUser.getId(), true, verifiedAt);

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertThat(updatedUser.getPhoneVerified()).isTrue();
        assertThat(updatedUser.getPhoneVerifiedAt()).isEqualToIgnoringNanos(verifiedAt);
    }

    @Test
    @DisplayName("Should update KYC status")
    void shouldUpdateKycStatus() {
        // Given
        LocalDateTime completedAt = LocalDateTime.now(ISTANBUL_ZONE);

        // When
        int updated = userRepository.updateKycStatus(
            inactiveUser.getId(), true, completedAt, UserEntity.KycLevel.BASIC
        );

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertThat(updatedUser.getKycCompleted()).isTrue();
        assertThat(updatedUser.getKycCompletedAt()).isEqualToIgnoringNanos(completedAt);
        assertThat(updatedUser.getKycLevel()).isEqualTo(UserEntity.KycLevel.BASIC);
    }

    @Test
    @DisplayName("Should increment failed login attempts")
    void shouldIncrementFailedLoginAttempts() {
        // When
        int updated = userRepository.incrementFailedLoginAttempts(testUser.getId());

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reset failed login attempts")
    void shouldResetFailedLoginAttempts() {
        // Given - First increment to have something to reset
        userRepository.incrementFailedLoginAttempts(testUser.getId());

        // When
        int updated = userRepository.resetFailedLoginAttempts(testUser.getId());

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getAccountLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Should lock user account")
    void shouldLockUserAccount() {
        // Given
        LocalDateTime lockUntil = LocalDateTime.now(ISTANBUL_ZONE).plusHours(1);

        // When
        int updated = userRepository.lockUserAccount(testUser.getId(), lockUntil);

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getAccountLockedUntil()).isEqualToIgnoringNanos(lockUntil);
    }

    @Test
    @DisplayName("Should update last login with Istanbul timezone")
    void shouldUpdateLastLoginWithIstanbulTimezone() {
        // Given
        LocalDateTime loginTime = LocalDateTime.now(ISTANBUL_ZONE);
        String ipAddress = "192.168.1.100";

        // When
        int updated = userRepository.updateLastLogin(testUser.getId(), loginTime, ipAddress);

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isEqualToIgnoringNanos(loginTime);
        assertThat(updatedUser.getLastLoginIp()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should soft delete user")
    void shouldSoftDeleteUser() {
        // Given
        LocalDateTime deletedAt = LocalDateTime.now(ISTANBUL_ZONE);

        // When
        int updated = userRepository.softDeleteUser(testUser.getId(), deletedAt);

        // Then
        assertThat(updated).isEqualTo(1);

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getDeletedAt()).isEqualToIgnoringNanos(deletedAt);
        assertThat(updatedUser.getStatus()).isEqualTo(UserEntity.UserStatus.CLOSED);
    }

    @Test
    @DisplayName("Should find users with expired passwords")
    void shouldFindUsersWithExpiredPasswords() {
        // Given - Set password expiry in the past
        testUser.setPasswordExpiresAt(LocalDateTime.now(ISTANBUL_ZONE).minusDays(1));
        entityManager.merge(testUser);
        entityManager.flush();

        // When
        List<UserEntity> expiredUsers = userRepository.findUsersWithExpiredPasswords(
            LocalDateTime.now(ISTANBUL_ZONE)
        );

        // Then
        assertThat(expiredUsers).hasSize(1);
        assertThat(expiredUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find users with locked accounts")
    void shouldFindUsersWithLockedAccounts() {
        // Given - Set account lock in the future
        testUser.setAccountLockedUntil(LocalDateTime.now(ISTANBUL_ZONE).plusHours(1));
        entityManager.merge(testUser);
        entityManager.flush();

        // When
        List<UserEntity> lockedUsers = userRepository.findUsersWithLockedAccounts(
            LocalDateTime.now(ISTANBUL_ZONE)
        );

        // Then
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find inactive users by last login date")
    void shouldFindInactiveUsersByLastLoginDate() {
        // Given - Set last login to old date
        testUser.setLastLoginAt(LocalDateTime.now(ISTANBUL_ZONE).minusDays(100));
        entityManager.merge(testUser);
        entityManager.flush();

        // When
        LocalDateTime cutoffDate = LocalDateTime.now(ISTANBUL_ZONE).minusDays(30);
        List<UserEntity> inactiveUsers = userRepository.findInactiveUsers(cutoffDate);

        // Then
        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find users by created date range")
    void shouldFindUsersByCreatedDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now(ISTANBUL_ZONE).minusDays(1);
        LocalDateTime endDate = LocalDateTime.now(ISTANBUL_ZONE).plusDays(1);

        // When
        List<UserEntity> usersInRange = userRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(usersInRange).hasSize(3); // All test users
    }

    @Test
    @DisplayName("Should handle null and empty values gracefully")
    void shouldHandleNullAndEmptyValuesGracefully() {
        // When & Then
        assertThat(userRepository.findByEmail(null)).isEmpty();
        assertThat(userRepository.findByEmail("")).isEmpty();
        assertThat(userRepository.findByUsername(null)).isEmpty();
        assertThat(userRepository.searchUsers("")).isEmpty();
        assertThat(userRepository.searchUsers(null)).isEmpty();
    }

    @Test
    @DisplayName("Should test entity business methods")
    void shouldTestEntityBusinessMethods() {
        // When & Then
        assertThat(testUser.isActive()).isTrue();
        assertThat(inactiveUser.isActive()).isFalse();

        assertThat(testUser.isFullyVerified()).isTrue();
        assertThat(inactiveUser.isFullyVerified()).isFalse();

        assertThat(testUser.canTrade()).isTrue();
        assertThat(inactiveUser.canTrade()).isFalse();

        assertThat(testUser.getFullName()).isEqualTo(testUser.getFirstName() + " " + testUser.getLastName());
        assertThat(turkishUser.getFullName()).contains("Çağlar");
    }

    @Test
    @DisplayName("Should handle concurrent user creation")
    void shouldHandleConcurrentUserCreation() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<UserEntity>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                UserEntity user = TestDataBuilder.validUser()
                    .email("concurrent" + index + "@example.com")
                    .username("concurrent" + index)
                    .tcKimlikNo(generateValidTcKimlik(index))
                    .build();

                entityManager.persist(user);
                entityManager.flush();
                return user;
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(10, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<UserEntity> future : futures) {
            UserEntity user = future.get();
            assertThat(user.getId()).isNotNull();
            assertThat(userRepository.existsByEmail(user.getEmail())).isTrue();
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle Turkish character encoding in database")
    void shouldHandleTurkishCharacterEncodingInDatabase() {
        // Given
        UserEntity userWithTurkishChars = TestDataBuilder.validUser()
            .firstName("Çağlar")
            .lastName("Şıktırıkoğlu")
            .email("çağlar@örnek.com")
            .city("İstanbul")
            .build();

        // When
        entityManager.persist(userWithTurkishChars);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context

        // Then
        Optional<UserEntity> retrieved = userRepository.findByEmail("çağlar@örnek.com");
        assertThat(retrieved).isPresent();

        UserEntity user = retrieved.get();
        assertThat(user.getFirstName()).isEqualTo("Çağlar");
        assertThat(user.getLastName()).isEqualTo("Şıktırıkoğlu");
        assertThat(user.getEmail()).isEqualTo("çağlar@örnek.com");
        assertThat(user.getCity()).isEqualTo("İstanbul");
    }

    @Test
    @DisplayName("Should test batch operations performance")
    void shouldTestBatchOperationsPerformance() {
        // Given
        int batchSize = 100;
        UserEntity[] users = TestDataBuilder.ConcurrentScenarios.multipleUsers(batchSize);

        long startTime = System.currentTimeMillis();

        // When
        for (UserEntity user : users) {
            entityManager.persist(user);
        }
        entityManager.flush();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertThat(duration).isLessThan(5000); // Should complete in less than 5 seconds
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(batchSize);
    }

    private String generateValidTcKimlik(int index) {
        String[] validTcKimliks = {
            "12345678901", "98765432109", "11111111110", "22222222220", "33333333330"
        };
        return validTcKimliks[index % validTcKimliks.length];
    }
}