package com.bisttrading.service;

import com.bisttrading.entity.UserEntity;
import com.bisttrading.repository.UserRepository;
import com.bisttrading.dto.*;
import com.bisttrading.exception.UserServiceException;
import com.bisttrading.security.jwt.JwtTokenProvider;
import com.bisttrading.security.config.JwtProperties;
import com.bisttrading.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication service for BIST Trading Platform.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * Authenticates user with username/password.
     */
    public JwtResponse authenticate(LoginRequest loginRequest) {
        log.info("Authentication attempt for user: {}", loginRequest.getUsername());

        // Find user by username or email
        UserEntity user = userRepository.findByUsername(loginRequest.getUsername())
            .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
            .orElseThrow(() -> new UserServiceException("Invalid username or password"));

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: {}", loginRequest.getUsername());
            throw new UserServiceException("Invalid username or password");
        }

        // Check if account is active
        if (!user.isActive()) {
            throw new UserServiceException("Account is not active");
        }

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new UserServiceException("Account is locked. Please try again later.");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        user.setLoginAttempts(0);
        userRepository.save(user);

        // Create CustomUserDetails
        CustomUserDetails userDetails = createCustomUserDetails(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        log.info("User logged in successfully: {}", user.getUsername());

        return JwtResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn((int) jwtProperties.getAccessTokenExpiryInSeconds())
            .userId(user.getId())
            .username(user.getUsername())
            .build();
    }

    /**
     * Converts UserEntity to CustomUserDetails for JWT generation.
     */
    private CustomUserDetails createCustomUserDetails(UserEntity user) {
        // Parse authorities/roles
        Set<String> roles = new HashSet<>();
        if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
            roles = Arrays.stream(user.getAuthorities().split(","))
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toSet());
        }

        return CustomUserDetails.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phoneNumber(user.getPhoneNumber())
            .roles(roles)
            .permissions(new HashSet<>()) // TODO: Load permissions if needed
            .active(user.isActive())
            .emailVerified(user.isEmailVerified())
            .phoneVerified(user.isPhoneVerified())
            .kycCompleted(user.getKycCompleted())
            .twoFactorEnabled(user.getTwoFactorEnabled())
            .lastLoginDate(user.getLastLoginAt())
            .failedLoginAttempts(user.getFailedLoginAttempts())
            .accountLockExpiry(user.getAccountLockedUntil())
            .preferredLanguage(user.getPreferredLanguage())
            .timezone(user.getTimezone())
            .mustChangePassword(user.getMustChangePassword())
            .passwordExpiryDate(user.getPasswordExpiresAt())
            .professionalInvestor(user.getProfessionalInvestor())
            .build();
    }

    /**
     * Registers a new user.
     */
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserServiceException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserServiceException("Email already exists: " + request.getEmail());
        }

        // Create new user entity
        UserEntity user = UserEntity.builder()
            .id(java.util.UUID.randomUUID().toString())
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .emailVerified(false)
            .phoneVerified(false)
            .authorities("ROLE_USER")
            .kycCompleted(false)
            .professionalInvestor(false)
            .mustChangePassword(false)
            .marketingConsent(false)
            .twoFactorEnabled(false)
            .preferredLanguage("tr")
            .timezone("Europe/Istanbul")
            .userType(UserEntity.UserType.INDIVIDUAL)
            .status(UserEntity.UserStatus.ACTIVE)
            .loginAttempts(0)
            .failedLoginAttempts(0)
            .build();

        // Save user
        UserEntity savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());

        return UserRegistrationResponse.builder()
            .success(true)
            .message("Registration successful")
            .userId(savedUser.getId())
            .build();
    }

    /**
     * Refreshes access token.
     * TODO: Implement proper token refresh.
     */
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Token refresh attempt");

        // TODO: Implement proper token refresh
        return JwtResponse.builder()
            .accessToken("new-stub-access-token")
            .refreshToken("new-stub-refresh-token")
            .tokenType("Bearer")
            .expiresIn(3600)
            .build();
    }

    /**
     * Logs out user.
     * TODO: Implement proper logout logic.
     */
    public void logout(LogoutRequest logoutRequest) {
        log.info("Logout attempt");
        // TODO: Implement proper logout
    }
}