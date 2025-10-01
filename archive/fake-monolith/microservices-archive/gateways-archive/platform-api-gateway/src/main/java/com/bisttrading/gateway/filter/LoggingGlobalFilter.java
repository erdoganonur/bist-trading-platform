package com.bisttrading.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Global Filter for Request/Response Logging and Monitoring.
 *
 * This filter provides comprehensive logging and metrics collection
 * for all requests passing through the API Gateway.
 *
 * Features:
 * - Request/response logging with correlation IDs
 * - Performance metrics collection
 * - Request counting by endpoint and method
 * - Response time measurement
 * - Error rate tracking
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_TIME_ATTRIBUTE = "gateway.request.time";
    private static final String CORRELATION_ID_ATTRIBUTE = "gateway.correlation.id";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate correlation ID for request tracing
        String correlationId = generateCorrelationId();
        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);

        // Record request start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(REQUEST_TIME_ATTRIBUTE, startTime);

        // Log incoming request
        logRequest(request, correlationId);

        // Count incoming requests
        Counter.builder("gateway.requests.total")
            .description("Total number of requests")
            .tag("method", request.getMethod().name())
            .tag("uri", getSimplifiedPath(request.getPath().value()))
            .register(meterRegistry)
            .increment();

        // Continue with the filter chain and log response
        return chain.filter(exchange)
            .doFinally(signalType -> {
                logResponse(exchange, correlationId, startTime);
                recordMetrics(exchange, startTime);
            });
    }

    /**
     * Log incoming request details.
     */
    private void logRequest(ServerHttpRequest request, String correlationId) {
        if (log.isDebugEnabled()) {
            log.debug("Gateway Request [{}] - {} {} from {} at {}",
                correlationId,
                request.getMethod(),
                request.getPath(),
                getClientIp(request),
                LocalDateTime.now().format(TIMESTAMP_FORMAT)
            );

            // Log headers if trace level enabled
            if (log.isTraceEnabled()) {
                request.getHeaders().forEach((name, values) ->
                    log.trace("Gateway Request Header [{}] - {}: {}", correlationId, name, values)
                );
            }
        }
    }

    /**
     * Log outgoing response details.
     */
    private void logResponse(ServerWebExchange exchange, String correlationId, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        long duration = System.currentTimeMillis() - startTime;

        if (log.isDebugEnabled()) {
            log.debug("Gateway Response [{}] - Status: {}, Duration: {}ms at {}",
                correlationId,
                response.getStatusCode(),
                duration,
                LocalDateTime.now().format(TIMESTAMP_FORMAT)
            );
        }
    }

    /**
     * Record performance and business metrics.
     */
    private void recordMetrics(ServerWebExchange exchange, long startTime) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        long duration = System.currentTimeMillis() - startTime;

        // Record response time
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("gateway.request.duration")
            .description("Request processing time")
            .tag("method", request.getMethod().name())
            .tag("uri", getSimplifiedPath(request.getPath().value()))
            .tag("status", String.valueOf(response.getStatusCode().value()))
            .register(meterRegistry));

        // Count responses by status code
        Counter.builder("gateway.responses.total")
            .description("Total number of responses")
            .tag("method", request.getMethod().name())
            .tag("uri", getSimplifiedPath(request.getPath().value()))
            .tag("status", String.valueOf(response.getStatusCode().value()))
            .tag("status_class", getStatusClass(response.getStatusCode().value()))
            .register(meterRegistry)
            .increment();

        // Log slow requests
        if (duration > 5000) { // 5 seconds threshold
            log.warn("Slow request detected - {} {} took {}ms",
                request.getMethod(), request.getPath(), duration);
        }
    }

    /**
     * Generate unique correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extract client IP address from request.
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    /**
     * Simplify path for metrics to avoid high cardinality.
     */
    private String getSimplifiedPath(String path) {
        if (path == null) return "unknown";

        // Replace IDs and UUIDs with placeholders
        path = path.replaceAll("/\\d+", "/{id}");
        path = path.replaceAll("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "/{uuid}");

        // Limit path segments to avoid explosion
        String[] segments = path.split("/");
        if (segments.length > 5) {
            return String.join("/", java.util.Arrays.copyOf(segments, 5)) + "/...";
        }

        return path;
    }

    /**
     * Get HTTP status class for metrics grouping.
     */
    private String getStatusClass(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500) return "5xx";
        return "1xx";
    }

    @Override
    public int getOrder() {
        return -1000; // High priority to log all requests
    }
}