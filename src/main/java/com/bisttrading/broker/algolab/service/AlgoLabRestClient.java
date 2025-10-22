package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.exception.AlgoLabApiException;
import com.bisttrading.broker.algolab.util.AlgoLabCheckerUtil;
import com.google.common.util.concurrent.RateLimiter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * REST client for AlgoLab API with rate limiting and resilience patterns.
 * Uses Circuit Breaker, Retry, and Time Limiter patterns.
 */
@Component
@Slf4j
public class AlgoLabRestClient {

    private final AlgoLabProperties properties;
    private final AlgoLabCheckerUtil checkerUtil;
    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final AlgoLabFallbackService fallbackService;
    private final MeterRegistry meterRegistry;

    private volatile String hash; // Authorization hash from LoginUserControl

    public AlgoLabRestClient(
        AlgoLabProperties properties,
        AlgoLabCheckerUtil checkerUtil,
        RestTemplate restTemplate,
        AlgoLabFallbackService fallbackService,
        MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.checkerUtil = checkerUtil;
        this.restTemplate = restTemplate;
        this.fallbackService = fallbackService;
        this.meterRegistry = meterRegistry;

        // Rate limiter: 0.2 permits/sec = 1 request per 5 seconds
        this.rateLimiter = RateLimiter.create(properties.getApi().getRateLimit());

        log.info("AlgoLab REST client initialized with rate limit: {} req/sec",
            properties.getApi().getRateLimit());
    }

    /**
     * Makes a POST request to AlgoLab API with resilience patterns.
     *
     * @param endpoint Endpoint path (e.g., "/api/GetEquityInfo")
     * @param payload Request payload
     * @param authenticated If true, adds Checker and Authorization headers
     * @param responseType Response class type
     * @return Response entity
     */
    @CircuitBreaker(name = "algolab", fallbackMethod = "fallbackPost")
    @Retry(name = "algolab")
    @TimeLimiter(name = "algolab")
    public <T> CompletableFuture<ResponseEntity<T>> postAsync(
        String endpoint,
        Object payload,
        boolean authenticated,
        Class<T> responseType
    ) {
        return CompletableFuture.supplyAsync(() -> post(endpoint, payload, authenticated, responseType));
    }

    /**
     * Synchronous POST request (used internally and by legacy code).
     * This method is called by postAsync and includes all the actual logic.
     */
    public <T> ResponseEntity<T> post(
        String endpoint,
        Object payload,
        boolean authenticated,
        Class<T> responseType
    ) {
        // Start timer for metrics
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Rate limiting - blocks until permit is available
            rateLimiter.acquire();

            // Build URL
            String url = properties.getApi().getUrl() + endpoint;

            // Build headers
            HttpHeaders headers = authenticated
                ? createAuthHeaders(endpoint, payload)
                : createLoginHeaders();

            // Build request
            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            log.debug("POST {} (authenticated: {})", endpoint, authenticated);
            log.debug("POST {} - Payload: {}", endpoint, payload);
            log.debug("POST {} - Headers: APIKEY={}, Checker={}, Auth={}",
                endpoint,
                headers.getFirst("APIKEY"),
                headers.getFirst("Checker"),
                headers.getFirst("Authorization") != null ? "***SET***" : "NOT_SET");

            ResponseEntity<T> response = restTemplate.postForEntity(url, request, responseType);
            log.debug("POST {} - Status: {}", endpoint, response.getStatusCode());

            // Cache successful response for fallback
            if (response.getStatusCode().is2xxSuccessful()) {
                fallbackService.cacheResponse(endpoint, response.getBody());
            }

            // Record successful request
            sample.stop(Timer.builder("algolab.api.request")
                .tag("endpoint", endpoint)
                .tag("status", "success")
                .register(meterRegistry));

            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("API request failed: {} - Status: {}, Body: {}",
                endpoint, e.getStatusCode(), e.getResponseBodyAsString());

            // Record failed request
            sample.stop(Timer.builder("algolab.api.request")
                .tag("endpoint", endpoint)
                .tag("status", "error")
                .tag("error_code", String.valueOf(e.getStatusCode().value()))
                .register(meterRegistry));

            throw new AlgoLabApiException(
                "API request failed: " + e.getMessage(),
                e.getStatusCode().value(),
                e
            );
        } catch (Exception e) {
            log.error("Unexpected error during API request: {}", endpoint, e);

            // Record failed request
            sample.stop(Timer.builder("algolab.api.request")
                .tag("endpoint", endpoint)
                .tag("status", "exception")
                .tag("exception", e.getClass().getSimpleName())
                .register(meterRegistry));

            throw new AlgoLabApiException("Unexpected error during API request", 500, e);
        }
    }

    /**
     * Fallback method for POST requests when circuit breaker is open or retries exhausted.
     */
    private <T> CompletableFuture<ResponseEntity<T>> fallbackPost(
        String endpoint,
        Object payload,
        boolean authenticated,
        Class<T> responseType,
        Throwable throwable
    ) {
        log.warn("Circuit breaker fallback triggered for POST {}", endpoint);

        // Record fallback metric
        meterRegistry.counter("algolab.api.fallback",
            "endpoint", endpoint,
            "reason", throwable.getClass().getSimpleName()
        ).increment();

        ResponseEntity<T> fallbackResponse = fallbackService.fallbackPost(
            endpoint,
            payload,
            authenticated,
            responseType,
            throwable
        );

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    /**
     * Creates headers for authenticated requests.
     */
    private HttpHeaders createAuthHeaders(String endpoint, Object payload) {
        if (hash == null || hash.isEmpty()) {
            throw new AlgoLabApiException("Not authenticated. Hash is null.", 401);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKEY", properties.getApi().getKey());
        headers.set("Checker", checkerUtil.makeChecker(endpoint, payload));
        headers.set("Authorization", hash);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Creates headers for login requests (no authentication required).
     */
    private HttpHeaders createLoginHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APIKEY", properties.getApi().getKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Sets the authorization hash (from LoginUserControl).
     */
    public void setHash(String hash) {
        this.hash = hash;
        log.debug("Authorization hash updated");
    }

    /**
     * Gets the current authorization hash.
     */
    public String getHash() {
        return hash;
    }

    /**
     * Clears authentication (hash).
     */
    public void clearAuth() {
        this.hash = null;
        log.debug("Authorization cleared");
    }

    /**
     * Checks if client is authenticated.
     */
    public boolean isAuthenticated() {
        return hash != null && !hash.isEmpty();
    }
}
