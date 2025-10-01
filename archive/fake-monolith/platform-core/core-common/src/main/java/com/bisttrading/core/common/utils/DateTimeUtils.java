package com.bisttrading.core.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utility class for date and time operations with Turkish timezone and locale support.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeUtils {

    /**
     * Turkish timezone (Turkey Time - TRT).
     */
    public static final ZoneId TURKISH_ZONE = ZoneId.of("Europe/Istanbul");

    /**
     * Turkish locale.
     */
    public static final Locale TURKISH_LOCALE = new Locale("tr", "TR");

    /**
     * Common date formatters for Turkish locale.
     */
    public static final DateTimeFormatter TURKISH_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", TURKISH_LOCALE);
    public static final DateTimeFormatter TURKISH_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", TURKISH_LOCALE);
    public static final DateTimeFormatter TURKISH_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", TURKISH_LOCALE);
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * BIST trading session times.
     */
    public static final LocalTime BIST_MORNING_SESSION_START = LocalTime.of(10, 0);
    public static final LocalTime BIST_MORNING_SESSION_END = LocalTime.of(12, 30);
    public static final LocalTime BIST_AFTERNOON_SESSION_START = LocalTime.of(14, 0);
    public static final LocalTime BIST_AFTERNOON_SESSION_END = LocalTime.of(18, 0);

    /**
     * Returns the current date and time in Turkish timezone.
     *
     * @return Current ZonedDateTime in Turkish timezone
     */
    public static ZonedDateTime nowInTurkey() {
        return ZonedDateTime.now(TURKISH_ZONE);
    }

    /**
     * Returns the current date in Turkish timezone.
     *
     * @return Current LocalDate in Turkish timezone
     */
    public static LocalDate todayInTurkey() {
        return LocalDate.now(TURKISH_ZONE);
    }

    /**
     * Returns the current time in Turkish timezone.
     *
     * @return Current LocalTime in Turkish timezone
     */
    public static LocalTime nowTimeInTurkey() {
        return LocalTime.now(TURKISH_ZONE);
    }

    /**
     * Converts a ZonedDateTime to Turkish timezone.
     *
     * @param dateTime The datetime to convert
     * @return ZonedDateTime in Turkish timezone
     */
    public static ZonedDateTime toTurkishTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(TURKISH_ZONE);
    }

    /**
     * Converts a LocalDateTime to ZonedDateTime in Turkish timezone.
     *
     * @param localDateTime The local datetime to convert
     * @return ZonedDateTime in Turkish timezone
     */
    public static ZonedDateTime toTurkishTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(TURKISH_ZONE);
    }

    /**
     * Formats a LocalDate using Turkish date format.
     *
     * @param date The date to format
     * @return Formatted date string (dd.MM.yyyy)
     */
    public static String formatTurkishDate(LocalDate date) {
        return date.format(TURKISH_DATE_FORMATTER);
    }

    /**
     * Formats a LocalDateTime using Turkish datetime format.
     *
     * @param dateTime The datetime to format
     * @return Formatted datetime string (dd.MM.yyyy HH:mm:ss)
     */
    public static String formatTurkishDateTime(LocalDateTime dateTime) {
        return dateTime.format(TURKISH_DATETIME_FORMATTER);
    }

    /**
     * Formats a ZonedDateTime using Turkish datetime format in Turkish timezone.
     *
     * @param zonedDateTime The zoned datetime to format
     * @return Formatted datetime string (dd.MM.yyyy HH:mm:ss)
     */
    public static String formatTurkishDateTime(ZonedDateTime zonedDateTime) {
        return toTurkishTime(zonedDateTime).format(TURKISH_DATETIME_FORMATTER);
    }

    /**
     * Formats a LocalTime using Turkish time format.
     *
     * @param time The time to format
     * @return Formatted time string (HH:mm:ss)
     */
    public static String formatTurkishTime(LocalTime time) {
        return time.format(TURKISH_TIME_FORMATTER);
    }

    /**
     * Parses a Turkish formatted date string.
     *
     * @param dateString Date string in format dd.MM.yyyy
     * @return Parsed LocalDate
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static LocalDate parseTurkishDate(String dateString) {
        return LocalDate.parse(dateString, TURKISH_DATE_FORMATTER);
    }

    /**
     * Parses a Turkish formatted datetime string.
     *
     * @param dateTimeString Datetime string in format dd.MM.yyyy HH:mm:ss
     * @return Parsed LocalDateTime
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static LocalDateTime parseTurkishDateTime(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, TURKISH_DATETIME_FORMATTER);
    }

    /**
     * Parses a Turkish formatted time string.
     *
     * @param timeString Time string in format HH:mm:ss
     * @return Parsed LocalTime
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static LocalTime parseTurkishTime(String timeString) {
        return LocalTime.parse(timeString, TURKISH_TIME_FORMATTER);
    }

    /**
     * Checks if the current time is within BIST trading hours.
     *
     * @return true if current time is within trading hours
     */
    public static boolean isBistTradingHours() {
        return isBistTradingHours(nowTimeInTurkey());
    }

    /**
     * Checks if the given time is within BIST trading hours.
     *
     * @param time The time to check
     * @return true if the time is within trading hours
     */
    public static boolean isBistTradingHours(LocalTime time) {
        return (time.isAfter(BIST_MORNING_SESSION_START.minusSeconds(1)) &&
                time.isBefore(BIST_MORNING_SESSION_END.plusSeconds(1))) ||
               (time.isAfter(BIST_AFTERNOON_SESSION_START.minusSeconds(1)) &&
                time.isBefore(BIST_AFTERNOON_SESSION_END.plusSeconds(1)));
    }

    /**
     * Checks if the current date is a Turkish business day (Monday to Friday).
     *
     * @return true if today is a business day
     */
    public static boolean isTurkishBusinessDay() {
        return isTurkishBusinessDay(todayInTurkey());
    }

    /**
     * Checks if the given date is a Turkish business day (Monday to Friday).
     *
     * @param date The date to check
     * @return true if the date is a business day
     */
    public static boolean isTurkishBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * Returns the next Turkish business day.
     *
     * @return Next business day
     */
    public static LocalDate getNextTurkishBusinessDay() {
        return getNextTurkishBusinessDay(todayInTurkey());
    }

    /**
     * Returns the next Turkish business day after the given date.
     *
     * @param date The starting date
     * @return Next business day
     */
    public static LocalDate getNextTurkishBusinessDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        while (!isTurkishBusinessDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    /**
     * Returns the previous Turkish business day.
     *
     * @return Previous business day
     */
    public static LocalDate getPreviousTurkishBusinessDay() {
        return getPreviousTurkishBusinessDay(todayInTurkey());
    }

    /**
     * Returns the previous Turkish business day before the given date.
     *
     * @param date The starting date
     * @return Previous business day
     */
    public static LocalDate getPreviousTurkishBusinessDay(LocalDate date) {
        LocalDate previousDay = date.minusDays(1);
        while (!isTurkishBusinessDay(previousDay)) {
            previousDay = previousDay.minusDays(1);
        }
        return previousDay;
    }

    /**
     * Calculates the number of business days between two dates.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (exclusive)
     * @return Number of business days
     */
    public static long getBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        long businessDays = 0;

        LocalDate current = startDate;
        for (long i = 0; i < totalDays; i++) {
            if (isTurkishBusinessDay(current)) {
                businessDays++;
            }
            current = current.plusDays(1);
        }

        return businessDays;
    }

    /**
     * Adds business days to a date.
     *
     * @param date         The starting date
     * @param businessDays Number of business days to add
     * @return Date after adding business days
     */
    public static LocalDate addBusinessDays(LocalDate date, int businessDays) {
        LocalDate result = date;
        int addedDays = 0;

        while (addedDays < businessDays) {
            result = result.plusDays(1);
            if (isTurkishBusinessDay(result)) {
                addedDays++;
            }
        }

        return result;
    }

    /**
     * Returns the start of the day in Turkish timezone.
     *
     * @param date The date
     * @return ZonedDateTime at start of day in Turkish timezone
     */
    public static ZonedDateTime startOfDayInTurkey(LocalDate date) {
        return date.atStartOfDay(TURKISH_ZONE);
    }

    /**
     * Returns the end of the day in Turkish timezone.
     *
     * @param date The date
     * @return ZonedDateTime at end of day in Turkish timezone
     */
    public static ZonedDateTime endOfDayInTurkey(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(TURKISH_ZONE);
    }

    /**
     * Returns the start of the month in Turkish timezone.
     *
     * @param date The date
     * @return ZonedDateTime at start of month in Turkish timezone
     */
    public static ZonedDateTime startOfMonthInTurkey(LocalDate date) {
        return date.withDayOfMonth(1).atStartOfDay(TURKISH_ZONE);
    }

    /**
     * Returns the end of the month in Turkish timezone.
     *
     * @param date The date
     * @return ZonedDateTime at end of month in Turkish timezone
     */
    public static ZonedDateTime endOfMonthInTurkey(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth())
                   .atTime(LocalTime.MAX)
                   .atZone(TURKISH_ZONE);
    }

    /**
     * Converts epoch milliseconds to ZonedDateTime in Turkish timezone.
     *
     * @param epochMilli Epoch milliseconds
     * @return ZonedDateTime in Turkish timezone
     */
    public static ZonedDateTime fromEpochMilliInTurkey(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli).atZone(TURKISH_ZONE);
    }

    /**
     * Converts ZonedDateTime to epoch milliseconds.
     *
     * @param zonedDateTime The zoned datetime
     * @return Epoch milliseconds
     */
    public static long toEpochMilli(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * Checks if a date is in the past (compared to today in Turkish timezone).
     *
     * @param date The date to check
     * @return true if the date is in the past
     */
    public static boolean isInPast(LocalDate date) {
        return date.isBefore(todayInTurkey());
    }

    /**
     * Checks if a date is in the future (compared to today in Turkish timezone).
     *
     * @param date The date to check
     * @return true if the date is in the future
     */
    public static boolean isInFuture(LocalDate date) {
        return date.isAfter(todayInTurkey());
    }

    /**
     * Checks if a datetime is in the past (compared to now in Turkish timezone).
     *
     * @param dateTime The datetime to check
     * @return true if the datetime is in the past
     */
    public static boolean isInPast(LocalDateTime dateTime) {
        return dateTime.isBefore(nowInTurkey().toLocalDateTime());
    }

    /**
     * Checks if a datetime is in the future (compared to now in Turkish timezone).
     *
     * @param dateTime The datetime to check
     * @return true if the datetime is in the future
     */
    public static boolean isInFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(nowInTurkey().toLocalDateTime());
    }

    /**
     * Returns a human-readable Turkish description of the time difference.
     *
     * @param fromDateTime Starting datetime
     * @param toDateTime   Ending datetime
     * @return Human-readable time difference in Turkish
     */
    public static String getHumanReadableTimeDifference(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        Duration duration = Duration.between(fromDateTime, toDateTime);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%d gün %d saat", days, hours);
        } else if (hours > 0) {
            return String.format("%d saat %d dakika", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d dakika", minutes);
        } else {
            return "Az önce";
        }
    }
}