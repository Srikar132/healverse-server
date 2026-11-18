package com.bytehealers.healverse.service;

import com.bytehealers.healverse.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTimeTests {

    private static final Long USER_ID = 7L;

    private UserRepository userRepository;
    private UserTime userTime;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        // 20:00 UTC on 15 Jan is already 16 Jan 01:30 in India
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T20:00:00Z"), ZoneOffset.UTC);
        userTime = new UserTime(clock, userRepository);
    }

    private void zone(String id) {
        when(userRepository.findTimeZoneById(USER_ID)).thenReturn(Optional.of(id));
    }

    @Test
    void todayIsTheLocalDateOfTheUsersZone() {
        zone("Asia/Kolkata");
        assertEquals(LocalDate.of(2026, 1, 16), userTime.today(USER_ID));

        zone("America/Los_Angeles");
        assertEquals(LocalDate.of(2026, 1, 15), userTime.today(USER_ID));
    }

    @Test
    void usersWithoutAZoneOrWithAnInvalidOneGetUtc() {
        assertEquals(LocalDate.of(2026, 1, 15), userTime.today(USER_ID));

        zone("not/a-zone");
        assertEquals(LocalDate.of(2026, 1, 15), userTime.today(USER_ID));
    }

    @Test
    void dayRangeIsTheUtcSpanOfTheLocalDate() {
        zone("Asia/Kolkata");

        UserTime.DayRange day = userTime.dayRange(USER_ID, LocalDate.of(2026, 1, 15));

        assertEquals(LocalDateTime.of(2026, 1, 14, 18, 30), day.start());
        assertEquals(LocalDateTime.of(2026, 1, 15, 18, 30), day.endExclusive());
        assertEquals(LocalDateTime.of(2026, 1, 15, 18, 29, 59, 999_999_000), day.endInclusive());
    }

    @Test
    void dayRangeInUtcIsTheCalendarDay() {
        UserTime.DayRange day = userTime.dayRange(USER_ID, LocalDate.of(2026, 1, 15));

        assertEquals(LocalDateTime.of(2026, 1, 15, 0, 0), day.start());
        assertEquals(LocalDateTime.of(2026, 1, 16, 0, 0), day.endExclusive());
    }

    @Test
    void rangeCoversAllDatesFromFirstToLastInclusive() {
        zone("Asia/Kolkata");

        UserTime.DayRange range = userTime.range(USER_ID, LocalDate.of(2026, 1, 13), LocalDate.of(2026, 1, 15));

        assertEquals(LocalDateTime.of(2026, 1, 12, 18, 30), range.start());
        assertEquals(LocalDateTime.of(2026, 1, 15, 18, 30), range.endExclusive());
    }

    @Test
    void dayThatLosesAnHourToDaylightSavingIsTwentyThreeHoursLong() {
        zone("America/New_York");

        UserTime.DayRange day = userTime.dayRange(USER_ID, LocalDate.of(2026, 3, 8));

        assertEquals(Duration.ofHours(23), Duration.between(day.start(), day.endExclusive()));
    }

    @Test
    void storedUtcTimeMapsToTheLocalDateInTheUsersZone() {
        zone("Asia/Kolkata");

        assertEquals(LocalDate.of(2026, 1, 16), userTime.localDateOf(USER_ID, LocalDateTime.of(2026, 1, 15, 18, 30)));
        assertEquals(LocalDate.of(2026, 1, 15), userTime.localDateOf(USER_ID, LocalDateTime.of(2026, 1, 15, 18, 29)));
    }
}
