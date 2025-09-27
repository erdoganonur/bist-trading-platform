package com.bisttrading.user;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * BIST Trading Platform - User Management Service
 *
 * This microservice handles user registration, authentication, profile management,
 * and user-related operations for the BIST Trading Platform.
 *
 * Features:
 * - User registration and authentication
 * - JWT token-based security
 * - Profile management and verification
 * - Turkish regulatory compliance (TC Kimlik, phone verification)
 * - Session management
 * - Email and SMS notifications
 *
 * Port: 8081
 * Base URL: http://localhost:8081
 * API Documentation: http://localhost:8081/swagger-ui.html
 * Health Check: http://localhost:8081/actuator/health
 */
@Slf4j
@SpringBootApplication
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
    "com.bisttrading.user",           // Current service
    "com.bisttrading.core.security",  // Security components
    "com.bisttrading.core.common",    // Common utilities
    "com.bisttrading.infrastructure.persistence" // Persistence layer
})
@EntityScan(basePackages = {
    "com.bisttrading.infrastructure.persistence.entity"
})
@OpenAPIDefinition(
    info = @Info(
        title = "BIST Trading Platform - User Management Service",
        version = "1.0.0",
        description = """
            BIST Trading Platform Kullanıcı Yönetim Servisi

            Bu mikroservis, BIST Trading Platform'da kullanıcı kaydı, kimlik doğrulama,
            profil yönetimi ve kullanıcı ile ilgili tüm işlemleri yönetir.

            ## Özellikler
            - Kullanıcı kaydı ve kimlik doğrulama
            - JWT token tabanlı güvenlik
            - Profil yönetimi ve doğrulama
            - Türk düzenlemelerine uyumluluk (TC Kimlik, telefon doğrulama)
            - Oturum yönetimi
            - E-posta ve SMS bildirimleri

            ## Güvenlik
            Tüm korumalı endpoint'ler için Authorization header'ında Bearer token gereklidir:
            ```
            Authorization: Bearer <access_token>
            ```

            ## Hata Kodları
            - 400: Geçersiz istek verisi
            - 401: Kimlik doğrulama gerekli
            - 403: Yetki yetersiz
            - 404: Kaynak bulunamadı
            - 409: Çakışma (örn: e-posta zaten kullanımda)
            - 423: Hesap kilitli
            - 500: Sunucu hatası
            """,
        contact = @Contact(
            name = "BIST Trading Platform Team",
            email = "support@bisttrading.com",
            url = "https://bisttrading.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://bisttrading.com/license"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8081",
            description = "Development Server"
        ),
        @Server(
            url = "https://api-dev.bisttrading.com",
            description = "Development Environment"
        ),
        @Server(
            url = "https://api-staging.bisttrading.com",
            description = "Staging Environment"
        ),
        @Server(
            url = "https://api.bisttrading.com",
            description = "Production Environment"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication. Obtain token from /api/v1/auth/login endpoint."
)
public class UserManagementServiceApplication {

    /**
     * Application entry point.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Set system properties for better performance and Turkish locale support
        System.setProperty("spring.output.ansi.enabled", "always");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "Europe/Istanbul");
        System.setProperty("user.language", "tr");
        System.setProperty("user.country", "TR");

        // Configure logging
        System.setProperty("logging.pattern.console",
            "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} " +
            "%clr(%5p) " +
            "%clr(${PID:- }){magenta} " +
            "%clr(---){faint} " +
            "%clr([%15.15t]){faint} " +
            "%clr(%-40.40logger{39}){cyan} " +
            "%clr(:){faint} %m%n%wEx"
        );

        try {
            log.info("Starting BIST Trading Platform - User Management Service...");
            log.info("Java Version: {}", System.getProperty("java.version"));
            log.info("Java Vendor: {}", System.getProperty("java.vendor"));
            log.info("Operating System: {} {}",
                System.getProperty("os.name"),
                System.getProperty("os.version"));
            log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
            log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);

            var context = SpringApplication.run(UserManagementServiceApplication.class, args);

            log.info("=".repeat(80));
            log.info("🚀 BIST Trading Platform - User Management Service started successfully!");
            log.info("🌐 Server running on port: 8081");
            log.info("📖 API Documentation: http://localhost:8081/swagger-ui.html");
            log.info("💚 Health Check: http://localhost:8081/actuator/health");
            log.info("📊 Metrics: http://localhost:8081/actuator/metrics");
            log.info("🔧 Environment: {}",
                context.getEnvironment().getActiveProfiles().length > 0 ?
                String.join(",", context.getEnvironment().getActiveProfiles()) : "default");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("❌ Failed to start User Management Service", e);
            System.exit(1);
        }
    }
}