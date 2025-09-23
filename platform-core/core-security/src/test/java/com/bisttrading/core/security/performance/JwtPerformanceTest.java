package com.bisttrading.core.security.performance;

import com.bisttrading.core.security.jwt.JwtTokenProvider;
import com.bisttrading.core.security.jwt.JwtTokenClaims;
import com.bisttrading.core.security.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

@DisplayName("JWT Performance Tests")
class JwtPerformanceTest {

    private JwtTokenProvider jwtTokenProvider;
    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("JWT token generation should complete within performance threshold")
    void shouldGenerateTokenWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.validJwtClaims();

        Instant start = Instant.now();
        String token = jwtTokenProvider.generateToken(claims);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(token).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("JWT token validation should complete within performance threshold")
    void shouldValidateTokenWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.validJwtClaims();
        String token = jwtTokenProvider.generateToken(claims);

        Instant start = Instant.now();
        boolean isValid = jwtTokenProvider.validateToken(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(isValid).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("JWT claims extraction should complete within performance threshold")
    void shouldExtractClaimsWithinPerformanceThreshold() {
        JwtTokenClaims originalClaims = testDataBuilder.validJwtClaims();
        String token = jwtTokenProvider.generateToken(originalClaims);

        Instant start = Instant.now();
        JwtTokenClaims extractedClaims = jwtTokenProvider.extractClaims(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(extractedClaims).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Turkish character JWT should not impact performance")
    void shouldHandleTurkishCharactersWithoutPerformanceImpact() {
        JwtTokenClaims turkishClaims = testDataBuilder.turkishJwtClaims();
        JwtTokenClaims englishClaims = testDataBuilder.validJwtClaims();

        Instant start1 = Instant.now();
        String turkishToken = jwtTokenProvider.generateToken(turkishClaims);
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        String englishToken = jwtTokenProvider.generateToken(englishClaims);
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishToken).isNotBlank();
        assertThat(englishToken).isNotBlank();
        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @Test
    @DisplayName("Concurrent token generation should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentTokenGeneration() throws Exception {
        int concurrentOperations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentOperations);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentOperations)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                JwtTokenClaims claims = testDataBuilder.validJwtClaims();
                claims.setSubject("user" + i);

                Instant start = Instant.now();
                jwtTokenProvider.generateToken(claims);
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

        assertThat(averageDuration).isLessThan(Duration.ofMillis(200));
        assertThat(durations).allSatisfy(duration ->
            assertThat(duration).isLessThan(Duration.ofMillis(300)));
    }

    @Test
    @DisplayName("Concurrent token validation should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentTokenValidation() throws Exception {
        int concurrentOperations = 50;
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < concurrentOperations; i++) {
            JwtTokenClaims claims = testDataBuilder.validJwtClaims();
            claims.setSubject("user" + i);
            tokens.add(jwtTokenProvider.generateToken(claims));
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrentOperations);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentOperations)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                String token = tokens.get(i);

                Instant start = Instant.now();
                jwtTokenProvider.validateToken(token);
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

        assertThat(averageDuration).isLessThan(Duration.ofMillis(100));
        assertThat(durations).allSatisfy(duration ->
            assertThat(duration).isLessThan(Duration.ofMillis(150)));
    }

    @Test
    @DisplayName("Bulk token generation should maintain performance")
    void shouldMaintainPerformanceForBulkTokenGeneration() {
        int tokenCount = 100;

        Instant start = Instant.now();

        for (int i = 0; i < tokenCount; i++) {
            JwtTokenClaims claims = testDataBuilder.validJwtClaims();
            claims.setSubject("bulk" + i);
            jwtTokenProvider.generateToken(claims);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerToken = duration.dividedBy(tokenCount);

        assertThat(averagePerToken).isLessThan(Duration.ofMillis(20));
        assertThat(duration).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Token refresh should complete within performance threshold")
    void shouldRefreshTokenWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.validJwtClaims();
        String originalToken = jwtTokenProvider.generateToken(claims);

        Instant start = Instant.now();
        String refreshedToken = jwtTokenProvider.refreshToken(originalToken);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(refreshedToken).isNotBlank();
        assertThat(refreshedToken).isNotEqualTo(originalToken);
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 50})
    @DisplayName("Token validation performance should scale with token count")
    void shouldMaintainValidationPerformanceWithVariousTokenCounts(int tokenCount) {
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < tokenCount; i++) {
            JwtTokenClaims claims = testDataBuilder.validJwtClaims();
            claims.setSubject("scale" + i);
            tokens.add(jwtTokenProvider.generateToken(claims));
        }

        Instant start = Instant.now();

        for (String token : tokens) {
            jwtTokenProvider.validateToken(token);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerToken = duration.dividedBy(tokenCount);

        assertThat(averagePerToken).isLessThan(Duration.ofMillis(10));
    }

    @Test
    @DisplayName("Token blacklisting should complete within performance threshold")
    void shouldBlacklistTokenWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.validJwtClaims();
        String token = jwtTokenProvider.generateToken(claims);

        Instant start = Instant.now();
        jwtTokenProvider.blacklistToken(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(jwtTokenProvider.isTokenBlacklisted(token)).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Blacklist checking should complete within performance threshold")
    void shouldCheckBlacklistWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.validJwtClaims();
        String token = jwtTokenProvider.generateToken(claims);
        jwtTokenProvider.blacklistToken(token);

        Instant start = Instant.now();
        boolean isBlacklisted = jwtTokenProvider.isTokenBlacklisted(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(isBlacklisted).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(30));
    }

    @Test
    @DisplayName("Complex claims processing should maintain performance")
    void shouldMaintainPerformanceWithComplexClaims() {
        JwtTokenClaims complexClaims = testDataBuilder.complexJwtClaims();

        Instant start = Instant.now();
        String token = jwtTokenProvider.generateToken(complexClaims);
        JwtTokenClaims extractedClaims = jwtTokenProvider.extractClaims(token);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(token).isNotBlank();
        assertThat(extractedClaims).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(150));
    }

    @Test
    @DisplayName("Token expiration checking should complete within performance threshold")
    void shouldCheckExpirationWithinPerformanceThreshold() {
        JwtTokenClaims claims = testDataBuilder.expiredJwtClaims();
        String expiredToken = jwtTokenProvider.generateToken(claims);

        Instant start = Instant.now();
        boolean isExpired = jwtTokenProvider.isTokenExpired(expiredToken);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(isExpired).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(30));
    }
}