package com.bisttrading.user.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 configuration for User Management Service
 *
 * Swagger UI URL: http://localhost:8081/swagger-ui.html
 * OpenAPI JSON: http://localhost:8081/v3/api-docs
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT Bearer Token authentication. " +
                  "Obtain token from /api/auth/login endpoint and use it in Authorization header."
)
public class SwaggerConfig {

    @Bean
    public OpenAPI userManagementOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags());
    }

    private Info createApiInfo() {
        return new Info()
                .title("BIST Trading Platform - User Management Service")
                .description(
                    """
                    **User Management Service API Documentation**

                    Bu servis, BIST Trading Platform'un kullanıcı yönetimi ve kimlik doğrulama işlemlerini gerçekleştirir.

                    ## 🔐 Kimlik Doğrulama

                    ### JWT Token Kullanımı
                    1. **Giriş**: `/api/auth/login` endpoint'ini kullanarak giriş yapın
                    2. **Token Alma**: Başarılı girişten sonra `accessToken` ve `refreshToken` alırsınız
                    3. **Authorization Header**: API çağrılarında `Authorization: Bearer <token>` header'ını kullanın
                    4. **Token Yenileme**: Token süresi dolduğunda `/api/auth/refresh` endpoint'ini kullanın

                    ### Güvenlik Önlemleri
                    - Access Token süresi: 15 dakika
                    - Refresh Token süresi: 7 gün
                    - Rate limiting: Dakika başına 100 istek
                    - Failed login protection: 5 başarısız girişten sonra hesap kilitleme

                    ## 🇹🇷 Türk Piyasası Desteği

                    - TCKN (TC Kimlik Numarası) validasyonu
                    - Türkçe hata mesajları
                    - Istanbul timezone desteği
                    - Türk Lirası (TRY) desteği

                    ## 📊 Örnek API Kullanımı

                    ```bash
                    # 1. Kullanıcı kaydı
                    curl -X POST "http://localhost:8081/api/auth/register" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "firstName": "Ahmet",
                        "lastName": "Yılmaz",
                        "email": "ahmet.yilmaz@example.com",
                        "password": "SecurePassword123!",
                        "tcknNumber": "12345678901",
                        "phoneNumber": "+905551234567"
                      }'

                    # 2. Kullanıcı girişi
                    curl -X POST "http://localhost:8081/api/auth/login" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "username": "ahmet.yilmaz@example.com",
                        "password": "SecurePassword123!"
                      }'

                    # 3. Kullanıcı profili görüntüleme
                    curl -X GET "http://localhost:8081/api/users/profile" \\
                      -H "Authorization: Bearer eyJ0eXAiOiJKV1Q..."
                    ```

                    ## 🚀 Performans Özellikleri

                    - **Yüksek Performans**: Redis cache ile session yönetimi
                    - **Ölçeklenebilirlik**: Horizontal scaling desteği
                    - **Güvenilirlik**: 99.9% uptime hedefi
                    - **İzlenebilirlik**: Comprehensive logging ve metrics
                    """
                )
                .version("1.0.0")
                .contact(new Contact()
                        .name("BIST Trading Platform Development Team")
                        .email("dev-team@bist-trading.com")
                        .url("https://github.com/your-org/bist-trading-platform"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8081")
                        .description("Local Development Server"),
                new Server()
                        .url("https://user-api-dev.bist-trading.com")
                        .description("Development Environment"),
                new Server()
                        .url("https://user-api-staging.bist-trading.com")
                        .description("Staging Environment"),
                new Server()
                        .url("https://user-api.bist-trading.com")
                        .description("Production Environment")
        );
    }

    private List<Tag> createTags() {
        return List.of(
                new Tag()
                        .name("Authentication")
                        .description("Kullanıcı kimlik doğrulama işlemleri - giriş, çıkış, kayıt, token yenileme"),

                new Tag()
                        .name("User Management")
                        .description("Kullanıcı profil yönetimi - profil görüntüleme, güncelleme, şifre değişikliği"),

                new Tag()
                        .name("Account Verification")
                        .description("Hesap doğrulama işlemleri - email doğrulama, telefon doğrulama, TCKN kontrolü"),

                new Tag()
                        .name("Security")
                        .description("Güvenlik işlemleri - şifre sıfırlama, iki faktörlü kimlik doğrulama"),

                new Tag()
                        .name("Admin Operations")
                        .description("Yönetici işlemleri - kullanıcı yönetimi, sistem ayarları (Sadece ADMIN rolü)"),

                new Tag()
                        .name("Health & Monitoring")
                        .description("Sistem durumu ve izleme - health check, metrics, actuator endpoints")
        );
    }
}