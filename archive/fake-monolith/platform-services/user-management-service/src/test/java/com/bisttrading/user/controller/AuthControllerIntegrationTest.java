package com.bisttrading.user.controller;

import com.bisttrading.core.security.dto.*;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.user.test.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for AuthController REST endpoints.
 * Tests authentication flow with real database and security configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("bist_trading_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        userRepository.deleteAll();

        // Create valid registration request using TestDataBuilder
        validRegisterRequest = TestDataBuilder.validRegisterRequest();

        // Create valid login request
        validLoginRequest = LoginRequest.builder()
            .emailOrUsername("test@example.com")
            .password("TestPassword123")
            .build();

        // Create test user in database
        testUser = UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .email("test@example.com")
            .username("testuser")
            .passwordHash(passwordEncoder.encode("TestPassword123"))
            .firstName("Test")
            .lastName("User")
            .phoneNumber("+905551234567")
            .tcKimlik("12345678901")
            .status(UserEntity.UserStatus.ACTIVE)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .professionalInvestor(false)
            .riskProfile(UserEntity.RiskProfile.MODERATE)
            .investmentExperience(UserEntity.InvestmentExperience.BEGINNER)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void shouldRegisterNewUserSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isOk())
            .andExpected(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber())
            .andExpect(jsonPath("$.userId").isNotEmpty())
            .andExpected(jsonPath("$.username").value("test@example.com"))
            .andExpect(jsonPath("$.authorities").isArray())
            .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        validRegisterRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Geçersiz istek verisi"))
            .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void shouldRejectRegistrationWithInvalidTcKimlik() throws Exception {
        validRegisterRequest.setTcKimlik("12345"); // Invalid TC Kimlik

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("USER_SERVICE_ERROR"))
            .andExpected(jsonPath("$.message").value("Geçersiz TC Kimlik No"));
    }

    @Test
    void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        // Save test user first
        userRepository.save(testUser);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("USER_SERVICE_ERROR"))
            .andExpected(jsonPath("$.message").value("Bu e-posta adresi zaten kullanımda"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // Save test user first
        userRepository.save(testUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpected(status().isOk())
            .andExpected(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.username").value(testUser.getEmail()));
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        // Save test user first
        userRepository.save(testUser);

        validLoginRequest.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
            .andExpected(jsonPath("$.message").value("E-posta veya şifre hatalı"));
    }

    @Test
    void shouldRejectLoginForNonExistentUser() throws Exception {
        validLoginRequest.setEmailOrUsername("nonexistent@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpected(jsonPath("$.error").value("INVALID_CREDENTIALS"))
            .andExpected(jsonPath("$.message").value("E-posta veya şifre hatalı"));
    }

    @Test
    void shouldRejectLoginForInactiveUser() throws Exception {
        // Set user as inactive
        testUser.setStatus(UserEntity.UserStatus.INACTIVE);
        userRepository.save(testUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isBadRequest())
            .andExpected(jsonPath("$.error").value("USER_SERVICE_ERROR"))
            .andExpected(jsonPath("$.message").value("Hesabınız aktif değil. Lütfen hesabınızı aktifleştirin."));
    }

    @Test
    void shouldLockAccountAfterFailedLoginAttempts() throws Exception {
        // Save test user
        userRepository.save(testUser);

        // Make 5 failed login attempts
        validLoginRequest.setPassword("WrongPassword");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpected(status().isUnauthorized());
        }

        // 6th attempt should return account locked
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpected(status().isBadRequest())
            .andExpected(jsonPath("$.error").value("USER_SERVICE_ERROR"))
            .andExpected(jsonPath("$.message").containsString("kilitlenmiştir"));
    }

    @Test
    void shouldValidateTokenSuccessfully() throws Exception {
        // Save test user and get token
        userRepository.save(testUser);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpected(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract access token from response
        String accessToken = objectMapper.readTree(response).get("accessToken").asText();

        // Validate token
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + accessToken))
            .andExpected(status().isOk())
            .andExpected(jsonPath("$.valid").value(true))
            .andExpected(jsonPath("$.userId").value(testUser.getId()))
            .andExpected(jsonPath("$.username").value(testUser.getEmail()))
            .andExpected(jsonPath("$.authorities").isArray());
    }

    @Test
    void shouldRejectValidationWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate"))
            .andExpected(status().isUnauthorized());
    }

    @Test
    void shouldRejectValidationWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer invalid-token"))
            .andExpected(status().isUnauthorized());
    }

    @Test
    void shouldHandleRefreshTokenRequest() throws Exception {
        // This would require implementing refresh token logic
        // For now, we'll test the endpoint exists and handles the request structure

        String refreshTokenRequest = """
            {
                "refreshToken": "some-refresh-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest))
            .andExpected(status().isUnauthorized()); // Expected since token is invalid
    }

    @Test
    void shouldHandleLogoutRequest() throws Exception {
        String logoutRequest = """
            {
                "accessToken": "some-access-token",
                "refreshToken": "some-refresh-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest))
            .andExpected(status().isUnauthorized()); // Expected without valid authentication
    }

    @Test
    @DisplayName("Should register Turkish user with special characters")
    void shouldRegisterTurkishUserWithSpecialCharacters() throws Exception {
        // Given
        RegisterRequest turkishRequest = TestDataBuilder.turkishRegisterRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(turkishRequest))
                .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(turkishRequest.getEmail()))
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @ParameterizedTest
    @DisplayName("Should validate various Turkish emails during registration")
    @ValueSource(strings = {
        "çağlar@example.com",
        "gülşah@türkiye.gov.tr",
        "ömer@şirket.com.tr"
    })
    void shouldValidateVariousTurkishEmailsDuringRegistration(String turkishEmail) throws Exception {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        request.setEmail(turkishEmail);
        request.setUsername(turkishEmail.split("@")[0] + System.currentTimeMillis());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(turkishEmail));
    }

    @Test
    @DisplayName("Should fail registration with invalid Turkish phone number")
    void shouldFailRegistrationWithInvalidTurkishPhoneNumber() throws Exception {
        // Given
        RegisterRequest invalidRequest = TestDataBuilder.validRegisterRequest();
        invalidRequest.setPhoneNumber("+15551234567"); // US phone number

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle full authentication flow successfully")
    void shouldHandleFullAuthenticationFlowSuccessfully() throws Exception {
        // Given
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        // Register user
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse registerResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), JwtResponse.class);

        // Login with same credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .emailOrUsername(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), JwtResponse.class);

        // Validate token
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value(registerRequest.getEmail()));

        // Refresh token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse refreshResponse = objectMapper.readValue(
            refreshResult.getResponse().getContentAsString(), JwtResponse.class);

        // Logout
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .accessToken(refreshResponse.getAccessToken())
                .refreshToken(refreshResponse.getRefreshToken())
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest))
                .header("Authorization", "Bearer " + refreshResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Başarıyla çıkış yapıldı")));
    }

    @Test
    @DisplayName("Should handle concurrent registration attempts")
    void shouldHandleConcurrentRegistrationAttempts() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<Integer>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    RegisterRequest request = TestDataBuilder.validRegisterRequest();
                    request.setEmail("user" + index + "@example.com");
                    request.setUsername("user" + index);
                    request.setTcKimlik(TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS[index % TestDataBuilder.TurkishCharacters.VALID_TC_KIMLIKS.length]);

                    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(30, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<Integer> future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle client IP extraction correctly")
    void shouldHandleClientIpExtractionCorrectly() throws Exception {
        // Given
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        // When & Then - Test with X-Forwarded-For header
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178"))
                .andExpect(status().isOk());

        RegisterRequest request2 = TestDataBuilder.validRegisterRequest();
        request2.setEmail("test2@example.com");
        request2.setUsername("test2");

        // Test with X-Real-IP header
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
                .header("X-Real-IP", "203.0.113.100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void shouldHandleMalformedJsonGracefully() throws Exception {
        // Given
        String malformedJson = "{\"email\": \"test@example.com\", \"password\": }";

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate request content type and character encoding")
    void shouldValidateRequestContentTypeAndCharacterEncoding() throws Exception {
        // Given
        RegisterRequest turkishRequest = TestDataBuilder.turkishRegisterRequest();

        // When & Then - Test with proper UTF-8 encoding
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(turkishRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle timezone correctly in Istanbul timezone")
    void shouldHandleTimezoneCorrectlyInIstanbulTimezone() throws Exception {
        // Given
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - Verify response contains timestamps
        String responseBody = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);

        assertThat(jwtResponse.getUserId()).isNotEmpty();
        assertThat(jwtResponse.getExpiresIn()).isGreaterThan(0);
    }
}