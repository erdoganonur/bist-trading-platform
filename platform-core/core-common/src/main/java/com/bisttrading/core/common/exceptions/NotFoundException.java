package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends BaseException {

    /**
     * Creates a not found exception with default error code.
     */
    public NotFoundException() {
        super(ErrorCodes.NOT_FOUND);
    }

    /**
     * Creates a not found exception with custom message.
     *
     * @param userMessage Custom user-friendly message
     */
    public NotFoundException(String userMessage) {
        super(ErrorCodes.NOT_FOUND, userMessage);
    }

    /**
     * Creates a not found exception with message parameters.
     *
     * @param messageParameters Parameters for formatting the error message
     */
    public NotFoundException(Object... messageParameters) {
        super(ErrorCodes.NOT_FOUND, messageParameters);
    }

    /**
     * Creates a not found exception with custom message and cause.
     *
     * @param userMessage Custom user-friendly message
     * @param cause       The root cause
     */
    public NotFoundException(String userMessage, Throwable cause) {
        super(ErrorCodes.NOT_FOUND, userMessage, cause);
    }

    // Convenience factory methods for common not found scenarios

    /**
     * Creates a not found exception for user.
     *
     * @param userId The user ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException user(String userId) {
        return new NotFoundException(ErrorCodes.USER_NOT_FOUND,
                String.format("Kullanıcı bulunamadı: %s", userId));
    }

    /**
     * Creates a not found exception for user by email.
     *
     * @param email The email that was not found
     * @return NotFoundException
     */
    public static NotFoundException userByEmail(String email) {
        return new NotFoundException(ErrorCodes.USER_NOT_FOUND,
                String.format("E-posta adresine kayıtlı kullanıcı bulunamadı: %s", email));
    }

    /**
     * Creates a not found exception for order.
     *
     * @param orderId The order ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException order(String orderId) {
        return new NotFoundException(ErrorCodes.ORDER_NOT_FOUND,
                String.format("Emir bulunamadı: %s", orderId));
    }

    /**
     * Creates a not found exception for portfolio.
     *
     * @param portfolioId The portfolio ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException portfolio(String portfolioId) {
        return new NotFoundException(ErrorCodes.PORTFOLIO_NOT_FOUND,
                String.format("Portföy bulunamadı: %s", portfolioId));
    }

    /**
     * Creates a not found exception for position.
     *
     * @param positionId The position ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException position(String positionId) {
        return new NotFoundException(ErrorCodes.POSITION_NOT_FOUND,
                String.format("Pozisyon bulunamadı: %s", positionId));
    }

    /**
     * Creates a not found exception for symbol.
     *
     * @param symbol The symbol that was not found
     * @return NotFoundException
     */
    public static NotFoundException symbol(String symbol) {
        return new NotFoundException(ErrorCodes.SYMBOL_NOT_FOUND,
                String.format("Hisse senedi bulunamadı: %s", symbol));
    }

    /**
     * Creates a not found exception for entity by ID.
     *
     * @param entityType The type of entity
     * @param entityId   The entity ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException entity(String entityType, String entityId) {
        return new NotFoundException(String.format("%s bulunamadı: %s", entityType, entityId));
    }

    /**
     * Creates a not found exception for entity by field.
     *
     * @param entityType The type of entity
     * @param fieldName  The field name
     * @param fieldValue The field value that was not found
     * @return NotFoundException
     */
    public static NotFoundException entityByField(String entityType, String fieldName, String fieldValue) {
        return new NotFoundException(String.format("%s bulunamadı (%s: %s)", entityType, fieldName, fieldValue));
    }

    /**
     * Creates a not found exception for resource.
     *
     * @param resourceType The type of resource
     * @param resourceId   The resource ID that was not found
     * @return NotFoundException
     */
    public static NotFoundException resource(String resourceType, String resourceId) {
        return new NotFoundException(String.format("%s kaynağı bulunamadı: %s", resourceType, resourceId));
    }
}