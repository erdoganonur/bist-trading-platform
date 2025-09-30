package com.bisttrading.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for Spring Cloud Gateway.
 *
 * Handles all exceptions that occur during request processing
 * and provides consistent error responses to clients.
 *
 * Features:
 * - Standardized error response format
 * - Proper HTTP status code mapping
 * - Request correlation ID inclusion
 * - Security-aware error messages (no sensitive information leak)
 * - Comprehensive logging
 */
@Slf4j
@Order(-2) // Higher precedence than default error handler
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Extract correlation ID if available
        String correlationId = exchange.getAttribute("gateway.correlation.id");
        if (correlationId == null) {
            correlationId = "unknown";
        }

        // Determine HTTP status and error details
        ErrorResponse errorResponse = createErrorResponse(ex, request, correlationId);

        // Log the error
        logError(ex, request, correlationId, errorResponse);

        // Set response status and content type
        response.setStatusCode(HttpStatus.valueOf(errorResponse.getStatus()));
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("X-Correlation-ID", correlationId);

        // Write error response
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException jsonEx) {
            log.error("Failed to serialize error response", jsonEx);
            return response.setComplete();
        }
    }

    /**
     * Create standardized error response based on exception type.
     */
    private ErrorResponse createErrorResponse(Throwable ex, ServerHttpRequest request, String correlationId) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
            .timestamp(Instant.now())
            .path(request.getPath().value())
            .method(request.getMethod().name())
            .correlationId(correlationId);

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            HttpStatus status = HttpStatus.valueOf(rse.getStatusCode().value());
            return builder
                .status(rse.getStatusCode().value())
                .error(status.getReasonPhrase())
                .message(getSecureMessage(rse.getReason(), status))
                .build();

        } else if (ex instanceof NotFoundException) {
            return builder
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("The requested service or endpoint was not found")
                .build();

        } else if (ex instanceof java.net.ConnectException) {
            return builder
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("Backend service is currently unavailable")
                .details(Map.of("suggestion", "Please try again later"))
                .build();

        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            return builder
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .error(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase())
                .message("Request timed out while processing")
                .details(Map.of("suggestion", "Please try again with a smaller request"))
                .build();

        } else if (ex instanceof io.jsonwebtoken.JwtException) {
            return builder
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid or expired authentication token")
                .details(Map.of("suggestion", "Please re-authenticate"))
                .build();

        } else if (ex.getMessage() != null && ex.getMessage().contains("Rate limit")) {
            return builder
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Rate limit exceeded")
                .details(Map.of(
                    "suggestion", "Please reduce request frequency",
                    "retryAfter", "60"
                ))
                .build();

        } else {
            // Generic internal server error
            return builder
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .details(Map.of("suggestion", "Please contact support if the issue persists"))
                .build();
        }
    }

    /**
     * Get secure error message that doesn't leak sensitive information.
     */
    private String getSecureMessage(String originalMessage, HttpStatus status) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return status.getReasonPhrase();
        }

        // For client errors (4xx), we can be more specific
        if (status.is4xxClientError()) {
            // Sanitize but keep meaningful information
            if (originalMessage.length() > 200) {
                return originalMessage.substring(0, 197) + "...";
            }
            return originalMessage;
        }

        // For server errors (5xx), be generic for security
        return "An internal error occurred while processing your request";
    }

    /**
     * Log error with appropriate level based on status code.
     */
    private void logError(Throwable ex, ServerHttpRequest request, String correlationId, ErrorResponse errorResponse) {
        String logMessage = "Gateway Error [{}] - {} {} -> Status: {}, Message: {}";

        if (errorResponse.getStatus() >= 500) {
            // Server errors - log as error with stack trace
            log.error(logMessage, correlationId, request.getMethod(), request.getPath(),
                errorResponse.getStatus(), errorResponse.getMessage(), ex);
        } else if (errorResponse.getStatus() >= 400) {
            // Client errors - log as warning
            log.warn(logMessage, correlationId, request.getMethod(), request.getPath(),
                errorResponse.getStatus(), errorResponse.getMessage());
        } else {
            // Other cases - log as info
            log.info(logMessage, correlationId, request.getMethod(), request.getPath(),
                errorResponse.getStatus(), errorResponse.getMessage());
        }
    }

    /**
     * Standardized error response model.
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String method;
        private String correlationId;
        @lombok.Singular
        private Map<String, Object> details;
    }
}