package com.bytehealers.healverse.controller;

import com.bytehealers.healverse.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FoodLoggingControllerTests {

    @Test
    void missingOrBlankLoggedAtMeansNotSent() {
        assertNull(FoodLoggingController.parseLoggedAt(null));
        assertNull(FoodLoggingController.parseLoggedAt("  "));
    }

    @Test
    void instantFromTheAppIsStoredAsUtcWallClock() {
        assertEquals(LocalDateTime.of(2026, 1, 2, 23, 5, 7, 123_000_000),
                FoodLoggingController.parseLoggedAt("2026-01-02T23:05:07.123Z"));
    }

    @Test
    void offsetIsConvertedToUtc() {
        assertEquals(LocalDateTime.of(2026, 1, 2, 18, 0),
                FoodLoggingController.parseLoggedAt("2026-01-02T23:30:00+05:30"));
    }

    @Test
    void plainLocalDateTimeIsKeptAsIs() {
        assertEquals(LocalDateTime.of(2026, 1, 2, 10, 30),
                FoodLoggingController.parseLoggedAt("2026-01-02T10:30:00"));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(BadRequestException.class, () -> FoodLoggingController.parseLoggedAt("yesterday"));
    }
}
