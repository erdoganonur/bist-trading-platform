package com.bisttrading.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker Patterns.
 *
 * Provides fallback responses when backend services are unavailable
 * or experiencing high error rates. Each service has its own fallback
 * strategy based on business requirements.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback for User Management Service.
     * Returns cached user data or minimal service response.
     */
    @RequestMapping("/user-management")
    public Mono<ResponseEntity<Map<String, Object>>> userManagementFallback(ServerWebExchange exchange) {
        log.warn("User Management Service fallback triggered for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "USER_SERVICE_UNAVAILABLE",
            "User management service is temporarily unavailable",
            "Please try again in a few minutes. Essential user operations may be limited."
        );

        // Add service-specific fallback data
        response.put("fallbackData", Map.of(
            "availableOperations", new String[]{"profile-view", "basic-auth"},
            "limitedFeatures", new String[]{"user-creation", "password-reset", "profile-update"}
        ));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Fallback for Order Management Service.
     * Critical for trading - provides emergency response with clear instructions.
     */
    @RequestMapping("/order-service")
    public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback(ServerWebExchange exchange) {
        log.error("Order Management Service fallback triggered - CRITICAL for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "TRADING_SERVICE_UNAVAILABLE",
            "Trading service is temporarily unavailable",
            "Trading operations are currently suspended. Please contact support immediately if you have open positions."
        );

        // Critical trading fallback information
        response.put("tradingStatus", Map.of(
            "status", "SUSPENDED",
            "reason", "SERVICE_UNAVAILABLE",
            "emergencyContact", "+90-212-xxx-xxxx",
            "alternativeMethod", "Call broker for emergency trades"
        ));

        // Add emergency trading guidelines
        response.put("emergencyInstructions", new String[]{
            "Existing orders remain active",
            "New order placement is suspended",
            "Contact support for urgent trading needs",
            "Monitor positions through alternative channels"
        });

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Fallback for Market Data Service.
     * Provides cached data or redirects to alternative data sources.
     */
    @RequestMapping("/market-data-service")
    public Mono<ResponseEntity<Map<String, Object>>> marketDataFallback(ServerWebExchange exchange) {
        log.warn("Market Data Service fallback triggered for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "MARKET_DATA_UNAVAILABLE",
            "Real-time market data is temporarily unavailable",
            "Displaying cached data. For critical trading decisions, please verify with alternative sources."
        );

        // Market data specific fallback
        response.put("dataStatus", Map.of(
            "realTimeData", "UNAVAILABLE",
            "cachedData", "AVAILABLE",
            "lastUpdate", "2024-01-15T10:30:00Z",
            "alternativeSources", new String[]{"BIST website", "Mobile app", "News feeds"}
        ));

        response.put("cachedMarketData", Map.of(
            "disclaimer", "This data may be delayed. Do not use for trading decisions.",
            "lastKnownStatus", "Market was operational as of last update",
            "dataAge", "Up to 15 minutes old"
        ));

        return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Fallback for Portfolio Service.
     * Returns last known portfolio state with warnings.
     */
    @RequestMapping("/portfolio-service")
    public Mono<ResponseEntity<Map<String, Object>>> portfolioFallback(ServerWebExchange exchange) {
        log.warn("Portfolio Service fallback triggered for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "PORTFOLIO_SERVICE_UNAVAILABLE",
            "Portfolio service is temporarily unavailable",
            "Displaying last known portfolio state. Real-time calculations are suspended."
        );

        // Portfolio-specific fallback data
        response.put("portfolioStatus", Map.of(
            "realTimeCalculations", "SUSPENDED",
            "lastSnapshot", "Available from cache",
            "riskMetrics", "OUTDATED",
            "recommendation", "Avoid major portfolio changes until service restoration"
        ));

        return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Fallback for Notification Service.
     * Queues notifications for later delivery.
     */
    @RequestMapping("/notification-service")
    public Mono<ResponseEntity<Map<String, Object>>> notificationFallback(ServerWebExchange exchange) {
        log.warn("Notification Service fallback triggered for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "NOTIFICATION_SERVICE_UNAVAILABLE",
            "Notification service is temporarily unavailable",
            "Notifications are being queued and will be delivered when service is restored."
        );

        response.put("notificationStatus", Map.of(
            "delivery", "QUEUED",
            "criticalAlerts", "Will be sent via SMS",
            "queueStatus", "Active",
            "estimatedDelay", "Service restoration in progress"
        ));

        return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Generic fallback for unspecified services.
     */
    @RequestMapping("/**")
    public Mono<ResponseEntity<Map<String, Object>>> genericFallback(ServerWebExchange exchange) {
        log.warn("Generic fallback triggered for path: {}",
            exchange.getRequest().getPath());

        Map<String, Object> response = createFallbackResponse(
            "SERVICE_UNAVAILABLE",
            "The requested service is temporarily unavailable",
            "Our team is working to restore service. Please try again shortly."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response));
    }

    /**
     * Create standardized fallback response structure.
     */
    private Map<String, Object> createFallbackResponse(String errorCode, String message, String userMessage) {
        Map<String, Object> response = new HashMap<>();

        response.put("error", errorCode);
        response.put("message", message);
        response.put("userMessage", userMessage);
        response.put("timestamp", Instant.now());
        response.put("fallback", true);
        response.put("serviceStatus", "DEGRADED");

        response.put("support", Map.of(
            "helpdesk", "support@bisttrading.com.tr",
            "phone", "+90-212-xxx-xxxx",
            "businessHours", "09:00-18:00 Turkey Time",
            "emergencySupport", "24/7 for trading issues"
        ));

        return response;
    }
}