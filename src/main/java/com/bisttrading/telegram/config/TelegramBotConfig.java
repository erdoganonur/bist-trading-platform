package com.bisttrading.telegram.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Telegram Bot Spring configuration.
 * Provides beans for Telegram bot infrastructure.
 * Uses TelegramBots 7.x API (Spring Boot 3 & Jakarta EE 9+ compatible).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final TelegramBotProperties properties;

    /**
     * TelegramClient bean for sending messages.
     * Uses OkHttp implementation (recommended for production).
     */
    @Bean
    public TelegramClient telegramClient() {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("⚠️  Telegram Bot token not configured. TelegramClient will not be created.");
            return null;
        }

        TelegramClient client = new OkHttpTelegramClient(properties.getToken());
        log.info("✅ TelegramClient created (OkHttp implementation)");
        return client;
    }

    /**
     * RedisTemplate for Telegram session storage.
     * Uses separate serialization strategy for Telegram-specific objects.
     */
    @Bean(name = "telegramRedisTemplate")
    public RedisTemplate<String, Object> telegramRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Create ObjectMapper with JavaTimeModule for LocalDateTime support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Use JSON serialization for values
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        log.info("✅ Telegram RedisTemplate configured");
        return template;
    }
}
