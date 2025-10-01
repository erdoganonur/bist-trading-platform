package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;

/**
 * Exception thrown when a user is not authorized to perform an operation.
 */
public class UnauthorizedException extends BaseException {

    /**
     * Creates an unauthorized exception with default error code.
     */
    public UnauthorizedException() {
        super(ErrorCodes.UNAUTHORIZED);
    }

    /**
     * Creates an unauthorized exception with custom message.
     *
     * @param userMessage Custom user-friendly message
     */
    public UnauthorizedException(String userMessage) {
        super(ErrorCodes.UNAUTHORIZED, userMessage);
    }

    /**
     * Creates an unauthorized exception with error code.
     *
     * @param errorCode The specific error code
     */
    public UnauthorizedException(ErrorCodes errorCode) {
        super(errorCode);
    }

    /**
     * Creates an unauthorized exception with error code and custom message.
     *
     * @param errorCode   The specific error code
     * @param userMessage Custom user-friendly message
     */
    public UnauthorizedException(ErrorCodes errorCode, String userMessage) {
        super(errorCode, userMessage);
    }

    /**
     * Creates an unauthorized exception with message parameters.
     *
     * @param messageParameters Parameters for formatting the error message
     */
    public UnauthorizedException(Object... messageParameters) {
        super(ErrorCodes.UNAUTHORIZED, messageParameters);
    }

    /**
     * Creates an unauthorized exception with custom message and cause.
     *
     * @param userMessage Custom user-friendly message
     * @param cause       The root cause
     */
    public UnauthorizedException(String userMessage, Throwable cause) {
        super(ErrorCodes.UNAUTHORIZED, userMessage, cause);
    }

    // Convenience factory methods for common unauthorized scenarios

    /**
     * Creates an unauthorized exception for invalid credentials.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS);
    }

    /**
     * Creates an unauthorized exception for expired token.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException tokenExpired() {
        return new UnauthorizedException(ErrorCodes.TOKEN_EXPIRED);
    }

    /**
     * Creates an unauthorized exception for invalid token.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCodes.TOKEN_INVALID);
    }

    /**
     * Creates an unauthorized exception for missing token.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException missingToken() {
        return new UnauthorizedException(ErrorCodes.TOKEN_MISSING);
    }

    /**
     * Creates an unauthorized exception for locked account.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException accountLocked() {
        return new UnauthorizedException(ErrorCodes.ACCOUNT_LOCKED);
    }

    /**
     * Creates an unauthorized exception for disabled account.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException accountDisabled() {
        return new UnauthorizedException(ErrorCodes.ACCOUNT_DISABLED);
    }

    /**
     * Creates an unauthorized exception for expired account.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException accountExpired() {
        return new UnauthorizedException(ErrorCodes.ACCOUNT_EXPIRED);
    }

    /**
     * Creates an unauthorized exception for expired password.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException passwordExpired() {
        return new UnauthorizedException(ErrorCodes.PASSWORD_EXPIRED);
    }

    /**
     * Creates an unauthorized exception for invalid refresh token.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN);
    }

    /**
     * Creates an unauthorized exception for insufficient permissions.
     *
     * @param operation The operation that requires permission
     * @return UnauthorizedException
     */
    public static UnauthorizedException insufficientPermissions(String operation) {
        return new UnauthorizedException(ErrorCodes.FORBIDDEN,
                String.format("%s işlemi için yetkiniz bulunmamaktadır", operation));
    }

    /**
     * Creates an unauthorized exception for resource access.
     *
     * @param resourceType The type of resource
     * @param resourceId   The resource ID
     * @return UnauthorizedException
     */
    public static UnauthorizedException resourceAccess(String resourceType, String resourceId) {
        return new UnauthorizedException(ErrorCodes.FORBIDDEN,
                String.format("%s kaynağına erişim izniniz yok: %s", resourceType, resourceId));
    }

    /**
     * Creates an unauthorized exception for portfolio access.
     *
     * @param portfolioId The portfolio ID
     * @return UnauthorizedException
     */
    public static UnauthorizedException portfolioAccess(String portfolioId) {
        return new UnauthorizedException(ErrorCodes.PORTFOLIO_ACCESS_DENIED,
                String.format("Portföy erişim izni yok: %s", portfolioId));
    }

    /**
     * Creates an unauthorized exception for session timeout.
     *
     * @return UnauthorizedException
     */
    public static UnauthorizedException sessionTimeout() {
        return new UnauthorizedException(ErrorCodes.TOKEN_EXPIRED,
                "Oturum süresi dolmuş. Lütfen tekrar giriş yapınız");
    }

    /**
     * Creates an unauthorized exception for role-based access.
     *
     * @param requiredRole The required role
     * @param userRole     The user's current role
     * @return UnauthorizedException
     */
    public static UnauthorizedException roleBasedAccess(String requiredRole, String userRole) {
        return new UnauthorizedException(ErrorCodes.FORBIDDEN,
                String.format("Bu işlem için %s yetkisi gerekli. Mevcut yetki: %s", requiredRole, userRole));
    }
}