package com.bisttrading.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security Configuration for BIST Trading Platform API Gateway.
 *
 * Configures Spring Security WebFlux for reactive security with JWT authentication.
 * This works in conjunction with JwtAuthenticationFilter to provide comprehensive
 * security for the gateway.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    /**
     * Configure security filter chain for the gateway.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())

            // Disable form login and HTTP basic auth
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Configure CORS (handled by gateway configuration)
            .cors(cors -> cors.disable()) // We handle CORS at gateway level

            // Disable default logout
            .logout(logout -> logout.disable())

            // Stateless session management
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication required
                .pathMatchers(HttpMethod.GET, "/health/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/market-data/public/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/symbols/search").permitAll()

                // API Documentation
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .pathMatchers("/webjars/**").permitAll()

                // Admin endpoints - require admin role
                .pathMatchers("/api/v1/admin/**", "/api/v1/users/admin/**").hasRole("ADMIN")
                .pathMatchers("/actuator/gateway/**").hasRole("SUPER_ADMIN")

                // Actuator endpoints - require admin access
                .matchers(EndpointRequest.toAnyEndpoint().excluding("health", "info")).hasRole("ADMIN")

                // All other requests require authentication
                .anyExchange().authenticated()
            )

            // Custom authentication entry point
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )

            .build();
    }

    /**
     * Custom authentication entry point for handling authentication failures.
     */
    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerWebExchange.Builder exchangeBuilder = exchange.mutate();

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            exchange.getResponse().getHeaders().add("WWW-Authenticate",
                "Bearer realm=\"BIST Trading Platform\", " +
                "error=\"invalid_token\", " +
                "error_description=\"JWT token is missing or invalid\"");

            log.warn("Authentication failed for request: {} {} - {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage());

            String errorResponse = """
                {
                    "timestamp": "%s",
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Authentication required",
                    "path": "%s",
                    "suggestion": "Please provide a valid JWT token in Authorization header",
                    "loginEndpoint": "/api/v1/auth/login"
                }
                """.formatted(
                    java.time.Instant.now(),
                    exchange.getRequest().getPath().value()
                );

            org.springframework.core.io.buffer.DataBuffer buffer =
                exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());

            return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
        };
    }

    /**
     * Custom access denied handler for handling authorization failures.
     */
    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");

            log.warn("Access denied for request: {} {} - {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                denied.getMessage());

            String errorResponse = """
                {
                    "timestamp": "%s",
                    "status": 403,
                    "error": "Forbidden",
                    "message": "Access denied",
                    "path": "%s",
                    "suggestion": "Your account doesn't have sufficient privileges for this operation",
                    "supportContact": "support@bisttrading.com.tr"
                }
                """.formatted(
                    java.time.Instant.now(),
                    exchange.getRequest().getPath().value()
                );

            org.springframework.core.io.buffer.DataBuffer buffer =
                exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());

            return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
        };
    }

    /**
     * Bean for handling CORS configuration at application level.
     * Note: CORS is primarily handled by Gateway configuration,
     * but this provides additional security layer.
     */
    @Bean
    public org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
            new org.springframework.web.cors.CorsConfiguration();

        configuration.setAllowedOriginPatterns(java.util.List.of(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.bisttrading.com.tr",
            "https://*.bist.com.tr"
        ));

        configuration.setAllowedMethods(java.util.List.of("*"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource source =
            new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}