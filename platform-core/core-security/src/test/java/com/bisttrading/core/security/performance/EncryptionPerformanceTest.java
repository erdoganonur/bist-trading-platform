package com.bisttrading.core.security.performance;

import com.bisttrading.core.security.encryption.AESEncryptionService;
import com.bisttrading.core.security.encryption.EncryptionResult;
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

@DisplayName("Encryption Performance Tests")
class EncryptionPerformanceTest {

    private AESEncryptionService encryptionService;
    private TestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        encryptionService = new AESEncryptionService();
        testDataBuilder = new TestDataBuilder();
    }

    @Test
    @DisplayName("Data encryption should complete within performance threshold")
    void shouldEncryptDataWithinPerformanceThreshold() {
        String data = "SensitiveFinancialData123";
        String key = testDataBuilder.validEncryptionKey();

        Instant start = Instant.now();
        EncryptionResult result = encryptionService.encrypt(data, key);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(result.getEncryptedData()).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Data decryption should complete within performance threshold")
    void shouldDecryptDataWithinPerformanceThreshold() {
        String data = "SensitiveFinancialData123";
        String key = testDataBuilder.validEncryptionKey();

        EncryptionResult encrypted = encryptionService.encrypt(data, key);

        Instant start = Instant.now();
        String decrypted = encryptionService.decrypt(encrypted, key);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(decrypted).isEqualTo(data);
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Turkish character encryption should not impact performance")
    void shouldHandleTurkishCharactersWithoutPerformanceImpact() {
        String turkishData = "Türkçe karakterler: çğıöşüÇĞIİÖŞÜ";
        String englishData = "English characters: abcdefghijklmnop";
        String key = testDataBuilder.validEncryptionKey();

        Instant start1 = Instant.now();
        encryptionService.encrypt(turkishData, key);
        Duration turkishDuration = Duration.between(start1, Instant.now());

        Instant start2 = Instant.now();
        encryptionService.encrypt(englishData, key);
        Duration englishDuration = Duration.between(start2, Instant.now());

        assertThat(turkishDuration).isLessThan(englishDuration.multipliedBy(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000, 100000})
    @DisplayName("Encryption performance should scale with data size")
    void shouldMaintainPerformanceWithVariousDataSizes(int dataSize) {
        StringBuilder dataBuilder = new StringBuilder();
        for (int i = 0; i < dataSize; i++) {
            dataBuilder.append("x");
        }
        String data = dataBuilder.toString();
        String key = testDataBuilder.validEncryptionKey();

        Instant start = Instant.now();
        EncryptionResult result = encryptionService.encrypt(data, key);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();

        Duration maxExpectedDuration = Duration.ofMillis(dataSize / 1000 + 100);
        assertThat(duration).isLessThan(maxExpectedDuration);
    }

    @Test
    @DisplayName("Concurrent encryption should maintain performance")
    void shouldMaintainPerformanceUnderConcurrentEncryption() throws Exception {
        int concurrentOperations = 20;
        String key = testDataBuilder.validEncryptionKey();
        ExecutorService executor = Executors.newFixedThreadPool(concurrentOperations);

        List<CompletableFuture<Duration>> futures = IntStream.range(0, concurrentOperations)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                String data = "ConcurrentData" + i;

                Instant start = Instant.now();
                encryptionService.encrypt(data, key);
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
            assertThat(duration).isLessThan(Duration.ofMillis(200)));
    }

    @Test
    @DisplayName("Bulk encryption should maintain performance")
    void shouldMaintainPerformanceForBulkEncryption() {
        int operationCount = 100;
        String key = testDataBuilder.validEncryptionKey();
        List<String> dataList = new ArrayList<>();

        for (int i = 0; i < operationCount; i++) {
            dataList.add("BulkData" + i);
        }

        Instant start = Instant.now();

        for (String data : dataList) {
            encryptionService.encrypt(data, key);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerOperation = duration.dividedBy(operationCount);

        assertThat(averagePerOperation).isLessThan(Duration.ofMillis(10));
        assertThat(duration).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("Key generation should complete within performance threshold")
    void shouldGenerateKeyWithinPerformanceThreshold() {
        Instant start = Instant.now();
        String key = encryptionService.generateKey();
        Duration duration = Duration.between(start, Instant.now());

        assertThat(key).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Hash generation should complete within performance threshold")
    void shouldGenerateHashWithinPerformanceThreshold() {
        String data = "DataToHash123";

        Instant start = Instant.now();
        String hash = encryptionService.generateHash(data);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(hash).isNotBlank();
        assertThat(duration).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("Financial data encryption should meet regulatory performance requirements")
    void shouldEncryptFinancialDataWithinRegulatoryRequirements() {
        String financialData = testDataBuilder.sensitiveFinancialData();
        String key = testDataBuilder.validEncryptionKey();

        Instant start = Instant.now();
        EncryptionResult result = encryptionService.encrypt(financialData, key);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Memory-intensive encryption should not degrade performance")
    void shouldMaintainPerformanceWithMemoryIntensiveOperations() {
        String largeData = "x".repeat(1000000); // 1MB of data
        String key = testDataBuilder.validEncryptionKey();

        Instant start = Instant.now();
        EncryptionResult result = encryptionService.encrypt(largeData, key);
        Duration duration = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Repeated encryption-decryption cycles should maintain performance")
    void shouldMaintainPerformanceForRepeatedCycles() {
        String data = "CyclicData123";
        String key = testDataBuilder.validEncryptionKey();
        int cycles = 50;

        Instant start = Instant.now();

        for (int i = 0; i < cycles; i++) {
            EncryptionResult encrypted = encryptionService.encrypt(data, key);
            String decrypted = encryptionService.decrypt(encrypted, key);
            assertThat(decrypted).isEqualTo(data);
        }

        Duration duration = Duration.between(start, Instant.now());
        Duration averagePerCycle = duration.dividedBy(cycles);

        assertThat(averagePerCycle).isLessThan(Duration.ofMillis(20));
        assertThat(duration).isLessThan(Duration.ofSeconds(2));
    }
}