package com.bisttrading.core.security.test;

import com.bisttrading.core.security.dto.*;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Test data builder for security module tests.
 * Provides consistent test data with Turkish localization support.
 */
@UtilityClass
public class TestDataBuilder {

    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    /**
     * Creates a valid RegisterRequest with Turkish data.
     */
    public static RegisterRequest.RegisterRequestBuilder validRegisterRequest() {
        return RegisterRequest.builder()
            .email("test@example.com")
            .username("testkullanici")
            .password("GüvenliŞifre123")
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .phoneNumber("+905551234567")
            .tcKimlik("12345678901");
    }

    /**
     * Creates a valid RegisterRequest with Turkish characters.
     */
    public static RegisterRequest.RegisterRequestBuilder turkishRegisterRequest() {
        return RegisterRequest.builder()
            .email("türkçe@example.com")
            .username("çağlarşık")
            .password("TürkçeŞifre123İĞÜ")
            .firstName("Çağlar")
            .lastName("Şıktırık")
            .phoneNumber("+905559876543")
            .tcKimlik("98765432109");
    }

    /**
     * Creates a valid LoginRequest.
     */
    public static LoginRequest.LoginRequestBuilder validLoginRequest() {
        return LoginRequest.builder()
            .emailOrUsername("test@example.com")
            .password("GüvenliŞifre123");
    }

    /**
     * Creates a LoginRequest with Turkish characters.
     */
    public static LoginRequest.LoginRequestBuilder turkishLoginRequest() {
        return LoginRequest.builder()
            .emailOrUsername("çağlarşık")
            .password("TürkçeŞifre123İĞÜ");
    }

    /**
     * Creates a valid JwtResponse.
     */
    public static JwtResponse.JwtResponseBuilder validJwtResponse() {
        return JwtResponse.builder()
            .accessToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.signature")
            .refreshToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidHlwIjoicmVmcmVzaCJ9.signature")
            .tokenType("Bearer")
            .expiresIn(900L)
            .userId(UUID.randomUUID().toString())
            .username("test@example.com")
            .authorities(List.of("ROLE_USER"));
    }

    /**
     * Creates a RefreshTokenRequest.
     */
    public static RefreshTokenRequest.RefreshTokenRequestBuilder validRefreshTokenRequest() {
        return RefreshTokenRequest.builder()
            .refreshToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidHlwIjoicmVmcmVzaCJ9.signature");
    }

    /**
     * Creates a LogoutRequest.
     */
    public static LogoutRequest.LogoutRequestBuilder validLogoutRequest() {
        return LogoutRequest.builder()
            .accessToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.signature")
            .refreshToken("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidHlwIjoicmVmcmVzaCJ9.signature");
    }

    /**
     * Creates test data for invalid registration scenarios.
     */
    public static class InvalidRegistrations {

        public static RegisterRequest invalidEmail() {
            return validRegisterRequest()
                .email("geçersiz-email")
                .build();
        }

        public static RegisterRequest invalidTcKimlik() {
            return validRegisterRequest()
                .tcKimlik("12345") // Invalid format
                .build();
        }

        public static RegisterRequest invalidPhoneNumber() {
            return validRegisterRequest()
                .phoneNumber("555-123-456") // Invalid Turkish format
                .build();
        }

        public static RegisterRequest weakPassword() {
            return validRegisterRequest()
                .password("weak") // Too weak
                .build();
        }

        public static RegisterRequest emptyFields() {
            return RegisterRequest.builder()
                .email("")
                .username("")
                .password("")
                .firstName("")
                .lastName("")
                .build();
        }
    }

    /**
     * Creates test data for Turkish character scenarios.
     */
    public static class TurkishCharacters {

        public static final String[] TURKISH_NAMES = {
            "Çağlar", "Gülşah", "Ömer", "Şeyma", "İbrahim", "Ğülizar"
        };

        public static final String[] TURKISH_SURNAMES = {
            "Çelik", "Öztürk", "Şahin", "Güneş", "İnanç", "Ğökçe"
        };

        public static final String[] TURKISH_PASSWORDS = {
            "TürkçeŞifre123", "GüvenliParola456", "SağlamKod789", "İyiŞifre012"
        };

        public static final String[] TURKISH_USERNAMES = {
            "çağlar123", "gülşah456", "ömer789", "şeyma012"
        };

        public static RegisterRequest allTurkishCharacters() {
            return RegisterRequest.builder()
                .email("çğıöşü@example.com")
                .username("çğıöşükullanıcı")
                .password("ÇĞIİÖŞÜşifre123")
                .firstName("Çağlar")
                .lastName("Şıktırıkoğlu")
                .phoneNumber("+905559876543")
                .tcKimlik("98765432109")
                .build();
        }
    }

    /**
     * Creates test data for timezone scenarios in Istanbul.
     */
    public static class TimezoneScenarios {

        public static LocalDateTime istanbulNow() {
            return LocalDateTime.now(ISTANBUL_ZONE);
        }

        public static LocalDateTime istanbulMidnight() {
            return LocalDateTime.now(ISTANBUL_ZONE)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        }

        public static LocalDateTime summerTime() {
            // July 1st, 2024 noon in Istanbul (UTC+3)
            return LocalDateTime.of(2024, 7, 1, 12, 0, 0);
        }

        public static LocalDateTime winterTime() {
            // January 1st, 2024 noon in Istanbul (UTC+3)
            return LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        }

        public static Instant toInstant(LocalDateTime localDateTime) {
            return localDateTime.atZone(ISTANBUL_ZONE).toInstant();
        }
    }

    /**
     * Creates test data for concurrent access scenarios.
     */
    public static class ConcurrentScenarios {

        public static RegisterRequest[] multipleUsers(int count) {
            RegisterRequest[] users = new RegisterRequest[count];
            for (int i = 0; i < count; i++) {
                users[i] = validRegisterRequest()
                    .email("user" + i + "@example.com")
                    .username("user" + i)
                    .tcKimlik(generateValidTcKimlik(i))
                    .phoneNumber("+9055512345" + String.format("%02d", i))
                    .build();
            }
            return users;
        }

        public static LoginRequest[] multipleLogins(String[] usernames) {
            LoginRequest[] logins = new LoginRequest[usernames.length];
            for (int i = 0; i < usernames.length; i++) {
                logins[i] = validLoginRequest()
                    .emailOrUsername(usernames[i])
                    .build();
            }
            return logins;
        }

        private static String generateValidTcKimlik(int index) {
            // Generate different valid TC Kimlik numbers for testing
            String[] validTcKimliks = {
                "12345678901", "98765432109", "11111111110", "22222222220",
                "33333333330", "44444444440", "55555555550", "66666666660",
                "77777777770", "88888888880"
            };
            return validTcKimliks[index % validTcKimliks.length];
        }
    }

    /**
     * Creates test data for edge cases and boundary conditions.
     */
    public static class EdgeCases {

        public static RegisterRequest maxLengthFields() {
            return validRegisterRequest()
                .email("a".repeat(64) + "@" + "b".repeat(60) + ".com")
                .username("u".repeat(50))
                .firstName("f".repeat(50))
                .lastName("l".repeat(50))
                .build();
        }

        public static RegisterRequest minLengthFields() {
            return validRegisterRequest()
                .email("a@b.co")
                .username("ab")
                .firstName("Al")
                .lastName("By")
                .build();
        }

        public static RegisterRequest specialCharacters() {
            return validRegisterRequest()
                .firstName("Al-Mehmet")
                .lastName("O'Connor")
                .build();
        }
    }

    /**
     * JWT token test data with various scenarios.
     */
    public static class JwtTokens {

        public static final String VALID_ACCESS_TOKEN =
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjQwOTk1MjAwLCJleHAiOjE2NDA5OTYxMDB9.signature";

        public static final String VALID_REFRESH_TOKEN =
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidHlwIjoicmVmcmVzaCIsImlhdCI6MTY0MDk5NTIwMCwiZXhwIjoxNjQxNjAwMDAwfQ.signature";

        public static final String EXPIRED_TOKEN =
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjQwODAwMDAwLCJleHAiOjE2NDA4MDA5MDB9.signature";

        public static final String MALFORMED_TOKEN = "not.a.valid.jwt.token";

        public static final String EMPTY_TOKEN = "";

        public static final String NULL_TOKEN = null;
    }

    /**
     * Error scenarios for testing exception handling.
     */
    public static class ErrorScenarios {

        public static RegisterRequest duplicateEmail() {
            return validRegisterRequest()
                .email("existing@example.com")
                .build();
        }

        public static RegisterRequest duplicateUsername() {
            return validRegisterRequest()
                .username("existinguser")
                .build();
        }

        public static RegisterRequest duplicateTcKimlik() {
            return validRegisterRequest()
                .tcKimlik("11111111110") // Assume this exists
                .build();
        }

        public static LoginRequest nonExistentUser() {
            return validLoginRequest()
                .emailOrUsername("nonexistent@example.com")
                .build();
        }

        public static LoginRequest wrongPassword() {
            return validLoginRequest()
                .password("WrongPassword123")
                .build();
        }
    }
}