package com.bisttrading.telegram.service;

import com.bisttrading.telegram.config.TelegramBotProperties;
import com.bisttrading.telegram.dto.ConversationState;
import com.bisttrading.telegram.dto.TelegramUserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Telegram user sessions in Redis.
 * Handles session storage, conversation state, and temporary data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramSessionService {

    private final RedisTemplate<String, Object> telegramRedisTemplate;
    private final TelegramBotProperties properties;

    private static final String SESSION_PREFIX = "telegram:session:";
    private static final String CONVERSATION_PREFIX = "telegram:conversation:";
    private static final String TEMP_DATA_PREFIX = "telegram:temp:";

    /**
     * Save user session to Redis
     */
    public void saveSession(TelegramUserSession session) {
        String key = SESSION_PREFIX + session.getTelegramUserId();
        session.updateLastActivity();

        telegramRedisTemplate.opsForValue().set(
            key,
            session,
            properties.getSession().getTimeoutSeconds(),
            TimeUnit.SECONDS
        );

        log.debug("Session saved for Telegram user: {}", session.getTelegramUserId());
    }

    /**
     * Get user session from Redis
     */
    public Optional<TelegramUserSession> getSession(Long telegramUserId) {
        String key = SESSION_PREFIX + telegramUserId;
        TelegramUserSession session = (TelegramUserSession) telegramRedisTemplate.opsForValue().get(key);

        if (session != null) {
            session.updateLastActivity();
            // Update session in Redis to refresh TTL
            saveSession(session);
        }

        return Optional.ofNullable(session);
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn(Long telegramUserId) {
        return getSession(telegramUserId)
            .map(TelegramUserSession::isLoggedIn)
            .orElse(false);
    }

    /**
     * Logout user (delete session)
     */
    public void logout(Long telegramUserId) {
        String key = SESSION_PREFIX + telegramUserId;
        telegramRedisTemplate.delete(key);

        // Clear conversation state and temp data
        clearConversationState(telegramUserId);
        clearAllTempData(telegramUserId);

        log.info("Session deleted for Telegram user: {}", telegramUserId);
    }

    /**
     * Update JWT tokens in session
     */
    public void updateTokens(Long telegramUserId, String jwtToken, String refreshToken) {
        getSession(telegramUserId).ifPresent(session -> {
            session.setJwtToken(jwtToken);
            session.setRefreshToken(refreshToken);
            saveSession(session);
            log.debug("Tokens updated for user: {}", telegramUserId);
        });
    }

    // =========================================================================
    // Conversation State Management
    // =========================================================================

    /**
     * Set conversation state for multi-step commands
     */
    public void setConversationState(Long telegramUserId, ConversationState state) {
        String key = CONVERSATION_PREFIX + telegramUserId + ":state";
        telegramRedisTemplate.opsForValue().set(key, state, 10, TimeUnit.MINUTES);
        log.debug("Conversation state set for user {}: {}", telegramUserId, state);
    }

    /**
     * Get current conversation state
     */
    public ConversationState getConversationState(Long telegramUserId) {
        String key = CONVERSATION_PREFIX + telegramUserId + ":state";
        ConversationState state = (ConversationState) telegramRedisTemplate.opsForValue().get(key);
        return state != null ? state : ConversationState.NONE;
    }

    /**
     * Clear conversation state
     */
    public void clearConversationState(Long telegramUserId) {
        String key = CONVERSATION_PREFIX + telegramUserId + ":state";
        telegramRedisTemplate.delete(key);
        log.debug("Conversation state cleared for user: {}", telegramUserId);
    }

    // =========================================================================
    // Temporary Data Storage (for conversation flows)
    // =========================================================================

    /**
     * Store temporary data during conversation
     */
    public void setTempData(Long telegramUserId, String dataKey, Object value) {
        String key = TEMP_DATA_PREFIX + telegramUserId + ":" + dataKey;
        telegramRedisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);
        log.debug("Temp data stored for user {}: {} = {}", telegramUserId, dataKey, value);
    }

    /**
     * Get temporary data
     */
    @SuppressWarnings("unchecked")
    public <T> T getTempData(Long telegramUserId, String dataKey, Class<T> type) {
        String key = TEMP_DATA_PREFIX + telegramUserId + ":" + dataKey;
        Object value = telegramRedisTemplate.opsForValue().get(key);
        return type.cast(value);
    }

    /**
     * Get temporary data as String
     */
    public String getTempData(Long telegramUserId, String dataKey) {
        return getTempData(telegramUserId, dataKey, String.class);
    }

    /**
     * Get all temporary data for user
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAllTempData(Long telegramUserId) {
        String pattern = TEMP_DATA_PREFIX + telegramUserId + ":*";
        var keys = telegramRedisTemplate.keys(pattern);

        Map<String, Object> data = new HashMap<>();
        if (keys != null) {
            for (String key : keys) {
                String dataKey = key.substring(key.lastIndexOf(":") + 1);
                Object value = telegramRedisTemplate.opsForValue().get(key);
                data.put(dataKey, value);
            }
        }

        return data;
    }

    /**
     * Clear specific temporary data
     */
    public void clearTempData(Long telegramUserId, String dataKey) {
        String key = TEMP_DATA_PREFIX + telegramUserId + ":" + dataKey;
        telegramRedisTemplate.delete(key);
    }

    /**
     * Clear all temporary data for user
     */
    public void clearAllTempData(Long telegramUserId) {
        String pattern = TEMP_DATA_PREFIX + telegramUserId + ":*";
        var keys = telegramRedisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            telegramRedisTemplate.delete(keys);
            log.debug("All temp data cleared for user: {}", telegramUserId);
        }
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Extend session TTL (useful for active users)
     */
    public void extendSession(Long telegramUserId) {
        getSession(telegramUserId).ifPresent(this::saveSession);
    }

    /**
     * Check if user has an active conversation
     */
    public boolean hasActiveConversation(Long telegramUserId) {
        return getConversationState(telegramUserId) != ConversationState.NONE;
    }

    /**
     * Get JWT token from session
     */
    public Optional<String> getJwtToken(Long telegramUserId) {
        return getSession(telegramUserId).map(TelegramUserSession::getJwtToken);
    }

    /**
     * Get platform user ID from session
     */
    public Optional<String> getPlatformUserId(Long telegramUserId) {
        return getSession(telegramUserId).map(TelegramUserSession::getPlatformUserId);
    }
}
