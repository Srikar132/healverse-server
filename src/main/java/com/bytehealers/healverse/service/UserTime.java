package com.bytehealers.healverse.service;

import com.bytehealers.healverse.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Decides where a user's day starts and ends. Logs are stored as UTC wall-clock time; a "day" is
 * the user's local date in their own time zone, so a day's logs are the UTC span that local date
 * covers. Everything that means "today" or "the logs of this date" goes through here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserTime {

    private final Clock clock;
    private final UserRepository userRepository;

    /** A span of UTC wall-clock time, from {@code start} up to but not including {@code endExclusive}. */
    public record DayRange(LocalDateTime start, LocalDateTime endExclusive) {

        /**
         * Last representable instant of the range at the database's microsecond precision, for
         * the inclusive BETWEEN queries.
         */
        public LocalDateTime endInclusive() {
            return endExclusive.minusNanos(1_000);
        }
    }

    public ZoneId zoneOf(Long userId) {
        return userRepository.findTimeZoneById(userId)
                .map(this::toZone)
                .orElse(ZoneOffset.UTC);
    }

    public LocalDate today(Long userId) {
        return LocalDate.now(clock.withZone(zoneOf(userId)));
    }

    /** The local date, in the user's zone, of a stored UTC wall-clock time. */
    public LocalDate localDateOf(Long userId, LocalDateTime utcWallClock) {
        return utcWallClock.atOffset(ZoneOffset.UTC).atZoneSameInstant(zoneOf(userId)).toLocalDate();
    }

    public DayRange dayRange(Long userId, LocalDate date) {
        return range(userId, date, date);
    }

    /** The span covering the local dates {@code from} to {@code to}, both inclusive. */
    public DayRange range(Long userId, LocalDate from, LocalDate to) {
        ZoneId zone = zoneOf(userId);
        return new DayRange(toUtc(from, zone), toUtc(to.plusDays(1), zone));
    }

    private static LocalDateTime toUtc(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private ZoneId toZone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            log.warn("Stored time zone '{}' is invalid, using UTC", id);
            return ZoneOffset.UTC;
        }
    }
}
