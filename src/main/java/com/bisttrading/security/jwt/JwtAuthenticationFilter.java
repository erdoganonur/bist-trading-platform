package com.bisttrading.security.jwt;

import com.bisttrading.security.config.JwtProperties;
import com.bisttrading.security.service.CustomUserDetailsService;
import com.bisttrading.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for BIST Trading Platform.
 * Processes JWT tokens from HTTP requests and sets up Spring Security authentication.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                 CustomUserDetailsService userDetailsService,
                                 @Autowired(required = false) TokenBlacklistService tokenBlacklistService,
                                 JwtProperties jwtProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT token from request
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                processJwtToken(jwt, request);
            }
        } catch (Exception e) {
            log.error("JWT authentication filtresi hatası: {}", e.getMessage(), e);
            // Don't block the request, let it continue without authentication
            // The security configuration will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Processes the JWT token and sets up authentication.
     *
     * @param jwt JWT token
     * @param request HTTP request
     */
    private void processJwtToken(String jwt, HttpServletRequest request) {
        // Validate the token
        if (!jwtTokenProvider.validateToken(jwt)) {
            log.debug("Geçersiz JWT token");
            return;
        }

        // Check if token is blacklisted (only if service is available)
        if (tokenBlacklistService != null && tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.debug("Blacklist'te bulunan token kullanıldı");
            return;
        }

        // Ensure this is an access token (not refresh token)
        if (!jwtTokenProvider.isAccessToken(jwt)) {
            log.debug("Access token bekleniyor, farklı token türü alındı");
            return;
        }

        // Get user ID from token
        String userId = jwtTokenProvider.getUserIdFromToken(jwt);
        if (!StringUtils.hasText(userId)) {
            log.debug("Token'dan kullanıcı ID'si alınamadı");
            return;
        }

        // Only set authentication if not already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            setupAuthentication(userId, jwt, request);
        }
    }

    /**
     * Sets up Spring Security authentication context.
     *
     * @param userId User ID from token
     * @param jwt JWT token
     * @param request HTTP request
     */
    private void setupAuthentication(String userId, String jwt, HttpServletRequest request) {
        try {
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUserId(userId);

            if (userDetails != null && userDetails.isEnabled()) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // Set additional details
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // JWT token is now available through the authentication principal

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication başarılı - userId: {}", userId);
            } else {
                log.debug("Kullanıcı bulunamadı veya aktif değil - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("Authentication kurulurken hata - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /**
     * Extracts JWT token from HTTP request.
     *
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // Check Authorization header
        String bearerToken = request.getHeader(jwtProperties.getHeaderName());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix())) {
            return bearerToken.substring(jwtProperties.getTokenPrefix().length()).trim();
        }

        // Check query parameter as fallback (for WebSocket connections, etc.)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip filter for public endpoints
        return isPublicEndpoint(path) || isStaticResource(path);
    }

    /**
     * Checks if the request path is a public endpoint.
     *
     * @param path Request path
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        // Public authentication endpoints
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }

        // Health check endpoints
        if (path.startsWith("/actuator/") || path.equals("/health")) {
            return true;
        }

        // API documentation endpoints
        if (path.startsWith("/swagger-") || path.startsWith("/v3/api-docs") ||
            path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/")) {
            return true;
        }

        // Public market data endpoints (if any)
        if (path.startsWith("/api/v1/public/")) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the request path is for static resources.
     *
     * @param path Request path
     * @return true if static resource
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/fonts/") ||
               path.endsWith(".ico") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".jpeg") ||
               path.endsWith(".gif") ||
               path.endsWith(".css") ||
               path.endsWith(".js");
    }

    /**
     * Extracts client IP address from request.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Logs authentication attempt for security monitoring.
     *
     * @param request HTTP request
     * @param userId User ID
     * @param success Whether authentication was successful
     */
    private void logAuthenticationAttempt(HttpServletRequest request, String userId, boolean success) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        if (success) {
            log.info("JWT authentication başarılı - userId: {}, IP: {}, UserAgent: {}",
                userId, clientIp, userAgent);
        } else {
            log.warn("JWT authentication başarısız - userId: {}, IP: {}, UserAgent: {}",
                userId, clientIp, userAgent);
        }
    }
}