package com.bisttrading.telegram.dto;

/**
 * Conversation state for multi-step commands.
 * Used to track where the user is in a conversation flow.
 */
public enum ConversationState {
    /**
     * No active conversation
     */
    NONE,

    /**
     * Login flow - waiting for username
     */
    WAITING_USERNAME,

    /**
     * Login flow - waiting for password
     */
    WAITING_PASSWORD,

    /**
     * AlgoLab login - waiting for username
     */
    WAITING_ALGOLAB_USERNAME,

    /**
     * AlgoLab login - waiting for password
     */
    WAITING_ALGOLAB_PASSWORD,

    /**
     * AlgoLab login - waiting for OTP code
     */
    WAITING_ALGOLAB_OTP,

    /**
     * Order flow - waiting for symbol
     */
    WAITING_ORDER_SYMBOL,

    /**
     * Order flow - waiting for side (BUY/SELL)
     */
    WAITING_ORDER_SIDE,

    /**
     * Order flow - waiting for price type (LIMIT/MARKET)
     */
    WAITING_ORDER_PRICE_TYPE,

    /**
     * Order flow - waiting for limit price
     */
    WAITING_ORDER_PRICE,

    /**
     * Order flow - waiting for quantity
     */
    WAITING_ORDER_QUANTITY,

    /**
     * Symbol search - waiting for search query
     */
    WAITING_SEARCH_QUERY,

    /**
     * Price alert - waiting for symbol
     */
    WAITING_ALERT_SYMBOL,

    /**
     * Price alert - waiting for target price
     */
    WAITING_ALERT_PRICE
}
