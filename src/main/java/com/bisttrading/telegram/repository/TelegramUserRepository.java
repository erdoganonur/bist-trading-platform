package com.bisttrading.telegram.repository;

import com.bisttrading.telegram.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Telegram User entity.
 */
@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    /**
     * Find Telegram user by Telegram user ID
     */
    Optional<TelegramUser> findByTelegramUserId(Long telegramUserId);

    /**
     * Find Telegram user by platform user ID
     */
    @Query("SELECT tu FROM TelegramUser tu WHERE tu.platformUser.id = :platformUserId")
    Optional<TelegramUser> findByPlatformUserId(@Param("platformUserId") String platformUserId);

    /**
     * Find all Telegram users linked to a platform user
     */
    @Query("SELECT tu FROM TelegramUser tu WHERE tu.platformUser.id = :platformUserId")
    List<TelegramUser> findAllByPlatformUserId(@Param("platformUserId") String platformUserId);

    /**
     * Find all active Telegram users
     */
    List<TelegramUser> findAllByIsActiveTrue();

    /**
     * Find all users with notifications enabled
     */
    @Query("SELECT tu FROM TelegramUser tu WHERE tu.notificationsEnabled = true AND tu.isActive = true")
    List<TelegramUser> findAllWithNotificationsEnabled();

    /**
     * Find all users with price alerts enabled
     */
    @Query("SELECT tu FROM TelegramUser tu WHERE tu.priceAlertsEnabled = true AND tu.isActive = true")
    List<TelegramUser> findAllWithPriceAlertsEnabled();

    /**
     * Find inactive users (haven't interacted in X days)
     */
    @Query("SELECT tu FROM TelegramUser tu WHERE tu.lastInteractionAt < :since")
    List<TelegramUser> findInactiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Check if Telegram user exists
     */
    boolean existsByTelegramUserId(Long telegramUserId);

    /**
     * Check if platform user is already linked to a Telegram account
     */
    @Query("SELECT CASE WHEN COUNT(tu) > 0 THEN true ELSE false END FROM TelegramUser tu WHERE tu.platformUser.id = :platformUserId")
    boolean existsByPlatformUserId(@Param("platformUserId") String platformUserId);
}
