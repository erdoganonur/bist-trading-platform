package com.bisttrading.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for BIST Trading Platform Monolith.
 */
@Configuration
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
            - 🏦 **Broker Entegrasyonu**: AlgoLab broker entegrasyonu
            - 🇹🇷 **Türk Mevzuatı**: TCKN doğrulama ve yasal uyumluluk
            - ⚡ **Gerçek Zamanlı**: WebSocket üzerinden canlı veri akışı
            - 📱 **Modern API**: RESTful API ve OpenAPI dokümantasyonu

            ## 🔧 Teknik Detaylar
            - **Mimari**: Basitleştirilmiş Monolith (Gerçek Tek Uygulama)
            - **Port**: 8080 (Tek port, tek uygulama)
            - **Database**: PostgreSQL + TimescaleDB
            - **Cache**: Redis
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
            - `/api/v1/users/*` - Kullanıcı profili yönetimi
            - `/api/v1/market-data/*` - Piyasa verisi ve analizler
            - `/api/v1/broker/*` - Broker entegrasyonu ve işlemler

            ## 📞 Destek
            Herhangi bir sorun için support@bisttrading.com adresine başvurabilirsiniz.
            """,
        contact = @Contact(
            name = "BIST Trading Platform Team",
            url = "https://bisttrading.com",
            email = "support@bisttrading.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://bisttrading.com/license"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development Server (Simplified Monolith)"),
        @Server(url = "https://api-dev.bisttrading.com", description = "Development Environment"),
        @Server(url = "https://api.bisttrading.com", description = "Production Environment")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token. Obtain from /api/v1/auth/login"
)
public class OpenApiConfig {

}