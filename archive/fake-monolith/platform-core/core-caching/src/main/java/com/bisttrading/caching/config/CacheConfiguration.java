package com.bisttrading.caching.config;

import com.bisttrading.caching.strategy.CacheInvalidationStrategy;
import com.bisttrading.caching.strategy.impl.RedisInvalidationStrategy;
import com.bisttrading.caching.warming.CacheWarmingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Cache configuration for BIST Trading Platform
 * Configures Redis caching with multi-level cache hierarchy and warming
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
public class CacheConfiguration {

    /**
     * Configure Redis template with optimized serialization
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer with Jackson
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure cache manager with different TTL policies
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Market data caches - short TTL for real-time data
        cacheConfigurations.put("market-data-cache", defaultConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigurations.put("order-book-cache", defaultConfig.entryTtl(Duration.ofSeconds(10)));

        // OHLCV data caches - variable TTL based on timeframe
        cacheConfigurations.put("ohlcv-1m-cache", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("ohlcv-5m-cache", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("ohlcv-1h-cache", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("ohlcv-1d-cache", defaultConfig.entryTtl(Duration.ofHours(24)));

        // Symbol and reference data - longer TTL
        cacheConfigurations.put("symbol-cache", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("reference-data-cache", defaultConfig.entryTtl(Duration.ofHours(6)));

        // User and session data
        cacheConfigurations.put("user-profile-cache", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("session-cache", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Trading parameters
        cacheConfigurations.put("trading-params-cache", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("market-status-cache", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Analytics and calculations
        cacheConfigurations.put("analytics-cache", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Rate limiting
        cacheConfigurations.put("rate-limit-cache", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Async executor for cache operations
     */
    @Bean(name = "cacheAsyncExecutor")
    public Executor cacheAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CacheAsync-");
        executor.setRejectedExecutionHandler(new ThreadPoolTaskExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Scheduled executor for cache maintenance tasks
     */
    @Bean(name = "cacheScheduledExecutor")
    public ScheduledExecutorService cacheScheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r, "CacheScheduled-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        return executor;
    }

    /**
     * Cache invalidation strategy bean
     */
    @Bean
    public CacheInvalidationStrategy cacheInvalidationStrategy(
            RedisTemplate<String, Object> redisTemplate,
            Executor cacheAsyncExecutor,
            ScheduledExecutorService cacheScheduledExecutor) {
        return new RedisInvalidationStrategy(redisTemplate, cacheAsyncExecutor, cacheScheduledExecutor);
    }

    /**
     * Cache warming on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup(ApplicationReadyEvent event) {
        CacheWarmingService cacheWarmingService = event.getApplicationContext().getBean(CacheWarmingService.class);

        // Perform cache warming asynchronously to not block application startup
        cacheWarmingService.warmUpOnStartup().whenComplete((result, throwable) -> {
            if (throwable != null) {
                // Log error but don't fail application startup
                org.slf4j.LoggerFactory.getLogger(CacheConfiguration.class)
                    .warn("Cache warm-up failed during startup", throwable);
            }
        });
    }

    /**
     * Cache key generator for consistent key generation across the application
     */
    @Bean
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CacheKeyGenerator();
    }

    /**
     * Custom cache key generator for trading-specific cache keys
     */
    public static class CacheKeyGenerator {

        private static final String DELIMITER = ":";

        public String generateMarketDataKey(String symbol) {
            return "market" + DELIMITER + "data" + DELIMITER + symbol + DELIMITER + "latest";
        }

        public String generateOrderBookKey(String symbol) {
            return "market" + DELIMITER + "orderbook" + DELIMITER + symbol + DELIMITER + "latest";
        }

        public String generateOhlcvKey(String symbol, String timeframe) {
            return "market" + DELIMITER + "ohlcv" + DELIMITER + symbol + DELIMITER + timeframe;
        }

        public String generateSymbolKey(String symbol) {
            return "market" + DELIMITER + "symbols" + DELIMITER + symbol;
        }

        public String generateUserProfileKey(String userId) {
            return "user" + DELIMITER + userId + DELIMITER + "profile";
        }

        public String generateUserSessionKey(String userId) {
            return "user" + DELIMITER + "sessions" + DELIMITER + userId;
        }

        public String generateTradingParamKey(String paramType) {
            return "trading" + DELIMITER + paramType + DELIMITER + "current";
        }

        public String generateAnalyticsKey(String symbol, String analysisType) {
            return "analytics" + DELIMITER + symbol + DELIMITER + analysisType;
        }

        public String generateRateLimitKey(String identifier, String action) {
            return "ratelimit" + DELIMITER + identifier + DELIMITER + action;
        }

        public String generateTagKey(String tag) {
            return "tag" + DELIMITER + tag;
        }

        // Pattern generators for bulk operations
        public String generateMarketDataPattern(String symbol) {
            return "market" + DELIMITER + "data" + DELIMITER + symbol + DELIMITER + "*";
        }

        public String generateUserPattern(String userId) {
            return "user" + DELIMITER + userId + DELIMITER + "*";
        }

        public String generateSymbolPattern(String symbol) {
            return "*" + DELIMITER + symbol + DELIMITER + "*";
        }
    }
}