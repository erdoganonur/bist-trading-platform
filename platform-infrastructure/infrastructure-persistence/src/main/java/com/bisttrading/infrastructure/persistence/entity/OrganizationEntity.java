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
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Organization entity for BIST Trading Platform.
 * Represents trading organizations like brokers, banks, and investment firms.
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_code", columnList = "organization_code"),
    @Index(name = "idx_organizations_type", columnList = "organization_type"),
    @Index(name = "idx_organizations_status", columnList = "status"),
    @Index(name = "idx_organizations_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrganizationEntity {

    /**
     * Primary key - UUID.
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    /**
     * Organization code (unique identifier).
     */
    @Column(name = "organization_code", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Organizasyon kodu boş olamaz")
    @Size(max = 20, message = "Organizasyon kodu en fazla 20 karakter olabilir")
    private String organizationCode;

    /**
     * Organization name.
     */
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Organizasyon adı boş olamaz")
    @Size(max = 255, message = "Organizasyon adı en fazla 255 karakter olabilir")
    private String name;

    /**
     * Organization display name.
     */
    @Column(name = "display_name", length = 255)
    @Size(max = 255, message = "Görünen ad en fazla 255 karakter olabilir")
    private String displayName;

    /**
     * Organization trade name.
     */
    @Column(name = "trade_name", length = 255)
    @Size(max = 255, message = "Ticari isim en fazla 255 karakter olabilir")
    private String tradeName;

    /**
     * Organization type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 20)
    @NotNull(message = "Organizasyon türü boş olamaz")
    private OrganizationType organizationType;

    /**
     * Organization status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Organizasyon durumu boş olamaz")
    private OrganizationStatus status;

    /**
     * Tax identification number - ENCRYPTED.
     */
    @Column(name = "tax_id", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String taxId;

    /**
     * Trade registry number - ENCRYPTED.
     */
    @Column(name = "trade_registry_no", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String tradeRegistryNo;

    /**
     * Organization website URL.
     */
    @Column(name = "website_url", length = 255)
    @Size(max = 255, message = "Website URL en fazla 255 karakter olabilir")
    private String websiteUrl;

    /**
     * Organization email address.
     */
    @Column(name = "email", length = 255)
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 255, message = "Email adresi en fazla 255 karakter olabilir")
    private String email;

    /**
     * Organization phone number - ENCRYPTED.
     */
    @Column(name = "phone_number", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String phoneNumber;

    /**
     * Organization fax number.
     */
    @Column(name = "fax_number", length = 50)
    @Size(max = 50, message = "Faks numarası en fazla 50 karakter olabilir")
    private String faxNumber;

    /**
     * Organization address.
     */
    @Column(name = "address", length = 500)
    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String address;

    /**
     * Organization city.
     */
    @Column(name = "city", length = 100)
    @Size(max = 100, message = "Şehir en fazla 100 karakter olabilir")
    private String city;

    /**
     * Organization postal code.
     */
    @Column(name = "postal_code", length = 20)
    @Size(max = 20, message = "Posta kodu en fazla 20 karakter olabilir")
    private String postalCode;

    /**
     * Organization country.
     */
    @Column(name = "country", length = 3)
    @Size(max = 3, message = "Ülke kodu en fazla 3 karakter olabilir")
    private String country;

    /**
     * License number from regulatory authority.
     */
    @Column(name = "license_number", length = 100)
    @Size(max = 100, message = "Lisans numarası en fazla 100 karakter olabilir")
    private String licenseNumber;

    /**
     * License issuing authority.
     */
    @Column(name = "license_authority", length = 100)
    @Size(max = 100, message = "Lisans otoritesi en fazla 100 karakter olabilir")
    private String licenseAuthority;

    /**
     * License issue date.
     */
    @Column(name = "license_issued_at")
    private LocalDateTime licenseIssuedAt;

    /**
     * License expiry date.
     */
    @Column(name = "license_expires_at")
    private LocalDateTime licenseExpiresAt;

    /**
     * Whether organization is active for trading.
     */
    @Column(name = "trading_enabled", nullable = false)
    @Builder.Default
    private Boolean tradingEnabled = false;

    /**
     * Maximum number of users allowed.
     */
    @Column(name = "max_users")
    @PositiveOrZero(message = "Maksimum kullanıcı sayısı negatif olamaz")
    private Integer maxUsers;

    /**
     * Current number of active users.
     */
    @Column(name = "current_users", nullable = false)
    @Builder.Default
    @PositiveOrZero(message = "Mevcut kullanıcı sayısı negatif olamaz")
    private Integer currentUsers = 0;

    /**
     * Organization contact person name.
     */
    @Column(name = "contact_person", length = 255)
    @Size(max = 255, message = "İletişim kişisi en fazla 255 karakter olabilir")
    private String contactPerson;

    /**
     * Contact person email.
     */
    @Column(name = "contact_email", length = 255)
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 255, message = "İletişim email'i en fazla 255 karakter olabilir")
    private String contactEmail;

    /**
     * Contact person phone - ENCRYPTED.
     */
    @Column(name = "contact_phone", length = 500)
    @Convert(converter = FieldEncryptionConverter.class)
    private String contactPhone;

    /**
     * Organization settings and configuration (JSONB).
     */
    @Type(JsonType.class)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;

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
     * Organization type enumeration.
     */
    public enum OrganizationType {
        BROKER,           // Investment broker
        BROKERAGE,        // Brokerage firm
        BANK,             // Bank with investment services
        INVESTMENT_FIRM,  // Investment management firm
        ASSET_MANAGER,    // Asset management company
        FUND_MANAGER,     // Fund management company
        FINTECH,          // Financial technology company
        TRADING_FIRM,     // Proprietary trading firm
        MARKET_MAKER,     // Market making firm
        CUSTODIAN,        // Custody services
        OTHER             // Other organization type
    }

    /**
     * Organization status enumeration.
     */
    public enum OrganizationStatus {
        PENDING,     // Awaiting approval
        ACTIVE,      // Active and operational
        SUSPENDED,   // Temporarily suspended
        INACTIVE,    // Deactivated
        CLOSED       // Permanently closed
    }

    /**
     * Checks if organization is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return OrganizationStatus.ACTIVE.equals(status) && deletedAt == null;
    }

    /**
     * Checks if organization can add more users.
     *
     * @return true if can add users
     */
    public boolean canAddUsers() {
        return isActive() && (maxUsers == null || currentUsers < maxUsers);
    }

    /**
     * Checks if organization has valid license.
     *
     * @return true if license is valid
     */
    public boolean hasValidLicense() {
        return licenseNumber != null &&
               licenseExpiresAt != null &&
               licenseExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Checks if organization can trade.
     *
     * @return true if trading is enabled
     */
    public boolean canTrade() {
        return isActive() &&
               Boolean.TRUE.equals(tradingEnabled) &&
               hasValidLicense();
    }

    /**
     * Gets organization display name or name.
     *
     * @return Display name if available, otherwise name
     */
    public String getDisplayNameOrName() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : name;
    }

    /**
     * Gets remaining user slots.
     *
     * @return Number of available user slots, null if unlimited
     */
    public Integer getRemainingUserSlots() {
        if (maxUsers == null) {
            return null; // Unlimited
        }
        return Math.max(0, maxUsers - currentUsers);
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void prePersist() {
        if (status == null) {
            status = OrganizationStatus.PENDING;
        }
        if (country == null) {
            country = "TUR"; // Default to Turkey
        }
        if (currentUsers == null) {
            currentUsers = 0;
        }
    }
}