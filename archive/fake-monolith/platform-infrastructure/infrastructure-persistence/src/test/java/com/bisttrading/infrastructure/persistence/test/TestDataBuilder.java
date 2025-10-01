package com.bisttrading.infrastructure.persistence.test;

import com.bisttrading.infrastructure.persistence.entity.*;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test data builder for persistence module tests.
 * Provides consistent test data with Turkish localization and financial precision.
 */
@UtilityClass
public class TestDataBuilder {

    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    /**
     * Creates a UserEntity builder with Turkish test data.
     */
    public static UserEntity.UserEntityBuilder validUser() {
        return UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .email("test@example.com")
            .username("testkullanici")
            .passwordHash("$2a$12$hashedpassword")
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .phoneNumber("+905551234567")
            .tcKimlikNo("12345678901")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .address("Atatürk Mahallesi, İstiklal Caddesi No:123")
            .city("İstanbul")
            .postalCode("34000")
            .country("Türkiye")
            .status(UserEntity.UserStatus.ACTIVE)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .kycLevel(UserEntity.KycLevel.BASIC)
            .professionalInvestor(false)
            .riskProfile(UserEntity.RiskProfile.MODERATE)
            .investmentExperience(UserEntity.InvestmentExperience.BEGINNER)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
            .updatedAt(LocalDateTime.now(ISTANBUL_ZONE));
    }

    /**
     * Creates a UserEntity with Turkish characters.
     */
    public static UserEntity.UserEntityBuilder turkishUser() {
        return validUser()
            .email("çağlar@example.com")
            .username("çağlarşık")
            .firstName("Çağlar")
            .lastName("Şıktırıkoğlu")
            .city("İstanbul");
    }

    /**
     * Creates a professional investor user.
     */
    public static UserEntity.UserEntityBuilder professionalInvestor() {
        return validUser()
            .email("professional@example.com")
            .username("proinvestor")
            .firstName("Mehmet")
            .lastName("Güven")
            .professionalInvestor(true)
            .riskProfile(UserEntity.RiskProfile.AGGRESSIVE)
            .investmentExperience(UserEntity.InvestmentExperience.EXPERT)
            .kycLevel(UserEntity.KycLevel.ENHANCED);
    }

    /**
     * Creates an inactive user.
     */
    public static UserEntity.UserEntityBuilder inactiveUser() {
        return validUser()
            .status(UserEntity.UserStatus.INACTIVE)
            .emailVerified(false)
            .phoneVerified(false)
            .kycCompleted(false);
    }

    /**
     * Creates a locked user account.
     */
    public static UserEntity.UserEntityBuilder lockedUser() {
        return validUser()
            .failedLoginAttempts(5)
            .accountLockedUntil(LocalDateTime.now(ISTANBUL_ZONE).plusHours(1));
    }

    /**
     * Creates an OrganizationEntity builder.
     */
    public static OrganizationEntity.OrganizationEntityBuilder validOrganization() {
        return OrganizationEntity.builder()
            .id(UUID.randomUUID().toString())
            .name("BIST Test Yatırım A.Ş.")
            .tradeName("BIST Test")
            .taxNumber("1234567890")
            .mersisNumber("0123456789012345")
            .type(OrganizationEntity.OrganizationType.BROKERAGE)
            .status(OrganizationEntity.OrganizationStatus.ACTIVE)
            .licenseNumber("SPK-12345")
            .licenseType("Aracı Kurum Lisansı")
            .licenseValidUntil(LocalDate.now().plusYears(5))
            .contactEmail("info@bisttest.com")
            .contactPhone("+902123456789")
            .address("Maslak Mahallesi, Büyükdere Caddesi No:255")
            .city("İstanbul")
            .postalCode("34398")
            .country("Türkiye")
            .maxUserCapacity(1000)
            .currentUserCount(0)
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
            .updatedAt(LocalDateTime.now(ISTANBUL_ZONE));
    }

    /**
     * Creates an organization with Turkish characters.
     */
    public static OrganizationEntity.OrganizationEntityBuilder turkishOrganization() {
        return validOrganization()
            .name("Türkiye Güvenilir Yatırım A.Ş.")
            .tradeName("TGY Aracılık")
            .address("Çamlıca Mahallesi, Gökçe Sokağı No:67")
            .contactEmail("bilgi@türkiyeguvenilir.com.tr");
    }

    /**
     * Creates a UserSessionEntity builder.
     */
    public static UserSessionEntity.UserSessionEntityBuilder validSession() {
        return UserSessionEntity.builder()
            .id(UUID.randomUUID().toString())
            .userId(UUID.randomUUID().toString())
            .sessionToken(UUID.randomUUID().toString())
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .deviceType(UserSessionEntity.DeviceType.DESKTOP)
            .securityLevel(UserSessionEntity.SecurityLevel.MEDIUM)
            .status(UserSessionEntity.SessionStatus.ACTIVE)
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
            .lastActivityAt(LocalDateTime.now(ISTANBUL_ZONE));
    }

    /**
     * Creates a mobile session.
     */
    public static UserSessionEntity.UserSessionEntityBuilder mobileSession() {
        return validSession()
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)")
            .deviceType(UserSessionEntity.DeviceType.MOBILE)
            .securityLevel(UserSessionEntity.SecurityLevel.HIGH);
    }

    /**
     * Creates an ended session.
     */
    public static UserSessionEntity.UserSessionEntityBuilder endedSession() {
        return validSession()
            .status(UserSessionEntity.SessionStatus.ENDED)
            .endedAt(LocalDateTime.now(ISTANBUL_ZONE));
    }

    /**
     * Creates a BrokerEntity builder.
     */
    public static BrokerEntity.BrokerEntityBuilder validBroker() {
        return BrokerEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("ALGOLAB")
            .name("AlgoLab Teknoloji")
            .apiEndpoint("https://api.algolab.com.tr")
            .status(BrokerEntity.BrokerStatus.ACTIVE)
            .supportedMarkets(Map.of(
                "BIST", true,
                "FOREX", false,
                "CRYPTO", false
            ))
            .commissionRate(BigDecimal.valueOf(0.001)) // 0.1%
            .minCommission(BigDecimal.valueOf(5.00))
            .maxCommission(BigDecimal.valueOf(100.00))
            .connectionTimeout(30000)
            .requestTimeout(10000)
            .maxDailyRequests(100000)
            .currentRequestCount(0L)
            .lastHealthCheck(LocalDateTime.now(ISTANBUL_ZONE))
            .healthStatus(BrokerEntity.HealthStatus.HEALTHY)
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
            .updatedAt(LocalDateTime.now(ISTANBUL_ZONE));
    }

    /**
     * Creates a broker with Turkish name.
     */
    public static BrokerEntity.BrokerEntityBuilder turkishBroker() {
        return validBroker()
            .code("GARANTIBBVA")
            .name("Garanti BBVA Yatırım")
            .apiEndpoint("https://api.garantibbva.com.tr");
    }

    /**
     * Creates an inactive broker.
     */
    public static BrokerEntity.BrokerEntityBuilder inactiveBroker() {
        return validBroker()
            .status(BrokerEntity.BrokerStatus.INACTIVE)
            .healthStatus(BrokerEntity.HealthStatus.UNHEALTHY);
    }

    /**
     * Test data for financial calculations with precision.
     */
    public static class FinancialData {

        // High precision financial amounts
        public static final BigDecimal SMALL_COMMISSION = new BigDecimal("0.00125"); // 0.125%
        public static final BigDecimal STANDARD_COMMISSION = new BigDecimal("0.0025"); // 0.25%
        public static final BigDecimal HIGH_COMMISSION = new BigDecimal("0.005"); // 0.5%

        // Turkish Lira amounts with proper precision
        public static final BigDecimal MIN_TRY_AMOUNT = new BigDecimal("1.00");
        public static final BigDecimal TYPICAL_TRY_AMOUNT = new BigDecimal("10000.50");
        public static final BigDecimal LARGE_TRY_AMOUNT = new BigDecimal("1000000.75");

        // USD amounts for international trading
        public static final BigDecimal MIN_USD_AMOUNT = new BigDecimal("0.01");
        public static final BigDecimal TYPICAL_USD_AMOUNT = new BigDecimal("1000.00");
        public static final BigDecimal LARGE_USD_AMOUNT = new BigDecimal("100000.00");

        public static BrokerEntity.BrokerEntityBuilder highCommissionBroker() {
            return validBroker()
                .commissionRate(HIGH_COMMISSION)
                .minCommission(new BigDecimal("10.00"))
                .maxCommission(new BigDecimal("500.00"));
        }

        public static BrokerEntity.BrokerEntityBuilder lowCommissionBroker() {
            return validBroker()
                .commissionRate(SMALL_COMMISSION)
                .minCommission(new BigDecimal("2.50"))
                .maxCommission(new BigDecimal("50.00"));
        }
    }

    /**
     * Test data for timezone scenarios.
     */
    public static class TimezoneScenarios {

        public static UserEntity userWithTimestamps() {
            LocalDateTime now = LocalDateTime.now(ISTANBUL_ZONE);
            return validUser()
                .createdAt(now)
                .updatedAt(now)
                .lastLoginAt(now.minusHours(2))
                .passwordChangedAt(now.minusDays(30))
                .emailVerifiedAt(now.minusDays(1))
                .phoneVerifiedAt(now.minusHours(3))
                .kycCompletedAt(now.minusHours(1))
                .build();
        }

        public static UserSessionEntity sessionWithTimestamps() {
            LocalDateTime now = LocalDateTime.now(ISTANBUL_ZONE);
            return validSession()
                .createdAt(now.minusHours(2))
                .lastActivityAt(now.minusMinutes(5))
                .build();
        }

        public static OrganizationEntity organizationWithExpiry() {
            return validOrganization()
                .licenseValidUntil(LocalDate.now().plusMonths(6))
                .createdAt(LocalDateTime.now(ISTANBUL_ZONE).minusYears(2))
                .build();
        }
    }

    /**
     * Test data for concurrent scenarios.
     */
    public static class ConcurrentScenarios {

        public static UserEntity[] multipleUsers(int count) {
            UserEntity[] users = new UserEntity[count];
            for (int i = 0; i < count; i++) {
                users[i] = validUser()
                    .id(UUID.randomUUID().toString())
                    .email("user" + i + "@example.com")
                    .username("user" + i)
                    .tcKimlik(generateValidTcKimlik(i))
                    .phoneNumber("+9055512345" + String.format("%02d", i))
                    .build();
            }
            return users;
        }

        public static UserSessionEntity[] multipleSessions(String userId, int count) {
            UserSessionEntity[] sessions = new UserSessionEntity[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = validSession()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .sessionToken(UUID.randomUUID().toString())
                    .ipAddress("192.168.1." + (100 + i))
                    .build();
            }
            return sessions;
        }

        private static String generateValidTcKimlik(int index) {
            String[] validTcKimliks = {
                "12345678901", "98765432109", "11111111110", "22222222220",
                "33333333330", "44444444440", "55555555550", "66666666660",
                "77777777770", "88888888880"
            };
            return validTcKimliks[index % validTcKimliks.length];
        }
    }

    /**
     * Test data with edge cases and boundary conditions.
     */
    public static class EdgeCases {

        public static UserEntity userWithMaxLengthFields() {
            return validUser()
                .email("a".repeat(64) + "@" + "b".repeat(60) + ".com")
                .firstName("f".repeat(50))
                .lastName("l".repeat(50))
                .address("a".repeat(255))
                .build();
        }

        public static UserEntity userWithMinLengthFields() {
            return validUser()
                .email("a@b.co")
                .firstName("Al")
                .lastName("By")
                .build();
        }

        public static UserEntity userWithAllNullOptionalFields() {
            return UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email("minimal@example.com")
                .username("minimal")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("Min")
                .lastName("User")
                .status(UserEntity.UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .kycCompleted(false)
                .professionalInvestor(false)
                .riskProfile(UserEntity.RiskProfile.CONSERVATIVE)
                .investmentExperience(UserEntity.InvestmentExperience.BEGINNER)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
                .updatedAt(LocalDateTime.now(ISTANBUL_ZONE))
                .build();
        }

        public static BrokerEntity brokerWithExtremeLimits() {
            Map<String, Object> extremeConfig = new HashMap<>();
            extremeConfig.put("maxPositions", 10000);
            extremeConfig.put("maxOrderSize", new BigDecimal("999999999.99"));
            extremeConfig.put("minOrderSize", new BigDecimal("0.01"));

            return validBroker()
                .commissionRate(new BigDecimal("0.00001")) // 0.001%
                .minCommission(new BigDecimal("0.01"))
                .maxCommission(new BigDecimal("999999.99"))
                .maxDailyRequests(1000000L)
                .configuration(extremeConfig)
                .build();
        }
    }

    /**
     * Test data for encryption scenarios.
     */
    public static class EncryptionScenarios {

        public static UserEntity userWithEncryptedData() {
            return validUser()
                .tcKimlikNo("12345678901") // Will be encrypted
                .phoneNumber("+905551234567") // Will be encrypted
                .address("Gizli Adres Bilgisi") // Will be encrypted
                .build();
        }

        public static UserEntity userWithTurkishEncryptedData() {
            return turkishUser()
                .tcKimlik("98765432109")
                .phoneNumber("+905559876543")
                .address("Çok Gizli Türkçe Adres Bilgisi")
                .build();
        }
    }
}