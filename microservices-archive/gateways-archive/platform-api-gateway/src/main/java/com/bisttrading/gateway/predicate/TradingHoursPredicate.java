package com.bisttrading.gateway.predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Custom Route Predicate for Trading Hours.
 *
 * This predicate evaluates whether the current request time falls within
 * BIST trading hours. It considers:
 * - Market open/close times
 * - Turkish holidays
 * - Weekend restrictions
 * - Special trading sessions
 */
@Slf4j
@Component
public class TradingHoursPredicate extends AbstractRoutePredicateFactory<TradingHoursPredicate.Config> {

    // BIST trading hours in Turkey timezone
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(18, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 30);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

    public TradingHoursPredicate() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            ZonedDateTime now = ZonedDateTime.now(TURKEY_ZONE);

            boolean isWithinTradingHours = evaluateTradingHours(now, config);

            if (!isWithinTradingHours) {
                log.debug("Request blocked - outside trading hours. Current time: {}, Config: {}",
                    now, config.toString());

                // Add headers to inform client about trading status
                exchange.getResponse().getHeaders().add("X-Trading-Status", "CLOSED");
                exchange.getResponse().getHeaders().add("X-Market-Open-Time",
                    MARKET_OPEN.atDate(LocalDate.now()).atZone(TURKEY_ZONE).toString());
                exchange.getResponse().getHeaders().add("X-Market-Close-Time",
                    MARKET_CLOSE.atDate(LocalDate.now()).atZone(TURKEY_ZONE).toString());
            } else {
                exchange.getResponse().getHeaders().add("X-Trading-Status", "OPEN");
                exchange.getResponse().getHeaders().add("X-Trading-Session", getCurrentSession(now.toLocalTime()));
            }

            return isWithinTradingHours;
        };
    }

    /**
     * Evaluate if current time is within trading hours based on configuration.
     */
    private boolean evaluateTradingHours(ZonedDateTime now, Config config) {
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalDate currentDate = now.toLocalDate();

        // Check if it's a weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return config.allowWeekends;
        }

        // Check if it's a Turkish public holiday
        if (isTurkishHoliday(currentDate)) {
            return config.allowHolidays;
        }

        // Check basic trading hours
        if (currentTime.isBefore(MARKET_OPEN) || currentTime.isAfter(MARKET_CLOSE)) {
            return config.allowAfterHours;
        }

        // Check lunch break (if configured to restrict)
        if (!config.allowLunchBreak &&
            currentTime.isAfter(LUNCH_START) && currentTime.isBefore(LUNCH_END)) {
            return false;
        }

        // Special session handling
        if (config.sessionType != null) {
            return isSessionActive(currentTime, config.sessionType);
        }

        return true;
    }

    /**
     * Get current trading session based on time.
     */
    private String getCurrentSession(LocalTime time) {
        if (time.isBefore(MARKET_OPEN)) {
            return "PRE_MARKET";
        } else if (time.isBefore(LUNCH_START)) {
            return "MORNING_SESSION";
        } else if (time.isBefore(LUNCH_END)) {
            return "LUNCH_BREAK";
        } else if (time.isBefore(MARKET_CLOSE)) {
            return "AFTERNOON_SESSION";
        } else {
            return "POST_MARKET";
        }
    }

    /**
     * Check if specific session is active.
     */
    private boolean isSessionActive(LocalTime time, SessionType sessionType) {
        return switch (sessionType) {
            case MORNING -> time.isAfter(MARKET_OPEN) && time.isBefore(LUNCH_START);
            case AFTERNOON -> time.isAfter(LUNCH_END) && time.isBefore(MARKET_CLOSE);
            case CONTINUOUS -> time.isAfter(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
            case PRE_MARKET -> time.isBefore(MARKET_OPEN);
            case POST_MARKET -> time.isAfter(MARKET_CLOSE);
        };
    }

    /**
     * Check if the given date is a Turkish public holiday.
     * This should be expanded with a comprehensive holiday calendar.
     */
    private boolean isTurkishHoliday(LocalDate date) {
        // Basic Turkish holidays (should be expanded with proper holiday calendar)
        int year = date.getYear();

        // Fixed holidays
        List<LocalDate> fixedHolidays = List.of(
            LocalDate.of(year, 1, 1),   // New Year
            LocalDate.of(year, 4, 23),  // National Sovereignty Day
            LocalDate.of(year, 5, 1),   // Labor Day
            LocalDate.of(year, 5, 19),  // Youth and Sports Day
            LocalDate.of(year, 7, 15),  // Democracy Day
            LocalDate.of(year, 8, 30),  // Victory Day
            LocalDate.of(year, 10, 29)  // Republic Day
        );

        if (fixedHolidays.contains(date)) {
            return true;
        }

        // Religious holidays (dates vary each year - should use proper calendar)
        // This is a simplified version - implement proper Islamic calendar calculation
        return isReligiousHoliday(date);
    }

    /**
     * Simplified religious holiday check.
     * In production, this should use a proper Islamic calendar library.
     */
    private boolean isReligiousHoliday(LocalDate date) {
        // This is a placeholder - implement proper Islamic calendar
        // Common religious holidays: Ramadan Feast, Sacrifice Feast, etc.
        return false;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("sessionType");
    }

    /**
     * Configuration class for Trading Hours predicate.
     */
    public static class Config {
        private SessionType sessionType;
        private boolean allowWeekends = false;
        private boolean allowHolidays = false;
        private boolean allowAfterHours = false;
        private boolean allowLunchBreak = true;
        private String timezone = "Europe/Istanbul";

        // Getters and setters
        public SessionType getSessionType() { return sessionType; }
        public void setSessionType(SessionType sessionType) { this.sessionType = sessionType; }

        public boolean isAllowWeekends() { return allowWeekends; }
        public void setAllowWeekends(boolean allowWeekends) { this.allowWeekends = allowWeekends; }

        public boolean isAllowHolidays() { return allowHolidays; }
        public void setAllowHolidays(boolean allowHolidays) { this.allowHolidays = allowHolidays; }

        public boolean isAllowAfterHours() { return allowAfterHours; }
        public void setAllowAfterHours(boolean allowAfterHours) { this.allowAfterHours = allowAfterHours; }

        public boolean isAllowLunchBreak() { return allowLunchBreak; }
        public void setAllowLunchBreak(boolean allowLunchBreak) { this.allowLunchBreak = allowLunchBreak; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        @Override
        public String toString() {
            return String.format("TradingHours[session=%s, weekends=%s, holidays=%s, afterHours=%s, lunch=%s]",
                sessionType, allowWeekends, allowHolidays, allowAfterHours, allowLunchBreak);
        }
    }

    /**
     * Trading session types.
     */
    public enum SessionType {
        MORNING,      // 09:30-12:30
        AFTERNOON,    // 14:00-18:00
        CONTINUOUS,   // 09:30-18:00 (excluding lunch)
        PRE_MARKET,   // Before 09:30
        POST_MARKET   // After 18:00
    }
}