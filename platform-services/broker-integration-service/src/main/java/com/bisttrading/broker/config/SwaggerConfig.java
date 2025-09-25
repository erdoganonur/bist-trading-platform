package com.bisttrading.broker.config;

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
 * Swagger/OpenAPI 3.0 configuration for Broker Integration Service
 *
 * Swagger UI URL: http://localhost:8083/swagger-ui.html
 * OpenAPI JSON: http://localhost:8083/v3/api-docs
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT Bearer Token authentication. " +
                  "Obtain token from User Management Service /api/auth/login endpoint and use it in Authorization header."
)
public class SwaggerConfig {

    @Bean
    public OpenAPI brokerIntegrationOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags());
    }

    private Info createApiInfo() {
        return new Info()
                .title("BIST Trading Platform - Broker Integration Service")
                .description(
                    """
                    **Broker Integration Service API Documentation**

                    Bu servis, BIST Trading Platform'un AlgoLab broker entegrasyonu ve emir yönetimi işlemlerini gerçekleştirir.

                    ## 🏛️ AlgoLab Broker Entegrasyonu

                    ### Desteklenen İşlemler
                    - **Emir İşlemleri**: Limit/Market emirleri, emir değiştirme, emir iptal etme
                    - **Portföy Yönetimi**: Mevcut pozisyonlar, bakiye sorguları
                    - **İşlem Geçmişi**: Geçmiş işlemler, kar/zarar raporları
                    - **Risk Yönetimi**: Position limits, günlük işlem limitleri

                    ### BIST Piyasa Kuralları
                    - **İşlem Saatleri**: 10:00-18:00 (İstanbul saati)
                    - **Desteklenen Semboller**: AKBNK, THYAO, GARAN, ISCTR, vb.
                    - **Minimum Lot**: 1 lot (genelde 100 adet)
                    - **Fiyat Adımları**: Sembol bazında değişken (0.01 TL - 0.25 TL)

                    ## 🔐 Güvenlik ve Kimlik Doğrulama

                    ### JWT Token Kullanımı
                    1. User Management Service'den token alın (`POST /api/auth/login`)
                    2. Tüm API çağrılarında `Authorization: Bearer <token>` header'ını kullanın
                    3. Token süresi: 15 dakika (refresh token ile yenileyin)

                    ### AlgoLab API Güvenliği
                    - Tüm broker API çağrıları şifrelenir
                    - API key ve code güvenli şekilde saklanır
                    - Rate limiting: Saniye başına 10 istek
                    - Request signing ile additional security

                    ## 📊 Örnek API Kullanımı

                    ```bash
                    # 1. Limit Emri Verme
                    curl -X POST "http://localhost:8083/api/trading/orders" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "symbol": "AKBNK",
                        "side": "BUY",
                        "orderType": "LIMIT",
                        "quantity": 1000,
                        "price": 15.75,
                        "timeInForce": "DAY"
                      }'

                    # 2. Portföy Görüntüleme
                    curl -X GET "http://localhost:8083/api/trading/portfolio" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"

                    # 3. Emir Durumu Sorgulama
                    curl -X GET "http://localhost:8083/api/trading/orders/ORD-123456789" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"

                    # 4. Emir İptal Etme
                    curl -X DELETE "http://localhost:8083/api/trading/orders/ORD-123456789" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"
                    ```

                    ## ⚡ Performans Özellikleri

                    - **Düşük Latency**: <100ms emir execution süresi
                    - **Yüksek Throughput**: Saniye başına 1000+ emir işleme kapasitesi
                    - **Real-time Updates**: WebSocket ile gerçek zamanlı emir durumu güncellemeleri
                    - **Fault Tolerance**: Circuit breaker pattern ile hata toleransı
                    - **Monitoring**: Comprehensive metrics ve alerting

                    ## 🚨 Risk Yönetimi

                    ### Otomatik Risk Kontrolleri
                    - **Pre-trade Risk**: Bakiye kontrolü, position limits
                    - **Real-time Risk**: Günlük zarar limitleri, concentration limits
                    - **Post-trade Risk**: Settlement monitoring, margin requirements

                    ### Compliance
                    - SPK (Sermaye Piyasası Kurulu) kurallarına uyumluluk
                    - BIST trading rules enforcement
                    - Audit trail for all trading activities
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
                        .url("http://localhost:8083")
                        .description("Local Development Server"),
                new Server()
                        .url("https://trading-api-dev.bist-trading.com")
                        .description("Development Environment"),
                new Server()
                        .url("https://trading-api-staging.bist-trading.com")
                        .description("Staging Environment"),
                new Server()
                        .url("https://trading-api.bist-trading.com")
                        .description("Production Environment")
        );
    }

    private List<Tag> createTags() {
        return List.of(
                new Tag()
                        .name("Order Management")
                        .description("Emir yönetimi işlemleri - emir verme, değiştirme, iptal etme, durum sorgulama"),

                new Tag()
                        .name("Portfolio Management")
                        .description("Portföy yönetimi - mevcut pozisyonlar, bakiye sorguları, kar/zarar hesaplamaları"),

                new Tag()
                        .name("Transaction History")
                        .description("İşlem geçmişi - geçmiş emirler, executed trades, settlement bilgileri"),

                new Tag()
                        .name("Risk Management")
                        .description("Risk yönetimi - position limits, günlük limitler, risk parametreleri"),

                new Tag()
                        .name("Market Information")
                        .description("Piyasa bilgileri - sembol listesi, lot büyüklükleri, işlem saatleri"),

                new Tag()
                        .name("AlgoLab Integration")
                        .description("AlgoLab broker entegrasyonu - API connectivity, authentication, data sync"),

                new Tag()
                        .name("WebSocket Notifications")
                        .description("Real-time bildirimler - emir durumu güncellemeleri, execution notifications"),

                new Tag()
                        .name("Health & Monitoring")
                        .description("Sistem durumu ve izleme - health check, metrics, broker connectivity status")
        );
    }
}