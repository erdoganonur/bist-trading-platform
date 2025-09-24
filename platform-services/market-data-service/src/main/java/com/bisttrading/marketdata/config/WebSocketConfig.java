package com.bisttrading.marketdata.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebSocket ve Redis konfigürasyonu
 */
@Configuration
public class WebSocketConfig {

    /**
     * WebSocket operations için task scheduler
     */
    @Bean("webSocketTaskScheduler")
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("AlgoLabWS-");
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Redis template for market data publishing
     */
    @Bean
    public RedisTemplate<String, Object> marketDataRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // JSON serializer for values
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * WebSocket connection metrics
     */
    @Bean
    public WebSocketMetrics webSocketMetrics(MeterRegistry meterRegistry) {
        return new WebSocketMetrics(meterRegistry);
    }

    /**
     * WebSocket connection metrics collector
     */
    public static class WebSocketMetrics {
        private final MeterRegistry meterRegistry;

        public WebSocketMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void incrementConnections() {
            meterRegistry.counter("websocket.connections.total").increment();
        }

        public void decrementConnections() {
            meterRegistry.counter("websocket.disconnections.total").increment();
        }

        public void recordMessageReceived() {
            meterRegistry.counter("websocket.messages.received").increment();
        }

        public void recordMessageSent() {
            meterRegistry.counter("websocket.messages.sent").increment();
        }

        public void recordReconnection() {
            meterRegistry.counter("websocket.reconnections.total").increment();
        }

        public void recordError(String errorType) {
            meterRegistry.counter("websocket.errors", "type", errorType).increment();
        }
    }
}