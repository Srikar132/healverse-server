package com.bytehealers.healverse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The single source of "now" for business logic. Logs are stored as UTC wall-clock time, so the
 * clock is UTC; tests substitute a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
