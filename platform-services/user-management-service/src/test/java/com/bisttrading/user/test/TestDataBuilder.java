package com.bisttrading.user.test;

import com.bisttrading.user.dto.*;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Test data builder for user management service tests.
 * Provides consistent test data with Turkish localization support.
 */
@UtilityClass
public class TestDataBuilder {

    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    /**
     * Creates a UserProfileDto builder with Turkish test data.
     */
    public static UserProfileDto.UserProfileDtoBuilder validUserProfile() {
        return UserProfileDto.builder()
            .id(UUID.randomUUID().toString())
            .email("test@example.com")
            .username("testkullanici")
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .phoneNumber("+905551234567")
            .tcKimlik("12345678901")
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
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
            .updatedAt(LocalDateTime.now(ISTANBUL_ZONE))
            .lastLoginAt(LocalDateTime.now(ISTANBUL_ZONE).minusHours(1))
            .lastLoginIp("192.168.1.100")
            .failedLoginAttempts(0);
    }

    /**
     * Creates a UserProfileDto with Turkish characters.
     */
    public static UserProfileDto.UserProfileDtoBuilder turkishUserProfile() {
        return validUserProfile()
            .email("çağlar@example.com")
            .username("çağlarşık")
            .firstName("Çağlar")
            .lastName("Şıktırıkoğlu")
            .address("Çamlıca Mahallesi, Gülşah Sokağı No:45")
            .city("İstanbul");
    }

    /**
     * Creates an UpdateProfileRequest builder.
     */
    public static UpdateProfileRequest.UpdateProfileRequestBuilder validUpdateProfileRequest() {
        return UpdateProfileRequest.builder()
            .firstName("Mehmet")
            .lastName("Güven")
            .phoneNumber("+905559876543")
            .dateOfBirth(LocalDate.of(1985, 8, 20))
            .address("Yeni Mahalle, Barış Caddesi No:67")
            .city("Ankara")
            .postalCode("06000")
            .country("Türkiye")
            .riskProfile(UserEntity.RiskProfile.AGGRESSIVE)
            .investmentExperience(UserEntity.InvestmentExperience.INTERMEDIATE);
    }

    /**
     * Creates an UpdateProfileRequest with Turkish characters.
     */
    public static UpdateProfileRequest.UpdateProfileRequestBuilder turkishUpdateProfileRequest() {
        return validUpdateProfileRequest()
            .firstName("Gülşah")
            .lastName("Çelik")
            .address("Çiçek Mahallesi, Gül Sokağı No:12")
            .city("İzmir");
    }

    /**
     * Creates a ChangePasswordRequest builder.
     */
    public static ChangePasswordRequest.ChangePasswordRequestBuilder validChangePasswordRequest() {
        return ChangePasswordRequest.builder()
            .currentPassword("EskiŞifre123")
            .newPassword("YeniGüvenliŞifre456")
            .confirmPassword("YeniGüvenliŞifre456");
    }

    /**
     * Creates a VerifyEmailRequest builder.
     */
    public static VerifyEmailRequest.VerifyEmailRequestBuilder validVerifyEmailRequest() {
        return VerifyEmailRequest.builder()
            .verificationCode("123456");
    }

    /**
     * Creates a VerifyPhoneRequest builder.
     */
    public static VerifyPhoneRequest.VerifyPhoneRequestBuilder validVerifyPhoneRequest() {
        return VerifyPhoneRequest.builder()
            .verificationCode("654321");
    }

    /**
     * Creates a UserSessionDto builder.
     */
    public static UserSessionDto.UserSessionDtoBuilder validUserSession() {
        return UserSessionDto.builder()
            .id(UUID.randomUUID().toString())
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .deviceType(com.bisttrading.infrastructure.persistence.entity.UserSessionEntity.DeviceType.DESKTOP)
            .location("İstanbul, Türkiye")
            .securityLevel(com.bisttrading.infrastructure.persistence.entity.UserSessionEntity.SecurityLevel.STANDARD)
            .status(com.bisttrading.infrastructure.persistence.entity.UserSessionEntity.SessionStatus.ACTIVE)
            .createdAt(LocalDateTime.now(ISTANBUL_ZONE).minusHours(2))
            .lastActivityAt(LocalDateTime.now(ISTANBUL_ZONE).minusMinutes(5))
            .isCurrent(false);
    }

    /**
     * Creates a mobile session.
     */
    public static UserSessionDto.UserSessionDtoBuilder mobileUserSession() {
        return validUserSession()
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)")
            .deviceType(com.bisttrading.infrastructure.persistence.entity.UserSessionEntity.DeviceType.MOBILE)
            .securityLevel(com.bisttrading.infrastructure.persistence.entity.UserSessionEntity.SecurityLevel.HIGH);
    }

    /**
     * Test data for invalid request scenarios.
     */
    public static class InvalidRequests {

        public static UpdateProfileRequest invalidTcKimlik() {
            return validUpdateProfileRequest()
                .tcKimlik("invalid-tc")
                .build();
        }

        public static UpdateProfileRequest invalidPhoneNumber() {
            return validUpdateProfileRequest()
                .phoneNumber("invalid-phone")
                .build();
        }

        public static UpdateProfileRequest emptyFirstName() {
            return validUpdateProfileRequest()
                .firstName("")
                .build();
        }

        public static UpdateProfileRequest tooLongAddress() {
            return validUpdateProfileRequest()
                .address("A".repeat(300)) // Exceeds max length
                .build();
        }

        public static ChangePasswordRequest invalidCurrentPassword() {
            return validChangePasswordRequest()
                .currentPassword("wrong-password")
                .build();
        }

        public static ChangePasswordRequest weakNewPassword() {
            return validChangePasswordRequest()
                .newPassword("weak")
                .confirmPassword("weak")
                .build();
        }

        public static ChangePasswordRequest passwordMismatch() {
            return validChangePasswordRequest()
                .newPassword("YeniŞifre123")
                .confirmPassword("FarklıŞifre456")
                .build();
        }

        public static VerifyEmailRequest invalidVerificationCode() {
            return validVerifyEmailRequest()
                .verificationCode("invalid")
                .build();
        }

        public static VerifyEmailRequest shortVerificationCode() {
            return validVerifyEmailRequest()
                .verificationCode("123")
                .build();
        }

        public static VerifyPhoneRequest emptyVerificationCode() {
            return validVerifyPhoneRequest()
                .verificationCode("")
                .build();
        }
    }

    /**
     * Test data for Turkish character scenarios.
     */
    public static class TurkishCharacters {

        public static final String[] TURKISH_FIRST_NAMES = {
            "Çağlar", "Gülşah", "Ömer", "Şeyma", "İbrahim", "Ğülizar"
        };

        public static final String[] TURKISH_LAST_NAMES = {
            "Çelik", "Öztürk", "Şahin", "Güneş", "İnanç", "Ğökçe"
        };

        public static final String[] TURKISH_CITIES = {
            "İstanbul", "Ankara", "İzmir", "Çanakkale", "Şanlıurfa", "Ğümüşhane"
        };

        public static final String[] TURKISH_ADDRESSES = {
            "Çamlıca Mahallesi, Gülşah Sokağı No:12",
            "Şişli Mahallesi, İnönü Caddesi No:45",
            "Çankaya Mahallesi, Atatürk Bulvarı No:78"
        };

        public static UpdateProfileRequest allTurkishCharacters() {
            return UpdateProfileRequest.builder()
                .firstName("Çağlar")
                .lastName("Şıktırıkoğlu")
                .address("Çiçek Mahallesi, Gölgelik Sokağı No:123")
                .city("İstanbul")
                .build();
        }

        public static UserProfileDto turkishUserWithAllFields() {
            return turkishUserProfile()
                .firstName("Çağlar")
                .lastName("Şıktırıkoğlu")
                .address("Çamlıca Mahallesi, Gülşah Sokağı No:45")
                .city("İstanbul")
                .build();
        }
    }

    /**
     * Test data for timezone scenarios.
     */
    public static class TimezoneScenarios {

        public static UserProfileDto userWithIstanbulTimestamps() {
            LocalDateTime now = LocalDateTime.now(ISTANBUL_ZONE);
            return validUserProfile()
                .createdAt(now.minusDays(30))
                .updatedAt(now.minusHours(2))
                .lastLoginAt(now.minusMinutes(30))
                .emailVerifiedAt(now.minusDays(29))
                .phoneVerifiedAt(now.minusDays(28))
                .kycCompletedAt(now.minusDays(1))
                .build();
        }

        public static UserSessionDto sessionWithIstanbulTimestamps() {
            LocalDateTime now = LocalDateTime.now(ISTANBUL_ZONE);
            return validUserSession()
                .createdAt(now.minusHours(3))
                .lastActivityAt(now.minusMinutes(10))
                .build();
        }

        public static UserProfileDto userWithExpiredVerifications() {
            LocalDateTime now = LocalDateTime.now(ISTANBUL_ZONE);
            return validUserProfile()
                .emailVerified(false)
                .phoneVerified(false)
                .createdAt(now.minusDays(7)) // Old enough for expired verifications
                .build();
        }
    }

    /**
     * Test data for concurrent scenarios.
     */
    public static class ConcurrentScenarios {

        public static UpdateProfileRequest[] multipleProfileUpdates(int count) {
            UpdateProfileRequest[] updates = new UpdateProfileRequest[count];
            for (int i = 0; i < count; i++) {
                updates[i] = validUpdateProfileRequest()
                    .firstName("User" + i)
                    .lastName("Surname" + i)
                    .phoneNumber("+9055512345" + String.format("%02d", i))
                    .build();
            }
            return updates;
        }

        public static ChangePasswordRequest[] multiplePasswordChanges(int count) {
            ChangePasswordRequest[] changes = new ChangePasswordRequest[count];
            for (int i = 0; i < count; i++) {
                String newPassword = "NewPassword" + i + "123";
                changes[i] = validChangePasswordRequest()
                    .newPassword(newPassword)
                    .confirmPassword(newPassword)
                    .build();
            }
            return changes;
        }

        public static UserSessionDto[] multipleSessions(int count) {
            UserSessionDto[] sessions = new UserSessionDto[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = validUserSession()
                    .id(UUID.randomUUID().toString())
                    .ipAddress("192.168.1." + (100 + i))
                    .build();
            }
            return sessions;
        }
    }

    /**
     * Test data for edge cases and boundary conditions.
     */
    public static class EdgeCases {

        public static UpdateProfileRequest maxLengthFields() {
            return validUpdateProfileRequest()
                .firstName("F".repeat(50))
                .lastName("L".repeat(50))
                .address("A".repeat(255))
                .city("C".repeat(100))
                .country("Co".repeat(50))
                .build();
        }

        public static UpdateProfileRequest minLengthFields() {
            return validUpdateProfileRequest()
                .firstName("Al")
                .lastName("By")
                .build();
        }

        public static UserProfileDto userWithNullOptionalFields() {
            return UserProfileDto.builder()
                .id(UUID.randomUUID().toString())
                .email("minimal@example.com")
                .username("minimal")
                .firstName("Min")
                .lastName("User")
                .status(UserEntity.UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .kycCompleted(false)
                .professionalInvestor(false)
                .riskProfile(UserEntity.RiskProfile.CONSERVATIVE)
                .investmentExperience(UserEntity.InvestmentExperience.BEGINNER)
                .createdAt(LocalDateTime.now(ISTANBUL_ZONE))
                .updatedAt(LocalDateTime.now(ISTANBUL_ZONE))
                .failedLoginAttempts(0)
                .build();
        }

        public static ChangePasswordRequest samePasswords() {
            return validChangePasswordRequest()
                .currentPassword("SamePassword123")
                .newPassword("SamePassword123")
                .confirmPassword("SamePassword123")
                .build();
        }

        public static VerifyEmailRequest leadingZeroCode() {
            return validVerifyEmailRequest()
                .verificationCode("012345")
                .build();
        }
    }

    /**
     * Test data for performance scenarios.
     */
    public static class PerformanceScenarios {

        public static UserProfileDto[] largeUserDataset(int count) {
            UserProfileDto[] users = new UserProfileDto[count];
            for (int i = 0; i < count; i++) {
                users[i] = validUserProfile()
                    .id(UUID.randomUUID().toString())
                    .email("user" + i + "@example.com")
                    .username("user" + i)
                    .firstName("User" + i)
                    .lastName("LastName" + i)
                    .build();
            }
            return users;
        }

        public static UpdateProfileRequest bulkUpdateRequest() {
            return validUpdateProfileRequest()
                .firstName("Updated")
                .lastName("User")
                .address("Bulk Update Address, Performance Test Street No:123")
                .build();
        }
    }

    /**
     * Test data for error scenarios.
     */
    public static class ErrorScenarios {

        public static UserProfileDto nonExistentUser() {
            return validUserProfile()
                .id("non-existent-user-id")
                .email("nonexistent@example.com")
                .build();
        }

        public static UpdateProfileRequest duplicatePhoneNumber() {
            return validUpdateProfileRequest()
                .phoneNumber("+905551111111") // Assume this already exists
                .build();
        }

        public static ChangePasswordRequest incorrectCurrentPassword() {
            return validChangePasswordRequest()
                .currentPassword("WrongCurrentPassword123")
                .build();
        }

        public static VerifyEmailRequest expiredVerificationCode() {
            return validVerifyEmailRequest()
                .verificationCode("999999") // Assume this has expired
                .build();
        }
    }
}