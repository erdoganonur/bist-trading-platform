package com.bisttrading.broker.algolab.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;

/**
 * Resilience4j configuration for AlgoLab API integration.
 * Configures circuit breaker, retry, and time limiter patterns.
 */
@Configuration
@Slf4j
public class AlgoLabCircuitBreakerConfig {

    /**
     * Circuit breaker configuration for AlgoLab API.
     *
     * Pattern: Circuit opens when 50% of calls fail or are slow.
     * After 60 seconds, circuit transitions to half-open state.
     */
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            // Failure threshold - circuit opens when 50% of calls fail
            .failureRateThreshold(50)

            // Slow call threshold - calls slower than 5 seconds are considered slow
            .slowCallDurationThreshold(Duration.ofSeconds(5))

            // Slow call rate threshold - circuit opens when 100% of calls are slow
            .slowCallRateThreshold(100)

            // Wait duration in open state before transitioning to half-open
            .waitDurationInOpenState(Duration.ofSeconds(60))

            // Number of calls in half-open state to determine if circuit should close
            .permittedNumberOfCallsInHalfOpenState(10)

            // Sliding window type - count-based
            .slidingWindowType(SlidingWindowType.COUNT_BASED)

            // Minimum number of calls before calculating error rate
            .minimumNumberOfCalls(5)

            // Sliding window size - last 100 calls
            .slidingWindowSize(100)

            // Record exceptions as failures
            .recordExceptions(
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
                java.util.concurrent.TimeoutException.class,
                java.io.IOException.class
            )

            // Ignore certain exceptions
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class
            )

            .build();
    }

    /**
     * Retry configuration for AlgoLab API.
     *
     * Pattern: Retry up to 3 times with exponential backoff.
     * Initial wait: 2 seconds, multiplier: 2x
     */
    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
            // Maximum number of retry attempts
            .maxAttempts(3)

            // Enable exponential backoff (2s, 4s, 8s)
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(Duration.ofSeconds(2), 2.0))

            // Retry on specific exceptions
            .retryExceptions(
                java.io.IOException.class,
                java.util.concurrent.TimeoutException.class,
                org.springframework.web.client.ResourceAccessException.class
            )

            // Don't retry on certain exceptions
            .ignoreExceptions(
                IllegalArgumentException.class,
                com.bisttrading.broker.algolab.exception.AlgoLabAuthenticationException.class
            )

            .build();
    }

    /**
     * Time limiter configuration for AlgoLab API.
     *
     * Pattern: Timeout after 10 seconds.
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
            // Timeout duration
            .timeoutDuration(Duration.ofSeconds(10))

            // Cancel running future on timeout
            .cancelRunningFuture(true)

            .build();
    }

    /**
     * Circuit breaker registry with event logging.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig config) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Register event consumer for logging
        registry.getEventPublisher()
            .onEntryAdded(event -> log.info("CircuitBreaker added: {}", event.getAddedEntry().getName()))
            .onEntryRemoved(event -> log.info("CircuitBreaker removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> log.info("CircuitBreaker replaced: {}", event.getNewEntry().getName()));

        return registry;
    }

    /**
     * Retry registry with event logging.
     */
    @Bean
    public RetryRegistry retryRegistry(RetryConfig config) {
        RetryRegistry registry = RetryRegistry.of(config);

        // Register event consumer for logging
        registry.getEventPublisher()
            .onEntryAdded(event -> log.info("Retry added: {}", event.getAddedEntry().getName()))
            .onEntryRemoved(event -> log.info("Retry removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> log.info("Retry replaced: {}", event.getNewEntry().getName()));

        return registry;
    }

    /**
     * Time limiter registry.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry(TimeLimiterConfig config) {
        return TimeLimiterRegistry.of(config);
    }

    /**
     * Custom registry event consumer for circuit breaker events.
     */
    @Bean
    public RegistryEventConsumer<io.github.resilience4j.circuitbreaker.CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<io.github.resilience4j.circuitbreaker.CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<io.github.resilience4j.circuitbreaker.CircuitBreaker> entryAddedEvent) {
                var circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("CircuitBreaker {} added", circuitBreaker.getName());

                // Register state transition events
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                        log.warn("CircuitBreaker {} state transition: {} -> {}",
                            circuitBreaker.getName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()
                        )
                    )
                    .onError(event ->
                        log.error("CircuitBreaker {} recorded error: {}",
                            circuitBreaker.getName(),
                            event.getThrowable().getMessage()
                        )
                    )
                    .onSuccess(event ->
                        log.debug("CircuitBreaker {} recorded success", circuitBreaker.getName())
                    )
                    .onCallNotPermitted(event ->
                        log.warn("CircuitBreaker {} rejected call", circuitBreaker.getName())
                    );
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<io.github.resilience4j.circuitbreaker.CircuitBreaker> entryRemoveEvent) {
                log.info("CircuitBreaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<io.github.resilience4j.circuitbreaker.CircuitBreaker> entryReplacedEvent) {
                log.info("CircuitBreaker {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    /**
     * Custom registry event consumer for retry events.
     */
    @Bean
    public RegistryEventConsumer<io.github.resilience4j.retry.Retry> retryEventConsumer() {
        return new RegistryEventConsumer<io.github.resilience4j.retry.Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<io.github.resilience4j.retry.Retry> entryAddedEvent) {
                var retry = entryAddedEvent.getAddedEntry();
                log.info("Retry {} added", retry.getName());

                // Register retry events
                retry.getEventPublisher()
                    .onRetry(event ->
                        log.warn("Retry {} attempt {} after error: {}",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()
                        )
                    )
                    .onSuccess(event ->
                        log.info("Retry {} succeeded after {} attempts",
                            retry.getName(),
                            event.getNumberOfRetryAttempts()
                        )
                    )
                    .onError(event ->
                        log.error("Retry {} failed after {} attempts: {}",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()
                        )
                    );
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<io.github.resilience4j.retry.Retry> entryRemoveEvent) {
                log.info("Retry {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<io.github.resilience4j.retry.Retry> entryReplacedEvent) {
                log.info("Retry {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}
