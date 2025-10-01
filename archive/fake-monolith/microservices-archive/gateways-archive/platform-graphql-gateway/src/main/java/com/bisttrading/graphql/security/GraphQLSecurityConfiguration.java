package com.bisttrading.graphql.security;

import com.bisttrading.graphql.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for GraphQL Gateway
 *
 * Configures JWT authentication for GraphQL endpoints with Turkish market compliance
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class GraphQLSecurityConfiguration {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Security filter chain for GraphQL endpoints
     */
    @Bean
    public SecurityFilterChain graphqlSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF disabled for GraphQL (uses tokens)
            .csrf(csrf -> csrf.disable())

            // Session management - stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/graphiql/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/graphql/playground").permitAll()
                .requestMatchers(HttpMethod.GET, "/graphql/schema").permitAll()
                .requestMatchers(HttpMethod.POST, "/graphql/public/**").permitAll()

                // Health and monitoring endpoints
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // All GraphQL operations require authentication
                .requestMatchers("/graphql/**").authenticated()
                .requestMatchers("/subscriptions/**").authenticated()

                // Default deny
                .anyRequest().authenticated()
            )

            // JWT configuration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Unauthorized GraphQL request from {}: {}",
                        request.getRemoteAddr(), authException.getMessage());

                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"errors\":[{\"message\":\"Authentication required\",\"extensions\":{\"code\":\"UNAUTHENTICATED\"}}]}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("Access denied for GraphQL request: {}", accessDeniedException.getMessage());

                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"errors\":[{\"message\":\"Access denied\",\"extensions\":{\"code\":\"FORBIDDEN\"}}]}"
                    );
                })
            )

            .build();
    }

    /**
     * JWT Decoder configuration
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Use the same key as the JWT provider
        return NimbusJwtDecoder
            .withSecretKey(jwtTokenProvider.getSigningKey())
            .build();
    }

    /**
     * JWT Authentication Converter
     *
     * Converts JWT claims to Spring Security authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        // Set principal claim name (usually 'sub' for user ID)
        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }

    /**
     * CORS configuration for GraphQL endpoints
     *
     * Configured for Turkish market domains and local development
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins - Turkish trading domains and development
        configuration.setAllowedOrigins(Arrays.asList(
            "https://trading.bist.com.tr",
            "https://app.bisttrading.com",
            "https://mobile.bisttrading.com",
            "https://localhost:3000",
            "http://localhost:3000",
            "https://localhost:8080",
            "http://localhost:8080"
        ));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Client-Version",
            "X-Device-Type",
            "Accept-Language"
        ));

        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "X-Request-ID"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/graphql/**", configuration);
        source.registerCorsConfiguration("/subscriptions/**", configuration);
        source.registerCorsConfiguration("/graphiql/**", configuration);

        return source;
    }

    /**
     * Custom authentication entry point for Turkish market compliance
     */
    public static class GraphQLAuthenticationEntryPoint
        implements org.springframework.security.web.AuthenticationEntryPoint {

        @Override
        public void commence(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                org.springframework.security.core.AuthenticationException authException)
                throws java.io.IOException {

            log.warn("Authentication failure for GraphQL request from {}: {}",
                request.getRemoteAddr(), authException.getMessage());

            // Log for Turkish market compliance
            log.info("COMPLIANCE_LOG - Unauthorized access attempt - IP: {}, User-Agent: {}, Timestamp: {}",
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                java.time.OffsetDateTime.now());

            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {
                  "errors": [
                    {
                      "message": "Authentication required",
                      "messageTR": "Kimlik doğrulama gerekli",
                      "extensions": {
                        "code": "UNAUTHENTICATED",
                        "timestamp": "%s"
                      }
                    }
                  ]
                }
                """.formatted(java.time.OffsetDateTime.now().toString()));
        }
    }

    /**
     * Custom access denied handler for Turkish market compliance
     */
    public static class GraphQLAccessDeniedHandler
        implements org.springframework.security.web.access.AccessDeniedHandler {

        @Override
        public void handle(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException)
                throws java.io.IOException {

            log.warn("Access denied for GraphQL request: {}", accessDeniedException.getMessage());

            // Log for Turkish market compliance
            String userId = "unknown";
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                userId = auth.getName();
            }

            log.info("COMPLIANCE_LOG - Access denied - User: {}, IP: {}, Operation: {}, Timestamp: {}",
                userId,
                request.getRemoteAddr(),
                request.getRequestURI(),
                java.time.OffsetDateTime.now());

            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {
                  "errors": [
                    {
                      "message": "Access denied",
                      "messageTR": "Erişim reddedildi",
                      "extensions": {
                        "code": "FORBIDDEN",
                        "timestamp": "%s"
                      }
                    }
                  ]
                }
                """.formatted(java.time.OffsetDateTime.now().toString()));
        }
    }
}