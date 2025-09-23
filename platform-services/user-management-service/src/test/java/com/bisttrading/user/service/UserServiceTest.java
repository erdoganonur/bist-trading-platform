package com.bisttrading.user.service;

import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.entity.UserSessionEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.infrastructure.persistence.repository.UserSessionRepository;
import com.bisttrading.user.dto.*;
import com.bisttrading.user.exception.UserServiceException;
import com.bisttrading.user.mapper.UserMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserService.
 * Tests user profile management, verification, and Turkish character handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TurkishValidationUtil validationUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository,
            sessionRepository,
            passwordEncoder,
            validationUtil,
            userMapper,
            emailService,
            smsService
        );
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void shouldGetUserProfileSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        UserProfileDto expectedProfile = TestDataBuilder.validUserProfile();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userMapper.toProfileDto(user)).thenReturn(expectedProfile);

        // When
        UserProfileDto actualProfile = userService.getUserProfile(username);

        // Then
        assertThat(actualProfile).isNotNull();
        assertThat(actualProfile).isEqualTo(expectedProfile);
        verify(userRepository).findByEmailOrUsername(username, username);
        verify(userMapper).toProfileDto(user);
    }

    @Test
    @DisplayName("Should get Turkish user profile with special characters")
    void shouldGetTurkishUserProfileWithSpecialCharacters() {
        // Given
        String turkishUsername = "çağlar@örnek.com";
        UserEntity turkishUser = TestDataBuilder.turkishUser();
        UserProfileDto turkishProfile = TestDataBuilder.turkishUserProfile();

        when(userRepository.findByEmailOrUsername(turkishUsername, turkishUsername)).thenReturn(Optional.of(turkishUser));
        when(userMapper.toProfileDto(turkishUser)).thenReturn(turkishProfile);

        // When
        UserProfileDto actualProfile = userService.getUserProfile(turkishUsername);

        // Then
        assertThat(actualProfile).isNotNull();
        assertThat(actualProfile.getFirstName()).isEqualTo("Çağlar");
        assertThat(actualProfile.getLastName()).isEqualTo("Şıktırıkoğlu");
        assertThat(actualProfile.getEmail()).isEqualTo("çağlar@örnek.com");
    }

    @Test
    @DisplayName("Should fail to get profile for non-existent user")
    void shouldFailToGetProfileForNonExistentUser() {
        // Given
        String username = "nonexistent@example.com";
        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserProfile(username))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("Kullanıcı bulunamadı");
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserProfileSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();
        UserProfileDto updatedProfile = TestDataBuilder.validUserProfile();
        String clientIp = "192.168.1.1";

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(validationUtil.isValidTurkishPhoneNumber(updateRequest.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(updateRequest.getPhoneNumber(), user.getId())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(updatedProfile);

        // When
        UserProfileDto result = userService.updateProfile(username, updateRequest, clientIp);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(updatedProfile);
        verify(userRepository).save(user);
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update Turkish user profile with special characters")
    void shouldUpdateTurkishUserProfileWithSpecialCharacters() {
        // Given
        String turkishUsername = "çağlar@örnek.com";
        UserEntity turkishUser = TestDataBuilder.turkishUser();
        UpdateProfileRequest turkishUpdate = TestDataBuilder.turkishUpdateProfileRequest();
        UserProfileDto updatedProfile = TestDataBuilder.turkishUserProfile();

        when(userRepository.findByEmailOrUsername(turkishUsername, turkishUsername)).thenReturn(Optional.of(turkishUser));
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.save(turkishUser)).thenReturn(turkishUser);
        when(userMapper.toProfileDto(turkishUser)).thenReturn(updatedProfile);

        // When
        UserProfileDto result = userService.updateProfile(turkishUsername, turkishUpdate, "127.0.0.1");

        // Then
        assertThat(result).isNotNull();
        verify(validationUtil).isValidTurkishPhoneNumber(turkishUpdate.getPhoneNumber());
    }

    @Test
    @DisplayName("Should fail update when TC Kimlik is changed")
    void shouldFailUpdateWhenTcKimlikIsChanged() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setTcKimlik("12345678901");

        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();
        updateRequest.setTcKimlik("98765432101"); // Different TC Kimlik

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(username, updateRequest, "127.0.0.1"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("TC Kimlik No değiştirilemez");
    }

    @Test
    @DisplayName("Should fail update when phone number is already used")
    void shouldFailUpdateWhenPhoneNumberIsAlreadyUsed() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(validationUtil.isValidTurkishPhoneNumber(updateRequest.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(updateRequest.getPhoneNumber(), user.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(username, updateRequest, "127.0.0.1"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Bu telefon numarası başka bir kullanıcı tarafından kullanılıyor");
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        ChangePasswordRequest changeRequest = TestDataBuilder.validChangePasswordRequest();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(changeRequest.getCurrentPassword(), user.getPasswordHash())).thenReturn(true);
        when(validationUtil.isValidPassword(changeRequest.getNewPassword())).thenReturn(true);
        when(passwordEncoder.matches(changeRequest.getNewPassword(), user.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode(changeRequest.getNewPassword())).thenReturn("new-encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.changePassword(username, changeRequest, clientIp, userAgent);

        // Then
        verify(userRepository).save(user);
        verify(emailService).sendPasswordChangeNotification(user.getEmail(), clientIp, userAgent);
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
        assertThat(user.getPasswordChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fail password change with wrong current password")
    void shouldFailPasswordChangeWithWrongCurrentPassword() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        ChangePasswordRequest changeRequest = TestDataBuilder.validChangePasswordRequest();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(changeRequest.getCurrentPassword(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(username, changeRequest, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Mevcut şifre hatalı");
    }

    @Test
    @DisplayName("Should fail password change when new password is same as current")
    void shouldFailPasswordChangeWhenNewPasswordIsSameAsCurrent() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        ChangePasswordRequest changeRequest = TestDataBuilder.validChangePasswordRequest();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(changeRequest.getCurrentPassword(), user.getPasswordHash())).thenReturn(true);
        when(validationUtil.isValidPassword(changeRequest.getNewPassword())).thenReturn(true);
        when(passwordEncoder.matches(changeRequest.getNewPassword(), user.getPasswordHash())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(username, changeRequest, "127.0.0.1", "test"))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Yeni şifre mevcut şifreden farklı olmalıdır");
    }

    @Test
    @DisplayName("Should send email verification successfully")
    void shouldSendEmailVerificationSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setEmailVerified(false);

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.sendEmailVerification(username);

        // Then
        verify(userRepository).save(user);
        verify(emailService).sendEmailVerificationCode(eq(user.getEmail()), anyString(), eq(user.getFirstName()));
        assertThat(user.getEmailVerificationCode()).isNotNull();
        assertThat(user.getEmailVerificationExpiry()).isNotNull();
    }

    @Test
    @DisplayName("Should fail to send email verification for already verified email")
    void shouldFailToSendEmailVerificationForAlreadyVerifiedEmail() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setEmailVerified(true);

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.sendEmailVerification(username))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("E-posta adresi zaten doğrulanmış");
    }

    @Test
    @DisplayName("Should verify email successfully")
    void shouldVerifyEmailSuccessfully() {
        // Given
        String username = "test@example.com";
        String verificationCode = "123456";
        UserEntity user = TestDataBuilder.activeUser();
        user.setEmailVerified(false);
        user.setEmailVerificationCode(verificationCode);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.verifyEmail(username, verificationCode);

        // Then
        verify(userRepository).updateEmailVerification(eq(user.getId()), eq(true), any(LocalDateTime.class));
        verify(userRepository).save(user);
        assertThat(user.getEmailVerificationCode()).isNull();
        assertThat(user.getEmailVerificationExpiry()).isNull();
    }

    @Test
    @DisplayName("Should fail email verification with wrong code")
    void shouldFailEmailVerificationWithWrongCode() {
        // Given
        String username = "test@example.com";
        String wrongCode = "wrong123";
        UserEntity user = TestDataBuilder.activeUser();
        user.setEmailVerified(false);
        user.setEmailVerificationCode("123456");
        user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.verifyEmail(username, wrongCode))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Geçersiz doğrulama kodu");
    }

    @Test
    @DisplayName("Should fail email verification with expired code")
    void shouldFailEmailVerificationWithExpiredCode() {
        // Given
        String username = "test@example.com";
        String verificationCode = "123456";
        UserEntity user = TestDataBuilder.activeUser();
        user.setEmailVerified(false);
        user.setEmailVerificationCode(verificationCode);
        user.setEmailVerificationExpiry(LocalDateTime.now().minusMinutes(10)); // Expired

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.verifyEmail(username, verificationCode))
            .isInstanceOf(UserServiceException.class)
            .hasMessageContaining("Doğrulama kodunun süresi dolmuş");
    }

    @Test
    @DisplayName("Should send phone verification SMS successfully")
    void shouldSendPhoneVerificationSmsSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setPhoneNumber("+905551234567");
        user.setPhoneVerified(false);

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.sendPhoneVerification(username);

        // Then
        verify(userRepository).save(user);
        verify(smsService).sendVerificationSms(eq(user.getPhoneNumber()), anyString(), eq(user.getFirstName()));
        assertThat(user.getPhoneVerificationCode()).isNotNull();
        assertThat(user.getPhoneVerificationExpiry()).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("Should send phone verification for various Turkish phone formats")
    @ValueSource(strings = {
        "+905551234567",
        "05551234567",
        "5551234567",
        "+90 555 123 45 67"
    })
    void shouldSendPhoneVerificationForVariousTurkishPhoneFormats(String phoneNumber) {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setPhoneNumber(phoneNumber);
        user.setPhoneVerified(false);

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.sendPhoneVerification(username);

        // Then
        verify(smsService).sendVerificationSms(eq(phoneNumber), anyString(), eq(user.getFirstName()));
    }

    @Test
    @DisplayName("Should verify phone successfully")
    void shouldVerifyPhoneSuccessfully() {
        // Given
        String username = "test@example.com";
        String verificationCode = "123456";
        UserEntity user = TestDataBuilder.activeUser();
        user.setPhoneVerified(false);
        user.setPhoneVerificationCode(verificationCode);
        user.setPhoneVerificationExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.verifyPhone(username, verificationCode);

        // Then
        verify(userRepository).updatePhoneVerification(eq(user.getId()), eq(true), any(LocalDateTime.class));
        verify(userRepository).save(user);
        assertThat(user.getPhoneVerificationCode()).isNull();
        assertThat(user.getPhoneVerificationExpiry()).isNull();
    }

    @Test
    @DisplayName("Should get active sessions successfully")
    void shouldGetActiveSessionsSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        Pageable pageable = PageRequest.of(0, 10);
        List<UserSessionEntity> sessions = List.of(TestDataBuilder.activeSession());
        Page<UserSessionEntity> sessionPage = new PageImpl<>(sessions, pageable, 1);
        UserSessionDto sessionDto = TestDataBuilder.validUserSessionDto();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(sessionRepository.findActiveSessionsByUserId(user.getId(), pageable)).thenReturn(sessionPage);
        when(userMapper.toSessionDto(any(UserSessionEntity.class))).thenReturn(sessionDto);

        // When
        Page<UserSessionDto> result = userService.getActiveSessions(username, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sessionDto);
    }

    @Test
    @DisplayName("Should terminate session successfully")
    void shouldTerminateSessionSuccessfully() {
        // Given
        String username = "test@example.com";
        String sessionId = "session-123";
        UserEntity user = TestDataBuilder.activeUser();
        UserSessionEntity session = TestDataBuilder.activeSession();
        session.setId(sessionId);
        session.setUserId(user.getId());

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        // When
        userService.terminateSession(username, sessionId);

        // Then
        verify(sessionRepository).save(session);
        assertThat(session.getStatus()).isEqualTo(UserSessionEntity.SessionStatus.ENDED);
        assertThat(session.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should terminate all other sessions successfully")
    void shouldTerminateAllOtherSessionsSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        int expectedTerminatedCount = 3;

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(sessionRepository.endAllActiveSessionsExceptCurrent(eq(user.getId()), any(LocalDateTime.class)))
            .thenReturn(expectedTerminatedCount);

        // When
        int actualCount = userService.terminateAllOtherSessions(username);

        // Then
        assertThat(actualCount).isEqualTo(expectedTerminatedCount);
        verify(sessionRepository).endAllActiveSessionsExceptCurrent(eq(user.getId()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should deactivate account successfully")
    void shouldDeactivateAccountSuccessfully() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.deactivateAccount(username, clientIp, userAgent);

        // Then
        verify(userRepository).save(user);
        verify(sessionRepository).endAllActiveSessionsForUser(eq(user.getId()), any(LocalDateTime.class));
        verify(emailService).sendAccountDeactivationNotification(user.getEmail(), user.getFirstName());
        assertThat(user.getStatus()).isEqualTo(UserEntity.UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should handle concurrent profile updates")
    void shouldHandleConcurrentProfileUpdates() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        UserEntity user = TestDataBuilder.activeUser();

        when(userRepository.findByEmailOrUsername(any(), any())).thenReturn(Optional.of(user));
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toProfileDto(any())).thenReturn(TestDataBuilder.validUserProfile());

        // When
        CompletableFuture<UserProfileDto>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                UpdateProfileRequest request = TestDataBuilder.ConcurrentScenarios.multipleProfileUpdates().get(index % 3);
                return userService.updateProfile("test@example.com", request, "127.0.0.1");
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(10, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<UserProfileDto> future : futures) {
            UserProfileDto result = future.get();
            assertThat(result).isNotNull();
        }

        verify(userRepository, times(threadCount)).save(any(UserEntity.class));
        executor.shutdown();
    }

    @Test
    @DisplayName("Should reset phone verification when phone number changes")
    void shouldResetPhoneVerificationWhenPhoneNumberChanges() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        user.setPhoneNumber("+905551234567");
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now().minusDays(1));

        UpdateProfileRequest updateRequest = TestDataBuilder.validUpdateProfileRequest();
        updateRequest.setPhoneNumber("+905559876543"); // Different phone number

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(validationUtil.isValidTurkishPhoneNumber(updateRequest.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(updateRequest.getPhoneNumber(), user.getId())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(TestDataBuilder.validUserProfile());

        // When
        userService.updateProfile(username, updateRequest, "127.0.0.1");

        // Then
        assertThat(user.getPhoneVerified()).isFalse();
        assertThat(user.getPhoneVerifiedAt()).isNull();
        assertThat(user.getPhoneNumber()).isEqualTo("+905559876543");
    }

    @Test
    @DisplayName("Should handle timezone correctly in Istanbul timezone")
    void shouldHandleTimezoneCorrectlyInIstanbulTimezone() {
        // Given
        String username = "test@example.com";
        UserEntity user = TestDataBuilder.activeUser();
        LocalDateTime beforeUpdate = TestDataBuilder.TimezoneScenarios.istanbulNow();

        when(userRepository.findByEmailOrUsername(username, username)).thenReturn(Optional.of(user));
        when(validationUtil.isValidTurkishPhoneNumber(any())).thenReturn(true);
        when(userRepository.existsByPhoneNumberAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toProfileDto(user)).thenReturn(TestDataBuilder.validUserProfile());

        // When
        userService.updateProfile(username, TestDataBuilder.validUpdateProfileRequest(), "127.0.0.1");

        // Then
        LocalDateTime afterUpdate = TestDataBuilder.TimezoneScenarios.istanbulNow();
        assertThat(user.getUpdatedAt()).isAfter(beforeUpdate.minusSeconds(5));
        assertThat(user.getUpdatedAt()).isBefore(afterUpdate.plusSeconds(5));
    }
}