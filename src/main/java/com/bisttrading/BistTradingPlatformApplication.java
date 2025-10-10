package com.bisttrading;

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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * BIST Trading Platform - Complete Monolith Application
 *
 * This is the unified trading platform that consolidates all functionality:
 * - User management and authentication
 * - Market data analysis and streaming
 * - Order management and execution
 * - Broker integration (AlgoLab)
 * - Real-time notifications
 *
 * Features:
 * ✅ User registration and authentication
 * ✅ JWT token-based security with RBAC
 * ✅ Market data analysis (OHLCV, technical indicators)
 * ✅ Order management (create, modify, cancel orders)
 * ✅ AlgoLab broker integration (mock implementation)
 * ✅ Turkish regulatory compliance (TCKN validation)
 * ✅ Real-time WebSocket streaming
 * ✅ Comprehensive REST API
 * ✅ OpenAPI/Swagger documentation
 *
 * Port: 8080
 * Base URL: http://localhost:8080
 * API Documentation: http://localhost:8080/swagger-ui.html
 * Health Check: http://localhost:8080/actuator/health
 */
@Slf4j
@SpringBootApplication
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "BIST Trading Platform - Complete Trading System",
        version = "2.0.0-SIMPLIFIED",
        description = """
            🚀 BIST Trading Platform - Gerçek Monolith Mimarisi

            Bu platform, Borsa İstanbul (BIST) için geliştirilmiş kapsamlı bir ticaret platformudur.
            Tüm fonksiyonalite tek bir uygulamada birleştirilmiştir.

            ## ✨ Ana Özellikler
            - 👤 **Kullanıcı Yönetimi**: Kayıt, giriş, profil yönetimi
            - 🔐 **Güvenlik**: JWT token tabanlı kimlik doğrulama ve yetkilendirme
            - 📊 **Piyasa Verisi**: Gerçek zamanlı OHLCV verileri ve teknik analizler
            - 📋 **Emir Yönetimi**: Emir oluşturma, değiştirme, iptal etme
            - 🏦 **Broker Entegrasyonu**: AlgoLab broker entegrasyonu (mock)
            - 🇹🇷 **Türk Mevzuatı**: TCKN doğrulama ve yasal uyumluluk
            - ⚡ **Gerçek Zamanlı**: WebSocket üzerinden canlı veri akışı
            - 📱 **Modern API**: RESTful API ve OpenAPI dokümantasyonu

            ## 🔧 Teknik Detaylar
            - **Mimari**: Basitleştirilmiş Monolith (Gerçek Tek Uygulama)
            - **Port**: 8080 (Tek port, tek uygulama)
            - **Database**: PostgreSQL
            - **Security**: Spring Security + JWT
            - **Documentation**: OpenAPI 3.0 + Swagger UI

            ## 🔐 Güvenlik
            Korumalı endpoint'ler için Authorization header'ında Bearer token gereklidir:
            ```
            Authorization: Bearer <access_token>
            ```

            Token almak için: `POST /api/v1/auth/login`

            ## 📋 API Grupları
            - `/api/v1/auth/*` - Kimlik doğrulama ve yetkilendirme
            - `/api/v1/profile/*` - Kullanıcı profili yönetimi
            - `/api/v1/market-data/*` - Piyasa verisi ve analizler
            - `/api/v1/orders/*` - Emir yönetimi
            - `/api/v1/broker/*` - Broker entegrasyonu

            ## 📞 Destek
            Herhangi bir sorun için support@bisttrading.com adresine başvurabilirsiniz.
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
            url = "http://localhost:8080",
            description = "Development Server (Simplified Monolith)"
        ),
        @Server(
            url = "https://api-dev.bisttrading.com",
            description = "Development Environment"
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
    description = "JWT Bearer token authentication. Get token from /api/v1/auth/login"
)
public class BistTradingPlatformApplication {

    /**
     * Application entry point for the unified BIST Trading Platform.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Set system properties for optimal performance and Turkish locale
        System.setProperty("spring.output.ansi.enabled", "always");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "Europe/Istanbul");
        System.setProperty("user.language", "tr");
        System.setProperty("user.country", "TR");

        // Enhanced logging pattern for monolith
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
            log.info("🚀 Starting BIST Trading Platform - REAL Monolith Architecture...");
            log.info("📋 System Information:");
            log.info("   └─ Java Version: {}", System.getProperty("java.version"));
            log.info("   └─ Java Vendor: {}", System.getProperty("java.vendor"));
            log.info("   └─ Operating System: {} {}",
                System.getProperty("os.name"),
                System.getProperty("os.version"));
            log.info("   └─ Available Processors: {}", Runtime.getRuntime().availableProcessors());
            log.info("   └─ Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
            log.info("   └─ Architecture: Simplified Monolith (Real Single Application)");

            var context = SpringApplication.run(BistTradingPlatformApplication.class, args);

            // Success banner
            String banner = """
                ╔══════════════════════════════════════════════════════════════════════════════╗
                ║  🎉 BIST Trading Platform - GERÇEK MONOLİTH BAŞARIYLA BAŞLATILDI!           ║
                ╠══════════════════════════════════════════════════════════════════════════════╣
                ║  🌐 Server Port: 8080                                                       ║
                ║  📖 API Docs: http://localhost:8080/swagger-ui.html                        ║
                ║  💚 Health: http://localhost:8080/actuator/health                          ║
                ║  📊 Metrics: http://localhost:8080/actuator/metrics                        ║
                ║  🔧 Environment: %-59s ║
                ║                                                                              ║
                ║  ✅ Tüm Servisler Tek Uygulamada Birleştirildi:                            ║
                ║     • User Management     • Market Data Analysis                           ║
                ║     • Order Management    • Broker Integration                             ║
                ║     • Authentication      • Real-time Streaming                            ║
                ║                                                                              ║
                ║  🎯 Artık gerçek bir monolith! Tek port, tek process, kolay yönetim.       ║
                ╚══════════════════════════════════════════════════════════════════════════════╝
                """;

            String environment = context.getEnvironment().getActiveProfiles().length > 0 ?
                String.join(",", context.getEnvironment().getActiveProfiles()) : "default";

            log.info(banner, environment);

        } catch (Exception e) {
            log.error("❌ Failed to display startup banner (non-critical)", e);
            // Don't exit - startup banner is not critical
        }
    }
}