package com.bisttrading.core.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DateTimeUtils.
 */
class DateTimeUtilsTest {

    @Test
    void shouldGetCurrentTimeInTurkey() {
        ZonedDateTime now = DateTimeUtils.nowInTurkey();

        assertThat(now.getZone()).isEqualTo(DateTimeUtils.TURKISH_ZONE);
        assertThat(now).isNotNull();
    }

    @Test
    void shouldGetTodayInTurkey() {
        LocalDate today = DateTimeUtils.todayInTurkey();

        assertThat(today).isNotNull();
        assertThat(today).isBeforeOrEqualTo(LocalDate.now());
    }

    @Test
    void shouldFormatTurkishDate() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        String formatted = DateTimeUtils.formatTurkishDate(date);

        assertThat(formatted).isEqualTo("15.01.2024");
    }

    @Test
    void shouldFormatTurkishDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        String formatted = DateTimeUtils.formatTurkishDateTime(dateTime);

        assertThat(formatted).isEqualTo("15.01.2024 14:30:45");
    }

    @Test
    void shouldFormatTurkishTime() {
        LocalTime time = LocalTime.of(14, 30, 45);
        String formatted = DateTimeUtils.formatTurkishTime(time);

        assertThat(formatted).isEqualTo("14:30:45");
    }

    @Test
    void shouldParseTurkishDate() {
        LocalDate parsed = DateTimeUtils.parseTurkishDate("15.01.2024");

        assertThat(parsed).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void shouldParseTurkishDateTime() {
        LocalDateTime parsed = DateTimeUtils.parseTurkishDateTime("15.01.2024 14:30:45");

        assertThat(parsed).isEqualTo(LocalDateTime.of(2024, 1, 15, 14, 30, 45));
    }

    @Test
    void shouldParseTurkishTime() {
        LocalTime parsed = DateTimeUtils.parseTurkishTime("14:30:45");

        assertThat(parsed).isEqualTo(LocalTime.of(14, 30, 45));
    }

    @Test
    void shouldDetectBistTradingHours() {
        // Morning session
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(10, 30))).isTrue();
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(12, 0))).isTrue();

        // Afternoon session
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(14, 30))).isTrue();
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(17, 30))).isTrue();

        // Outside trading hours
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(9, 30))).isFalse();
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(13, 0))).isFalse();
        assertThat(DateTimeUtils.isBistTradingHours(LocalTime.of(19, 0))).isFalse();
    }

    @Test
    void shouldDetectBusinessDays() {
        // Monday
        LocalDate monday = LocalDate.of(2024, 1, 15);
        assertThat(DateTimeUtils.isTurkishBusinessDay(monday)).isTrue();

        // Friday
        LocalDate friday = LocalDate.of(2024, 1, 19);
        assertThat(DateTimeUtils.isTurkishBusinessDay(friday)).isTrue();

        // Saturday
        LocalDate saturday = LocalDate.of(2024, 1, 20);
        assertThat(DateTimeUtils.isTurkishBusinessDay(saturday)).isFalse();

        // Sunday
        LocalDate sunday = LocalDate.of(2024, 1, 21);
        assertThat(DateTimeUtils.isTurkishBusinessDay(sunday)).isFalse();
    }

    @Test
    void shouldGetNextBusinessDay() {
        // Friday -> next business day should be Monday
        LocalDate friday = LocalDate.of(2024, 1, 19);
        LocalDate nextBusinessDay = DateTimeUtils.getNextTurkishBusinessDay(friday);

        assertThat(nextBusinessDay).isEqualTo(LocalDate.of(2024, 1, 22)); // Monday

        // Wednesday -> next business day should be Thursday
        LocalDate wednesday = LocalDate.of(2024, 1, 17);
        LocalDate nextBusinessDay2 = DateTimeUtils.getNextTurkishBusinessDay(wednesday);

        assertThat(nextBusinessDay2).isEqualTo(LocalDate.of(2024, 1, 18)); // Thursday
    }

    @Test
    void shouldGetPreviousBusinessDay() {
        // Monday -> previous business day should be Friday
        LocalDate monday = LocalDate.of(2024, 1, 22);
        LocalDate previousBusinessDay = DateTimeUtils.getPreviousTurkishBusinessDay(monday);

        assertThat(previousBusinessDay).isEqualTo(LocalDate.of(2024, 1, 19)); // Friday

        // Wednesday -> previous business day should be Tuesday
        LocalDate wednesday = LocalDate.of(2024, 1, 17);
        LocalDate previousBusinessDay2 = DateTimeUtils.getPreviousTurkishBusinessDay(wednesday);

        assertThat(previousBusinessDay2).isEqualTo(LocalDate.of(2024, 1, 16)); // Tuesday
    }

    @Test
    void shouldCalculateBusinessDaysBetween() {
        LocalDate start = LocalDate.of(2024, 1, 15); // Monday
        LocalDate end = LocalDate.of(2024, 1, 22); // Monday (next week)

        long businessDays = DateTimeUtils.getBusinessDaysBetween(start, end);

        assertThat(businessDays).isEqualTo(5); // Mon, Tue, Wed, Thu, Fri
    }

    @Test
    void shouldAddBusinessDays() {
        LocalDate start = LocalDate.of(2024, 1, 17); // Wednesday
        LocalDate result = DateTimeUtils.addBusinessDays(start, 3);

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 22)); // Monday (skipping weekend)
    }

    @Test
    void shouldConvertToTurkishTime() {
        ZonedDateTime utc = ZonedDateTime.parse("2024-01-15T12:00:00Z");
        ZonedDateTime turkish = DateTimeUtils.toTurkishTime(utc);

        assertThat(turkish.getZone()).isEqualTo(DateTimeUtils.TURKISH_ZONE);
        // Turkey is UTC+3, so 12:00 UTC = 15:00 Turkish time
        assertThat(turkish.getHour()).isEqualTo(15);
    }

    @Test
    void shouldDetectPastAndFutureDates() {
        LocalDate today = DateTimeUtils.todayInTurkey();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        assertThat(DateTimeUtils.isInPast(yesterday)).isTrue();
        assertThat(DateTimeUtils.isInPast(today)).isFalse();
        assertThat(DateTimeUtils.isInFuture(tomorrow)).isTrue();
        assertThat(DateTimeUtils.isInFuture(today)).isFalse();
    }

    @Test
    void shouldCreateStartAndEndOfDay() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        ZonedDateTime startOfDay = DateTimeUtils.startOfDayInTurkey(date);
        ZonedDateTime endOfDay = DateTimeUtils.endOfDayInTurkey(date);

        assertThat(startOfDay.toLocalTime()).isEqualTo(LocalTime.MIN);
        assertThat(endOfDay.toLocalTime()).isEqualTo(LocalTime.MAX);
        assertThat(startOfDay.getZone()).isEqualTo(DateTimeUtils.TURKISH_ZONE);
        assertThat(endOfDay.getZone()).isEqualTo(DateTimeUtils.TURKISH_ZONE);
    }

    @Test
    void shouldCreateStartAndEndOfMonth() {
        LocalDate date = LocalDate.of(2024, 2, 15); // February

        ZonedDateTime startOfMonth = DateTimeUtils.startOfMonthInTurkey(date);
        ZonedDateTime endOfMonth = DateTimeUtils.endOfMonthInTurkey(date);

        assertThat(startOfMonth.toLocalDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(endOfMonth.toLocalDate()).isEqualTo(LocalDate.of(2024, 2, 29)); // 2024 is leap year
    }

    @Test
    void shouldConvertEpochMillis() {
        long epochMilli = 1705320000000L; // Jan 15, 2024 12:00:00 UTC
        ZonedDateTime converted = DateTimeUtils.fromEpochMilliInTurkey(epochMilli);

        assertThat(converted.getZone()).isEqualTo(DateTimeUtils.TURKISH_ZONE);

        long convertedBack = DateTimeUtils.toEpochMilli(converted);
        assertThat(convertedBack).isEqualTo(epochMilli);
    }

    @Test
    void shouldGenerateHumanReadableTimeDifference() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 16, 12, 30);

        String difference = DateTimeUtils.getHumanReadableTimeDifference(start, end);

        assertThat(difference).contains("gün").contains("saat");
    }
}