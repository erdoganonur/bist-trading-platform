package com.bisttrading.user.performance;

import com.bisttrading.user.util.TurkishValidationUtil;
import com.bisttrading.user.test.TestDataBuilder;
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

@DisplayName("Validation Performance Tests")
class ValidationPerformanceTest {

    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("TC Kimlik validation should complete within performance threshold")
    void shouldValidateTcKimlikWithinPerformanceThreshold() {
        String tcKimlik = "12345678901";

        Instant start = Instant.now();
        boolean isValid = TurkishValidationUtil.isValidTCKimlik(tcKimlik);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(100));
    }

    @Test
    @DisplayName("Phone number validation should complete within performance threshold")
    void shouldValidatePhoneNumberWithinPerformanceThreshold() {
        String phoneNumber = "5551234567";

        Instant start = Instant.now();
        boolean isValid = TurkishValidationUtil.isValidPhoneNumber(phoneNumber);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(50));
    }

    @Test
    @DisplayName("IBAN validation should complete within performance threshold")
    void shouldValidateIbanWithinPerformanceThreshold() {
        String iban = "TR320010009999901234567890";

        Instant start = Instant.now();
        boolean isValid = TurkishValidationUtil.isValidIBAN(iban);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(200));
    }

    @Test
    @DisplayName("Email validation should complete within performance threshold")
    void shouldValidateEmailWithinPerformanceThreshold() {
        String email = "test@example.com";

        Instant start = Instant.now();
        boolean isValid = TurkishValidationUtil.isValidEmail(email);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(100));
    }

    @Test
    @DisplayName("Turkish character validation should not impact performance")
    void shouldHandleTurkishCharactersWithoutPerformanceImpact() {
        String turkishEmail = "test.çğıöşü@örnek.com.tr";
        String englishEmail = "test.abcdef@example.com";

        Instant start1 = Instant.now();
        TurkishValidationUtil.isValidEmail(turkishEmail);
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        TurkishValidationUtil.isValidEmail(englishEmail);
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @Test
    @DisplayName("Bulk TC Kimlik validation should maintain performance")
    void shouldMaintainPerformanceForBulkTcKimlikValidation() {
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

        assertThat(averagePerValidation).isLessThan(Duration.ofMicros(10));
        assertThat(duration).isLessThan(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("Concurrent validation should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentValidation() throws Exception {
        int concurrentOperations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentOperations);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentOperations)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                String tcKimlik = "1234567890" + (i % 10);

                Instant start = Instant.now();
                TurkishValidationUtil.isValidTCKimlik(tcKimlik);
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

        assertThat(averageDuration).isLessThan(Duration.ofMicros(500));
        assertThat(durations).allSatisfy(duration ->
            assertThat(duration).isLessThan(Duration.ofMillis(1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000, 10000})
    @DisplayName("Validation performance should scale with input count")
    void shouldMaintainValidationPerformanceWithVariousInputCounts(int inputCount) {
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < inputCount; i++) {
            inputs.add("test" + i + "@example.com");
        }

        Instant start = Instant.now();

        for (String email : inputs) {
            TurkishValidationUtil.isValidEmail(email);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerValidation = duration.dividedBy(inputCount);

        Duration maxExpectedAverage = Duration.ofMicros(50);
        assertThat(averagePerValidation).isLessThan(maxExpectedAverage);
    }

    @Test
    @DisplayName("Complex validation chain should complete within performance threshold")
    void shouldCompleteComplexValidationChainWithinPerformanceThreshold() {
        String email = "test@example.com";
        String tcKimlik = "12345678901";
        String phoneNumber = "5551234567";
        String iban = "TR320010009999901234567890";

        Instant start = Instant.now();
        boolean emailValid = TurkishValidationUtil.isValidEmail(email);
        boolean tcValid = TurkishValidationUtil.isValidTCKimlik(tcKimlik);
        boolean phoneValid = TurkishValidationUtil.isValidPhoneNumber(phoneNumber);
        boolean ibanValid = TurkishValidationUtil.isValidIBAN(iban);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(500));
    }

    @Test
    @DisplayName("Invalid input validation should not degrade performance")
    void shouldMaintainPerformanceWithInvalidInputs() {
        String invalidTcKimlik = "invalid";
        String invalidEmail = "not-an-email";
        String invalidPhone = "abc123";
        String invalidIban = "not-an-iban";

        Instant start = Instant.now();
        TurkishValidationUtil.isValidTCKimlik(invalidTcKimlik);
        TurkishValidationUtil.isValidEmail(invalidEmail);
        TurkishValidationUtil.isValidPhoneNumber(invalidPhone);
        TurkishValidationUtil.isValidIBAN(invalidIban);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(200));
    }

    @Test
    @DisplayName("Edge case validation should complete within performance threshold")
    void shouldValidateEdgeCasesWithinPerformanceThreshold() {
        String emptyString = "";
        String nullString = null;
        String veryLongString = "a".repeat(10000);

        Instant start = Instant.now();
        TurkishValidationUtil.isValidEmail(emptyString);
        TurkishValidationUtil.isValidEmail(nullString);
        TurkishValidationUtil.isValidEmail(veryLongString);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMillis(1));
    }

    @Test
    @DisplayName("Memory-intensive validation should not degrade performance")
    void shouldMaintainPerformanceWithMemoryIntensiveValidation() {
        int validationCount = 10000;

        Instant start = Instant.now();

        for (int i = 0; i < validationCount; i++) {
            String email = "test" + i + "@example.com";
            TurkishValidationUtil.isValidEmail(email);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerValidation = duration.dividedBy(validationCount);

        assertThat(averagePerValidation).isLessThan(Duration.ofMicros(10));
        assertThat(duration).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("Repeated validation should maintain consistent performance")
    void shouldMaintainConsistentPerformanceForRepeatedValidation() {
        String tcKimlik = "12345678901";
        int repetitions = 1000;
        List<Duration> durations = new ArrayList<>();

        for (int i = 0; i < repetitions; i++) {
            Instant start = Instant.now();
            TurkishValidationUtil.isValidTCKimlik(tcKimlik);
            Duration duration = Duration.between(start, Instant.now());
            durations.add(duration);
        }

        Duration averageDuration = durations.stream()
            .reduce(Duration.ZERO, Duration::plus)
            .dividedBy(durations.size());

        Duration maxDuration = durations.stream()
            .max(Duration::compareTo)
            .orElse(Duration.ZERO);

        assertThat(averageDuration).isLessThan(Duration.ofMicros(50));
        assertThat(maxDuration).isLessThan(Duration.ofMicros(500));
    }

    @Test
    @DisplayName("Turkish locale validation should complete within performance threshold")
    void shouldValidateTurkishLocaleWithinPerformanceThreshold() {
        String turkishTcKimlik = "12345678901";
        String turkishPhone = "5551234567";
        String turkishIban = "TR320010009999901234567890";

        Instant start = Instant.now();
        boolean tcValid = TurkishValidationUtil.isValidTCKimlik(turkishTcKimlik);
        boolean phoneValid = TurkishValidationUtil.isValidPhoneNumber(turkishPhone);
        boolean ibanValid = TurkishValidationUtil.isValidIBAN(turkishIban);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(duration).isLessThan(Duration.ofMicros(300));
    }
}