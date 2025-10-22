package com.bisttrading.telegram.model;

import com.bisttrading.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Telegram User entity.
 * Links Telegram users to platform users and stores Telegram-specific preferences.
 */
@Entity
@Table(name = "telegram_users", indexes = {
    @Index(name = "idx_telegram_user_id", columnList = "telegram_user_id"),
    @Index(name = "idx_platform_user_id", columnList = "platform_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Telegram user ID (from Telegram API)
     */
    @Column(name = "telegram_user_id", unique = true, nullable = false)
    private Long telegramUserId;

    /**
     * Link to platform user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_user_id")
    private UserEntity platformUser;

    /**
     * Telegram first name
     */
    @Column(name = "first_name", length = 255)
    private String firstName;

    /**
     * Telegram last name
     */
    @Column(name = "last_name", length = 255)
    private String lastName;

    /**
     * Telegram username (without @)
     */
    @Column(name = "username", length = 255)
    private String username;

    /**
     * Telegram language code (e.g., "tr", "en")
     */
    @Column(name = "language_code", length = 10)
    private String languageCode;

    /**
     * Telegram chat ID (usually same as user ID for private chats)
     */
    @Column(name = "chat_id")
    private Long chatId;

    /**
     * Enable/disable all notifications
     */
    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    /**
     * Enable/disable price alerts
     */
    @Column(name = "price_alerts_enabled", nullable = false)
    @Builder.Default
    private Boolean priceAlertsEnabled = true;

    /**
     * Enable/disable order notifications
     */
    @Column(name = "order_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean orderNotificationsEnabled = true;

    /**
     * Bot is active (user hasn't blocked the bot)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Last interaction with bot
     */
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Update last interaction timestamp
     */
    public void updateLastInteraction() {
        this.lastInteractionAt = LocalDateTime.now();
    }

    /**
     * Check if user is linked to a platform account
     */
    public boolean isLinked() {
        return platformUser != null;
    }
}
