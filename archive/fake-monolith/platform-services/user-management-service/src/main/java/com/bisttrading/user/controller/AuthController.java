package com.bisttrading.user.controller;

import com.bisttrading.user.dto.JwtResponse;
import com.bisttrading.user.dto.LoginRequest;
import com.bisttrading.user.dto.LogoutRequest;
import com.bisttrading.user.dto.RefreshTokenRequest;
import com.bisttrading.user.dto.UserRegistrationRequest;
import com.bisttrading.user.dto.UserRegistrationResponse;
import com.bisttrading.user.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller for BIST Trading Platform.
 * Handles user registration, login, token refresh, and logout operations.
 *
 * Base URL: /api/v1/auth
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kullanıcı kimlik doğrulama işlemleri")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Registers a new user in the system.
     *
     * @param registerRequest User registration data
     * @param request HTTP request for IP tracking
     * @return JWT response with access and refresh tokens
     */
    @PostMapping("/register")
    @Operation(
        summary = "Yeni kullanıcı kaydı",
        description = "Sisteme yeni bir kullanıcı kaydeder. TC Kimlik No ve e-posta adresinin benzersiz olması gerekir."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Kayıt başarılı",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = JwtResponse.class),
                examples = @ExampleObject(
                    name = "Başarılı kayıt",
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 900,
                        "userId": "user123",
                        "username": "kullanici@example.com",
                        "authorities": ["ROLE_USER"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz kayıt verisi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                    {
                        "error": "VALIDATION_ERROR",
                        "message": "Geçersiz TC Kimlik No",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "E-posta adresi veya TC Kimlik No zaten kullanımda",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Conflict error",
                    value = """
                    {
                        "error": "USER_ALREADY_EXISTS",
                        "message": "Bu e-posta adresi zaten kullanımda",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<UserRegistrationResponse> register(
            @Valid @RequestBody
            @Parameter(description = "Kullanıcı kayıt bilgileri", required = true)
            UserRegistrationRequest registerRequest,
            HttpServletRequest request) {

        log.info("User registration attempt for email: {}", registerRequest.getEmail());

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        UserRegistrationResponse response = authenticationService.register(registerRequest);

        log.info("User registered successfully with ID: {}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates user and returns JWT tokens.
     *
     * @param loginRequest Login credentials
     * @param request HTTP request for security tracking
     * @return JWT response with tokens
     */
    @PostMapping("/login")
    @Operation(
        summary = "Kullanıcı girişi",
        description = "E-posta/kullanıcı adı ve şifre ile sisteme giriş yapar"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Giriş başarılı",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = JwtResponse.class),
                examples = @ExampleObject(
                    name = "Başarılı giriş",
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 900,
                        "userId": "user123",
                        "username": "kullanici@example.com",
                        "authorities": ["ROLE_USER"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Geçersiz kimlik bilgileri",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Authentication error",
                    value = """
                    {
                        "error": "INVALID_CREDENTIALS",
                        "message": "E-posta veya şifre hatalı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Hesap kilitli",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account locked",
                    value = """
                    {
                        "error": "ACCOUNT_LOCKED",
                        "message": "Hesabınız geçici olarak kilitlenmiştir. Lütfen daha sonra tekrar deneyin.",
                        "timestamp": "2024-01-15T10:30:00Z",
                        "lockUntil": "2024-01-15T11:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody
            @Parameter(description = "Giriş kimlik bilgileri", required = true)
            LoginRequest loginRequest,
            HttpServletRequest request) {

        log.info("Login attempt for user: {}", loginRequest.getUsername());

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        JwtResponse response = authenticationService.authenticate(loginRequest);

        log.info("User logged in successfully: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes access token using refresh token.
     *
     * @param refreshRequest Refresh token request
     * @param request HTTP request for security tracking
     * @return New JWT response with fresh tokens
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Token yenileme",
        description = "Refresh token kullanarak yeni access token alır"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token yenileme başarılı",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = JwtResponse.class),
                examples = @ExampleObject(
                    name = "Token yenilendi",
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 900,
                        "userId": "user123",
                        "username": "kullanici@example.com",
                        "authorities": ["ROLE_USER"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Geçersiz veya süresi dolmuş refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid refresh token",
                    value = """
                    {
                        "error": "INVALID_REFRESH_TOKEN",
                        "message": "Refresh token geçersiz veya süresi dolmuş",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<JwtResponse> refresh(
            @Valid @RequestBody
            @Parameter(description = "Refresh token bilgisi", required = true)
            RefreshTokenRequest refreshRequest,
            HttpServletRequest request) {

        log.debug("Token refresh attempt");

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        JwtResponse response = authenticationService.refreshToken(refreshRequest);

        log.debug("Token refreshed successfully for user: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out user and invalidates tokens.
     *
     * @param logoutRequest Logout request with tokens
     * @param request HTTP request for security tracking
     * @return Success response
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Kullanıcı çıkışı",
        description = "Kullanıcının çıkış yapması ve token'ların geçersiz kılınması"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Çıkış başarılı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Başarılı çıkış",
                    value = """
                    {
                        "message": "Başarıyla çıkış yapıldı",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Geçersiz token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid token",
                    value = """
                    {
                        "error": "INVALID_TOKEN",
                        "message": "Geçersiz veya süresi dolmuş token",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> logout(
            @Valid @RequestBody
            @Parameter(description = "Çıkış token bilgisi", required = true)
            LogoutRequest logoutRequest,
            HttpServletRequest request) {

        log.info("Logout attempt");

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        authenticationService.logout(logoutRequest);

        log.info("User logged out successfully");
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "message", "Başarıyla çıkış yapıldı",
                "timestamp", java.time.Instant.now()
            ));
    }

    /**
     * Validates current authentication status.
     *
     * @return Authentication status
     */
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Token doğrulama",
        description = "Mevcut token'ın geçerliliğini kontrol eder"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token geçerli",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Valid token",
                    value = """
                    {
                        "valid": true,
                        "userId": "user123",
                        "username": "kullanici@example.com",
                        "authorities": ["ROLE_USER"],
                        "expiresAt": "2024-01-15T11:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token geçersiz",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid token",
                    value = """
                    {
                        "error": "INVALID_TOKEN",
                        "message": "Token geçersiz veya süresi dolmuş",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> validate() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "valid", true,
                "message", "Token is valid",
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