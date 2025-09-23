package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;

/**
 * Exception thrown when business rules are violated.
 * This exception represents domain-specific errors that occur during business logic execution.
 */
public class BusinessException extends BaseException {

    /**
     * Creates a business exception with error code.
     *
     * @param errorCode The error code
     */
    public BusinessException(ErrorCodes errorCode) {
        super(errorCode);
    }

    /**
     * Creates a business exception with error code and custom message.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     */
    public BusinessException(ErrorCodes errorCode, String userMessage) {
        super(errorCode, userMessage);
    }

    /**
     * Creates a business exception with error code and message parameters.
     *
     * @param errorCode         The error code
     * @param messageParameters Parameters for formatting the error message
     */
    public BusinessException(ErrorCodes errorCode, Object... messageParameters) {
        super(errorCode, messageParameters);
    }

    /**
     * Creates a business exception with error code, custom message and cause.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     * @param cause       The root cause
     */
    public BusinessException(ErrorCodes errorCode, String userMessage, Throwable cause) {
        super(errorCode, userMessage, cause);
    }

    /**
     * Creates a business exception with error code, message parameters and cause.
     *
     * @param errorCode         The error code
     * @param cause             The root cause
     * @param messageParameters Parameters for formatting the error message
     */
    public BusinessException(ErrorCodes errorCode, Throwable cause, Object... messageParameters) {
        super(errorCode, cause, messageParameters);
    }

    // Convenience factory methods for common business exceptions

    /**
     * Creates a business exception for insufficient balance.
     *
     * @param availableBalance Available balance
     * @param requiredAmount   Required amount
     * @return BusinessException
     */
    public static BusinessException insufficientBalance(String availableBalance, String requiredAmount) {
        return new BusinessException(ErrorCodes.INSUFFICIENT_BALANCE,
                "Yetersiz bakiye. Mevcut: %s, Gerekli: %s", availableBalance, requiredAmount);
    }

    /**
     * Creates a business exception for invalid order quantity.
     *
     * @param quantity The invalid quantity
     * @return BusinessException
     */
    public static BusinessException invalidOrderQuantity(String quantity) {
        return new BusinessException(ErrorCodes.INVALID_ORDER_QUANTITY,
                "Geçersiz emir miktarı: %s", quantity);
    }

    /**
     * Creates a business exception for invalid order price.
     *
     * @param price The invalid price
     * @return BusinessException
     */
    public static BusinessException invalidOrderPrice(String price) {
        return new BusinessException(ErrorCodes.INVALID_ORDER_PRICE,
                "Geçersiz emir fiyatı: %s", price);
    }

    /**
     * Creates a business exception for market closed.
     *
     * @param marketName The market name
     * @return BusinessException
     */
    public static BusinessException marketClosed(String marketName) {
        return new BusinessException(ErrorCodes.MARKET_CLOSED,
                "%s piyasası kapalı", marketName);
    }

    /**
     * Creates a business exception for trading hours violation.
     *
     * @param currentTime  Current time
     * @param tradingHours Trading hours
     * @return BusinessException
     */
    public static BusinessException tradingHoursViolation(String currentTime, String tradingHours) {
        return new BusinessException(ErrorCodes.TRADING_HOURS_VIOLATION,
                "İşlem saatleri dışında (%s). İşlem saatleri: %s", currentTime, tradingHours);
    }

    /**
     * Creates a business exception for daily limit exceeded.
     *
     * @param currentAmount Current amount
     * @param dailyLimit    Daily limit
     * @return BusinessException
     */
    public static BusinessException dailyLimitExceeded(String currentAmount, String dailyLimit) {
        return new BusinessException(ErrorCodes.DAILY_LIMIT_EXCEEDED,
                "Günlük limit aşıldı. Mevcut: %s, Limit: %s", currentAmount, dailyLimit);
    }

    /**
     * Creates a business exception for symbol suspension.
     *
     * @param symbol The suspended symbol
     * @return BusinessException
     */
    public static BusinessException symbolSuspended(String symbol) {
        return new BusinessException(ErrorCodes.SYMBOL_SUSPENDED,
                "%s hisse senedi işlemleri durdurulmuş", symbol);
    }

    /**
     * Creates a business exception for insufficient position.
     *
     * @param symbol            The symbol
     * @param availableQuantity Available quantity
     * @param requiredQuantity  Required quantity
     * @return BusinessException
     */
    public static BusinessException insufficientPosition(String symbol, String availableQuantity, String requiredQuantity) {
        return new BusinessException(ErrorCodes.INSUFFICIENT_POSITION,
                "%s için yetersiz pozisyon. Mevcut: %s, Gerekli: %s", symbol, availableQuantity, requiredQuantity);
    }
}