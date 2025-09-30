package com.bisttrading.broker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Resilience4j konfigürasyonu - AlgoLab API için circuit breaker, retry ve timeout ayarları
 */
@Configuration
public class Resilience4jConfig {

    /**
     * AlgoLab API için Circuit Breaker konfigürasyonu
     */
    @Bean
    public CircuitBreaker algoLabCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // %50 hata oranında circuit açılır
            .waitDurationInOpenState(Duration.ofSeconds(30)) // 30 saniye açık kalır
            .slidingWindowSize(10) // Son 10 request'i değerlendirir
            .minimumNumberOfCalls(5) // Minimum 5 call gerekli
            .slowCallRateThreshold(50) // %50 slow call oranında circuit açılır
            .slowCallDurationThreshold(Duration.ofSeconds(10)) // 10 saniyeden fazla olan çağrılar slow sayılır
            .permittedNumberOfCallsInHalfOpenState(3) // Half-open durumunda 3 test çağrısı yapar
            .recordExceptions(
                WebClientResponseException.class,
                TimeoutException.class,
                RuntimeException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class // Validation hataları ignore edilir
            )
            .build();

        return CircuitBreaker.of("algolab-api", config);
    }

    /**
     * AlgoLab API için Retry konfigürasyonu
     */
    @Bean
    public Retry algoLabRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Maksimum 3 deneme
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(2), // Başlangıç
                2.0 // Çarpan (2, 4, 8 saniye şeklinde artar)
            ))
            .retryOnException(throwable -> {
                // Hangi durumlar için retry yapılacak
                if (throwable instanceof WebClientResponseException ex) {
                    int status = ex.getStatusCode().value();
                    // 5xx server errors ve 429 rate limit için retry
                    return status >= 500 || status == 429;
                }
                // Timeout ve network hataları için retry
                return throwable instanceof TimeoutException ||
                       throwable.getCause() instanceof java.net.ConnectException;
            })
            .build();

        return Retry.of("algolab-api", config);
    }

    /**
     * AlgoLab API için Time Limiter konfigürasyonu
     */
    @Bean
    public TimeLimiter algoLabTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(30)) // 30 saniye timeout
            .cancelRunningFuture(true) // Timeout olursa running future'ları iptal et
            .build();

        return TimeLimiter.of("algolab-api", config);
    }
}