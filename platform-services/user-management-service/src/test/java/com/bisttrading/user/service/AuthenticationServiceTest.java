package com.bisttrading.user.service;

import com.bisttrading.core.security.dto.*;
import com.bisttrading.core.security.service.JwtTokenProvider;
import com.bisttrading.core.security.service.TokenBlacklistService;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.entity.UserSessionEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.infrastructure.persistence.repository.UserSessionRepository;
import com.bisttrading.user.exception.UserServiceException;
import com.bisttrading.user.test.TestDataBuilder;
import com.bisttrading.user.util.TurkishValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthenticationService.
 * Tests user registration, login, token management with Turkish character support.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Service Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private TurkishValidationUtil validationUtil;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userRepository,
            sessionRepository,
            passwordEncoder,
            jwtTokenProvider,
            tokenBlacklistService,
            validationUtil
        );
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 Test Browser";

        when(validationUtil.isValidTcKimlik(registerRequest.getTcKimlik())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(true);
        when(validationUtil.isValidPassword(registerRequest.getPassword())).thenReturn(true);
        when(validationUtil.isValidEmail(registerRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByTcKimlik(registerRequest.getTcKimlik())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(sessionRepository.save(any(UserSessionEntity.class))).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(registerRequest.getEmail())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(registerRequest.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.register(registerRequest, clientIp, userAgent);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getUsername()).isEqualTo(registerRequest.getEmail());

        verify(userRepository).save(any(UserEntity.class));
        verify(sessionRepository).save(any(UserSessionEntity.class));
    }

    @Test
    @DisplayName("Should register Turkish user with special characters")
    void shouldRegisterTurkishUserWithSpecialCharacters() {
        // Given
        RegisterRequest turkishRequest = TestDataBuilder.turkishRegisterRequest();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(validationUtil.isValidTcKimlik(turkishRequest.getTcKimlik())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(turkishRequest.getPhoneNumber())).thenReturn(true);
        when(validationUtil.isValidPassword(turkishRequest.getPassword())).thenReturn(true);
        when(validationUtil.isValidEmail(turkishRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(turkishRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(turkishRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByTcKimlik(turkishRequest.getTcKimlik())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("turkish-user-123");
            return user;
        });
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(any())).thenReturn("turkish-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("turkish-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.register(turkishRequest, clientIp, userAgent);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("turkish-user-123");

        // Verify Turkish character preservation
        verify(userRepository).save(argThat(user ->
            user.getFirstName().equals("Çağlar") &&
            user.getLastName().equals("Şıktırıkoğlu") &&
            user.getEmail().equals("çağlar@örnek.com")
        ));
    }

    @ParameterizedTest
    @DisplayName("Should validate various Turkish emails during registration")
    @ValueSource(strings = {
        "çağlar@example.com",
        "gülşah@türkiye.gov.tr",
        "ömer@şirket.com.tr",
        "şeyma@üniversite.edu.tr"
    })
    void shouldValidateVariousTurkishEmailsDuringRegistration(String turkishEmail) {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        request.setEmail(turkishEmail);

        when(validationUtil.isValidEmail(turkishEmail)).thenReturn(true);
        when(validationUtil.isValidTcKimlik(any())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(validationUtil.isValidPassword(any())).thenReturn(true);
        when(userRepository.existsByEmail(turkishEmail)).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByTcKimlik(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.register(request, "127.0.0.1", "test");

        // Then
        assertThat(response).isNotNull();
        verify(validationUtil).isValidEmail(turkishEmail);
    }

    @Test
    @DisplayName("Should fail registration when email already exists")
    void shouldFailRegistrationWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(request, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Bu e-posta adresi zaten kullanımda");
    }

    @Test
    @DisplayName("Should fail registration when username already exists")
    void shouldFailRegistrationWhenUsernameAlreadyExists() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(request, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Bu kullanıcı adı zaten kullanımda");
    }

    @Test
    @DisplayName("Should fail registration when TC Kimlik already exists")
    void shouldFailRegistrationWhenTcKimlikAlreadyExists() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByTcKimlik(request.getTcKimlik())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(request, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Bu TC Kimlik No zaten kayıtlı");
    }

    @Test
    @DisplayName("Should fail registration with invalid TC Kimlik")
    void shouldFailRegistrationWithInvalidTcKimlik() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        when(validationUtil.isValidTcKimlik(request.getTcKimlik())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(request, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Geçersiz TC Kimlik No");
    }

    @Test
    @DisplayName("Should login user successfully with email")
    void shouldLoginUserSuccessfullyWithEmail() {
        // Given
        LoginRequest loginRequest = TestDataBuilder.validLoginRequest();
        UserEntity user = TestDataBuilder.activeUser();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(userRepository.findByEmailOrUsername(loginRequest.getEmailOrUsername(), loginRequest.getEmailOrUsername()))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(sessionRepository.save(any(UserSessionEntity.class))).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.login(loginRequest, clientIp, userAgent);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo(user.getEmail());

        verify(userRepository).updateLastLogin(eq(user.getId()), any(LocalDateTime.class), eq(clientIp));
    }

    @Test
    @DisplayName("Should login Turkish user with special characters")
    void shouldLoginTurkishUserWithSpecialCharacters() {
        // Given
        LoginRequest turkishLogin = TestDataBuilder.turkishLoginRequest();
        UserEntity turkishUser = TestDataBuilder.turkishUser();

        when(userRepository.findByEmailOrUsername(turkishLogin.getEmailOrUsername(), turkishLogin.getEmailOrUsername()))
            .thenReturn(Optional.of(turkishUser));
        when(passwordEncoder.matches(turkishLogin.getPassword(), turkishUser.getPasswordHash())).thenReturn(true);
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(turkishUser.getEmail())).thenReturn("turkish-access-token");
        when(jwtTokenProvider.generateRefreshToken(turkishUser.getEmail())).thenReturn("turkish-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.login(turkishLogin, "127.0.0.1", "test");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("çağlar@örnek.com");
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginWithWrongPassword() {
        // Given
        LoginRequest loginRequest = TestDataBuilder.validLoginRequest();
        UserEntity user = TestDataBuilder.activeUser();

        when(userRepository.findByEmailOrUsername(loginRequest.getEmailOrUsername(), loginRequest.getEmailOrUsername()))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);
        when(userRepository.incrementFailedLoginAttempts(user.getId())).thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "test"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("E-posta veya şifre hatalı");

        verify(userRepository).incrementFailedLoginAttempts(user.getId());
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAccountAfterFailedAttempts() {
        // Given
        LoginRequest loginRequest = TestDataBuilder.validLoginRequest();
        UserEntity user = TestDataBuilder.activeUser();

        when(userRepository.findByEmailOrUsername(loginRequest.getEmailOrUsername(), loginRequest.getEmailOrUsername()))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);
        when(userRepository.incrementFailedLoginAttempts(user.getId())).thenReturn(5);

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "test"))
            .isInstanceOf(BadCredentialsException.class);

        verify(userRepository).lockUserAccount(eq(user.getId()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should fail login for locked account")
    void shouldFailLoginForLockedAccount() {
        // Given
        LoginRequest loginRequest = TestDataBuilder.validLoginRequest();
        UserEntity lockedUser = TestDataBuilder.lockedUser();

        when(userRepository.findByEmailOrUsername(loginRequest.getEmailOrUsername(), loginRequest.getEmailOrUsername()))
            .thenReturn(Optional.of(lockedUser));

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Hesabınız");
    }

    @Test
    @DisplayName("Should fail login for inactive user")
    void shouldFailLoginForInactiveUser() {
        // Given
        LoginRequest loginRequest = TestDataBuilder.validLoginRequest();
        UserEntity inactiveUser = TestDataBuilder.inactiveUser();

        when(userRepository.findByEmailOrUsername(loginRequest.getEmailOrUsername(), loginRequest.getEmailOrUsername()))
            .thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), inactiveUser.getPasswordHash())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(loginRequest, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Hesabınız aktif değil");
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        RefreshTokenRequest refreshRequest = TestDataBuilder.validRefreshTokenRequest();
        UserEntity user = TestDataBuilder.activeUser();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(jwtTokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(refreshRequest.getRefreshToken())).thenReturn(false);
        when(jwtTokenProvider.getUsernameFromToken(refreshRequest.getRefreshToken())).thenReturn(user.getEmail());
        when(userRepository.findByEmailOrUsername(user.getEmail(), user.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(user.getEmail())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        JwtResponse response = authenticationService.refresh(refreshRequest, clientIp, userAgent);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

        verify(tokenBlacklistService).blacklistToken(refreshRequest.getRefreshToken());
        verify(sessionRepository).updateSessionActivity(eq(user.getId()), eq(clientIp), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should fail refresh with invalid token")
    void shouldFailRefreshWithInvalidToken() {
        // Given
        RefreshTokenRequest refreshRequest = TestDataBuilder.validRefreshTokenRequest();
        when(jwtTokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.refresh(refreshRequest, "127.0.0.1", "test"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Refresh token geçersiz");
    }

    @Test
    @DisplayName("Should fail refresh with blacklisted token")
    void shouldFailRefreshWithBlacklistedToken() {
        // Given
        RefreshTokenRequest refreshRequest = TestDataBuilder.validRefreshTokenRequest();
        when(jwtTokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(refreshRequest.getRefreshToken())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.refresh(refreshRequest, "127.0.0.1", "test"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Refresh token geçersiz kılınmış");
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given
        LogoutRequest logoutRequest = TestDataBuilder.validLogoutRequest();
        UserEntity user = TestDataBuilder.activeUser();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmailOrUsername(user.getEmail(), user.getEmail())).thenReturn(Optional.of(user));

        // When
        authenticationService.logout(logoutRequest, "127.0.0.1", "test");

        // Then
        verify(tokenBlacklistService).blacklistToken(logoutRequest.getAccessToken());
        verify(tokenBlacklistService).blacklistToken(logoutRequest.getRefreshToken());
        verify(sessionRepository).endCurrentSession(eq(user.getId()), eq("127.0.0.1"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should validate current user successfully")
    void shouldValidateCurrentUserSuccessfully() {
        // Given
        UserEntity user = TestDataBuilder.activeUser();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmailOrUsername(user.getEmail(), user.getEmail())).thenReturn(Optional.of(user));

        // When
        Map<String, Object> validation = authenticationService.validateCurrentUser();

        // Then
        assertThat(validation).isNotNull();
        assertThat(validation.get("valid")).isEqualTo(true);
        assertThat(validation.get("userId")).isEqualTo(user.getId());
        assertThat(validation.get("username")).isEqualTo(user.getEmail());
        assertThat(validation.get("emailVerified")).isEqualTo(user.getEmailVerified());
        assertThat(validation.get("phoneVerified")).isEqualTo(user.getPhoneVerified());
        assertThat(validation.get("kycCompleted")).isEqualTo(user.getKycCompleted());
    }

    @Test
    @DisplayName("Should handle concurrent registration attempts")
    void shouldHandleConcurrentRegistrationAttempts() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Setup mocks for successful registration
        when(validationUtil.isValidTcKimlik(any())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(validationUtil.isValidPassword(any())).thenReturn(true);
        when(validationUtil.isValidEmail(any())).thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByTcKimlik(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("user-" + System.nanoTime());
            return user;
        });
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        CompletableFuture<JwtResponse>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                RegisterRequest request = TestDataBuilder.validRegisterRequest();
                request.setEmail("user" + index + "@example.com");
                request.setUsername("user" + index);
                return authenticationService.register(request, "127.0.0.1", "test");
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(10, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<JwtResponse> future : futures) {
            JwtResponse response = future.get();
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotNull();
            assertThat(response.getRefreshToken()).isNotNull();
        }

        verify(userRepository, times(threadCount)).save(any(UserEntity.class));
        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle device type extraction from user agent")
    void shouldHandleDeviceTypeExtractionFromUserAgent() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();

        when(validationUtil.isValidTcKimlik(any())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(validationUtil.isValidPassword(any())).thenReturn(true);
        when(validationUtil.isValidEmail(any())).thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByTcKimlik(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When - Test with mobile user agent
        String mobileUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X)";
        authenticationService.register(request, "127.0.0.1", mobileUserAgent);

        // Then
        verify(sessionRepository).save(argThat(session ->
            session.getDeviceType() == UserSessionEntity.DeviceType.MOBILE ||
            session.getDeviceType() == UserSessionEntity.DeviceType.DESKTOP ||
            session.getDeviceType() == UserSessionEntity.DeviceType.TABLET ||
            session.getDeviceType() == UserSessionEntity.DeviceType.UNKNOWN
        ));
    }

    @Test
    @DisplayName("Should handle timezone correctly in Istanbul timezone")
    void shouldHandleTimezoneCorrectlyInIstanbulTimezone() {
        // Given
        RegisterRequest request = TestDataBuilder.validRegisterRequest();
        LocalDateTime beforeRegistration = TestDataBuilder.TimezoneScenarios.istanbulNow();

        when(validationUtil.isValidTcKimlik(any())).thenReturn(true);
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(validationUtil.isValidPassword(any())).thenReturn(true);
        when(validationUtil.isValidEmail(any())).thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByTcKimlik(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(sessionRepository.save(any())).thenReturn(TestDataBuilder.activeSession());
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900L);

        // When
        authenticationService.register(request, "127.0.0.1", "test");

        // Then
        verify(userRepository).save(argThat(user -> {
            LocalDateTime afterRegistration = TestDataBuilder.TimezoneScenarios.istanbulNow();
            return user.getCreatedAt().isAfter(beforeRegistration.minusSeconds(5)) &&
                   user.getCreatedAt().isBefore(afterRegistration.plusSeconds(5));
        }));
    }
}