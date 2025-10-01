package com.bisttrading.user.controller;

import com.bisttrading.core.security.dto.JwtResponse;
import com.bisttrading.core.security.dto.LoginRequest;
import com.bisttrading.core.security.dto.RegisterRequest;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.user.dto.*;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
 * Comprehensive integration tests for UserController REST endpoints.
 * Tests user management operations with real database and security configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("User Controller Integration Tests")
class UserControllerIntegrationTest {

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

    private String accessToken;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up database
        userRepository.deleteAll();

        // Register and login a test user to get access token
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), JwtResponse.class);

        accessToken = jwtResponse.getAccessToken();
        userId = jwtResponse.getUserId();
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void shouldGetUserProfileSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.emailVerified").isBoolean())
                .andExpect(jsonPath("$.phoneVerified").isBoolean())
                .andExpect(jsonPath("$.kycCompleted").isBoolean())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should fail to get profile without authentication")
    void shouldFailToGetProfileWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/profile"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserProfileSuccessfully() throws Exception {
        // Given
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value(updateRequest.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(updateRequest.getLastName()))
                .andExpect(jsonPath("$.phoneNumber").value(updateRequest.getPhoneNumber()));
    }

    @Test
    @DisplayName("Should update Turkish user profile with special characters")
    void shouldUpdateTurkishUserProfileWithSpecialCharacters() throws Exception {
        // Given
        UpdateProfileRequest turkishUpdate = TestDataBuilder.turkishUpdateProfileRequest();

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(turkishUpdate))
                .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value(turkishUpdate.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(turkishUpdate.getLastName()));
    }

    @Test
    @DisplayName("Should fail profile update with invalid phone number")
    void shouldFailProfileUpdateWithInvalidPhoneNumber() throws Exception {
        // Given
        UpdateProfileRequest invalidUpdate = TestDataBuilder.validUpdateProfileRequest();
        invalidUpdate.setPhoneNumber("+15551234567"); // US phone number

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() throws Exception {
        // Given
        ChangePasswordRequest changeRequest = TestDataBuilder.validChangePasswordRequest();

        // When & Then
        mockMvc.perform(put("/api/v1/users/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Şifreniz başarıyla değiştirildi")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should fail password change with wrong current password")
    void shouldFailPasswordChangeWithWrongCurrentPassword() throws Exception {
        // Given
        ChangePasswordRequest wrongPasswordRequest = TestDataBuilder.validChangePasswordRequest();
        wrongPasswordRequest.setCurrentPassword("WrongPassword123");

        // When & Then
        mockMvc.perform(put("/api/v1/users/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Mevcut şifre hatalı")));
    }

    @Test
    @DisplayName("Should send email verification successfully")
    void shouldSendEmailVerificationSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-email")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("E-posta doğrulama kodu gönderildi")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should confirm email verification successfully")
    void shouldConfirmEmailVerificationSuccessfully() throws Exception {
        // Given - First send verification
        mockMvc.perform(post("/api/v1/users/verify-email")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Get the verification code from database (in real test, this would be mocked)
        UserEntity user = userRepository.findById(userId).orElseThrow();
        String verificationCode = user.getEmailVerificationCode();

        VerifyEmailRequest verifyRequest = VerifyEmailRequest.builder()
                .verificationCode(verificationCode)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-email/confirm")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("E-posta adresiniz başarıyla doğrulandı")));
    }

    @Test
    @DisplayName("Should fail email verification with wrong code")
    void shouldFailEmailVerificationWithWrongCode() throws Exception {
        // Given
        VerifyEmailRequest wrongCodeRequest = VerifyEmailRequest.builder()
                .verificationCode("wrong123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-email/confirm")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongCodeRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Geçersiz doğrulama kodu")));
    }

    @Test
    @DisplayName("Should send phone verification SMS successfully")
    void shouldSendPhoneVerificationSmsSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-phone")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("SMS doğrulama kodu gönderildi")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @ParameterizedTest
    @DisplayName("Should send phone verification for various Turkish phone formats")
    @ValueSource(strings = {
        "+905551234567",
        "+905321234567",
        "+905431234567"
    })
    void shouldSendPhoneVerificationForVariousTurkishPhoneFormats(String phoneNumber) throws Exception {
        // Given - Update user's phone number
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();
        updateRequest.setPhoneNumber(phoneNumber);

        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-phone")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("SMS doğrulama kodu gönderildi")));
    }

    @Test
    @DisplayName("Should confirm phone verification successfully")
    void shouldConfirmPhoneVerificationSuccessfully() throws Exception {
        // Given - First send verification
        mockMvc.perform(post("/api/v1/users/verify-phone")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Get the verification code from database
        UserEntity user = userRepository.findById(userId).orElseThrow();
        String verificationCode = user.getPhoneVerificationCode();

        VerifyPhoneRequest verifyRequest = VerifyPhoneRequest.builder()
                .verificationCode(verificationCode)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users/verify-phone/confirm")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Telefon numaranız başarıyla doğrulandı")));
    }

    @Test
    @DisplayName("Should get active sessions successfully")
    void shouldGetActiveSessionsSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/sessions")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.number").isNumber())
                .andExpect(jsonPath("$.size").isNumber());
    }

    @Test
    @DisplayName("Should get active sessions with pagination")
    void shouldGetActiveSessionsWithPagination() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/sessions")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Should terminate all other sessions successfully")
    void shouldTerminateAllOtherSessionsSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/sessions/terminate-all")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Tüm diğer oturumlar sonlandırıldı")))
                .andExpect(jsonPath("$.terminatedCount").isNumber())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should deactivate account successfully")
    void shouldDeactivateAccountSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/deactivate")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Hesabınız başarıyla deaktive edildi")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle concurrent profile updates")
    void shouldHandleConcurrentProfileUpdates() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<Integer>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    UpdateProfileRequest request = TestDataBuilder.ConcurrentScenarios.multipleProfileUpdates().get(index % 3);

                    MvcResult result = mockMvc.perform(put("/api/v1/users/profile")
                            .header("Authorization", "Bearer " + accessToken)
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
    @DisplayName("Should handle timezone correctly in Istanbul timezone")
    void shouldHandleTimezoneCorrectlyInIstanbulTimezone() throws Exception {
        // Given
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();

        // When
        MvcResult result = mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - Verify response contains proper timestamps
        String responseBody = result.getResponse().getContentAsString();
        UserProfileDto profile = objectMapper.readValue(responseBody, UserProfileDto.class);

        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should validate request content type and character encoding for Turkish")
    void shouldValidateRequestContentTypeAndCharacterEncodingForTurkish() throws Exception {
        // Given
        UpdateProfileRequest turkishUpdate = TestDataBuilder.turkishUpdateProfileRequest();

        // When & Then - Test with proper UTF-8 encoding
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(turkishUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully in profile update")
    void shouldHandleMalformedJsonGracefullyInProfileUpdate() throws Exception {
        // Given
        String malformedJson = "{\"firstName\": \"Test\", \"lastName\": }";

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail all operations without authentication")
    void shouldFailAllOperationsWithoutAuthentication() throws Exception {
        // When & Then - Test various endpoints without authentication
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/verify-email"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/verify-phone"))
                .andExpected(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/sessions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle client IP extraction in user operations")
    void shouldHandleClientIpExtractionInUserOperations() throws Exception {
        // Given
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();

        // When & Then - Test with various IP headers
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18")
                .header("X-Real-IP", "203.0.113.100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        ChangePasswordRequest passwordRequest = TestDataBuilder.validChangePasswordRequest();

        mockMvc.perform(put("/api/v1/users/password")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Forwarded-For", "198.51.100.178")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk());
    }
}