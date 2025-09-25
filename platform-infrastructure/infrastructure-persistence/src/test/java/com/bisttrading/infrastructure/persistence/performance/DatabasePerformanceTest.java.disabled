package com.bisttrading.infrastructure.persistence.performance;

import com.bisttrading.infrastructure.persistence.entity.User;
import com.bisttrading.infrastructure.persistence.entity.Address;
import com.bisttrading.infrastructure.persistence.entity.FinancialData;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.infrastructure.persistence.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bist_trading_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("User save operation should complete within performance threshold")
    void shouldSaveUserWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();

        Instant start = Instant.now();
        User savedUser = userRepository.save(user);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(savedUser.getId()).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("User retrieval should complete within performance threshold")
    void shouldRetrieveUserWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
        Duration duration = Duration.between(start, Instant.now());

        assertThat(retrievedUser).isPresent();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("User update should complete within performance threshold")
    void shouldUpdateUserWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        User savedUser = userRepository.save(user);
        entityManager.flush();

        savedUser.setFirstName("Updated");
        savedUser.setLastName("Name");

        Instant start = Instant.now();
        userRepository.save(savedUser);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("User deletion should complete within performance threshold")
    void shouldDeleteUserWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        User savedUser = userRepository.save(user);
        entityManager.flush();

        Instant start = Instant.now();
        userRepository.deleteById(savedUser.getId());
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Turkish character persistence should not impact performance")
    void shouldHandleTurkishCharactersWithoutPerformanceImpact() {
        User turkishUser = testDataBuilder.turkishUser();
        User englishUser = testDataBuilder.validUser();

        Instant start1 = Instant.now();
        userRepository.save(turkishUser);
        entityManager.flush();
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        userRepository.save(englishUser);
        entityManager.flush();
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @Test
    @DisplayName("Complex entity with relationships should maintain performance")
    void shouldMaintainPerformanceWithComplexEntity() {
        User user = testDataBuilder.validUser();

        Address address = testDataBuilder.validAddress();
        user.setAddress(address);

        FinancialData financialData = testDataBuilder.validFinancialData();
        user.setFinancialData(financialData);

        Instant start = Instant.now();
        User savedUser = userRepository.save(user);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getAddress()).isNotNull();
        assertThat(savedUser.getFinancialData()).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Bulk insert should maintain performance")
    void shouldMaintainPerformanceForBulkInsert() {
        int userCount = 50;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("bulk" + i + "@test.com");
            users.add(user);
        }

        Instant start = Instant.now();
        userRepository.saveAll(users);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        Duration averagePerUser = duration.dividedBy(userCount);

        assertThat(averagePerUser).isLessThan(Duration.ofMillis(50));
        assertThat(duration).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Email search should complete within performance threshold")
    void shouldSearchByEmailWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        Optional<User> foundUser = userRepository.findByEmail(user.getEmail());
        Duration duration = Duration.between(start, Instant.now());

        assertThat(foundUser).isPresent();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("TC Kimlik search should complete within performance threshold")
    void shouldSearchByTcKimlikWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        Optional<User> foundUser = userRepository.findByTcKimlik(user.getTcKimlik());
        Duration duration = Duration.between(start, Instant.now());

        assertThat(foundUser).isPresent();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Financial precision operations should maintain performance")
    void shouldMaintainPerformanceWithFinancialPrecision() {
        User user = testDataBuilder.validUser();
        FinancialData financialData = testDataBuilder.validFinancialData();

        BigDecimal preciseBalance = new BigDecimal("12345.6789012345");
        BigDecimal preciseCommission = new BigDecimal("0.0025");

        financialData.setAccountBalance(preciseBalance);
        financialData.setCommissionRate(preciseCommission);
        user.setFinancialData(financialData);

        Instant start = Instant.now();
        userRepository.save(user);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(150));
    }

    @Test
    @DisplayName("Timezone-aware operations should maintain performance")
    void shouldMaintainPerformanceWithTimezoneOperations() {
        User user = testDataBuilder.validUser();

        ZonedDateTime istanbulTime = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
        user.setCreatedAt(istanbulTime.toInstant());
        user.setUpdatedAt(istanbulTime.toInstant());

        Instant start = Instant.now();
        userRepository.save(user);
        entityManager.flush();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Concurrent database operations should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentOperations() throws Exception {
        int concurrentOperations = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentOperations);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentOperations)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                User user = testDataBuilder.validUser();
                user.setEmail("concurrent" + i + "@test.com");

                Instant start = Instant.now();
                userRepository.save(user);
                entityManager.flush();
                return Duration.between(start, Instant.now());
            }, executor))
            .toList();

        List<Duration> durations = new ArrayList<>();
        for (CompletableFuture<Duration> future : futures) {
            durations.add(future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();

        Duration averageDuration = durations.stream()
            .reduce(Duration.ZERO, Duration::plus)
            .dividedBy(durations.size());

        assertThat(averageDuration).isLessThan(Duration.ofMillis(500));
        assertThat(durations).allSatisfy(duration ->
            assertThat(duration).isLessThan(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("Complex query should complete within performance threshold")
    void shouldExecuteComplexQueryWithinPerformanceThreshold() {
        for (int i = 0; i < 20; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("query" + i + "@test.com");
            user.setFirstName("Test" + i);
            userRepository.save(user);
        }
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        List<User> users = userRepository.findByFirstNameContainingIgnoreCase("Test");
        Duration duration = Duration.between(start, Instant.now());

        assertThat(users).isNotEmpty();
        assertThat(duration).isLessThan(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Large result set should maintain performance")
    void shouldMaintainPerformanceWithLargeResultSet() {
        int userCount = 100;

        for (int i = 0; i < userCount; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("large" + i + "@test.com");
            userRepository.save(user);
        }
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        List<User> allUsers = userRepository.findAll();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(allUsers).hasSize(userCount);
        assertThat(duration).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Paginated queries should complete within performance threshold")
    void shouldExecutePaginatedQueriesWithinPerformanceThreshold() {
        for (int i = 0; i < 50; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("page" + i + "@test.com");
            userRepository.save(user);
        }
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        List<User> firstPage = userRepository.findByEmailContaining("page",
            org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(firstPage).hasSize(10);
        assertThat(duration).isLessThan(Duration.ofMillis(150));
    }

    @Test
    @DisplayName("Index-based queries should maintain optimal performance")
    void shouldMaintainOptimalPerformanceWithIndexedQueries() {
        for (int i = 0; i < 30; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("indexed" + i + "@test.com");
            userRepository.save(user);
        }
        entityManager.flush();
        entityManager.clear();

        Instant start = Instant.now();
        Optional<User> user = userRepository.findByEmail("indexed15@test.com");
        Duration duration = Duration.between(start, Instant.now());

        assertThat(user).isPresent();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }
}