package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.BadRequestException;

import java.time.DateTimeException;
import java.time.ZoneId;

/** Parsing of the IANA time zone ids clients send. */
final class TimeZones {

    private TimeZones() {
    }

    /**
     * Accepts region ids such as {@code Asia/Kolkata} or {@code UTC}. Fixed offsets such as
     * {@code +05:30} are rejected: they carry no daylight-saving rules, so a day computed from
     * them would drift for users in zones that observe it.
     */
    static ZoneId parse(String value) {
        String id = value == null ? "" : value.trim();
        if (!ZoneId.getAvailableZoneIds().contains(id)) {
            throw new BadRequestException("Unknown time zone '" + id + "'. Use an IANA id such as Asia/Kolkata.");
        }
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            throw new BadRequestException("Unknown time zone '" + id + "'. Use an IANA id such as Asia/Kolkata.");
        }
    }
}
