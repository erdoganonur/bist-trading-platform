package com.bisttrading.broker.dto;

/**
 * AlgoLab API endpoint constants
 * Python karşılığı: config.py dosyasındaki URL_* sabitler
 */
public final class AlgoLabEndpoints {

    private AlgoLabEndpoints() {
        // Utility class
    }

    // Authentication endpoints
    public static final String LOGIN_USER = "/api/LoginUser";
    public static final String LOGIN_USER_CONTROL = "/api/LoginUserControl";
    public static final String SESSION_REFRESH = "/api/SessionRefresh";

    // Market data endpoints
    public static final String GET_EQUITY_INFO = "/api/GetEquityInfo";
    public static final String GET_CANDLE_DATA = "/api/GetCandleData";

    // Account endpoints
    public static final String GET_SUB_ACCOUNTS = "/api/GetSubAccounts";
    public static final String INSTANT_POSITION = "/api/InstantPosition";
    public static final String TODAYS_TRANSACTION = "/api/TodaysTransaction";
    public static final String CASH_FLOW = "/api/CashFlow";
    public static final String ACCOUNT_EXTRE = "/api/AccountExtre";

    // VIOP endpoints
    public static final String VIOP_CUSTOMER_OVERALL = "/api/ViopCustomerOverall";
    public static final String VIOP_CUSTOMER_TRANSACTIONS = "/api/ViopCustomerTransactions";
    public static final String VIOP_COLLATERAL_INFO = "/api/ViopCollateralInfo";

    // Trading endpoints
    public static final String SEND_ORDER = "/api/SendOrder";
    public static final String MODIFY_ORDER = "/api/ModifyOrder";
    public static final String DELETE_ORDER = "/api/DeleteOrder";
    public static final String DELETE_ORDER_VIOP = "/api/DeleteOrderViop";

    // Order history endpoints
    public static final String GET_EQUITY_ORDER_HISTORY = "/api/GetEquityOrderHistory";
    public static final String GET_VIOP_ORDER_HISTORY = "/api/GetViopOrderHistory";

    // Risk endpoints
    public static final String RISK_SIMULATION = "/api/RiskSimulation";
}