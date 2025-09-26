package com.bisttrading.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

/**
 * BIST Trading Platform API Gateway Application.
 *
 * This application serves as the single entry point for all client requests,
 * providing routing, authentication, rate limiting, and cross-cutting concerns
 * for the microservices architecture.
 *
 * Features:
 * - Dynamic routing to backend services
 * - JWT-based authentication and authorization
 * - Rate limiting with Redis
 * - Circuit breaker pattern implementation
 * - Request/response logging and monitoring
 * - CORS configuration
 * - Health checks and actuator endpoints
 *
 * @author BIST Trading Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
public class BistTradingGatewayApplication {

    /**
     * Main method to bootstrap the Spring Boot application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BistTradingGatewayApplication.class, args);
    }

    /**
     * CORS configuration for cross-origin requests.
     *
     * Configured to allow requests from frontend applications and
     * third-party integrations with proper security headers.
     *
     * @return Configured CORS web filter
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.bisttrading.com.tr",
            "https://*.bist.com.tr"
        ));
        corsConfig.setMaxAge(Duration.ofHours(1).getSeconds());
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization",
            "Access-Control-Request-Method", "Access-Control-Request-Headers",
            "X-Requested-With", "X-API-Key", "X-Client-Version"
        ));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Default route configuration for basic routing setup.
     *
     * This provides a minimal routing configuration for immediate functionality.
     * More comprehensive routing rules are defined in RouteConfiguration.
     *
     * @param builder RouteLocator builder
     * @return Configured RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Health check route - bypass authentication
            .route("actuator-route", r -> r
                .path("/actuator/**")
                .uri("no://op"))

            // API documentation route - bypass authentication
            .route("docs-route", r -> r
                .path("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .uri("no://op"))

            // Fallback route for unmatched requests
            .route("fallback-route", r -> r
                .path("/**")
                .uri("http://localhost:8080"))

            .build();
    }
}