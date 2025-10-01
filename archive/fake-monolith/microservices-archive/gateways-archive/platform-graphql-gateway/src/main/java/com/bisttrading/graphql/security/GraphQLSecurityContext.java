package com.bisttrading.graphql.security;

import com.bisttrading.graphql.security.JwtTokenProvider;
import com.bisttrading.graphql.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL Security Context for BIST Trading Platform
 *
 * Provides security context information for GraphQL operations including:
 * - Current user identification
 * - Role-based access control
 * - JWT token validation
 * - Turkish market compliance logging
 */
@Slf4j
@Component("graphQLSecurityContext")
@RequiredArgsConstructor
public class GraphQLSecurityContext {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Get current authenticated user ID
     *
     * @return User ID from JWT token
     * @throws UnauthorizedException if no authenticated user
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Attempted to access GraphQL operation without authentication");
            throw new UnauthorizedException("Authentication required");
        }

        String userId = authentication.getName();
        if (userId == null || userId.equals("anonymousUser")) {
            log.warn("Invalid or anonymous authentication in GraphQL context");
            throw new UnauthorizedException("Valid authentication required");
        }

        log.trace("GraphQL operation requested by user: {}", userId);
        return userId;
    }

    /**
     * Get current authentication object
     *
     * @return Spring Security Authentication
     */
    public Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }

        return authentication;
    }

    /**
     * Check if current user has specific role
     *
     * @param role Role to check (without ROLE_ prefix)
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        try {
            Authentication authentication = getCurrentAuthentication();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            boolean hasRole = authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));

            log.trace("Role check for user {}: {} = {}",
                authentication.getName(), roleWithPrefix, hasRole);

            return hasRole;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Get all roles for current user
     *
     * @return List of role names (without ROLE_ prefix)
     */
    public List<String> getCurrentUserRoles() {
        Authentication authentication = getCurrentAuthentication();

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
            .collect(Collectors.toList());
    }

    /**
     * Check if current user can access another user's data
     *
     * Business rule: Users can access their own data, admins can access any data
     *
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     */
    public boolean canAccessUser(String targetUserId) {
        try {
            String currentUserId = getCurrentUserId();

            // Users can always access their own data
            if (currentUserId.equals(targetUserId)) {
                return true;
            }

            // Admins can access any user's data
            if (hasRole("ADMIN") || hasRole("SUPER_ADMIN")) {
                log.info("Admin user {} accessing data for user {}", currentUserId, targetUserId);
                return true;
            }

            // Professional users can access limited data of other users (for analysis)
            if (hasRole("PROFESSIONAL")) {
                log.info("Professional user {} accessing limited data for user {}", currentUserId, targetUserId);
                return true;
            }

            log.warn("User {} denied access to user {} data", currentUserId, targetUserId);
            return false;

        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Check if current user can perform admin operations
     *
     * @return true if user has admin privileges
     */
    public boolean canPerformAdminOperations() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    /**
     * Check if current user can access trading operations
     *
     * Business rule: Must be verified and have appropriate KYC level
     *
     * @return true if trading is allowed
     */
    public boolean canTrade() {
        try {
            // Get JWT token to extract claims
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return false;
            }

            String token = extractTokenFromRequest(request);
            if (token == null) {
                return false;
            }

            // Extract user verification status from JWT claims
            boolean isVerified = jwtTokenProvider.extractClaim(token, "verified", Boolean.class);
            String kycLevel = jwtTokenProvider.extractClaim(token, "kycLevel", String.class);

            boolean canTrade = isVerified &&
                (kycLevel != null && !kycLevel.equals("BASIC"));

            if (!canTrade) {
                log.warn("User {} denied trading access - verified: {}, kycLevel: {}",
                    getCurrentUserId(), isVerified, kycLevel);
            }

            return canTrade;

        } catch (Exception e) {
            log.error("Error checking trading permissions: ", e);
            return false;
        }
    }

    /**
     * Check if current user can access real-time market data
     *
     * Business rule: Premium and professional users get real-time data
     *
     * @return true if real-time data access is allowed
     */
    public boolean canAccessRealTimeData() {
        return hasRole("PREMIUM_USER") ||
               hasRole("PROFESSIONAL") ||
               hasRole("ADMIN") ||
               hasRole("SUPER_ADMIN");
    }

    /**
     * Get user's subscription level for rate limiting
     *
     * @return Subscription level string
     */
    public String getUserSubscriptionLevel() {
        if (hasRole("SUPER_ADMIN")) {
            return "UNLIMITED";
        } else if (hasRole("ADMIN")) {
            return "ADMIN";
        } else if (hasRole("PROFESSIONAL")) {
            return "PROFESSIONAL";
        } else if (hasRole("PREMIUM_USER")) {
            return "PREMIUM";
        } else {
            return "BASIC";
        }
    }

    /**
     * Log security event for compliance (Turkish market regulations)
     *
     * @param operation GraphQL operation name
     * @param details Additional details
     */
    public void logSecurityEvent(String operation, String details) {
        try {
            String userId = getCurrentUserId();
            String roles = String.join(",", getCurrentUserRoles());
            HttpServletRequest request = getCurrentRequest();
            String clientIp = request != null ? getClientIpAddress(request) : "unknown";

            // Structured logging for compliance and audit
            log.info("SECURITY_EVENT - User: {}, Roles: {}, Operation: {}, IP: {}, Details: {}",
                userId, roles, operation, clientIp, details);

        } catch (Exception e) {
            log.error("Error logging security event: ", e);
        }
    }

    /**
     * Validate GraphQL operation complexity for current user
     *
     * @param complexity Calculated query complexity
     * @return true if complexity is within limits
     */
    public boolean isComplexityAllowed(int complexity) {
        String subscriptionLevel = getUserSubscriptionLevel();

        int maxComplexity = switch (subscriptionLevel) {
            case "UNLIMITED" -> Integer.MAX_VALUE;
            case "ADMIN" -> 5000;
            case "PROFESSIONAL" -> 2000;
            case "PREMIUM" -> 1000;
            case "BASIC" -> 500;
            default -> 100;
        };

        boolean allowed = complexity <= maxComplexity;

        if (!allowed) {
            log.warn("Query complexity {} exceeds limit {} for user {} (subscription: {})",
                complexity, maxComplexity, getCurrentUserId(), subscriptionLevel);

            logSecurityEvent("QUERY_COMPLEXITY_EXCEEDED",
                String.format("complexity=%d, limit=%d, subscription=%s",
                    complexity, maxComplexity, subscriptionLevel));
        }

        return allowed;
    }

    // ===============================
    // Private helper methods
    // ===============================

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // Check various headers for real client IP (behind proxies/load balancers)
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract organization ID from JWT for multi-tenancy
     *
     * @return Organization ID or null
     */
    public String getCurrentOrganizationId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }

            String token = extractTokenFromRequest(request);
            if (token == null) {
                return null;
            }

            return jwtTokenProvider.extractClaim(token, "organizationId", String.class);

        } catch (Exception e) {
            log.debug("No organization ID found in token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if current session is within Turkish trading hours
     * (For compliance with BIST regulations)
     *
     * @return true if within trading hours
     */
    public boolean isWithinTradingHours() {
        // This would integrate with Turkish market calendar
        // For now, return true - actual implementation would check BIST hours
        return true;
    }

    /**
     * Get user's preferred language from JWT claims
     *
     * @return Language code (tr, en, etc.) or default 'tr'
     */
    public String getUserLanguage() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return "tr";  // Default to Turkish
            }

            String token = extractTokenFromRequest(request);
            if (token == null) {
                return "tr";
            }

            String language = jwtTokenProvider.extractClaim(token, "language", String.class);
            return language != null ? language : "tr";

        } catch (Exception e) {
            log.debug("Could not extract language from token, defaulting to Turkish: {}", e.getMessage());
            return "tr";
        }
    }
}