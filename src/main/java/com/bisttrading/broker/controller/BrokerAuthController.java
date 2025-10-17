package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.exception.AlgoLabAuthenticationException;
import com.bisttrading.broker.algolab.service.AlgoLabAuthService;
import com.bisttrading.broker.dto.BrokerAuthResponse;
import com.bisttrading.broker.dto.BrokerAuthStatusResponse;
import com.bisttrading.broker.dto.BrokerLoginRequest;
import com.bisttrading.broker.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * AlgoLab Broker Authentication Controller
 *
 * Handles AlgoLab broker authentication flow:
 * 1. Login with username/password (triggers SMS OTP)
 * 2. Verify SMS OTP code
 * 3. Check authentication status
 * 4. Logout and clear session
 *
 * Base URL: /api/v1/broker/auth
 *
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/broker/auth")
@RequiredArgsConstructor
@Tag(name = "🔐 Broker Authentication",
     description = "AlgoLab broker kimlik doğrulama işlemleri (Login, OTP Verification, Logout)")
public class BrokerAuthController {

    private final AlgoLabAuthService algoLabAuthService;

    /**
     * Step 1: Login with AlgoLab credentials.
     * Triggers SMS OTP to be sent to user's phone.
     */
    @PostMapping("/login")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "AlgoLab'a Giriş Yap (Adım 1)",
        description = """
            AlgoLab broker hesabına giriş yapar ve SMS doğrulama kodu gönderir.

            📱 AKIŞ:
            1. AlgoLab kullanıcı adı ve şifrenizi gönderin
            2. AlgoLab telefonunuza SMS kodu gönderir
            3. SMS kodunu /verify-otp endpoint'ine göndererek doğrulayın

            ⚠️ NOT:
            - Bu endpoint sadece SMS tetikler, henüz authenticated olmaz
            - SMS kodunu aldıktan sonra /verify-otp ile doğrulama yapmalısınız
            - BIST Trading Platform JWT token'ınız olmalı (authenticated user)

            🔒 Gereksinim: Authenticated user (BIST Trading Platform)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SMS kodu başarıyla gönderildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BrokerAuthResponse.class),
                examples = @ExampleObject(
                    name = "SMS Sent",
                    value = """
                    {
                        "success": true,
                        "message": "SMS kodu telefonunuza gönderildi. Lütfen kodu girerek doğrulama yapın.",
                        "smsSent": true,
                        "authenticated": false,
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz kullanıcı adı veya şifre",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "AlgoLab giriş başarısız: Geçersiz kimlik bilgileri",
                        "authenticated": false,
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - BIST Trading Platform authentication gerekli"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab servisi şu anda kullanılamıyor"
        )
    })
    public ResponseEntity<BrokerAuthResponse> login(
            @Valid @RequestBody
            @Parameter(description = "AlgoLab kullanıcı adı ve şifre", required = true)
            BrokerLoginRequest loginRequest,
            Authentication authentication) {

        log.info("AlgoLab login attempt from user: {} (username: {})",
            authentication.getName(), loginRequest.getUsername());

        try {
            // Step 1: Send credentials to AlgoLab, which triggers SMS
            String token = algoLabAuthService.loginUser(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            log.info("✅ AlgoLab login successful for user: {} (AlgoLab username: {}). SMS sent. Token received.",
                authentication.getName(), loginRequest.getUsername());

            return ResponseEntity.ok(BrokerAuthResponse.loginSuccess());

        } catch (AlgoLabAuthenticationException e) {
            log.error("❌ AlgoLab login failed for user: {} - {}",
                authentication.getName(), e.getMessage());

            return ResponseEntity
                .badRequest()
                .body(BrokerAuthResponse.error("AlgoLab giriş başarısız: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Unexpected error during AlgoLab login for user: {}",
                authentication.getName(), e);

            return ResponseEntity
                .status(503)
                .body(BrokerAuthResponse.error("AlgoLab servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."));
        }
    }

    /**
     * Step 2: Verify SMS OTP code.
     * Completes authentication and establishes session.
     */
    @PostMapping("/verify-otp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "SMS Kodunu Doğrula (Adım 2)",
        description = """
            SMS ile gelen doğrulama kodunu onaylayarak AlgoLab authentication'ı tamamlar.

            ✅ AKIŞ:
            1. Telefonunuza gelen SMS kodunu girin
            2. AlgoLab kimlik doğrulaması tamamlanır
            3. Session otomatik olarak kaydedilir ve yenilenir

            🔄 SESSION YÖNETİMİ:
            - Session 24 saat geçerlidir
            - Her 5 dakikada bir otomatik refresh edilir
            - Uygulama restart olsa bile session korunur

            🚀 DOĞRULAMA SONRASI:
            - /api/v1/broker/orders endpoint'lerini kullanabilirsiniz
            - Gerçek para ile işlem yapabilirsiniz
            - Portfolio verilerinizi görebilirsiniz

            🔒 Gereksinim: Authenticated user + /login ile SMS almış olmalısınız
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OTP doğrulama başarılı, AlgoLab session oluşturuldu",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BrokerAuthResponse.class),
                examples = @ExampleObject(
                    name = "Verification Success",
                    value = """
                    {
                        "success": true,
                        "message": "Doğrulama başarılı. AlgoLab'a başarıyla bağlandınız.",
                        "authenticated": true,
                        "sessionExpiresAt": "2025-10-16T10:30:00Z",
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz OTP kodu",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "OTP doğrulama başarısız: Geçersiz kod",
                        "authenticated": false,
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - BIST Trading Platform authentication gerekli"
        ),
        @ApiResponse(
            responseCode = "428",
            description = "Precondition Required - Önce /login endpoint'ini çağırmalısınız",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "message": "Token bulunamadı. Önce /login endpoint'ini çağırmalısınız.",
                        "authenticated": false,
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<BrokerAuthResponse> verifyOtp(
            @Valid @RequestBody
            @Parameter(description = "SMS ile gelen OTP kodu", required = true)
            VerifyOtpRequest otpRequest,
            Authentication authentication) {

        log.info("AlgoLab OTP verification attempt from user: {}", authentication.getName());

        try {
            // Step 2: Verify SMS code and get authorization hash
            String hash = algoLabAuthService.loginUserControl(otpRequest.getOtpCode());

            log.info("✅ AlgoLab OTP verification successful for user: {}. Session established.",
                authentication.getName());

            // Calculate session expiration (24 hours from now - configurable)
            Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            return ResponseEntity.ok(BrokerAuthResponse.verifySuccess(expiresAt));

        } catch (AlgoLabAuthenticationException e) {
            log.error("❌ AlgoLab OTP verification failed for user: {} - {}",
                authentication.getName(), e.getMessage());

            // Check if the error is due to missing token
            if (e.getMessage().contains("Token is null")) {
                return ResponseEntity
                    .status(428) // Precondition Required
                    .body(BrokerAuthResponse.error("Token bulunamadı. Önce /login endpoint'ini çağırmalısınız."));
            }

            return ResponseEntity
                .badRequest()
                .body(BrokerAuthResponse.error("OTP doğrulama başarısız: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Unexpected error during AlgoLab OTP verification for user: {}",
                authentication.getName(), e);

            return ResponseEntity
                .status(503)
                .body(BrokerAuthResponse.error("AlgoLab servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."));
        }
    }

    /**
     * Check AlgoLab authentication status.
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "AlgoLab Bağlantı Durumunu Kontrol Et",
        description = """
            AlgoLab broker authentication durumunu ve session bilgilerini döner.

            📊 DÖNEN BİLGİLER:
            - Authenticated: AlgoLab'a giriş yapılmış mı?
            - Session Active: Session aktif mi?
            - Connection Alive: AlgoLab bağlantısı canlı mı?
            - Last Refresh: Son yenilenme zamanı
            - Expires At: Session bitiş zamanı

            💡 KULLANIM:
            - Emir vermeden önce bağlantıyı kontrol edin
            - Session expired ise yeniden /login yapın
            - Frontend'de status göstermek için kullanın

            🔒 Gereksinim: Authenticated user (BIST Trading Platform)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Durum bilgisi başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BrokerAuthStatusResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Authenticated",
                        description = "User is authenticated with AlgoLab",
                        value = """
                        {
                            "authenticated": true,
                            "sessionActive": true,
                            "connectionAlive": true,
                            "lastRefreshTime": "2025-10-15T10:25:00Z",
                            "sessionExpiresAt": "2025-10-16T10:30:00Z",
                            "brokerName": "AlgoLab",
                            "timestamp": "2025-10-15T10:30:00Z",
                            "message": "AlgoLab bağlantısı aktif"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Not Authenticated",
                        description = "User is not authenticated with AlgoLab",
                        value = """
                        {
                            "authenticated": false,
                            "sessionActive": false,
                            "connectionAlive": false,
                            "brokerName": "AlgoLab",
                            "timestamp": "2025-10-15T10:30:00Z",
                            "message": "AlgoLab'a giriş yapılmamış. Lütfen /login endpoint'ini kullanın."
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - BIST Trading Platform authentication gerekli"
        )
    })
    public ResponseEntity<BrokerAuthStatusResponse> getAuthStatus(Authentication authentication) {
        log.debug("AlgoLab auth status check from user: {}", authentication.getName());

        boolean isAuthenticated = algoLabAuthService.isAuthenticated();
        boolean isAlive = algoLabAuthService.isAlive();

        BrokerAuthStatusResponse.BrokerAuthStatusResponseBuilder response = BrokerAuthStatusResponse.builder()
            .authenticated(isAuthenticated)
            .sessionActive(isAuthenticated)
            .connectionAlive(isAlive);

        if (isAuthenticated) {
            response.message("AlgoLab bağlantısı aktif")
                    .lastRefreshTime(Instant.now())
                    .sessionExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        } else {
            response.message("AlgoLab'a giriş yapılmamış. Lütfen /login endpoint'ini kullanın.");
        }

        log.debug("AlgoLab auth status for user {}: authenticated={}, alive={}",
            authentication.getName(), isAuthenticated, isAlive);

        return ResponseEntity.ok(response.build());
    }

    /**
     * Logout and clear AlgoLab session.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "AlgoLab Oturumunu Sonlandır",
        description = """
            AlgoLab broker session'ını sonlandırır ve temizler.

            🔒 İŞLEM:
            - Tüm session bilgileri silinir
            - Token ve hash temizlenir
            - Database/file'dan session kaldırılır

            ⚠️ NOT:
            - Logout sonrası emir veremezsiniz
            - Yeniden işlem yapmak için /login gerekir
            - BIST Trading Platform session'ınız devam eder

            🔒 Gereksinim: Authenticated user (BIST Trading Platform)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Oturum başarıyla sonlandırıldı",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BrokerAuthResponse.class),
                examples = @ExampleObject(
                    name = "Logout Success",
                    value = """
                    {
                        "success": true,
                        "message": "AlgoLab oturumu başarıyla sonlandırıldı.",
                        "authenticated": false,
                        "timestamp": "2025-10-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - BIST Trading Platform authentication gerekli"
        )
    })
    public ResponseEntity<BrokerAuthResponse> logout(Authentication authentication) {
        log.info("AlgoLab logout request from user: {}", authentication.getName());

        try {
            algoLabAuthService.clearAuth();

            log.info("✅ AlgoLab session cleared for user: {}", authentication.getName());

            return ResponseEntity.ok(BrokerAuthResponse.logoutSuccess());

        } catch (Exception e) {
            log.error("❌ Error during AlgoLab logout for user: {}",
                authentication.getName(), e);

            return ResponseEntity
                .status(500)
                .body(BrokerAuthResponse.error("Oturum sonlandırılırken hata oluştu: " + e.getMessage()));
        }
    }
}
