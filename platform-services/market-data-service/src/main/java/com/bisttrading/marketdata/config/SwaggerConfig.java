package com.bisttrading.marketdata.config;

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
 * Swagger/OpenAPI 3.0 configuration for Market Data Service
 *
 * Swagger UI URL: http://localhost:8082/swagger-ui.html
 * OpenAPI JSON: http://localhost:8082/v3/api-docs
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
    public OpenAPI marketDataOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags());
    }

    private Info createApiInfo() {
        return new Info()
                .title("BIST Trading Platform - Market Data Service")
                .description(
                    """
                    **Market Data Service API Documentation**

                    Bu servis, BIST Trading Platform'un gerçek zamanlı piyasa verisi işleme, analiz ve dağıtım işlemlerini gerçekleştirir.

                    ## 📊 Piyasa Verisi Özellikleri

                    ### Gerçek Zamanlı Veri
                    - **Yüksek Performans**: 50,000+ tick/saniye işleme kapasitesi
                    - **Düşük Latency**: <50ms end-to-end gecikme
                    - **WebSocket Streaming**: Real-time veri dağıtımı
                    - **Data Integrity**: Veri bütünlüğü kontrolleri

                    ### Desteklenen Veri Türleri
                    - **Market Ticks**: Anlık fiyat ve hacim verileri
                    - **OHLCV Data**: Open, High, Low, Close, Volume verileri (1m, 5m, 15m, 1h, 1d)
                    - **Order Book**: Emir defteri derinlik verileri (Level 2)
                    - **Trade History**: İşlem geçmişi ve analitik veriler

                    ### BIST Piyasa Desteği
                    - **Tüm BIST Sembolleri**: BIST 30, BIST 100, tüm hisse senetleri
                    - **Turkish Market Hours**: 10:00-18:00 İstanbul saati
                    - **Local Timezone**: Europe/Istanbul timezone desteği
                    - **Currency**: Turkish Lira (TRY) desteği

                    ## 🔐 Güvenlik ve Kimlik Doğrulama

                    ### JWT Token Kullanımı
                    1. User Management Service'den token alın (`POST /api/auth/login`)
                    2. WebSocket bağlantısı için: `ws://localhost:8082/ws/market-data?token=YOUR_JWT_TOKEN`
                    3. REST API çağrıları için: `Authorization: Bearer <token>` header'ı
                    4. Token süresi: 15 dakika (refresh gerekli)

                    ### Subscription Levels
                    - **Basic**: Delayed data (15 dakika gecikme)
                    - **Premium**: Real-time data + analytics
                    - **Professional**: Full market depth + historical data

                    ## 📡 WebSocket API Kullanımı

                    ```javascript
                    // WebSocket bağlantısı kurma
                    const ws = new WebSocket('ws://localhost:8082/ws/market-data?token=YOUR_JWT_TOKEN');

                    // Sembol aboneliği
                    ws.send(JSON.stringify({
                      type: 'SUBSCRIBE',
                      symbols: ['AKBNK', 'THYAO', 'GARAN'],
                      dataTypes: ['QUOTE', 'TRADE', 'DEPTH']
                    }));

                    // Veri alma
                    ws.onmessage = (event) => {
                      const data = JSON.parse(event.data);
                      if (data.type === 'QUOTE') {
                        console.log('Price update:', data.symbol, data.data.lastPrice);
                      }
                    };
                    ```

                    ## 📊 REST API Örnekleri

                    ```bash
                    # 1. Multi-timeframe OHLCV verisi
                    curl -X GET "http://localhost:8082/api/v1/market-data/ohlcv/AKBNK?startTime=2024-09-24T10:00:00Z&endTime=2024-09-24T18:00:00Z" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"

                    # 2. Volume analizi
                    curl -X GET "http://localhost:8082/api/v1/market-data/volume/THYAO?startTime=2024-09-24T10:00:00Z&endTime=2024-09-24T18:00:00Z" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"

                    # 3. Teknik analiz indikatörleri
                    curl -X GET "http://localhost:8082/api/v1/market-data/technical/GARAN?timeframe=1%20hour&startTime=2024-09-24T10:00:00Z&endTime=2024-09-24T18:00:00Z" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"

                    # 4. Emir defteri analizi
                    curl -X GET "http://localhost:8082/api/v1/market-data/orderbook/ISCTR?startTime=2024-09-24T10:00:00Z&endTime=2024-09-24T18:00:00Z" \\
                      -H "Authorization: Bearer YOUR_JWT_TOKEN"
                    ```

                    ## ⚡ Performans Özellikleri

                    ### TimescaleDB Integration
                    - **Hypertables**: Otomatik zaman bazlı partitioning
                    - **Compression**: 1 gün sonra otomatik sıkıştırma
                    - **Retention Policy**: 1 yıl veri saklama
                    - **Continuous Aggregates**: Real-time OHLCV hesaplamaları

                    ### Caching Strategy
                    - **Redis Cache**: Latest price cache (sub-second access)
                    - **Application Cache**: Frequently accessed historical data
                    - **CDN**: Static market data (symbol lists, trading hours)

                    ### Monitoring & Observability
                    - **Real-time Metrics**: Processing rate, latency, error rates
                    - **Distributed Tracing**: End-to-end request tracking
                    - **Health Checks**: Data source connectivity monitoring

                    ## 📈 Analitik Özellikleri

                    ### Teknik İndikatörler
                    - **Moving Averages**: SMA, EMA (20, 50, 200 periods)
                    - **Oscillators**: RSI, MACD, Stochastic
                    - **Bollinger Bands**: Standard deviation bands
                    - **Volume Indicators**: VWAP, Volume Profile

                    ### Market Microstructure
                    - **Bid-Ask Spread Analysis**: Liquidity metrics
                    - **Order Book Imbalance**: Market depth analysis
                    - **Tick Direction**: Price movement correlation
                    - **Market Impact**: Price impact of large trades

                    ## 🚨 Rate Limiting & Fair Usage

                    ### API Rate Limits
                    - **REST API**: 100 requests/minute per user
                    - **WebSocket**: 10 subscriptions/second
                    - **Historical Data**: 50 requests/hour
                    - **Real-time Streams**: 1000 messages/second per connection

                    ### Data Usage Policies
                    - Market data is for personal/internal use only
                    - Redistribution requires separate license
                    - Commercial usage terms apply for business accounts
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
                        .url("http://localhost:8082")
                        .description("Local Development Server"),
                new Server()
                        .url("wss://localhost:8082")
                        .description("Local WebSocket Server"),
                new Server()
                        .url("https://market-api-dev.bist-trading.com")
                        .description("Development Environment"),
                new Server()
                        .url("wss://market-api-dev.bist-trading.com")
                        .description("Development WebSocket"),
                new Server()
                        .url("https://market-api-staging.bist-trading.com")
                        .description("Staging Environment"),
                new Server()
                        .url("https://market-api.bist-trading.com")
                        .description("Production Environment"),
                new Server()
                        .url("wss://market-api.bist-trading.com")
                        .description("Production WebSocket")
        );
    }

    private List<Tag> createTags() {
        return List.of(
                new Tag()
                        .name("Market Data")
                        .description("Temel piyasa verisi işlemleri - OHLCV, ticks, real-time prices"),

                new Tag()
                        .name("Technical Analysis")
                        .description("Teknik analiz indikatörleri - SMA, RSI, Bollinger Bands, trend analizi"),

                new Tag()
                        .name("Volume Analysis")
                        .description("Hacim analizi - VWAP, volume profile, price impact analysis"),

                new Tag()
                        .name("Order Book Analytics")
                        .description("Emir defteri analitiği - spread analizi, liquidity depth, imbalance tracking"),

                new Tag()
                        .name("Market Microstructure")
                        .description("Piyasa mikro yapısı - bid-ask spread, tick analysis, market thickness"),

                new Tag()
                        .name("Historical Data")
                        .description("Tarihsel veri sorguları - geçmiş fiyatlar, volume data, OHLCV archives"),

                new Tag()
                        .name("Market Overview")
                        .description("Piyasa genel görünümü - active symbols, top movers, market statistics"),

                new Tag()
                        .name("WebSocket Streaming")
                        .description("Real-time veri streaming - live quotes, trade updates, order book changes"),

                new Tag()
                        .name("Data Quality")
                        .description("Veri kalitesi ve monitoring - data validation, completeness checks"),

                new Tag()
                        .name("Health & Monitoring")
                        .description("Sistem durumu ve izleme - health check, metrics, data source connectivity")
        );
    }
}