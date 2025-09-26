package com.bisttrading.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Authentication Filter for Spring Cloud Gateway.
 *
 * This filter handles JWT token validation for all incoming requests.
 * It provides comprehensive authentication and authorization including:
 * - JWT token extraction and validation
 * - Role-based access control
 * - Organization-based multi-tenancy
 * - Public endpoint bypass
 * - Custom authentication headers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenValidator tokenValidator;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
        "/health/**",
        "/actuator/health",
        "/actuator/info",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/market-data/public/**",
        "/api/v1/symbols/search",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/webjars/**"
    );

    // Admin-only endpoints
    private static final List<String> ADMIN_PATHS = List.of(
        "/api/v1/admin/**",
        "/api/v1/users/admin/**",
        "/actuator/gateway/**"
    );

    // Trading-specific endpoints that require additional validation
    private static final List<String> TRADING_PATHS = List.of(
        "/api/v1/orders/**",
        "/api/v1/positions/**",
        "/api/v1/trading/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.debug("JWT Filter processing request: {} {}", request.getMethod(), path);

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Public path accessed, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token from request
        String token = extractToken(request);

        if (token == null) {
            log.warn("No JWT token found in request to protected path: {}", path);
            return handleAuthenticationError(exchange, "Authentication required", "MISSING_TOKEN");
        }

        // Validate JWT token
        return tokenValidator.validateToken(token)
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    log.warn("Invalid JWT token for path {}: {}", path, validationResult.getErrorMessage());
                    return handleAuthenticationError(exchange, validationResult.getErrorMessage(), "INVALID_TOKEN");
                }

                JwtClaims claims = validationResult.getClaims();
                log.debug("Authenticated user: {}", claims.toLogString());

                // Check if user account is active
                if (!Boolean.TRUE.equals(claims.getActive())) {
                    log.warn("Inactive user attempted access: {}", claims.getUserId());
                    return handleAuthenticationError(exchange, "Account is inactive", "INACTIVE_ACCOUNT");
                }

                // Role-based authorization
                if (isAdminPath(path) && !claims.isAdmin()) {
                    log.warn("Non-admin user attempted admin access: {} -> {}", claims.getUserId(), path);
                    return handleAuthorizationError(exchange, "Admin access required", "INSUFFICIENT_PRIVILEGES");
                }

                // Trading-specific authorization
                if (isTradingPath(path) && !claims.canTrade()) {
                    log.warn("User without trading permissions attempted trading: {} -> {}", claims.getUserId(), path);
                    return handleAuthorizationError(exchange, "Trading permissions required", "TRADING_NOT_ALLOWED");
                }

                // Add user context to request headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-ID", claims.getUserId())
                    .header("X-User-Email", claims.getEmail())
                    .header("X-User-Roles", String.join(",", claims.getRoles()))
                    .header("X-Organization-ID", claims.getOrganizationId())
                    .header("X-Authenticated", "true")
                    .header("X-JWT-Subject", claims.getSubject())
                    .build();

                // Store claims in exchange attributes for use by other filters
                exchange.getAttributes().put("jwt.claims", claims);
                exchange.getAttributes().put("jwt.token", token);

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .doOnError(error -> log.error("JWT authentication filter error", error))
            .onErrorResume(error -> handleAuthenticationError(exchange, "Authentication failed", "INTERNAL_ERROR"));
    }

    /**
     * Extract JWT token from Authorization header or cookies.
     */
    private String extractToken(ServerHttpRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = tokenValidator.extractTokenFromHeader(authHeader);

        if (token != null) {
            return token;
        }

        // Try cookies as fallback
        return request.getCookies().getFirst("access_token") != null
            ? request.getCookies().getFirst("access_token").getValue()
            : null;
    }

    /**
     * Check if path is public (doesn't require authentication).
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Check if path requires admin privileges.
     */
    private boolean isAdminPath(String path) {
        return ADMIN_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Check if path is trading-related.
     */
    private boolean isTradingPath(String path) {
        return TRADING_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Handle authentication errors (401 Unauthorized).
     */
    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String message, String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("WWW-Authenticate", "Bearer realm=\"BIST Trading Platform\"");

        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            message,
            errorCode,
            exchange.getRequest().getPath().value()
        );

        return writeErrorResponse(response, errorResponse);
    }

    /**
     * Handle authorization errors (403 Forbidden).
     */
    private Mono<Void> handleAuthorizationError(ServerWebExchange exchange, String message, String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            message,
            errorCode,
            exchange.getRequest().getPath().value()
        );

        return writeErrorResponse(response, errorResponse);
    }

    /**
     * Create standardized error response.
     */
    private Map<String, Object> createErrorResponse(int status, String error, String message, String code, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("path", path);

        // Add helpful information for authentication errors
        if (status == 401) {
            errorResponse.put("suggestion", "Please provide a valid JWT token in Authorization header");
            errorResponse.put("loginEndpoint", "/api/v1/auth/login");
        } else if (status == 403) {
            errorResponse.put("suggestion", "Your account doesn't have sufficient privileges for this operation");
            errorResponse.put("supportContact", "support@bisttrading.com.tr");
        }

        return errorResponse;
    }

    /**
     * Write error response to client.
     */
    private Mono<Void> writeErrorResponse(ServerHttpResponse response, Map<String, Object> errorResponse) {
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100; // Execute after logging but before other business filters
    }
}