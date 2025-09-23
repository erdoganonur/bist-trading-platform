package com.bisttrading.user.performance;

import com.bisttrading.user.dto.request.UpdateProfileRequest;
import com.bisttrading.user.dto.request.ChangePasswordRequest;
import com.bisttrading.user.dto.response.UserProfileResponse;
import com.bisttrading.user.entity.User;
import com.bisttrading.user.service.UserService;
import com.bisttrading.user.test.TestDataBuilder;
import com.bisttrading.user.util.TurkishValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User Service Performance Tests")
class UserServicePerformanceTest {

    @Autowired
    private UserService userService;

    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("Profile retrieval should complete within performance threshold")
    void shouldRetrieveProfileWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        Instant start = Instant.now();
        UserProfileResponse profile = userService.getUserProfile(user.getId());
        Duration duration = Duration.between(start, Instant.now());

        assertThat(profile).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Profile update should complete within performance threshold")
    void shouldUpdateProfileWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        UpdateProfileRequest updateRequest = testDataBuilder.validUpdateProfileRequest();

        Instant start = Instant.now();
        userService.updateProfile(user.getId(), updateRequest);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Turkish profile update should not impact performance")
    void shouldHandleTurkishProfileUpdateWithoutPerformanceImpact() {
        User turkishUser = testDataBuilder.turkishUser();
        User englishUser = testDataBuilder.validUser();

        turkishUser = userService.saveUser(turkishUser);
        englishUser = userService.saveUser(englishUser);

        UpdateProfileRequest turkishUpdate = testDataBuilder.turkishUpdateProfileRequest();
        UpdateProfileRequest englishUpdate = testDataBuilder.validUpdateProfileRequest();

        Instant start1 = Instant.now();
        userService.updateProfile(turkishUser.getId(), turkishUpdate);
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        userService.updateProfile(englishUser.getId(), englishUpdate);
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @Test
    @DisplayName("Password change should complete within performance threshold")
    void shouldChangePasswordWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        ChangePasswordRequest changeRequest = testDataBuilder.validChangePasswordRequest();

        Instant start = Instant.now();
        userService.changePassword(user.getId(), changeRequest);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(150));
    }

    @Test
    @DisplayName("Email verification should complete within performance threshold")
    void shouldVerifyEmailWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        String verificationCode = "123456";

        Instant start = Instant.now();
        userService.verifyEmail(user.getId(), verificationCode);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Phone verification should complete within performance threshold")
    void shouldVerifyPhoneWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        String verificationCode = "123456";

        Instant start = Instant.now();
        userService.verifyPhone(user.getId(), verificationCode);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Concurrent profile updates should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentProfileUpdates() throws Exception {
        int concurrentUpdates = 10;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < concurrentUpdates; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("concurrent" + i + "@test.com");
            users.add(userService.saveUser(user));
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUpdates);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentUpdates)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                User user = users.get(i);
                UpdateProfileRequest updateRequest = testDataBuilder.validUpdateProfileRequest();

                Instant start = Instant.now();
                userService.updateProfile(user.getId(), updateRequest);
                return Duration.between(start, Instant.now());
            }, executor))
            .toList();

        List<Duration> durations = new ArrayList<>();
        for (CompletableFuture<Duration> future : futures) {
            durations.add(future.get(5, TimeUnit.SECONDS));
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
    @DisplayName("Bulk user retrieval should maintain performance")
    void shouldMaintainPerformanceForBulkUserRetrieval() {
        int userCount = 50;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("bulk" + i + "@test.com");
            users.add(userService.saveUser(user));
        }

        Instant start = Instant.now();

        for (User user : users) {
            userService.getUserProfile(user.getId());
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerUser = duration.dividedBy(userCount);

        assertThat(averagePerUser).isLessThan(Duration.ofMillis(20));
        assertThat(duration).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    @DisplayName("Turkish validation should complete within performance threshold")
    void shouldValidateTurkishDataWithinPerformanceThreshold() {
        String tcKimlik = "12345678901";
        String phoneNumber = "5551234567";
        String iban = "TR320010009999901234567890";

        Instant start = Instant.now();
        boolean tcValid = TurkishValidationUtil.isValidTCKimlik(tcKimlik);
        boolean phoneValid = TurkishValidationUtil.isValidPhoneNumber(phoneNumber);
        boolean ibanValid = TurkishValidationUtil.isValidIBAN(iban);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Bulk Turkish validation should maintain performance")
    void shouldMaintainPerformanceForBulkTurkishValidation() {
        int validationCount = 1000;
        List<String> tcKimliks = new ArrayList<>();

        for (int i = 0; i < validationCount; i++) {
            tcKimliks.add("1234567890" + (i % 10));
        }

        Instant start = Instant.now();

        for (String tcKimlik : tcKimliks) {
            TurkishValidationUtil.isValidTCKimlik(tcKimlik);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerValidation = duration.dividedBy(validationCount);

        assertThat(averagePerValidation).isLessThan(Duration.ofMicros(100));
        assertThat(duration).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("User search should complete within performance threshold")
    void shouldSearchUsersWithinPerformanceThreshold() {
        for (int i = 0; i < 20; i++) {
            User user = testDataBuilder.validUser();
            user.setEmail("search" + i + "@test.com");
            user.setFirstName("Test" + i);
            userService.saveUser(user);
        }

        String searchQuery = "Test";

        Instant start = Instant.now();
        List<UserProfileResponse> results = userService.searchUsers(searchQuery);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(results).isNotEmpty();
        assertThat(duration).isLessThan(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Account deactivation should complete within performance threshold")
    void shouldDeactivateAccountWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        Instant start = Instant.now();
        userService.deactivateAccount(user.getId());
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Session management should complete within performance threshold")
    void shouldManageSessionWithinPerformanceThreshold() {
        User user = testDataBuilder.validUser();
        user = userService.saveUser(user);

        String sessionId = "test-session-123";

        Instant start = Instant.now();
        userService.createUserSession(user.getId(), sessionId);
        userService.invalidateUserSession(user.getId(), sessionId);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(150));
    }
}