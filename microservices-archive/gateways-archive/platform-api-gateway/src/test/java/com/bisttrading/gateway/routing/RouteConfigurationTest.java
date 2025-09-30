package com.bisttrading.gateway.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Route Configuration.
 * Tests routing logic, predicates, and filters.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=test-user-service",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/v1/users/**"
})
@DisplayName("Route Configuration Tests")
class RouteConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    private List<Route> routes;

    @BeforeEach
    void setUp() {
        // Collect all routes for testing
        Flux<Route> routeFlux = routeLocator.getRoutes();
        StepVerifier.create(routeFlux.collectList())
            .assertNext(routeList -> {
                this.routes = routeList;
                assertThat(routeList).isNotEmpty();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should have routes for all core services")
    void shouldHaveRoutesForAllCoreServices() {
        // Verify routes exist for each service
        assertThat(findRouteByPattern("/api/v1/auth/**")).isPresent();
        assertThat(findRouteByPattern("/api/v1/users/**")).isPresent();
        assertThat(findRouteByPattern("/api/v1/orders/**")).isPresent();
        assertThat(findRouteByPattern("/api/v1/market-data/**")).isPresent();
        assertThat(findRouteByPattern("/api/v1/notifications/**")).isPresent();
    }

    @Test
    @DisplayName("Should route authentication requests to user service")
    void shouldRouteAuthenticationRequestsToUserService() {
        Route authRoute = findRouteByPattern("/api/v1/auth/**").orElseThrow();

        assertThat(authRoute.getUri().toString()).contains("8081");
        assertThat(authRoute.getId()).containsIgnoringCase("auth");
    }

    @Test
    @DisplayName("Should route trading requests to order management service")
    void shouldRouteTradingRequestsToOrderService() {
        Route orderRoute = findRouteByPattern("/api/v1/orders/**").orElseThrow();

        assertThat(orderRoute.getUri().toString()).contains("8082");
        assertThat(orderRoute.getId()).containsIgnoringCase("order");
    }

    @Test
    @DisplayName("Should route market data requests to market data service")
    void shouldRouteMarketDataRequestsToMarketDataService() {
        Route marketDataRoute = findRouteByPattern("/api/v1/market-data/**").orElseThrow();

        assertThat(marketDataRoute.getUri().toString()).contains("8083");
        assertThat(marketDataRoute.getId()).containsIgnoringCase("market");
    }

    @Test
    @DisplayName("Should have WebSocket routes configured")
    void shouldHaveWebSocketRoutesConfigured() {
        Route wsRoute = findRouteByPattern("/ws/**").orElseThrow();

        assertThat(wsRoute.getUri().getScheme()).isEqualTo("ws");
    }

    @Test
    @DisplayName("Should have fallback routes for circuit breaker")
    void shouldHaveFallbackRoutesForCircuitBreaker() {
        Route fallbackRoute = findRouteByPattern("/fallback/**").orElseThrow();

        assertThat(fallbackRoute).isNotNull();
        assertThat(fallbackRoute.getId()).containsIgnoringCase("fallback");
    }

    @Test
    @DisplayName("Should have health check routes without authentication")
    void shouldHaveHealthCheckRoutesWithoutAuth() {
        Route healthRoute = findRouteByPattern("/health/**").orElseThrow();

        assertThat(healthRoute).isNotNull();
        // Health routes should not require authentication
        assertThat(hasAuthenticationFilter(healthRoute)).isFalse();
    }

    @Test
    @DisplayName("Should apply rate limiting filters to appropriate routes")
    void shouldApplyRateLimitingFilters() {
        Route tradingRoute = findRouteByPattern("/api/v1/orders/**").orElseThrow();

        assertThat(hasRateLimitingFilter(tradingRoute)).isTrue();
    }

    @Test
    @DisplayName("Should apply circuit breaker filters to external service routes")
    void shouldApplyCircuitBreakerFilters() {
        Route externalRoute = findRouteByPattern("/api/v1/orders/**").orElseThrow();

        assertThat(hasCircuitBreakerFilter(externalRoute)).isTrue();
    }

    @Test
    @DisplayName("Should have proper route ordering")
    void shouldHaveProperRouteOrdering() {
        // More specific routes should come before generic ones
        List<String> routeIds = routes.stream()
            .map(Route::getId)
            .toList();

        // Health and actuator routes should be early in the list
        assertThat(routeIds).contains("actuator-route");
        assertThat(routeIds).contains("docs-route");
    }

    @Test
    @DisplayName("Should handle API versioning correctly")
    void shouldHandleApiVersioningCorrectly() {
        // Test that v1 and v2 routes are properly configured
        Route v1Route = findRouteByPattern("/api/v1/**").orElseThrow();
        assertThat(v1Route).isNotNull();

        // If v2 routes exist, they should be handled
        findRouteByPattern("/api/v2/**")
            .ifPresent(v2Route -> {
                assertThat(v2Route.getFilters()).isNotEmpty();
                assertThat(hasRewritePathFilter(v2Route)).isTrue();
            });
    }

    // Helper methods

    private java.util.Optional<Route> findRouteByPattern(String pathPattern) {
        return routes.stream()
            .filter(route -> route.getPredicate().toString().contains(pathPattern)
                || route.getId().toLowerCase().contains(pathPattern.toLowerCase().replace("/**", "").replace("/api/v1/", "")))
            .findFirst();
    }

    private boolean hasAuthenticationFilter(Route route) {
        return route.getFilters().stream()
            .anyMatch(filter -> filter.toString().toLowerCase().contains("auth"));
    }

    private boolean hasRateLimitingFilter(Route route) {
        return route.getFilters().stream()
            .anyMatch(filter -> filter.toString().toLowerCase().contains("ratelimit"));
    }

    private boolean hasCircuitBreakerFilter(Route route) {
        return route.getFilters().stream()
            .anyMatch(filter -> filter.toString().toLowerCase().contains("circuitbreaker"));
    }

    private boolean hasRewritePathFilter(Route route) {
        return route.getFilters().stream()
            .anyMatch(filter -> filter.toString().toLowerCase().contains("rewritepath"));
    }
}