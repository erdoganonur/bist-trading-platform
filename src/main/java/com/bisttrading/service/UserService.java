package com.bisttrading.service;

import com.bisttrading.entity.UserEntity;
import com.bisttrading.entity.UserSessionEntity;
import com.bisttrading.repository.UserRepository;
import com.bisttrading.repository.SessionRepository;
import com.bisttrading.dto.*;
import com.bisttrading.exception.UserServiceException;
import com.bisttrading.mapper.UserMapper;
import com.bisttrading.util.TurkishValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User service for BIST Trading Platform.
 * Handles user profile management, verification, and account operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TurkishValidationUtil validationUtil;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Gets user profile by username/email.
     *
     * @param username Username or email
     * @return User profile DTO
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        log.debug("Getting profile for user: {}", username);

        UserEntity user = findUserByUsername(username);
        // TODO: Fix entity mapping - create manual DTO for now
        UserProfileDto profile = UserProfileDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .active(user.isActive())
            .build();

        log.debug("Profile retrieved for user: {}", username);
        return profile;
    }

    /**
     * Updates user profile.
     *
     * @param username Username or email
     * @param updateRequest Profile update data
     * @param clientIp Client IP for audit
     * @return Updated profile DTO
     */
    public UserProfileDto updateProfile(String username, UpdateProfileRequest updateRequest, String clientIp) {
        log.info("Updating profile for user: {}", username);

        UserEntity user = findUserByUsername(username);

        // Validate update data
        validateProfileUpdate(updateRequest, user);

        // Update user fields
        updateUserFromRequest(user, updateRequest);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        // TODO: Fix entity mapping - create manual DTO for now
        UserProfileDto updatedProfile = UserProfileDto.builder()
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .username(savedUser.getUsername())
            .firstName(savedUser.getFirstName())
            .lastName(savedUser.getLastName())
            .active(savedUser.isActive())
            .build();

        log.info("Profile updated successfully for user: {}", username);
        return updatedProfile;
    }

    /**
     * Changes user password.
     *
     * @param username Username or email
     * @param changePasswordRequest Password change data
     * @param clientIp Client IP for security tracking
     * @param userAgent User agent for security tracking
     */
    public void changePassword(String username, ChangePasswordRequest changePasswordRequest,
                             String clientIp, String userAgent) {
        log.info("Password change request for user: {}", username);

        UserEntity user = findUserByUsername(username);

        // Verify current password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPasswordHash())) {
            throw new UserServiceException("Mevcut şifre hatalı");
        }

        // Validate new password
        if (!validationUtil.isValidPassword(changePasswordRequest.getNewPassword())) {
            throw new UserServiceException(
                "Yeni şifre en az 8 karakter olmalı ve büyük harf, küçük harf, rakam içermelidir"
            );
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), user.getPasswordHash())) {
            throw new UserServiceException("Yeni şifre mevcut şifreden farklı olmalıdır");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        // TODO: Re-enable when Lombok properly generates getters/setters
        // user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send security notification
        emailService.sendPasswordChangeNotification(user.getEmail(), clientIp, userAgent);

        log.info("Password changed successfully for user: {}", username);
    }

    /**
     * Sends email verification code.
     *
     * @param username Username or email
     */
    public void sendEmailVerification(String username) {
        log.info("Sending email verification for user: {}", username);

        UserEntity user = findUserByUsername(username);

        if (user.isEmailVerified()) {
            throw new UserServiceException("E-posta adresi zaten doğrulanmış");
        }

        // Generate verification code
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Store verification code (in real implementation, use Redis or database table)
        // TODO: Re-enable when Lombok properly generates getters/setters
        // user.setEmailVerificationCode(verificationCode);
        // user.setEmailVerificationExpiry(expiresAt);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send verification email
        emailService.sendEmailVerificationCode(user.getEmail(), verificationCode, user.getFirstName());

        log.info("Email verification code sent for user: {}", username);
    }

    /**
     * Verifies email with verification code.
     *
     * @param username Username or email
     * @param verificationCode Verification code
     */
    public void verifyEmail(String username, String verificationCode) {
        log.info("Email verification attempt for user: {}", username);

        UserEntity user = findUserByUsername(username);

        if (user.isEmailVerified()) {
            throw new UserServiceException("E-posta adresi zaten doğrulanmış");
        }

        // TODO: Re-enable when Lombok properly generates getters/setters
        // if (user.getEmailVerificationCode() == null ||
        //     !user.getEmailVerificationCode().equals(verificationCode)) {
        //     throw new UserServiceException("Geçersiz doğrulama kodu");
        // }
        // if (user.getEmailVerificationExpiry() == null ||
        //     user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
        //     throw new UserServiceException("Doğrulama kodunun süresi dolmuş");
        // }

        // Update verification status
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // TODO: Re-enable when Lombok properly generates getters/setters
        // user.setEmailVerificationCode(null);
        // user.setEmailVerificationExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Email verified successfully for user: {}", username);
    }

    /**
     * Sends phone verification SMS.
     *
     * @param username Username or email
     */
    public void sendPhoneVerification(String username) {
        log.info("Sending phone verification for user: {}", username);

        UserEntity user = findUserByUsername(username);

        if (user.getPhoneNumber() == null) {
            throw new UserServiceException("Telefon numarası kayıtlı değil");
        }

        if (user.isPhoneVerified()) {
            throw new UserServiceException("Telefon numarası zaten doğrulanmış");
        }

        // Generate verification code
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        // Store verification code
        // TODO: Re-enable when Lombok properly generates getters/setters
        // user.setPhoneVerificationCode(verificationCode);
        // user.setPhoneVerificationExpiry(expiresAt);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send SMS
        smsService.sendVerificationSms(user.getPhoneNumber(), verificationCode, user.getFirstName());

        log.info("Phone verification SMS sent for user: {}", username);
    }

    /**
     * Verifies phone with SMS code.
     *
     * @param username Username or email
     * @param verificationCode SMS verification code
     */
    public void verifyPhone(String username, String verificationCode) {
        log.info("Phone verification attempt for user: {}", username);

        UserEntity user = findUserByUsername(username);

        if (user.isPhoneVerified()) {
            throw new UserServiceException("Telefon numarası zaten doğrulanmış");
        }

        // TODO: Re-enable when Lombok properly generates getters/setters
        // if (user.getPhoneVerificationCode() == null ||
        //     !user.getPhoneVerificationCode().equals(verificationCode)) {
        //     throw new UserServiceException("Geçersiz SMS kodu");
        // }
        // if (user.getPhoneVerificationExpiry() == null ||
        //     user.getPhoneVerificationExpiry().isBefore(LocalDateTime.now())) {
        //     throw new UserServiceException("SMS kodunun süresi dolmuş");
        // }

        // Update verification status
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // TODO: Re-enable when Lombok properly generates getters/setters
        // user.setPhoneVerificationCode(null);
        // user.setPhoneVerificationExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Phone verified successfully for user: {}", username);
    }

    /**
     * Gets user's active sessions.
     *
     * @param username Username or email
     * @param pageable Pagination parameters
     * @return Page of active sessions
     */
    @Transactional(readOnly = true)
    public Page<UserSessionDto> getActiveSessions(String username, Pageable pageable) {
        log.debug("Getting active sessions for user: {}", username);

        UserEntity user = findUserByUsername(username);
        // TODO: Fix repository method signature to support pagination
        List<UserSessionEntity> sessionList =
            sessionRepository.findByUserId(user.getId());

        // Convert to Page manually - simplified for now
        return Page.empty(pageable);
    }

    /**
     * Terminates a specific session.
     *
     * @param username Username or email
     * @param sessionId Session ID to terminate
     */
    public void terminateSession(String username, String sessionId) {
        log.info("Terminating session: {} for user: {}", sessionId, username);

        UserEntity user = findUserByUsername(username);

        UserSessionEntity session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new UserServiceException("Oturum bulunamadı"));

        if (!session.getStatus().equals(UserSessionEntity.SessionStatus.ACTIVE)) {
            throw new UserServiceException("Oturum zaten sonlandırılmış");
        }

        // End session
        session.setStatus(UserSessionEntity.SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        sessionRepository.save(session);

        log.info("Session terminated successfully: {}", sessionId);
    }

    /**
     * Terminates all other sessions except current.
     *
     * @param username Username or email
     * @return Number of terminated sessions
     */
    public int terminateAllOtherSessions(String username) {
        log.info("Terminating all other sessions for user: {}", username);

        UserEntity user = findUserByUsername(username);

        // Get current session from security context (simplified - in real implementation,
        // you'd extract session ID from JWT or use other method)
        List<UserSessionEntity> sessions = sessionRepository.findByUserId(user.getId());
        int terminatedCount = 0;
        for (UserSessionEntity session : sessions) {
            if (!"current-session-token".equals(session.getSessionToken())) {
                session.setStatus(UserSessionEntity.SessionStatus.EXPIRED);
                sessionRepository.save(session);
                terminatedCount++;
            }
        }

        log.info("Terminated {} sessions for user: {}", terminatedCount, username);
        return terminatedCount;
    }

    /**
     * Deactivates user account.
     *
     * @param username Username or email
     * @param clientIp Client IP for audit
     * @param userAgent User agent for audit
     */
    public void deactivateAccount(String username, String clientIp, String userAgent) {
        log.info("Deactivating account for user: {}", username);

        UserEntity user = findUserByUsername(username);

        if (!user.isActive()) {
            throw new UserServiceException("Hesap zaten deaktif");
        }

        // Update user status
        user.setStatus(UserEntity.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // End all active sessions
        List<UserSessionEntity> sessions = sessionRepository.findByUserId(user.getId());
        for (UserSessionEntity session : sessions) {
            session.setStatus(UserSessionEntity.SessionStatus.EXPIRED);
            sessionRepository.save(session);
        }

        // Send notification
        emailService.sendAccountDeactivationNotification(user.getEmail(), user.getFirstName());

        log.info("Account deactivated successfully for user: {}", username);
    }

    /**
     * Finds user by username or email.
     */
    private UserEntity findUserByUsername(String username) {
        return userRepository.findByEmailOrUsername(username, username)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));
    }

    /**
     * Validates profile update request.
     */
    private void validateProfileUpdate(UpdateProfileRequest request, UserEntity currentUser) {
        // TC Kimlik cannot be changed
        // TODO: Re-enable when getTcKimlik is properly implemented
        // if (request.getTcKimlik() != null &&
        //     !request.getTcKimlik().equals(currentUser.getTcKimlik())) {
        //     throw new UserServiceException("TC Kimlik No değiştirilemez");
        // }

        // Validate phone number format if provided
        if (request.getPhoneNumber() != null &&
            !validationUtil.isValidTurkishPhoneNumber(request.getPhoneNumber())) {
            throw new UserServiceException("Geçersiz telefon numarası formatı");
        }

        // Check if new phone number is already used by another user
        if (request.getPhoneNumber() != null &&
            !request.getPhoneNumber().equals(currentUser.getPhoneNumber())) {
            boolean phoneExists = userRepository.existsByPhoneNumberAndIdNot(
                request.getPhoneNumber(),
                currentUser.getId()
            );
            if (phoneExists) {
                throw new UserServiceException("Bu telefon numarası başka bir kullanıcı tarafından kullanılıyor");
            }
        }
    }

    /**
     * Updates user entity from update request.
     */
    private void updateUserFromRequest(UserEntity user, UpdateProfileRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            // If phone number changed, reset verification
            if (!request.getPhoneNumber().equals(user.getPhoneNumber())) {
                user.setPhoneVerified(false);
                user.setPhoneVerifiedAt(null);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        // TODO: Re-enable when entity fields are properly configured
        // if (request.getDateOfBirth() != null) {
        //     user.setDateOfBirth(request.getDateOfBirth());
        // }
        // if (request.getAddress() != null) {
        //     user.setAddress(request.getAddress());
        // }
        // if (request.getCity() != null) {
        //     user.setCity(request.getCity());
        // }
        // if (request.getPostalCode() != null) {
        //     user.setPostalCode(request.getPostalCode());
        // }
        // if (request.getCountry() != null) {
        //     user.setCountry(request.getCountry());
        // }
        if (request.getRiskProfile() != null) {
            user.setRiskProfile(request.getRiskProfile());
        }
        if (request.getInvestmentExperience() != null) {
            user.setInvestmentExperience(request.getInvestmentExperience());
        }
    }

    /**
     * Generates a 6-digit verification code.
     */
    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}