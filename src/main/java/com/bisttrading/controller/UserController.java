package com.bisttrading.controller;

import com.bisttrading.dto.*;
import com.bisttrading.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User Management REST Controller for BIST Trading Platform.
 * Handles user profile operations, verification, and account management.
 *
 * Base URL: /api/v1/users
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Kullanıcı profil yönetimi işlemleri")
public class UserController {

    private final UserService userService;

    /**
     * Gets current user's profile.
     *
     * @param authentication Current user's authentication
     * @return User profile data
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Kullanıcı profili getir",
        description = "Mevcut kullanıcının profil bilgilerini getirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profil bilgileri başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileDto.class),
                examples = @ExampleObject(
                    name = "Kullanıcı profili",
                    value = """
                    {
                        "id": "user123",
                        "email": "kullanici@example.com",
                        "username": "kullanici123",
                        "firstName": "Ahmet",
                        "lastName": "Yılmaz",
                        "phoneNumber": "+905551234567",
                        "tcKimlik": "12345678901",
                        "status": "ACTIVE",
                        "emailVerified": true,
                        "phoneVerified": true,
                        "kycCompleted": true,
                        "kycLevel": "BASIC",
                        "professionalInvestor": false,
                        "riskProfile": "MODERATE",
                        "investmentExperience": "BEGINNER",
                        "createdAt": "2024-01-15T10:30:00Z",
                        "lastLoginAt": "2024-01-15T14:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Kimlik doğrulama gerekli",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        log.debug("Getting profile for user: {}", authentication.getName());

        UserProfileDto profile = userService.getUserProfile(authentication.getName());

        log.debug("Profile retrieved successfully for user: {}", authentication.getName());
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates current user's profile.
     *
     * @param updateRequest Profile update data
     * @param authentication Current user's authentication
     * @param request HTTP request for audit
     * @return Updated profile data
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Kullanıcı profili güncelle",
        description = "Mevcut kullanıcının profil bilgilerini günceller"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profil başarıyla güncellendi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz profil verisi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                    {
                        "error": "VALIDATION_ERROR",
                        "message": "TC Kimlik No değiştirilemez",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Kimlik doğrulama gerekli",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody
            @Parameter(description = "Profil güncelleme bilgileri", required = true)
            UpdateProfileRequest updateRequest,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Profile update request from user: {}", authentication.getName());

        String clientIp = getClientIpAddress(request);
        UserProfileDto updatedProfile = userService.updateProfile(
            authentication.getName(),
            updateRequest,
            clientIp
        );

        log.info("Profile updated successfully for user: {}", authentication.getName());
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Changes user's password.
     *
     * @param changePasswordRequest Password change data
     * @param authentication Current user's authentication
     * @param request HTTP request for security tracking
     * @return Success response
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Şifre değiştir",
        description = "Kullanıcının mevcut şifresini değiştirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Şifre başarıyla değiştirildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Şifre değiştirildi",
                    value = """
                    {
                        "message": "Şifreniz başarıyla değiştirildi",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Mevcut şifre hatalı veya yeni şifre geçersiz",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Password error",
                    value = """
                    {
                        "error": "INVALID_PASSWORD",
                        "message": "Mevcut şifre hatalı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody
            @Parameter(description = "Şifre değiştirme bilgileri", required = true)
            ChangePasswordRequest changePasswordRequest,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Password change request from user: {}", authentication.getName());

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        userService.changePassword(
            authentication.getName(),
            changePasswordRequest,
            clientIp,
            userAgent
        );

        log.info("Password changed successfully for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Şifreniz başarıyla değiştirildi",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Sends email verification.
     *
     * @param authentication Current user's authentication
     * @return Success response
     */
    @PostMapping("/verify-email")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "E-posta doğrulama gönder",
        description = "Kullanıcının e-posta adresine doğrulama kodu gönderir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Doğrulama kodu gönderildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Verification sent",
                    value = """
                    {
                        "message": "E-posta doğrulama kodu gönderildi",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "E-posta zaten doğrulanmış veya doğrulama gönderilemedi",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<?> sendEmailVerification(Authentication authentication) {
        log.info("Email verification request from user: {}", authentication.getName());

        userService.sendEmailVerification(authentication.getName());

        log.info("Email verification sent for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "E-posta doğrulama kodu gönderildi",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Confirms email verification.
     *
     * @param verifyRequest Verification code
     * @param authentication Current user's authentication
     * @return Success response
     */
    @PostMapping("/verify-email/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "E-posta doğrulama onayla",
        description = "Gönderilen doğrulama kodu ile e-posta adresini doğrular"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "E-posta başarıyla doğrulandı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Email verified",
                    value = """
                    {
                        "message": "E-posta adresiniz başarıyla doğrulandı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz doğrulama kodu",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid code",
                    value = """
                    {
                        "error": "INVALID_VERIFICATION_CODE",
                        "message": "Doğrulama kodu geçersiz veya süresi dolmuş",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> confirmEmailVerification(
            @Valid @RequestBody
            @Parameter(description = "E-posta doğrulama kodu", required = true)
            VerifyEmailRequest verifyRequest,
            Authentication authentication) {

        log.info("Email verification confirmation from user: {}", authentication.getName());

        userService.verifyEmail(authentication.getName(), verifyRequest.getVerificationCode());

        log.info("Email verified successfully for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "E-posta adresiniz başarıyla doğrulandı",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Sends phone verification SMS.
     *
     * @param authentication Current user's authentication
     * @return Success response
     */
    @PostMapping("/verify-phone")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Telefon doğrulama gönder",
        description = "Kullanıcının telefon numarasına SMS doğrulama kodu gönderir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SMS doğrulama kodu gönderildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "SMS sent",
                    value = """
                    {
                        "message": "SMS doğrulama kodu gönderildi",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Telefon zaten doğrulanmış veya SMS gönderilemedi",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<?> sendPhoneVerification(Authentication authentication) {
        log.info("Phone verification request from user: {}", authentication.getName());

        userService.sendPhoneVerification(authentication.getName());

        log.info("Phone verification SMS sent for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "SMS doğrulama kodu gönderildi",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Confirms phone verification.
     *
     * @param verifyRequest SMS verification code
     * @param authentication Current user's authentication
     * @return Success response
     */
    @PostMapping("/verify-phone/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Telefon doğrulama onayla",
        description = "Gönderilen SMS kodu ile telefon numarasını doğrular"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Telefon başarıyla doğrulandı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Phone verified",
                    value = """
                    {
                        "message": "Telefon numaranız başarıyla doğrulandı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz SMS kodu",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid SMS code",
                    value = """
                    {
                        "error": "INVALID_VERIFICATION_CODE",
                        "message": "SMS kodu geçersiz veya süresi dolmuş",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> confirmPhoneVerification(
            @Valid @RequestBody
            @Parameter(description = "SMS doğrulama kodu", required = true)
            VerifyPhoneRequest verifyRequest,
            Authentication authentication) {

        log.info("Phone verification confirmation from user: {}", authentication.getName());

        userService.verifyPhone(authentication.getName(), verifyRequest.getVerificationCode());

        log.info("Phone verified successfully for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Telefon numaranız başarıyla doğrulandı",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Gets user's active sessions.
     *
     * @param authentication Current user's authentication
     * @param pageable Pagination parameters
     * @return Page of active sessions
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Aktif oturumları listele",
        description = "Kullanıcının tüm aktif oturumlarını listeler"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Aktif oturumlar başarıyla listelendi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserSessionDto.class)
            )
        )
    })
    public ResponseEntity<Page<UserSessionDto>> getActiveSessions(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting active sessions for user: {}", authentication.getName());

        Page<UserSessionDto> sessions = userService.getActiveSessions(authentication.getName(), pageable);

        log.debug("Found {} active sessions for user: {}", sessions.getTotalElements(), authentication.getName());
        return ResponseEntity.ok(sessions);
    }

    /**
     * Terminates a specific session.
     *
     * @param sessionId Session ID to terminate
     * @param authentication Current user's authentication
     * @return Success response
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Oturumu sonlandır",
        description = "Belirtilen oturumu sonlandırır"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Oturum başarıyla sonlandırıldı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Session terminated",
                    value = """
                    {
                        "message": "Oturum başarıyla sonlandırıldı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Oturum bulunamadı",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<?> terminateSession(
            @PathVariable
            @Parameter(description = "Sonlandırılacak oturum ID'si", required = true)
            String sessionId,
            Authentication authentication) {

        log.info("Session termination request for session: {} by user: {}", sessionId, authentication.getName());

        userService.terminateSession(authentication.getName(), sessionId);

        log.info("Session {} terminated successfully by user: {}", sessionId, authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Oturum başarıyla sonlandırıldı",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Terminates all other sessions except current.
     *
     * @param authentication Current user's authentication
     * @return Success response
     */
    @DeleteMapping("/sessions/terminate-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Diğer tüm oturumları sonlandır",
        description = "Mevcut oturum hariç tüm diğer oturumları sonlandırır"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tüm diğer oturumlar sonlandırıldı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "All sessions terminated",
                    value = """
                    {
                        "message": "Tüm diğer oturumlar sonlandırıldı",
                        "terminatedCount": 3,
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> terminateAllOtherSessions(Authentication authentication) {
        log.info("Terminate all other sessions request from user: {}", authentication.getName());

        int terminatedCount = userService.terminateAllOtherSessions(authentication.getName());

        log.info("Terminated {} sessions for user: {}", terminatedCount, authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Tüm diğer oturumlar sonlandırıldı",
                "terminatedCount", terminatedCount,
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Deactivates user account.
     *
     * @param authentication Current user's authentication
     * @param request HTTP request for audit
     * @return Success response
     */
    @PostMapping("/deactivate")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Hesabı deaktive et",
        description = "Kullanıcı hesabını deaktive eder (geri alınabilir)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Hesap başarıyla deaktive edildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account deactivated",
                    value = """
                    {
                        "message": "Hesabınız başarıyla deaktive edildi",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> deactivateAccount(
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Account deactivation request from user: {}", authentication.getName());

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        userService.deactivateAccount(authentication.getName(), clientIp, userAgent);

        log.info("Account deactivated successfully for user: {}", authentication.getName());
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Hesabınız başarıyla deaktive edildi",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Extracts client IP address from request.
     * Handles various proxy headers for accurate IP detection.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }

        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded;
        }

        return request.getRemoteAddr();
    }
}