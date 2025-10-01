package com.bisttrading.user.entity.trading;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import com.bisttrading.user.entity.User;
import com.bisttrading.user.entity.Organization;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Entity representing broker account integration
 * Supports multiple brokers with encrypted credential storage
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Entity
@Table(name = "broker_accounts",
    indexes = {
        @Index(name = "idx_broker_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_broker_accounts_broker", columnList = "broker_name"),
        @Index(name = "idx_broker_accounts_active", columnList = "is_active, user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "broker_accounts_unique_account",
                         columnNames = {"user_id", "broker_name", "account_number"})
    }
)
@SQLDelete(sql = "UPDATE broker_accounts SET deleted_at = CURRENT_TIMESTAMP WHERE broker_account_id = ?")
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "encryptedApiKey", "encryptedApiSecret", "encryptedUsername", "encryptedPassword"})
public class BrokerAccount extends BaseEntity {

    /**
     * User who owns this broker account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @ToString.Exclude
    private User user;

    /**
     * Organization (if account belongs to organization)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @ToString.Exclude
    private Organization organization;

    /**
     * Broker name (AlgoLab, DenizBank, Garanti, etc.)
     */
    @Column(name = "broker_name", length = 100, nullable = false)
    @NotBlank(message = "Broker name is required")
    @Size(max = 100, message = "Broker name must not exceed 100 characters")
    private String brokerName;

    /**
     * Account number at the broker
     */
    @Column(name = "account_number", length = 50, nullable = false)
    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    /**
     * Sub-account number (if applicable)
     */
    @Column(name = "sub_account_number", length = 50)
    @Size(max = 50, message = "Sub-account number must not exceed 50 characters")
    private String subAccountNumber;

    /**
     * Encrypted API key (AES-256-GCM)
     */
    @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
    private String encryptedApiKey;

    /**
     * Encrypted API secret (AES-256-GCM)
     */
    @Column(name = "encrypted_api_secret", columnDefinition = "TEXT")
    private String encryptedApiSecret;

    /**
     * Encrypted username (AES-256-GCM)
     */
    @Column(name = "encrypted_username", columnDefinition = "TEXT")
    private String encryptedUsername;

    /**
     * Encrypted password (AES-256-GCM)
     */
    @Column(name = "encrypted_password", columnDefinition = "TEXT")
    private String encryptedPassword;

    /**
     * Account active status
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Account verification status
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Trading enabled flag
     */
    @Column(name = "trading_enabled", nullable = false)
    @Builder.Default
    private Boolean tradingEnabled = false;

    /**
     * Daily trading limit
     */
    @Column(name = "daily_trade_limit", precision = 20, scale = 2)
    @Positive(message = "Daily trade limit must be positive")
    @Digits(integer = 18, fraction = 2, message = "Invalid daily trade limit format")
    private BigDecimal dailyTradeLimit;

    /**
     * Maximum position value limit
     */
    @Column(name = "position_limit", precision = 20, scale = 2)
    @Positive(message = "Position limit must be positive")
    @Digits(integer = 18, fraction = 2, message = "Invalid position limit format")
    private BigDecimal positionLimit;

    /**
     * Maximum single order value
     */
    @Column(name = "max_order_value", precision = 20, scale = 2)
    @Positive(message = "Max order value must be positive")
    @Digits(integer = 18, fraction = 2, message = "Invalid max order value format")
    private BigDecimal maxOrderValue;

    /**
     * Available cash balance (cached from broker)
     */
    @Column(name = "cash_balance", precision = 20, scale = 2)
    @PositiveOrZero(message = "Cash balance must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid cash balance format")
    private BigDecimal cashBalance;

    /**
     * Available buying power
     */
    @Column(name = "buying_power", precision = 20, scale = 2)
    @PositiveOrZero(message = "Buying power must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid buying power format")
    private BigDecimal buyingPower;

    /**
     * Total account equity
     */
    @Column(name = "total_equity", precision = 20, scale = 2)
    @PositiveOrZero(message = "Total equity must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid total equity format")
    private BigDecimal totalEquity;

    /**
     * Margin used
     */
    @Column(name = "margin_used", precision = 20, scale = 2)
    @PositiveOrZero(message = "Margin used must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid margin used format")
    private BigDecimal marginUsed;

    /**
     * Available margin
     */
    @Column(name = "margin_available", precision = 20, scale = 2)
    @PositiveOrZero(message = "Available margin must be non-negative")
    @Digits(integer = 18, fraction = 2, message = "Invalid available margin format")
    private BigDecimal marginAvailable;

    /**
     * Last successful sync with broker
     */
    @Column(name = "last_sync_at")
    private ZonedDateTime lastSyncAt;

    /**
     * Current connection status
     */
    @Column(name = "connection_status", length = 20)
    @Size(max = 20, message = "Connection status must not exceed 20 characters")
    @Builder.Default
    private String connectionStatus = "DISCONNECTED";

    /**
     * Last error message (if any)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Broker-specific metadata
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "broker_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode brokerMetadata = null;

    // Business methods

    /**
     * Check if account is ready for trading
     */
    public boolean isReadyForTrading() {
        return isActive && isVerified && tradingEnabled &&
               "CONNECTED".equals(connectionStatus) && hasValidCredentials();
    }

    /**
     * Check if account has valid credentials
     */
    public boolean hasValidCredentials() {
        return (encryptedApiKey != null && encryptedApiSecret != null) ||
               (encryptedUsername != null && encryptedPassword != null);
    }

    /**
     * Check if account supports API trading
     */
    public boolean supportsApiTrading() {
        return encryptedApiKey != null && encryptedApiSecret != null;
    }

    /**
     * Check if order value is within limits
     */
    public boolean isOrderValueAllowed(BigDecimal orderValue) {
        if (orderValue == null) return false;
        return maxOrderValue == null || orderValue.compareTo(maxOrderValue) <= 0;
    }

    /**
     * Check if daily limit allows this trade
     */
    public boolean isDailyLimitAllowed(BigDecimal tradeValue, BigDecimal todaysVolume) {
        if (dailyTradeLimit == null) return true;
        if (tradeValue == null) return false;

        BigDecimal currentVolume = todaysVolume != null ? todaysVolume : BigDecimal.ZERO;
        return currentVolume.add(tradeValue).compareTo(dailyTradeLimit) <= 0;
    }

    /**
     * Check if position limit allows this position
     */
    public boolean isPositionLimitAllowed(BigDecimal newPositionValue, BigDecimal currentPositionValue) {
        if (positionLimit == null) return true;
        if (newPositionValue == null) return false;

        BigDecimal currentValue = currentPositionValue != null ? currentPositionValue : BigDecimal.ZERO;
        return currentValue.add(newPositionValue).compareTo(positionLimit) <= 0;
    }

    /**
     * Check if sufficient buying power is available
     */
    public boolean hasSufficientBuyingPower(BigDecimal requiredAmount) {
        if (buyingPower == null || requiredAmount == null) return false;
        return buyingPower.compareTo(requiredAmount) >= 0;
    }

    /**
     * Update connection status
     */
    public void updateConnectionStatus(String status, String errorMsg) {
        this.connectionStatus = status;
        this.errorMessage = errorMsg;

        if ("CONNECTED".equals(status)) {
            this.lastSyncAt = ZonedDateTime.now();
        }
    }

    /**
     * Update account balances from broker sync
     */
    public void updateBalances(BigDecimal cash, BigDecimal buyingPwr, BigDecimal totalEq,
                              BigDecimal marginUsed, BigDecimal marginAvail) {
        this.cashBalance = cash;
        this.buyingPower = buyingPwr;
        this.totalEquity = totalEq;
        this.marginUsed = marginUsed;
        this.marginAvailable = marginAvail;
        this.lastSyncAt = ZonedDateTime.now();
        this.connectionStatus = "CONNECTED";
        this.errorMessage = null;
    }

    /**
     * Get margin utilization percentage
     */
    public BigDecimal getMarginUtilization() {
        if (marginUsed == null || totalEquity == null || totalEquity.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return marginUsed.divide(totalEquity, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get available margin as percentage of total equity
     */
    public BigDecimal getAvailableMarginPercentage() {
        if (marginAvailable == null || totalEquity == null || totalEquity.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return marginAvailable.divide(totalEquity, 4, java.math.RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if account needs balance refresh
     */
    public boolean needsBalanceRefresh(int maxAgeMinutes) {
        if (lastSyncAt == null) return true;
        return ZonedDateTime.now().isAfter(lastSyncAt.plusMinutes(maxAgeMinutes));
    }

    /**
     * Get display name for the account
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        sb.append(brokerName);
        if (accountNumber != null) {
            sb.append(" - ").append(accountNumber);
        }
        if (subAccountNumber != null) {
            sb.append(" (").append(subAccountNumber).append(")");
        }
        return sb.toString();
    }

    /**
     * Check if account is stale (not synced recently)
     */
    public boolean isStale(int maxStaleHours) {
        if (lastSyncAt == null) return true;
        return ZonedDateTime.now().isAfter(lastSyncAt.plusHours(maxStaleHours));
    }

    /**
     * Get risk score based on utilization and limits
     */
    public int getRiskScore() {
        int score = 0;

        // Margin utilization risk (0-40 points)
        BigDecimal marginUtil = getMarginUtilization();
        if (marginUtil.compareTo(BigDecimal.valueOf(80)) > 0) {
            score += 40;
        } else if (marginUtil.compareTo(BigDecimal.valueOf(60)) > 0) {
            score += 30;
        } else if (marginUtil.compareTo(BigDecimal.valueOf(40)) > 0) {
            score += 20;
        } else if (marginUtil.compareTo(BigDecimal.valueOf(20)) > 0) {
            score += 10;
        }

        // Connection stability (0-30 points)
        if (!"CONNECTED".equals(connectionStatus)) {
            score += 30;
        } else if (isStale(24)) {
            score += 20;
        } else if (isStale(6)) {
            score += 10;
        }

        // Account status (0-30 points)
        if (!isActive) score += 30;
        else if (!isVerified) score += 20;
        else if (!tradingEnabled) score += 15;
        else if (errorMessage != null) score += 10;

        return Math.min(score, 100); // Cap at 100
    }

    // Encryption helper methods (to be used by service layer)

    /**
     * Set API credentials (will be encrypted by service layer)
     */
    public void setApiCredentials(String apiKey, String apiSecret) {
        // Note: Actual encryption should be handled by the service layer
        // This is just for the entity structure
    }

    /**
     * Set login credentials (will be encrypted by service layer)
     */
    public void setLoginCredentials(String username, String password) {
        // Note: Actual encryption should be handled by the service layer
        // This is just for the entity structure
    }

    // Validation methods

    @PrePersist
    @PreUpdate
    private void validate() {
        // Ensure we have some form of credentials
        if (!hasValidCredentials()) {
            throw new IllegalArgumentException("Account must have either API credentials or login credentials");
        }

        // Validate connection status
        if (connectionStatus != null &&
            !connectionStatus.matches("CONNECTED|DISCONNECTED|ERROR|CONNECTING|AUTHENTICATING")) {
            throw new IllegalArgumentException("Invalid connection status: " + connectionStatus);
        }

        // Validate margin consistency
        if (marginUsed != null && marginAvailable != null && totalEquity != null) {
            BigDecimal totalMargin = marginUsed.add(marginAvailable);
            // Allow for small rounding differences
            if (totalMargin.subtract(totalEquity).abs().compareTo(new BigDecimal("0.01")) > 0) {
                // This is a warning, not an error, as brokers may calculate differently
                // throw new IllegalArgumentException("Margin used + available should equal total equity");
            }
        }

        // Set default values
        if (isActive == null) isActive = true;
        if (isVerified == null) isVerified = false;
        if (tradingEnabled == null) tradingEnabled = false;
        if (connectionStatus == null) connectionStatus = "DISCONNECTED";
    }
}