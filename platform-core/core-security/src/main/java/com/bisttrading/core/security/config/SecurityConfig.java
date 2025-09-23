package com.bisttrading.core.security.config;

import com.bisttrading.core.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for BIST Trading Platform.
 * Configures JWT authentication, CORS, endpoint security, and security headers.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the security filter chain.
     *
     * @param http HttpSecurity configuration
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure session management - stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Public market data endpoints
                .requestMatchers("/api/v1/public/**").permitAll()

                // Health check and monitoring endpoints
                .requestMatchers("/actuator/health", "/health").permitAll()
                .requestMatchers("/actuator/info", "/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()

                // API documentation endpoints
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()

                // Static resources
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/favicon.ico", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif").permitAll()

                // WebSocket endpoints (if any)
                .requestMatchers("/ws/**").authenticated()

                // Admin endpoints - require ADMIN role
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Trading endpoints - require authenticated user with trading permissions
                .requestMatchers("/api/v1/trading/**").hasAnyRole("TRADER", "CUSTOMER", "PROFESSIONAL_TRADER")

                // Portfolio endpoints - require customer or trader role
                .requestMatchers("/api/v1/portfolio/**").hasAnyRole("CUSTOMER", "TRADER", "PROFESSIONAL_TRADER")

                // User management endpoints - require authenticated user
                .requestMatchers("/api/v1/users/**").authenticated()

                // Risk management endpoints - require risk manager role
                .requestMatchers("/api/v1/risk/**").hasAnyRole("RISK_MANAGER", "ADMIN")

                // Compliance endpoints - require compliance officer role
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("COMPLIANCE_OFFICER", "ADMIN")

                // Market data endpoints - require authenticated user
                .requestMatchers("/api/v1/market-data/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Configure security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
            )

            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security filter chain yapılandırıldı");
        return http.build();
    }

    /**
     * Configures CORS settings for the application.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins in production, all in development
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",     // React development server
            "http://localhost:4200",     // Angular development server
            "https://*.bisttrading.com", // Production domains
            "https://*.bist.com.tr"      // BIST domains
        ));

        // Allow specific HTTP methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // Allow specific headers
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Cache-Control",
            "X-File-Name",
            "X-Request-ID",
            "X-Client-Version"
        ));

        // Expose specific headers to the client
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Disposition",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size",
            "X-Request-ID"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS yapılandırması tamamlandı");
        return source;
    }

    /**
     * Password encoder bean for hashing passwords.
     *
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        log.info("BCrypt password encoder yapılandırıldı - strength: 12");
        return encoder;
    }

    /**
     * Authentication manager bean for handling authentication.
     *
     * @param config AuthenticationConfiguration
     * @return AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}