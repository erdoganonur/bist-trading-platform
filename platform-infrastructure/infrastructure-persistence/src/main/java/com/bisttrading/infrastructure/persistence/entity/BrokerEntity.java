package com.bisttrading.infrastructure.persistence.entity;

import com.bisttrading.infrastructure.persistence.converter.FieldEncryptionConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Broker entity for BIST Trading Platform.
 * Represents external brokers and trading service providers.
 */
@Entity
@Table(name = "brokers", indexes = {
    @Index(name = "idx_brokers_broker_code", columnList = "broker_code"),
    @Index(name = "idx_brokers_name", columnList = "name"),
    @Index(name = "idx_brokers_status", columnList = "status"),
    @Index(name = "idx_brokers_broker_type", columnList = "broker_type"),
    @Index(name = "idx_brokers_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class BrokerEntity {

    /**
     * Primary key - UUID.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    /**
     * Broker code (unique identifier).
     */
    @Column(name = "broker_code", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Broker kodu boş olamaz")
    @Size(max = 20, message = "Broker kodu en fazla 20 karakter olabilir")
    private String brokerCode;

    /**
     * Broker name.
     */
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Broker adı boş olamaz")
    @Size(max = 255, message = "Broker adı en fazla 255 karakter olabilir")
    private String name;

    /**
     * Broker display name.
     */
    @Column(name = "display_name", length = 255)
    @Size(max = 255, message = "Görünen ad en fazla 255 karakter olabilir")
    private String displayName;

    /**
     * Broker description.
     */
    @Column(name = "description", length = 1000)
    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    private String description;

    /**
     * Broker type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 20)
    @NotNull(message = "Broker türü boş olamaz")
    private BrokerType brokerType;

    /**
     * Broker status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Broker durumu boş olamaz")
    private BrokerStatus status;

    /**
     * Broker website URL.
     */
    @Column(name = "website_url", length = 255)
    @Size(max = 255, message = "Website URL en fazla 255 karakter olabilir")
    private String websiteUrl;

    /**
     * Broker API endpoint URL.
     */
    @Column(name = "api_endpoint", length = 255)
    @Size(max = 255, message = "API endpoint en fazla 255 karakter olabilir")
    private String apiEndpoint;

    /**
     * API version supported.
     */
    @Column(name = "api_version", length = 20)
    @Size(max = 20, message = "API versiyonu en fazla 20 karakter olabilir")
    private String apiVersion;

    /**
     * API key for authentication - ENCRYPTED.
     */
    @Column(name = "api_key", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String apiKey;

    /**
     * API secret for authentication - ENCRYPTED.
     */
    @Column(name = "api_secret", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String apiSecret;

    /**
     * Whether API connection uses SSL/TLS.
     */
    @Column(name = "ssl_enabled", nullable = false)
    @Builder.Default
    private Boolean sslEnabled = true;

    /**
     * Connection timeout in milliseconds.
     */
    @Column(name = "connection_timeout")
    @Positive(message = "Bağlantı timeout'u pozitif olmalıdır")
    private Integer connectionTimeout;

    /**
     * Read timeout in milliseconds.
     */
    @Column(name = "read_timeout")
    @Positive(message = "Okuma timeout'u pozitif olmalıdır")
    private Integer readTimeout;

    /**
     * Maximum number of concurrent connections.
     */
    @Column(name = "max_connections")
    @Positive(message = "Maksimum bağlantı sayısı pozitif olmalıdır")
    private Integer maxConnections;

    /**
     * Rate limit per minute.
     */
    @Column(name = "rate_limit_per_minute")
    @PositiveOrZero(message = "Dakika başına istek limiti negatif olamaz")
    private Integer rateLimitPerMinute;

    /**
     * Commission rate for trades (percentage).
     */
    @Column(name = "commission_rate", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Komisyon oranı negatif olamaz")
    @DecimalMax(value = "100.0", message = "Komisyon oranı %100'den fazla olamaz")
    private BigDecimal commissionRate;

    /**
     * Minimum commission amount.
     */
    @Column(name = "minimum_commission", precision = 10, scale = 2)
    @PositiveOrZero(message = "Minimum komisyon negatif olamaz")
    private BigDecimal minimumCommission;

    /**
     * Maximum commission amount.
     */
    @Column(name = "maximum_commission", precision = 10, scale = 2)
    @PositiveOrZero(message = "Maksimum komisyon negatif olamaz")
    private BigDecimal maximumCommission;

    /**
     * Currency code for commission.
     */
    @Column(name = "commission_currency", length = 3)
    @Size(max = 3, message = "Para birimi kodu en fazla 3 karakter olabilir")
    private String commissionCurrency;

    /**
     * Whether broker supports real-time data.
     */
    @Column(name = "supports_realtime_data", nullable = false)
    @Builder.Default
    private Boolean supportsRealtimeData = false;

    /**
     * Whether broker supports historical data.
     */
    @Column(name = "supports_historical_data", nullable = false)
    @Builder.Default
    private Boolean supportsHistoricalData = false;

    /**
     * Whether broker supports order management.
     */
    @Column(name = "supports_order_management", nullable = false)
    @Builder.Default
    private Boolean supportsOrderManagement = false;

    /**
     * Whether broker supports portfolio tracking.
     */
    @Column(name = "supports_portfolio_tracking", nullable = false)
    @Builder.Default
    private Boolean supportsPortfolioTracking = false;

    /**
     * Supported order types (JSON array).
     */
    @Type(JsonType.class)
    @Column(name = "supported_order_types", columnDefinition = "jsonb")
    private java.util.List<String> supportedOrderTypes;

    /**
     * Supported markets/exchanges (JSON array).
     */
    @Type(JsonType.class)
    @Column(name = "supported_markets", columnDefinition = "jsonb")
    private java.util.List<String> supportedMarkets;

    /**
     * Supported instrument types (JSON array).
     */
    @Type(JsonType.class)
    @Column(name = "supported_instruments", columnDefinition = "jsonb")
    private java.util.List<String> supportedInstruments;

    /**
     * Broker contact email.
     */
    @Column(name = "contact_email", length = 255)
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 255, message = "İletişim email'i en fazla 255 karakter olabilir")
    private String contactEmail;

    /**
     * Broker contact phone - ENCRYPTED.
     */
    @Column(name = "contact_phone", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String contactPhone;

    /**
     * Technical support email.
     */
    @Column(name = "support_email", length = 255)
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 255, message = "Destek email'i en fazla 255 karakter olabilir")
    private String supportEmail;

    /**
     * Technical support phone - ENCRYPTED.
     */
    @Column(name = "support_phone", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String supportPhone;

    /**
     * Last successful connection timestamp.
     */
    @Column(name = "last_connection_at")
    private LocalDateTime lastConnectionAt;

    /**
     * Last connection error message.
     */
    @Column(name = "last_connection_error", length = 1000)
    @Size(max = 1000, message = "Bağlantı hatası en fazla 1000 karakter olabilir")
    private String lastConnectionError;

    /**
     * Connection success rate (percentage).
     */
    @Column(name = "connection_success_rate", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Bağlantı başarı oranı negatif olamaz")
    @DecimalMax(value = "100.0", message = "Bağlantı başarı oranı %100'den fazla olamaz")
    private BigDecimal connectionSuccessRate;

    /**
     * Average response time in milliseconds.
     */
    @Column(name = "average_response_time")
    @PositiveOrZero(message = "Ortalama yanıt süresi negatif olamaz")
    private Integer averageResponseTime;

    /**
     * Whether broker is enabled for trading.
     */
    @Column(name = "trading_enabled", nullable = false)
    @Builder.Default
    private Boolean tradingEnabled = false;

    /**
     * Priority order for broker selection.
     */
    @Column(name = "priority_order")
    @PositiveOrZero(message = "Öncelik sırası negatif olamaz")
    private Integer priorityOrder;

    /**
     * Broker configuration settings (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    /**
     * Additional metadata (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Record creation timestamp.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last modification timestamp.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Record deletion timestamp (soft delete).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Broker type enumeration.
     */
    public enum BrokerType {
        TRADITIONAL,     // Traditional brokerage firm
        DIGITAL,         // Digital/online broker
        ROBO_ADVISOR,    // Automated investment service
        BANK,            // Bank with brokerage services
        FINTECH,         // Financial technology broker
        MARKET_MAKER,    // Market making broker
        ECN,             // Electronic Communication Network
        OTHER            // Other broker type
    }

    /**
     * Broker status enumeration.
     */
    public enum BrokerStatus {
        ACTIVE,          // Active and available
        INACTIVE,        // Temporarily inactive
        MAINTENANCE,     // Under maintenance
        DEPRECATED,      // Deprecated, avoid new connections
        SUSPENDED,       // Suspended due to issues
        TESTING          // Testing phase
    }

    /**
     * Checks if broker is active and available.
     *
     * @return true if broker is active
     */
    public boolean isActive() {
        return BrokerStatus.ACTIVE.equals(status) && deletedAt == null;
    }

    /**
     * Checks if broker can be used for trading.
     *
     * @return true if trading is enabled
     */
    public boolean canTrade() {
        return isActive() && Boolean.TRUE.equals(tradingEnabled);
    }

    /**
     * Checks if broker API is properly configured.
     *
     * @return true if API configuration is complete
     */
    public boolean isApiConfigured() {
        return apiEndpoint != null && !apiEndpoint.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Gets broker display name or name.
     *
     * @return Display name if available, otherwise name
     */
    public String getDisplayNameOrName() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : name;
    }

    /**
     * Checks if broker connection is healthy.
     *
     * @return true if recent connection was successful
     */
    public boolean isConnectionHealthy() {
        return lastConnectionAt != null &&
               lastConnectionAt.isAfter(LocalDateTime.now().minusHours(1)) &&
               (connectionSuccessRate == null || connectionSuccessRate.compareTo(BigDecimal.valueOf(90)) >= 0);
    }

    /**
     * Updates connection statistics.
     *
     * @param success Whether connection was successful
     * @param responseTime Response time in milliseconds
     * @param errorMessage Error message if failed
     */
    public void updateConnectionStats(boolean success, Integer responseTime, String errorMessage) {
        this.lastConnectionAt = LocalDateTime.now();

        if (success) {
            this.lastConnectionError = null;
            if (responseTime != null) {
                // Update average response time (simple moving average)
                if (this.averageResponseTime == null) {
                    this.averageResponseTime = responseTime;
                } else {
                    this.averageResponseTime = (this.averageResponseTime + responseTime) / 2;
                }
            }
        } else {
            this.lastConnectionError = errorMessage;
        }
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void prePersist() {
        if (status == null) {
            status = BrokerStatus.INACTIVE;
        }
        if (brokerType == null) {
            brokerType = BrokerType.TRADITIONAL;
        }
        if (commissionCurrency == null) {
            commissionCurrency = "TRY";
        }
        if (connectionTimeout == null) {
            connectionTimeout = 30000; // 30 seconds
        }
        if (readTimeout == null) {
            readTimeout = 60000; // 60 seconds
        }
        if (maxConnections == null) {
            maxConnections = 10;
        }
    }
}