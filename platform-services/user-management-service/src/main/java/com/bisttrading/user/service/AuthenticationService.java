package com.bisttrading.user.service;

import com.bisttrading.core.security.dto.*;
import com.bisttrading.core.security.service.JwtTokenProvider;
import com.bisttrading.core.security.service.TokenBlacklistService;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.entity.UserSessionEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.infrastructure.persistence.repository.UserSessionRepository;
import com.bisttrading.user.exception.UserServiceException;
import com.bisttrading.user.util.TurkishValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication service for BIST Trading Platform.
 * Handles user registration, login, token management, and session tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final TurkishValidationUtil validationUtil;

    /**
     * Registers a new user in the system.
     *
     * @param registerRequest User registration data
     * @param clientIp Client IP address
     * @param userAgent User agent string
     * @return JWT response with tokens
     */
    public JwtResponse register(RegisterRequest registerRequest, String clientIp, String userAgent) {
        log.info("Processing registration for email: {}", registerRequest.getEmail());

        // Validate Turkish-specific data
        validateRegistrationData(registerRequest);

        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserServiceException("Bu e-posta adresi zaten kullanımda");
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserServiceException("Bu kullanıcı adı zaten kullanımda");
        }

        // Check TC Kimlik uniqueness if provided
        if (registerRequest.getTcKimlik() != null &&
            userRepository.existsByTcKimlik(registerRequest.getTcKimlik())) {
            throw new UserServiceException("Bu TC Kimlik No zaten kayıtlı");
        }

        // Create new user entity
        UserEntity user = createUserFromRegistration(registerRequest);
        user = userRepository.save(user);

        // Create session
        UserSessionEntity session = createSession(user, clientIp, userAgent);
        sessionRepository.save(session);

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("User registered successfully with ID: {}", user.getId());

        return JwtResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .userId(user.getId())
            .username(user.getEmail())
            .authorities(user.getAuthorities().stream()
                .map(Object::toString)
                .toList())
            .build();
    }

    /**
     * Authenticates user and returns JWT tokens.
     *
     * @param loginRequest Login credentials
     * @param clientIp Client IP address
     * @param userAgent User agent string
     * @return JWT response with tokens
     */
    public JwtResponse login(LoginRequest loginRequest, String clientIp, String userAgent) {
        log.info("Processing login for: {}", loginRequest.getEmailOrUsername());

        // Find user by email or username
        UserEntity user = userRepository.findByEmailOrUsername(
            loginRequest.getEmailOrUsername(),
            loginRequest.getEmailOrUsername()
        ).orElseThrow(() -> new BadCredentialsException("E-posta veya şifre hatalı"));

        // Check if account is locked
        if (user.getAccountLockedUntil() != null &&
            user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UserServiceException(
                String.format("Hesabınız %s tarihine kadar kilitlenmiştir",
                    user.getAccountLockedUntil())
            );
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("E-posta veya şifre hatalı");
        }

        // Check user status
        if (!user.isActive()) {
            throw new UserServiceException("Hesabınız aktif değil. Lütfen hesabınızı aktifleştirin.");
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            userRepository.resetFailedLoginAttempts(user.getId());
        }

        // Update last login
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now(), clientIp);

        // Create session
        UserSessionEntity session = createSession(user, clientIp, userAgent);
        sessionRepository.save(session);

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("User logged in successfully: {}", user.getEmail());

        return JwtResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .userId(user.getId())
            .username(user.getEmail())
            .authorities(user.getAuthorities().stream()
                .map(Object::toString)
                .toList())
            .build();
    }

    /**
     * Refreshes access token using refresh token.
     *
     * @param refreshRequest Refresh token request
     * @param clientIp Client IP address
     * @param userAgent User agent string
     * @return New JWT response with fresh tokens
     */
    public JwtResponse refresh(RefreshTokenRequest refreshRequest, String clientIp, String userAgent) {
        log.debug("Processing token refresh");

        String refreshToken = refreshRequest.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token geçersiz veya süresi dolmuş");
        }

        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new BadCredentialsException("Refresh token geçersiz kılınmış");
        }

        // Extract username from token
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // Find user
        UserEntity user = userRepository.findByEmailOrUsername(username, username)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));

        // Check user status
        if (!user.isActive()) {
            throw new UserServiceException("Hesabınız aktif değil");
        }

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateToken(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Blacklist old refresh token
        tokenBlacklistService.blacklistToken(refreshToken);

        // Update session activity
        sessionRepository.updateSessionActivity(user.getId(), clientIp, LocalDateTime.now());

        log.debug("Token refreshed successfully for user: {}", user.getEmail());

        return JwtResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .userId(user.getId())
            .username(user.getEmail())
            .authorities(user.getAuthorities().stream()
                .map(Object::toString)
                .toList())
            .build();
    }

    /**
     * Logs out user and invalidates tokens.
     *
     * @param logoutRequest Logout request with tokens
     * @param clientIp Client IP address
     * @param userAgent User agent string
     */
    public void logout(LogoutRequest logoutRequest, String clientIp, String userAgent) {
        log.info("Processing logout");

        try {
            // Blacklist tokens
            if (logoutRequest.getAccessToken() != null) {
                tokenBlacklistService.blacklistToken(logoutRequest.getAccessToken());
            }

            if (logoutRequest.getRefreshToken() != null) {
                tokenBlacklistService.blacklistToken(logoutRequest.getRefreshToken());
            }

            // Get current user if available
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                UserEntity user = userRepository.findByEmailOrUsername(username, username)
                    .orElse(null);

                if (user != null) {
                    // End current session
                    sessionRepository.endCurrentSession(user.getId(), clientIp, LocalDateTime.now());
                }
            }

            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Error during logout process", e);
            // Don't throw exception for logout - always succeed
        }
    }

    /**
     * Validates current authentication status.
     *
     * @return Current user validation data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("Token geçersiz");
        }

        String username = auth.getName();
        UserEntity user = userRepository.findByEmailOrUsername(username, username)
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));

        if (!user.isActive()) {
            throw new UserServiceException("Hesap aktif değil");
        }

        return Map.of(
            "valid", true,
            "userId", user.getId(),
            "username", user.getEmail(),
            "authorities", user.getAuthorities().stream()
                .map(Object::toString)
                .toList(),
            "emailVerified", user.getEmailVerified(),
            "phoneVerified", user.getPhoneVerified(),
            "kycCompleted", user.getKycCompleted()
        );
    }

    /**
     * Validates registration data according to Turkish regulations.
     */
    private void validateRegistrationData(RegisterRequest request) {
        // Validate TC Kimlik if provided
        if (request.getTcKimlik() != null &&
            !validationUtil.isValidTcKimlik(request.getTcKimlik())) {
            throw new UserServiceException("Geçersiz TC Kimlik No");
        }

        // Validate Turkish phone number if provided
        if (request.getPhoneNumber() != null &&
            !validationUtil.isValidTurkishPhoneNumber(request.getPhoneNumber())) {
            throw new UserServiceException("Geçersiz telefon numarası formatı");
        }

        // Validate password strength
        if (!validationUtil.isValidPassword(request.getPassword())) {
            throw new UserServiceException(
                "Şifre en az 8 karakter olmalı ve büyük harf, küçük harf, rakam içermelidir"
            );
        }

        // Validate email format
        if (!validationUtil.isValidEmail(request.getEmail())) {
            throw new UserServiceException("Geçersiz e-posta adresi formatı");
        }
    }

    /**
     * Creates user entity from registration request.
     */
    private UserEntity createUserFromRegistration(RegisterRequest request) {
        return UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .email(request.getEmail().toLowerCase())
            .username(request.getUsername().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .tcKimlik(request.getTcKimlik())
            .status(UserEntity.UserStatus.ACTIVE)
            .emailVerified(false)
            .phoneVerified(false)
            .kycCompleted(false)
            .professionalInvestor(false)
            .riskProfile(UserEntity.RiskProfile.CONSERVATIVE)
            .investmentExperience(UserEntity.InvestmentExperience.BEGINNER)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Creates user session entity.
     */
    private UserSessionEntity createSession(UserEntity user, String clientIp, String userAgent) {
        return UserSessionEntity.builder()
            .id(UUID.randomUUID().toString())
            .userId(user.getId())
            .sessionToken(UUID.randomUUID().toString())
            .ipAddress(clientIp)
            .userAgent(userAgent)
            .deviceType(extractDeviceType(userAgent))
            .securityLevel(UserSessionEntity.SecurityLevel.STANDARD)
            .status(UserSessionEntity.SessionStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .lastActivityAt(LocalDateTime.now())
            .build();
    }

    /**
     * Handles failed login attempt.
     */
    private void handleFailedLogin(UserEntity user) {
        int failedAttempts = userRepository.incrementFailedLoginAttempts(user.getId());

        // Lock account after 5 failed attempts
        if (failedAttempts >= 5) {
            LocalDateTime lockUntil = LocalDateTime.now().plusHours(1);
            userRepository.lockUserAccount(user.getId(), lockUntil);
            log.warn("Account locked for user: {} due to {} failed login attempts",
                user.getEmail(), failedAttempts);
        } else {
            log.warn("Failed login attempt {} for user: {}", failedAttempts, user.getEmail());
        }
    }

    /**
     * Extracts device type from user agent.
     */
    private UserSessionEntity.DeviceType extractDeviceType(String userAgent) {
        if (userAgent == null) {
            return UserSessionEntity.DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return UserSessionEntity.DeviceType.MOBILE;
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return UserSessionEntity.DeviceType.TABLET;
        } else {
            return UserSessionEntity.DeviceType.DESKTOP;
        }
    }
}