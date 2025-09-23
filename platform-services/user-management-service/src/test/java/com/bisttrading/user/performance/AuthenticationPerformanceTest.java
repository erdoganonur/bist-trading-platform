package com.bisttrading.user.performance;

import com.bisttrading.user.dto.request.LoginRequest;
import com.bisttrading.user.dto.request.RegisterRequest;
import com.bisttrading.user.dto.response.AuthenticationResponse;
import com.bisttrading.user.service.AuthenticationService;
import com.bisttrading.user.test.TestDataBuilder;
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
@DisplayName("Authentication Performance Tests")
class AuthenticationPerformanceTest {

    @Autowired
    private AuthenticationService authenticationService;

    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("User registration should complete within performance threshold")
    void shouldRegisterUserWithinPerformanceThreshold() {
        RegisterRequest request = testDataBuilder.validRegisterRequest();

        Instant start = Instant.now();
        AuthenticationResponse response = authenticationService.register(request);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("User login should complete within performance threshold")
    void shouldLoginUserWithinPerformanceThreshold() {
        RegisterRequest registerRequest = testDataBuilder.validRegisterRequest();
        authenticationService.register(registerRequest);

        LoginRequest loginRequest = testDataBuilder.validLoginRequest(
            registerRequest.getEmail(), registerRequest.getPassword());

        Instant start = Instant.now();
        AuthenticationResponse response = authenticationService.login(loginRequest);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(300));
    }

    @Test
    @DisplayName("JWT token validation should complete within performance threshold")
    void shouldValidateTokenWithinPerformanceThreshold() {
        RegisterRequest registerRequest = testDataBuilder.validRegisterRequest();
        AuthenticationResponse authResponse = authenticationService.register(registerRequest);

        String token = authResponse.getAccessToken();

        Instant start = Instant.now();
        boolean isValid = authenticationService.validateToken(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(isValid).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Turkish character registration should not impact performance")
    void shouldHandleTurkishCharactersWithoutPerformanceImpact() {
        RegisterRequest turkishRequest = testDataBuilder.turkishRegisterRequest();
        RegisterRequest englishRequest = testDataBuilder.validRegisterRequest();

        Instant start1 = Instant.now();
        authenticationService.register(turkishRequest);
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        authenticationService.register(englishRequest);
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @Test
    @DisplayName("Concurrent user registrations should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentRegistrations() throws Exception {
        int concurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentUsers)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                RegisterRequest request = testDataBuilder.validRegisterRequest();
                request.setEmail("user" + i + "@test.com");

                Instant start = Instant.now();
                authenticationService.register(request);
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

        assertThat(averageDuration).isLessThan(Duration.ofSeconds(1));
        assertThat(durations).allSatisfy(duration ->
            assertThat(duration).isLessThan(Duration.ofSeconds(2)));
    }

    @Test
    @DisplayName("Concurrent logins should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentLogins() throws Exception {
        int concurrentUsers = 10;
        List<RegisterRequest> users = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            RegisterRequest registerRequest = testDataBuilder.validRegisterRequest();
            registerRequest.setEmail("concurrent" + i + "@test.com");
            authenticationService.register(registerRequest);
            users.add(registerRequest);
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentUsers)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                RegisterRequest user = users.get(i);
                LoginRequest loginRequest = testDataBuilder.validLoginRequest(
                    user.getEmail(), user.getPassword());

                Instant start = Instant.now();
                authenticationService.login(loginRequest);
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
    @DisplayName("Token refresh should complete within performance threshold")
    void shouldRefreshTokenWithinPerformanceThreshold() {
        RegisterRequest registerRequest = testDataBuilder.validRegisterRequest();
        AuthenticationResponse authResponse = authenticationService.register(registerRequest);

        String refreshToken = authResponse.getRefreshToken();

        Instant start = Instant.now();
        AuthenticationResponse refreshResponse = authenticationService.refreshToken(refreshToken);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Bulk token validation should maintain performance")
    void shouldMaintainPerformanceForBulkTokenValidation() {
        int tokenCount = 100;
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < tokenCount; i++) {
            RegisterRequest request = testDataBuilder.validRegisterRequest();
            request.setEmail("bulk" + i + "@test.com");
            AuthenticationResponse response = authenticationService.register(request);
            tokens.add(response.getAccessToken());
        }

        Instant start = Instant.now();

        for (String token : tokens) {
            authenticationService.validateToken(token);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerToken = duration.dividedBy(tokenCount);

        assertThat(averagePerToken).isLessThan(Duration.ofMillis(10));
        assertThat(duration).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Password hashing should complete within performance threshold")
    void shouldHashPasswordWithinPerformanceThreshold() {
        String password = "TestPassword123!";

        Instant start = Instant.now();
        String hashedPassword = authenticationService.hashPassword(password);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(hashedPassword).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }
}