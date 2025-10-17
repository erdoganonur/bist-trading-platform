package com.bisttrading.broker.algolab.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for AlgoLab API integration.
 * Configures different cache regions with custom TTL settings.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig {

    /**
     * Cache names used throughout the application.
     */
    public static class CacheNames {
        /** Symbol information cache - 1 hour TTL (rarely changes) */
        public static final String SYMBOLS = "algolab:symbols";

        /** Candle/OHLC data cache - 5 minutes TTL (updates every bar) */
        public static final String CANDLES = "algolab:candles";

        /** User positions cache - 1 minute TTL (frequently updated) */
        public static final String POSITIONS = "algolab:positions";

        /** Order status cache - 30 seconds TTL (very frequently updated) */
        public static final String ORDERS = "algolab:orders";

        /** Real-time market data cache - 30 seconds TTL (tick data) */
        public static final String MARKET_DATA = "algolab:market-data";

        /** Account information cache - 5 minutes TTL (balance, buying power) */
        public static final String ACCOUNT_INFO = "algolab:account-info";
    }

    /**
     * TTL configurations for each cache type.
     */
    public static class CacheTTL {
        public static final Duration SYMBOLS = Duration.ofHours(1);
        public static final Duration CANDLES = Duration.ofMinutes(5);
        public static final Duration POSITIONS = Duration.ofMinutes(1);
        public static final Duration ORDERS = Duration.ofSeconds(30);
        public static final Duration MARKET_DATA = Duration.ofSeconds(30);
        public static final Duration ACCOUNT_INFO = Duration.ofMinutes(5);
        public static final Duration DEFAULT = Duration.ofMinutes(10);
    }

    /**
     * Default ObjectMapper for general application use (HTTP endpoints, utilities, etc.).
     * Does NOT use polymorphic typing - suitable for REST APIs.
     *
     * This is the PRIMARY ObjectMapper bean that will be autowired by default.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTime module for LocalDateTime, Instant support
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // NO polymorphic typing for HTTP endpoints
        log.info("Configured default ObjectMapper with JavaTimeModule (for HTTP and general use)");
        return mapper;
    }

    /**
     * Redis-specific ObjectMapper configured ONLY for Redis cache serialization.
     * Supports JavaTime types (LocalDateTime, Instant, etc.)
     *
     * WARNING: This ObjectMapper uses polymorphic typing (type information in JSON).
     * It should ONLY be used for Redis caching, NOT for HTTP endpoints.
     *
     * @Bean name is intentionally specific and requires @Qualifier for injection
     */
    @Bean(name = "algoLabRedisCacheObjectMapper")
    public ObjectMapper redisCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTime module for LocalDateTime, Instant support
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable default typing for polymorphic types (REDIS ONLY!)
        // This adds type information to JSON for deserialization safety
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        log.info("Configured Redis cache ObjectMapper with JavaTimeModule and polymorphic typing");
        return mapper;
    }

    /**
     * Configures Redis cache manager with custom TTL per cache.
     *
     * Features:
     * - String keys with "bist:algolab:" prefix
     * - JSON serialization for values
     * - Custom TTL per cache region
     * - Null values not cached
     * - Cache statistics enabled
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("algoLabRedisCacheObjectMapper") ObjectMapper redisCacheObjectMapper) {

        log.info("Configuring Redis cache manager for AlgoLab integration");

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            // Key prefix for all caches
            .prefixCacheNameWith("bist:algolab:")

            // Key serializer - String
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )

            // Value serializer - JSON with JavaTime support
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(redisCacheObjectMapper)
                )
            )

            // Default TTL
            .entryTtl(CacheTTL.DEFAULT)

            // Don't cache null values
            .disableCachingNullValues()

            // Enable cache statistics
            .enableTimeToIdle();

        // Configure specific caches with custom TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Symbols cache - 1 hour (data rarely changes)
        cacheConfigurations.put(
            CacheNames.SYMBOLS,
            defaultConfig.entryTtl(CacheTTL.SYMBOLS)
        );

        // Candles cache - 5 minutes (updates every bar completion)
        cacheConfigurations.put(
            CacheNames.CANDLES,
            defaultConfig.entryTtl(CacheTTL.CANDLES)
        );

        // Positions cache - 1 minute (frequently updated)
        cacheConfigurations.put(
            CacheNames.POSITIONS,
            defaultConfig.entryTtl(CacheTTL.POSITIONS)
        );

        // Orders cache - 30 seconds (very frequently updated)
        cacheConfigurations.put(
            CacheNames.ORDERS,
            defaultConfig.entryTtl(CacheTTL.ORDERS)
        );

        // Market data cache - 30 seconds (real-time tick data)
        cacheConfigurations.put(
            CacheNames.MARKET_DATA,
            defaultConfig.entryTtl(CacheTTL.MARKET_DATA)
        );

        // Account info cache - 5 minutes (balance, buying power)
        cacheConfigurations.put(
            CacheNames.ACCOUNT_INFO,
            defaultConfig.entryTtl(CacheTTL.ACCOUNT_INFO)
        );

        // Build cache manager
        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()  // Sync cache with transactions
            .build();

        log.info("Redis cache manager configured with {} cache regions", cacheConfigurations.size());
        log.info("Cache TTL configuration:");
        log.info("  - Symbols: {} (1 hour)", CacheTTL.SYMBOLS);
        log.info("  - Candles: {} (5 minutes)", CacheTTL.CANDLES);
        log.info("  - Positions: {} (1 minute)", CacheTTL.POSITIONS);
        log.info("  - Orders: {} (30 seconds)", CacheTTL.ORDERS);
        log.info("  - Market Data: {} (30 seconds)", CacheTTL.MARKET_DATA);
        log.info("  - Account Info: {} (5 minutes)", CacheTTL.ACCOUNT_INFO);

        return cacheManager;
    }
}
