package com.bisttrading.ums.dto;

import java.util.Map;

public class UserPreferencesResponse {
    private String userId;
    private String theme;
    private String language;
    private String timezone;
    private String currency;
    private boolean realTimeNotifications;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private Map<String, Object> tradingPreferences;
    private Map<String, Object> displayPreferences;

    public UserPreferencesResponse() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isRealTimeNotifications() { return realTimeNotifications; }
    public void setRealTimeNotifications(boolean realTimeNotifications) { this.realTimeNotifications = realTimeNotifications; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }

    public Map<String, Object> getTradingPreferences() { return tradingPreferences; }
    public void setTradingPreferences(Map<String, Object> tradingPreferences) { this.tradingPreferences = tradingPreferences; }

    public Map<String, Object> getDisplayPreferences() { return displayPreferences; }
    public void setDisplayPreferences(Map<String, Object> displayPreferences) { this.displayPreferences = displayPreferences; }
}